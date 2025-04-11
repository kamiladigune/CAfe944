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
import com.cafe94.enums.Permission;
import com.cafe94.enums.UserRole; // Keep UserRole import
// Import necessary *specific* permissions
import static com.cafe94.enums.Permission.APPROVE_DELIVERY;
import static com.cafe94.enums.Permission.ASSIGN_DRIVER;
import static com.cafe94.enums.Permission.CANCEL_ANY_ORDER;
import static com.cafe94.enums.Permission.CANCEL_OWN_ORDER;
import static com.cafe94.enums.Permission.TAKE_EAT_IN_ORDER;
import static com.cafe94.enums.Permission.UPDATE_ORDER_STATUS_COLLECTED;
// import static com.cafe94.enums.Permission.UPDATE_ORDER_STATUS_COMPLETED; // Replaced
import static com.cafe94.enums.Permission.UPDATE_ORDER_STATUS_READY; // Use this
import static com.cafe94.enums.Permission.UPDATE_ORDER_STATUS_DELIVERED;
import static com.cafe94.enums.Permission.UPDATE_ORDER_STATUS_OUT_FOR_DELIVERY;
import static com.cafe94.enums.Permission.UPDATE_ORDER_STATUS_PREPARING;
import static com.cafe94.enums.Permission.UPDATE_ORDER_STATUS_SERVED;

import com.cafe94.persistence.IOrderRepository;
import com.cafe94.persistence.ITableRepository;
import com.cafe94.persistence.IUserRepository;
import com.cafe94.util.ValidationUtils;

/**
 * Implementation of the IOrderService interface
 */
public class OrderService implements IOrderService {

    private static final Logger LOGGER =
        Logger.getLogger(OrderService.class.getName());

    // Dependencies (remain the same)
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
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.tableRepository = Objects.requireNonNull(tableRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.authService = Objects.requireNonNull(authService);
        this.notificationService = Objects.requireNonNull(notificationService);
    }

    // --- createEatInOrder method (no changes needed for permissions) ---
    @Override
    public EatIn createEatInOrder(List<Item> items, int tableNumber,
                                  int customerId, User staffMember) {
        Objects.requireNonNull(items, "Items list cannot be null.");
        Objects.requireNonNull(staffMember, "Staff member cannot be null.");
        authService.checkPermission(staffMember, TAKE_EAT_IN_ORDER);

        validateOrderItems(items);
        if (tableNumber <= 0) throw new IllegalArgumentException(
            "Table number must be positive.");
        userRepository.findById(customerId).orElseThrow(() ->
            new NoSuchElementException("Customer ID " + customerId));
        Table table = tableRepository.findByTableNumber(tableNumber)
            .orElseThrow(() -> new NoSuchElementException("Table " +
            tableNumber + " does not exist."));

        EatIn newOrder = new EatIn(0, items, customerId, tableNumber,
            OrderStatus.CONFIRMED); // Or PENDING_PREPARATION?
        EatIn savedOrder = (EatIn) orderRepository.save(newOrder);
        LOGGER.log(Level.INFO, "Staff {0} created Eat-In Order ID: {1} " +
            "for Customer {2} at Table {3}", new Object[]{
            staffMember.getUserID(), savedOrder.getOrderID(), customerId,
            tableNumber});

        try {
             table.occupy();
             tableRepository.save(table);
             LOGGER.log(Level.INFO,
                 "Table {0} marked OCCUPIED for order {1}",
                 new Object[]{tableNumber, savedOrder.getOrderID()});
        } catch (IllegalStateException e) {
             LOGGER.log(Level.SEVERE, "Failed to occupy table {0} for " +
                 "order {1}: {2}.", new Object[]{tableNumber,
                 savedOrder.getOrderID(), e.getMessage()});
             // Decide if order should still proceed or be cancelled
             throw new IllegalStateException("Table " + tableNumber +
                 " could not be occupied.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CRITICAL: Failed save table status " +
                "T{0} after order {1}", new Object[]{tableNumber,
                savedOrder.getOrderID(), e});
            throw new RuntimeException("Failed table status update.", e);
        }
        notificationService.sendOrderConfirmation(savedOrder);
        return savedOrder;
    }

