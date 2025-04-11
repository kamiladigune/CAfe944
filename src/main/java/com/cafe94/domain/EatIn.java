package com.cafe94.domain;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cafe94.enums.OrderStatus;


/**
 * Represents an order placed by a customer dining within the restaurant (Eat-In).
 * Extends the base {@link Order} class.
 * @author  Adigun Lateef
 * @version 1.0
 */
public class EatIn extends Order {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(EatIn.class.getName());
    private int tableNumber;

    /**
     * Constructs a new EatIn order.
     *
     * @param orderID       Unique ID or existing ID.
     * @param items         List of items ordered.
     * @param customerID    ID of the customer placing the order.
     * @param tableNumber   The table number associated with the order.
     * @param initialStatus The starting status for the order
     * @throws IllegalArgumentException if tableNumber is not positive,
     * or if inherited validation fails.
     * @throws NullPointerException if items or initialStatus is null.
     */
    public EatIn(int orderID, List<Item> items, int customerID,
                 int tableNumber, OrderStatus initialStatus) {
        // Calls base constructor first to validate common fields
        super(orderID, items, customerID, initialStatus);

        // Validate table number specific to EatIn orders
        if (tableNumber <= 0) {
            throw new IllegalArgumentException("Eat-in order must have a " +
            "valid positive table number. Provided: " + tableNumber);
        }
        this.tableNumber = tableNumber;
    }

    // Getters

    /**
     * @return The table number associated with eat-in order.
     */
    public int getTableNumber() {
        return tableNumber;
    }

    // Setters

    /**
     * Sets or updates the table number associated with this order.
     * @param tableNumber The new table number.
     * @throws IllegalArgumentException if tableNumber is not positive.
     */
    public void setTableNumber(int tableNumber) {
         if (tableNumber <= 0) {
            throw new IllegalArgumentException("Table number must be " +
            "positive. Provided: " + tableNumber);
        }
        if (this.tableNumber != tableNumber) {
             LOGGER.log(Level.INFO, "Changing table number for EatIn " +
             "order ID {0} from {1} to {2}",
             new Object[]{this.getOrderID(), this.tableNumber, tableNumber});
            this.tableNumber = tableNumber;
        }
    }

    /**
     * Marks the eat-in order as served to the customer at the table..
     * @throws IllegalStateException if the order is not currently READY.
     */
    public void markAsServed() {
        if (this.getStatus() == OrderStatus.READY) {
            LOGGER.log(Level.FINE, "Marking EatIn order {0} as SERVED.",
            this.getOrderID());
            updateStatusInternal(OrderStatus.SERVED);
        } else {
            throw new IllegalStateException("EatIn Order cannot be marked " +
            "as served from status: " + this.getStatus() + ". Must be READY.");
        }
   }

    // Standard Methods
   /**
     * String representaion of the objects
     * @return a string reprentation of the EatIn order objects
     */
    @Override
    public String toString() {
        // Append table number to the base Order string representation
        return super.toString().replaceFirst("]$", "")
               + ", Table=" + tableNumber + ']';
    }

    /**
     * Compares EatIn objects for equality.
     * @param o The object to compare with.
     * @return true if the objects are considered equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        // Cast 'o' to EatIn and compare the table number
        EatIn that = (EatIn) o;
        return tableNumber == that.tableNumber;
    }

    /**
     * Generates a hash code for the EatIn object.
     * @return The hash code for this object.
     */
    @Override
    public int hashCode() {
        // Combine hash code from superclass with table number
        return Objects.hash(super.hashCode(), tableNumber);
    }
}