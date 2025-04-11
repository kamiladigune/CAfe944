// File: src/main/java/com/cafe94/services/IOrderService.java
package com.cafe94.services;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.cafe94.domain.Delivery;
import com.cafe94.domain.EatIn;
import com.cafe94.domain.Item;
import com.cafe94.domain.Order;
import com.cafe94.domain.Takeaway;
import com.cafe94.domain.User;
import com.cafe94.enums.OrderStatus;

/**
 * Interface defining business logic operations related to managing
 * customer orders
 * @author Adigun Lateef
 * @version 1.0
 */
public interface IOrderService {


    /**
     * Creates a new Eat-In order
     *
     * @param items       The list of {@link Item} objects ordered
     * @param tableNumber The table number where the order is placed
     * @param customerId  The ID of the customer associated with the order
     * @param staffMember The Waiter or authorized staff {@link User}
     * creating the order
     * @return The newly created and persisted {@link EatIn} order object
     * @throws SecurityException if staffMember lacks the required permission
     * @throws IllegalArgumentException if inputs are invalid
     * @throws IllegalStateException if the specified table is not in an
     * appropriate state
     * @throws java.util.NoSuchElementException if customerId, tableNumber,
     * or any item ID does not exist
     */
    EatIn createEatInOrder(List<Item> items, int tableNumber, int customerId,
    User staffMember);

    /**
     * Creates a new Takeaway order
     * @param items      The list of {@link Item} objects ordered
     * @param customerId The ID of the customer placing the order
     * @param pickupTime The requested {@link LocalTime} for order pickup
     * @return The newly created and persisted {@link Takeaway} order object
     * @throws IllegalArgumentException if inputs are invalid
     * @throws java.util.NoSuchElementException if customerId or
     * any item ID does not exist
     */
    Takeaway placeTakeawayOrder(List<Item> items, int customerId,
    LocalTime pickupTime );

    /**
     * Creates a new Delivery order, typically initiated by a Customer
     * @param items                The list of {@link Item} objects ordered
     * @param customerId           The ID of the customer placing the order
     * @param deliveryAddress      The delivery address
     * @param estimatedDeliveryTime Optional estimated delivery time requested
     * by the customer
     * @return The newly created and persisted {@link Delivery} order object
     * @throws IllegalArgumentException if inputs are invalid
     * @throws java.util.NoSuchElementException if customerId or any item ID
     * does not exist
     */
    Delivery placeDeliveryOrder(List<Item> items, int customerId,
    String deliveryAddress, LocalTime estimatedDeliveryTime);


    /**
     * Approves a pending delivery order, allowing it to proceed in the
     * workflow
     * @param orderId     The unique ID of the delivery order to approve
     * @param staffMember The staff {@link User} performing the approval
     * @return {@code true} if the order was successfully approved and its
     * status updated, {@code false} otherwise
     * @throws SecurityException if staffMember lacks the required permission
     * @throws java.util.NoSuchElementException if no order is found with
     * the given orderId
     * @throws IllegalArgumentException if the order found is not a delivery
     * order
     * @throws IllegalStateException if the order is not in a state that allows
     * approval
     */
    boolean approveDeliveryOrder(int orderId, User staffMember);

    /**
     * Assigns a driver to a ready delivery order
     *
     * @param orderId     The unique ID of the delivery order
     * @param driverId    The unique user ID of the driver to assign
     * @param staffMember The staff {@link User} performing the assignment
     * @return {@code true} if the driver was successfully assigned,
     * {@code false} otherwise
     * @throws SecurityException if staffMember lacks the required permission
     * @throws java.util.NoSuchElementException if orderId or driverId does
     * not correspond to an existing entity
     * @throws IllegalArgumentException if the order is not a delivery order
     * or the driverId does not correspond to a user with the DRIVER role
     * @throws IllegalStateException if the order is not in a state ready for
     * driver assignment
     */
    boolean assignDriverToOrder(int orderId, int driverId, User staffMember);

    /**
     * Marks an order's status as PREPARING
     *
     * @param orderId The unique ID of the order
     * @param chef    The staff {@link User} initiating preparation
     * @return The updated {@link Order} object with the new status
     * @throws SecurityException if the chef lacks the required permission

     * @throws IllegalStateException if the order cannot be moved to PREPARING
     * from its current state
     */
    Order startOrderPreparation(int orderId, User chef);

    /**
     * Marks an order's status as READY
     *
     * @param orderId The unique ID of the order
     * @param chef    The staff {@link User} marking the order ready
     * @return The updated {@link Order} object with the new status
     * @throws SecurityException if the chef lacks the required permission
     * @throws java.util.NoSuchElementException if no order is found with the
     * given orderId
     * @throws IllegalStateException if the order cannot be marked as
     * READY from its current state
     */
    Order markOrderReady(int orderId, User chef);

