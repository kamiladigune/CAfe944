package com.cafe94.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

import com.cafe94.enums.UserRole;
import com.cafe94.util.ValidationUtils;

/**
 * Abstract base class for all users (Customers and Staff)
 * in the Cafe94 system.
 * @author Adigun Lateef
 * @version 1.0
 */
public abstract class User implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private int userID;
    private String firstName;
    private String lastName;
    private final UserRole role;
    private String email;
    private String hashedPassword;

    /**
     * Constructor for User subclasses. Validates input parameters.
     *
     * @param userID         The user's ID
     * @param firstName      User's first name
     * @param lastName       User's last name
     * @param role           The user's role
     * @param email          User's email address
     * @param hashedPassword The user's pre-hashed password
     */
    protected User(int userID, String firstName, String lastName,
    UserRole role, String email, String hashedPassword) {
        Objects.requireNonNull(role, "User role cannot be null.");
        this.firstName = ValidationUtils.requireNonBlank(firstName,
        "User first name");
        this.lastName = ValidationUtils.requireNonBlank(lastName,
        "User last name");
        this.email = requireValidEmail(email,
        "User email");
        this.hashedPassword = ValidationUtils.requireNonBlank(hashedPassword,
        "User hashed password");
        this.userID = userID;
        this.role = role;
    }

    // Getters

    /**
     * @return The User ID
     */
    public int getUserID() {
        return userID;
    }

    /**
     * @return The user first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * @return The user last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * @return The user role
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * @return The user email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Provides access to the stored hashed password.
     * @return The stored hashed password.
     */
    public String getHashedPassword() {
        return hashedPassword;
    }

    // Setters
    /**
     * Sets the user's first name after validation
     * @param firstName The new first name
     */
    public void setFirstName(String firstName) {
        this.firstName = ValidationUtils.requireNonBlank(firstName,
        "User first name");
    }

    /**
     * Sets the user's last name after validation.
     * @param lastName The new last name
     */
    public void setLastName(String lastName) {
        this.lastName = ValidationUtils.requireNonBlank(lastName,
        "User last name");
    }

    /**
     * Sets the user's email address after validation.
     * @param email The new email address
     */
    public void setEmail(String email) {
        this.email = requireValidEmail(email,
        "User email");
    }

    /**
     * Sets the user's hashed password.
     * @param newHashedPassword The new hashed password (non-blank).
     */
    protected void setHashedPassword(String newHashedPassword) {
        this.hashedPassword = ValidationUtils.requireNonBlank(
            newHashedPassword, "User hashed password");
    }

    /**
     * Sets the user's persistent ID.
     * @param userID The persistent user ID.
     */
    public void setUserID(int userID) {
         if (this.userID > 0 && this.userID != userID) {
             System.err.println("Warning: Attempting to change an already " +
             "assigned User ID from " + this.userID + " to " + userID);
         }
         this.userID = userID;
    }


    /**
     * Validates that the provided email is non-blank and matches
     * the basic email pattern.
     *
     * @param email     The email string to validate.
     * @param fieldName The name of the field being validated
     * @return The validated email string.
     * @throws IllegalArgumentException if the email format is invalid.
     */
    private static String requireValidEmail(String email, String fieldName) {
        ValidationUtils.requireNonBlank(email, fieldName);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format for "
            + fieldName + ": " + email);
        }
        return email;
    }

    // Standard Methods

     /** String representaion of the objects
     * @return a string reprentation of the User objects
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
               "ID=" + userID +
               ", Name='" + firstName + " " + lastName + '\'' +
               ", Role=" + role +
               ", Email='" + email + '\'' + ']';
    }

    /**
     * Compares User objects for equality.
     * @param o The object to compare with.
     * @return true if the objects are considered equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        if (userID > 0 && other.userID > 0) {
            return userID == other.userID;
        }
        return Objects.equals(email, other.email) && Objects.equals(role, other.role);
    }

    /**
     * Generates a hash code for the User object
     * @return The hash code for this object.
     */
    @Override
    public int hashCode() {
        if (userID > 0) {
            return Objects.hash(userID);
        }
        return Objects.hash(email, role);
    }
}
