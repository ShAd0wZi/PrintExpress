package com.printxpress.app.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class SecurityHelper {

    private static final int SALT_LENGTH_BYTES = 16;
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Hashes a plain-text password using PBKDF2-HMAC-SHA256 with a unique random 16-byte salt.
     * The returned string is formatted as: "<hex-salt>:<hex-hash>"
     */
    public static String hashPassword(String password) {
        if (password == null) {
            return null;
        }
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = computePbkdf2Hash(password, salt);
        return bytesToHex(salt) + ":" + bytesToHex(hash);
    }

    /**
     * Verifies a plain-text password against a stored hash.
     * Supports PBKDF2 salted (<salt>:<hash>), legacy SHA-256 salted (<salt>:<hash>), and legacy unsalted SHA-256 hashes.
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        if (storedHash.contains(":")) {
            String[] parts = storedHash.split(":", 2);
            if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty() || parts[0].length() % 2 != 0) {
                return false;
            }
            try {
                byte[] salt = hexToBytes(parts[0]);
                String pbkdf2Hex = bytesToHex(computePbkdf2Hash(password, salt));
                if (constantTimeEquals(pbkdf2Hex, parts[1].toLowerCase())) {
                    return true;
                }
                // Backward-compatible fallback for legacy single-pass salted SHA-256
                String sha256Hex = bytesToHex(computeSha256Hash(password, salt));
                return constantTimeEquals(sha256Hex, parts[1].toLowerCase());
            } catch (Exception e) {
                return false;
            }
        } else {
            // Legacy unsalted fallback
            String legacyHash = hashUnsalted(password);
            return constantTimeEquals(legacyHash.toLowerCase(), storedHash.toLowerCase());
        }
    }

    private static byte[] computePbkdf2Hash(String password, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            return computeSha256Hash(password, salt);
        }
    }

    private static byte[] computeSha256Hash(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private static String hashUnsalted(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Prevents timing attacks when comparing password hashes.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
