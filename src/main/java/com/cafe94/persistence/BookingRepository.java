// File: src/main/java/com/cafe94/persistence/BookingRepository.java
package com.cafe94.persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.cafe94.domain.Booking;
import com.cafe94.enums.BookingStatus;

/**
 * Concrete implementation of {@link IBookingRepository} using Java
 * Serialization for persistence.
 * @author  Adigun Lateef
 * @version 1.0
 */
public class BookingRepository implements IBookingRepository {

    private static final Logger LOGGER =
    Logger.getLogger(BookingRepository.class.getName());

    private final Map<Integer, Booking> bookings = new ConcurrentHashMap<>();
    // Thread-safe ID generator.
    private final AtomicInteger nextId = new AtomicInteger(1);
    // Path to the persistence file.
    private final String storageFilePath;

    /**
     * Constructs the repository, loading existing data from the specified
     * file path and initialises the next available ID based on loaded data.
     *
     * @param storageFilePath The path to the file for storing booking data
     * @throws NullPointerException if storageFilePath is null.
     */
    public BookingRepository(String storageFilePath) {

        // Load data on initialisation
        this.storageFilePath = Objects.requireNonNull(storageFilePath,
        "Storage file path cannot be null.");
        loadData();
        // Initialise nextId based on the maximum ID found in loaded data
        int maxId =
        bookings.keySet().stream().max(Integer::compare).orElse(0);
        nextId.set(maxId + 1);
        LOGGER.log(Level.INFO, "BookingRepository initialized. Loaded {0} " +
        "bookings from {1}. Next ID: {2}",
        new Object[]{bookings.size(), this.storageFilePath, nextId.get()});
    }

    /**
     * Saves a new booking or updates an existing one.
     * @param booking The Booking to save or update (must not be null).
     * @return The saved Booking object
     * @throws NullPointerException if booking is null.
     */
    @Override
    public synchronized Booking save(Booking booking) {
        Objects.requireNonNull(booking, "Booking to save cannot be null.");
        int bookingId = booking.getBookingID();

        if (bookingId <= 0) {
            // New booking
            bookingId = nextId.getAndIncrement();
            try {
                
                booking.setBookingID(bookingId);
                LOGGER.log(Level.FINE, "Assigned new ID {0} to booking for " +
                "customer: {1}",
                new Object[]{bookingId, booking.getCustomerID()});
            } catch (Exception e) {
                // If setBookingID throws an exception, log potential errors
                LOGGER.log(Level.SEVERE, "Failed to set new ID on booking " +
                "object. ID generated: " + bookingId, e);
                throw new RuntimeException(
                    "Failed to assign ID to new booking", e);
            }
        } else {
            // Ensure nextId is correctly positioned if saving an
            // existing booking
            nextId.accumulateAndGet(bookingId + 1, Math::max);
             LOGGER.log(Level.FINEST,
             "Saving existing booking with ID {0}", bookingId);
        }
        // Add or update in the map
        bookings.put(bookingId, booking);
        // Persist the changes
        saveData();
        LOGGER.log(Level.INFO,
        "Saved booking: ID={0}, Customer={1}, Date={2}, Status={3}",
        new Object[]{bookingId, booking.getCustomerID(),
            booking.getBookingDate(), booking.getStatus()});

        // Return booking
        return booking;
    }

    /**
     * Finds a booking by its unique persistent ID.
     *
     * @param bookingId The ID of the booking
     * @return An Optional containing the Booking if found, otherwise empty.
     */
    @Override
    public Optional<Booking> findById(int bookingId) {
        if (bookingId <= 0) {
            LOGGER.log(Level.FINER,
            "findById called with non-positive ID: {0}", bookingId);
            return Optional.empty();
        }
        return Optional.ofNullable(bookings.get(bookingId));
    }

