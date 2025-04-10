package com.cafe94.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cafe94.dto.StaffDetailsDto;
import com.cafe94.enums.UserRole;
import com.cafe94.util.ValidationUtils;


/**
 * Abstract base class for all staff members
 * @author Adigun Lateef
 * @version 1.0
 */
public abstract class Staff extends User implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER =
    Logger.getLogger(Staff.class.getName());
    private final String staffId;
    private Integer associatedCustomerUserId;

    /**
     * Constructor for Staff subclasses. Validates input parameters.
     */
    protected Staff(int userID, String firstName, String lastName,
    UserRole role, String email, String hashedPassword, String staffId) {
        super(userID, firstName, lastName, role, email, hashedPassword);

        if (!role.isStaffRole()) {
            throw new IllegalArgumentException("Staff user cannot have a " +
            "non-staff role: " + role);
        }
        this.staffId = ValidationUtils.requireNonBlank(staffId, "Staff ID");
    }

    // Getters

    /**
     * @return The staff ID
     */
    public String getStaffId() {
        return staffId;
    }

    public Integer getAssociatedCustomerUserId() {
        return associatedCustomerUserId;
    }

    // Setters

    /**
     * Sets or clears the associated customer user ID link
     * @param customerUserId The ID of the customer user to associate,
     * or null to remove association.
     * @throws IllegalArgumentException if customerUserId is provided
     * but not positive.
     */
    public void linkAssociatedCustomer(Integer customerUserId) {
         LOGGER.log(Level.FINE, "Setting associated customer ID to {0} " +
         "for staff ID {1}", new Object[]{customerUserId, this.getUserID()});
        this.setAssociatedCustomerUserId(customerUserId);
    }


    /**
     * Updates the staff member's mutable details (name, email, schedule) from a DTO.
     * Internally calls the necessary protected or public setters.
     * Assumes uniqueness check for email has been performed by the calling service if necessary.
     *
     * @param details The DTO containing new details (must not be null).
     * @throws NullPointerException if details is null.
     * @throws IllegalArgumentException if data within DTO is invalid (handled by setters).
     */
    public void updateDetails(StaffDetailsDto details) {
        Objects.requireNonNull(details, "StaffDetails DTO cannot be null " +
        "for updateDetails.");
        LOGGER.log(Level.INFO, "Updating details for staff ID {0} using DTO",
        this.getUserID());
        this.setFirstName(details.getFirstName());
        this.setLastName(details.getLastName());
        this.setEmail(details.getEmail());
    }

    // Setters

    

    /**
     * Sets the associated customer user ID link internally.
     * @param customerUserId The ID of the customer user, or null.
     * @throws IllegalArgumentException if customerUserId is provided but not positive.
     */
    protected void setAssociatedCustomerUserId(Integer customerUserId) {
         if (customerUserId != null && customerUserId <= 0) {
             throw new IllegalArgumentException("Associated Customer User " +
             "ID must be positive if provided. Value: " + customerUserId);
         }
         if (!Objects.equals(this.associatedCustomerUserId, customerUserId)) {
              LOGGER.log(Level.FINEST, "Internal: Setting associated " +
              "customer ID for staff {0} to {1}",
              new Object[]{this.getUserID(), customerUserId});
              this.associatedCustomerUserId = customerUserId;
         }
    }

    // Standard Methods

    /** String representaion of the objects
     * @return a string reprentation of the Staff objects
     */
    @Override
    public String toString() {
        return super.toString().replaceFirst("]$", "")
               + ", StaffID='" + staffId + '\''
               + ", CustomerLink="
               + (associatedCustomerUserId != null ?
               associatedCustomerUserId : "None") + ']';
    }

    /** Compares Staff objects for equality and extends User equality check.
    */
    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        if (!(o instanceof Staff)) {
            return false;
        }
        Staff staff = (Staff) o;
        return Objects.equals(staffId, staff.staffId)
        && Objects.equals(associatedCustomerUserId,
        staff.associatedCustomerUserId);
    }

    /** Generates a hash code for the Staff object
    */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), staffId,
        associatedCustomerUserId);
    }
}
