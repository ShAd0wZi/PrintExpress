package com.printxpress.app.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class PricingCalculatorTest {

    @Test
    public void testBasePriceSingleQuantity() {
        double price = PricingCalculator.calculateItemPrice(100.0, "Small", "Standard", 1);
        assertEquals(110.0, price, 0.01); // Standard has 1.1x multiplier
    }

    @Test
    public void testLargeSizeMultiplier() {
        double standardPrice = PricingCalculator.calculateItemPrice(100.0, "Small", "Standard", 1);
        double largePrice = PricingCalculator.calculateItemPrice(100.0, "Large", "Standard", 1);
        
        // Large is 2.0x, Small is 1.0x
        assertTrue(largePrice > standardPrice);
        assertEquals(220.0, largePrice, 0.01); // 100 * 2.0 * 1.1 = 220
    }

    @Test
    public void testVolumeDiscountFiftyPlus() {
        double unitPriceSingle = PricingCalculator.calculateItemPrice(100.0, "Small", "Basic", 1);
        double unitPriceFifty = PricingCalculator.calculateItemPrice(100.0, "Small", "Basic", 50);
        
        // 5% discount on 50 units
        assertEquals(100.0, unitPriceSingle, 0.01);
        assertEquals(95.0, unitPriceFifty, 0.01);
    }

    @Test
    public void testVolumeDiscountFiveHundredPlus() {
        double unitPriceBulk = PricingCalculator.calculateItemPrice(100.0, "Small", "Basic", 500);
        
        // 20% discount on 500+ units
        assertEquals(80.0, unitPriceBulk, 0.01);
    }

    @Test
    public void testDeliveryFeeCalculation() {
        assertEquals(0.0, PricingCalculator.calculateDeliveryFee("Store Pickup"), 0.001);
        assertEquals(0.0, PricingCalculator.calculateDeliveryFee("PICKUP"), 0.001);
        assertEquals(500.0, PricingCalculator.calculateDeliveryFee("Home Delivery"), 0.001);
        assertEquals(500.0, PricingCalculator.calculateDeliveryFee("Express Delivery"), 0.001);
    }

    @Test
    public void testTaxAndGrandTotalCalculation() {
        double subtotal = 1000.0;
        double tax = PricingCalculator.calculateTax(subtotal);
        assertEquals(80.0, tax, 0.001);

        double pickupTotal = PricingCalculator.calculateGrandTotal(subtotal, "Store Pickup");
        assertEquals(1080.0, pickupTotal, 0.001);

        double deliveryTotal = PricingCalculator.calculateGrandTotal(subtotal, "Home Delivery");
        assertEquals(1580.0, deliveryTotal, 0.001);
    }

    @Test
    public void testNegativeAndZeroInputGuards() {
        assertEquals(0.0, PricingCalculator.calculateItemPrice(-50.0, "Small", "Standard", 1), 0.001);
        assertEquals(110.0, PricingCalculator.calculateItemPrice(100.0, "Small", "Standard", 0), 0.001);
        assertEquals(110.0, PricingCalculator.calculateItemPrice(100.0, "Small", "Standard", -10), 0.001);
        assertEquals(0.0, PricingCalculator.calculateTax(-100.0), 0.001);
        assertEquals(0.0, PricingCalculator.calculateSubtotal(-10.0, 5), 0.001);
        assertEquals(0.0, PricingCalculator.calculateSubtotal(100.0, 0), 0.001);
    }

    @Test
    public void testNullSizeAndMaterialFallbacks() {
        double price = PricingCalculator.calculateItemPrice(100.0, null, null, 1);
        assertEquals(100.0, price, 0.001);
    }

    @Test
    public void testBigDecimalRoundingPrecision() {
        double subtotal = PricingCalculator.calculateSubtotal(33.33, 3);
        assertEquals(99.99, subtotal, 0.0001);
        double tax = PricingCalculator.calculateTax(123.45);
        assertEquals(9.88, tax, 0.0001); // 123.45 * 0.08 = 9.876 -> 9.88
    }

    @Test
    public void testPrintProviderHubMultipliers() {
        double colomboPrice = PricingCalculator.calculateItemPrice(1000.0, "Standard", "Basic", "Colombo 03 — Heidelberg Offset Hub", 1);
        double kelaniyaPrice = PricingCalculator.calculateItemPrice(1000.0, "Standard", "Basic", "Kelaniya — Konica Digital Press Hub", 1);
        double kandyPrice = PricingCalculator.calculateItemPrice(1000.0, "Standard", "Basic", "Kandy — Central Regional Print Hub", 1);

        assertEquals(1000.0, colomboPrice, 0.01);
        assertEquals(920.0, kelaniyaPrice, 0.01); // 8% Eco Saver
        assertEquals(960.0, kandyPrice, 0.01); // 4% Regional
    }

    @Test
    public void testDynamicZoneAndWeightShippingCalculator() {
        // Store pickup is always free
        assertEquals(0.0, PricingCalculator.calculateZoneShippingFee("Store Pickup", "Colombo 03", 5000.0, 10), 0.01);
        // Orders >= 15,000 get free commercial shipping
        assertEquals(0.0, PricingCalculator.calculateZoneShippingFee("Home Delivery", "Kandy", 15000.0, 50), 0.01);
        // Zone rates (light parcel < 100 units)
        assertEquals(450.0, PricingCalculator.calculateZoneShippingFee("Home Delivery", "Colombo 03", 4000.0, 10), 0.01);
        assertEquals(600.0, PricingCalculator.calculateZoneShippingFee("Home Delivery", "Nugegoda", 4000.0, 10), 0.01);
        assertEquals(850.0, PricingCalculator.calculateZoneShippingFee("Home Delivery", "Galle", 4000.0, 10), 0.01);
        // Bulk weight surcharges (+150 for 100+, +300 for 250+)
        assertEquals(600.0, PricingCalculator.calculateZoneShippingFee("Home Delivery", "Colombo 07", 4000.0, 120), 0.01);
        assertEquals(750.0, PricingCalculator.calculateZoneShippingFee("Home Delivery", "Colombo 07", 4000.0, 300), 0.01);
    }

    @Test
    public void testMultiCurrencyConversionAndFormatting() {
        assertEquals("Rs. 3,000.00", PricingCalculator.formatCurrency(3000.0, "LKR"));
        assertEquals("$10.00", PricingCalculator.formatCurrency(3000.0, "USD"));
        assertEquals("€10.00", PricingCalculator.formatCurrency(3250.0, "EUR"));
        assertEquals("£10.00", PricingCalculator.formatCurrency(3800.0, "GBP"));
    }

    @Test
    public void testProductInventoryStockBounds() {
        com.printxpress.app.data.local.entity.Product p = new com.printxpress.app.data.local.entity.Product();
        assertEquals(100, p.getStockQuantity());
        p.setStockQuantity(25);
        assertEquals(25, p.getStockQuantity());
        p.setStockQuantity(-10);
        assertEquals(0, p.getStockQuantity());
    }
}
