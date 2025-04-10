package com.cafe94.permission;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cafe94.enums.Permission;
import static com.cafe94.enums.Permission.*;
import com.cafe94.enums.UserRole;
import static com.cafe94.enums.UserRole.CHEF;
import static com.cafe94.enums.UserRole.CUSTOMER;
import static com.cafe94.enums.UserRole.DRIVER;
import static com.cafe94.enums.UserRole.MANAGER;
import static com.cafe94.enums.UserRole.WAITER;

/**
 * Loads and provides statically defined permissions
 * associated with each {@link UserRole}.
 * @author Adigun Lateef
 * @version 1.0
 */
public class PermissionLoader {

    private static final Logger LOGGER =
    Logger.getLogger(PermissionLoader.class.getName());
    private static final Map<UserRole, Set<Permission>> permissions;
    static {
        Map<UserRole, Set<Permission>> map = new EnumMap<>(UserRole.class);


        // Customer Permissions
        map.put(CUSTOMER, EnumSet.of(
                VIEW_MENU,
                PLACE_ORDER,
                TRACK_OWN_ORDER,
                CANCEL_OWN_ORDER,
                REQUEST_BOOKING,
                CANCEL_OWN_BOOKING,
                VIEW_OWN_PROFILE,
                UPDATE_OWN_PROFILE
        ));
        LOGGER.log(Level.CONFIG, "Loaded permissions for CUSTOMER role.");

        // Chef Permissions
        map.put(CHEF, EnumSet.of(
                VIEW_ORDERS,
                UPDATE_ORDER_STATUS_PREPARING,
                UPDATE_ORDER_STATUS_COMPLETED,
                MANAGE_MENU_ITEMS,
                SET_DAILY_SPECIAL

        ));
         LOGGER.log(Level.CONFIG, "Loaded permissions for CHEF role.");

        // Waiter Permissions
        map.put(WAITER, EnumSet.of(
                VIEW_ORDERS,
                TAKE_EAT_IN_ORDER,
                UPDATE_ORDER_STATUS_SERVED,
                UPDATE_ORDER_STATUS_COLLECTED,
                APPROVE_BOOKING,
                REJECT_BOOKING,
                MANAGE_TABLE_STATUS,
                ASSIGN_TABLE_BOOKING,
                APPROVE_DELIVERY,
                ASSIGN_DRIVER,
                CANCEL_ANY_ORDER
        ));
         LOGGER.log(Level.CONFIG, "Loaded permissions for WAITER role.");

        // Driver Permissions
        map.put(DRIVER, EnumSet.of(
                UPDATE_ORDER_STATUS_OUT_FOR_DELIVERY,
                UPDATE_ORDER_STATUS_DELIVERED
        ));
         LOGGER.log(Level.CONFIG, "Loaded permissions for DRIVER role.");
        map.put(MANAGER, EnumSet.allOf(Permission.class));
        LOGGER.log(Level.CONFIG, "Loaded all permissions for MANAGER role.");

        // Prevent changes after initialization.
        permissions = Collections.unmodifiableMap(map);
        LOGGER.log(Level.INFO,
        "Permission map initialized and made unmodifiable.");
    }

    /**
     * Retrieves the immutable set of permissions associated with a given UserRole.
     *
     * @param roleType The {@link UserRole} enum value (can be null).
     * @return An unmodifiable {@code Set<Permission>}. Returns an empty set if the
     * roleType is null or has no permissions explicitly defined
     */
    public static Set<Permission> getPermissions(UserRole roleType) {
        return permissions.getOrDefault(roleType, Collections.emptySet());
    }

    /**
     * Checks if a given role possesses a specific permission.
     *
     * @param roleType The {@link UserRole} enum value
     * @param permission The {@link Permission} enum constant to check for
     * @return {@code true} if the roleType is not null, the permission
     * is not null, and the role has the specified permission
     * according to the map;
     */
    public static boolean hasPermission(UserRole roleType,
    Permission permission) {
        if (roleType == null || permission == null) {
            return false;
        }
        return getPermissions(roleType).contains(permission);
    }

    // Prevent instantiation of this utility class.
    private PermissionLoader() {
        throw new IllegalStateException(
            "Utility class PermissionLoader cannot be instantiated.");
    }
}
