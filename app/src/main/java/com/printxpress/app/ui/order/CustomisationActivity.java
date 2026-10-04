package com.printxpress.app.ui.order;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.printxpress.app.R;
import com.printxpress.app.databinding.ActivityCustomisationBinding;
import com.printxpress.app.util.PricingCalculator;
import com.printxpress.app.util.SessionManager;
import com.printxpress.app.util.ValidationHelper;
import com.printxpress.app.viewModel.ProductViewModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomisationActivity extends AppCompatActivity {
    
    private ActivityCustomisationBinding binding;
    
    private int productId;
    private String productName;
    private double basePrice;
    private int availableStock = 100;
    private String selectedFinishMode = "Full CMYK";
    private androidx.activity.result.ActivityResultLauncher<Intent> artworkLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SessionManager.requireLogin(this)) {
            finish();
            return;
        }

        binding = ActivityCustomisationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        artworkLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        finish();
                    }
                }
        );
        
        binding.btnBack.setOnClickListener(v -> finish());
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnBack);
        
        productId = getIntent().getIntExtra("PRODUCT_ID", 0);
        productName = getIntent().getStringExtra("PRODUCT_NAME");
        basePrice = getIntent().getDoubleExtra("UNIT_PRICE", 0.0);
        String productImage = getIntent().getStringExtra("PRODUCT_IMAGE");
        String sizesStr = getIntent().getStringExtra("AVAILABLE_SIZES");
        String materialsStr = getIntent().getStringExtra("AVAILABLE_MATERIALS");
        
        binding.tvProductName.setText("Customize " + productName);
        
        // Load mockup preview image
        int resourceId = productImage != null ? getResources().getIdentifier(productImage, "drawable", getPackageName()) : 0;
        if (resourceId != 0) {
            Glide.with(this).load(resourceId).into(binding.ivProductMockup);
        }

        // Observe live stock level from Room database
        if (productId > 0) {
            new ViewModelProvider(this).get(ProductViewModel.class)
                    .getProductLiveData(productId).observe(this, product -> {
                        if (product != null) {
                            availableStock = product.getStockQuantity();
                            if (availableStock <= 0) {
                                binding.tvStockBadge.setText("Out of Stock");
                            } else if (availableStock <= 15) {
                                binding.tvStockBadge.setText("Low Stock: " + availableStock + " left");
                            } else {
                                binding.tvStockBadge.setText("In Stock: " + availableStock);
                            }
                        }
                    });
        }

        // Setup Print Provider / Production Hub Spinner
        String[] hubs = getResources().getStringArray(R.array.print_hubs);
        ArrayAdapter<String> hubAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, hubs);
        hubAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPrintHub.setAdapter(hubAdapter);
        
        // Parse and setup Size Spinner
        List<String> sizeList = new ArrayList<>();
        sizeList.add("Select Size");
        if (sizesStr != null && !sizesStr.isEmpty()) {
            sizeList.addAll(Arrays.asList(sizesStr.split(",\\s*")));
        } else {
            sizeList.addAll(Arrays.asList(getResources().getStringArray(R.array.sizes)));
        }
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sizeList);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSize.setAdapter(sizeAdapter);
        
        // Parse and setup Material Spinner
        List<String> materialList = new ArrayList<>();
        materialList.add("Select Material");
        if (materialsStr != null && !materialsStr.isEmpty()) {
            materialList.addAll(Arrays.asList(materialsStr.split(",\\s*")));
        } else {
            materialList.addAll(Arrays.asList(getResources().getStringArray(R.array.materials)));
        }
        ArrayAdapter<String> materialAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, materialList);
        materialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMaterial.setAdapter(materialAdapter);
        
        // Set up live price & live mockup listeners
        AdapterView.OnItemSelectedListener updatePriceListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateLivePriceAndMockup();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        
        binding.spinnerPrintHub.setOnItemSelectedListener(updatePriceListener);
        binding.spinnerSize.setOnItemSelectedListener(updatePriceListener);
        binding.spinnerMaterial.setOnItemSelectedListener(updatePriceListener);
        
        binding.etQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateLivePriceAndMockup();
            }
        });

        binding.etCustomText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateLivePriceAndMockup();
            }
        });

        // Interactive Ink/Finish Mockup Preview Toggles
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnTintCmyk);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnTintGold);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnTintMono);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnNext);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnQtyMinus);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnQtyPlus);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.chipQty50);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.chipQty100);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.chipQty250);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.chipQty500);

        binding.btnTintCmyk.setOnClickListener(v -> {
            selectedFinishMode = "Full CMYK";
            binding.ivProductMockup.clearColorFilter();
            binding.tvMockupOverlayText.setTextColor(Color.WHITE);
            updateLivePriceAndMockup();
        });

        binding.btnTintGold.setOnClickListener(v -> {
            selectedFinishMode = "Gold Foil";
            binding.ivProductMockup.setColorFilter(Color.parseColor("#33FFD700"), PorterDuff.Mode.SRC_ATOP);
            binding.tvMockupOverlayText.setTextColor(Color.parseColor("#FFD700"));
            updateLivePriceAndMockup();
        });

        binding.btnTintMono.setOnClickListener(v -> {
            selectedFinishMode = "Mono Press";
            binding.ivProductMockup.setColorFilter(Color.parseColor("#55222222"), PorterDuff.Mode.SRC_ATOP);
            binding.tvMockupOverlayText.setTextColor(Color.parseColor("#E0E0E0"));
            updateLivePriceAndMockup();
        });

        binding.btnQtyMinus.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            int current = getParsedQuantity();
            int step = current > 50 ? 10 : 1;
            int next = Math.max(1, current - step);
            binding.etQuantity.setText(String.valueOf(next));
        });

        binding.btnQtyPlus.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            int current = getParsedQuantity();
            int step = current >= 50 ? 10 : 1;
            int next = Math.min(1000, current + step);
            binding.etQuantity.setText(String.valueOf(next));
        });

        binding.chipQty50.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            binding.etQuantity.setText("50");
        });
        binding.chipQty100.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            binding.etQuantity.setText("100");
        });
        binding.chipQty250.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            binding.etQuantity.setText("250");
        });
        binding.chipQty500.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            binding.etQuantity.setText("500");
        });

        // Set default quantity
        binding.etQuantity.setText("1");
        
        binding.btnNext.setOnClickListener(v -> {
            if (validateInputs()) {
                proceedToArtworkUpload();
            }
        });
    }

    private int getParsedQuantity() {
        try {
            String qStr = binding.etQuantity.getText() != null ? binding.etQuantity.getText().toString().trim() : "";
            if (!qStr.isEmpty()) {
                return Math.max(1, Math.min(1000, Integer.parseInt(qStr)));
            }
        } catch (NumberFormatException ignored) {}
        return 1;
    }
    
    private void updateLivePriceAndMockup() {
        int quantity = getParsedQuantity();
        
        Object sizeObj = binding.spinnerSize.getSelectedItemPosition() > 0 
                ? binding.spinnerSize.getSelectedItem() : null;
        Object materialObj = binding.spinnerMaterial.getSelectedItemPosition() > 0 
                ? binding.spinnerMaterial.getSelectedItem() : null;
        Object hubObj = binding.spinnerPrintHub.getSelectedItem();

        String selectedSize = sizeObj != null ? sizeObj.toString() : null;
        String selectedMaterial = materialObj != null ? materialObj.toString() : null;
        String selectedHub = hubObj != null ? hubObj.toString() : "Colombo 03 — Heidelberg Offset Hub";

        if (selectedHub.toLowerCase().contains("kelaniya")) {
            binding.tvHubTurnaround.setText("Kelaniya Digital Press • Eco Saver (-8%) • 48h");
        } else if (selectedHub.toLowerCase().contains("kandy")) {
            binding.tvHubTurnaround.setText("Kandy Regional Hub • Outstation (-4%) • 36h");
        } else {
            binding.tvHubTurnaround.setText("Colombo 03 Offset Hub • Express 24h");
        }

        // Update Live Mockup Canvas Badge & Imprint Text
        String badgeSize = selectedSize != null ? selectedSize : "Standard Size";
        String badgeMat = selectedMaterial != null ? selectedMaterial : "Standard Stock";
        binding.tvMockupSpecBadge.setText("LIVE PROOF • " + badgeSize + " • " + badgeMat + " • " + selectedFinishMode);

        String customImprint = binding.etCustomText.getText() != null
                ? binding.etCustomText.getText().toString().trim() : "";
        if (!customImprint.isEmpty()) {
            binding.tvMockupOverlayText.setText("“" + customImprint + "”");
        } else {
            binding.tvMockupOverlayText.setText(productName + " • " + quantity + "x Batch Run");
        }
                
        double finalSubtotal = PricingCalculator.calculateItemSubtotal(
                basePrice, selectedSize, selectedMaterial, selectedHub, quantity);
        binding.tvLivePrice.setText(SessionManager.formatPrice(this, finalSubtotal));
        android.widget.TextView tvBreakdown = findViewById(R.id.tvPriceBreakdown);
        if (tvBreakdown != null) {
            tvBreakdown.setText(PricingCalculator.getPricingBreakdownSummary(
                    basePrice, selectedSize, selectedMaterial, selectedHub, quantity));
        }
    }
    
    private boolean validateInputs() {
        String quantityStr = binding.etQuantity.getText().toString().trim();
        
        if (!ValidationHelper.isValidQuantity(quantityStr)) {
            binding.etQuantity.setError("Enter valid quantity (1-1000)");
            binding.etQuantity.requestFocus();
            return false;
        }

        int requestedQty = Integer.parseInt(quantityStr);
        if (availableStock > 0 && requestedQty > availableStock) {
            com.printxpress.app.util.PrintXpressToast.info(this, "Batch print production scheduled for " + requestedQty + " units.");
        }
        
        if (binding.spinnerSize.getSelectedItemPosition() <= 0 || binding.spinnerSize.getSelectedItem() == null) {
            com.printxpress.app.util.PrintXpressToast.info(this, "Please select a print size");
            return false;
        }
        
        if (binding.spinnerMaterial.getSelectedItemPosition() <= 0 || binding.spinnerMaterial.getSelectedItem() == null) {
            com.printxpress.app.util.PrintXpressToast.info(this, "Please select a material stock");
            return false;
        }
        
        return true;
    }
    
    private void proceedToArtworkUpload() {
        int quantity = Integer.parseInt(binding.etQuantity.getText().toString().trim());
        String size = binding.spinnerSize.getSelectedItem().toString();
        String material = binding.spinnerMaterial.getSelectedItem().toString();
        String hub = binding.spinnerPrintHub.getSelectedItem() != null
                ? binding.spinnerPrintHub.getSelectedItem().toString()
                : "Colombo 03 — Heidelberg Offset Hub";
        String userText = binding.etCustomText.getText() != null
                ? binding.etCustomText.getText().toString().trim() : "";
        String combinedNotes = userText.isEmpty()
                ? "Hub: " + hub + " (" + selectedFinishMode + ")"
                : userText + " | Hub: " + hub + " (" + selectedFinishMode + ")";

        Intent intent = new Intent(this, ArtworkUploadActivity.class);
        intent.putExtra("PRODUCT_ID", productId);
        intent.putExtra("PRODUCT_NAME", productName);
        intent.putExtra("QUANTITY", quantity);
        intent.putExtra("SIZE", size);
        intent.putExtra("MATERIAL", material);
        intent.putExtra("CUSTOM_TEXT", combinedNotes);
        intent.putExtra("UNIT_PRICE", basePrice);
        
        // Pass volume & hub discounted unit price for the selected quantity
        double calculatedPrice = PricingCalculator.calculateItemPrice(
                basePrice, 
                size, 
                material,
                hub,
                quantity);
        intent.putExtra("CALCULATED_UNIT_PRICE", calculatedPrice);
        intent.putExtra("PRINT_HUB", hub);
        
        artworkLauncher.launch(intent);
    }
}