package com.cafe94.services;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.cafe94.domain.Booking;
import com.cafe94.domain.Table;
import com.cafe94.domain.User;
import com.cafe94.enums.BookingStatus;
import com.cafe94.enums.Permission;
import com.cafe94.enums.TableStatus;
// ** Import specific permissions **
import static com.cafe94.enums.Permission.APPROVE_BOOKING;
import static com.cafe94.enums.Permission.REJECT_BOOKING;
import static com.cafe94.enums.Permission.CANCEL_OWN_BOOKING;
import static com.cafe94.enums.Permission.CANCEL_ANY_BOOKING;
// Removed UPDATE_BOOKING_STATUS, DELETE_BOOKING if not needed elsewhere

import com.cafe94.persistence.IBookingRepository;
import com.cafe94.persistence.ITableRepository;
import com.cafe94.persistence.IUserRepository;

/**
 * Implementation of the IBookingService interface
 */
public class BookingService implements IBookingService {

    private static final Logger LOGGER =
        Logger.getLogger(BookingService.class.getName());
    private static final Duration DEFAULT_BOOKING_DURATION =
        Duration.ofHours(1);

    // Dependencies
    private final IBookingRepository bookingRepository;
    private final IUserRepository userRepository;
    private final ITableRepository tableRepository;
    private final AuthorizationService authService;
    private final INotificationService notificationService;

