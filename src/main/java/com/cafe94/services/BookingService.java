// File: src/main/java/com/cafe94/services/BookingService.java
package com.cafe94.services;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.cafe94.domain.Booking;
import com.cafe94.domain.Table;
import com.cafe94.domain.User;
import com.cafe94.enums.BookingStatus;
import com.cafe94.enums.Permission;
import static com.cafe94.enums.Permission.APPROVE_BOOKING;
import static com.cafe94.enums.Permission.CANCEL_ANY_BOOKING;
import static com.cafe94.enums.Permission.CANCEL_OWN_BOOKING;
import static com.cafe94.enums.Permission.REJECT_BOOKING;
import com.cafe94.persistence.IBookingRepository;
import com.cafe94.persistence.ITableRepository;
import com.cafe94.persistence.IUserRepository;

/**
 * Implementation of the IBookingService interface
 * @author Adigun Lateef
 * @version 1.0
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
        this.bookingRepository = Objects.requireNonNull(bookingRepository,
        "BookingRepository cannot be null.");
        this.userRepository = Objects.requireNonNull(userRepository,
        "UserRepository cannot be null.");
        this.tableRepository = Objects.requireNonNull(tableRepository,
        "TableRepository cannot be null.");
        this.authService = Objects.requireNonNull(authService,
        "AuthorizationService cannot be null.");
        this.notificationService = Objects.requireNonNull(notificationService,
        "NotificationService cannot be null.");
    }

    /**
     * Handles a customer's request for a new booking.
     * @param customerId ID of the customer.
     * @param date Booking date.
     * @param time Booking time.
     * @param guests Number of guests.
     * @param duration Requested duration
     * @param notes Optional notes.
     * @return The created Booking in PENDING_APPROVAL state.
     * @throws // Exceptions from validation or persistence.
     */
    @Override
    // Added Duration parameter to match interface
    public Booking requestBooking(int customerId, int numberOfGuests,
    LocalDate date, LocalTime time, Duration duration, String notes) {
        // Validate Inputs
        Objects.requireNonNull(date, "Booking date cannot be null.");
        Objects.requireNonNull(time, "Booking time cannot be null.");
        Duration bookingDuration = (duration != null && !duration.isZero()
        && !duration.isNegative()) ? duration : DEFAULT_BOOKING_DURATION;
        // Log effective duration
        LOGGER.log(Level.FINE, "Using booking duration: {0}",
        bookingDuration);
        if (numberOfGuests <= 0) {
            throw new IllegalArgumentException(
                "Number of guests must be positive.");
        }
        LocalDateTime bookingDateTime = LocalDateTime.of(date, time);
        // Prevent booking in the past
        if (bookingDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                "Booking date and time must be in the future.");
        }

        // Validate Customer
        userRepository.findById(customerId)
            .orElseThrow(() -> new NoSuchElementException(
                "Cannot request booking: Customer not found with ID " +
                customerId));

        // Create Booking entity in PENDING state
        Booking newBooking = new Booking(0, customerId, date, time,
        numberOfGuests, BookingStatus.PENDING_APPROVAL, notes);
    

        // Save Booking Request
        Booking savedBooking = bookingRepository.save(newBooking);
        LOGGER.log(Level.INFO, "Customer {0} requested booking (ID: {1}) " +
        "for {2} at {3} for {4} guests.",
                   new Object[]{customerId, savedBooking.getBookingID(), date,
                    time, numberOfGuests});

        // Notify Staff about pending request
        notificationService.notifyPendingBooking(savedBooking);

        return savedBooking;
    }

    /**
     * Cancels a booking
     * @param bookingId ID of the booking to cancel.
     * @param canceller The user attempting cancellation.
     * @return true if cancelled successfully, false otherwise.
     * @throws // SecurityException, IllegalStateException,
     * NoSuchElementException
     */
    @Override
    public boolean cancelBooking(int bookingId, User canceller) {
        Objects.requireNonNull(canceller,
        "Canceller user cannot be null.");
        // Finds or throws NoSuchElementException
        Booking booking = findBookingByIdOrThrow(bookingId);
        // Authorization check
        boolean isOwner = booking.getCustomerID() == canceller.getUserID();
        Permission requiredPermission = isOwner ? CANCEL_OWN_BOOKING :
        CANCEL_ANY_BOOKING;
        try {
             authService.checkPermission(canceller, requiredPermission);
        } catch (SecurityException e) {
            // If owner failed, check if user has general cancel permission
             if (!isOwner) {
                 authService.checkPermission(canceller, CANCEL_ANY_BOOKING);
             } else {
                // Re-throw if owner truly lacks permission
                 throw e;
             }
        }

        // Check if booking status allows cancellation
        BookingStatus currentStatus = booking.getStatus();
        if (currentStatus != BookingStatus.PENDING_APPROVAL &&
        currentStatus != BookingStatus.CONFIRMED) {
             throw new IllegalStateException("Booking " + bookingId +
             " cannot be cancelled from status: " + currentStatus);
        }

        // Determine final cancelled status and update booking
        BookingStatus cancelledStatus = isOwner ? BookingStatus
        .CANCELLED_BY_CUSTOMER : BookingStatus.CANCELLED_BY_STAFF;
        booking.setStatus(cancelledStatus);

        // Save updated booking
        bookingRepository.save(booking);
        LOGGER.log(Level.INFO,
        "User {0} cancelled booking {1}. New Status: {2}",
        new Object[]{canceller.getUserID(), bookingId, cancelledStatus});

        if (currentStatus == BookingStatus.CONFIRMED &&
        booking.getTableNumber() > 0) {
             releaseReservedTableSimple(booking.getTableNumber());
        }

        // Notify customer
        notificationService.sendBookingCancellation(booking, cancelledStatus);

        return true;
    }

    /**
     * Approves a pending booking, finds and reserves a suitable table.
     * @param bookingId ID of the booking to approve.
     * @param approver Staff member approving the booking.
     * @return true if approved and reserved, false if rejected due to no
     * table.
     * @throws // SecurityException, IllegalStateException,
     * NoSuchElementException
     */
    @Override
    public boolean approveBooking(int bookingId, User approver) {
        Objects.requireNonNull(approver, "Approver user cannot be null.");
        authService.checkPermission(approver, APPROVE_BOOKING);

        Booking booking = findBookingByIdOrThrow(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING_APPROVAL) {
            LOGGER.log(Level.WARNING,"Booking {0} is not pending approval " +
            "(Status: {1}). No action taken.",
            new Object[]{bookingId, booking.getStatus()});
            return false;
        }

        // Find and reserve table
        int assignedTableNumber = findAndReserveSuitableTable(booking);
        if (assignedTableNumber <= 0) {
            LOGGER.log(Level.WARNING, "No suitable table could be reserved " +
            "for booking {0}. Rejecting.", bookingId);
            // Reject booking if no table found
            return rejectBookingInternal(booking, approver,
            "No suitable table available at the requested time.");
        }

        // Assign table and update status
        booking.assignTable(assignedTableNumber);
        booking.setStatus(BookingStatus.CONFIRMED);

        // Save updated booking
        bookingRepository.save(booking);
        LOGGER.log(Level.INFO,
        "Booking {0} approved by User {1}, assigned Table {2}",
                   new Object[]{bookingId, approver.getUserID(),
                    assignedTableNumber});

        // Send confirmation notification
        notificationService.sendBookingConfirmation(booking);

        return true;
    }

    /**
     * Rejects a pending booking.
     * @param bookingId ID of the booking to reject.
     * @param staffMember Staff member rejecting the booking.
     * @param reason Reason for rejection.
     * @return true if rejection successful, false otherwise
     * @throws // SecurityException, NoSuchElementException
     */
    @Override
    public boolean rejectBooking(int bookingId, User staffMember,
    String reason) {
        Objects.requireNonNull(staffMember,
        "Staff member cannot be null.");
        authService.checkPermission(staffMember, REJECT_BOOKING);

        Booking booking = findBookingByIdOrThrow(bookingId);

        return rejectBookingInternal(booking, staffMember, reason);
    }

    /** Internal helper for rejection logic */
    private boolean rejectBookingInternal(Booking booking, User staffMember,
    String reason) {
        if (booking.getStatus() != BookingStatus.PENDING_APPROVAL) {
            LOGGER.log(Level.WARNING,
            "Booking {0} cannot be rejected from status: {1}",
            new Object[]{booking.getBookingID(), booking.getStatus()});
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

    // --- Retrieval Methods ---

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
             throw new IllegalArgumentException(
                "Customer ID must be positive.");
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


    /** Finds a booking by ID or throws NoSuchElementException if not
     * found/invalid.
     */
    private Booking findBookingByIdOrThrow(int bookingId) {
         if (bookingId <= 0) throw new IllegalArgumentException(
            "Booking ID must be positive.");
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException(
                    "Booking not found with ID: " + bookingId));
    }

    /**
     * Placeholder logic: Finds first available table matching capacity
     * and reserves it.
     */
    private int findAndReserveSuitableTable(Booking booking) {
        LOGGER.log(Level.WARNING, "Executing PLACEHOLDER logic for " +
        "findAndReserveSuitableTable for booking {0}. TIME CONFLICTS ARE " +
        "NOT CHECKED DUE TO CONSTRAINT.", booking.getBookingID());

        LocalDateTime bookingStart = booking.getBookingDateTime();
        Duration bookingDuration = DEFAULT_BOOKING_DURATION;
        LocalDateTime bookingEnd = bookingStart.plus(bookingDuration);

        // Find tables that have capacity and are currently available
        List<Table> potentialTables = tableRepository.findAll().stream()
                 .filter(t -> t.getCapacity() >= booking.getNumberOfGuests())
                 .collect(Collectors.toList());

         if (!potentialTables.isEmpty()) {
             Table assignedTable = potentialTables.get(0);
             try {
                 
                 assignedTable.reserve();
                 tableRepository.save(assignedTable);
                 LOGGER.log(Level.INFO, "Placeholder: Reserved table {0} " +
                 "for booking {1}",
                 new Object[]{assignedTable.getTableNumber(),
                    booking.getBookingID()});
                 return assignedTable.getTableNumber();
             } catch (IllegalStateException e) {
                 // Log error if table could not be reserved
                 LOGGER.log(Level.SEVERE,"Placeholder: Failed to reserve " +
                 "table {0} for booking {1}: {2}",
                 new Object[]{assignedTable.getTableNumber(),
                    booking.getBookingID(), e.getMessage()});
                 return 0;
             }
         } else {
              LOGGER.log(Level.WARNING, "Placeholder: No AVAILABLE table " +
              "found with capacity >= {0} for booking {1}",
              new Object[]{booking.getNumberOfGuests(),
                booking.getBookingID()});
              return 0;
         }
    }

    /** Simplified logic to release a reserved table
    */
    private void releaseReservedTableSimple(int tableNumber) {
         if (tableNumber <= 0) {
             LOGGER.log(Level.WARNING,
             "Attempted to release table with invalid number: {0}",
             tableNumber);
             return;
         }
         LOGGER.log(Level.FINE, "Attempting to release RESERVED table {0}",
         tableNumber);
         try{
             tableRepository.findByTableNumber(tableNumber).ifPresent(table -> {
                     table.makeAvailable();
                     tableRepository.save(table);
                     LOGGER.log(Level.INFO,
                     "Table {0} status set back to AVAILABLE after booking " +
                     "cancellation.", tableNumber);
             });
         } catch(IllegalStateException e) {
             LOGGER.log(Level.SEVERE, "Error making table " + tableNumber +
             " available (invalid state transition?)", e);
         } catch(Exception e) {
              LOGGER.log(Level.SEVERE,
              "Error occurred while attempting to release table " +
              tableNumber, e);
         }
    }

}