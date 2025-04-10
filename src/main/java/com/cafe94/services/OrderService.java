// File: src/main/java/com/cafe94/services/OrderService.java
package com.cafe94.services;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cafe94.domain.Delivery;
import com.cafe94.domain.Driver;
import com.cafe94.domain.EatIn;
import com.cafe94.domain.Item;
import com.cafe94.domain.Order;
import com.cafe94.domain.Table;
import com.cafe94.domain.Takeaway;
import com.cafe94.domain.User;
import com.cafe94.enums.OrderStatus;
import static com.cafe94.enums.Permission.APPROVE_DELIVERY;
import static com.cafe94.enums.Permission.ASSIGN_DRIVER;
import static com.cafe94.enums.Permission.CANCEL_ANY_ORDER;
import static com.cafe94.enums.Permission.CANCEL_OWN_ORDER;
import static com.cafe94.enums.Permission.TAKE_EAT_IN_ORDER;
import static com.cafe94.enums.Permission.UPDATE_ORDER_STATUS_COLLECTED;
import static com.cafe94.enums.Permission.UPDATE_ORDER_STATUS_COMPLETED;
import static com.cafe94.enums.Permission.UPDATE_ORDER_STATUS_DELIVERED;
import static com.cafe94.enums.Permission.UPDATE_ORDER_STATUS_OUT_FOR_DELIVERY;
import static com.cafe94.enums.Permission.UPDATE_ORDER_STATUS_PREPARING;
import static com.cafe94.enums.Permission.UPDATE_ORDER_STATUS_SERVED;
import com.cafe94.enums.UserRole;
import com.cafe94.persistence.IOrderRepository;
import com.cafe94.persistence.ITableRepository;
import com.cafe94.persistence.IUserRepository;
import com.cafe94.util.ValidationUtils;

/**
 * Implementation of the IOrderService interface
 * @author Adigun Lateef
 * @version 1.0
 */
public class OrderService implements IOrderService {

    private static final Logger LOGGER =
    Logger.getLogger(OrderService.class.getName());

    // Dependencies
    private final IOrderRepository orderRepository;
    private final ITableRepository tableRepository;
    private final IUserRepository userRepository;
    private final AuthorizationService authService;
    private final INotificationService notificationService;

    /** Constructor for Dependency Injection. */
    public OrderService(IOrderRepository orderRepository,
                            ITableRepository tableRepository,
                            IUserRepository userRepository,
                            AuthorizationService authService,
                            INotificationService notificationService) {
        this.orderRepository = Objects.requireNonNull(orderRepository,
        "OrderRepository cannot be null.");
        this.tableRepository = Objects.requireNonNull(tableRepository,
        "TableRepository cannot be null.");
        this.userRepository = Objects.requireNonNull(userRepository,
        "UserRepository cannot be null.");
        this.authService = Objects.requireNonNull(authService,
        "AuthorizationService cannot be null.");
        this.notificationService = Objects.requireNonNull(notificationService,
        "NotificationService cannot be null.");
    }

    
    /**Create eat-in order
     * @return Saved new eat-in order
    */
    @Override
    public EatIn createEatInOrder(List<Item> items, int tableNumber,
    int customerId, User staffMember) {
        Objects.requireNonNull(items, "Items list cannot be null.");
        Objects.requireNonNull(staffMember, "Staff member cannot be null.");
        authService.checkPermission(staffMember, TAKE_EAT_IN_ORDER);

        validateOrderItems(items);
        if (tableNumber <= 0) throw new IllegalArgumentException(
            "Table number must be positive.");
        userRepository.findById(customerId)
            .orElseThrow(() -> new NoSuchElementException(
                "Customer not found with ID " + customerId));

        Table table = tableRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> new NoSuchElementException("Table " +
                tableNumber + " does not exist."));

        // Create a new EatIn order
        EatIn newOrder = new EatIn(0, items, customerId, tableNumber,
        OrderStatus.CONFIRMED);
        EatIn savedOrder = (EatIn) orderRepository.save(newOrder);
        LOGGER.log(Level.INFO, "Staff {0} created Eat-In Order ID: {1} for " +
        "Customer {2} at Table {3}",
        new Object[]{staffMember.getUserID(), savedOrder.getOrderID(),
            customerId, tableNumber});

