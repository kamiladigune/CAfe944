
package com.cafe94.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.cafe94.domain.Chef;
import com.cafe94.domain.Customer;
import com.cafe94.domain.Driver;
import com.cafe94.domain.Manager;
import com.cafe94.domain.Staff;
import com.cafe94.domain.User;
import com.cafe94.domain.Waiter;
import com.cafe94.dto.StaffDetailsDto;
import static com.cafe94.enums.Permission.MANAGE_STAFF;
import com.cafe94.enums.UserRole;
import com.cafe94.persistence.IUserRepository;
import com.cafe94.util.PasswordHasher;
import com.cafe94.util.SessionManager;
import com.cafe94.util.ValidationUtils;

/**
 * Implementation of IUserService
 * @author Adigun Lateef
 * @version 1.0
 */
public class UserService implements IUserService {

    private static final Logger LOGGER =
    Logger.getLogger(UserService.class.getName());

    private final IUserRepository userRepository;
    private final AuthorizationService authService;
    private final PasswordHasher passwordHasher;
    private final SessionManager sessionManager;

    /** Constructor for Dependency Injection. */
    public UserService(IUserRepository userRepository,
                           AuthorizationService authService,
                           PasswordHasher passwordHasher,
                           SessionManager sessionManager) {
        this.userRepository = Objects.requireNonNull(userRepository,
        "UserRepository cannot be null.");
        this.authService = Objects.requireNonNull(authService,
        "AuthorizationService cannot be null.");
        this.passwordHasher = Objects.requireNonNull(passwordHasher,
        "PasswordHasher cannot be null.");
        this.sessionManager = Objects.requireNonNull(sessionManager,
        "SessionManager cannot be null.");
    }

    @Override
    public List<User> login(String email, String rawPassword) {
        ValidationUtils.requireNonBlank(email, "Login email");
        ValidationUtils.requireNonBlank(rawPassword, "Login password");
        LOGGER.log(Level.INFO, "Attempting login for email: {0}", email);

        List<User> potentialProfiles = userRepository.findByEmail(email);

        if (potentialProfiles.isEmpty()) {
            LOGGER.log(Level.WARNING, "Login failed - Email not found: {0}",
            email);
            return Collections.emptyList();
        }

        List<User> authenticatedProfiles = new ArrayList<>();
        for (User profile : potentialProfiles) {
            String storedHash = profile.getHashedPassword();
            if (passwordHasher.verify(rawPassword, storedHash)) {
                authenticatedProfiles.add(profile);
            } else {
                LOGGER.log(Level.FINE, "Incorrect password attempt for " +
                "profile ID: {0}, Email: {1}", new Object[]{profile.getUserID(),
                    email});
            }
        }

        if (authenticatedProfiles.isEmpty()) {
            LOGGER.log(Level.WARNING,
            "Login failed - Incorrect password for email: {0}", email);
        } else {
             LOGGER.log(Level.INFO, "Login successful for email {0}, {1} " +
             "profile(s) authenticated.", new Object[]{email,
                authenticatedProfiles.size()});
        }
        return authenticatedProfiles;
    }

