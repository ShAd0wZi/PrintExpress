package com.printxpress.app.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.printxpress.app.R;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.entity.Category;
import com.printxpress.app.data.local.entity.Notification;
import com.printxpress.app.data.local.entity.Order;
import com.printxpress.app.data.local.entity.Product;
import com.printxpress.app.databinding.ActivityAdminDashboardBinding;
import com.printxpress.app.databinding.DialogManageOrderBinding;
import com.printxpress.app.ui.adapter.AdminInventoryAdapter;
import com.printxpress.app.ui.adapter.OrderHistoryAdapter;
import com.printxpress.app.ui.order.OrderDetailActivity;
import com.printxpress.app.ui.product.CategoryListActivity;
import com.printxpress.app.ui.product.ProductListActivity;
import com.printxpress.app.ui.support.PromotionsActivity;
import com.printxpress.app.util.SessionManager;
import com.printxpress.app.viewModel.OrderViewModel;
import com.printxpress.app.viewModel.ProductViewModel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String FILTER_ALL = "ALL";
    private static final String FILTER_ACTIVE = "ACTIVE";
    private static final String FILTER_COMPLETED = "COMPLETED";
    private static final String FILTER_CANCELLED = "CANCELLED";

    private ActivityAdminDashboardBinding binding;
    private OrderViewModel orderViewModel;
    private ProductViewModel productViewModel;
    private OrderHistoryAdapter orderAdapter;
    private AdminInventoryAdapter inventoryAdapter;
    private PrintXpressDatabase db;

    private List<Order> allOrders = new ArrayList<>();
    private List<Product> lowStockProducts = new ArrayList<>();
    private List<Category> allCategoriesList = new ArrayList<>();
    private String currentOrderFilter = FILTER_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.isAdmin(this)) {
            com.printxpress.app.util.PrintXpressToast.error(this, "Access denied: Administrator privileges required.");
            finish();
            return;
        }

        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = PrintXpressDatabase.getDatabase(this);
        binding.btnBack.setOnClickListener(v -> finish());
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnBack);

        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        setupRecyclerView();
        setupQuickActions();
        setupOrderFilterChips();
        observeAdminMetricsAndQueue();
    }

    private void setupRecyclerView() {
        // Customer Orders Recycler
        binding.rvAdminOrders.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderHistoryAdapter(new ArrayList<>(), this::showOrderAdminActionDialog);
        binding.rvAdminOrders.setAdapter(orderAdapter);

        // Inventory Stock Levels Recycler
        binding.rvAdminInventory.setLayoutManager(new LinearLayoutManager(this));
        inventoryAdapter = new AdminInventoryAdapter(
                new ArrayList<>(),
                (product, addUnits) -> {
                    int newStock = product.getStockQuantity() + addUnits;
                    productViewModel.updateStock(product.getProductId(), newStock);
                    com.printxpress.app.util.PrintXpressToast.success(this, "Restocked " + product.getName() + " to " + newStock + " units!");
                },
                product -> com.printxpress.app.util.ProductDialogHelper.showAddEditDialog(
                        this,
                        productViewModel,
                        product,
                        allCategoriesList,
                        product.getCategoryId(),
                        () -> com.printxpress.app.util.PrintXpressToast.success(this, "Stock specifications updated")
                )
        );
        binding.rvAdminInventory.setAdapter(inventoryAdapter);
    }

    private void setupQuickActions() {
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnAddProductDirect);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnManageProducts);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnManageCategories);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnManagePromos);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnRestockLowItems);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.cardLowStockKpi);

        binding.btnAddProductDirect.setOnClickListener(v -> {
            com.printxpress.app.util.ProductDialogHelper.showAddEditDialog(
                    this,
                    productViewModel,
                    null,
                    allCategoriesList,
                    1,
                    () -> com.printxpress.app.util.PrintXpressToast.success(this, "Product added to catalog")
            );
        });

        binding.btnManageProducts.setOnClickListener(v ->
                startActivity(new Intent(this, ProductListActivity.class)));
        binding.btnManageCategories.setOnClickListener(v ->
                startActivity(new Intent(this, CategoryListActivity.class)));
        binding.btnManagePromos.setOnClickListener(v ->
                startActivity(new Intent(this, PromotionsActivity.class)));

        binding.btnRestockLowItems.setOnClickListener(v -> restockAllLowInventoryItems());
        binding.cardLowStockKpi.setOnClickListener(v -> {
            if (!lowStockProducts.isEmpty()) {
                restockAllLowInventoryItems();
            } else {
                com.printxpress.app.util.PrintXpressToast.info(this, "All products have healthy inventory (>15 units)");
            }
        });
    }

    private void setupOrderFilterChips() {
        binding.chipOrdersAll.setOnClickListener(v -> {
            currentOrderFilter = FILTER_ALL;
            updateFilterChipStyles();
            applyOrderFilter();
        });
        binding.chipOrdersActive.setOnClickListener(v -> {
            currentOrderFilter = FILTER_ACTIVE;
            updateFilterChipStyles();
            applyOrderFilter();
        });
        binding.chipOrdersCompleted.setOnClickListener(v -> {
            currentOrderFilter = FILTER_COMPLETED;
            updateFilterChipStyles();
            applyOrderFilter();
        });
        binding.chipOrdersCancelled.setOnClickListener(v -> {
            currentOrderFilter = FILTER_CANCELLED;
            updateFilterChipStyles();
            applyOrderFilter();
        });
    }

    private void updateFilterChipStyles() {
        binding.chipOrdersAll.setBackgroundResource(FILTER_ALL.equals(currentOrderFilter) ? R.drawable.bg_pill_location : R.drawable.bg_search_box);
        binding.chipOrdersActive.setBackgroundResource(FILTER_ACTIVE.equals(currentOrderFilter) ? R.drawable.bg_pill_location : R.drawable.bg_search_box);
        binding.chipOrdersCompleted.setBackgroundResource(FILTER_COMPLETED.equals(currentOrderFilter) ? R.drawable.bg_pill_location : R.drawable.bg_search_box);
        binding.chipOrdersCancelled.setBackgroundResource(FILTER_CANCELLED.equals(currentOrderFilter) ? R.drawable.bg_pill_location : R.drawable.bg_search_box);
    }

    private void observeAdminMetricsAndQueue() {
        orderViewModel.getTotalPlatformRevenue().observe(this, revenue -> {
            double safeRev = revenue != null ? revenue : 0.0;
            binding.tvAdminRevenue.setText(SessionManager.formatPrice(this, safeRev));
        });

        orderViewModel.getTotalOrderCount().observe(this, count -> {
            int safeCount = count != null ? count : 0;
            binding.tvAdminTotalOrders.setText(SessionManager.formatNumber(safeCount) + (safeCount == 1 ? " Order" : " Orders"));
        });

        orderViewModel.getActivePressQueueCount().observe(this, activeCount -> {
            int safeActive = activeCount != null ? activeCount : 0;
            binding.tvAdminActiveQueue.setText(SessionManager.formatNumber(safeActive) + (safeActive == 1 ? " Active Run" : " Active Runs"));
        });

        productViewModel.getLowStockProducts(15).observe(this, products -> {
            lowStockProducts = products != null ? products : new ArrayList<>();
            int count = lowStockProducts.size();
            binding.tvAdminLowStockCount.setText(count + (count == 1 ? " Alert" : " Alerts"));
            if (count > 0) {
                binding.cardLowStockBanner.setVisibility(View.VISIBLE);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lowStockProducts.size(); i++) {
                    Product p = lowStockProducts.get(i);
                    if (i > 0) sb.append(" • ");
                    sb.append(p.getName()).append(" (").append(p.getStockQuantity()).append(" left)");
                }
                binding.tvLowStockNames.setText(sb.toString());
            } else {
                binding.cardLowStockBanner.setVisibility(View.GONE);
            }
        });

        // Live Inventory Observation
        productViewModel.getAllProducts().observe(this, products -> {
            if (products != null) {
                inventoryAdapter.setProducts(products);
                binding.tvAdminInventorySummary.setText(SessionManager.formatNumber(products.size()) + (products.size() == 1 ? " Product Tracked" : " Products Tracked"));
            }
        });

        orderViewModel.getAllOrders().observe(this, orders -> {
            allOrders = orders != null ? orders : new ArrayList<>();
            applyOrderFilter();
        });

        productViewModel.getAllCategories().observe(this, cats -> {
            if (cats != null) {
                allCategoriesList = cats;
                inventoryAdapter.setCategories(cats);
            }
        });
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

        orderAdapter.setOrders(filtered);
        binding.tvEmptyAdminOrders.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvAdminOrders.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void restockAllLowInventoryItems() {
        if (lowStockProducts.isEmpty()) return;
        for (Product p : lowStockProducts) {
            productViewModel.updateStock(p.getProductId(), p.getStockQuantity() + 50);
        }
        com.printxpress.app.util.PrintXpressToast.success(this, "Restocked +50 units for all low-inventory items!");
    }

    private void showOrderAdminActionDialog(Order order) {
        DialogManageOrderBinding dialogBinding = DialogManageOrderBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.70f);
        }

        dialogBinding.tvOrderNumberTitle.setText("Manage Press Order #" + order.getOrderId());
        dialogBinding.tvOrderDetailsSub.setText("Customer #" + order.getCustomerId() + " • Total " + SessionManager.formatPrice(this, order.getTotalAmount()));
        dialogBinding.tvCurrentStatusBadge.setText(order.getOrderStatus() != null ? order.getOrderStatus() : "PENDING");

        String currentStatus = order.getOrderStatus() != null ? order.getOrderStatus().toUpperCase() : "PROCESSING";
        switch (currentStatus) {
            case "PRINTING":
                dialogBinding.rbStatusPrinting.setChecked(true);
                break;
            case "READY":
                dialogBinding.rbStatusReady.setChecked(true);
                break;
            case "COMPLETED":
                dialogBinding.rbStatusCompleted.setChecked(true);
                break;
            case "CANCELLED":
                dialogBinding.rbStatusCancelled.setChecked(true);
                break;
            default:
                dialogBinding.rbStatusProcessing.setChecked(true);
                break;
        }

        dialogBinding.btnViewOrderSpecs.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("ORDER_ID", order.getOrderId());
            intent.putExtra("ORDER_STATUS", order.getOrderStatus());
            startActivity(intent);
        });

        dialogBinding.btnCloseManageOrder.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnUpdateStatus.setOnClickListener(v -> {
            String targetStatus = "PROCESSING";
            if (dialogBinding.rbStatusPrinting.isChecked()) {
                targetStatus = "PRINTING";
            } else if (dialogBinding.rbStatusReady.isChecked()) {
                targetStatus = "READY";
            } else if (dialogBinding.rbStatusCompleted.isChecked()) {
                targetStatus = "COMPLETED";
            } else if (dialogBinding.rbStatusCancelled.isChecked()) {
                targetStatus = "CANCELLED";
            }

            final String finalStatus = targetStatus;
            orderViewModel.updateOrderStatus(order.getOrderId(), finalStatus);
            PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
                Notification notif = new Notification();
                notif.setUserId(order.getCustomerId());
                notif.setTitle("Order #" + order.getOrderId() + " Status: " + finalStatus);
                notif.setMessage("Press Admin updated your order #" + order.getOrderId() + " to " + finalStatus + ".");
                notif.setTimestamp(new Date());
                notif.setRead(false);
                notif.setType("ORDER");
                db.notificationDao().insert(notif);
            });
            com.printxpress.app.util.PrintXpressToast.success(this, "Order #" + order.getOrderId() + " updated to " + finalStatus);
            dialog.dismiss();
        });

        dialog.show();
    }
}
