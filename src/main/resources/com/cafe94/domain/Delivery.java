// File: src/main/java/com/cafe94/domain/Delivery.java
package com.cafe94.domain;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cafe94.enums.OrderStatus;
import com.cafe94.util.ValidationUtils;

/**
 * Represents an order placed for delivery to a customer's address.
 * Extends the base {@link Order} class.
 * @author Adigun Lateef
 * @version 1.0
 */
public class Delivery extends Order {

    private static final long serialVersionUID = 1L;

    // Logger for this class
    private static final Logger LOGGER =
    Logger.getLogger(Delivery.class.getName());
    private final String deliveryAddress;
    private LocalTime estimatedDeliveryTime;
    private int assignedDriverID = 0;

    /**
     * Constructs a new Delivery order.
     *
     * @param orderID         Unique ID or existing ID.
     * @param items           List of items ordered.
     * @param customerID      ID of the customer placing the order.
     * @param deliveryAddress Delivery address.
     * @param initialStatus   The starting status for the order.
     * @throws NullPointerException if items, initialStatus, or
     * deliveryAddress is null.
     * @throws IllegalArgumentException if items list is empty,
     * customerID is not positive, or deliveryAddress is blank.
     */
    public Delivery(int orderID, List<Item> items, int customerID,
                    String deliveryAddress, OrderStatus initialStatus) {
        // Call base constructor to validate common fields
        super(orderID, items, customerID, initialStatus);

        // Validate and set the final deliveryAddress field
        this.deliveryAddress = ValidationUtils.requireNonBlank(deliveryAddress,
        "Delivery address");

    }

    // Getters

    /**
     * @return The delivery address for this order.
     */
    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    /**
     * @return The estimated delivery time, or null if not set.
     */
    public LocalTime getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    /**
     * @return The ID of the assigned driver.
     */
    public int getAssignedDriverID() {
        return assignedDriverID;
    }

    /**
     * Sets or updates the estimated delivery time for delivery order.
     * Also updates the last updated timestamp of the order.
     * @param estimatedDeliveryTime The estimated time.
     */
    public void setEstimatedDeliveryTime(LocalTime estimatedDeliveryTime) {
        // Log if the time is changing
        if (!Objects.equals(this.estimatedDeliveryTime,
        estimatedDeliveryTime)) {
            LOGGER.log(Level.FINE, "Updating estimated delivery " +
            " time for order ID {0} to {1}",
                       new Object[]{this.getOrderID(), estimatedDeliveryTime});
            this.estimatedDeliveryTime = estimatedDeliveryTime;
            // Update lastUpdatedTimestamp
            this.lastUpdatedTimestamp = LocalDateTime.now();
        }
    }

    /**
     * Assigns a driver to delivery order
     * @param driverID The user ID of the assigned driver.
     * @throws IllegalArgumentException if driverID is not positive.
     */
    public void assignDriver(int driverID) {
        if (driverID <= 0) {
            throw new IllegalArgumentException("Assigned Driver User ID " +
            "must be positive. Provided: " + driverID);
        }
        if (this.assignedDriverID != driverID) {
             LOGGER.log(Level.INFO, "Assigning driver ID {0} to Delivery " +
             "order ID {1}", new Object[]{driverID, this.getOrderID()});
            this.assignedDriverID = driverID;
            // Update timestamp
            this.lastUpdatedTimestamp = LocalDateTime.now();
        } else {
             LOGGER.log(Level.FINE, "Driver ID {0} is already assigned to " +
             "Delivery order ID {1}. No change.",
             new Object[]{driverID, this.getOrderID()});
        }
    }

     /**
     * Marks the delivery order as ready for driver assignment.
     * @throws IllegalStateException if the order is not in a valid state
     * (e.g., CONFIRMED, READY).
     */
    public void markReadyForDispatch() {
        if (this.getStatus() == OrderStatus.CONFIRMED || this.getStatus() ==
        OrderStatus.READY) {
            updateStatusInternal(OrderStatus.READY_FOR_DISPATCH);
        } else {
            throw new IllegalStateException("Order cannot be marked ready " +
            "for dispatch from status: " + this.getStatus());
        }
   }

   /**
    * Marks the delivery order as being out for delivery.
    * @throws IllegalStateException if the order is not in a
    valid state for dispatch.
    */

   public void markAsOutForDelivery() {
       if (this.getStatus() == OrderStatus.READY_FOR_DISPATCH
       || this.getStatus() == OrderStatus.READY) {
        updateStatusInternal(OrderStatus.OUT_FOR_DELIVERY);
       } else {
           throw new IllegalStateException("Order cannot be marked out for " +
           "delivery from status: " + this.getStatus());
       }
   }
      /**
     * Marks the delivery order as delivered and potentially
     * completes the order.
     * @throws IllegalStateException if the order is not
     * currently OUT_FOR_DELIVERY.
     */
    public void markAsDelivered() {
        if (this.getStatus() == OrderStatus.OUT_FOR_DELIVERY) {
            LOGGER.log(Level.FINE, "Marking Delivery order {0} as DELIVERED.",
            this.getOrderID());
            // Set status to DELIVERED first
            updateStatusInternal(OrderStatus.DELIVERED);
            try {
                this.completeOrder();
            } catch (IllegalStateException e) {
                 // This is okay if already completed, log and continue
                 LOGGER.log(Level.FINE, "Order {0} was already completed " +
                 "when marked delivered.", this.getOrderID());
            }
        } else {
            throw new IllegalStateException("Order cannot be marked as " +
            "delivered from status: " + this.getStatus() +
            ". Must be OUT_FOR_DELIVERY.");
        }
    }

    /**
     * String representaion of the objects
     * @return a string reprentation of the delivery objects
     */
    @Override
    public String toString() {
        
        return super.toString().replaceFirst("]$", "")
               + ", Address='" + deliveryAddress + '\''
               + ", DriverID=" + (assignedDriverID > 0 ? assignedDriverID :
               "Unassigned") + (estimatedDeliveryTime != null ? ", EstTime=" +
               estimatedDeliveryTime : "") + ']';
    }

    /**
     * Compares Delivery objects for equality.
     * @param o The object to compare with.
     * @return true if the objects are considered equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        // Cast 'o' to Delivery and compare specific fields
        Delivery that = (Delivery) o;
        return assignedDriverID == that.assignedDriverID &&
               Objects.equals(deliveryAddress, that.deliveryAddress) &&
               Objects.equals(estimatedDeliveryTime,
               that.estimatedDeliveryTime);
    }

    /**
     * Generates a hash code for the Delivery object.
     * @return The hash code for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), deliveryAddress,
        estimatedDeliveryTime, assignedDriverID);
    }

}