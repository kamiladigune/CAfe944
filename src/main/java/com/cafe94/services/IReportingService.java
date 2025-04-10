// File: src/main/java/com/cafe94/services/IReportingService.java
package com.cafe94.services;

import java.time.LocalTime;

import com.cafe94.domain.Customer;
import com.cafe94.domain.Item;
import com.cafe94.domain.Report;
import com.cafe94.domain.Staff;
import com.cafe94.domain.User;

/**
 * Interface defining operations for generating various analytical reports
 * required by Management
 * @author Adigun Lateef
 * @version 1.0
 */
public interface IReportingService {

    /**
     * Generates a report identifying the most frequently ordered menu items
     *
     * @param callingUser The user requesting the report
     * @return A {@link Report} object containing the results entries where
     * keys are {@link Item} or objects and values are their order
     * counts (Long)
     * @throws SecurityException if callingUser is null or does not have
     * the required Manager role/permissions
     */
    Report generateMostPopularItemsReport(User callingUser);

    /**
     * Generates a report identifying the busiest periods
     *
     * @param callingUser The user requesting the report
     * @return A {@link Report} object
     * {@link java.time.DayOfWeek} to counts
     * @throws SecurityException if callingUser is null or does not have
     * the required Manager role/permissions
     */
    Report generateBusiestPeriodsReport(User callingUser);

    /**
     * Generates a report identifying the most active customer(s) based on
     * the number of orders placed
     *
     * @param callingUser The user requesting the report
     * @param limit The maximum number of top customers to include in the
     * report
     * @return A {@link Report} object
     * @throws SecurityException if callingUser is null or does not have the
     * required Manager role/permissions
     * @throws IllegalArgumentException if limit is not positive
     */
    Report generateMostActiveCustomerReport(User callingUser, int limit);

}