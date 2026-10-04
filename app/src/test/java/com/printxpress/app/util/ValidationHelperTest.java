package com.printxpress.app.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class ValidationHelperTest {

    @Test
    public void testValidEmail() {
        assertTrue(ValidationHelper.isValidEmail("sasiru@example.com"));
        assertFalse(ValidationHelper.isValidEmail("invalid-email"));
        assertFalse(ValidationHelper.isValidEmail(""));
        assertFalse(ValidationHelper.isValidEmail(null));
    }

    @Test
    public void testValidPhone() {
        assertTrue(ValidationHelper.isValidPhone("0771234567"));
        assertTrue(ValidationHelper.isValidPhone("+94771234567"));
        assertFalse(ValidationHelper.isValidPhone("123"));
        assertFalse(ValidationHelper.isValidPhone(null));
    }

    @Test
    public void testValidQuantity() {
        assertTrue(ValidationHelper.isValidQuantity("1"));
        assertTrue(ValidationHelper.isValidQuantity("500"));
        assertTrue(ValidationHelper.isValidQuantity("1000"));
        assertFalse(ValidationHelper.isValidQuantity("0"));
        assertFalse(ValidationHelper.isValidQuantity("-5"));
        assertFalse(ValidationHelper.isValidQuantity("1001"));
        assertFalse(ValidationHelper.isValidQuantity("abc"));
        assertFalse(ValidationHelper.isValidQuantity(null));
    }

    @Test
    public void testValidCardNumber() {
        assertTrue(ValidationHelper.isValidCardNumber("1234567812345678"));
        assertFalse(ValidationHelper.isValidCardNumber("1234"));
        assertFalse(ValidationHelper.isValidCardNumber("lettersnotdigits"));
        assertFalse(ValidationHelper.isValidCardNumber(null));
    }

    @Test
    public void testValidCVV() {
        assertTrue(ValidationHelper.isValidCVV("123"));
        assertTrue(ValidationHelper.isValidCVV("1234"));
        assertFalse(ValidationHelper.isValidCVV("12"));
        assertFalse(ValidationHelper.isValidCVV("12345"));
        assertFalse(ValidationHelper.isValidCVV(null));
    }

    @Test
    public void testValidCardNumberLuhn() {
        assertTrue(ValidationHelper.isValidCardNumberLuhn("4532015112830366"));
        assertFalse(ValidationHelper.isValidCardNumberLuhn("4532015112830367"));
        assertFalse(ValidationHelper.isValidCardNumberLuhn("1234"));
        assertFalse(ValidationHelper.isValidCardNumberLuhn(null));
    }

    @Test
    public void testStrongPassword() {
        assertTrue(ValidationHelper.isStrongPassword("PrintXpress2026"));
        assertFalse(ValidationHelper.isStrongPassword("short1"));
        assertFalse(ValidationHelper.isStrongPassword("alllettersonly"));
        assertFalse(ValidationHelper.isStrongPassword("1234567890"));
        assertFalse(ValidationHelper.isStrongPassword(null));
    }

    @Test
    public void testValidExpiryDate() {
        assertTrue(ValidationHelper.isValidExpiryDate("08/27"));
        assertTrue(ValidationHelper.isValidExpiryDate("12/30"));
        assertTrue(ValidationHelper.isValidExpiryDate("01/25"));
        assertFalse(ValidationHelper.isValidExpiryDate("13/26"));
        assertFalse(ValidationHelper.isValidExpiryDate("00/26"));
        assertFalse(ValidationHelper.isValidExpiryDate("8/26"));
        assertFalse(ValidationHelper.isValidExpiryDate("08-26"));
        assertFalse(ValidationHelper.isValidExpiryDate(""));
        assertFalse(ValidationHelper.isValidExpiryDate(null));
    }
}
