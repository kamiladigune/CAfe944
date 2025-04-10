// File: src/main/java/com/cafe94/util/SessionManager.java (Conceptual)
package com.cafe94.util;

import com.cafe94.domain.User;

/**
 * Manages the current user session state
 */
public class SessionManager {
    private User currentUser;
    private static SessionManager instance;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    protected void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        System.out.println("Session started for: " +
        (currentUser != null ? currentUser.getEmail() + " as " +
        currentUser.getRole() : "null"));
    }

    // Called by logOut() in UserService
    public void clearSession() {
        User oldUser = this.currentUser;
        this.currentUser = null;
        if (oldUser != null) {
             System.out.println("Session cleared for user: " +
             oldUser.getEmail());
        } else {
             System.out.println("Session cleared (no user was logged in).");
        }
    }
}