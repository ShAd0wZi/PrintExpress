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
    
    public static final int PAYMENT_MODE_CARD = 1;
    public static final int PAYMENT_MODE_CASH = 2;
    public static final int PAYMENT_MODE_BANK = 3;
    public static final int PAYMENT_MODE_QR = 4;

    private int selectedPaymentMode = PAYMENT_MODE_CARD;

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

        if (binding.tvQrTotalAmount != null) {
            binding.tvQrTotalAmount.setText(SessionManager.formatPrice(this, grandTotal));
        }
        if (binding.tvQrReference != null) {
            int seed = Math.abs((userId * 31 + (deliveryMethod != null ? deliveryMethod.hashCode() : 0) + (int) (grandTotal * 100) + singleCartItemId));
            long refNum = (seed % 900000L) + 100000L;
            binding.tvQrReference.setText("REF: LK-PX-" + refNum);
        }
        
        // Single selection across all 4 payment options (guarantees mutual exclusion)
        binding.layoutOptionCard.setOnClickListener(v -> selectPaymentMode(PAYMENT_MODE_CARD));
        binding.rbCard.setOnClickListener(v -> selectPaymentMode(PAYMENT_MODE_CARD));

        binding.layoutOptionCash.setOnClickListener(v -> selectPaymentMode(PAYMENT_MODE_CASH));
        binding.rbCash.setOnClickListener(v -> selectPaymentMode(PAYMENT_MODE_CASH));

        binding.layoutOptionBank.setOnClickListener(v -> selectPaymentMode(PAYMENT_MODE_BANK));
        binding.rbBankTransfer.setOnClickListener(v -> selectPaymentMode(PAYMENT_MODE_BANK));

        binding.layoutOptionQr.setOnClickListener(v -> selectPaymentMode(PAYMENT_MODE_QR));
        binding.rbOnlineQr.setOnClickListener(v -> selectPaymentMode(PAYMENT_MODE_QR));

        selectPaymentMode(PAYMENT_MODE_CARD);
        
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

    private void selectPaymentMode(int mode) {
        selectedPaymentMode = mode;

        binding.rbCard.setChecked(mode == PAYMENT_MODE_CARD);
        binding.rbCash.setChecked(mode == PAYMENT_MODE_CASH);
        binding.rbBankTransfer.setChecked(mode == PAYMENT_MODE_BANK);
        binding.rbOnlineQr.setChecked(mode == PAYMENT_MODE_QR);

        binding.llCardDetails.setVisibility(mode == PAYMENT_MODE_CARD ? View.VISIBLE : View.GONE);
        if (binding.llBankDetails != null) {
            binding.llBankDetails.setVisibility(mode == PAYMENT_MODE_BANK ? View.VISIBLE : View.GONE);
        }
        if (binding.llQrDetails != null) {
            binding.llQrDetails.setVisibility(mode == PAYMENT_MODE_QR ? View.VISIBLE : View.GONE);
        }

        switch (mode) {
            case PAYMENT_MODE_CASH:
                binding.btnPay.setText("Confirm Order (Cash on Delivery)  →");
                break;
            case PAYMENT_MODE_BANK:
                binding.btnPay.setText("Confirm Order (Bank Transfer)  →");
                break;
            case PAYMENT_MODE_QR:
                binding.btnPay.setText("Confirm Order (LankaQR Verified)  →");
                break;
            case PAYMENT_MODE_CARD:
            default:
                binding.btnPay.setText("Pay Now  →");
                break;
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (binding.pbPaymentLoading != null) {
            binding.pbPaymentLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        binding.btnPay.setEnabled(!isLoading);
        if (isLoading) {
            binding.btnPay.setText("Processing Order...");
        } else {
            selectPaymentMode(selectedPaymentMode);
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
        if (selectedPaymentMode == PAYMENT_MODE_CARD) {
            String rawCard = binding.etCardNumber.getText() != null ? binding.etCardNumber.getText().toString().trim() : "";
            String cardNumber = rawCard.replaceAll("\\s+", "");
            String cvv = binding.etCVV.getText() != null ? binding.etCVV.getText().toString().trim() : "";
            
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

            String expiry = binding.etExpiryDate != null && binding.etExpiryDate.getText() != null
                    ? binding.etExpiryDate.getText().toString().trim() : "";
            if (!ValidationHelper.isValidExpiryDate(expiry)) {
                binding.etExpiryDate.setError("Enter valid expiry (MM/YY)");
                binding.etExpiryDate.requestFocus();
                return false;
            }
        }
        return true;
    }
    
    private void processPaymentAndSaveOrder() {
        String paymentMethod;
        String paymentStatus;
        switch (selectedPaymentMode) {
            case PAYMENT_MODE_CASH:
                paymentMethod = "Cash on Delivery / Pickup";
                paymentStatus = "PENDING";
                break;
            case PAYMENT_MODE_BANK:
                paymentMethod = "Bank Deposit / Transfer";
                paymentStatus = "PENDING_VERIFICATION";
                break;
            case PAYMENT_MODE_QR:
                paymentMethod = "Online Banking / LankaQR";
                paymentStatus = "PAID";
                break;
            case PAYMENT_MODE_CARD:
            default:
                paymentMethod = "Credit / Debit Card";
                paymentStatus = "PAID";
                break;
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