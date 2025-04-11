package com.cafe94.services;

import java.time.format.TextStyle;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.cafe94.domain.Booking;
import com.cafe94.domain.Item;
import com.cafe94.domain.Order;
import com.cafe94.domain.Report;
import com.cafe94.domain.User;
import com.cafe94.enums.BookingStatus;
import static com.cafe94.enums.Permission.GENERATE_REPORTS;
import com.cafe94.enums.ReportType;
import com.cafe94.persistence.IBookingRepository;
import com.cafe94.persistence.IOrderRepository;
import com.cafe94.persistence.IUserRepository;

/**
 * Implementation of the {@link IReportingService} interface.
 * Provides methods to generate system reports
 * @author Adigun Lateef
 * @version 1.1
 */
public class ReportingService implements IReportingService {

    private static final Logger LOGGER =
        Logger.getLogger(ReportingService.class.getName());

    // Dependencies
    private final IOrderRepository orderRepository;
    private final IBookingRepository bookingRepository;
    private final IUserRepository userRepository;
    private final AuthorizationService authService;

    // Limit for top item/period reports
    private static final int REPORT_LIMIT = 5;

    /**
     * Constructor for Dependency Injection.
     */
    public ReportingService(IOrderRepository orderRepository,
                            IBookingRepository bookingRepository,
                            IUserRepository userRepository,
                            AuthorizationService authService) {
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.authService = Objects.requireNonNull(authService);
    }

    /**
     * Generates report on most popular items based on order history
     */
    @Override
    public Report generateMostPopularItemsReport(User manager) {
        Objects.requireNonNull(manager, "Calling manager cannot be null.");
        authService.checkPermission(manager, GENERATE_REPORTS);
        LOGGER.log(Level.INFO, "Generating Popular Items Report " +
            "requested by Manager ID: {0}", manager.getUserID());

        Map<String, Long> itemCounts;
        try {
            List<Order> allOrders = orderRepository.findAll();
            // Flatten list of items from all orders and count occurrences
            itemCounts = allOrders.stream()
                .flatMap(order -> order.getItems().stream())
                .filter(Objects::nonNull)
                .map(Item::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                    Function.identity(),
                    Collectors.counting()
                ));
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error accessing order data " +
                 "for popular items report", e);
             // Return empty report on error
             return new Report(ReportType.POPULAR_ITEMS,
                 "Error Generating Popular Items Report",
                 Collections.emptyMap());
        }

        // Sort by count descending and limit results
        Map<String, Object> reportData = itemCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(REPORT_LIMIT)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));

        String reportTitle = String.format(
            "Most Popular Menu Items (Top %d)", REPORT_LIMIT);
        return new Report(ReportType.POPULAR_ITEMS, reportTitle, reportData);
    }

    /**
     * Generates report on busiest periods based on booking data.
     * Uses placeholder logic: counts confirmed bookings by hour and day.
     */
    @Override
    public Report generateBusiestPeriodsReport(User manager) {
        Objects.requireNonNull(manager, "Calling manager cannot be null.");
        authService.checkPermission(manager, GENERATE_REPORTS);
        LOGGER.log(Level.INFO, "Generating Busiest Periods Report " +
            "requested by Manager ID: {0}", manager.getUserID());

        Map<String, Long> bookingsByHour;
        Map<String, Long> bookingsByDay;

        try {
            List<Booking> allBookings = bookingRepository.findAll();

            // Filter for relevant bookings
            List<Booking> relevantBookings = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED ||
                             b.getStatus() == BookingStatus.COMPLETED)
                .filter(b -> b.getBookingDateTime() != null)
                .collect(Collectors.toList());

            // Count bookings per hour of the day
            bookingsByHour = relevantBookings.stream()
                .collect(Collectors.groupingBy(
                    b -> String.format("%02d:00",
                                       b.getBookingTime().getHour()),
                    Collectors.counting()
                ));

            // Count bookings per day of the week
            bookingsByDay = relevantBookings.stream()
                .collect(Collectors.groupingBy(
                     b -> b.getBookingDate().getDayOfWeek().getDisplayName(
                         TextStyle.FULL, Locale.UK),
                     Collectors.counting()
                ));

        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error accessing booking data " +
                 "for busiest periods report", e);
             return new Report(ReportType.BUSIEST_PERIODS,
                 "Error Generating Busiest Periods Report",
                 Collections.emptyMap());
        }

        // Sort results by count descending
        Map<String, Object> reportData = new LinkedHashMap<>();

        Map<String, Long> sortedHours = bookingsByHour.entrySet().stream()
             .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
             .limit(REPORT_LIMIT)
             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                 (e1, e2) -> e1, LinkedHashMap::new));

        Map<String, Long> sortedDays = bookingsByDay.entrySet().stream()
             .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
             // No limit applied to days, show all 7 if present
             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                 (e1, e2) -> e1, LinkedHashMap::new));

        reportData.put("BookingsByHourOfDay (Top " + REPORT_LIMIT + ")",
                       sortedHours);
        reportData.put("BookingsByDayOfWeek", sortedDays);

        String reportTitle = "Busiest Periods Analysis";
        return new Report(ReportType.BUSIEST_PERIODS, reportTitle, reportData);
    }

    /**
     * Generates report on most active customers based on order count
     */
    @Override
    public Report generateMostActiveCustomerReport(User manager, int limit) {
        Objects.requireNonNull(manager,
        "Calling manager cannot be null.");
        if (limit <= 0) {
            throw new IllegalArgumentException(
                "Report limit must be positive.");
        }
        authService.checkPermission(manager, GENERATE_REPORTS);
        LOGGER.log(Level.INFO, "Generating Top {0} Active Customers Report " +
            "requested by Manager ID: {1}",
            new Object[]{limit, manager.getUserID()});

        Map<Integer, Long> customerOrderCounts;
        try {
            customerOrderCounts =
                orderRepository.findTopCustomersByOrderCount(limit);
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error accessing order/customer data " +
                 "for active customer report", e);
             return new Report(ReportType.CUSTOMER_ACTIVITY,
                 "Error Generating Active Customer Report",
                 Collections.emptyMap());
        }

        // Prepare Report Data
        Map<String, Object> customerActivityData = new LinkedHashMap<>();
        if (customerOrderCounts != null && !customerOrderCounts.isEmpty()) {
            customerOrderCounts.forEach((customerId, count) -> {
                String customerName = userRepository.findById(customerId)
                    .map(user -> String.format("%s %s (ID:%d)",
                         user.getFirstName(), user.getLastName(),
                         user.getUserID()))
                    .orElse("Customer ID " + customerId + " (Not Found)");
                customerActivityData.put(customerName, count);
            });
             LOGGER.log(Level.INFO,
                 "Generated active customer report for {0} customers.",
                 customerActivityData.size());
        } else {
             LOGGER.log(Level.WARNING, "No customer activity data found.");
        }

        String reportTitle = String.format(
            "Most Active Customers (Top %d by Order Count)", limit);
        return new Report(ReportType.CUSTOMER_ACTIVITY, reportTitle,
                          customerActivityData);
    }

}