    // --- placeTakeawayOrder method (no permission checks here) ---
    @Override
    public Takeaway placeTakeawayOrder(List<Item> items, int customerId,
                                       LocalTime pickupTime) {
        Objects.requireNonNull(items, "Items list cannot be null.");
        Objects.requireNonNull(pickupTime, "Pickup time cannot be null.");
        validateOrderItems(items);
        if (customerId <= 0) throw new IllegalArgumentException(
            "Customer ID must be positive.");
        userRepository.findById(customerId).orElseThrow(() ->
            new NoSuchElementException("Customer ID " + customerId));

        Takeaway newOrder = new Takeaway(0, items, customerId,
            OrderStatus.CONFIRMED, pickupTime); // Assume confirmed
        Takeaway savedOrder = (Takeaway) orderRepository.save(newOrder);
        LOGGER.log(Level.INFO, "Customer {0} placed Takeaway Order ID: " +
            "{1} for pickup at {2}", new Object[]{customerId,
            savedOrder.getOrderID(), pickupTime});
        notificationService.sendOrderConfirmation(savedOrder);
        return savedOrder;
    }

    // --- placeDeliveryOrder method (no permission checks here) ---
    @Override
    public Delivery placeDeliveryOrder(List<Item> items, int customerId,
        String deliveryAddress, LocalTime estimatedDeliveryTime) {
        Objects.requireNonNull(items, "Items list cannot be null.");
        ValidationUtils.requireNonBlank(deliveryAddress, "Delivery address");
        validateOrderItems(items);
        if (customerId <= 0) throw new IllegalArgumentException(
            "Customer ID must be positive.");
        userRepository.findById(customerId).orElseThrow(() ->
            new NoSuchElementException("Customer ID " + customerId));

        Delivery newOrder = new Delivery(0, items, customerId,
            deliveryAddress, OrderStatus.CONFIRMED); // Assume confirmed
        if (estimatedDeliveryTime != null) {
            newOrder.setEstimatedDeliveryTime(estimatedDeliveryTime);
        }
        Delivery savedOrder = (Delivery) orderRepository.save(newOrder);
        LOGGER.log(Level.INFO, "Customer {0} placed Delivery Order ID: " +
            "{1} for address: {2}", new Object[]{customerId,
            savedOrder.getOrderID(), deliveryAddress});
        notificationService.sendOrderConfirmation(savedOrder);
        return savedOrder;
    }