    /**
     * Logs out the currently authenticated user
     */
    @Override
    public void logout() {
        LOGGER.log(Level.INFO, "Logging out current user.");
        sessionManager.clearSession();
    }
    /**
     * Registers a new customer account in the system
     */
    @Override
    public Customer registerCustomer(String firstName, String lastName,
    String email, String rawPassword, String address, String phoneNumber) {
        ValidationUtils.requireNonBlank(firstName, "First Name");
        ValidationUtils.requireNonBlank(lastName, "Last Name");
        ValidationUtils.requireNonBlank(email, "Email");
        ValidationUtils.requireNonBlank(rawPassword, "Password");
        ValidationUtils.requireNonBlank(address, "Address");

        List<User> existing = userRepository.findByEmail(email);
        if (existing.stream().anyMatch(u -> u instanceof Customer)) {
            LOGGER.log(Level.WARNING, "Registration failed: Email {0} " +
            "already registered to a customer.", email);
            throw new IllegalArgumentException("Email address '" + email +
            "' is already registered to a customer.");
        }

        String hashedPassword = passwordHasher.hash(rawPassword);
        Customer newCustomer = new Customer(0, firstName, lastName,
        email, hashedPassword, address, phoneNumber);

        User savedUser = savePrimaryUser(newCustomer);
        Customer primaryCustomer = (Customer) savedUser;
        Optional<User> counterpartOpt =
        findCounterpartProfile(primaryCustomer);
        if (counterpartOpt.isPresent()) {
            primaryCustomer =
            (Customer) linkProfilesIfNecessary(primaryCustomer,
            counterpartOpt.get());
        }

        Customer finalCustomer =
        userRepository.findById(primaryCustomer.getUserID())
        .filter(Customer.class::isInstance)
        .map(Customer.class::cast)
        .orElse(primaryCustomer);

        LOGGER.log(Level.INFO,
        "Registered new customer: ID {0}, Email {1}",
        new Object[]{finalCustomer.getUserID(), finalCustomer.getEmail()});
        return finalCustomer;
    }
    /**
     * Hires a new staff member and creates their profile
     */
    @Override
    public Staff hireStaff(String firstName, String lastName, String email,
    String rawPassword, UserRole role, String staffId, double initialHours,
    User callingUser) {
        Objects.requireNonNull(callingUser,
        "Calling user cannot be null for hiring staff.");
        authService.checkPermission(callingUser, MANAGE_STAFF);

        // Check for uniqueness
        List<User> existingByEmail = userRepository.findByEmail(email);
        if (existingByEmail.stream().anyMatch(u -> u instanceof Staff)) {
             LOGGER.log(Level.WARNING, "Hire staff failed: Email {0} " +
             "already registered to staff.", email);
            throw new IllegalArgumentException("Email address '" + email +
             "' is already registered to a staff member.");
        }

        String hashedPassword = passwordHasher.hash(rawPassword);
        Staff newStaff = createStaffInstance(0, firstName, lastName, email,
        hashedPassword, role, staffId, initialHours);

        User savedUser = savePrimaryUser(newStaff);
        Staff primaryStaff = (Staff) savedUser;
        Optional<User> counterpartOpt = findCounterpartProfile(primaryStaff);
        if (counterpartOpt.isPresent()) {
            primaryStaff =
            (Staff) linkProfilesIfNecessary(primaryStaff,counterpartOpt.get());
        }

        Staff finalStaff = userRepository.findById(primaryStaff.getUserID())
                                     .filter(Staff.class::isInstance)
                                     .map(Staff.class::cast)
                                     .orElse(primaryStaff);

        LOGGER.log(Level.INFO, "Manager ID {0} hired new staff: ID {1}, " +
        "Email {2}, Role {3}",
                   new Object[]{callingUser.getUserID(),
                    finalStaff.getUserID(), finalStaff.getEmail(),
                    finalStaff.getRole()});
        return finalStaff;
    }

