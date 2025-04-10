// File: src/main/java/com/cafe94/persistence/IBookingRepository.java
package com.cafe94.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.cafe94.domain.Booking;
import com.cafe94.enums.BookingStatus;

/**
 * Interface defining persistence operations for {@link Booking} entities.
 * @author Adigun Lateef
 * @version 1.0
 */
public interface IBookingRepository {

    /**
     * Saves a new booking or updates an existing one based on its ID.
     *
     * @param booking The Booking entity to save or update
     * @return The saved or updated Booking entity
     * @throws NullPointerException if the booking parameter is null.
     */
    Booking save(Booking booking);

    /**
     * Finds a booking by its unique persistent identifier (ID).
     *
     * @param bookingId The unique ID of the booking to find
     * @return An {@code Optional<Booking>} containing the found
     * booking if one with the specified ID exists, otherwise an empty Optional
     */
    Optional<Booking> findById(int bookingId);

    /**
     * Finds all bookings currently matching the specified status.
     *
     * @param status The {@link BookingStatus}
     * @return A {@code List<Booking>} containing all bookings with the
     * specified status and returns an empty list if the status is null or no
     * bookings match.
     * @throws NullPointerException if the status parameter is null
     */
    List<Booking> findByStatus(BookingStatus status);

    /**
     * Finds all bookings made by a specific customer,
     * identified by their unique ID.
     *
     * @param customerId The unique ID of the customer
     * @return A {@code List<Booking>} containing all bookings made by the
     * specified customer an returns an empty list if the customer has made
     * no bookings or the ID is invalid.
     */
    List<Booking> findByCustomerId(int customerId);

    /**
     * Finds all bookings scheduled for a specific date
     *
     * @param date The {@link LocalDate} to query
     * @return A {@code List<Booking>} containing all bookings scheduled
     * for the specified date and returns an empty list if the date is null or
     * no bookings exist for that date.
     * @throws NullPointerException if the date parameter is null
     */
    List<Booking> findByDate(LocalDate date);

    /**
     * Finds all bookings assigned to a specific table that overlap with the
     * given date and time range
     * @param tableNumber   The table number to check
     * @param startDateTime The start of the time range
     * @param endDateTime   The end of the time range
     * @return A {@code List<Booking>} containing overlapping for the
     * specified table and time range and returns an empty list if no overlaps
     * are found or if parameters are invalid.
     * @throws NullPointerException if startDateTime or endDateTime are null.
     * @throws IllegalArgumentException if endDateTime is not after
     * startDateTime.
     */
    List<Booking> findByTableAndDateTimeRange(int tableNumber,
    LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * Retrieves all bookings from the repository
     * @return A {@code List<Booking>} containing all bookings and returns an
     * empty list if none exist.
     */
    List<Booking> findAll();

    /**
     * Deletes a booking by its unique persistent ID.
     *
     * @param bookingId The unique ID of the booking to delete
     * @return {@code true} if a booking with the specified ID was found
     * and successfully deleted, {@code false} otherwise
     */
    boolean deleteById(int bookingId);
}