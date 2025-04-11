package com.cafe94.enums;

/**
 * Represents the possible states of a customer table booking.
 * @author Adigun Lateef
 * @version 1.0
 */
public enum BookingStatus {
    PENDING_APPROVAL,
    CONFIRMED,
    REJECTED,
    CANCELLED_BY_CUSTOMER,
    CANCELLED_BY_STAFF,
    COMPLETED,
    NO_SHOW;
}