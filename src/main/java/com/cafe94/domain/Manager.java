// File: src/main/java/com/cafe94/domain/Manager.java
package com.cafe94.domain;

import java.io.Serializable;

import com.cafe94.enums.UserRole;

/**
 * Represents a Manager user in the Cafe94 system.
 * This is a concrete class inheriting properties from {@link Staff}
 */
public class Manager extends Staff implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new Manager object.
     *
     * @param userID            Unique User ID  or existing ID.
     * @param firstName         Manager's first name
     * @param lastName          Manager's last name
     * @param email             Manager's email address
     * @param hashedPassword    Manager's hashed password
     * @param staffId           The unique staff identifier
     */
    public Manager(int userID, String firstName, String lastName, String email,
    String hashedPassword, String staffId) {
        super(userID, firstName, lastName, UserRole.MANAGER, email,
        hashedPassword, staffId);
    }
}