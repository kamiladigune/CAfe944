package com.cafe94.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.cafe94.domain.Order;
import com.cafe94.enums.OrderStatus;

/**
 * Interface defining persistence operations for {@link Order} entities and
 * its subclasses
 * @author Adigun Lateef
 * @version 1.0
 */
public interface IOrderRepository {

    /**
     * Saves a new order or updates an existing one based on its ID
     * @param order The Order entity to save or update
     * @return The saved or updated Order entity
     * @throws NullPointerException if the order parameter is null.
     */
    Order save(Order order);

    /**
     * Finds an order by its unique persistent identifier (ID).
     *
     * @param orderId The unique ID of the order to find
     * @return An {@code Optional<Order>} containing the found Order if
     * an order with the specified ID exists, otherwise an empty Optional and
     * returns empty Optional if orderId is not positive.
     */
    Optional<Order> findById(int orderId);

    /**
     * Deletes an order by its unique persistent ID.
     *
     * @param orderId The unique ID of the order to delete
     * @return {@code true} if an order with the specified ID was found and
     * successfully deleted, {@code false} otherwise
     */
    boolean deleteById(int orderId);

    /**
     * Finds all orders placed by a specific customer, identified by
     * their unique ID
     * @param customerId The unique ID of the customer
     * @return A {@code List<Order>} containing all orders placed by the
     * specified customer and returns an empty list if the customer has placed
     * no orders or the ID is invalid.
     */
    List<Order> findByCustomerId(int customerId);

    /**
     * Finds all orders currently assigned to a specific driver, identified by
     * their unique user ID.
     * @param driverId The unique user ID of the driver
     * @return A {@code List<Order>} containing all orders currently assigned
     * to the specified driver and returns an empty list if the driver has no
     * assigned orders or the ID is invalid.
     */
    List<Order> findByDriverId(int driverId);

    /**
     * Finds all orders that currently match one of the specified statuses
     * @param statuses A List of {@link OrderStatus} values to match
     * @return A {@code List<Order>} containing all orders whose current status
     * is present in the input list and returns an empty list if the statuses
     * list is null/empty or no orders match.
     * @throws NullPointerException if the statuses parameter is null.
     */
    List<Order> findOrdersByStatuses(List<OrderStatus> statuses);

    /**
     * Finds all "outstanding" orders
     *
     * @return A {@code List<Order>} containing all outstanding orders and
     * returns an empty list if no orders are currently outstanding.
     */
    List<Order> findOutstandingOrders();

    /**
     * Retrieves all orders from the repository.
     * Use with caution if the order history is expected to be very large, as this may
     * consume significant memory. Consider pagination or more specific query methods.
     *
     * @return A {@code List<Order>} containing all orders, potentially sorted.
     * Returns an empty list if the repository contains no orders.
     */
    List<Order> findAll();

    /**
     * Finds the top customers based on the total number of orders they
     * have placed
     * @param limit The maximum number of top customers to return
     * @return A {@code Map<Integer, Long>} where keys are Customer User IDs
     * and values are their corresponding total order counts
     * @throws IllegalArgumentException if limit is not positive.
     */
    Map<Integer, Long> findTopCustomersByOrderCount(int limit);
}
