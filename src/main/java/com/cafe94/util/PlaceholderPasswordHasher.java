package com.cafe94.util;

import java.util.Objects;

/**
 *This implementation provides a non-secure way to simulate hashing using only
 *standard Java
 * @author Adigun Lateef
 * @version 1.0
 */
public class PlaceholderPasswordHasher implements PasswordHasher {

    private static final String HASH_PREFIX = "SIMULATED_HASH_FOR_";

    /**
     * Simulates hashing by adding a prefix
     * @param rawPassword The plain text password
     * @return A simulated "hash".
     */
    @Override
    public String hash(String rawPassword) {
        System.err.println("SECURITY WARNING: Using insecure placeholder " +
        "password hashing!");
        Objects.requireNonNull(rawPassword,
        "Password cannot be null for hashing.");
        if (rawPassword.isEmpty()) {
            throw new IllegalArgumentException(
                "Password cannot be empty for hashing.");
        }
        return HASH_PREFIX + rawPassword;
    }

    /**
     * Simulates verification by checking the prefix and comparing the rest
     * @param rawPassword The plain text password attempt.
     * @param storedHash The simulated hash retrieved from storage.
     * @return true if the simulated hash matches the raw password,
     * false otherwise.
     */
    @Override
    public boolean verify(String rawPassword, String storedHash) {
        System.err.println("SECURITY WARNING: Using insecure placeholder " +
        "password verification!");
        if (rawPassword == null || storedHash == null ||
        !storedHash.startsWith(HASH_PREFIX)) {
            return false;
        }
        String expectedHash = HASH_PREFIX + rawPassword;
        return storedHash.equals(expectedHash);
    }
}