// File: src/main/java/com/cafe94/domain/Customer.java
package com.cafe94.domain;
import java.util.Objects;

import com.cafe94.enums.UserRole;
import com.cafe94.util.ValidationUtils;

/**
 * Represents a customer user in the Cafe94 system.
 * Extends the base {@link User} class
 * @author Adigun Lateef
 * @version 1.0
 */
public class Customer extends User {

    private static final long serialVersionUID = 1L;

    private String address;
    private String phoneNumber;
    private Integer associatedStaffUserId;

    /**
     * Constructs a validated Customer entity.
     *
     * @param userID         Temporary identifier or existing persistent ID.
     * @param firstName      Customer's first name.
     * @param lastName       Customer's last name.
     * @param email          Customer's unique email address.
     * @param hashedPassword Customer's secure credential hash.
     * @param address        Customer's physical address.
     * @param phoneNumber    Customer's phone number.
     */
    public Customer(int userID, String firstName, String lastName,
    String email, String hashedPassword, String address, String phoneNumber) {
        super(userID, firstName, lastName, UserRole.CUSTOMER, email,
        hashedPassword);

        // Validate and set customer-specific fields
        this.address = ValidationUtils.requireNonBlank(address,
        "Customer address");

        this.phoneNumber = ValidationUtils.requireNonBlank(phoneNumber,
        "Customer phone number");
    }


    // Getters

    /**
     * @return The customer's address.
     */
    public String getAddress() {
        return address;
    }

    /**
     * @return The customer's phone number.
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * @return The user ID of the associated staff member,
     * or null if there is no association.
     */
    public Integer getAssociatedStaffUserId() {
        return associatedStaffUserId;
    }

    // Setters
    /**
     * Sets or updates the customer's address.
     * @param address The new address.
     */
    public void setAddress(String address) {
        this.address = ValidationUtils.requireNonBlank(address,
        "Customer address");
    }

    /**
     * Sets or updates the customer's phone number.
     * @param phoneNumber The new phone number.
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = ValidationUtils.requireNonBlank(phoneNumber,
        "Customer phone number");
    }

    /**
     *  Sets associated staff user ID
     * @param staffUserId The ID of the staff member to associate.
     */
    private void associatedStaffUserId(Integer staffUserId) {
        this.associatedStaffUserId = staffUserId;
     }
    /**
     * Sets or clears the associated staff user ID link
     * @param staffUserId The ID of the staff member to associate,
     * or null to remove the association.
     * @throws IllegalArgumentException if staffUserId is provided
     * but is not positive.
     */
    public void setAssociatedStaffUserId(Integer staffUserId) {
        if (staffUserId != null && staffUserId <= 0) {
            throw new IllegalArgumentException("Associated Staff User ID " +
            "must be positive if provided. Value: " + staffUserId);
        }
        this.associatedStaffUserId = staffUserId;
    }
    
    // Standard Methods

    /**
     * String representaion of the objects
     * @return a string reprentation of the booking objects
     */
    @Override
    public String toString() {
        return super.toString().replaceFirst("]$", "")
               + ", Address='" + address + '\''
               + ", Phone='" + (phoneNumber != null ? phoneNumber : "N/A") +
               '\'' + ", StaffLink=" + (associatedStaffUserId != null ?
               associatedStaffUserId : "None") + ']';
    }

    /**
     * Links associated StaffID
     * @param staffUserId The associated staffID
     */
    public void linkAssociatedStaff(Integer staffUserId) {
        this.associatedStaffUserId(staffUserId);
    }

    /**
     * Compares Customer objects for equality.
     * @param o The object to compare with.
     * @return true if the objects are considered equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        // Cast 'o' to Customer and compare specific fields
        Customer customer = (Customer) o;
        return Objects.equals(address, customer.address) &&
               Objects.equals(phoneNumber, customer.phoneNumber) &&
               Objects.equals(associatedStaffUserId,
               customer.associatedStaffUserId);
    }

    /**
     * Generates a hash code for the Customer object.
     * @return The hash code for this object.
     */
    @Override
    public int hashCode() {
        // Combine hash code from superclass with customer-specific fields
        return Objects.hash(super.hashCode(), address, phoneNumber,
        associatedStaffUserId);
    }
}