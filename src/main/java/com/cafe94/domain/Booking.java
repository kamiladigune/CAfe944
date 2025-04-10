// File: src/main/java/com/cafe94/domain/Booking.java
package com.cafe94.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import com.cafe94.enums.BookingStatus;

/**
 * Represents a customer booking for a table at Cafe94.
 * @author Adigun Lateef
 * @version 1.0
 */
public class Booking implements Serializable {

    // Fields
    private static final long serialVersionUID = 1L;
    private int bookingID;
    private final int customerID;
    private int tableNumber = 0;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private int numberOfGuests;
    private BookingStatus status;
    private final LocalDateTime creationTimestamp;


    /**
     * Constructs a new Booking request.
     *
     * @param bookingID      Unique ID.
     * @param customerID     ID of the customer.
     * @param bookingDate    Date of booking.
     * @param bookingTime    Time of booking.
     * @param numberOfGuests Number of guests.
     * @param initialStatus  Initial status.
     * @throws NullPointerException if bookingDate, bookingTime,
     * or initialStatus is null.
     * @throws IllegalArgumentException if customerID or numberOfGuests is not
     * positive.
     */
    public Booking(int bookingID, int customerID, LocalDate bookingDate,
                   LocalTime bookingTime, int numberOfGuests,
                   BookingStatus initialStatus, String notes) {

        // Validate required parameters immediately
        Objects.requireNonNull(bookingDate, "Booking date cannot be null.");
        Objects.requireNonNull(bookingTime, "Booking time cannot be null.");
        Objects.requireNonNull(initialStatus, "Initial booking status cannot "
        + "be null.");

        if (customerID <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive. "
            + "Provided: " + customerID);
        }
        if (numberOfGuests <= 0) {
            throw new IllegalArgumentException("Number of guests must be " +
            "positive. Provided: " + numberOfGuests);
        }

        // Assign fields
        this.bookingID = bookingID;
        this.customerID = customerID;
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
        this.numberOfGuests = numberOfGuests;
        this.status = initialStatus;
        this.creationTimestamp = LocalDateTime.now();
    }

    // Getters

    /**
     * @return The assigned booking number
    */
    public int getBookingID() {
        return bookingID;
    }
    /**
     * @return The corresponding customer ID
     */
    public int getCustomerID() {
        return customerID;
    }

    /**
     * @return The assigned table number, or 0 if no table is assigned yet.
     */
    public int getTableNumber() {
        return tableNumber;
    }

    /**
     * @return The booking date
     */
    public LocalDate getBookingDate() {
        return bookingDate;
    }

    /**
     * @return The booking time
     */
    public LocalTime getBookingTime() {
        return bookingTime;
    }

    /**
     * Convenience method to get the combined date and time of the booking.
     * @return LocalDateTime representation of the booking,
     * or null if date or time is null.
     */
    public LocalDateTime getBookingDateTime() {
        if (bookingDate == null || bookingTime == null) {
            return null;
        }
        return LocalDateTime.of(bookingDate, bookingTime);
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreationTimestamp() {
        return creationTimestamp;
    }

    // Setters

    /**
     * Sets the booking's persistent ID..
     *
     * @param bookingID The assigned persistent ID (must be positive).
     * @throws IllegalArgumentException if the provided bookingID is
     * not positive.
     * @throws IllegalStateException if attempting to change an already
     * assigned positive ID.
     */
    public void setBookingID(int bookingID) {
        if (bookingID <= 0) {
            throw new IllegalArgumentException("Assigned Booking ID must be " +
            "positive. Provided: " + bookingID);
        }
        if (this.bookingID > 0 && this.bookingID != bookingID) {
            // Prevent changing an already assigned ID
            System.err.println("Warning: Attempting to change an already " +
            "assigned booking ID from " + this.bookingID + " to " + bookingID);
            throw new IllegalStateException("Cannot change an already " +
            "assigned persistent booking ID.");
        }
        this.bookingID = bookingID;
    }

    /**
     * Assigns a table number to the booking.
     * @param tableNumber The number of the assigned table (must be positive).
     * @throws IllegalArgumentException if tableNumber is not positive.
     */
    public void assignTable(int tableNumber) {
        if (tableNumber <= 0) {
            throw new IllegalArgumentException("Assigned table number must " +
            "be positive. Provided: " + tableNumber);
        }
        if (this.tableNumber > 0 && this.tableNumber != tableNumber) {
             System.err.println("Warning: Changing assigned table for " +
             "booking ID " + this.bookingID + " from " + this.tableNumber +
             " to " + tableNumber);
        }
        this.tableNumber = tableNumber;
    }

    /**
     * Updates the status of the booking.
     * @param newStatus The new status (must not be null).
     */
    public void setStatus(BookingStatus newStatus) {
        Objects.requireNonNull(newStatus, "Booking status cannot be null.");
        // Consider logging the status change
        if (this.status != newStatus) {
             // Use proper logging in a real application
             System.out.println("Booking ID " + this.bookingID +
             " status changed from " + this.status + " to " + newStatus);
             this.status = newStatus;
        }
    }

    /**
     * Updates core details of the booking (date, time, guest count).
     *
     * @param newDate        The new date for the booking (non-null).
     * @param newTime        The new time for the booking (non-null).
     * @param newGuestCount  The new number of guests (must be positive).
     */
    public void updateDetails(LocalDate newDate, LocalTime newTime,
    int newGuestCount) {
        Objects.requireNonNull(newDate, "Booking date cannot be null.");
        Objects.requireNonNull(newTime, "Booking time cannot be null.");
        if (newGuestCount <= 0) {
            throw new IllegalArgumentException("Number of guests must be " +
            "positive. Provided: " + newGuestCount);
        }
        this.bookingDate = newDate;
        this.bookingTime = newTime;
        this.numberOfGuests = newGuestCount;
    }

    // Standard Methods
    /**
     * String representaion of the objects
     * @return a string reprentation of the booking objects
     */
    @Override
    public String toString() {
        return "Booking[ID=" + bookingID +
               ", CustID=" + customerID +
               ", DateTime=" + (getBookingDateTime() != null ?
               getBookingDateTime().toString() : "N/A") +
               ", Guests=" + numberOfGuests +
               ", Status=" + status +
               (tableNumber > 0 ? ", Table=" + tableNumber : "") +
               ']';
    }

    /**
     * Compares Booking objects for equality.
     * @param o The object to compare with.
     * @return true if the objects are considered equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;

        // If both have been persisted (positive ID), use ID for equality
        if (bookingID > 0 && booking.bookingID > 0) {
            return bookingID == booking.bookingID;
        }

        // Fallback for transient objects (compare key identifying fields)
        return customerID == booking.customerID &&
               numberOfGuests == booking.numberOfGuests &&
               Objects.equals(getBookingDateTime(),
               booking.getBookingDateTime()) && status == booking.status;
    }

    /**
     * Generates a hash code for the Booking object.
     * @return The hash code for this object.
     */
    @Override
    public int hashCode() {
        // Use ID if persisted (positive)
        if (bookingID > 0) {
            return Objects.hash(bookingID);
        }
        // Fallback hashcode for transient objects,
        // consistent with equals fallback
        return Objects.hash(customerID, getBookingDateTime(), numberOfGuests,
        status);
    }
}