        try {
             table.occupy();
             // Save updated table
             tableRepository.save(table);
             LOGGER.log(Level.INFO,
             "Table {0} marked as OCCUPIED for order {1}",
             new Object[]{tableNumber, savedOrder.getOrderID()});
        } catch (IllegalStateException e) {
             LOGGER.log(Level.SEVERE, "Failed to occupy table {0} for new " +
             "order {1}: {2}. Order created but table not marked.",
             new Object[]{tableNumber, savedOrder.getOrderID(),
                e.getMessage()});
             throw new IllegalStateException("Table " + tableNumber +
             " could not be occupied.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CRITICAL: Failed to save table status "
            + "update for Table " + tableNumber + " after creating order " +
            savedOrder.getOrderID(), e);
            throw new RuntimeException(
                "Failed to update table status after order creation.", e);
        }

        notificationService.sendOrderConfirmation(savedOrder);
        return savedOrder;
    }

    /**Create takeaway order
     * @return Saved new takeaway order
    */
    @Override
    public Takeaway placeTakeawayOrder(List<Item> items, int customerId,
    LocalTime pickupTime) {
         Objects.requireNonNull(items, "Items list cannot be null.");
         Objects.requireNonNull(pickupTime, "Pickup time cannot be null.");

        validateOrderItems(items);
         if (customerId <= 0) throw new IllegalArgumentException(
            "Customer ID must be positive.");
        userRepository.findById(customerId)
             .orElseThrow(() -> new NoSuchElementException(
                "Customer not found with ID " + customerId));

        Takeaway newOrder = new Takeaway(0, items, customerId,
        OrderStatus.CONFIRMED, pickupTime);
        Takeaway savedOrder = (Takeaway) orderRepository.save(newOrder);
        LOGGER.log(Level.INFO, "Customer {0} placed Takeaway Order ID: {1} " +
        "for pickup at {2}",
        new Object[]{customerId, savedOrder.getOrderID(), pickupTime});

        notificationService.sendOrderConfirmation(savedOrder);
        return savedOrder;
    }

    /**Create delivery order
     * @return Saved new delivery order
    */
    @Override
    public Delivery placeDeliveryOrder(List<Item> items, int customerId,
    String deliveryAddress, LocalTime estimatedDeliveryTime) {
        Objects.requireNonNull(items, "Items list cannot be null.");
        ValidationUtils.requireNonBlank(deliveryAddress, "Delivery address");

        validateOrderItems(items);
         if (customerId <= 0) throw new IllegalArgumentException(
            "Customer ID must be positive.");
        userRepository.findById(customerId)
             .orElseThrow(() -> new NoSuchElementException(
                "Customer not found with ID " + customerId));

        Delivery newOrder = new Delivery(0, items, customerId,
        deliveryAddress, OrderStatus.CONFIRMED);
        if (estimatedDeliveryTime != null) {
            newOrder.setEstimatedDeliveryTime(estimatedDeliveryTime);
        }

        Delivery savedOrder = (Delivery) orderRepository.save(newOrder);
        LOGGER.log(Level.INFO, "Customer {0} placed Delivery Order ID: " +
        "{1} for address: {2}",
        new Object[]{customerId, savedOrder.getOrderID(), deliveryAddress});

        notificationService.sendOrderConfirmation(savedOrder);
        return savedOrder;
    }

    /**Approve delivery order by waiter
    */
    @Override
    public boolean approveDeliveryOrder(int orderId, User staffMember) {
        Objects.requireNonNull(staffMember, "Staff member cannot be null.");
        authService.checkPermission(staffMember, APPROVE_DELIVERY);

        Order order = findOrderByIdOrThrow(orderId);
        if (!(order instanceof Delivery)) {
            throw new IllegalArgumentException("Order " + orderId +
            " is not a Delivery Order.");
        }
        Delivery deliveryOrder = (Delivery) order;

        try {
            deliveryOrder.markReadyForDispatch();

            orderRepository.save(deliveryOrder);
            LOGGER.log(Level.INFO, "Staff {0} approved Delivery Order {1} " +
            "for dispatch.", new Object[]{staffMember.getUserID(), orderId});
            notificationService.sendOrderStatusUpdate(deliveryOrder);
            return true;
         } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING, "Cannot approve delivery for Order " +
            "{0}. Invalid state transition: {1}", new Object[]{orderId,
                e.getMessage()});
            return false;
         } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error approving delivery order " +
            orderId, e);
            return false;
        }
    }

    /**Assign order to driver
    */
    @Override
    public boolean assignDriverToOrder(int orderId, int driverId,
    User staffMember) {
         Objects.requireNonNull(staffMember,
         "Staff member cannot be null.");
         if (driverId <= 0) throw new IllegalArgumentException(
            "Driver ID must be positive.");
         authService.checkPermission(staffMember, ASSIGN_DRIVER);

         Order order = findOrderByIdOrThrow(orderId);
         if (!(order instanceof Delivery)) {
            throw new IllegalArgumentException("Order " + orderId +
            " is not a Delivery Order.");
        }
         Delivery deliveryOrder = (Delivery) order;

        if (deliveryOrder.getStatus() != OrderStatus.READY_FOR_DISPATCH) {
            throw new IllegalStateException("Order " + orderId +
            " is not ready for driver assignment (Status: " +
            deliveryOrder.getStatus() + "). Requires READY_FOR_DISPATCH.");
        }

        User driverUser = userRepository.findById(driverId)
                .orElseThrow(() -> new NoSuchElementException(
                    "Driver user with ID " + driverId + " not found."));
        if (driverUser.getRole() != UserRole.DRIVER) {
             throw new IllegalArgumentException("User " + driverId +
             " is not a Driver.");
        }
        if (!(driverUser instanceof Driver)){
             LOGGER.log(Level.SEVERE, "User {0} has DRIVER role but is " +
             "not instance of Driver class!", driverId);
        }

        try {
            deliveryOrder.assignDriver(driverId);
            orderRepository.save(deliveryOrder);
            LOGGER.log(Level.INFO, "Driver {0} assigned to Order {1} by " +
            "Staff {2}", new Object[]{driverId, orderId,
                staffMember.getUserID()});
            notificationService.notifyDriverAssigned(deliveryOrder,
            (Driver) driverUser);
            return true;
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error assigning driver " + driverId +
             " to order " + orderId, e);
             return false;
        }
    }

    /**Start Order preparation
    */
    @Override
    public Order startOrderPreparation(int orderId, User chef) {
         Objects.requireNonNull(chef, "Chef user cannot be null.");
         authService.checkPermission(chef, UPDATE_ORDER_STATUS_PREPARING);
         Order order = findOrderByIdOrThrow(orderId);
         try {
            order.startPreparation();
            Order savedOrder = orderRepository.save(order);
            notificationService.sendOrderStatusUpdate(savedOrder);
            LOGGER.log(Level.INFO, "Order {0} status set to PREPARING by " +
            "Chef {1}", new Object[]{orderId, chef.getUserID()});
            return savedOrder;
         } catch (IllegalStateException e) {
             LOGGER.log(Level.WARNING, "Cannot start preparation for order " +
             "{0}: {1}", new Object[]{orderId, e.getMessage()});
             throw e;
         }
    }

    /**Mark order as ready */
    @Override
    public Order markOrderReady(int orderId, User chef) {
        Objects.requireNonNull(chef, "Chef user cannot be null.");
        authService.checkPermission(chef, UPDATE_ORDER_STATUS_COMPLETED);
         Order order = findOrderByIdOrThrow(orderId);
         try {
            order.markAsReady();
            Order savedOrder = orderRepository.save(order);
            notificationService.sendOrderStatusUpdate(savedOrder);
            notificationService.notifyOrderReady(savedOrder);
            LOGGER.log(Level.INFO, "Order {0} status set to READY by Chef " +
            "{1}", new Object[]{orderId, chef.getUserID()});
            return savedOrder;
         } catch (IllegalStateException e) {
             LOGGER.log(Level.WARNING, "Cannot mark order {0} as ready: {1}",
             new Object[]{orderId, e.getMessage()});
             throw e;
         }
    }

    /**MArk order as out for delivery
    */
    @Override
    public boolean markAsOutForDelivery(int orderId, User driver) {
        Objects.requireNonNull(driver, "Driver user cannot be null.");
        authService.checkPermission(driver, UPDATE_ORDER_STATUS_OUT_FOR_DELIVERY);

        Order order = findOrderByIdOrThrow(orderId);
        if (!(order instanceof Delivery)) {
             throw new IllegalArgumentException("Order " + orderId +
             " is not a Delivery order.");
        }
        Delivery deliveryOrder = (Delivery) order;

        if (deliveryOrder.getAssignedDriverID() != driver.getUserID()) {
            throw new SecurityException("Driver " + driver.getUserID() +
            " is not assigned to order " + orderId);
        }

        try {
             deliveryOrder.markAsOutForDelivery();

             orderRepository.save(deliveryOrder);
             notificationService.sendOrderStatusUpdate(deliveryOrder);
             LOGGER.log(Level.INFO,
             "Order {0} marked OUT_FOR_DELIVERY by Driver {1}",
             new Object[]{orderId, driver.getUserID()});
             return true;
        } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING,
            "Cannot mark order {0} as OUT_FOR_DELIVERY: {1}",
            new Object[]{orderId, e.getMessage()});
             throw e;
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error marking order " + orderId +
             " as out for delivery", e);
             return false;
        }
    }

    /**Mark order as delivered */
    @Override
    public boolean markOrderDelivered(int orderId, User driver) {
        Objects.requireNonNull(driver, "Driver user cannot be null.");
        authService.checkPermission(driver, UPDATE_ORDER_STATUS_DELIVERED);

        Order order = findOrderByIdOrThrow(orderId);
        if (!(order instanceof Delivery)) {
             throw new IllegalArgumentException("Order " + orderId +
             " is not a Delivery order.");
        }
        Delivery deliveryOrder = (Delivery) order;

        if (deliveryOrder.getAssignedDriverID() != driver.getUserID()) {
            throw new SecurityException("Driver " + driver.getUserID() +
            " is not assigned to order " + orderId);
        }

        try {
             
             deliveryOrder.markAsDelivered();
             orderRepository.save(deliveryOrder);
             notificationService.sendOrderStatusUpdate(deliveryOrder);
             notificationService.sendOrderDeliveredNotification(deliveryOrder);
             LOGGER.log(Level.INFO,
             "Order {0} marked DELIVERED by Driver {1}",
             new Object[]{orderId, driver.getUserID()});
             return true;
        } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING,
            "Cannot mark order {0} as DELIVERED: {1}",
            new Object[]{orderId, e.getMessage()});
             throw e;
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error marking order " + orderId +
             " as delivered", e);
             return false;
        }
    }

    /**Mark order as served*/
    @Override
    public boolean markOrderServed(int orderId, User waiter) {
         Objects.requireNonNull(waiter, "Waiter user cannot be null.");
         authService.checkPermission(waiter, UPDATE_ORDER_STATUS_SERVED);

        Order order = findOrderByIdOrThrow(orderId);
         if (!(order instanceof EatIn)) {
             throw new IllegalArgumentException("Order " + orderId +
             " is not an Eat-In order.");
         }
         EatIn eatInOrder = (EatIn) order;

        try {
            eatInOrder.markAsServed();

            orderRepository.save(eatInOrder);
            notificationService.sendOrderStatusUpdate(eatInOrder);
            LOGGER.log(Level.INFO, "Order {0} marked SERVED by Waiter {1}",
            new Object[]{orderId, waiter.getUserID()});
            return true;
        } catch (IllegalStateException e) {
             LOGGER.log(Level.WARNING, "Cannot mark order {0} as SERVED: {1}",
             new Object[]{orderId, e.getMessage()});
             throw e;
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error marking order " + orderId +
             " as served", e);
             return false;
        }
    }

    /**Mark order as collected
    */
    @Override
    public boolean markOrderCollected(int orderId, User staffMember) {
        Objects.requireNonNull(staffMember,
        "Staff member cannot be null.");
        authService.checkPermission(staffMember, UPDATE_ORDER_STATUS_COLLECTED);

        Order order = findOrderByIdOrThrow(orderId);
         if (!(order instanceof Takeaway)) {
             throw new IllegalArgumentException("Order " + orderId +
             " is not a Takeaway order.");
         }
         Takeaway takeawayOrder = (Takeaway) order;

        try {
            takeawayOrder.markAsCollected();

            orderRepository.save(takeawayOrder);
            notificationService.sendOrderStatusUpdate(takeawayOrder);
            LOGGER.log(Level.INFO,
            "Order {0} marked COLLECTED by Staff {1}",
            new Object[]{orderId, staffMember.getUserID()});
            return true;
        } catch (IllegalStateException e) {
             LOGGER.log(Level.WARNING,
             "Cannot mark order {0} as COLLECTED: {1}",
             new Object[]{orderId, e.getMessage()});
             throw e;
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error marking order " + orderId +
             " as collected", e);
             return false;
        }
    }

    /**Cancels order */
    @Override
    public boolean cancelOrder(int orderId, User canceller) {
        Objects.requireNonNull(canceller, "Canceller user cannot be null.");
        Order order = findOrderByIdOrThrow(orderId);

        boolean isOwner = order.getCustomerID() == canceller.getUserID();
        boolean hasStaffPermission = false;
        try {
             authService.checkPermission(canceller, CANCEL_ANY_ORDER);
             hasStaffPermission = true;
        } catch (SecurityException e) {
            
        }

        try {
            if (isOwner) {
                authService.checkPermission(canceller, CANCEL_OWN_ORDER);
                order.cancelOrder();
                LOGGER.log(Level.INFO,
                "Customer {0} cancelling own order {1}",
                new Object[]{canceller.getUserID(), orderId});
            } else {
                 if (!hasStaffPermission) throw new SecurityException(
                    "User lacks permission to cancel this order.");
                 order.cancelOrder();
                 LOGGER.log(Level.INFO,
                 "Staff {0} cancelling order {1}",
                 new Object[]{canceller.getUserID(), orderId});
            }
        } catch (SecurityException e) {
             LOGGER.log(Level.WARNING,
             "User {0} lacks permission to cancel order {1}.",
             new Object[]{canceller.getUserID(), orderId});
             throw e;
        } catch (IllegalStateException e) {
             LOGGER.log(Level.WARNING,
             "Cancellation rejected for order {0} due to state: {1}",
             new Object[]{orderId, order.getStatus()});
              throw e;
        }

        // Persist the CANCELLED status
        orderRepository.save(order);
        LOGGER.log(Level.INFO,
        "Order {0} successfully cancelled by User {1}.",
        new Object[]{orderId, canceller.getUserID()});

         if (order instanceof EatIn) {
             EatIn eatInOrder = (EatIn) order;
             releaseOccupiedTableSimple(eatInOrder.getTableNumber());
         }

        notificationService.sendOrderCancellation(order);
        return true;
    }


    /**Get order by ID */
    @Override
    public Optional<Order> getOrderById(int orderId) {
        if (orderId <= 0) {
            LOGGER.log(Level.FINER,
            "getOrderById called with non-positive ID: {0}", orderId);
            return Optional.empty();
        }
        return orderRepository.findById(orderId);
    }

    /**Get outstanding order */
    @Override
    public List<Order> getOutstandingOrders() {
         LOGGER.log(Level.FINE, "Retrieving outstanding orders.");
        return orderRepository.findOutstandingOrders();
    }

    /**Get order by status */
    @Override
    public List<Order> getOrdersByStatus(OrderStatus status) {
        Objects.requireNonNull(status,
        "Status cannot be null for getOrdersByStatus.");
         LOGGER.log(Level.FINE, "Retrieving orders with status: {0}",
         status);
         return orderRepository.findOrdersByStatuses(Collections
         .singletonList(status));
    }

    /**Get Customer order history */
    @Override
    public List<Order> getCustomerOrderHistory(int customerId) {
        if (customerId <= 0) {
             throw new IllegalArgumentException(
                "Customer ID must be positive.");
        }
        userRepository.findById(customerId).orElseThrow(() ->
        new NoSuchElementException("Customer not found: " + customerId));
         LOGGER.log(Level.FINE,
         "Retrieving order history for customer ID: {0}", customerId);
        return orderRepository.findByCustomerId(customerId);
    }

    /**Get Driver current assigned order */
    @Override
    public List<Order> getDriverCurrentOrders(int driverId) {
        if (driverId <= 0) {
            throw new IllegalArgumentException("Driver ID must be positive.");
        }
        userRepository.findById(driverId)
            .filter(u -> u.getRole() == UserRole.DRIVER)
            .orElseThrow(() -> new NoSuchElementException(
                "Driver not found or user is not a driver: " + driverId));
         LOGGER.log(Level.FINE,
          "Retrieving current orders for driver ID: {0}", driverId);
        return orderRepository.findByDriverId(driverId);
    }


    /**Finds order by ID or throw  */
    private Order findOrderByIdOrThrow(int orderId) {
         if (orderId <= 0) throw new IllegalArgumentException(
            "Order ID must be positive.");
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException(
                    "Order not found with ID: " + orderId));
    }

    private void validateOrderItems(List<Item> items) {
        Objects.requireNonNull(items,
        "Order items list cannot be null.");
        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                "Order must contain at least one item.");
        }
    }

    private void releaseOccupiedTableSimple(int tableNumber) {
         if (tableNumber <= 0) {
            LOGGER.log(Level.WARNING,
            "Attempted to release table with invalid number: {0}",
            tableNumber);
             return;
         }
         try{
             Optional<Table> tableOpt =
             tableRepository.findByTableNumber(tableNumber);
             if(tableOpt.isPresent()){
                 Table table = tableOpt.get();
                     table.makeAvailable();
                     tableRepository.save(table);
                     LOGGER.log(Level.INFO, "Table {0} status set back to " +
                     "AVAILABLE after order completion/cancellation.",
                     tableNumber);
                 } else {
                      LOGGER.log(Level.WARNING, "Attempted to release table " +
                      "{0} which was not OCCUPIED (Status: {1}). No status " +
                      "change applied.", new Object[]{tableNumber});
                 }
         } catch(IllegalStateException e) {
             LOGGER.log(Level.SEVERE, "Error making table " + tableNumber
             + " available (invalid state transition?)", e);
         } catch(Exception e) {
              LOGGER.log(Level.SEVERE,
              "Error occurred while attempting to release table " +
              tableNumber, e);
         }
    }

}