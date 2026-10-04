package com.printxpress.app.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class SecurityHelperTest {

    @Test
    public void testHashFormatContainsSaltAndHash() {
        String password = "SecretPassword123!";
        String saltedHash = SecurityHelper.hashPassword(password);
        
        assertNotNull(saltedHash);
        assertTrue("Hash must contain salt delimiter ':'", saltedHash.contains(":"));
        
        String[] parts = saltedHash.split(":");
        assertEquals("Must have salt and hash parts", 2, parts.length);
        assertEquals("Salt must be 16 bytes = 32 hex chars", 32, parts[0].length());
        assertEquals("SHA-256 hash must be 32 bytes = 64 hex chars", 64, parts[1].length());
    }

    @Test
    public void testDifferentSaltsForSamePassword() {
        String password = "SamePassword";
        String hash1 = SecurityHelper.hashPassword(password);
        String hash2 = SecurityHelper.hashPassword(password);
        
        assertNotEquals("Each hash must have a unique random salt", hash1, hash2);
        assertTrue(SecurityHelper.verifyPassword(password, hash1));
        assertTrue(SecurityHelper.verifyPassword(password, hash2));
    }

    @Test
    public void testPasswordVerificationSuccess() {
        String password = "MySecurePassword2026";
        String saltedHash = SecurityHelper.hashPassword(password);
        
        assertTrue(SecurityHelper.verifyPassword(password, saltedHash));
    }

    @Test
    public void testPasswordVerificationFailureOnWrongPassword() {
        String password = "CorrectPassword";
        String saltedHash = SecurityHelper.hashPassword(password);
        
        assertFalse(SecurityHelper.verifyPassword("WrongPassword", saltedHash));
        assertFalse(SecurityHelper.verifyPassword("", saltedHash));
        assertFalse(SecurityHelper.verifyPassword(null, saltedHash));
    }

    @Test
    public void testLegacyUnsaltedHashCompatibility() {
        // Test standard SHA-256 unsalted hash verification for backward compatibility
        // SHA-256 for "password" = 5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8
        String legacyHash = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";
        assertTrue("Must support legacy unsalted hash", SecurityHelper.verifyPassword("password", legacyHash));
        assertFalse("Must reject incorrect password on legacy hash", SecurityHelper.verifyPassword("wrong", legacyHash));
    }

    @Test
    public void testMalformedAndNullStoredHashes() {
        assertFalse(SecurityHelper.verifyPassword("password", null));
        assertFalse(SecurityHelper.verifyPassword("password", ":"));
        assertFalse(SecurityHelper.verifyPassword("password", "invalidsalt:invalidhash"));
    }
}
