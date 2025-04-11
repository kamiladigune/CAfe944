package com.cafe94.services;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.cafe94.domain.Booking;
import com.cafe94.domain.User;
import com.cafe94.enums.BookingStatus;

/**
 * Interface defining operations related to managing customer bookings
 * @author Adigun LAteef
 * @version 1.0
 */
public interface IBookingService {

    /**
     * Allows a customer request a new booking
     *
     * @param customerId     The ID of the customer making the request
     * @param date           The desired date for the booking
     * @param time           The desired time for the booking
     * @param numberOfGuests The number of guests for the booking
     * @param duration       The desired duration for the booking
     * @param notes          Optional notes from the customer
     * @return The newly created {@link Booking} object in
     * PENDING_APPROVAL status.
     * @throws IllegalArgumentException if inputs are invalid
     * @throws java.util.NoSuchElementException if customerId does not
     * correspond to an existing user.
     */
    // Corrected signature to include Duration
    Booking requestBooking(int customerId, int numberOfGuests,
    LocalDate date, LocalTime time, Duration duration, String notes);

    /**
     * Approves a pending booking request
     *
     * @param bookingId   The unique ID of the booking to approve
     * @param approver    The staff {@link User} approving the booking
     * @return {@code true} if the booking was successfully approved and
     * a table assigned {@code false} otherwise (e.g., booking not found,
     * not pending, no suitable table available).
     * @throws SecurityException if the approver lacks permission
     * @throws java.util.NoSuchElementException if no booking is found with the
     * given bookingId.
     * @throws IllegalStateException if the booking is not in PENDING_APPROVAL
     * status or if table assignment fails unexpectedly
     */
    boolean approveBooking(int bookingId, User approver);

     /**
     * Rejects a pending booking request
     *
     * @param bookingId   The unique ID of the booking to reject
     * @param staffMember The staff {@link User} rejecting the booking
     * @param reason      An optional reason for the rejection
     * @return {@code true} if the booking was successfully rejected,
     * {@code false} otherwise
     * @throws SecurityException if the staffMember lacks permission
     * @throws java.util.NoSuchElementException if no booking is found with the
     * given bookingId.
     * @throws IllegalStateException if the booking is not in
     * PENDING_APPROVAL status
     */
     
    boolean rejectBooking(int bookingId, User staffMember, String reason);


    /**
     * Cancels an existing booking
     *
     * @param bookingId   The unique ID of the booking to cancel
     * @param canceller   The {@link User} initiating the cancellation
     * @return {@code true} if the booking was successfully cancelled,
     * {@code false} otherwise
     * @throws SecurityException if the canceller lacks permission based on
     * role and booking status.
     * @throws java.util.NoSuchElementException if no booking is found with the
     * given bookingId.
     * @throws IllegalStateException if the booking is in a state that cannot
     * be cancelled
     */
    boolean cancelBooking(int bookingId, User canceller);

    /**
     * Retrieves a specific booking by its unique persistent ID
     *
     * @param bookingId The unique ID of the booking to retrieve
     * @return An {@code Optional<Booking>} containing the Booking if found
     * and accessible, otherwise empty.
     */
    Optional<Booking> getBookingById(int bookingId);

    /**
     * Retrieves all bookings associated with a specific customer
     * @param customerId The unique ID of the customer whose bookings are to be
     * retrieved
     * @return A {@code List<Booking>} containing the customer's bookings,
     * potentially sorted chronologically.
     * Returns an empty list if the customer ID is invalid or
     * the customer has no bookings.
     * @throws java.util.NoSuchElementException if customerId does not
     * correspond to an existing user
     */
    List<Booking> getCustomerBookings(int customerId);

    /**
     * Retrieves all bookings scheduled for a specific date
     *
     * @param date The {@link LocalDate} for which to retrieve bookings
     * @return A {@code List<Booking>} containing bookings for the specified
     * date, potentially sorted by time.
     * Returns an empty list if the date is null or no bookings
     * exist for that date.
     */
    List<Booking> getBookingsByDate(LocalDate date);

    /**
     * Finds all bookings currently matching the specified status.
     * Results are potentially sorted (e.g., chronologically).
     *
     * @param status The {@link BookingStatus} to filter by (non-null).
     * @return A {@code List<Booking>} containing all bookings with the
     * specified status. Returns an empty list if the status is null or
     * no bookings match.
     * @throws NullPointerException if the status parameter is null.
     */
    List<Booking> getBookingByStatus(BookingStatus status);
}