    /**
     * Updates details for an existing staff member
     *
     * @param staffUserId    The ID of the staff member to update.
     * @param details        A DTO containing the updated details.
     * @param callingUser    The authenticated user performing the action
     * @return The updated Staff object.
     * @throws SecurityException if callingUser is not authorized.
     * @throws NoSuchElementException if staffUserId is not found or is not
     * Staff
     * @throws IllegalArgumentException if email in details is already in use
     * by another user
     * @throws NullPointerException if details or callingUser is null
     */
    @Override
    public Staff updateStaffDetails(int staffUserId, StaffDetailsDto details,
    User callingUser) {
        Objects.requireNonNull(callingUser,
        "Calling user cannot be null for updating staff details.");
        Objects.requireNonNull(details,
        "StaffDetails DTO cannot be null.");
        authService.checkPermission(callingUser, MANAGE_STAFF);

        Staff staffToUpdate = findStaffByIdOrThrow(staffUserId);
        String newEmail = details.getEmail();

        // Check email uniqueness before calling update
        if (!newEmail.equalsIgnoreCase(staffToUpdate.getEmail())) {
            LOGGER.log(Level.INFO,
            "Checking email uniqueness before updating staff ID {0}",
            staffUserId);
            List<User> existingUsersWithNewEmail =
            userRepository.findByEmail(newEmail);
            if (existingUsersWithNewEmail.stream().anyMatch(u -> u.getUserID()
            != staffUserId)) {
                 LOGGER.log(Level.WARNING,
                 "Update staff failed: Email {0} is already in use by " +
                 "another user.", newEmail);
                throw new IllegalArgumentException("Email address '" +
                newEmail + "' is already in use by another user.");
            }
        }

       
        staffToUpdate.updateDetails(details);
        LOGGER.log(Level.INFO,"Called updateDetails on Staff object ID: {0}",
        staffUserId);

        // Save the updated staff member
        Staff updatedStaff = (Staff) userRepository.save(staffToUpdate);
        LOGGER.log(Level.INFO, "Manager ID {0} finished updating details " +
        "for staff ID: {1}", new Object[]{callingUser.getUserID(),
            staffUserId});

        return updatedStaff;
    }
    /**
     * Removes a staff member from the system
     */
     @Override
    public boolean removeStaff(int staffUserId, User callingUser) {
        Objects.requireNonNull(callingUser,
        "Calling user cannot be null for removing staff.");
        authService.checkPermission(callingUser, MANAGE_STAFF);

        if (staffUserId <= 0) {
             throw new IllegalArgumentException(
                "Staff User ID must be positive for removal.");
        }
        if (staffUserId == callingUser.getUserID()) {
            LOGGER.log(Level.WARNING,
            "Manager ID {0} attempted self-removal.",
            callingUser.getUserID());
            throw new SecurityException("Manager cannot remove themselves.");
        }

        Staff staffToRemove = findStaffByIdOrThrow(staffUserId);

        Integer customerId = staffToRemove.getAssociatedCustomerUserId();
        if (customerId != null) {
            userRepository.findById(customerId)
                .filter(Customer.class::isInstance)
                .map(Customer.class::cast)
                .ifPresent(c -> {
                    if (Objects.equals(c.getAssociatedStaffUserId(),
                    staffToRemove.getUserID())) {
                        LOGGER.log(Level.INFO,
                        "Unlinking Customer ID {0} from removed Staff ID {1}",
                        new Object[]{customerId, staffUserId});
                        c.linkAssociatedStaff(null);
                        userRepository.save(c);
                    }
                });
        }

        boolean deleted = userRepository.deleteById(staffUserId);

        if (deleted) {
            LOGGER.log(Level.INFO,
            "Manager ID {0} removed staff member with ID: {1}",
            new Object[]{callingUser.getUserID(), staffUserId});
        } else {
            LOGGER.log(Level.SEVERE,
            "Failed to remove staff member with ID: {0} (repository error?)",
            staffUserId);
        }
        return deleted;
    }