    /**
     * Constructor for Dependency Injection.
     */
    public BookingService(IBookingRepository bookingRepository,
                          IUserRepository userRepository,
                          ITableRepository tableRepository,
                          AuthorizationService authService,
                          INotificationService notificationService) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.tableRepository = Objects.requireNonNull(tableRepository);
        this.authService = Objects.requireNonNull(authService);
        this.notificationService = Objects.requireNonNull(notificationService);
    }

    @Override
    public Booking requestBooking(int customerId, int numberOfGuests,
                                  LocalDate date, LocalTime time,
                                  Duration duration, String notes) {
        // Validation remains the same...
        Objects.requireNonNull(date, "Booking date cannot be null.");
        Objects.requireNonNull(time, "Booking time cannot be null.");
        Duration bookingDuration = (duration != null && !duration.isZero()
            && !duration.isNegative()) ? duration : DEFAULT_BOOKING_DURATION;
        LOGGER.log(Level.FINE, "Using booking duration: {0}",
            bookingDuration);
        if (numberOfGuests <= 0) {
            throw new IllegalArgumentException(
                "Number of guests must be positive.");
        }
        LocalDateTime bookingDateTime = LocalDateTime.of(date, time);
        if (bookingDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                "Booking date and time must be in the future.");
        }
        userRepository.findById(customerId)
            .orElseThrow(() -> new NoSuchElementException(
                "Customer not found with ID " + customerId));

        // Create Booking entity
        Booking newBooking = new Booking(0, customerId, date, time,
            numberOfGuests, BookingStatus.PENDING_APPROVAL, notes);

        // Save Booking Request
        Booking savedBooking = bookingRepository.save(newBooking);
        LOGGER.log(Level.INFO, "Customer {0} requested booking (ID: {1}) " +
            "for {2} at {3} for {4} guests.",
                   new Object[]{customerId, savedBooking.getBookingID(), date,
                    time, numberOfGuests});

        notificationService.notifyPendingBooking(savedBooking);
        return savedBooking;
    }

    @Override
    public boolean approveBooking(int bookingId, User approver) {
        Objects.requireNonNull(approver, "Approver user cannot be null.");
        // ** Use specific permission **
        authService.checkPermission(approver, APPROVE_BOOKING);

        Booking booking = findBookingByIdOrThrow(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING_APPROVAL) {
            LOGGER.log(Level.WARNING, "Booking {0} is not pending " +
                "approval (Status: {1}). No action taken.",
                new Object[]{bookingId, booking.getStatus()});
            return false;
        }

        int assignedTableNumber = findAndReserveSuitableTable(booking);

        if (assignedTableNumber <= 0) {
            LOGGER.log(Level.WARNING, "No suitable table could be " +
                "reserved for booking {0}. Rejecting.", bookingId);
            // Use internal reject helper, which also checks permission
            return rejectBookingInternal(booking, approver,
                "No suitable table available at the requested time.");
        }

        booking.assignTable(assignedTableNumber);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        LOGGER.log(Level.INFO,
            "Booking {0} approved by User {1}, assigned Table {2}",
            new Object[]{bookingId, approver.getUserID(),
                         assignedTableNumber});
        notificationService.sendBookingConfirmation(booking);
        return true;
    }

    @Override
    public boolean rejectBooking(int bookingId, User staffMember,
                                 String reason) {
        Objects.requireNonNull(staffMember, "Staff member cannot be null.");
        // ** Use specific permission **
        authService.checkPermission(staffMember, REJECT_BOOKING);

        Booking booking = findBookingByIdOrThrow(bookingId);
        // Delegate actual rejection logic to internal method
        return rejectBookingInternal(booking, staffMember, reason);
    }

    // Internal helper ensures status check happens before saving REJECTED
    private boolean rejectBookingInternal(Booking booking, User staffMember,
                                          String reason) {
        // Permission check should have happened before calling this internal method

        if (booking.getStatus() != BookingStatus.PENDING_APPROVAL) {
            LOGGER.log(Level.WARNING,
                "Booking {0} cannot be rejected from status: {1}",
                new Object[]{booking.getBookingID(), booking.getStatus()});
            // If called during approveBooking due to no table, this might
            // indicate a race condition or logic issue. Return false.
            return false;
        }

        booking.setStatus(BookingStatus.REJECTED);
        bookingRepository.save(booking);
        String reasonText = (reason != null && !reason.trim().isEmpty()) ?
            reason : "Not specified";
        LOGGER.log(Level.INFO,
            "Booking {0} rejected by User {1}. Reason: {2}",
            new Object[]{booking.getBookingID(), staffMember.getUserID(),
                         reasonText});
        notificationService.sendBookingRejection(booking, reason);
        return true;
    }


    @Override
    public boolean cancelBooking(int bookingId, User canceller) {
        Objects.requireNonNull(canceller, "Canceller user cannot be null.");
        Booking booking = findBookingByIdOrThrow(bookingId);

        // ** Use specific permissions **
        boolean isOwner = booking.getCustomerID() == canceller.getUserID();
        Permission requiredPerm = isOwner ? CANCEL_OWN_BOOKING
                                          : CANCEL_ANY_BOOKING;
        // Check if user has the specific permission needed
        try {
             authService.checkPermission(canceller, requiredPerm);
        } catch (SecurityException e) {
             // If owner lacked CANCEL_OWN, check if they have CANCEL_ANY
             // (e.g., staff cancelling their own booking as customer)
             if (isOwner) {
                 // If they don't have CANCEL_ANY either, re-throw original error
                 authService.checkPermission(canceller, CANCEL_ANY_BOOKING);
             } else {
                 // If non-owner lacked CANCEL_ANY, re-throw original error
                 throw e;
             }
        }


        BookingStatus currentStatus = booking.getStatus();
        if (currentStatus != BookingStatus.PENDING_APPROVAL &&
            currentStatus != BookingStatus.CONFIRMED) {
             throw new IllegalStateException("Booking " + bookingId +
                 " cannot be cancelled from status: " + currentStatus);
        }

        BookingStatus cancelledStatus = isOwner ?
            BookingStatus.CANCELLED_BY_CUSTOMER :
            BookingStatus.CANCELLED_BY_STAFF;
        booking.setStatus(cancelledStatus);

        bookingRepository.save(booking);
        LOGGER.log(Level.INFO,
            "User {0} cancelled booking {1}. New Status: {2}",
            new Object[]{canceller.getUserID(), bookingId, cancelledStatus});

        if (currentStatus == BookingStatus.CONFIRMED &&
            booking.getTableNumber() > 0) {
             releaseReservedTable(booking.getTableNumber());
        }

        notificationService.sendBookingCancellation(booking, cancelledStatus);
        return true;
    }

    // --- Retrieval Methods (remain the same) ---

    @Override
    public Optional<Booking> getBookingById(int bookingId) {
        if (bookingId <= 0) {
             LOGGER.log(Level.FINER,
                 "getBookingById called with non-positive ID: {0}", bookingId);
            return Optional.empty();
        }
        return bookingRepository.findById(bookingId);
    }

    @Override
    public List<Booking> getCustomerBookings(int customerId) {
        if (customerId <= 0) {
             throw new IllegalArgumentException("Customer ID must be positive.");
        }
         userRepository.findById(customerId).orElseThrow(() ->
             new NoSuchElementException("Customer not found: " + customerId));
         LOGGER.log(Level.FINE, "Retrieving bookings for customer ID: {0}",
             customerId);
        return bookingRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Booking> getBookingsByDate(LocalDate date) {
        Objects.requireNonNull(date, "Date cannot be null.");
         LOGGER.log(Level.FINE, "Retrieving bookings for date: {0}", date);
        return bookingRepository.findByDate(date);
    }

    // --- Helper Methods (remain the same) ---

    /** Finds a booking by ID or throws NoSuchElementException. */
    private Booking findBookingByIdOrThrow(int bookingId) {
         if (bookingId <= 0) throw new IllegalArgumentException(
            "Booking ID must be positive.");
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException(
                    "Booking not found with ID: " + bookingId));
    }

    /** Finds and reserves a suitable table. */
    private int findAndReserveSuitableTable(Booking booking) {
        // Logic remains the same as previous version...
        int requiredCapacity = booking.getNumberOfGuests();
        LocalDateTime bookingStart = booking.getBookingDateTime();
        LocalDateTime bookingEnd = bookingStart.plus(DEFAULT_BOOKING_DURATION);

        LOGGER.log(Level.INFO, "Searching for table for {0} guests " +
            "from {1} to {2}", new Object[]{requiredCapacity,
            bookingStart, bookingEnd});

        List<Table> potentialTables = tableRepository
            .findWithCapacityGreaterThanOrEqual(requiredCapacity);

        potentialTables.sort(
            Comparator.comparingInt(Table::getCapacity)
                      .thenComparingInt(Table::getTableNumber)
        );

        LOGGER.log(Level.FINE, "Found {0} potential tables by capacity.",
                   potentialTables.size());

        for (Table table : potentialTables) {
            int tableNum = table.getTableNumber();
            LOGGER.log(Level.FINER,
                "Checking table T{0} (Cap: {1})...",
                new Object[]{tableNum, table.getCapacity()});

            List<Booking> conflictingBookings = bookingRepository
                .findByTableAndDateTimeRange(tableNum, bookingStart, bookingEnd);

            if (conflictingBookings.isEmpty()) {
                LOGGER.log(Level.INFO,
                    "Table T{0} has no conflicting bookings.", tableNum);
                try {
                    Table freshTable = tableRepository
                                        .findByTableNumber(tableNum)
                                        .orElse(table);

                    if (freshTable.getStatus() == TableStatus.AVAILABLE) {
                        freshTable.reserve();
                        tableRepository.save(freshTable);
                        LOGGER.log(Level.INFO,
                            "Reserved table T{0} for booking {1}",
                            new Object[]{tableNum, booking.getBookingID()});
                        return tableNum; // Success
                    } else {
                        LOGGER.log(Level.INFO, "Table T{0} no conflicts, " +
                            "but status {1} != AVAILABLE.",
                            new Object[]{tableNum, freshTable.getStatus()});
                    }
                } catch (IllegalStateException e) {
                    LOGGER.log(Level.WARNING,
                        "Failed reserve table T{0} state: {1}",
                        new Object[]{tableNum, e.getMessage()});
                } catch (Exception e) {
                     LOGGER.log(Level.SEVERE,
                        "Error saving reserved status for table T{0}",
                        new Object[]{tableNum, e});
                }
            } else {
                 LOGGER.log(Level.INFO,
                    "Table T{0} has {1} conflicting booking(s).",
                    new Object[]{tableNum, conflictingBookings.size()});
            }
        }
        LOGGER.log(Level.WARNING,
            "No suitable table found for booking {0}.",
            booking.getBookingID());
        return 0; // No suitable table found/reserved
    }

    /** Releases a reserved table. */
    private void releaseReservedTable(int tableNumber) {
        // Logic remains the same as previous version...
         if (tableNumber <= 0) {
             LOGGER.log(Level.WARNING,
                 "Attempt release table invalid number: {0}", tableNumber);
             return;
         }
         LOGGER.log(Level.FINE, "Attempting release table T{0}", tableNumber);
         try {
             Optional<Table> tableOpt =
                 tableRepository.findByTableNumber(tableNumber);
             if (tableOpt.isPresent()) {
                 Table table = tableOpt.get();
                 if (table.getStatus() == TableStatus.RESERVED) {
                     table.makeAvailable();
                     tableRepository.save(table);
                     LOGGER.log(Level.INFO, "Table T{0} status set back " +
                         "to AVAILABLE after booking cancellation.",
                         tableNumber);
                 } else {
                     LOGGER.log(Level.WARNING, "Attempted release table " +
                         "T{0} which was not RESERVED (Status: {1}).",
                         new Object[]{tableNumber, table.getStatus()});
                 }
             } else {
                 LOGGER.log(Level.SEVERE,
                     "Cannot release table T{0}: Not found.", tableNumber);
             }
         } catch (IllegalStateException e) {
             LOGGER.log(Level.SEVERE, "Error making table T{0} " +
                 "available (state issue?): {1}",
                 new Object[]{tableNumber, e.getMessage()});
         } catch (Exception e) {
              LOGGER.log(Level.SEVERE,
                  "Error releasing table T{0}",
                  new Object[]{tableNumber, e});
         }
    }

    @Override
    public List<Booking> getBookingByStatus(BookingStatus status) {
         // Ensures status parameter is not null before proceeding
         Objects.requireNonNull(status,
             "Status cannot be null for findByStatus.");

         // Delegates the actual data retrieval to the booking repository
         return bookingRepository.findByStatus(status);
    }
}