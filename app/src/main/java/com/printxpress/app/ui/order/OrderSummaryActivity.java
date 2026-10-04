package com.printxpress.app.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.entity.Address;
import com.printxpress.app.data.local.entity.Promotion;
import com.printxpress.app.data.local.model.CartItemWithProduct;
import com.printxpress.app.databinding.ActivityOrderSummaryBinding;
import com.printxpress.app.ui.adapter.CartAdapter;
import com.printxpress.app.util.PricingCalculator;
import com.printxpress.app.util.SessionManager;
import com.printxpress.app.viewModel.CartViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderSummaryActivity extends AppCompatActivity {
    
    private ActivityOrderSummaryBinding binding;
    private CartViewModel cartViewModel;
    private CartAdapter adapter;
    private PrintXpressDatabase db;
    private int userId;
    
    private String deliveryMethod;
    private int deliveryAddressId;
    private String shippingCity = "Colombo";
    private int totalCartUnits = 0;
    private double cartSubtotal = 0.0;
    private double discountAmount = 0.0;
    private Promotion appliedPromotion = null;
    private List<Promotion> activePromotions = new ArrayList<>();
    private double deliveryFee = 500.0;
    private double tax = 0.0;
    private double grandTotal = 0.0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.requireLogin(this)) {
            finish();
            return;
        }
        userId = SessionManager.getUserId(this);

        binding = ActivityOrderSummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        db = PrintXpressDatabase.getDatabase(this);
        binding.btnBack.setOnClickListener(v -> finish());
        
        deliveryMethod = getIntent().getStringExtra("DELIVERY_METHOD");
        deliveryAddressId = getIntent().getIntExtra("DELIVERY_ADDRESS_ID", -1);
        
        binding.tvDeliveryMethod.setText(deliveryMethod);
        if ("Store Pickup".equals(deliveryMethod)) {
            deliveryFee = 0.0;
            binding.tvDeliveryAddressTitle.setVisibility(View.GONE);
            binding.tvDeliveryAddress.setVisibility(View.GONE);
            binding.tvShippingZoneNote.setText("✓ Store Pickup at Colombo 03 Hub • Free Collection (Rs. 0.00)");
        } else {
            // Query real Address details from SQLite database
            PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
                Address address = db.addressDao().getAddressById(deliveryAddressId);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (address != null) {
                        if (address.getCity() != null && !address.getCity().isEmpty()) {
                            shippingCity = address.getCity();
                        }
                        StringBuilder addrText = new StringBuilder(address.getAddressLine1());
                        if (address.getAddressLine2() != null && !address.getAddressLine2().isEmpty()) {
                            addrText.append(", ").append(address.getAddressLine2());
                        }
                        addrText.append(", ").append(address.getCity());
                        if (address.getPostalCode() != null && !address.getPostalCode().isEmpty()) {
                            addrText.append(" ").append(address.getPostalCode());
                        }
                        if (address.getPhone() != null && !address.getPhone().isEmpty()) {
                            addrText.append(" • Tel: ").append(address.getPhone());
                        }
                        binding.tvDeliveryAddress.setText(addrText.toString());
                    } else {
                        shippingCity = "Colombo";
                        binding.tvDeliveryAddress.setText("Standard Colombo Delivery");
                    }
                    recalculateTotals();
                });
            });
        }
        
        int singleCartItemId = getIntent().getIntExtra("SINGLE_CART_ITEM_ID", -1);
        
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
        
        binding.rvSummaryItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(new ArrayList<>(), null);
        binding.rvSummaryItems.setAdapter(adapter);
        
        cartViewModel.getCartItems(userId).observe(this, items -> {
            if (items != null) {
                List<CartItemWithProduct> displayItems = new ArrayList<>();
                double singleSubtotal = 0.0;
                for (CartItemWithProduct cp : items) {
                    if (singleCartItemId > 0) {
                        if (cp.cartItem != null && cp.cartItem.getCartItemId() == singleCartItemId) {
                            displayItems.add(cp);
                            singleSubtotal += cp.cartItem.getSubtotal();
                        }
                    } else {
                        displayItems.add(cp);
                    }
                }
                adapter.setItems(displayItems);
                if (singleCartItemId > 0) {
                    cartSubtotal = singleSubtotal;
                }

                int units = 0;
                String detectedHub = null;
                for (CartItemWithProduct cp : displayItems) {
                    if (cp.cartItem != null) {
                        units += cp.cartItem.getQuantity();
                        if (detectedHub == null && cp.cartItem.getCustomText() != null) {
                            String n = cp.cartItem.getCustomText();
                            if (n.contains("Kelaniya")) {
                                detectedHub = "Kelaniya Production Hub";
                            } else if (n.contains("Kandy")) {
                                detectedHub = "Kandy Regional Hub";
                            } else if (n.contains("Colombo")) {
                                detectedHub = "Colombo 03 Flagship Hub";
                            }
                        }
                    }
                }
                totalCartUnits = units;
                if ("Store Pickup".equals(deliveryMethod) && detectedHub != null) {
                    binding.tvShippingZoneNote.setText("✓ Store Pickup at " + detectedHub + " • Free Collection (Rs. 0.00)");
                }
                recalculateTotals();
            }
        });
        
        cartViewModel.getCartTotal(userId).observe(this, total -> {
            if (total != null && singleCartItemId <= 0) {
                cartSubtotal = total;
                recalculateTotals();
            }
        });

        db.promotionDao().getActivePromotions().observe(this, promos -> {
            if (promos != null) {
                activePromotions = promos;
            }
        });

        binding.btnApplyPromo.setOnClickListener(v -> applyPromoCodeFromDatabase());
        binding.btnBrowsePromos.setOnClickListener(v -> showAvailablePromosDialog());
        
        binding.btnProceedToPayment.setOnClickListener(v -> {
            Intent intent = new Intent(OrderSummaryActivity.this, PaymentActivity.class);
            intent.putExtra("DELIVERY_METHOD", deliveryMethod);
            intent.putExtra("DELIVERY_ADDRESS_ID", deliveryAddressId);
            intent.putExtra("GRAND_TOTAL", grandTotal);
            intent.putExtra("SINGLE_CART_ITEM_ID", singleCartItemId);
            startActivity(intent);
        });
    }

    private void showAvailablePromosDialog() {
        if (activePromotions == null || activePromotions.isEmpty()) {
            com.printxpress.app.util.PrintXpressToast.info(this, "No active promo codes in database");
            return;
        }
        String[] labels = new String[activePromotions.size()];
        for (int i = 0; i < activePromotions.size(); i++) {
            Promotion p = activePromotions.get(i);
            labels[i] = String.format(Locale.US, "%s — %.0f%% OFF (Min: Rs. %,.2f)\n%s",
                    p.getCode(), p.getDiscountPercentage(), p.getMinOrderAmount(), p.getTitle());
        }
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Active Promo Code")
                .setItems(labels, (d, which) -> {
                    Promotion selected = activePromotions.get(which);
                    binding.etPromoCode.setText(selected.getCode());
                    applyPromoCodeFromDatabase();
                })
                .setNegativeButton("Cancel", null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.65f);
        }
        dialog.show();
    }

    private void applyPromoCodeFromDatabase() {
        String code = binding.etPromoCode.getText().toString().trim();
        if (code.isEmpty()) {
            com.printxpress.app.util.PrintXpressToast.info(this, "Enter a promo code first");
            return;
        }
        PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
            Promotion promo = db.promotionDao().getPromotionByCode(code);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (promo == null || !promo.isActive()) {
                    appliedPromotion = null;
                    recalculateTotals();
                    com.printxpress.app.util.PrintXpressToast.error(this, "Invalid or expired promo code in database");
                    return;
                }
                if (cartSubtotal < promo.getMinOrderAmount()) {
                    com.printxpress.app.util.PrintXpressToast.error(this, String.format(Locale.getDefault(),
                            "Code %s requires a minimum subtotal of %s",
                            promo.getCode(), SessionManager.formatPrice(this, promo.getMinOrderAmount())));
                    return;
                }
                appliedPromotion = promo;
                recalculateTotals();
                com.printxpress.app.util.PrintXpressToast.success(this, String.format(Locale.getDefault(),
                        "Applied %s (%.0f%% OFF)!", promo.getCode(), promo.getDiscountPercentage()));
            });
        });
    }

    private void recalculateTotals() {
        if (appliedPromotion != null && cartSubtotal >= appliedPromotion.getMinOrderAmount()) {
            discountAmount = cartSubtotal * (appliedPromotion.getDiscountPercentage() / 100.0);
            binding.llDiscountRow.setVisibility(View.VISIBLE);
            binding.tvDiscountLabel.setText(String.format(Locale.getDefault(),
                    "Promo (%s - %.0f%%)", appliedPromotion.getCode(), appliedPromotion.getDiscountPercentage()));
            binding.tvDiscount.setText("- " + SessionManager.formatPrice(this, discountAmount));
        } else {
            discountAmount = 0.0;
            binding.llDiscountRow.setVisibility(View.GONE);
        }

        double discountedSubtotal = Math.max(0.0, cartSubtotal - discountAmount);
        deliveryFee = PricingCalculator.calculateZoneShippingFee(
                deliveryMethod, shippingCity, discountedSubtotal, totalCartUnits);
        tax = PricingCalculator.calculateTax(discountedSubtotal);
        grandTotal = discountedSubtotal + deliveryFee + tax;

        if (!"Store Pickup".equals(deliveryMethod)) {
            if (discountedSubtotal >= PricingCalculator.FREE_SHIPPING_THRESHOLD) {
                binding.tvShippingZoneNote.setText("🎉 Zone: " + shippingCity + " • FREE Commercial Shipping Unlocked!");
            } else {
                double remaining = PricingCalculator.FREE_SHIPPING_THRESHOLD - discountedSubtotal;
                binding.tvShippingZoneNote.setText(String.format(Locale.US,
                        "🚚 Zone: %s (%d units) • Add %s more for FREE shipping",
                        shippingCity, totalCartUnits, SessionManager.formatPrice(this, remaining)));
            }
        }

        binding.tvSubtotal.setText(SessionManager.formatPrice(this, cartSubtotal));
        binding.tvDeliveryFee.setText(deliveryFee <= 0.0 ? "FREE" : SessionManager.formatPrice(this, deliveryFee));
        binding.tvTax.setText(SessionManager.formatPrice(this, tax));
        binding.tvGrandTotal.setText(SessionManager.formatPrice(this, grandTotal));
    }
}