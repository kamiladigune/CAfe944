// File: src/main/java/com/cafe94/util/ConsoleNotificationService.java
package com.cafe94.util;
 
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.cafe94.domain.Booking;
import com.cafe94.domain.Driver;
import com.cafe94.domain.Order;
import com.cafe94.enums.BookingStatus;
import com.cafe94.services.INotificationService;

/**
 * Implementation of INotificationService that prints messages to the console
 * @author Adigun Lateef
 * @version 1.0
 */
public class ConsoleNotificationService implements INotificationService {

    private static final DateTimeFormatter TIME_FORMATTER =
    DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER =
    DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    @Override
    public void sendBookingConfirmation(Booking booking) {
        Objects.requireNonNull(booking,
        "Booking cannot be null for confirmation notification.");
        System.out.printf(
            "[NOTIFICATION] Booking Confirmed: ID %d for Customer %d " +
            "on %s at %s for %d guests. Table: %d%n",
                booking.getBookingID(), booking.getCustomerID(),
                booking.getBookingDate().format(DATE_FORMATTER),
                booking.getBookingTime().format(TIME_FORMATTER),
                booking.getNumberOfGuests(), booking.getTableNumber());
    }

    @Override
    public void sendBookingRejection(Booking booking, String reason) {
        Objects.requireNonNull(booking, "Booking cannot be null for " +
        "rejection notification.");
        System.out.printf("[NOTIFICATION] Booking Rejected: ID %d for " +
        "Customer %d on %s at %s. Reason: %s%n",
                booking.getBookingID(), booking.getCustomerID(),
                booking.getBookingDate().format(DATE_FORMATTER),
                booking.getBookingTime().format(TIME_FORMATTER),
                reason != null ? reason : "No reason specified");
    }

     @Override
    public void sendBookingCancellation(Booking booking,
    BookingStatus cancelStatus) {
         Objects.requireNonNull(booking,
         "Booking cannot be null for cancellation notification.");
         System.out.printf("[NOTIFICATION] Booking Cancelled: ID %d for " +
         "Customer %d on %s at %s. Status: %s%n",
                booking.getBookingID(), booking.getCustomerID(),
                booking.getBookingDate().format(DATE_FORMATTER),
                booking.getBookingTime().format(TIME_FORMATTER),
                cancelStatus);
    }

     @Override
    public void notifyPendingBooking(Booking booking) {
        Objects.requireNonNull(booking,
        "Booking cannot be null for pending notification.");
        System.out.printf("[STAFF ALERT] Pending Booking Approval: ID %d " +
        "for Customer %d on %s at %s for %d guests.%n",
                booking.getBookingID(), booking.getCustomerID(),
                booking.getBookingDate().format(DATE_FORMATTER),
                booking.getBookingTime().format(TIME_FORMATTER),
                booking.getNumberOfGuests());
    }


    @Override
    public void sendOrderConfirmation(Order order) {
         Objects.requireNonNull(order,
         "Order cannot be null for confirmation notification.");
         System.out.printf("[NOTIFICATION] Order Confirmed: ID %d for " +
         "Customer %d. Status: %s. Total: £%.2f%n",
                order.getOrderID(), order.getCustomerID(),
                order.getStatus(), order.getTotalPrice());
    }


    @Override
    public void sendOrderStatusUpdate(Order order) {
        Objects.requireNonNull(order,
        "Order cannot be null for status update notification.");
         System.out.printf("[NOTIFICATION] Order Status Update: ID "
         + "%d for Customer %d is now %s.%n",
         order.getOrderID(), order.getCustomerID(), order.getStatus());
    }

    @Override
    public void notifyOrderReady(Order order) {
        Objects.requireNonNull(order,
        "Order cannot be null for ready notification.");
         System.out.printf("[STAFF/CUSTOMER ALERT] Order Ready: ID %d for " +
         "Customer %d. Type: %s%n",
         order.getOrderID(), order.getCustomerID(),
         order.getClass().getSimpleName());
    }

     @Override
    public void notifyDriverAssigned(com.cafe94.domain.Delivery order,
    Driver driver) {
         Objects.requireNonNull(order,
         "Order cannot be null for driver assignment notification.");
         Objects.requireNonNull(driver,
         "Driver cannot be null for driver assignment notification.");

         String address;
            address = order.getDeliveryAddress();

         System.out.printf("[DRIVER ALERT - %s %s (ID:%d)] Delivery " +
         "Assigned: Order ID %d for Customer %d. Address: %s%n",
                driver.getFirstName(), driver.getLastName(),
                driver.getUserID(), order.getOrderID(),
                order.getCustomerID(), address);
    }

    @Override
    public void sendOrderDeliveredNotification(Order order) {
        Objects.requireNonNull(order,
        "Order cannot be null for delivered notification.");
         System.out.printf("[NOTIFICATION] Order Delivered: ID %d for " +
         "Customer %d has been delivered.%n",
         order.getOrderID(), order.getCustomerID());
    }

    @Override
    public void sendOrderCancellation(Order order) {
         Objects.requireNonNull(order,
         "Order cannot be null for cancellation notification.");
         System.out.printf("[NOTIFICATION] Order Cancelled: ID %d for " +
         "Customer %d has been cancelled.%n",
                order.getOrderID(), order.getCustomerID());
    }
}