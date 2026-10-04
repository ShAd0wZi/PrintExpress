package com.printxpress.app.util;

import java.io.File;
import java.util.regex.Pattern;

public class ValidationHelper {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) return false;
        phone = phone.replaceAll("[\\s-]", "");
        return phone.matches("^0\\d{9}$") || phone.matches("^\\+94\\d{9}$");
    }
    
    public static boolean isValidPassword(String password) {
        return password != null && password.trim().length() >= 6;
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }
    
    public static boolean isValidQuantity(String quantity) {
        if (quantity == null || quantity.trim().isEmpty()) return false;
        try {
            int qty = Integer.parseInt(quantity.trim());
            return qty > 0 && qty <= 1000;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isValidArtworkFile(File file) {
        if (file == null || !file.exists() || file.length() <= 0) return false;
        String name = file.getName().toLowerCase();
        if (!name.endsWith(".pdf") && !name.endsWith(".jpg") && 
            !name.endsWith(".jpeg") && !name.endsWith(".png")) {
            return false;
        }
        return file.length() <= 10L * 1024L * 1024L; // 10MB
    }
    
    public static boolean isFieldFilled(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    public static boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) return false;
        String cleaned = cardNumber.replaceAll("[\\s-]", "");
        return cleaned.matches("^\\d{16}$");
    }

    /**
     * Validates a 13-to-19 digit payment card number using the ISO/IEC 7812 Luhn algorithm.
     */
    public static boolean isValidCardNumberLuhn(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) return false;
        String cleaned = cardNumber.replaceAll("[\\s-]", "");
        if (!cleaned.matches("^\\d{13,19}$")) return false;

        int sum = 0;
        boolean alternate = false;
        for (int i = cleaned.length() - 1; i >= 0; i--) {
            int n = cleaned.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
    
    public static boolean isValidCVV(String cvv) {
        return cvv != null && cvv.trim().matches("^\\d{3,4}$");
    }

    public static boolean isValidExpiryDate(String expiry) {
        if (expiry == null) return false;
        return expiry.trim().matches("^(0[1-9]|1[0-2])/[0-9]{2}$");
    }
}