    // --- approveDeliveryOrder method (permission OK) ---
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
            // Assuming approval moves state to READY_FOR_DISPATCH
            deliveryOrder.markReadyForDispatch();
            orderRepository.save(deliveryOrder);
            LOGGER.log(Level.INFO, "Staff {0} approved Delivery Order {1}.",
                       new Object[]{staffMember.getUserID(), orderId});
            notificationService.sendOrderStatusUpdate(deliveryOrder);
            return true;
         } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING, "Cannot approve delivery {0}: {1}",
                       new Object[]{orderId, e.getMessage()});
            return false;
         } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error approving delivery {0}",
                       new Object[]{orderId, e});
            return false;
        }
    }

    // --- assignDriverToOrder method (permission OK) ---
    @Override
    public boolean assignDriverToOrder(int orderId, int driverId,
                                       User staffMember) {
        Objects.requireNonNull(staffMember, "Staff member cannot be null.");
        if (driverId <= 0) throw new IllegalArgumentException(
            "Driver ID must be positive.");
        authService.checkPermission(staffMember, ASSIGN_DRIVER);
        Order order = findOrderByIdOrThrow(orderId);
        if (!(order instanceof Delivery)) {
            throw new IllegalArgumentException("Order " + orderId +
                " is not a Delivery Order.");
        }
        Delivery deliveryOrder = (Delivery) order;
        // Ensure order is ready for driver assignment
        if (deliveryOrder.getStatus() != OrderStatus.READY_FOR_DISPATCH) {
            throw new IllegalStateException("Order " + orderId +
                " not ready for driver (Status: " +
                deliveryOrder.getStatus() + ").");
        }
        // Validate driver exists and has DRIVER role
        User driverUser = userRepository.findById(driverId).orElseThrow(() ->
            new NoSuchElementException("Driver user ID " + driverId));
        if (driverUser.getRole() != UserRole.DRIVER) {
             throw new IllegalArgumentException("User " + driverId +
                 " is not a Driver.");
        }
        try {
            deliveryOrder.assignDriver(driverId);
            orderRepository.save(deliveryOrder);
            LOGGER.log(Level.INFO, "Driver {0} assigned to Order {1} by " +
                "Staff {2}", new Object[]{driverId, orderId,
                staffMember.getUserID()});
            if (driverUser instanceof Driver) {
                 notificationService.notifyDriverAssigned(deliveryOrder,
                    (Driver) driverUser);
            } else {
                LOGGER.log(Level.SEVERE, "User {0} has DRIVER role but " +
                    "is not instance of Driver class!", driverId);
            }
            return true;
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error assigning driver {0} to " +
                 "order {1}", new Object[]{driverId, orderId, e});
             return false;
        }
    }

    // --- startOrderPreparation method (permission OK) ---
    @Override
    public Order startOrderPreparation(int orderId, User chef) {
        Objects.requireNonNull(chef, "Chef user cannot be null.");
        authService.checkPermission(chef, UPDATE_ORDER_STATUS_PREPARING);
        Order order = findOrderByIdOrThrow(orderId);
        try {
            order.startPreparation(); // Changes state if valid
            Order savedOrder = orderRepository.save(order);
            notificationService.sendOrderStatusUpdate(savedOrder);
            LOGGER.log(Level.INFO, "Order {0} status set to PREPARING by " +
                "Chef {1}", new Object[]{orderId, chef.getUserID()});
            return savedOrder;
         } catch (IllegalStateException e) {
             LOGGER.log(Level.WARNING, "Cannot start prep for {0}: {1}",
                        new Object[]{orderId, e.getMessage()});
             throw e;
         }
    }

    // --- markOrderReady method (PERMISSION CHECK UPDATED) ---
    @Override
    public Order markOrderReady(int orderId, User chef) {
        Objects.requireNonNull(chef, "Chef user cannot be null.");
        // ** Use specific permission UPDATE_ORDER_STATUS_READY **
        authService.checkPermission(chef, UPDATE_ORDER_STATUS_READY);
        Order order = findOrderByIdOrThrow(orderId);
        try {
            order.markAsReady(); // Changes state if valid (e.g., from PREPARING)
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

    // --- markAsOutForDelivery method (permission OK) ---
    @Override
    public boolean markAsOutForDelivery(int orderId, User driver) {
        Objects.requireNonNull(driver, "Driver user cannot be null.");
        authService.checkPermission(driver,
                                    UPDATE_ORDER_STATUS_OUT_FOR_DELIVERY);
        Order order = findOrderByIdOrThrow(orderId);
        if (!(order instanceof Delivery)) {
             throw new IllegalArgumentException("Order " + orderId +
                 " is not a Delivery order.");
        }
        Delivery deliveryOrder = (Delivery) order;
        if (deliveryOrder.getAssignedDriverID() != driver.getUserID()) {
            throw new SecurityException("Driver " + driver.getUserID() +
                " not assigned to order " + orderId);
        }
        try {
             deliveryOrder.markAsOutForDelivery(); // State change
             orderRepository.save(deliveryOrder);
             notificationService.sendOrderStatusUpdate(deliveryOrder);
             LOGGER.log(Level.INFO,
                 "Order {0} marked OUT_FOR_DELIVERY by Driver {1}",
                 new Object[]{orderId, driver.getUserID()});
             return true;
        } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING,
                "Cannot mark {0} OUT_FOR_DELIVERY: {1}",
                new Object[]{orderId, e.getMessage()});
             throw e;
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error mark out for delivery {0}",
                 new Object[]{orderId, e});
             return false;
        }
    }

    // --- markOrderDelivered method (permission OK) ---
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
                " not assigned to order " + orderId);
        }
        try {
             deliveryOrder.markAsDelivered(); // State change
             orderRepository.save(deliveryOrder);
             notificationService.sendOrderStatusUpdate(deliveryOrder);
             notificationService.sendOrderDeliveredNotification(deliveryOrder);
             LOGGER.log(Level.INFO,
                 "Order {0} marked DELIVERED by Driver {1}",
                 new Object[]{orderId, driver.getUserID()});
             return true;
        } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING, "Cannot mark {0} DELIVERED: {1}",
                new Object[]{orderId, e.getMessage()});
             throw e;
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error marking delivered {0}",
                 new Object[]{orderId, e});
             return false;
        }
    }

    // --- markOrderServed method (permission OK) ---
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
            // Release table after serving? Or after payment? Assuming after
             releaseOccupiedTableSimple(eatInOrder.getTableNumber());
            return true;
        } catch (IllegalStateException e) {
             LOGGER.log(Level.WARNING, "Cannot mark {0} SERVED: {1}",
                 new Object[]{orderId, e.getMessage()});
             throw e;
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error marking served {0}",
                 new Object[]{orderId, e});
             return false;
        }
    }

    // markOrderCollected method
    @Override
    public boolean markOrderCollected(int orderId, User staffMember) {
        Objects.requireNonNull(staffMember,
        "Staff member cannot be null.");
        authService.checkPermission(staffMember,
                                    UPDATE_ORDER_STATUS_COLLECTED);
        Order order = findOrderByIdOrThrow(orderId);
        if (!(order instanceof Takeaway)) {
             throw new IllegalArgumentException("Order " + orderId +
                 " is not a Takeaway order.");
        }
        Takeaway takeawayOrder = (Takeaway) order;
        try {
            takeawayOrder.markAsCollected(); // State change
            orderRepository.save(takeawayOrder);
            notificationService.sendOrderStatusUpdate(takeawayOrder);
            LOGGER.log(Level.INFO,
                "Order {0} marked COLLECTED by Staff {1}",
                new Object[]{orderId, staffMember.getUserID()});
            return true;
        } catch (IllegalStateException e) {
             LOGGER.log(Level.WARNING, "Cannot mark {0} COLLECTED: {1}",
                 new Object[]{orderId, e.getMessage()});
             throw e;
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error marking collected {0}",
                 new Object[]{orderId, e});
             return false;
        }
    }

    // --- cancelOrder method (permissions OK) ---
    @Override
    public boolean cancelOrder(int orderId, User canceller) {
        Objects.requireNonNull(canceller, "Canceller user cannot be null.");
        Order order = findOrderByIdOrThrow(orderId);

        boolean isOwner = order.getCustomerID() == canceller.getUserID();
        Permission requiredPerm = isOwner ? CANCEL_OWN_ORDER
                                          : CANCEL_ANY_ORDER;
        try {
             authService.checkPermission(canceller, requiredPerm);
        } catch (SecurityException e) {
             if (isOwner) { // If owner check failed, try general cancel
                 authService.checkPermission(canceller, CANCEL_ANY_ORDER);
             } else {
                 throw e; // Non-owner lacks general cancel permission
             }
        }

        try {
            order.cancelOrder(); // Domain method handles state check
        } catch (IllegalStateException e) {
             LOGGER.log(Level.WARNING,
                 "Cancellation rejected for {0} due to state: {1}",
                 new Object[]{orderId, order.getStatus()});
              throw e;
        }

        orderRepository.save(order); // Persist CANCELLED status
        LOGGER.log(Level.INFO,
            "Order {0} cancelled by User {1}.",
            new Object[]{orderId, canceller.getUserID()});

         if (order instanceof EatIn) { // Release table if EatIn cancelled
             EatIn eatInOrder = (EatIn) order;
             // Check if table was occupied/reserved by this order?
             // Simple release for now
             releaseOccupiedTableSimple(eatInOrder.getTableNumber());
         }
        notificationService.sendOrderCancellation(order);
        return true;
    }

    // --- Retrieval methods (remain the same) ---
    @Override
    public Optional<Order> getOrderById(int orderId) {
         if (orderId <= 0) return Optional.empty();
        return orderRepository.findById(orderId);
    }
    @Override
    public List<Order> getOutstandingOrders() {
         LOGGER.log(Level.FINE, "Retrieving outstanding orders.");
        return orderRepository.findOutstandingOrders();
    }
    @Override
    public List<Order> getOrdersByStatus(OrderStatus status) {
        Objects.requireNonNull(status, "Status cannot be null.");
         LOGGER.log(Level.FINE, "Retrieving orders with status: {0}", status);
         return orderRepository.findOrdersByStatuses(
             Collections.singletonList(status)
         );
    }
    @Override
    public List<Order> getCustomerOrderHistory(int customerId) {
        if (customerId <= 0) throw new IllegalArgumentException(
            "Customer ID must be positive.");
        userRepository.findById(customerId).orElseThrow(() ->
            new NoSuchElementException("Customer not found: " + customerId));
         LOGGER.log(Level.FINE,
             "Retrieving order history for customer ID: {0}", customerId);
        return orderRepository.findByCustomerId(customerId);
    }
    @Override
    public List<Order> getDriverCurrentOrders(int driverId) {
        if (driverId <= 0) throw new IllegalArgumentException(
            "Driver ID must be positive.");
        userRepository.findById(driverId)
            .filter(u -> u.getRole() == UserRole.DRIVER)
            .orElseThrow(() -> new NoSuchElementException(
                "Driver not found or user is not driver: " + driverId));
         LOGGER.log(Level.FINE,
             "Retrieving current orders for driver ID: {0}", driverId);
        return orderRepository.findByDriverId(driverId);
    }

    // --- Helper methods (remain the same) ---
    /** Finds order by ID or throws NoSuchElementException. */
    private Order findOrderByIdOrThrow(int orderId) {
         if (orderId <= 0) throw new IllegalArgumentException(
            "Order ID must be positive.");
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException(
                    "Order not found with ID: " + orderId));
    }
    /** Validates order items are not null or empty. */
    private void validateOrderItems(List<Item> items) {
        Objects.requireNonNull(items, "Order items list cannot be null.");
        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                "Order must contain at least one item.");
        }
    }
    /** Simple table release (could be more robust). */
    private void releaseOccupiedTableSimple(int tableNumber) {
         if (tableNumber <= 0) return;
         try {
             tableRepository.findByTableNumber(tableNumber).ifPresent(table -> {
                 try {
                     table.makeAvailable(); // Attempt to make available
                     tableRepository.save(table);
                     LOGGER.log(Level.INFO, "Table T{0} made AVAILABLE.",
                         tableNumber);
                 } catch (IllegalStateException e) {
                      LOGGER.log(Level.WARNING, "Could not make table T{0} " +
                         "available (current state: {1}).", new Object[]{
                         tableNumber, table.getStatus()});
                 }
             });
         } catch(Exception e) {
              LOGGER.log(Level.SEVERE, "Error releasing table T{0}",
                  new Object[]{tableNumber, e});
         }
    }
}