    /**
     * Marks a delivery order's status as OUT_FOR_DELIVERY
     *
     * @param orderId The unique ID of the delivery order
     * @param driver  The assigned {@link User} performing the action
     * @return {@code true} if the status was successfully updated,
     * {@code false} otherwise.
     * @throws SecurityException if the driver lacks permission or is not
     * assigned to this order.
     * @throws java.util.NoSuchElementException if no order is found with the
     * given orderId.
     * @throws IllegalArgumentException if the order found is not a
     * delivery order.
     * @throws IllegalStateException if the order cannot be marked as
     * OUT_FOR_DELIVERY from its current state
     */
    boolean markAsOutForDelivery(int orderId, User driver);

    /**
     * Marks a delivery order's status as DELIVERED
     *
     * @param orderId The unique ID of the delivery order
     * @param driver  The assigned {@link User} confirming delivery
     * @return {@code true} if the status was successfully updated,
     * {@code false} otherwise
     * @throws SecurityException if the driver lacks permission or is not
     * assigned to this order
     * @throws java.util.NoSuchElementException if no order is found with
     * the given orderId
     * @throws IllegalArgumentException if the order found is not a
     *  order
     * @throws IllegalStateException if the order cannot be marked as
     * DELIVERED from its current state
     */
    boolean markOrderDelivered(int orderId, User driver);

    /**
     * Marks an eat-in order's status as SERVED
     *
     * @param orderId The unique ID of the eat-in order
     * @param waiter  The staff {@link User} serving the order
     * @return {@code true} if the status was successfully updated,
     * {@code false} otherwise
     * @throws SecurityException if the waiter lacks the required permission
     * @throws java.util.NoSuchElementException if no order is found with the
     * given orderId
     * @throws IllegalArgumentException if the order found is not an eat-in
     * order
     * @throws IllegalStateException if the order cannot be marked as
     * SERVED from its current state
     */
    boolean markOrderServed(int orderId, User waiter);

    /**
     * Marks a takeaway order's status as COLLECTED by the customer
     * @param orderId     The unique ID of the takeaway order
     * @param staffMember The staff {@link User} confirming collection
     * @return {@code true} if the status was successfully updated,
     * {@code false} otherwise
     * @throws SecurityException if the staffMember lacks the required
     * permission
     * @throws java.util.NoSuchElementException if no order is found with the
     * given orderId
     * @throws IllegalArgumentException if the order found is not a
     * takeaway order
     * @throws IllegalStateException if the order cannot be marked as
     * COLLECTED from its current state
     */
    boolean markOrderCollected(int orderId, User staffMember);

    /**
     * Cancels an existing order
     *
     * @param orderId   The unique ID of the order to cancel
     * @param canceller The {@link User} initiating the cancellation
     * @return {@code true} if the order was successfully cancelled,
     * {@code false} otherwise
     * @throws SecurityException if the canceller lacks permission based on
     * their role and the order status/ownership.
     * @throws java.util.NoSuchElementException if no order is found with the
     * given orderId
     * @throws IllegalStateException if the order is in a state that
     *  prevents cancellation
     */
    boolean cancelOrder(int orderId, User canceller);



    /**
     * Retrieves a specific order by its unique persistent ID
     * @param orderId The unique ID of the order to retrieve
     * @return An {@code Optional<Order>} containing the Order if found and
     * accessible, otherwise empty
     */
    Optional<Order> getOrderById(int orderId);

    /**
     * Retrieves a list of all currently "outstanding" orders
     * @return A {@code List<Order>} containing outstanding orders and
     * returns an empty list if none exist
     */
    List<Order> getOutstandingOrders();

    /**
     * Retrieves the order history for a specific customer
     * @param customerId The unique ID of the customer
     * @return A {@code List<Order>} containing the customer's orders
     * Returns an empty list if the customer ID is invalid or the
     * customer has no orders
     * @throws java.util.NoSuchElementException if customerId does not
     * correspond to an existing user
     */
    List<Order> getCustomerOrderHistory(int customerId);

    /**
     * Retrieves the list of orders currently assigned to a specific driver
     *
     * @param driverId The unique user ID of the driver
     * @return A {@code List<Order>} (likely Delivery orders) assigned to the
     * driver and returns an empty list if the driver ID is invalid or the
     * driver has no assigned orders
     * @throws java.util.NoSuchElementException if driverId does not correspond
     * to an existing user
     * @throws IllegalArgumentException if the user identified by driverId
     * does not have the DRIVER role
     */
    List<Order> getDriverCurrentOrders(int driverId);

     /**
     * Retrieves all orders matching a specific status
     *
     * @param status The {@link OrderStatus} to filter by
     * @return A {@code List<Order>} containing orders with the
     * specified status and returns an empty list if status is null
     * or no orders match
     */
    List<Order> getOrdersByStatus(OrderStatus status);

}
