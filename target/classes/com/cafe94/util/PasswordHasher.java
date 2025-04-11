// File: src/main/java/com/cafe94/util/PasswordHasher.java
package com.cafe94.util;

/**
 * Interface for password hashing and verification
 */
public interface PasswordHasher {

    /**
     * Creates a hash of a given raw password.
     * @param rawPassword The plain text password.
     * @return A hash string
     */
    String hash(String rawPassword);

    /**
     * Verifies a raw password attempt against a stored hash.
     * @param rawPassword The plain text password attempt.
     * @param storedHash The hash retrieved from storage.
     * @return true if the raw password matches the hash, false otherwise.
     */
    boolean verify(String rawPassword, String storedHash);
}
