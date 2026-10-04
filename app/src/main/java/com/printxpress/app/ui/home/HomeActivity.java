package com.printxpress.app.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.printxpress.app.R;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.entity.Address;
import com.printxpress.app.data.local.entity.Order;
import com.printxpress.app.data.local.entity.OrderItem;
import com.printxpress.app.data.local.entity.Product;
import com.printxpress.app.data.local.entity.Promotion;
import com.printxpress.app.databinding.ActivityHomeBinding;
import com.printxpress.app.ui.adapter.CategoryAdapter;
import com.printxpress.app.ui.order.CartActivity;
import com.printxpress.app.ui.order.OrderHistoryActivity;
import com.printxpress.app.ui.order.OrderTrackingActivity;
import com.printxpress.app.ui.product.CategoryListActivity;
import com.printxpress.app.ui.product.ProductListActivity;
import com.printxpress.app.ui.profile.ProfileActivity;
import com.printxpress.app.ui.profile.SavedAddressesActivity;
import com.printxpress.app.ui.support.NotificationsActivity;
import com.printxpress.app.ui.support.PromotionsActivity;
import com.printxpress.app.util.SessionManager;
import com.printxpress.app.viewModel.ProductViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class HomeActivity extends AppCompatActivity {
    
    private static final int REQ_POST_NOTIFICATIONS = 2001;
    private ActivityHomeBinding binding;
    private ProductViewModel viewModel;
    private CategoryAdapter categoryAdapter;
    private PrintXpressDatabase db;
    private final ExecutorService executorService = PrintXpressDatabase.databaseWriteExecutor;

    private int observedUserId = -2;
    private LiveData<Integer> unreadLiveData;
    private LiveData<List<Address>> addressesLiveData;
    private LiveData<List<Order>> ordersLiveData;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        db = PrintXpressDatabase.getDatabase(this);
        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        requestNotificationPermissionIfNeeded();
        
        boolean isTabletOrLandscape = getResources().getConfiguration().smallestScreenWidthDp >= 600
                || getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        if (isTabletOrLandscape) {
            binding.rvCategories.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        } else {
            binding.rvCategories.setLayoutManager(new LinearLayoutManager(this));
        }
        categoryAdapter = new CategoryAdapter(new ArrayList<>(), category -> {
            Intent intent = new Intent(HomeActivity.this, ProductListActivity.class);
            intent.putExtra("CATEGORY_ID", category.getCategoryId());
            intent.putExtra("CATEGORY_NAME", category.getName());
            startActivity(intent);
        });
        binding.rvCategories.setAdapter(categoryAdapter);
        
        // Show loading spinner initially
        binding.pbCategories.setVisibility(View.VISIBLE);
        
        // 1. Dynamically observe Categories from Database
        viewModel.getAllCategories().observe(this, categories -> {
            binding.pbCategories.setVisibility(View.GONE);
            if (categories != null) {
                categoryAdapter.setCategories(categories);
            }
        });

        // 2. Dynamically observe Active Promotions from Database for the Hero Banner
        db.promotionDao().getActivePromotions().observe(this, promotions -> {
            if (promotions != null && !promotions.isEmpty()) {
                Promotion featured = promotions.get(0);
                binding.promoBanner.setVisibility(View.VISIBLE);
                binding.tvPromoTitle.setText(featured.getTitle());
                binding.tvPromoDescription.setText(
                        featured.getDescription() + " Apply code " + featured.getCode() + " at checkout.");
                binding.tvPromoMinOrder.setText(
                        String.format(Locale.getDefault(), "Min. Rs. %,.0f", featured.getMinOrderAmount()));

                if (featured.getValidUntil() != null) {
                    long diffMs = featured.getValidUntil().getTime() - System.currentTimeMillis();
                    long days = Math.max(1, TimeUnit.MILLISECONDS.toDays(diffMs));
                    binding.tvPromoExpiry.setText("Expires in " + days + (days == 1 ? " day" : " days"));
                } else {
                    binding.tvPromoExpiry.setText("Limited Time");
                }
            } else {
                binding.promoBanner.setVisibility(View.GONE);
            }
        });

        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.promoBanner);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnShopNow);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.llDeliveryLocation);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.ivNotifications);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.cvProfileHeader);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnSearchGo);

        binding.promoBanner.setOnClickListener(v -> {
            startActivity(new Intent(this, PromotionsActivity.class));
        });

        binding.llDeliveryLocation.setOnClickListener(v -> {
            if (SessionManager.requireLogin(this)) {
                startActivity(new Intent(this, SavedAddressesActivity.class));
            }
        });
        
        binding.ivNotifications.setOnClickListener(v -> {
            if (SessionManager.requireLogin(this)) {
                startActivity(new Intent(this, NotificationsActivity.class));
            }
        });

        binding.cvProfileHeader.setOnClickListener(v -> {
            if (SessionManager.requireLogin(this)) {
                startActivity(new Intent(this, ProfileActivity.class));
            }
        });

        binding.tvSeeAllCategories.setOnClickListener(v -> {
            startActivity(new Intent(this, CategoryListActivity.class));
        });

        binding.btnShopNow.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProductListActivity.class);
            intent.putExtra("CATEGORY_ID", 0);
            intent.putExtra("CATEGORY_NAME", "All Commercial Print Products");
            startActivity(intent);
        });

        binding.etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s != null && s.length() > 0) {
                    binding.btnClearSearch.setVisibility(View.VISIBLE);
                } else {
                    binding.btnClearSearch.setVisibility(View.GONE);
                }
            }
        });

        binding.btnClearSearch.setOnClickListener(v -> {
            binding.etSearch.setText("");
            binding.btnClearSearch.setVisibility(View.GONE);
        });

        binding.llSearchContainer.setOnClickListener(v -> {
            binding.etSearch.requestFocus();
            showSoftKeyboard(binding.etSearch);
        });

        binding.ivSearchStartIcon.setOnClickListener(v -> triggerSearch());

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                    || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                triggerSearch();
                return true;
            }
            return false;
        });

        binding.btnSearchGo.setOnClickListener(v -> triggerSearch());
        
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_orders) {
                if (!SessionManager.requireLogin(HomeActivity.this)) {
                    return false;
                }
                Intent intent = new Intent(HomeActivity.this, OrderHistoryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_cart) {
                if (!SessionManager.requireLogin(HomeActivity.this)) {
                    return false;
                }
                Intent intent = new Intent(HomeActivity.this, CartActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_profile) {
                if (!SessionManager.requireLogin(HomeActivity.this)) {
                    return false;
                }
                Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            return false;
        });

        bindUserSpecificDatabaseStreams();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTIFICATIONS
                );
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.bottomNavigation.getMenu().findItem(R.id.nav_home).setChecked(true);
        bindUserSpecificDatabaseStreams();
    }

    private void triggerSearch() {
        hideSoftKeyboard(binding.etSearch);
        String query = binding.etSearch.getText().toString().trim();
        Intent intent = new Intent(HomeActivity.this, ProductListActivity.class);
        if (!query.isEmpty()) {
            intent.putExtra("SEARCH_QUERY", query);
        } else {
            intent.putExtra("CATEGORY_ID", 0);
            intent.putExtra("CATEGORY_NAME", "All Commercial Print Products");
        }
        startActivity(intent);
    }

    private void hideSoftKeyboard(View view) {
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void showSoftKeyboard(View view) {
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private void bindUserSpecificDatabaseStreams() {
        int currentUserId = SessionManager.isLoggedIn(this) ? SessionManager.getUserId(this) : SessionManager.NO_USER;
        if (currentUserId == observedUserId) {
            return;
        }
        observedUserId = currentUserId;

        if (unreadLiveData != null) unreadLiveData.removeObservers(this);
        if (addressesLiveData != null) addressesLiveData.removeObservers(this);
        if (ordersLiveData != null) ordersLiveData.removeObservers(this);

        if (currentUserId == SessionManager.NO_USER) {
            binding.tvNotificationBadge.setVisibility(View.GONE);
            binding.tvDeliveryLocation.setText("Sign in to set delivery address");
            binding.cvLiveJobSnippet.setVisibility(View.GONE);
            return;
        }

        // 3. Dynamically observe Unread Notification Count from Database
        unreadLiveData = db.notificationDao().getUnreadCountLive(currentUserId);
        unreadLiveData.observe(this, count -> {
            if (count != null && count > 0) {
                binding.tvNotificationBadge.setVisibility(View.VISIBLE);
                binding.tvNotificationBadge.setText(String.valueOf(Math.min(count, 99)));
            } else {
                binding.tvNotificationBadge.setVisibility(View.GONE);
            }
        });

        // 4. Dynamically observe User's Saved Addresses from Database
        addressesLiveData = db.addressDao().getAddressesByUserId(currentUserId);
        addressesLiveData.observe(this, addresses -> {
            if (addresses != null && !addresses.isEmpty()) {
                Address chosen = addresses.get(0);
                for (Address a : addresses) {
                    if (a.isDefault()) {
                        chosen = a;
                        break;
                    }
                }
                binding.tvDeliveryLocation.setText("Delivering to " + chosen.getCity());
            } else {
                binding.tvDeliveryLocation.setText("Add Delivery Address");
            }
        });

        // 5. Dynamically observe User's Active Orders from Database
        ordersLiveData = db.orderDao().getOrdersByCustomerId(currentUserId);
        ordersLiveData.observe(this, orders -> {
            if (orders == null || orders.isEmpty()) {
                binding.cvLiveJobSnippet.setVisibility(View.GONE);
                return;
            }

            // Pick the most recent non-cancelled order (or the latest order)
            Order activeOrder = null;
            for (Order o : orders) {
                if (!"CANCELLED".equalsIgnoreCase(o.getOrderStatus())) {
                    activeOrder = o;
                    break;
                }
            }
            if (activeOrder == null) {
                binding.cvLiveJobSnippet.setVisibility(View.GONE);
                return;
            }

            final Order orderToShow = activeOrder;
            binding.cvLiveJobSnippet.setVisibility(View.VISIBLE);
            binding.tvLiveOrderId.setText("Order #" + orderToShow.getOrderId());
            binding.tvLiveOrderStatus.setText("• " + orderToShow.getOrderStatus());

            View.OnClickListener trackListener = v -> {
                Intent intent = new Intent(HomeActivity.this, OrderTrackingActivity.class);
                intent.putExtra("ORDER_ID", orderToShow.getOrderId());
                intent.putExtra("ORDER_STATUS", orderToShow.getOrderStatus());
                startActivity(intent);
            };
            binding.btnTrackOrderSnippet.setOnClickListener(trackListener);
            binding.cvLiveJobSnippet.setOnClickListener(trackListener);

            // Load real order items & product names from Database
            executorService.execute(() -> {
                List<OrderItem> items = db.orderItemDao().getOrderItemsByOrderId(orderToShow.getOrderId());
                String summaryText;
                if (items != null && !items.isEmpty()) {
                    OrderItem firstItem = items.get(0);
                    Product prod = db.productDao().getProductById(firstItem.getProductId());
                    String prodName = prod != null ? prod.getName() : "Custom Print Item";
                    summaryText = firstItem.getQuantity() + "x " + prodName;
                    if (items.size() > 1) {
                        summaryText += " (+" + (items.size() - 1) + " more)";
                    }
                } else {
                    summaryText = orderToShow.getDeliveryMethod() + " • " +
                            com.printxpress.app.util.SessionManager.formatPrice(this, orderToShow.getTotalAmount());
                }
                String finalSummary = summaryText;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    binding.tvLiveOrderItem.setText(finalSummary);
                });
            });
        });
    }
}