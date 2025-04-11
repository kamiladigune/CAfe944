package com.cafe94.services;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cafe94.domain.User;
import com.cafe94.enums.Permission;
import com.cafe94.enums.UserRole;
import com.cafe94.permission.PermissionLoader;

/**
 * Implementation of {@link AuthorizationService} that checks user permissions
 * based on their role using the centralized {@link PermissionLoader}
 * @author Adigun Lateef
 * @version 1.0
 */
public class AuthorizationServiceImpl implements AuthorizationService {

    // Use standard Java logger
    private static final Logger LOGGER =
    Logger.getLogger(AuthorizationServiceImpl.class.getName());

    public static Logger getLOGGER() {
        return LOGGER;
    }

    public AuthorizationServiceImpl() {
        
        LOGGER.log(Level.CONFIG, "AuthorizationServiceImpl initialized.");
    }

    /**
     * Checks if the given user has the specified permission and returns false
     * if user, permission, or user's role is null.
     */
    @Override
    public boolean hasPermission(User user, Permission permission) {
         if (user == null || permission == null) {
             LOGGER.log(Level.FINER,
             "hasPermission check failed (null user or permission).");
            return false;
        }
        UserRole role = user.getRole();
        if (role == null) {
            LOGGER.log(Level.WARNING,
            "hasPermission check failed for User {0}: Role is null.",
            user.getUserID());
            return false;
        }

        // Delegate check to PermissionLoader
        boolean hasPerm = PermissionLoader.hasPermission(role, permission);

        if (!hasPerm) {
             LOGGER.log(Level.FINER,"Permission check failed for User {0} " +
             "(Role: {1}) -> Required: {2}",
             new Object[]{user.getUserID(), role, permission});
        } else {
             LOGGER.log(Level.FINEST,
             "Permission check succeeded for User {0} (Role: {1}) -> " +
             "Required: {2}",
             new Object[]{user.getUserID(), role, permission});
        }
        return hasPerm;
    }

    /**
     * Checks permission and throws a SecurityException if unauthorized.
     * Ensures permission enum itself is not null.
     */
    @Override
    public void checkPermission(User user, Permission permission)
    throws SecurityException {
         Objects.requireNonNull(permission,
         "Permission enum cannot be null for checkPermission.");

        if (user == null) {
             LOGGER.log(Level.WARNING,
             "Permission check failed - No authenticated user provided for " +
             "permission: {0}", permission);
             throw new SecurityException("Authentication required to " +
             "perform this action (permission: " + permission + ").");
        }

        if (!this.hasPermission(user, permission)) {
             // Log the authorization failure details
             LOGGER.log(Level.WARNING, "Authorization failed - User ''{0}'' " +
             "(ID: {1}, Role: {2}) lacks required permission: ''{3}''",
                        new Object[]{user.getEmail(), user.getUserID(),
                            user.getRole(), permission});
             throw new SecurityException("User '" + user.getEmail() +
             "' (Role: " + user.getRole() +
             ") does not have required permission: '" + permission + "'");
        }

        // If permission is granted, log success
         LOGGER.log(Level.FINE, "Permission check passed for User {0} (Role: {1}) -> Required: {2}",
                    new Object[]{user.getUserID(), user.getRole(), permission});
    }
}