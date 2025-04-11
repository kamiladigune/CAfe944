// File: src/main/java/com/cafe94/services/INotificationService.java
package com.cafe94.services;

import com.cafe94.domain.Booking;
import com.cafe94.domain.Delivery;
import com.cafe94.domain.Driver;
import com.cafe94.domain.Order;
import com.cafe94.enums.BookingStatus;

/**
 * Interface for sending notifications related to various system events
 * @author  Adigun Lateef
 * @version 1.0
 */
public interface INotificationService {


    /**
     * Sends booking confirmation details to the customer associated
     * with the booking
     * @param booking The confirmed {@link Booking}
     */
    void sendBookingConfirmation(Booking booking);

    /**
     * Sends a booking rejection notice to the customer associated
     * with the booking
     * @param booking The rejected {@link Booking} (must not be null).
     * @param reason The reason for rejection (can be null or empty).
     */
    void sendBookingRejection(Booking booking, String reason);

    /**
     * Sends a booking cancellation notice
     * @param booking The cancelled {@link Booking}
     * @param cancelStatus The status indicating who cancelled
     */
    void sendBookingCancellation(Booking booking, BookingStatus cancelStatus);

    /**
     * Notifies relevant staff that a new booking request has been
     * submitted and requires approval.
     * @param booking The {@link Booking} in PENDING_APPROVAL status
     */
    void notifyPendingBooking(Booking booking);


    /**
     * Sends a general order confirmation to the customer associated
     * with the order
     * @param order The confirmed {@link Order}
     */
    void sendOrderConfirmation(Order order);

    /**
     * Sends an update about the order's status change to the customer
     * @param order The {@link Order} whose status has been updated
     */
    void sendOrderStatusUpdate(Order order);

    /**
     * Notifies relevant parties that an order is ready for the next step
     * @param order The {@link Order} that has reached the READY status
     */
    void notifyOrderReady(Order order);

    /**
     * Notifies a driver that they have been assigned a specific delivery order
     * @param order The {@link Delivery} order assigned
     * @param driver The {@link Driver} assigned to the order
     */
    
    void notifyDriverAssigned(Delivery order, Driver driver);

    /**
     * Sends notification that their delivery order has been delivered
     * @param order The {@link Order} that has been delivered
     */
    void sendOrderDeliveredNotification(Order order);

    /**
     * Sends notification that their order has been cancelled
     * @param order The {@link Order} that has been cancelled
     */
    void sendOrderCancellation(Order order);

}