// File: src/main/java/com/cafe94/enums/UserRole.java
package com.cafe94.enums;

/**
 * Defines the distinct roles a User can assume within the Cafe94 system.
 * @author Adigun Lateef
 * @version 1.0
 */
public enum UserRole {
    MANAGER,
    CHEF,
    WAITER,
    DRIVER,
    CUSTOMER;

    /** Checks if this role is a staff role.
     * @return true if the role is correct, otherwise false
    */
    public boolean isStaffRole() {
        return this == MANAGER || this == CHEF || this == WAITER || this == DRIVER;
    }
}
