package com.cafe94.enums;

/**
 * Defines specific actions or permissions within the system.
 * These are used to control access for different user roles.
 *@author Adigun Lateef
 * @version 1.2
 */
public enum Permission {
    // User Management
    CREATE_USER,
    READ_USER,
    UPDATE_USER,
    DELETE_USER,


    // Profile Management
    UPDATE_OWN_PROFILE,

    // Staff Management
    MANAGE_STAFF,

    // Menu Viewing/Management
    VIEW_MENU,
    MANAGE_MENU_ITEMS,
    SET_DAILY_SPECIAL,

    // Order Placement/Management
    PLACE_ORDER,
    TAKE_EAT_IN_ORDER,
    VIEW_ORDERS,
    TRACK_OWN_ORDER,
    APPROVE_DELIVERY,
    ASSIGN_DRIVER,
    CANCEL_OWN_ORDER,
    CANCEL_ANY_ORDER,
    // Order Status Updates
    UPDATE_ORDER_STATUS_PREPARING,
    UPDATE_ORDER_STATUS_COMPLETED,
    UPDATE_ORDER_STATUS_READY,
    UPDATE_ORDER_STATUS_SERVED,
    UPDATE_ORDER_STATUS_COLLECTED,
    UPDATE_ORDER_STATUS_OUT_FOR_DELIVERY,
    UPDATE_ORDER_STATUS_DELIVERED,

    // Booking Management
    REQUEST_BOOKING,
    APPROVE_BOOKING,
    REJECT_BOOKING,
    ASSIGN_TABLE_BOOKING,
    CANCEL_OWN_BOOKING,
    CANCEL_ANY_BOOKING,

    // Table Management
    MANAGE_TABLE_STATUS, // Waiter/Manager
    VIEW_OWN_PROFILE,
    // Reporting
    GENERATE_REPORTS;
}