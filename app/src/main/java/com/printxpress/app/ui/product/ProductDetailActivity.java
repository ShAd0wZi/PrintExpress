package com.printxpress.app.ui.product;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.printxpress.app.R;
import com.printxpress.app.databinding.ActivityProductDetailBinding;
import com.printxpress.app.ui.order.CustomisationActivity;
import com.printxpress.app.viewModel.ProductViewModel;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {
    
    private ActivityProductDetailBinding binding;
    private int productId;
    private String productName;
    private double productPrice;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnBack.setOnClickListener(v -> finish());
        
        productId = getIntent().getIntExtra("PRODUCT_ID", 0);
        productName = getIntent().getStringExtra("PRODUCT_NAME");
        productPrice = getIntent().getDoubleExtra("PRODUCT_PRICE", 0.0);
        String productImage = getIntent().getStringExtra("PRODUCT_IMAGE");
        
        if (productImage != null) {
            int resourceId = getResources().getIdentifier(productImage, "drawable", getPackageName());
            if (resourceId != 0) {
                Glide.with(this).load(resourceId).into(binding.ivProduct);
            } else {
                Glide.with(this).load(R.color.primary_light).into(binding.ivProduct);
            }
        }
        
        String description = getIntent().getStringExtra("PRODUCT_DESCRIPTION");
        if (description != null && !description.isEmpty()) {
            binding.tvDescription.setText(description);
        }

        new ViewModelProvider(this).get(ProductViewModel.class)
                .getProductLiveData(productId).observe(this, prod -> {
                    if (prod != null) {
                        if (prod.getDescription() != null && (description == null || description.isEmpty())) {
                            binding.tvDescription.setText(prod.getDescription());
                        }
                        int stock = prod.getStockQuantity();
                        if (stock <= 0) {
                            binding.tvDetailStockBadge.setText("⚠️ Out of Stock (0)");
                            binding.tvDetailStockBadge.setBackgroundResource(R.drawable.bg_badge_cancelled);
                            binding.tvDetailStockBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.status_cancelled_text));
                        } else if (stock <= 15) {
                            binding.tvDetailStockBadge.setText("⚠️ Low Stock: " + com.printxpress.app.util.SessionManager.formatNumber(stock) + " left");
                            binding.tvDetailStockBadge.setBackgroundResource(R.drawable.bg_badge_printing);
                            binding.tvDetailStockBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.status_printing_text));
                        } else {
                            binding.tvDetailStockBadge.setText("📦 In Stock (" + com.printxpress.app.util.SessionManager.formatNumber(stock) + " units)");
                            binding.tvDetailStockBadge.setBackgroundResource(R.drawable.bg_badge_ready);
                            binding.tvDetailStockBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.status_ready_text));
                        }
                    }
                });
        
        binding.tvName.setText(productName);
        binding.tvPrice.setText(com.printxpress.app.util.SessionManager.formatPrice(this, productPrice));
        
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnBack);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnShareProduct);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnCustomise);

        binding.btnShareProduct.setOnClickListener(v -> {
            binding.btnShareProduct.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            String shareBody = "Check out " + productName + " on PrintXpress Sri Lanka!\n" +
                    "Starting at " + com.printxpress.app.util.SessionManager.formatPrice(this, productPrice) + "\n" +
                    "Commercial offset & digital pre-press specs: " + (description != null ? description : "High-fidelity industrial printing");
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, productName + " - PrintXpress Specs");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(shareIntent, "Share Print Specifications"));
        });

        binding.btnCustomise.setOnClickListener(v -> {
            if (!com.printxpress.app.util.SessionManager.requireLogin(ProductDetailActivity.this)) {
                return;
            }
            Intent intent = new Intent(ProductDetailActivity.this, CustomisationActivity.class);
            intent.putExtra("PRODUCT_ID", productId);
            intent.putExtra("PRODUCT_NAME", productName);
            intent.putExtra("UNIT_PRICE", productPrice);
            intent.putExtra("PRODUCT_IMAGE", productImage);
            intent.putExtra("AVAILABLE_SIZES", getIntent().getStringExtra("AVAILABLE_SIZES"));
            intent.putExtra("AVAILABLE_MATERIALS", getIntent().getStringExtra("AVAILABLE_MATERIALS"));
            startActivity(intent);
        });
    }
}