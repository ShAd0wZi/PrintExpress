package com.printxpress.app.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.printxpress.app.R;
import com.printxpress.app.data.local.entity.CartItem;
import com.printxpress.app.databinding.ActivityArtworkUploadBinding;
import com.printxpress.app.util.FileHelper;
import com.printxpress.app.util.PricingCalculator;
import com.printxpress.app.util.SessionManager;
import com.printxpress.app.util.ValidationHelper;
import com.printxpress.app.viewModel.CartViewModel;
import java.io.File;

public class ArtworkUploadActivity extends AppCompatActivity {
    
    private static final String STATE_UPLOADED_FILE_PATH = "state_uploaded_file_path";
    private ActivityArtworkUploadBinding binding;
    private CartViewModel cartViewModel;
    private File uploadedFile;
    private ActivityResultLauncher<String[]> filePickerLauncher;
    
    private int productId;
    private int quantity;
    private String selectedSize;
    private String selectedMaterial;
    private String customText;
    private double unitPrice;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SessionManager.requireLogin(this)) {
            finish();
            return;
        }

        binding = ActivityArtworkUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnBack.setOnClickListener(v -> finish());
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnBack);
        
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
        
        productId = getIntent().getIntExtra("PRODUCT_ID", 0);
        quantity = getIntent().getIntExtra("QUANTITY", 1);
        selectedSize = getIntent().getStringExtra("SIZE");
        selectedMaterial = getIntent().getStringExtra("MATERIAL");
        customText = getIntent().getStringExtra("CUSTOM_TEXT");
        unitPrice = getIntent().getDoubleExtra("CALCULATED_UNIT_PRICE", 0.0);

        if (savedInstanceState != null) {
            String savedPath = savedInstanceState.getString(STATE_UPLOADED_FILE_PATH);
            if (savedPath != null) {
                File restored = new File(savedPath);
                if (ValidationHelper.isValidArtworkFile(restored)) {
                    uploadedFile = restored;
                    renderFilePreview(uploadedFile);
                }
            }
        }
        
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    // Clean up any previously selected temporary file to prevent storage leak
                    if (uploadedFile != null) {
                        FileHelper.deleteFileIfExists(uploadedFile);
                        uploadedFile = null;
                    }
                    File candidateFile = FileHelper.saveFileToInternalStorage(this, uri, "artwork_" + System.currentTimeMillis());
                    if (candidateFile != null && ValidationHelper.isValidArtworkFile(candidateFile)) {
                        uploadedFile = candidateFile;
                        renderFilePreview(uploadedFile);
                        binding.getRoot().performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                        com.printxpress.app.util.PrintXpressToast.success(this, "Artwork loaded successfully");
                    } else {
                        FileHelper.deleteFileIfExists(candidateFile);
                        com.printxpress.app.util.PrintXpressToast.error(this, "Invalid file type or size (Max 10MB PDF, PNG, JPG)");
                        clearFileSelection();
                    }
                }
            }
        );
        
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnAddToCart);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.llUploadPrompt);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnRemoveArtwork);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnViewGuidelines);

        binding.ivArtworkPreview.setOnClickListener(v -> openFilePicker());
        binding.llUploadPrompt.setOnClickListener(v -> openFilePicker());
        binding.btnRemoveArtwork.setOnClickListener(v -> clearFileSelection());
        binding.btnViewGuidelines.setOnClickListener(v -> {
            startActivity(new Intent(this, com.printxpress.app.ui.support.DesignGuidelinesActivity.class));
        });
        
        binding.btnAddToCart.setOnClickListener(v -> {
            binding.btnAddToCart.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            addToCartAndProceed();
        });
    }

    private void openFilePicker() {
        filePickerLauncher.launch(new String[]{"image/*", "application/pdf"});
    }

    private void renderFilePreview(File file) {
        if (file == null || !file.exists()) return;
        binding.llUploadPrompt.setVisibility(View.GONE);
        binding.btnRemoveArtwork.setVisibility(View.VISIBLE);
        long sizeKb = Math.max(1, file.length() / 1024);
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) {
            binding.ivArtworkPreview.setImageResource(R.drawable.ic_file_check);
            binding.ivArtworkPreview.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.primary));
            binding.tvFileName.setText("PDF: " + file.getName() + " (" + sizeKb + " KB)");
        } else {
            binding.ivArtworkPreview.clearColorFilter();
            Glide.with(this).load(file).into(binding.ivArtworkPreview);
            binding.tvFileName.setText(file.getName() + " (" + sizeKb + " KB)");
        }
    }

    private void clearFileSelection() {
        if (uploadedFile != null) {
            FileHelper.deleteFileIfExists(uploadedFile);
            uploadedFile = null;
        }
        binding.llUploadPrompt.setVisibility(View.VISIBLE);
        binding.btnRemoveArtwork.setVisibility(View.GONE);
        binding.ivArtworkPreview.clearColorFilter();
        binding.ivArtworkPreview.setImageResource(R.drawable.premium_card);
        binding.tvFileName.setText("No file selected yet");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (uploadedFile != null && uploadedFile.exists()) {
            outState.putString(STATE_UPLOADED_FILE_PATH, uploadedFile.getAbsolutePath());
        }
    }
    
    private void addToCartAndProceed() {
        if (uploadedFile == null && (customText == null || customText.isEmpty())) {
            com.printxpress.app.util.PrintXpressToast.info(this, "Please upload artwork or provide custom text");
            return;
        }
        
        if (!SessionManager.requireLogin(this)) {
            return;
        }
        int userId = SessionManager.getUserId(this);
                
        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setProductId(productId);
        cartItem.setQuantity(quantity);
        cartItem.setSelectedSize(selectedSize);
        cartItem.setSelectedMaterial(selectedMaterial);
        cartItem.setCustomText(customText);
        if (uploadedFile != null) {
            cartItem.setArtworkFilePath(uploadedFile.getAbsolutePath());
        }
        cartItem.setUnitPrice(unitPrice);
        cartItem.setSubtotal(unitPrice * quantity);
        
        cartViewModel.addToCart(cartItem);
        
        com.printxpress.app.util.PrintXpressToast.success(this, "Added to Studio Cart!");
        
        setResult(RESULT_OK);
        Intent intent = new Intent(this, CartActivity.class);
        startActivity(intent);
        finish(); // Finish artwork upload
    }
}