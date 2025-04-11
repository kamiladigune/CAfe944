package com.cafe94.dto;

import java.io.Serializable;
import java.util.Objects;

import com.cafe94.util.ValidationUtils;

/**
 * Data Transfer Object for updating staff details.
 * @author Adigun Lateef
 * @version 1.0
 */
// Added final keyword for immutability guarantee
public final class StaffDetailsDto implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String firstName;
    private final String lastName;
    private final String email;

    /**
     * Constructs an immutable StaffDetailsDto
     *
     * @param firstName Staff member's first name
     * @param lastName Staff member's last name
     * @param email Staff member's email address
     * @throws NullPointerException if firstName, lastName, or email is null.
     * @throws IllegalArgumentException if firstName, lastName,
     * or email is blank.
     */
    public StaffDetailsDto(String firstName, String lastName, String email) {
        this.firstName = ValidationUtils.requireNonBlank(firstName,
        "DTO First name");
        this.lastName = ValidationUtils.requireNonBlank(lastName,
        "DTO Last name");
        this.email = ValidationUtils.requireNonBlank(email,
        "DTO Email");
    }

    // Getters

    /**
     * @return Staff First name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * @return Staff last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * @return Staff email
     */
    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "StaffDetailsDto{" +
               "firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               ", email='" + email + '\'' + '}';
    }

    /**
     * Compares this DTO to another object for equality based on all fields.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaffDetailsDto that = (StaffDetailsDto) o;
        return Objects.equals(firstName, that.firstName) &&
               Objects.equals(lastName, that.lastName) &&
               Objects.equals(email, that.email);
    }

    /**
     * Generates a hash code based on all fields.
     */
    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, email);
    }
}