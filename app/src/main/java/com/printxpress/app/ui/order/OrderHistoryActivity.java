package com.printxpress.app.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.printxpress.app.R;
import com.printxpress.app.data.local.entity.Order;
import com.printxpress.app.databinding.ActivityOrderHistoryBinding;
import com.printxpress.app.ui.adapter.OrderHistoryAdapter;
import com.printxpress.app.util.SessionManager;
import com.printxpress.app.viewModel.OrderViewModel;
import java.util.ArrayList;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {
    
    private static final String FILTER_ALL = "ALL";
    private static final String FILTER_ACTIVE = "ACTIVE";
    private static final String FILTER_COMPLETED = "COMPLETED";
    private static final String FILTER_CANCELLED = "CANCELLED";

    private ActivityOrderHistoryBinding binding;
    private OrderViewModel viewModel;
    private OrderHistoryAdapter adapter;
    private int userId;
    private List<Order> allOrders = new ArrayList<>();
    private String currentOrderFilter = FILTER_ALL;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.requireLogin(this)) {
            finish();
            return;
        }
        userId = SessionManager.getUserId(this);

        binding = ActivityOrderHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnBack.setOnClickListener(v -> finish());
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnBack);
        
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new OrderHistoryAdapter(new ArrayList<>(), order -> {
            Intent intent = new Intent(OrderHistoryActivity.this, OrderDetailActivity.class);
            intent.putExtra("ORDER_ID", order.getOrderId());
            intent.putExtra("ORDER_STATUS", order.getOrderStatus());
            intent.putExtra("TOTAL_AMOUNT", order.getTotalAmount());
            intent.putExtra("DELIVERY_METHOD", order.getDeliveryMethod());
            startActivity(intent);
        });
        binding.rvOrders.setAdapter(adapter);

        setupFilterChips();
        
        viewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        
        viewModel.getOrdersByCustomerId(userId).observe(this, orders -> {
            binding.pbLoading.setVisibility(View.GONE);
            allOrders = orders != null ? orders : new ArrayList<>();
            applyOrderFilter();
        });
    }

    private void setupFilterChips() {
        binding.chipAll.setOnClickListener(v -> {
            currentOrderFilter = FILTER_ALL;
            updateFilterChipStyles();
            applyOrderFilter();
        });
        binding.chipActive.setOnClickListener(v -> {
            currentOrderFilter = FILTER_ACTIVE;
            updateFilterChipStyles();
            applyOrderFilter();
        });
        binding.chipCompleted.setOnClickListener(v -> {
            currentOrderFilter = FILTER_COMPLETED;
            updateFilterChipStyles();
            applyOrderFilter();
        });
        binding.chipCancelled.setOnClickListener(v -> {
            currentOrderFilter = FILTER_CANCELLED;
            updateFilterChipStyles();
            applyOrderFilter();
        });
    }

    private void updateFilterChipStyles() {
        binding.chipAll.setBackgroundResource(FILTER_ALL.equals(currentOrderFilter) ? R.drawable.bg_chip_active : R.drawable.bg_chip_neutral);
        binding.chipAll.setTextColor(ContextCompat.getColor(this, FILTER_ALL.equals(currentOrderFilter) ? R.color.white : R.color.on_surface));

        binding.chipActive.setBackgroundResource(FILTER_ACTIVE.equals(currentOrderFilter) ? R.drawable.bg_chip_active : R.drawable.bg_chip_neutral);
        binding.chipActive.setTextColor(ContextCompat.getColor(this, FILTER_ACTIVE.equals(currentOrderFilter) ? R.color.white : R.color.on_surface));

        binding.chipCompleted.setBackgroundResource(FILTER_COMPLETED.equals(currentOrderFilter) ? R.drawable.bg_chip_active : R.drawable.bg_chip_neutral);
        binding.chipCompleted.setTextColor(ContextCompat.getColor(this, FILTER_COMPLETED.equals(currentOrderFilter) ? R.color.white : R.color.on_surface));

        binding.chipCancelled.setBackgroundResource(FILTER_CANCELLED.equals(currentOrderFilter) ? R.drawable.bg_chip_active : R.drawable.bg_chip_neutral);
        binding.chipCancelled.setTextColor(ContextCompat.getColor(this, FILTER_CANCELLED.equals(currentOrderFilter) ? R.color.white : R.color.on_surface));
    }

    private void applyOrderFilter() {
        List<Order> filtered = new ArrayList<>();
        for (Order o : allOrders) {
            String st = o.getOrderStatus() != null ? o.getOrderStatus().toUpperCase() : "PENDING";
            if (FILTER_ACTIVE.equals(currentOrderFilter)) {
                if ("PENDING".equals(st) || "PROCESSING".equals(st) || "PRINTING".equals(st) || "READY".equals(st)) {
                    filtered.add(o);
                }
            } else if (FILTER_COMPLETED.equals(currentOrderFilter)) {
                if ("COMPLETED".equals(st)) {
                    filtered.add(o);
                }
            } else if (FILTER_CANCELLED.equals(currentOrderFilter)) {
                if ("CANCELLED".equals(st)) {
                    filtered.add(o);
                }
            } else {
                filtered.add(o);
            }
        }

        adapter.setOrders(filtered);
        if (filtered.isEmpty()) {
            binding.llEmptyState.setVisibility(View.VISIBLE);
            binding.rvOrders.setVisibility(View.GONE);
        } else {
            binding.llEmptyState.setVisibility(View.GONE);
            binding.rvOrders.setVisibility(View.VISIBLE);
        }
    }
}