// File: src/main/java/com/cafe94/services/NotificationService.java
package com.cafe94.services;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cafe94.domain.Booking;
import com.cafe94.domain.Delivery;
import com.cafe94.domain.Driver;
import com.cafe94.domain.EatIn;
import com.cafe94.domain.Order;
import com.cafe94.domain.Takeaway;
import com.cafe94.enums.BookingStatus;

/**
 * Implementation of INotificationService that logs notifications
 * @author Adigun Lateef
 * @version 1.0
 */
public class NotificationService implements INotificationService {

    private static final Logger LOGGER =
    Logger.getLogger(NotificationService.class.getName());

    @Override
    public void sendBookingConfirmation(Booking booking) {
        Objects.requireNonNull(booking,
        "Booking cannot be null for confirmation notification.");
        String message = String.format("Your booking (ID: %d) for %d " +
        "guests on %s at %s at table %d is CONFIRMED.",
                booking.getBookingID(),
                booking.getNumberOfGuests(),
                booking.getBookingDate(),
                booking.getBookingTime(),
                booking.getTableNumber());
        LOGGER.log(Level.INFO,
        "[Booking confirmed] To Customer {0}: {1}",
        new Object[]{booking.getCustomerID(), message});
    }

    @Override
    public void sendBookingRejection(Booking booking, String reason) {
         Objects.requireNonNull(booking,
         "Booking cannot be null for rejection notification.");
         String reasonText = (reason != null && !reason.trim().isEmpty()) ?
         reason : "Not specified";
         String message = String.format("Your booking request (ID: %d) " +
         "for %s at %s could not be confirmed. Reason: %s",
                 booking.getBookingID(),
                 booking.getBookingDate(),
                 booking.getBookingTime(),
                 reasonText);
         LOGGER.log(Level.INFO,
         "[Booking rejected] To Customer {0}: {1}",
         new Object[]{booking.getCustomerID(), message});
    }

    @Override
    public void sendBookingCancellation(Booking booking,
    BookingStatus cancelStatus) {
        Objects.requireNonNull(booking,
        "Booking cannot be null for cancellation notification.");
        Objects.requireNonNull(cancelStatus,
        "Cancellation status cannot be null.");

        String cancelledBy = "";
        if (cancelStatus == BookingStatus.CANCELLED_BY_CUSTOMER) {
            cancelledBy = "by customer";
        } else if (cancelStatus == BookingStatus.CANCELLED_BY_STAFF) {
            cancelledBy = "by staff";
        }
        

         String message = String.format(
            "Your booking (ID: %d) for %s at %s has been CANCELLED %s.",
            booking.getBookingID(), booking.getBookingDate(),
            booking.getBookingTime(), cancelledBy);
         LOGGER.log(Level.INFO,
         "[Booking cancelled] To Customer {0}: {1}",
         new Object[]{booking.getCustomerID(), message});
    }


    @Override
    public void notifyPendingBooking(Booking booking) {
        Objects.requireNonNull(booking,
        "Booking cannot be null for pending notification.");
        // Notify relevant staff
        String message = String.format("Pending Booking Approval Needed: " +
        "ID %d for Customer %d on %s at %s (%d guests).",
        booking.getBookingID(), booking.getCustomerID(),
        booking.getBookingDate(), booking.getBookingTime(),
        booking.getNumberOfGuests());
        // Log message directed
        LOGGER.log(Level.INFO, "[Incoming Booking request] {0}", message);
    }

    /**Send confirmation notification */
    @Override
    public void sendOrderConfirmation(Order order) {
        Objects.requireNonNull(order,
        "Order cannot be null for confirmation notification.");
        String message = String.format("Your order (ID: %d, Type: %s) " +
        "has been placed successfully. Total: £%.2f",
                order.getOrderID(),
                order.getClass().getSimpleName(),
                order.getTotalPrice());
         LOGGER.log(Level.INFO,
         "[Order confirmed] To Customer {0}: {1}",
         new Object[]{order.getCustomerID(), message});
    }

    /**
     * Send order status update
     */
    @Override
    public void sendOrderStatusUpdate(Order order) {
         Objects.requireNonNull(order,
         "Order cannot be null for status update notification.");
         String message = String.format(
            "Update for order ID %d: Status is now %s.",
                 order.getOrderID(),
                 order.getStatus());
          LOGGER.log(Level.INFO,
          "[Order ready for collection] To Customer {0}: {1}",
          new Object[]{order.getCustomerID(), message});
    }

    /**
     * Send notification that informs the recipient that a specific order is
     * now RAEDY
     */
    @Override
    public void notifyOrderReady(Order order) {
         Objects.requireNonNull(order,"Order cannot be null for ready notification.");
         String message = String.format("Order ID %d is now READY for %s.",
                                        order.getOrderID(),
                                        getOrderFulfilmentType(order));
         // Log for staff/customer alert
         LOGGER.log(Level.INFO, "[ALERT] {0}", message);
    }

    // Determine fulfilment type string
    private String getOrderFulfilmentType(Order order) {
        if (order instanceof EatIn) return "serving";
        if (order instanceof Takeaway) return "collection";
        if (order instanceof Delivery) return "delivery";
        return "processing";
    }

    /**Notify driver of delivery assignment
    */
    @Override
    public void notifyDriverAssigned(Delivery order, Driver driver) {
        Objects.requireNonNull(order,"Delivery order cannot be null for " +
        "driver assignment notification.");
        Objects.requireNonNull(driver, "Driver cannot be null for driver " +
        "assignment notification.");
        String message = String.format("You have been assigned delivery " +
        "order ID %d for address: %s.",
                 order.getOrderID(),
                 order.getDeliveryAddress());
        // Log notification directed to a specific driver
        LOGGER.log(Level.INFO, "[Order assign] To Driver {0} ({1} {2}): {3}",
                   new Object[]{driver.getUserID(), driver.getFirstName(),
                    driver.getLastName(), message});
    }

    /**Send delivery notification to customer */
    @Override
    public void sendOrderDeliveredNotification(Order order) {
        Objects.requireNonNull(order,
        "Order cannot be null for delivered notification.");
        String message = String.format(
            "Your order (ID: %d) has been delivered.", order.getOrderID());
         LOGGER.log(Level.INFO,
         "[SIMULATED NOTIFICATION] To Customer {0}: {1}",
         new Object[]{order.getCustomerID(), message});
    }

    /**Send order cancellation notification to customer */
    @Override
    public void sendOrderCancellation(Order order) {
         Objects.requireNonNull(order,
         "Order cannot be null for cancellation notification.");
         String message = String.format(
            "Your order (ID: %d) has been cancelled.", order.getOrderID());
          LOGGER.log(Level.INFO,
          "[Order cancelled] To Customer {0}: {1}",
          new Object[]{order.getCustomerID(), message});
    }
}