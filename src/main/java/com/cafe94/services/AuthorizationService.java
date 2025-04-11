package com.cafe94.services;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cafe94.domain.User;
import com.cafe94.enums.Permission;
import com.cafe94.enums.UserRole;
import com.cafe94.permission.PermissionLoader;

/**
 * Service responsible for checking user permissions based on their
 * @author Adigun LAteef
 * @version 1.0
 */
public interface AuthorizationService {

    static final Logger LOGGER =
    Logger.getLogger(AuthorizationService.class.getName());
    /**
     * Checks if a user has the required permission to perform an action and
     * returns false if user, permission, or user's role is null.
     *
     * @param user       The user attempting the action
     * @param permission The {@link Permission} enum constant required for
     * the action
     * @return {@code true} if the user is not null, has a valid role,
     * the permission is not null, and their role has the specified permission
     * according to PermissionLoader, {@code false} otherwise.
     */
    public default boolean hasPermission(User user, Permission permission) {
        if (user == null || permission == null) {
            LOGGER.log(Level.FINER,
            "hasPermission check failed (null user or permission).");
            return false;
        }
        UserRole role = user.getRole();
        if (role == null) {
            LOGGER.log(Level.WARNING, "hasPermission check failed for User " +
            "{0}: Role is null.", user.getUserID());
            return false;
        }

        // Delegate check to PermissionLoader
        boolean hasPerm = PermissionLoader.hasPermission(role, permission);

        if (!hasPerm) {
             LOGGER.log(Level.FINER, "Permission check failed for User {0} " +
             "(Role: {1}) -> Required: {2}",
                        new Object[]{user.getUserID(), role, permission});
        } else {
             LOGGER.log(Level.FINEST, "Permission check succeeded for User " +
             "{0} (Role: {1}) -> Required: {2}",
             new Object[]{user.getUserID(), role, permission});
        }
        return hasPerm;
    }

    /**
     * Checks permission and throws a SecurityException if unauthorized
     *
     * @param user       The user attempting the action
     * @param permission The required {@link Permission} enum constant
     * @throws SecurityException if the user is null or does not
     * have the required permission.
     * @throws NullPointerException if permission is null.
     */
    public default void checkPermission(User user, Permission permission)
    throws SecurityException {
         Objects.requireNonNull(permission,
         "Permission enum cannot be null for checkPermission.");

        // Check for null user explicitly first
        if (user == null) {
             // Log this specific failure case
             LOGGER.log(Level.WARNING, "Permission check failed - " +
             "No authenticated user provided for permission: {0}", permission);
             throw new SecurityException("Authentication required to " +
             "perform this action (permission: " + permission + ").");
        }

        if (!hasPermission(user, permission)) {
             // Log the authorization failure details
             LOGGER.log(Level.WARNING, "Authorization failed - " +
             "User ''{0}'' (ID: {1}, Role: {2}) lacks required permission: " +
             "''{3}''",
                        new Object[]{user.getEmail(), user.getUserID(),
                            user.getRole(), permission});
             throw new SecurityException("User '" + user.getEmail() +
             "' (Role: " + user.getRole() + ") does not have required " +
             "permission: '" + permission + "'");
        }

        // If permission is granted, log success
         LOGGER.log(Level.FINE, "Permission check passed for User {0} " +
         "(Role: {1}) -> Required: {2}",
         new Object[]{user.getUserID(), user.getRole(), permission});
    }
}