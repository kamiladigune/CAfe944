// File: src/main/java/com/cafe94/persistence/OrderRepository.java
package com.cafe94.persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.cafe94.domain.Delivery;
import com.cafe94.domain.EatIn;
import com.cafe94.domain.Order;
import com.cafe94.domain.Takeaway;
import com.cafe94.enums.OrderStatus;

/**
 * Concrete implementation of {@link IOrderRepository} using Java Serialization
 * for persistence
 * @author Adigun Lateef
 * @version 1.0
 */
public class OrderRepository implements IOrderRepository {

    private static final Logger LOGGER =
    Logger.getLogger(OrderRepository.class.getName());
    private final Map<Integer, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final String storageFilePath;

    /**
     * Constructs the repository, loading existing data from the specified
     * file path.
     * Initializes the next available ID based on loaded data.
     *
     * @param storageFilePath The path to the file for storing order data
     * @throws NullPointerException if storageFilePath is null.
     */
    public OrderRepository(String storageFilePath) {
        this.storageFilePath = Objects.requireNonNull(storageFilePath,
        "Storage file path cannot be null.");
        loadData();
        int maxId =
        orders.keySet().stream().max(Integer::compare).orElse(0);
        nextId.set(maxId + 1);
        LOGGER.log(Level.INFO,
        "OrderRepository initialized. Loaded {0} orders from {1}. " +
        "Next ID: {2}",
        new Object[]{orders.size(), this.storageFilePath, nextId.get()});
    }

    /**
     * Saves a new order or updates an existing one
     *
     * @param order The Order to save or update
     * @return The saved Order object
     * @throws NullPointerException if order is null.
     */
    @Override
    public synchronized Order save(Order order) {
        Objects.requireNonNull(order, "Order cannot be null.");
        int orderId = order.getOrderID();
        Order orderToSave = order;
    
        if (orderId <= 0) {
            int newId = nextId.getAndIncrement();
            System.out.printf(
                "DEBUG: Assigning new ID %d to order for " +
                "customer: %d%n", newId, order.getCustomerID());
    
            if (order instanceof EatIn) {
                EatIn eatInOrder = (EatIn) order;
                orderToSave = new EatIn(
                        newId,
                        eatInOrder.getItems(),
                        eatInOrder.getCustomerID(),
                        eatInOrder.getTableNumber(),
                        eatInOrder.getStatus()
                );
            } else if (order instanceof Takeaway) {
                Takeaway takeAwayOrder = (Takeaway) order;
                orderToSave = new Takeaway(
                        newId,
                        takeAwayOrder.getItems(),
                        takeAwayOrder.getCustomerID(),
                        takeAwayOrder.getStatus(),
                        takeAwayOrder.getPickupTime()
                );
            } else if (order instanceof Delivery) {
                Delivery deliveryOrder = (Delivery) order;
                orderToSave = new Delivery(
                        newId,
                        deliveryOrder.getItems(),
                        deliveryOrder.getCustomerID(),
                        deliveryOrder.getDeliveryAddress(), 
                        deliveryOrder.getStatus()
                );
                    
                ((Delivery) orderToSave).setEstimatedDeliveryTime(deliveryOrder
                .getEstimatedDeliveryTime());
                ((Delivery) orderToSave).assignDriver(deliveryOrder
                .getAssignedDriverID());
            } else {
                throw new IllegalArgumentException
                ("Unsupported Order subclass: " + order.getClass().getName());
            }
            orderId = newId;
        } else {
            
            nextId.accumulateAndGet(orderId + 1, Math::max);
        }
    
        orders.put(orderId, orderToSave); 
        // Persist the data
        saveData();
        System.out.printf(
            "INFO: Saved order: ID %d, Customer: %d, Status: %s%n",
            orderId, orderToSave.getCustomerID(), orderToSave.getStatus());
        return orders.get(orderId);
    }

    /**
     * Finds an order by its unique persistent ID.
     *
     * @param orderId The ID of the order
     * @return An Optional containing the Order if found, otherwise empty.
     */
    @Override
    public Optional<Order> findById(int orderId) {
        if (orderId <= 0) {
             LOGGER.log(Level.FINER,
             "findById called with non-positive ID: {0}", orderId);
            return Optional.empty();
        }
        return Optional.ofNullable(orders.get(orderId));
    }

