// File: src/main/java/com/cafe94/persistence/UserRepository.java
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.cafe94.domain.Staff;
import com.cafe94.domain.User;
import com.cafe94.enums.UserRole;

/**
 * Concrete implementation of {@link IUserRepository} using Java Serialization
 * for persistence
 */
public class UserRepository implements IUserRepository {

    private static final Logger LOGGER =
    Logger.getLogger(UserRepository.class.getName());
    private final Map<Integer, User> users = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final String storageFilePath;

    /**
     * Constructs the repository, loading existing data from the
     * specified file path
     *
     * @param storageFilePath The path to the file used for storing user data
     * @throws NullPointerException if storageFilePath is null.
     */
    public UserRepository(String storageFilePath) {
        this.storageFilePath = Objects.requireNonNull(storageFilePath,
        "Storage file path cannot be null.");
        // Load data
        loadData();
        int maxId = users.keySet().stream().max(Integer::compare).orElse(0);
        nextId.set(maxId + 1);
        LOGGER.log(Level.INFO,
        "UserRepository initialized. Loaded {0} users from {1}. Next ID: {2}",
                   new Object[]{users.size(), this.storageFilePath,
                    nextId.get()});
    }


    /**
     * Finds a User by their unique persistent identifier (ID).
     *
     * @param userId The unique ID of the user to find (must be positive).
     * @return An Optional containing the User if found, otherwise empty.
     */
    @Override
    public Optional<User> findById(int userId) {
        if (userId <= 0) {
            LOGGER.log(Level.FINER,
            "findById called with non-positive ID: {0}", userId);
            return Optional.empty();
        }
        User user = users.get(userId);
        LOGGER.log(Level.FINEST, "findById({0}) - Found: {1}",
        new Object[]{userId, user != null});
        return Optional.ofNullable(user);
    }

