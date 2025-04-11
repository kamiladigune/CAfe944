// File: src/main/java/com/cafe94/domain/Takeaway.java
package com.cafe94.domain;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cafe94.enums.OrderStatus;

/**
 * Represents an order placed by a customer for collection (Takeaway).
 * Extends the base {@link Order} class
 * @author Adigun Lateef
 * @version 1.0
 */
public class Takeaway extends Order {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER =
    Logger.getLogger(Takeaway.class.getName());
    private final LocalTime pickupTime;

    /**
     * Constructs a new Takeaway order.
     *
     * @param orderID       Unique ID or existing ID.
     * @param items         List of items ordered
     * @param customerID    ID of the customer placing the order
     * @param initialStatus The starting status for the order
     * @param pickupTime    The requested time for pickup
     * @throws NullPointerException if items, initialStatus, or pickupTime
     * is null.
     * @throws IllegalArgumentException if items list is empty or customerID
     * is invalid
     */
    public Takeaway(int orderID, List<Item> items, int customerID,
                    OrderStatus initialStatus, LocalTime pickupTime) {
        // Calls base constructor first
        super(orderID, items, customerID, initialStatus);

        // Validate and set the final pickupTime field
        this.pickupTime = Objects.requireNonNull(pickupTime,
        "Pickup time cannot be null for a takeaway order.");
    }

    // Getters
    /**
     * @return The pickup time requested for the takeaway order.
     */
    public LocalTime getPickupTime() {
        return pickupTime;
    }

    // Setters

   
    /**
     * Marks the takeaway order as collected by the customer
     * and completes the order.
     * @throws IllegalStateException if the order is not currently READY.
     */

    public void markAsCollected() {
        if (this.getStatus() == OrderStatus.READY) {
             LOGGER.log(Level.FINE, "Marking Takeaway order {0} as COLLECTED.",
             this.getOrderID());
            // Set status to collected
            updateStatusInternal(OrderStatus.COLLECTED);
            try {
                this.completeOrder();
            } catch (IllegalStateException e) {
                // Log if already completed
                 LOGGER.log(Level.FINE,
                 "Order {0} was already completed when marked collected.",
                 this.getOrderID());
            }
        } else {
            throw new IllegalStateException("Takeaway Order cannot be " +
            "marked as collected from status: " + this.getStatus() +
            ". Must be READY.");
        }
    }

    // Standard Methods

    /** String representaion of the objects
     * @return a string reprentation of the Takeaway objects
     */
    @Override
    public String toString() {
        return super.toString().replaceFirst("]$", "")
               + ", PickupTime=" + pickupTime + ']';
    }

    /**
     * Compares Takeaway objects for equality.
     * @param o The object to compare with.
     * @return true if the objects are considered equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        // Cast 'o' to Takeaway and compare the pickup time
        Takeaway that = (Takeaway) o;
        return Objects.equals(pickupTime, that.pickupTime);
    }

    /**
     * Generates a hash code for the Takeaway object.
     * @return The hash code for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), pickupTime);
    }

}