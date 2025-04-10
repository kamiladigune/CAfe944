// File: src/main/java/com/cafe94/services/IUserService.java
package com.cafe94.services;

import java.util.List;
import java.util.Optional;

import com.cafe94.domain.Customer;
import com.cafe94.domain.Staff;
import com.cafe94.domain.User;
import com.cafe94.dto.StaffDetailsDto;
import com.cafe94.enums.UserRole;

/**
 * Interface defining user-related business logic operations
 * including authentication, registration, staff management,
 * profile retrieval, and recording work hours
 * @author Adigun Lateef
 * @version 1.0
 */
public interface IUserService {

    /**
     * Authenticates a user based on email and password
     * @param email       The user's email address
     * @param rawPassword The user's plain text password attempt
     * @return A {@code List<User>} containing authenticated profiles,
     * returns an empty list if authentication fails
     * @throws IllegalArgumentException if email or rawPassword are null or
     * blank
     */
    
    List<User> login(String email, String rawPassword);

    /**
     * Logs out the currently authenticated user
     */
    void logout();

    /**
     * Registers a new customer account in the system
     *
     * @param firstName   Customer's first name
     * @param lastName    Customer's last name
     * @param email       Customer's email address
     * @param rawPassword Customer's chosen plain text password
     * @param address     Customer's physical address
     * @param phoneNumber Customer's phone number
     * @return The newly created and persisted {@link Customer} object
     * @throws IllegalArgumentException if any required input is invalid,
     * or if the email is already registered to a customer
     */
    Customer registerCustomer(String firstName, String lastName,
    String email, String rawPassword, String address, String phoneNumber);

    /**
     * Hires a new staff member and creates their profile
     *
     * @param firstName        Staff member's first name
     * @param lastName         Staff member's last name
     * @param email            Staff member's email address
     * @param rawPassword      Staff member's initial plain text password
     * @param role             The assigned {@link UserRole}
     * @param staffId          The unique identifier for the staff member
     * @param initialHours     Initial total hours worked
     * @param callingUser      The authenticated {@link User} performing
     * the action
     * @return The newly created and persisted {@link Staff} object
     * @throws SecurityException if callingUser is null or does not have
     * Manager privileges
     * @throws IllegalArgumentException if any required input is invalid
     * or if the email/staffId is already in use by another staff member
     */
    Staff hireStaff(String firstName, String lastName, String email,
    String rawPassword, UserRole role, String staffId, double initialHours,
    User callingUser);

    /**
     * Updates profile details for an existing staff member
     * @param staffUserId    The unique ID of the staff member to update
     * @param staffDetails   A {@link StaffDetailsDto} containing the updated
     * details
     * @param callingUser    The authenticated {@link User} performing the
     * action
     * @return The updated and persisted {@link Staff} object
     * @throws SecurityException if callingUser is null or does not have
     * Manager privileges
     * @throws java.util.NoSuchElementException if staffUserId is not found
     * or does not correspond to a Staff member
     * @throws IllegalArgumentException if details in the DTO are invalid
     */
    Staff updateStaffDetails(int staffUserId, StaffDetailsDto staffDetails,
                             User callingUser);

    /**
     * Removes a staff member from the system
     *
     * @param staffUserId   The unique ID of the staff member to remove
     * @param callingUser   The authenticated {@link User} performing the
     * action
     * @return {@code true} if the staff member was successfully found and
     * removed, {@code false} otherwise
     * @throws SecurityException if callingUser is null, not a Manager,
     * or attempts self-removal
     * @throws java.util.NoSuchElementException if staffUserId is not found
     * or does not correspond to a Staff member
     */
    boolean removeStaff(int staffUserId, User callingUser);

    /**
     * Finds a user by their unique persistent ID
     *
     * @param userId The unique ID of the user to find
     * @return An {@code Optional<User>} containing the User if found,
     * otherwise empty
     */
    Optional<User> findUserById(int userId);


}
