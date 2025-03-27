package com.wonkglorg.doc.core.hash;

import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * Utility class for hashing passwords and comparing
 */
public class BCryptUtils {

    /**
     * Hashes a password using BCrypt
     *
     * @param password the password to hash
     * @return the hashed password
     */
    public static String hashPassword(String password) {
        int logRounds = 12;
        String salt = BCrypt.gensalt(logRounds);
        return BCrypt.hashpw(password, salt);
    }

    /**
     * Verifies a password against a stored hash
     *
     * @param password   the password to verify
     * @param storedHash the stored hash
     * @return true if the password matches the stored hash
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (storedHash == null || storedHash.isEmpty()) {
            throw new IllegalArgumentException("Stored hash cannot be null or empty");
        }

        return BCrypt.checkpw(password, storedHash);
    }
}
