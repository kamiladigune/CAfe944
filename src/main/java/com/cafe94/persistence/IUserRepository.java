// File: src/main/java/com/cafe94/persistence/IUserRepository.java
package com.cafe94.persistence;

import java.util.List;
import java.util.Optional;

import com.cafe94.domain.Staff;
import com.cafe94.domain.User;
import com.cafe94.enums.UserRole;

/**
 * Interface defining persistence operations for {@link User} entities and
 * its subclasses
 * @author Adigun Lateef
 * @version 1.0
 */
public interface IUserRepository {

    /**
     * Finds all user profiles associated with the given email address.
     *
     * @param email The email address to search for
     * @return A {@code List<User>} containing all user profiles
     * matching the email and returns an empty list if no match is found or if
     * the email is invalid.
     */
    List<User> findByEmail(String email);

    /**
     * Finds a User by their unique persistent identifier (ID).
     *
     * @param userId The unique ID of the user to find
     * @return An {@code Optional<User>} containing the User if found,
     * otherwise an empty Optional and returns empty Optional if userId is
     * not positive.
     */
    Optional<User> findById(int userId);

    /**
     * Retrieves all User entities from the repository
     *
     * @return A {@code List<User>} containing all users, potentially sorted.
     * Returns an empty list if the repository contains no users.
     */
    List<User> findAll();

    /**
     * Retrieves all Staff entities from the repository.
     *
     * @return A {@code List<Staff>} containing all staff members and returns
     * an empty list if the repository contains no staff members.
     */
    List<Staff> findAllStaff();

    /**
     * Finds all Staff members matching a specific role.
     *
     * @param role The {@link UserRole} to filter by
     * @return A {@code List<Staff>} containing all staff members with
     * the specified role and returns an empty list if no staff match the role
     * or if the role is invalid/non-staff.
     */
    List<Staff> findStaffByRole(UserRole role);

    /**
     * Saves a new User entity or updates an existing one based on its ID
     * {@code user.getUserID()} is zero or negative
     *
     * @param user The User entity to save or update
     * @return The saved or updated User entity
     * @throws NullPointerException if the user parameter is null
     */
    User save(User user);

    /**
     * Deletes a User entity using the provided object instance
     *
     * @param user The User entity to delete
     * @return {@code true} if a user corresponding to the input object's ID
     * was found and deleted, {@code false} otherwise
     * @throws NullPointerException if the user parameter is null
     */
    boolean delete(User user);

    /**
     * Deletes a User entity directly by its unique persistent identifier (ID).
     *
     * @param userId The unique ID of the user to delete (must be positive).
     * @return {@code true} if a user with the specified ID was found and
     * deleted, {@code false} otherwise
     */
    boolean deleteById(int userId);
}