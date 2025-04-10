package com.cafe94.domain;

import java.io.Serializable;

import com.cafe94.enums.UserRole;

/**
 * Represents a Driver user in the Cafe94 system, responsible for
 * delivering orders.
 * This is a concrete class inheriting properties from {@link Staff}.
 *@author Adigun Lateef
 @version 1.0
 */
public class Driver extends Staff implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new Driver object.
     *
     * @param userID            Unique User ID or existing ID.
     * @param firstName         Driver's first name
     * @param lastName          Driver's last name
     * @param email             Driver's email address
     * @param hashedPassword    Driver's hashed password
     * @param staffId           The unique staff identifier
     */
    public Driver(int userID, String firstName, String lastName, String email,
                  String hashedPassword, String staffId) {
        // Explicitly call the Staff constructor, setting the role to
        // UserRole.DRIVER
        super(userID, firstName, lastName, UserRole.DRIVER, email,
        hashedPassword, staffId);
    }
        
}
