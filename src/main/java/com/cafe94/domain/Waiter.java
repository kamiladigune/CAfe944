// File: src/main/java/com/cafe94/domain/Waiter.java
package com.cafe94.domain;

import java.io.Serializable;

import com.cafe94.enums.UserRole;

/**
 * Represents a Waiter user in the Cafe94 system.
 * This is a concrete class inheriting properties from {@link Staff}.
 * @author Adigun Lateef
 * @version 1.0
 */
public class Waiter extends Staff implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new Waiter object.
     *
     * @param userID            Unique User ID  or existing ID.
     * @param firstName         Waiter's first name
     * @param lastName          Waiter's last name
     * @param email             Waiter's email address
     * @param hashedPassword    Waiter's hashed password
     * @param staffId           The unique staff identifier
     */
    public Waiter(int userID, String firstName, String lastName, String email, String hashedPassword, String staffId) {
    
        super(userID, firstName, lastName, UserRole.WAITER, email, hashedPassword, staffId);
    }
}