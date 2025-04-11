// File: src/main/java/com/cafe94/domain/Order.java
package com.cafe94.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cafe94.enums.OrderStatus;

/**
 * Abstract base class representing an order placed at Cafe94.
 * @author Adigun Lateef
 * @version 1.0
 */
public abstract class Order implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(Order.class.getName());
    private int orderID;
    private List<Item> items;
    private final int customerID;
    private OrderStatus status;
    private final LocalDateTime orderTimestamp;
    protected LocalDateTime lastUpdatedTimestamp;
    private double totalPrice;

    /**
     * Protected constructor for use by subclasses. Validates input.
     *
     * @param orderID       Unique ID or existing ID.
     * @param items         List of items
     * @param customerID    ID of the customer placing the order
     * @param initialStatus The starting status of the order
     * @throws NullPointerException if items list or initialStatus is null.
     * @throws IllegalArgumentException if items list is empty or customerID is
     * not positive.
     */
    protected Order(int orderID, List<Item> items, int customerID,
    OrderStatus initialStatus) {
        // Validate required parameters
        Objects.requireNonNull(items, "Order items list cannot be null.");
        Objects.requireNonNull(initialStatus, "Initial order status " +
        "cannot be null.");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least " +
            "one item.");
        }
        if (customerID <= 0) {
            throw new IllegalArgumentException("Valid Customer ID is " +
            "required for an order. Provided: " + customerID);
        }

        // Assign fields
        this.orderID = orderID;
        this.items = new ArrayList<>(items);
        this.customerID = customerID;
        this.status = initialStatus;
        this.orderTimestamp = LocalDateTime.now();
        this.lastUpdatedTimestamp = this.orderTimestamp;
        this.totalPrice = calculateTotalPrice();
    }

    /**
     * Calculates the total price based on the current items in the order.
     * @return The sum of the prices of all non-null items.
     */
    private double calculateTotalPrice() {
        if (this.items == null) {
            return 0.0;
        }
        return this.items.stream()
                .filter(Objects::nonNull)
                .mapToDouble(Item::getPrice)
                .sum();
    }

    // Getters

    /**
     * @return  The order ID
     */
    public int getOrderID() {
        return orderID;
    }

    /**
     * Returns an unmodifiable view of the items currently in the order.
     * @return An unmodifiable list of Items.
     */
    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * @return The corresponding customer ID
     */
    public int getCustomerID() {
        return customerID;
    }

    /**
     * @return The order status
     */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * @return The order timestamp
     */
    public LocalDateTime getOrderTimestamp() {
        return orderTimestamp;
    }

    /**
     * @return The last updated timestamp for the order
     */
    public LocalDateTime getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    /**
     * Gets the pre-calculated total price of the order.
     * @return The total price.
     */
    public double getTotalPrice() {
        return totalPrice;
    }

    // Setters

    /**
     * Sets the order's persistent ID.
     *
     * @param orderID The assigned persistent ID (must be positive).
     * @throws IllegalArgumentException if the provided orderID is not positive
     * @throws IllegalStateException if attempting to change an already
     * assigned positive ID.
     */
    protected void setOrderID(int orderID) {
        if (orderID <= 0) {
            throw new IllegalArgumentException("Assigned Order ID must be " +
            "positive. Provided: " + orderID);
        }
        if (this.orderID > 0 && this.orderID != orderID) {
            // Prevent changing an already assigned ID
            LOGGER.log(Level.WARNING, "Attempting to change an already " +
            "assigned order ID from {0} to {1}",
            new Object[]{this.orderID, orderID});
            throw new IllegalStateException("Cannot change an already " +
            "assigned persistent order ID.");
        }
        this.orderID = orderID;
    }

    /**
     * Updates the status of the order internally. Only intended for use by
     * state transition methods.
     * @param newStatus The new status
     * @throws NullPointerException if newStatus is null.
     */
    protected void setStatus(OrderStatus newStatus) {
        Objects.requireNonNull(newStatus, "New order status cannot be null.");
        this.status = newStatus;
    }

    /**
     * Replaces the items in the order, recalculates the total price, and
     * updates the last updated timestamp.
     *
     * @param newItems New list of items
     * @throws NullPointerException if newItems is null.
     * @throws IllegalArgumentException if newItems is empty.
     */
    public void updateItems(List<Item> newItems) {
        Objects.requireNonNull(newItems, "Order items list cannot be null " +
        "when updating.");
        if (newItems.isEmpty()) {
            throw new IllegalArgumentException("Cannot update order to have " +
            "no items.");
        }

        this.items = new ArrayList<>(newItems);
        // Recalculate price
        this.totalPrice = calculateTotalPrice();
        // Update timestamp
        this.lastUpdatedTimestamp = LocalDateTime.now();

        LOGGER.log(Level.INFO, "Items updated for order ID: {0}. New total " +
        "price: {1,number,#.##}", new Object[]{orderID, totalPrice});
    }

    /**
     * Marks the order as confirmed
     * @throws IllegalStateException if order is not in
     * PENDING_CONFIRMATION state.
     */
    public void confirmOrder() {
        if (this.status == OrderStatus.PENDING_CONFIRMATION) {
            updateStatusInternal(OrderStatus.CONFIRMED);
        } else {
            LOGGER.log(Level.WARNING, "Cannot confirm order {0}: Invalid " +
            "current state {1}", new Object[]{orderID, this.status});
            throw new IllegalStateException("Order cannot be confirmed from " +
            "status: " + this.status);
        }
    }

    /**
     * Marks the order as being prepared by the kitchen.
     * @throws IllegalStateException if order is not in CONFIRMED state.
     */
    public void startPreparation() {
        // update status to PREPARING if order is CONFIRMED
         if (this.status == OrderStatus.CONFIRMED) {
            updateStatusInternal(OrderStatus.PREPARING);
        } else {
             LOGGER.log(Level.WARNING, "Cannot start preparation for order " +
             "{0}: Invalid current state {1}",
             new Object[]{orderID, this.status});
            throw new IllegalStateException("Order preparation cannot start " +
            "from status: " + this.status);
        }
    }

    /**
     * Marks the order as ready
     * @throws IllegalStateException if order is not in PREPARING state.
     */
    public void markAsReady() {
        if (this.status == OrderStatus.PREPARING) {
            updateStatusInternal(OrderStatus.READY);
        } else {
            LOGGER.log(Level.WARNING, "Cannot mark order {0} as ready: " +
            "Invalid current state {1}", new Object[]{orderID, this.status});
            throw new IllegalStateException("Order cannot be marked as " +
            "ready from status: " + this.status);
        }
    }

    /**
     * Marks the order as completed
     * @throws IllegalStateException if order is already completed/cancelled
     * or in an invalid state for completion.
     */
    public void completeOrder() {
        
        List<OrderStatus> completableStates = Arrays.asList(
            OrderStatus.READY,
            OrderStatus.SERVED,
            OrderStatus.COLLECTED,
            OrderStatus.DELIVERED
        );

        if (completableStates.contains(this.status)) {
            updateStatusInternal(OrderStatus.COMPLETED);
        } else if (this.status == OrderStatus.COMPLETED) {
            LOGGER.log(Level.FINE, "Order {0} is already completed.", orderID);
        } else {
             LOGGER.log(Level.WARNING, "Cannot complete order {0}: Invalid " +
             "current state {1}", new Object[]{orderID, this.status});
            throw new IllegalStateException("Order cannot be completed from " +
            "status: " + this.status);
        }
    }

    /**
     * Cancels the order. Checks if cancellation is allowed based on the
     * current status.
     * @throws IllegalStateException if order cannot be cancelled from its
     * current state according to domain rules.
     */
    public void cancelOrder() {
        List<OrderStatus> cancellableStates = Arrays.asList(
            OrderStatus.PENDING_CONFIRMATION,
            OrderStatus.CONFIRMED
        );

        if (cancellableStates.contains(this.status)) {
            updateStatusInternal(OrderStatus.CANCELLED);
        } else if (this.status == OrderStatus.CANCELLED) {
            // Already cancelled, do nothing but log it
            LOGGER.log(Level.FINE, "Order {0} is already cancelled.", orderID);
        } else {
            LOGGER.log(Level.WARNING, "Cannot cancel order {0} directly: " +
            "Invalid current state {1}", new Object[]{orderID, this.status});
            throw new IllegalStateException("Order cannot be cancelled from " +
            "status: " + this.status + " through this method.");
        }
    }

    /**
     * Helper to update status and the last updated timestamp consistently,
     * and logs the status change.
     * @param newStatus The new status to set
     */
    protected void updateStatusInternal(OrderStatus newStatus) {
        Objects.requireNonNull(newStatus, "New status cannot be null for " +
        "internal update.");
        if (this.status != newStatus) {
            LOGGER.log(Level.INFO, "Order {0}: Status changing from {1} " +
            "to {2}", new Object[]{this.orderID, this.status, newStatus});
            this.status = newStatus;
            this.lastUpdatedTimestamp = LocalDateTime.now();
        } else {
             LOGGER.log(Level.FINE, "Order {0}: Status update called with " +
             "same status ({1}). No change.",
                        new Object[]{this.orderID, newStatus});
        }
    }

    // Standard Methods

    /**
     * String representaion of the objects
     * @return a string reprentation of the Order objects
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
               "ID=" + orderID +
               ", CustID=" + customerID +
               ", Status=" + status +
               ", Items=" + (items != null ? items.size() : 0) +
               ", Total=" + String.format("%.2f", totalPrice) +
               ", Placed=" + (orderTimestamp != null ?
               orderTimestamp.toLocalDate() + "T" +
               orderTimestamp.toLocalTime() : "N/A") + ']';
    }

    /**
     * Compares Order objects for equality.
     * @param o The object to compare with.
     * @return true if the objects are considered equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        if (orderID > 0 && order.orderID > 0) {
            return orderID == order.orderID;
        }

        // Fallback for transient objects
        return customerID == order.customerID &&
               Objects.equals(orderTimestamp, order.orderTimestamp);
    }

    /**
     * Generates a hash code for the Order object.
     * @return The hash code for this object.
     */
    @Override
    public int hashCode() {
        if (orderID > 0) {
            return Objects.hash(orderID);
        }
        // Fallback hashcode for transient objects
        return Objects.hash(customerID, orderTimestamp);
    }
}