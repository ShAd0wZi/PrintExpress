package com.printxpress.app.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PricingCalculator {

    public static final double DEFAULT_TAX_RATE = 0.08; // 8%
    public static final double DEFAULT_DELIVERY_FEE = 500.0; // Rs. 500 for home delivery
    public static final double FREE_SHIPPING_THRESHOLD = 15000.0; // Free shipping over Rs. 15,000

    public static double calculateProviderMultiplier(String printHub) {
        if (printHub == null) return 1.0;
        String lower = printHub.toLowerCase();
        if (lower.contains("kelaniya") || lower.contains("eco")) {
            return 0.92; // 8% savings on Digital Press Hub
        } else if (lower.contains("kandy") || lower.contains("regional")) {
            return 0.96; // 4% savings on Regional Hub
        }
        return 1.0; // Colombo 03 Heidelberg Offset Hub (Express)
    }

    public static double calculateItemPrice(double basePrice, String size, String material, int quantity) {
        return calculateItemPrice(basePrice, size, material, null, quantity);
    }

    public static double calculateItemPrice(double basePrice, String size, String material, String printHub, int quantity) {
        if (basePrice <= 0.0) {
            return 0.0;
        }
        double sizeMultiplier = 1.0;
        double materialMultiplier = 1.0;
        double providerMultiplier = calculateProviderMultiplier(printHub);
        
        // Size scaling
        if (size != null) {
            String lowerSize = size.toLowerCase();
            if (lowerSize.contains("a3") || lowerSize.contains("large")) sizeMultiplier = 2.0;
            else if (lowerSize.contains("a4") || lowerSize.contains("15 oz")) sizeMultiplier = 1.5;
            else if (lowerSize.contains("a5") || lowerSize.contains("medium")) sizeMultiplier = 1.2;
            else sizeMultiplier = 1.0; // standard / small
        }
        
        // Material scaling
        if (material != null) {
            String lowerMat = material.toLowerCase();
            if (lowerMat.contains("glossy") || lowerMat.contains("premium") || lowerMat.contains("velvet")) materialMultiplier = 1.2;
            else if (lowerMat.contains("matte") || lowerMat.contains("standard") || lowerMat.contains("silk")) materialMultiplier = 1.1;
            else materialMultiplier = 1.0; // basic
        }
        
        // Volume discounts
        double discountRate = 0.0;
        if (quantity >= 500) discountRate = 0.20; // 20% off for 500+
        else if (quantity >= 100) discountRate = 0.10; // 10% off for 100+
        else if (quantity >= 50) discountRate = 0.05; // 5% off for 50+

        BigDecimal price = BigDecimal.valueOf(basePrice)
                .multiply(BigDecimal.valueOf(sizeMultiplier))
                .multiply(BigDecimal.valueOf(materialMultiplier))
                .multiply(BigDecimal.valueOf(providerMultiplier))
                .multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(discountRate)));
        return roundCurrency(price);
    }

    public static double calculateItemSubtotal(double basePrice, String size, String material, int quantity) {
        return calculateItemSubtotal(basePrice, size, material, null, quantity);
    }

    public static double calculateItemSubtotal(double basePrice, String size, String material, String printHub, int quantity) {
        if (basePrice <= 0.0 || quantity <= 0) {
            return 0.0;
        }
        int safeQuantity = Math.min(quantity, 100000);
        BigDecimal unitPrice = BigDecimal.valueOf(calculateItemPrice(basePrice, size, material, printHub, safeQuantity));
        return roundCurrency(unitPrice.multiply(BigDecimal.valueOf(safeQuantity)));
    }

    public static double calculateSubtotal(double unitPrice, int quantity) {
        if (unitPrice <= 0.0 || quantity <= 0) {
            return 0.0;
        }
        int safeQuantity = Math.min(quantity, 100000);
        return roundCurrency(BigDecimal.valueOf(unitPrice).multiply(BigDecimal.valueOf(safeQuantity)));
    }

    public static String getPricingBreakdownSummary(double basePrice, String size, String material, String printHub, int quantity) {
        if (basePrice <= 0.0 || quantity <= 0) return "";
        double unitPrice = calculateItemPrice(basePrice, size, material, printHub, quantity);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(java.util.Locale.US, "Rs. %,.2f / unit", unitPrice));

        java.util.List<String> perks = new java.util.ArrayList<>();
        if (quantity >= 500) perks.add("Bulk Tier: -20%");
        else if (quantity >= 100) perks.add("Bulk Tier: -10%");
        else if (quantity >= 50) perks.add("Bulk Tier: -5%");

        double hubMult = calculateProviderMultiplier(printHub);
        if (hubMult < 1.0) {
            int savings = (int) Math.round((1.0 - hubMult) * 100);
            perks.add("Eco Hub: -" + savings + "%");
        }

        if (!perks.isEmpty()) {
            sb.append(" (").append(android.text.TextUtils.join(" • ", perks)).append(")");
        }
        return sb.toString();
    }

    public static double calculateDeliveryFee(String deliveryMethod) {
        if (deliveryMethod != null && ("Store Pickup".equalsIgnoreCase(deliveryMethod.trim())
                || "PICKUP".equalsIgnoreCase(deliveryMethod.trim()))) {
            return 0.0;
        }
        return DEFAULT_DELIVERY_FEE;
    }

    /**
     * Dynamic Zone & Weight-Based Shipping Calculator:
     * - Free shipping on Store Pickup or orders >= Rs. 15,000
     * - Colombo Zone: Rs. 450 base
     * - Greater Colombo Suburbs: Rs. 600 base
     * - Outstation Regional: Rs. 850 base
     * - Bulk press weight surcharge: +Rs. 150 for 100+ units, +Rs. 300 for 250+ units
     */
    public static double calculateZoneShippingFee(String deliveryMethod, String city, double discountedSubtotal, int totalUnits) {
        if (deliveryMethod != null && ("Store Pickup".equalsIgnoreCase(deliveryMethod.trim())
                || "PICKUP".equalsIgnoreCase(deliveryMethod.trim()))) {
            return 0.0;
        }
        if (discountedSubtotal >= FREE_SHIPPING_THRESHOLD) {
            return 0.0;
        }
        double baseFee = DEFAULT_DELIVERY_FEE;
        if (city != null && !city.trim().isEmpty()) {
            String lowerCity = city.trim().toLowerCase();
            if (lowerCity.contains("colombo")) {
                baseFee = 450.0;
            } else if (lowerCity.contains("dehiwala") || lowerCity.contains("mount lavinia")
                    || lowerCity.contains("nugegoda") || lowerCity.contains("maharagama")
                    || lowerCity.contains("battaramulla") || lowerCity.contains("kelaniya")
                    || lowerCity.contains("malabe") || lowerCity.contains("wattala")
                    || lowerCity.contains("moratuwa") || lowerCity.contains("kotte")
                    || lowerCity.contains("rajagiriya")) {
                baseFee = 600.0;
            } else {
                baseFee = 850.0; // Outstation
            }
        }
        double weightSurcharge = 0.0;
        if (totalUnits >= 250) {
            weightSurcharge = 300.0;
        } else if (totalUnits >= 100) {
            weightSurcharge = 150.0;
        }
        return roundCurrency(BigDecimal.valueOf(baseFee + weightSurcharge));
    }

    public static double convertCurrencyFromLkr(double amountInLkr, String currencyCode) {
        if (currencyCode == null) return roundCurrency(BigDecimal.valueOf(amountInLkr));
        double rate = 1.0;
        switch (currencyCode.toUpperCase()) {
            case "USD":
                rate = 1.0 / 300.0;
                break;
            case "EUR":
                rate = 1.0 / 325.0;
                break;
            case "GBP":
                rate = 1.0 / 380.0;
                break;
            default:
                rate = 1.0;
                break;
        }
        return roundCurrency(BigDecimal.valueOf(amountInLkr).multiply(BigDecimal.valueOf(rate)));
    }

    public static String formatCurrency(double amountInLkr, String currencyCode) {
        String code = currencyCode != null ? currencyCode.toUpperCase() : "LKR";
        double converted = convertCurrencyFromLkr(amountInLkr, code);
        switch (code) {
            case "USD":
                return String.format(java.util.Locale.US, "$%,.2f", converted);
            case "EUR":
                return String.format(java.util.Locale.US, "€%,.2f", converted);
            case "GBP":
                return String.format(java.util.Locale.US, "£%,.2f", converted);
            default:
                return String.format(java.util.Locale.US, "Rs. %,.2f", converted);
        }
    }

    public static double calculateTax(double subtotal) {
        return calculateTax(subtotal, DEFAULT_TAX_RATE);
    }

    public static double calculateTax(double subtotal, double taxRate) {
        if (subtotal <= 0.0 || taxRate <= 0.0) {
            return 0.0;
        }
        BigDecimal tax = BigDecimal.valueOf(subtotal).multiply(BigDecimal.valueOf(taxRate));
        return roundCurrency(tax);
    }

    public static double calculateGrandTotal(double subtotal, String deliveryMethod) {
        double safeSubtotal = Math.max(0.0, subtotal);
        BigDecimal total = BigDecimal.valueOf(safeSubtotal)
                .add(BigDecimal.valueOf(calculateDeliveryFee(deliveryMethod)))
                .add(BigDecimal.valueOf(calculateTax(safeSubtotal)));
        return roundCurrency(total);
    }

    private static double roundCurrency(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
