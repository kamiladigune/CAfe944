package com.cafe94.enums;

/**
 * Represents the possible states of an Order throughout its lifecycle.
 * @author  Adigun Lateef
 * @version 1.0
 */
public enum OrderStatus {
    PENDING_CONFIRMATION,
    CONFIRMED,
    PREPARING,
    READY,
    READY_FOR_DISPATCH,
    OUT_FOR_DELIVERY,
    DELIVERED,
    SERVED,
    COLLECTED,
    COMPLETED,
    CANCELLED;

    /** Checks if this status represents a final, non-active state.
     * @return  true if order is non-active, otherwise false
    */
    public boolean isFinalStatus() {
        return this == COMPLETED || this == CANCELLED || this == DELIVERED || this == COLLECTED || this == SERVED;
    }
}