    /**
     * Finds a user by their unique persistent ID
     */
    @Override
    public Optional<User> findUserById(int userId) {
        if (userId <= 0) {
            LOGGER.log(Level.FINER,
            "findUserById called with non-positive ID: {0}", userId);
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }



    /** Saves the primary user using the repository. */
    private User savePrimaryUser(User userToSave) {
        LOGGER.log(Level.FINE, "Attempting to save primary user: Email={0}, " +
        "ID={1}", new Object[]{userToSave.getEmail(), userToSave.getUserID()});
        User savedUser = userRepository.save(userToSave);
        if (savedUser == null || savedUser.getUserID() <= 0) {
             LOGGER.log(Level.SEVERE, "Failed to save primary user or " +
             "retrieve valid saved ID for email {0}", userToSave.getEmail());
             throw new RuntimeException("Failed to save user or get valid ID: "
             + userToSave.getEmail());
        }
        LOGGER.log(Level.INFO, "Primary user saved successfully: ID {0}, " +
        "Email {1}", new Object[]{savedUser.getUserID(),
            savedUser.getEmail()});
        return savedUser;
    }

    /** Finds a counterpart profile */
    private Optional<User> findCounterpartProfile(User savedUser) {
         String email = savedUser.getEmail();
         int savedUserId = savedUser.getUserID();
         LOGGER.log(Level.FINE, "Searching for counterpart profile for " +
         "email {0}, excluding ID {1}", new Object[]{email, savedUserId});

        List<User> existingUsers = userRepository.findByEmail(email).stream()
                .filter(u -> u.getUserID() != savedUserId)
                .collect(Collectors.toList());

        Optional<User> counterpartOpt;
        if (savedUser instanceof Customer) {
            counterpartOpt = existingUsers.stream()
            .filter(Staff.class::isInstance).findFirst();
        } else {
            counterpartOpt = existingUsers.stream()
            .filter(Customer.class::isInstance).findFirst();
        }
        LOGGER.log(Level.FINER, "Counterpart profile found? {0}",
        counterpartOpt.isPresent());
        return counterpartOpt;
    }

    private User linkProfilesIfNecessary(User primaryUser,
    User counterpartProfile) {
        LOGGER.log(Level.INFO,
        "Linking profiles for email {0} (Primary ID: {1}, " +
        "Counterpart ID: {2})",
                   new Object[]{primaryUser.getEmail(),
                    primaryUser.getUserID(),
                    counterpartProfile.getUserID()});

        boolean primaryNeedsSave = false;
        boolean counterpartNeedsSave = false;
        Customer customer;
        Staff staff;

        if (primaryUser instanceof Customer) {
            customer = (Customer) primaryUser;
            staff = (Staff) counterpartProfile;
        } else {
            staff = (Staff) primaryUser;
            customer = (Customer) counterpartProfile;
        }

        // Check and update link on Customer
        if (!Objects.equals(customer.getAssociatedStaffUserId(),
        staff.getUserID())) {
            LOGGER.log(Level.FINER,
            "   Link needs setting on Customer {0} -> Staff {1}",
            new Object[]{customer.getUserID(), staff.getUserID()});
            customer.linkAssociatedStaff(staff.getUserID());
            if (primaryUser == customer) {
                primaryNeedsSave = true;
            } else {
                counterpartNeedsSave = true;
            }
        }

        // Check and update link on Staff
        if (!Objects.equals(staff.getAssociatedCustomerUserId(),
        customer.getUserID())) {
            LOGGER.log(Level.FINER,
            "Link needs setting on Staff {0} -> Customer {1}",
            new Object[]{staff.getUserID(), customer.getUserID()});
            staff.linkAssociatedCustomer(customer.getUserID());
            if (primaryUser == staff) {
                primaryNeedsSave = true;
            } else {
                counterpartNeedsSave = true;
            }
        }

        User potentiallyUpdatedPrimaryUser = primaryUser;
        if (primaryNeedsSave) {
             LOGGER.log(Level.INFO,
             "Saving primary profile (ID: {0}) with updated link.",
             primaryUser.getUserID());
            potentiallyUpdatedPrimaryUser = userRepository.save(primaryUser);
        }
        if (counterpartNeedsSave) {
            LOGGER.log(Level.INFO,
            "Saving counterpart profile (ID: {0}) with updated link.",
            counterpartProfile.getUserID());
            userRepository.save(counterpartProfile);
        }

        if (!primaryNeedsSave && !counterpartNeedsSave) {
             LOGGER.log(Level.INFO,
             "Profiles for email {0} already correctly linked.",
             primaryUser.getEmail());
        }

        return potentiallyUpdatedPrimaryUser;
    }


    /** Finds a Staff member by ID or throws NoSuchElementException if not
     * found/not staff.
     */
    private Staff findStaffByIdOrThrow(int staffUserId) {
         if (staffUserId <= 0) {
             throw new IllegalArgumentException(
                "Staff User ID must be positive. Provided: " + staffUserId);
         }
        return userRepository.findById(staffUserId)
                .filter(Staff.class::isInstance)
                .map(Staff.class::cast)
                .orElseThrow(() -> {
                    LOGGER.log(Level.WARNING,
                    "Staff member not found with ID: {0}", staffUserId);
                    return new NoSuchElementException(
                        "Staff member not found with ID: " + staffUserId);
                });
    }

    /** Creates the correct Staff subclass instance based on the role */
    private Staff createStaffInstance(int id, String fn, String ln, String e,
    String hp, UserRole r, String sId, double hrs) {
         
           switch (r) {
               case MANAGER: return new Manager(id, fn, ln, e, hp, sId);
               case CHEF:    return new Chef(id, fn, ln, e, hp, sId);
               case WAITER:  return new Waiter(id, fn, ln, e, hp, sId);
               case DRIVER:  return new Driver(id, fn, ln, e, hp, sId);
               default:
                   LOGGER.log(Level.SEVERE,"Attempted to create staff " +
                   "instance for non-staff role: {0}", r);
                   throw new IllegalArgumentException(
                    "Cannot create staff instance for non-staff role: " + r);
           }
    }

}