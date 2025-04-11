package com.cafe94.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cafe94.enums.TableStatus;

/**
 * Represents a dining table within the Cafe94 restaurant.
 * @author  Adigun Lateef
 * @version 1.0
 */
public class Table implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER =
    Logger.getLogger(Table.class.getName());
    private final int tableNumber;
    private final int capacity;
    private TableStatus status;

    /**
     * Constructs a new Table object.
     */
    public Table(int tableNumber, int capacity, TableStatus initialStatus) {
        if (tableNumber <= 0) {
            throw new IllegalArgumentException("Table number must be " +
            "positive. Provided: " + tableNumber);
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Table capacity must be " +
            "positive. Provided: " + capacity);
        }
        this.status = Objects.requireNonNull(initialStatus,
        "Initial table status cannot be null.");
        this.tableNumber = tableNumber;
        this.capacity = capacity;
        LOGGER.log(Level.CONFIG,
        "Created Table object: Num={0}, Cap={1}, Status={2}",
        new Object[]{this.tableNumber, this.capacity, this.status});
    }

    // Getters

    /**
     * @return The table number
     */
    public int getTableNumber() {
        return tableNumber;
    }

    /**
     * @return The table capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * @return The table status
     */
    public TableStatus getStatus() {
        return status;
    }

    // Setter

    /**
     * Sets the status internally
     * @param newStatus The new status to set.
     */
    protected void setStatusInternal(TableStatus newStatus) {
        Objects.requireNonNull(newStatus, "New table status cannot be null.");
        if (this.status != newStatus) {
            LOGGER.log(Level.INFO, "Table {0}: Status changing from {1} to " +
            "{2}", new Object[]{this.tableNumber, this.status, newStatus});
            this.status = newStatus;
        } else {
             LOGGER.log(Level.FINEST, "Table {0}: setStatusInternal called " +
             "with same status ({1}). No change.",
             new Object[]{this.tableNumber, newStatus});
        }
    }

    //  Public State Transition Methods

    /**
     * Marks the table as OCCUPIED.
     * @throws IllegalStateException if the table is not currently AVAILABLE.
     */
    public void occupy() {
        if (this.status == TableStatus.AVAILABLE) {
            setStatusInternal(TableStatus.OCCUPIED);
        } else {
            throw new IllegalStateException("Table " + tableNumber +
            " cannot be occupied from status: " + this.status +
            ". Must be AVAILABLE.");
        }
    }

    /**
     * Marks the table as RESERVED.
     * @throws IllegalStateException if the table is not currently AVAILABLE.
     */
    public void reserve() {
        if (this.status == TableStatus.AVAILABLE) {
             setStatusInternal(TableStatus.RESERVED);
        } else {
             throw new IllegalStateException("Table " + tableNumber +
             " cannot be reserved from status: " + this.status +
             ". Must be AVAILABLE.");
        }
    }

    /**
     * Marks the table as AVAILABLE.
     */
    public void makeAvailable() {
         if (null == this.status) {
             LOGGER.log(Level.WARNING, "Unexpected state {0} encountered " +
             "when trying to make table {1} available.",
             new Object[]{this.status, this.tableNumber});
             throw new IllegalStateException("Cannot make table available " +
             "from status: " + this.status);
         } else switch (this.status) {
            case OCCUPIED:
            case RESERVED:
                setStatusInternal(TableStatus.AVAILABLE);
                break;
            case AVAILABLE:
                LOGGER.log(Level.FINE, "Table {0} is already AVAILABLE.",
                this.tableNumber);
                // Do nothing if available
                break;
            default:
                LOGGER.log(Level.WARNING, "Unexpected state {0} encountered " +
                "when trying to make table {1} available.",
                new Object[]{this.status, this.tableNumber});
                throw new IllegalStateException("Cannot make table available from status: " + this.status);
        }
    }


    // Standard Methods

    /** String representaion of the objects
     * @return a string reprentation of the Table objects
     */
    @Override
    public String toString() {
        return "Table[Num=" + tableNumber +
               ", Cap=" + capacity +
               ", Status=" + status +
               ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        return tableNumber == table.tableNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableNumber);
    }
}