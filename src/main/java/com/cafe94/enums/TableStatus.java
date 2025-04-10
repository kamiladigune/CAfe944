package com.cafe94.enums;

/**
 * Represents the different states a table can be in within the cafe.
 * Helps manage table availability and workflow.
 *@author Adigun Lateef
 * @version 1.1
 */
public enum TableStatus {
    /**
     * The table is available for customers.
     */
    AVAILABLE,

    /**
     * The table is currently in use by customers.
     */
    OCCUPIED,
    /**
     * The table has been booked but is not yet occupied.
     */
    RESERVED,
}