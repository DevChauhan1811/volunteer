package com.volunteer.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class providing secure password hashing and verification using BCrypt.
 * Protects against rainbow tables and brute-force attacks via random per-password salts.
 */
public final class PasswordHasher {

    // BCrypt workload factor (12 is standard balance of security and speed)
    private static final int BCRYPT_WORKLOAD = 12;

    private PasswordHasher() {
        // Prevent instantiation
    }

    /**
     * Hashes a plaintext password using BCrypt with an automatically generated salt.
     *
     * @param plainPassword the plaintext password to hash
     * @return the resulting salted BCrypt hash string
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_WORKLOAD));
    }

    /**
     * Verifies a candidate plaintext password against an existing BCrypt hash.
     *
     * @param plainPassword candidate plaintext password
     * @param hashedPassword existing BCrypt hash from database
     * @return true if password matches hash; false otherwise
     */
    public static boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Malformed hash
            return false;
        }
    }
}