    /**
     * Deletes a booking by its unique persistent ID and persists the changes
     * to the storage file.
     *
     * @param bookingId The ID of the booking to delete
     * @return true if a booking was found and deleted, false otherwise.
     */
    @Override
    public synchronized boolean deleteById(int bookingId) {
        if (bookingId <= 0) {
            LOGGER.log(Level.WARNING,
            "Attempted to delete booking with invalid ID: {0}", bookingId);
            return false;
        }
        // Returns the removed value, or null if key not present
        Booking removedBooking = bookings.remove(bookingId);
        if (removedBooking != null) {
            // Persist the removal
            saveData();
            LOGGER.log(Level.INFO, "Deleted booking ID: {0}", bookingId);
            return true;
        } else {
            LOGGER.log(Level.WARNING,
            "Booking ID {0} not found for deletion.", bookingId);
            return false;
        }
    }

    /**
     * Finds all bookings made by a specific customer.
     * @param customerId The ID of the customer
     * @return An unmodifiable List of Bookings made by the customer.
     */
    @Override
    public List<Booking> findByCustomerId(int customerId) {
        if (customerId <= 0) {
            LOGGER.log(Level.FINER,
            "findByCustomerId called with non-positive ID: {0}", customerId);
            return Collections.emptyList();
        }
        return bookings.values().stream()
                .filter(booking -> booking.getCustomerID() == customerId)
                .sorted(Comparator.comparing(Booking::getBookingDateTime,
                Comparator.nullsLast(Comparator.naturalOrder())))
                 // Collect to an unmodifiable list
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                Collections::unmodifiableList));
    }

    /**
     * Finds all bookings scheduled for a specific date.
     *
     * @param date The date to query
     * @return An unmodifiable List of Bookings scheduled for that date.
     */
    @Override
    public List<Booking> findByDate(LocalDate date) {
        Objects.requireNonNull(date, "Date cannot be null for findByDate.");
        return bookings.values().stream()
                .filter(booking -> date.equals(booking.getBookingDate()))
                .sorted(Comparator.comparing(Booking::getBookingTime,
                Comparator.nullsLast(Comparator.naturalOrder())))
                // Collect to an unmodifiable list
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                Collections::unmodifiableList));
    }

    /**
     * Finds all bookings
     *
     * @return An unmodifiable List of all Bookings.
     */
    @Override
    public List<Booking> findAll() {
        // Create a new list for sorting
        List<Booking> sortedBookings = new ArrayList<>(bookings.values());
        sortedBookings.sort(Comparator.comparing(Booking::getBookingDateTime,
        Comparator.nullsLast(Comparator.naturalOrder())));
        return Collections.unmodifiableList(sortedBookings);
    }


    @Override
    public List<Booking> findByStatus(BookingStatus status) {
         Objects.requireNonNull(status,
         "Status cannot be null for findByStatus.");
         
         return bookings.values().stream()
                .filter(booking -> booking.getStatus() == status)
                .sorted(Comparator.comparing(Booking::getBookingDateTime,
                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                Collections::unmodifiableList));
    }

    @Override
    public List<Booking> findByTableAndDateTimeRange(int tableNumber,
    LocalDateTime startDateTime, LocalDateTime endDateTime) {
        
         Objects.requireNonNull(startDateTime,
         "StartDateTime cannot be null.");
         Objects.requireNonNull(endDateTime,
         "EndDateTime cannot be null.");
         if (tableNumber <= 0) {
            LOGGER.log(Level.FINER,
            "findByTableAndDateTimeRange called with non-positive table " +
            "number: {0}", tableNumber);
             return Collections.emptyList();
         }
         if (!endDateTime.isAfter(startDateTime)) {
             throw new IllegalArgumentException(
                "EndDateTime must be after startDateTime.");
         }

        return bookings.values().stream()
               .filter(b -> b.getTableNumber() == tableNumber)
               .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
               || b.getStatus() == BookingStatus.PENDING_APPROVAL)
               .filter(b -> {
                    LocalDateTime bookingStart = b.getBookingDateTime();
                    LocalDateTime bookingEnd =
                    bookingStart != null ? bookingStart.plusHours(1) : null;
                    if (bookingStart == null||bookingEnd == null) return false;

                    // Check for overlap:
                    return bookingStart.isBefore(endDateTime) &&
                    bookingEnd.isAfter(startDateTime);
               })
               .sorted(Comparator.comparing(Booking::getBookingDateTime,
               Comparator.nullsLast(Comparator.naturalOrder())))
               .collect(Collectors.collectingAndThen(Collectors.toList(),
               Collections::unmodifiableList));
    }


    /**
     * Loads booking data from the specified storage file.
     */
    @SuppressWarnings("unchecked")
    private synchronized void loadData() {
        File file = new File(storageFilePath);
        if (!file.exists() || file.length() == 0) {
             LOGGER.log(Level.INFO,
             "Booking data file not found or empty ({0}). Starting with an " +
             "empty repository.", storageFilePath);
             // Ensure directory exists for future saves
             ensureDirectoryExists(file);
             return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ObjectInputStream ois = new ObjectInputStream(bis)) {

            Object readObject = ois.readObject();
             if (readObject instanceof Map) {
                // Clear existing map before loading
                bookings.clear();
                Map<?,?> rawMap = (Map<?,?>) readObject;
                // Safely populate the map and checking types
                rawMap.forEach((key, value) -> {
                    if (key instanceof Integer && value instanceof Booking) {
                         bookings.put((Integer)key, (Booking)value);
                    } else {
                         LOGGER.log(Level.WARNING,
                         "Skipping invalid entry during load: Key type {0}, " +
                         "Value type {1}",
                         new Object[]{key != null ?
                            key.getClass().getName() : "null",
                            value != null ?
                            value.getClass().getName() : "null"});
                    }
                });
                LOGGER.log(Level.INFO,
                "Successfully loaded {0} booking entries from: {1}",
                new Object[]{bookings.size(), storageFilePath});
            } else {
                LOGGER.log(Level.SEVERE,
                "Booking data file ({0}) does not contain a valid Map.",
                storageFilePath);
                // Clear Storage
                bookings.clear();
            }
        } catch (FileNotFoundException e) {
             LOGGER.log(Level.SEVERE,
             "Booking data file not found for loading: " + storageFilePath, e);
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
             LOGGER.log(Level.SEVERE, "Failed to load booking data from " +
             "file (" + storageFilePath + "). Data might be corrupted or " +
             "class versions incompatible.", e);
             // clear data
             bookings.clear();
        }
    }

    /**
     * Saves the current state of the bookings map to the storage file.
     */
    private synchronized void saveData() {
        File file = new File(storageFilePath);
        // Ensure parent directory exists
        ensureDirectoryExists(file);

        // Overwrites existing file
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            // Save a snapshot copy of the map to avoid issues
            // if map is modified during serialisation
            oos.writeObject(new ConcurrentHashMap<>(bookings));
            LOGGER.log(Level.FINE,
            "Booking data saved successfully to {0}", storageFilePath);

        } catch (IOException e) {
            // Log critical error
            LOGGER.log(Level.SEVERE,
            "CRITICAL: Failed to save booking data to file (" + storageFilePath
            + "). Data loss may occur.", e);
        }
    }

    /**
     * Ensures the parent directory for the storage file exists
     * @param file The storage file.
     */
     private void ensureDirectoryExists(File file) {
        Objects.requireNonNull(file, "File cannot be null");
        File parentDir = file.getParentFile();
         if (parentDir != null && !parentDir.exists()) {
             LOGGER.log(Level.INFO,
             "Attempting to create directory for booking data: {0}",
             parentDir.getAbsolutePath());
             if (parentDir.mkdirs()) {
                 LOGGER.log(Level.INFO,
                 "Successfully created directory: {0}",
                 parentDir.getAbsolutePath());
             } else {
                 LOGGER.log(Level.SEVERE,
                 "Failed to create directory for booking data: {0}",
                 parentDir.getAbsolutePath());
                 throw new RuntimeException("Failed to create directory: "
                 + parentDir.getAbsolutePath());
             }
         }
     }
}