package com.cafe94.enums;

/**
 * Defines specific actions or permissions within the system.
 * These are used to control access for different user roles.
 *
 * @version 1.1
 */
public enum Permission {
    // User Management
    CREATE_USER,
    READ_USER,
    UPDATE_USER,
    DELETE_USER,

    // Menu Management
    CREATE_MENU_ITEM,
    READ_MENU_ITEM,
    UPDATE_MENU_ITEM,
    DELETE_MENU_ITEM,

    // Order Management
    CREATE_ORDER,
    READ_ORDER,
    UPDATE_ORDER_STATUS,
    CANCEL_ORDER,
    PROCESS_PAYMENT,

    // Booking Management
    CREATE_BOOKING,
    READ_BOOKING,
    UPDATE_BOOKING_STATUS,
    DELETE_BOOKING,

    // Table Management
    READ_TABLE_STATUS,
    UPDATE_TABLE_STATUS,

    // Reporting
    GENERATE_REPORTS,

    // Staff Specific Actions
    ASSIGN_DELIVERY,
    MARK_DELIVERY_COMPLETE,

    // Customer Specific Actions
    VIEW_ORDER_HISTORY,
    PLACE_ORDER,
    MAKE_BOOKING
}