    /**
     * Finds all user profiles associated with the given email address
     *
     * @param email The email address to search for
     * @return An unmodifiable List containing all User profiles
     * matching the email.
     */
    @Override
    public List<User> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            LOGGER.log(Level.WARNING,
            "findByEmail called with null or blank email.");
            return Collections.emptyList();
        }
        String lowerCaseEmail = email.toLowerCase();
        List<User> foundUsers = users.values().stream()
                .filter(user -> user.getEmail() != null &&
                user.getEmail().toLowerCase().equals(lowerCaseEmail))
                // Collect to a standard list first
                .collect(Collectors.toList());
        LOGGER.log(Level.FINE, "findByEmail({0}) - Found: {1} users",
        new Object[]{email, foundUsers.size()});
        // Return as unmodifiable
        return Collections.unmodifiableList(foundUsers);
    }

     /**
     * Retrieves all Staff entities from the repository
     *
     * @return An unmodifiable List containing all staff members.
     */
    @Override
    public List<Staff> findAllStaff() {
        List<Staff> staffList = users.values().stream()
                .filter(Staff.class::isInstance)
                .map(Staff.class::cast)
                .sorted(Comparator.comparing(User::getLastName,
                String.CASE_INSENSITIVE_ORDER)
                                  .thenComparing(User::getFirstName,
                                  String.CASE_INSENSITIVE_ORDER))
                 // Collect to an unmodifiable list
                .collect(Collectors.collectingAndThen(Collectors.toList(),
            Collections::unmodifiableList));
        LOGGER.log(Level.FINE, "findAllStaff - Found: {0} staff members",
        staffList.size());
        return staffList;
    }

    /**
     * Finds all staff members matching a specific role
     *
     * @param role The UserRole to filter by
     * @return An unmodifiable List containing staff members with the
     * specified role.
     * @throws NullPointerException if role is null.
     */
    @Override
    public List<Staff> findStaffByRole(UserRole role) {
        Objects.requireNonNull(role,
        "Role cannot be null for findStaffByRole.");
         if (!role.isStaffRole()) {
             LOGGER.log(Level.WARNING,
             "Attempted to find staff by non-staff role: {0}", role);
             return Collections.emptyList();
         }
        List<Staff> staffList = users.values().stream()
                .filter(Staff.class::isInstance)
                .map(Staff.class::cast)
                .filter(staff -> staff.getRole() == role)
                .sorted(Comparator.comparing(User::getLastName,
                String.CASE_INSENSITIVE_ORDER)
                                  .thenComparing(User::getFirstName,
                                  String.CASE_INSENSITIVE_ORDER))
                // Collect to an unmodifiable list
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                Collections::unmodifiableList));
        LOGGER.log(Level.FINE,
        "findStaffByRole({0}) - Found: {1} staff members",
        new Object[]{role, staffList.size()});
        return staffList;
    }

    /**
     * Retrieves all User entities
     *
     * @return An unmodifiable List containing all users.
     */
    @Override
    public List<User> findAll() {
        // Return an unmodifiable list copy
        List<User> sortedUsers = new ArrayList<>(users.values());
        sortedUsers.sort(Comparator.comparing(User::getLastName,
        String.CASE_INSENSITIVE_ORDER)
                                  .thenComparing(User::getFirstName,
                                  String.CASE_INSENSITIVE_ORDER));
        LOGGER.log(Level.FINE, "findAll - Found: {0} users",
        sortedUsers.size());
        return Collections.unmodifiableList(sortedUsers);
    }

    /**
     * Saves a new user or updates an existing one
     *
     * @param user The User object to save
     * @return The saved User object
     * @throws NullPointerException if user is null.
     */
    @Override
    public synchronized User save(User user) {
        Objects.requireNonNull(user, "User to save cannot be null.");

        int currentUserId = user.getUserID();
        int finalUserId;
        if (currentUserId <= 0) {
            finalUserId = nextId.getAndIncrement();
            try {
               
                user.setUserID(finalUserId);
                LOGGER.log(Level.FINE, "Assigned new ID {0} to user: {1}",
                new Object[]{finalUserId, user.getEmail()});
            } catch (Exception e) {
                 // Log errors if setUserID throws an exception
                LOGGER.log(Level.SEVERE,
                "Failed to set new ID on user object. ID generated: " +
                finalUserId, e);
                throw new RuntimeException(
                    "Failed to assign ID to new user", e);
            }
        } else {
             finalUserId = currentUserId;
             // Ensure nextId is correctly positioned if saving an existing
             // user
             nextId.accumulateAndGet(finalUserId + 1, Math::max);
              LOGGER.log(Level.FINEST, "Saving existing user with ID {0}",
              finalUserId);
        }

        users.put(finalUserId, user);
        // Persist the changes
        saveData();
        LOGGER.log(Level.INFO, "Saved user: ID={0}, Email='{1}', Role={2}",
                   new Object[]{finalUserId, user.getEmail(), user.getRole()});

                return user;
    }

     /**
     * Deletes a User entity by its unique persistent identifier (ID)
     *
     * @param userId The unique ID of the user to delete
     * @return true if a user was found and deleted, false otherwise.
     */
    @Override
    public synchronized boolean deleteById(int userId) {
         if (userId <= 0) {
             LOGGER.log(Level.WARNING,
             "Attempted to delete user with invalid ID: {0}", userId);
             return false;
         }
        // Returns the value or null
        User removedUser = users.remove(userId);
        if (removedUser != null) {
            // Persist the changes
            saveData();
            return true;
        } else {
            LOGGER.log(Level.WARNING, "User ID {0} not found for deletion.",
            userId);
            return false;
        }
    }

    /**
     * Deletes a user by passing the User object instance
     * @param user The User entity to delete 
     * @return true if the user was deleted, false otherwise.
     * @throws NullPointerException if user is null.
     */
    @Override
    public synchronized boolean delete(User user) {
        Objects.requireNonNull(user, "User to delete cannot be null.");
        if (user.getUserID() <= 0) {
             LOGGER.log(Level.WARNING,
             "Attempted to delete user with invalid ID via object: {0}",
             user);
             return false;
        }
        
        return deleteById(user.getUserID());
    }

  

    /**
     * Loads user data from the specified storage file.
     * This method is synchronized to prevent conflicts during loading.
     */
    @SuppressWarnings("unchecked")
    private synchronized void loadData() {
        File file = new File(storageFilePath);
        if (!file.exists() || file.length() == 0) {
            LOGGER.log(Level.INFO,"User data file not found or empty ({0}). " +
            "Starting with empty repository.", storageFilePath);
            ensureDirectoryExists(file);
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ObjectInputStream ois = new ObjectInputStream(bis)) {

            Object readObject = ois.readObject();
            if (readObject instanceof Map) {
                users.clear();
                Map<?, ?> rawMap = (Map<?, ?>) readObject;
                rawMap.forEach((key, value) -> {
                    if (key instanceof Integer && value instanceof User) {
                        users.put((Integer) key, (User) value);
                    } else {
                         LOGGER.log(Level.WARNING,"Skipping invalid entry " +
                         "during load: Key type {0}, Value type {1}",
                         new Object[]{key != null ? key.getClass()
                            .getName() : "null",
                            value != null ? value.getClass()
                            .getName() : "null"});
                    }
                });
                LOGGER.log(Level.INFO,
                "Successfully loaded {0} user entries from: {1}",
                new Object[]{users.size(), storageFilePath});
            } else {
                LOGGER.log(Level.SEVERE,
                "User data file ({0}) does not contain a valid Map.",
                storageFilePath);
            }
        } catch (FileNotFoundException e) {
             LOGGER.log(Level.SEVERE, "User data file not found for loading: "
             + storageFilePath, e);
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
             LOGGER.log(Level.SEVERE, "Failed to load user data from file ("
             + storageFilePath + "). Data might be corrupted or class " +
             "versions incompatible.", e);
        }
    }

     /**
     * Saves the current state of the users map to the storage file
     */
    private synchronized void saveData() {
        File file = new File(storageFilePath);
        ensureDirectoryExists(file);

        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(new ConcurrentHashMap<>(users));
            LOGGER.log(Level.FINE, "User data saved successfully to: {0}",
            storageFilePath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "CRITICAL ERROR: Failed to save user " +
            "data to file ({0}). Data loss may occur.",
            new Object[]{storageFilePath, e});
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
             "Attempting to create directory for user data: {0}",
             parentDir.getAbsolutePath());
             if (parentDir.mkdirs()) {
                  LOGGER.log(Level.INFO,
                  "Successfully created directory: {0}",
                  parentDir.getAbsolutePath());
             } else {
                  LOGGER.log(Level.SEVERE,
                  "Failed to create directory for user data: {0}",
                  parentDir.getAbsolutePath());
             }
         }
     }
}