    /**
     * Deletes an order by its unique persistent ID
     * @param orderId The ID of the order to delete
     * @return true if an order was found and deleted, false otherwise.
     */
    @Override
    public synchronized boolean deleteById(int orderId) {
        if (orderId <= 0) {
            LOGGER.log(Level.WARNING,
            "Attempted to delete order with invalid ID: {0}", orderId);
            return false;
        }
        Order removedOrder = orders.remove(orderId);
        if (removedOrder != null) {
            saveData(); // Persist the removal
            LOGGER.log(Level.INFO, "Deleted order ID: {0}", orderId);
            return true;
        } else {
            LOGGER.log(Level.WARNING, "Order ID {0} not found for deletion.",
            orderId);
            return false;
        }
    }

    /**
     * Finds all orders placed by a specific customer.
     * Results are sorted by order timestamp descending
     *
     * @param customerId The ID of the customer
     * @return An unmodifiable List of Orders placed by the customer.
     */
    @Override
    public List<Order> findByCustomerId(int customerId) {
        if (customerId <= 0) {
            LOGGER.log(Level.FINER,
            "findByCustomerId called with non-positive ID: {0}",
            customerId);
            return Collections.emptyList();
        }
        return orders.values().stream()
                .filter(order -> order.getCustomerID() == customerId)
                .sorted(Comparator.comparing(Order::getOrderTimestamp, 
                Comparator.nullsLast(Comparator.reverseOrder())))
                // Collect to an unmodifiable list
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                Collections::unmodifiableList));
    }

    /**
     * Finds all orders currently assigned to a specific driver
     *
     * @param driverId The user ID of the driver
     * @return An unmodifiable List containing Delivery Orders assigned
     * to the driver.
     */
    @Override
    public List<Order> findByDriverId(int driverId) {
        if (driverId <= 0) {
             LOGGER.log(Level.FINER,
             "findByDriverId called with non-positive ID: {0}", driverId);
            return Collections.emptyList();
        }
        return orders.values().stream()
                .filter(Delivery.class::isInstance)
                .map(Delivery.class::cast)
                .filter(deliveryOrder -> deliveryOrder
                .getAssignedDriverID() == driverId)
                .sorted(Comparator.comparing(Order::getOrderTimestamp,
                Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                Collections::unmodifiableList));
    }

    /**
     * Finds all orders matching one of the specified statuses
     *
     * @param statuses A List of OrderStatus values to match
     * @return An unmodifiable List containing Orders with a matching status.
     * @throws NullPointerException if statuses is null.
     */
    @Override
    public List<Order> findOrdersByStatuses(List<OrderStatus> statuses) {
        Objects.requireNonNull(statuses,
        "Statuses list cannot be null for findOrdersByStatuses.");
        if (statuses.isEmpty()) {
            return Collections.emptyList();
        }
        
        Set<OrderStatus> statusSet = EnumSet.copyOf(statuses);
        return orders.values().stream()
                .filter(order -> statusSet.contains(order.getStatus()))

                .sorted(Comparator.comparing(Order::getOrderTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())))
                
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                Collections::unmodifiableList));
    }

    /**
     * Retrieves all orders
     *
     * @return An unmodifiable List of all Orders.
     */
    @Override
    public List<Order> findAll() {
        // Create a new list for sorting
        List<Order> sortedOrders = new ArrayList<>(orders.values());
        sortedOrders.sort(Comparator.comparing(Order::getOrderTimestamp,
        Comparator.nullsLast(Comparator.naturalOrder())));
        return Collections.unmodifiableList(sortedOrders);
    }

     /**
     * Finds all "outstanding" orders
     * Results are sorted by order timestamp ascending
     *
     * @return An unmodifiable List containing all outstanding Orders.
     */
    @Override
    public List<Order> findOutstandingOrders() {
        List<Order> outstanding = orders.values().stream()
                // Filter orders where status is not null and not final status
                .filter(order -> order.getStatus() != null
                && !order.getStatus().isFinalStatus())
                .sorted(Comparator.comparing(Order::getOrderTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())))
                // Collect mutable list
                .collect(Collectors.toList());
        LOGGER.log(Level.FINE, "Found {0} outstanding orders.",
        outstanding.size());
        // Return as unmodifiable
        return Collections.unmodifiableList(outstanding);
    }


    /**
     * Finds the top customers based on the number of orders placed
     * @param limit The maximum number of top customers to return
     * @return A Map ordered by count descending.
     * @throws IllegalArgumentException if limit is not positive.
     */
    @Override
    public Map<Integer, Long> findTopCustomersByOrderCount(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException(
                "Limit must be positive for findTopCustomersByOrderCount.");
        }
        LOGGER.log(Level.WARNING,
        "findTopCustomersByOrderCount performing potentially inefficient " +
        "in-memory aggregation.");

        // Group orders by customer ID and count them
        Map<Integer, Long> counts = orders.values().stream()
                // Ensure valid customer ID
                .filter(o -> o.getCustomerID() > 0)
                .collect(Collectors.groupingBy(
                        Order::getCustomerID,
                        Collectors.counting()
                ));

        // Sort the results by count descending
        return counts.entrySet().stream()
                // Sort by count descending
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Loads order data from the specified storage file
     */
    @SuppressWarnings("unchecked")
    private synchronized void loadData() {
        File file = new File(storageFilePath);
        if (!file.exists() || file.length() == 0) {
            LOGGER.log(Level.INFO, "Order data file not found or empty " +
            "({0}). Starting with an empty repository.", storageFilePath);
            ensureDirectoryExists(file);
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ObjectInputStream ois = new ObjectInputStream(bis)) {

            Object readObject = ois.readObject();
             if (readObject instanceof Map) {
                orders.clear();
                Map<?,?> rawMap = (Map<?,?>) readObject;
                rawMap.forEach((key, value) -> {
                    if (key instanceof Integer && value instanceof Order) {
                         orders.put((Integer)key, (Order)value);
                    } else {
                         LOGGER.log(Level.WARNING,
                         "Skipping invalid entry during load: Key type {0}, "
                         + "Value type {1}",
                         new Object[]{key != null ? key.getClass()
                            .getName() : "null",
                            value != null ? value.getClass()
                            .getName() : "null"});
                    }
                });
                LOGGER.log(Level.INFO,
                "Successfully loaded {0} order entries from: {1}",
                new Object[]{orders.size(), storageFilePath});
            } else {
                 LOGGER.log(Level.SEVERE,
                 "Order data file ({0}) does not contain a valid Map.",
                 storageFilePath);
            }
        } catch (FileNotFoundException e) {
              LOGGER.log(Level.SEVERE,
              "Order data file not found for loading: " + storageFilePath, e);
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
             LOGGER.log(Level.SEVERE, "Failed to load order data from file ("
             + storageFilePath + "). Data might be corrupted or class " +
             "versions incompatible.", e);
        }
    }

    /**
     * Saves the current state of the orders map to the storage file
     */
    private synchronized void saveData() {
        File file = new File(storageFilePath);
        ensureDirectoryExists(file);

        // Overwrites
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(new ConcurrentHashMap<>(orders));
             LOGGER.log(Level.FINE, "Order data saved successfully to {0}",
             storageFilePath);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,
            "CRITICAL: Failed to save order data to file ("
            + storageFilePath + "). Data loss may occur.", e);
        }
    }

    /**
     * Ensures the parent directory for the storage file exists and
     * creates it if necessary.
     * @param file The storage file.
     */
    private void ensureDirectoryExists(File file) {
        Objects.requireNonNull(file, "File cannot be null");
        File parentDir = file.getParentFile();
         if (parentDir != null && !parentDir.exists()) {
              LOGGER.log(Level.INFO,
              "Attempting to create directory for order data: {0}",
              parentDir.getAbsolutePath());
             if (parentDir.mkdirs()) {
                 LOGGER.log(Level.INFO,
                 "Successfully created directory: {0}",
                 parentDir.getAbsolutePath());
             } else {
                  LOGGER.log(Level.SEVERE,
                  "Failed to create directory for order data: {0}",
                  parentDir.getAbsolutePath());
             }
         }
     }
}