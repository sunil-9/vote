package com.example.vote.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for password hashing and verification
 */
public class PasswordHash {

    // Defines how computationally intensive the hashing will be (recommended: 10-12)
    private static final int WORKLOAD = 12;

    /**
     * Hash a password using BCrypt
     * 
     * @param passwordPlaintext The plain text password to hash
     * @return The hashed password
     */
    public static String hashPassword(String passwordPlaintext) {
        if (passwordPlaintext == null || passwordPlaintext.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        // Generate a salt with specified workload factor
        String salt = BCrypt.gensalt(WORKLOAD);
        
        // Hash the password
        return BCrypt.hashpw(passwordPlaintext, salt);
    }

    /**
     * Verify a password against a stored hash
     * 
     * @param passwordPlaintext The plain text password to verify
     * @param storedHash The stored hash to check against
     * @return true if password matches the hash, false otherwise
     */
    public static boolean verifyPassword(String passwordPlaintext, String storedHash) {
        if (passwordPlaintext == null || passwordPlaintext.isEmpty() || 
            storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        try {
            // BCrypt.checkpw will hash the plaintext and compare it with storedHash
            return BCrypt.checkpw(passwordPlaintext, storedHash);
        } catch (IllegalArgumentException e) {
            // In case the stored hash is not a valid BCrypt hash
            return false;
        }
    }
}
