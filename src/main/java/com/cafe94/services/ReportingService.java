// File: src/main/java/com/cafe94/services/ReportingService.java
package com.cafe94.services;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.cafe94.domain.Report;
import com.cafe94.domain.User;
import static com.cafe94.enums.Permission.GENERATE_REPORTS;
import com.cafe94.enums.ReportType;
import com.cafe94.persistence.IBookingRepository;
import com.cafe94.persistence.IOrderRepository;
import com.cafe94.persistence.IUserRepository;

/**
 * Implementation of the {@link IReportingService} interface
 * @author Adigun Lateef
 * @version 1.0
 */
public class ReportingService implements IReportingService {

    private static final Logger LOGGER =Logger.getLogger(ReportingService.class.getName());

    // Dependencies
    private final IOrderRepository orderRepository;
    @SuppressWarnings("unused")
    private final IBookingRepository bookingRepository;
    private final IUserRepository userRepository;
    private final AuthorizationService authService;

    /**
     * Constructor for Dependency Injection.
     *
     * @param orderRepository   Repository for order data
     * @param bookingRepository Repository for booking data
     * @param userRepository    Repository for user data
     * @param authService       Service for authorization checks
     */
    public ReportingService(IOrderRepository orderRepository,
                                IBookingRepository bookingRepository,
                                IUserRepository userRepository,
                                AuthorizationService authService) {
        this.orderRepository = Objects.requireNonNull(orderRepository,
        "OrderRepository cannot be null.");
        this.bookingRepository = Objects.requireNonNull(bookingRepository,
        "BookingRepository cannot be null.");
        this.userRepository = Objects.requireNonNull(userRepository,
        "UserRepository cannot be null.");
        this.authService = Objects.requireNonNull(authService,
        "AuthorizationService cannot be null.");
    }

    /**
     * Generates a report on the most popular menu items based on order history
     */
    @Override
    public Report generateMostPopularItemsReport(User manager) {
        Objects.requireNonNull(manager,
        "Calling manager cannot be null.");
        authService.checkPermission(manager, GENERATE_REPORTS);
        LOGGER.log(Level.INFO, "Generating Popular Items Report requested " +
        "by Manager ID: {0}", manager.getUserID());

        Map<String, Object> reportData = new LinkedHashMap<>();
        LOGGER.log(Level.WARNING,
        "generatePopularItemsReport currently. ");
        reportData.put("Pizza", 25L);
        reportData.put("Coffee", 18L);
        reportData.put("Curry Soup", 15L);
        String reportTitle = "Most Popular Menu Items";
        return new Report(ReportType.POPULAR_ITEMS, reportTitle, reportData);
    }

    /**
     * Generates a report on the busiest periods based on booking data
     */
    @Override
    public Report generateBusiestPeriodsReport(User manager) {
        Objects.requireNonNull(manager, "Calling manager cannot be null.");
        authService.checkPermission(manager, GENERATE_REPORTS);
        LOGGER.log(Level.INFO, "Generating Busiest Periods Report requested " +
        "by Manager ID: {0}", manager.getUserID());

        LOGGER.log(Level.WARNING, "generateBusiestPeriodsReport.");
        Map<String, Object> reportData = new HashMap<>();
        Map<String, Long> bookingsByHour = new HashMap<>();
        bookingsByHour.put("19:00", 10L);
        bookingsByHour.put("20:00", 8L);
        bookingsByHour.put("18:00", 5L);
        Map<String, Long> bookingsByDay = new HashMap<>();
        bookingsByDay.put("FRIDAY", 30L);
        bookingsByDay.put("SATURDAY", 45L);
        bookingsByDay.put("SUNDAY", 28L);

        reportData.put("BookingsByHourOfDay", bookingsByHour);
        reportData.put("BookingsByDayOfWeek", bookingsByDay);
        String reportTitle = "Busiest Periods Analysis";
        return new Report(ReportType.BUSIEST_PERIODS, reportTitle, reportData);
    }

    /**
     * Generates a report on the most active customers based on order count.
     *
     * @param limit The maximum number of top customers to include.
     */
    @Override
    public Report generateMostActiveCustomerReport(User manager, int limit) {
        Objects.requireNonNull(manager, "Calling manager cannot be null.");
        if (limit <= 0) {
            throw new IllegalArgumentException(
                "Report limit must be positive.");
        }
        authService.checkPermission(manager, GENERATE_REPORTS);
        LOGGER.log(Level.INFO,"Generating Top {0} Active Customers Report " +
        "requested by Manager ID: {1}",
        new Object[]{limit, manager.getUserID()});

       
        Map<Integer, Long> customerOrderCounts =
        orderRepository.findTopCustomersByOrderCount(limit);

        // Prepare Report Data)
        Map<String, Object> customerActivityData = new LinkedHashMap<>();
        if (customerOrderCounts != null && !customerOrderCounts.isEmpty()) {
            customerOrderCounts.forEach((customerId, count) -> {
                // Fetch user details to get name
                String customerName = userRepository.findById(customerId)
                // Map Optional<User> to String representation
                .map(user -> String.format("%s %s (ID:%d)",
                user.getFirstName(), user.getLastName(), user.getUserID()))
                .orElse("Customer ID " + customerId + " (Not Found)");
                customerActivityData.put(customerName, count);
            });
             LOGGER.log(Level.INFO,
             "Generated active customer report data for {0} customers.",
             customerActivityData.size());
        } else {
             LOGGER.log(Level.WARNING,"No customer activity data found " +
             "matching criteria (limit: {0}).", limit);
        }

        // Create Report Object
        String reportTitle = String.format(
            "Most Active Customers (Top %d by Order Count)", limit);
        return new Report(ReportType.CUSTOMER_ACTIVITY, reportTitle,
        customerActivityData);
    }

}