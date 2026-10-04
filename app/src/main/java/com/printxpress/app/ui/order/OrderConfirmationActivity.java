package com.printxpress.app.ui.order;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.entity.Order;
import com.printxpress.app.data.local.entity.OrderItem;
import com.printxpress.app.data.local.entity.Product;
import com.printxpress.app.databinding.ActivityOrderConfirmationBinding;
import com.printxpress.app.ui.home.HomeActivity;
import com.printxpress.app.util.PdfReceiptGenerator;
import com.printxpress.app.util.SessionManager;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderConfirmationActivity extends AppCompatActivity {
    
    private ActivityOrderConfirmationBinding binding;
    private int orderId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderConfirmationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        orderId = getIntent().getIntExtra("ORDER_ID", 0);
        
        binding.tvOrderId.setText("Order #" + orderId);
        
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnDownloadReceipt);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnShareReceipt);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnViewOrders);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnHome);

        binding.btnDownloadReceipt.setOnClickListener(v -> generateAndOpenPdf());
        binding.btnShareReceipt.setOnClickListener(v -> generateAndSharePdf());
        
        binding.btnViewOrders.setOnClickListener(v -> {
            Intent intent = new Intent(OrderConfirmationActivity.this, OrderTrackingActivity.class);
            intent.putExtra("ORDER_ID", orderId);
            intent.putExtra("ORDER_STATUS", "PROCESSING");
            startActivity(intent);
            finish();
        });
        
        binding.btnHome.setOnClickListener(v -> navigateHome());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateHome();
            }
        });
    }

    private void generateAndSharePdf() {
        binding.btnShareReceipt.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        com.printxpress.app.util.PrintXpressToast.info(this, "Preparing invoice to share...");
        PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
            PrintXpressDatabase db = PrintXpressDatabase.getDatabase(this);
            Order order = db.orderDao().getOrderById(orderId);
            if (order != null) {
                List<OrderItem> items = db.orderItemDao().getOrderItemsByOrderId(orderId);
                Map<Integer, Product> productMap = new HashMap<>();
                if (items != null) {
                    for (OrderItem item : items) {
                        if (!productMap.containsKey(item.getProductId())) {
                            Product p = db.productDao().getProductById(item.getProductId());
                            if (p != null) {
                                productMap.put(p.getProductId(), p);
                            }
                        }
                    }
                }
                String customerName = SessionManager.getUserName(this);
                File pdfFile = PdfReceiptGenerator.generateDetailedReceipt(this, order, items, productMap, customerName);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (pdfFile != null && pdfFile.exists()) {
                        try {
                            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("application/pdf");
                            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "PrintXpress Commercial Invoice - Order #" + orderId);
                            shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is the commercial tax invoice and pre-press receipt for PrintXpress Order #" + orderId + ".");
                            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(shareIntent, "Share Commercial Invoice"));
                        } catch (Exception e) {
                            com.printxpress.app.util.PrintXpressToast.error(this, "Could not share PDF: " + e.getMessage());
                        }
                    } else {
                        com.printxpress.app.util.PrintXpressToast.error(this, "Failed to generate receipt PDF.");
                    }
                });
            }
        });
    }

    private void navigateHome() {
        Intent intent = new Intent(OrderConfirmationActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
    
    private void generateAndOpenPdf() {
        com.printxpress.app.util.PrintXpressToast.info(this, "Generating signed PDF receipt...");
        
        PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
            PrintXpressDatabase db = PrintXpressDatabase.getDatabase(this);
            Order order = db.orderDao().getOrderById(orderId);
            
            if (order != null) {
                List<OrderItem> items = db.orderItemDao().getOrderItemsByOrderId(orderId);
                Map<Integer, Product> productMap = new HashMap<>();
                if (items != null) {
                    for (OrderItem item : items) {
                        if (!productMap.containsKey(item.getProductId())) {
                            Product p = db.productDao().getProductById(item.getProductId());
                            if (p != null) {
                                productMap.put(p.getProductId(), p);
                            }
                        }
                    }
                }
                
                String customerName = SessionManager.getUserName(this);
                File pdfFile = PdfReceiptGenerator.generateDetailedReceipt(this, order, items, productMap, customerName);
                
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (pdfFile != null && pdfFile.exists()) {
                        openPdf(pdfFile);
                    } else {
                        com.printxpress.app.util.PrintXpressToast.error(this, "Failed to generate receipt PDF.");
                    }
                });
            } else {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    com.printxpress.app.util.PrintXpressToast.error(this, "Order not found in database.");
                });
            }
        });
    }
    
    private void openPdf(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            com.printxpress.app.util.PrintXpressToast.info(this, "No PDF viewer installed on this device.");
        }
    }
}