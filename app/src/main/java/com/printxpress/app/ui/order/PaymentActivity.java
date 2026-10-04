package com.printxpress.app.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.printxpress.app.R;
import com.printxpress.app.databinding.ActivityPaymentBinding;
import com.printxpress.app.util.NotificationHelper;
import com.printxpress.app.util.SessionManager;
import com.printxpress.app.util.ValidationHelper;
import com.printxpress.app.viewModel.OrderViewModel;

public class PaymentActivity extends AppCompatActivity {
    
    private ActivityPaymentBinding binding;
    private OrderViewModel orderViewModel;
    
    private String deliveryMethod;
    private int deliveryAddressId;
    private double grandTotal;
    private int userId;
    private int singleCartItemId = -1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!SessionManager.requireLogin(this)) {
            finish();
            return;
        }
        userId = SessionManager.getUserId(this);

        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnBack.setOnClickListener(v -> finish());
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnBack);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnPay);
        
        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        
        deliveryMethod = getIntent().getStringExtra("DELIVERY_METHOD");
        deliveryAddressId = getIntent().getIntExtra("DELIVERY_ADDRESS_ID", -1);
        grandTotal = getIntent().getDoubleExtra("GRAND_TOTAL", 0.0);
        singleCartItemId = getIntent().getIntExtra("SINGLE_CART_ITEM_ID", -1);
        
        binding.tvTotalAmount.setText("Total to Pay: " + SessionManager.formatPrice(this, grandTotal));
        
        binding.rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCash) {
                binding.llCardDetails.setVisibility(View.GONE);
                binding.btnPay.setText("Confirm Order");
            } else if (checkedId == R.id.rbBankTransfer) {
                binding.llCardDetails.setVisibility(View.GONE);
                binding.btnPay.setText("Confirm Order (Bank Transfer)");
            } else {
                binding.llCardDetails.setVisibility(View.VISIBLE);
                binding.btnPay.setText("Pay Now");
            }
        });
        
        setupExpiryDateAutoFormatter();
        setupCardNumberAutoFormatter();
        setupOrderObservers();

        binding.btnPay.setOnClickListener(v -> {
            binding.btnPay.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (validatePayment()) {
                setLoadingState(true);
                processPaymentAndSaveOrder();
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        if (binding.pbPaymentLoading != null) {
            binding.pbPaymentLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        binding.btnPay.setEnabled(!isLoading);
        if (isLoading) {
            binding.btnPay.setText("Processing Order...");
        } else {
            int selectedId = binding.rgPaymentMethod.getCheckedRadioButtonId();
            if (selectedId == R.id.rbCash) {
                binding.btnPay.setText("Confirm Order");
            } else if (selectedId == R.id.rbBankTransfer) {
                binding.btnPay.setText("Confirm Order (Bank Transfer)");
            } else {
                binding.btnPay.setText("Pay Now");
            }
        }
    }

    private void setupCardNumberAutoFormatter() {
        if (binding.etCardNumber == null) return;
        binding.etCardNumber.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isFormatting = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                String digits = s.toString().replaceAll("[^\\d]", "");
                if (digits.length() > 16) {
                    digits = digits.substring(0, 16);
                }
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(digits.charAt(i));
                }
                s.replace(0, s.length(), formatted.toString());
                isFormatting = false;
            }
        });
    }

    private void setupExpiryDateAutoFormatter() {
        if (binding.etExpiryDate == null) return;
        binding.etExpiryDate.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isFormatting = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                String input = s.toString().replaceAll("[^\\d]", "");
                if (input.length() > 4) {
                    input = input.substring(0, 4);
                }
                StringBuilder formatted = new StringBuilder();
                if (input.length() >= 2) {
                    formatted.append(input.substring(0, 2)).append("/").append(input.substring(2));
                } else {
                    formatted.append(input);
                }
                s.replace(0, s.length(), formatted.toString());
                isFormatting = false;
            }
        });
    }
    
    private void setupOrderObservers() {
        orderViewModel.getOrderPlacementResult().observe(this, orderId -> {
            if (orderId != null && orderId > 0) {
                // Clear sensitive payment inputs from memory
                clearSensitiveInputs();
                
                // Show notification
                NotificationHelper.showNotification(
                        PaymentActivity.this,
                        orderId.intValue(),
                        "Order #" + orderId + " Confirmed!",
                        "Your print order has been placed and is now processing.",
                        new Intent(PaymentActivity.this, OrderHistoryActivity.class)
                );
                
                // Navigate to confirmation
                Intent intent = new Intent(PaymentActivity.this, OrderConfirmationActivity.class);
                intent.putExtra("ORDER_ID", orderId.intValue());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else if (orderId != null && orderId <= 0) {
                setLoadingState(false);
            }
        });

        orderViewModel.getMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                com.printxpress.app.util.PrintXpressToast.info(PaymentActivity.this, msg);
            }
        });
    }

    private boolean validatePayment() {
        int selectedId = binding.rgPaymentMethod.getCheckedRadioButtonId();
        if (selectedId == -1) {
            com.printxpress.app.util.PrintXpressToast.info(this, "Please select a payment method");
            return false;
        }
        
        if (selectedId != R.id.rbCash && selectedId != R.id.rbBankTransfer) {
            String rawCard = binding.etCardNumber.getText().toString().trim();
            String cardNumber = rawCard.replaceAll("\\s+", "");
            String cvv = binding.etCVV.getText().toString().trim();
            
            if (!ValidationHelper.isValidCardNumber(cardNumber) || !ValidationHelper.isValidCardNumberLuhn(cardNumber)) {
                binding.etCardNumber.setError("Enter a valid 16-digit card number (Luhn verified)");
                binding.etCardNumber.requestFocus();
                return false;
            }
            
            if (!ValidationHelper.isValidCVV(cvv)) {
                binding.etCVV.setError("Enter valid CVV (3-4 digits)");
                binding.etCVV.requestFocus();
                return false;
            }

            String expiry = binding.etExpiryDate != null ? binding.etExpiryDate.getText().toString().trim() : "";
            if (!ValidationHelper.isValidExpiryDate(expiry)) {
                binding.etExpiryDate.setError("Enter valid expiry (MM/YY)");
                binding.etExpiryDate.requestFocus();
                return false;
            }
        }
        return true;
    }
    
    private void processPaymentAndSaveOrder() {
        int selectedId = binding.rgPaymentMethod.getCheckedRadioButtonId();
        String paymentMethod;
        String paymentStatus;
        if (selectedId == R.id.rbCash) {
            paymentMethod = "Cash on Delivery / Pickup";
            paymentStatus = "PENDING";
        } else if (selectedId == R.id.rbBankTransfer) {
            paymentMethod = "Bank Deposit / Transfer";
            paymentStatus = "PENDING_VERIFICATION";
        } else {
            paymentMethod = "Credit / Debit Card";
            paymentStatus = "PAID";
        }
        
        orderViewModel.placeOrder(
                userId,
                deliveryMethod,
                deliveryAddressId > 0 ? deliveryAddressId : null,
                paymentMethod,
                paymentStatus,
                grandTotal,
                singleCartItemId
        );
    }

    private void clearSensitiveInputs() {
        if (binding.etCardNumber != null) {
            binding.etCardNumber.setText("");
        }
        if (binding.etCVV != null) {
            binding.etCVV.setText("");
        }
        if (binding.etExpiryDate != null) {
            binding.etExpiryDate.setText("");
        }
        if (binding.etCardholderName != null) {
            binding.etCardholderName.setText("");
        }
    }
}