package com.printxpress.app.ui.order;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.entity.Order;
import com.printxpress.app.data.local.entity.OrderItem;
import com.printxpress.app.data.local.entity.Product;
import com.printxpress.app.databinding.ActivityOrderTrackingBinding;
import com.printxpress.app.R;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderTrackingActivity extends AppCompatActivity {
    
    private ActivityOrderTrackingBinding binding;
    private PrintXpressDatabase db;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderTrackingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        db = PrintXpressDatabase.getDatabase(this);
        binding.btnBack.setOnClickListener(v -> finish());
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnBack);
        
        int orderId = getIntent().getIntExtra("ORDER_ID", 0);
        String initialStatus = getIntent().getStringExtra("ORDER_STATUS");
        if (initialStatus == null) initialStatus = "PROCESSING";
        
        renderTimeline(orderId, initialStatus, null, null);

        // Observe live Order row from SQLite database
        db.orderDao().getOrderLiveData(orderId).observe(this, order -> {
            if (order != null) {
                int currentUserId = com.printxpress.app.util.SessionManager.getUserId(this);
                if (!com.printxpress.app.util.SessionManager.isAdmin(this) && order.getCustomerId() != currentUserId) {
                    com.printxpress.app.util.PrintXpressToast.error(this, "Unauthorized access to order tracking.");
                    finish();
                    return;
                }
                PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
                    List<OrderItem> items = db.orderItemDao().getOrderItemsByOrderId(orderId);
                    StringBuilder itemSummary = new StringBuilder();
                    if (items != null) {
                        for (OrderItem item : items) {
                            Product p = db.productDao().getProductById(item.getProductId());
                            String name = p != null ? p.getName() : "Print Item";
                            itemSummary.append("• ").append(item.getQuantity()).append("x ").append(name).append("\n");
                        }
                    }
                    String itemsStr = itemSummary.toString().trim();
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        renderTimeline(order.getOrderId(), order.getOrderStatus(), order, itemsStr);
                    });
                });
            }
        });
    }

    private void renderTimeline(int orderId, String status, Order order, String itemsSummary) {
        if (status == null) status = "PENDING";
        binding.tvOrderId.setText("Tracking Order #" + orderId + " (" + status + ")");

        // Update Press Job Summary Card
        if (order != null && order.getOrderDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            binding.tvOrderDate.setText("Placed: " + sdf.format(order.getOrderDate()));
        } else {
            binding.tvOrderDate.setText("Placed: In Production");
        }
        if (order != null && order.getDeliveryMethod() != null) {
            binding.tvFulfillmentMethod.setText(order.getDeliveryMethod());
        } else {
            binding.tvFulfillmentMethod.setText("Standard Fulfillment");
        }
        if (itemsSummary != null && !itemsSummary.trim().isEmpty()) {
            binding.tvItemsQueue.setText(itemsSummary.trim());
        } else {
            binding.tvItemsQueue.setText("• Commercial Print Run Specs");
        }

        boolean isCancelled = "CANCELLED".equalsIgnoreCase(status);
        int activeStage = 1;
        if (isCancelled) {
            activeStage = 1;
            binding.tvTimelineLiveBadge.setText("CANCELLED");
            binding.tvTimelineLiveBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.error));
        } else if ("PROCESSING".equalsIgnoreCase(status)) {
            activeStage = 2;
        } else if ("PRINTING".equalsIgnoreCase(status)) {
            activeStage = 3;
        } else if ("READY".equalsIgnoreCase(status)) {
            activeStage = 4;
        } else if ("COMPLETED".equalsIgnoreCase(status)) {
            activeStage = 5;
        }

        bindStageNode(binding.ivStage1Node, binding.lineStage1, binding.tvStage1Title, 1, activeStage, isCancelled);
        bindStageNode(binding.ivStage2Node, binding.lineStage2, binding.tvStage2Title, 2, activeStage, isCancelled);
        bindStageNode(binding.ivStage3Node, binding.lineStage3, binding.tvStage3Title, 3, activeStage, isCancelled);
        bindStageNode(binding.ivStage4Node, binding.lineStage4, binding.tvStage4Title, 4, activeStage, isCancelled);
        bindStageNode(binding.ivStage5Node, null, binding.tvStage5Title, 5, activeStage, isCancelled);
    }

    private void bindStageNode(android.widget.ImageView node, android.view.View line, android.widget.TextView title,
                               int stageIndex, int activeStage, boolean isCancelled) {
        int colorPrimary = androidx.core.content.ContextCompat.getColor(this, R.color.primary_container);
        int colorOutline = androidx.core.content.ContextCompat.getColor(this, R.color.outline);
        int colorVariant = androidx.core.content.ContextCompat.getColor(this, R.color.outline_variant);
        int colorTextNormal = androidx.core.content.ContextCompat.getColor(this, R.color.on_surface);
        int colorTextMuted = androidx.core.content.ContextCompat.getColor(this, R.color.on_surface_variant);
        int colorError = androidx.core.content.ContextCompat.getColor(this, R.color.error);

        if (isCancelled) {
            node.setImageResource(R.drawable.ic_circle_active);
            node.setColorFilter(colorError);
            if (line != null) line.setBackgroundColor(colorVariant);
            title.setTextColor(colorError);
            return;
        }

        if (stageIndex < activeStage) {
            // Completed stage
            node.setImageResource(R.drawable.ic_check_circle);
            node.setColorFilter(colorPrimary);
            if (line != null) line.setBackgroundColor(colorPrimary);
            title.setTextColor(colorTextNormal);
        } else if (stageIndex == activeStage) {
            // Current in-progress stage
            node.setImageResource(stageIndex == 5 ? R.drawable.ic_check_circle : R.drawable.ic_circle_active);
            node.setColorFilter(colorPrimary);
            if (line != null) line.setBackgroundColor(colorVariant);
            title.setTextColor(colorPrimary);
        } else {
            // Upcoming stage
            node.setImageResource(R.drawable.ic_circle_outline);
            node.setColorFilter(colorOutline);
            if (line != null) line.setBackgroundColor(colorVariant);
            title.setTextColor(colorTextMuted);
        }
    }
}