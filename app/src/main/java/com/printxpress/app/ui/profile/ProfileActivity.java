package com.printxpress.app.ui.profile;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import com.printxpress.app.R;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.entity.Address;
import com.printxpress.app.data.local.entity.Order;
import com.printxpress.app.data.local.entity.User;
import com.printxpress.app.databinding.ActivityProfileBinding;
import com.printxpress.app.ui.admin.AdminDashboardActivity;
import com.printxpress.app.ui.auth.LoginActivity;
import com.printxpress.app.util.Constants;
import com.printxpress.app.util.SessionManager;
import com.printxpress.app.viewModel.OrderViewModel;
import com.printxpress.app.viewModel.ProfileViewModel;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {
    
    private ActivityProfileBinding binding;
    private ProfileViewModel profileViewModel;
    private OrderViewModel orderViewModel;
    private double cachedTotalSpentLkr = 0.0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        
        loadUserProfile();
        setupThemeSelector();
        setupCurrencySelector();
        setupLanguageSelector();
        setupGdprControls();
        
        binding.btnBack.setOnClickListener(v -> finish());
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnBack);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnEditProfile);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnAddresses);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnMyOrders);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnGuidelines);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnPromotions);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnNotifications);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnFaq);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnAdminDashboard);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnLogout);

        binding.btnAdminDashboard.setOnClickListener(v -> {
            if (SessionManager.isAdmin(this)) {
                startActivity(new Intent(ProfileActivity.this, AdminDashboardActivity.class));
            } else {
                com.printxpress.app.util.PrintXpressToast.error(this, "Administrator privileges required.");
            }
        });
        
        binding.btnEditProfile.setOnClickListener(v -> {
            if (SessionManager.requireLogin(this)) {
                startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
            }
        });
        
        binding.btnAddresses.setOnClickListener(v -> {
            if (SessionManager.requireLogin(this)) {
                startActivity(new Intent(ProfileActivity.this, SavedAddressesActivity.class));
            }
        });

        binding.btnMyOrders.setOnClickListener(v -> {
            if (SessionManager.requireLogin(this)) {
                startActivity(new Intent(ProfileActivity.this, com.printxpress.app.ui.order.OrderHistoryActivity.class));
            }
        });

        binding.btnGuidelines.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, com.printxpress.app.ui.support.DesignGuidelinesActivity.class));
        });

        binding.btnPromotions.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, com.printxpress.app.ui.support.PromotionsActivity.class));
        });

        binding.btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, com.printxpress.app.ui.support.NotificationsActivity.class));
        });

        binding.btnFaq.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, com.printxpress.app.ui.support.FAQActivity.class));
        });
        
        binding.btnLogout.setOnClickListener(v -> {
            boolean wasLoggedIn = SessionManager.isLoggedIn(this);
            if (wasLoggedIn) {
                SessionManager.clearSession(this);
            }
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finishAffinity();
        });
    }

    private void setupThemeSelector() {
        updateThemeSelectorUI(SessionManager.getThemeMode(this));

        binding.btnThemeSystem.setOnClickListener(v -> {
            SessionManager.setThemeMode(this, Constants.THEME_SYSTEM);
            updateThemeSelectorUI(Constants.THEME_SYSTEM);
        });

        binding.btnThemeLight.setOnClickListener(v -> {
            SessionManager.setThemeMode(this, Constants.THEME_LIGHT);
            updateThemeSelectorUI(Constants.THEME_LIGHT);
        });

        binding.btnThemeDark.setOnClickListener(v -> {
            SessionManager.setThemeMode(this, Constants.THEME_DARK);
            updateThemeSelectorUI(Constants.THEME_DARK);
        });
    }

    private void setupCurrencySelector() {
        updateCurrencySelectorUI(SessionManager.getCurrency(this));

        View.OnClickListener listener = v -> {
            String currency = Constants.CURRENCY_LKR;
            if (v.getId() == R.id.btnCurrUsd) currency = Constants.CURRENCY_USD;
            else if (v.getId() == R.id.btnCurrEur) currency = Constants.CURRENCY_EUR;
            else if (v.getId() == R.id.btnCurrGbp) currency = Constants.CURRENCY_GBP;

            SessionManager.setCurrency(this, currency);
            updateCurrencySelectorUI(currency);
            binding.tvStatTotalSpent.setText(SessionManager.formatPrice(this, cachedTotalSpentLkr));
            com.printxpress.app.util.PrintXpressToast.success(this, "Display currency set to " + currency);
        };

        binding.btnCurrLkr.setOnClickListener(listener);
        binding.btnCurrUsd.setOnClickListener(listener);
        binding.btnCurrEur.setOnClickListener(listener);
        binding.btnCurrGbp.setOnClickListener(listener);
    }

    private void updateCurrencySelectorUI(String currencyCode) {
        String code = currencyCode != null ? currencyCode.toUpperCase() : Constants.CURRENCY_LKR;
        styleThemeOption(binding.btnCurrLkr, Constants.CURRENCY_LKR.equals(code));
        styleThemeOption(binding.btnCurrUsd, Constants.CURRENCY_USD.equals(code));
        styleThemeOption(binding.btnCurrEur, Constants.CURRENCY_EUR.equals(code));
        styleThemeOption(binding.btnCurrGbp, Constants.CURRENCY_GBP.equals(code));
    }

    private void setupLanguageSelector() {
        updateLanguageSelectorUI(SessionManager.getLanguage(this));

        View.OnClickListener langListener = v -> {
            String target = Constants.LANG_EN;
            if (v.getId() == R.id.btnLangSi) target = Constants.LANG_SI;
            else if (v.getId() == R.id.btnLangTa) target = Constants.LANG_TA;

            if (!target.equalsIgnoreCase(SessionManager.getLanguage(this))) {
                SessionManager.setLanguage(this, target);
                updateLanguageSelectorUI(target);
                recreate();
            }
        };

        binding.btnLangEn.setOnClickListener(langListener);
        binding.btnLangSi.setOnClickListener(langListener);
        binding.btnLangTa.setOnClickListener(langListener);
    }

    private void updateLanguageSelectorUI(String langCode) {
        String code = langCode != null ? langCode.toLowerCase() : Constants.LANG_EN;
        styleThemeOption(binding.btnLangEn, Constants.LANG_EN.equals(code));
        styleThemeOption(binding.btnLangSi, Constants.LANG_SI.equals(code));
        styleThemeOption(binding.btnLangTa, Constants.LANG_TA.equals(code));
    }

    private void setupGdprControls() {
        binding.btnExportData.setOnClickListener(v -> {
            if (!SessionManager.requireLogin(this)) return;
            exportUserDataJson();
        });

        binding.btnDeleteAccount.setOnClickListener(v -> {
            if (!SessionManager.requireLogin(this)) return;
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Delete Account (GDPR)")
                    .setMessage("Are you sure you want to permanently delete your account, saved addresses, and order history from the local database?")
                    .setPositiveButton("Delete Permanently", (d, which) -> deleteCurrentUserAccount())
                    .setNegativeButton("Cancel", null)
                    .create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                dialog.getWindow().setDimAmount(0.65f);
            }
            dialog.show();
        });
    }

    private void exportUserDataJson() {
        int userId = SessionManager.getUserId(this);
        PrintXpressDatabase db = PrintXpressDatabase.getDatabase(this);
        PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
            try {
                User user = db.userDao().getUserById(userId);
                List<Address> addresses = db.addressDao().getAddressesByUserIdSync(userId);
                List<Order> orders = db.orderDao().getOrdersByCustomerIdSync(userId);

                JSONObject root = new JSONObject();
                root.put("exportStandard", "GDPR Personal Data Archive");
                root.put("exportedAtUtc", com.printxpress.app.data.local.Converters.formatUtcIso(new java.util.Date()));

                if (user != null) {
                    JSONObject userObj = new JSONObject();
                    userObj.put("userId", user.getUserId());
                    userObj.put("name", user.getName());
                    userObj.put("email", user.getEmail());
                    userObj.put("phone", user.getPhone());
                    userObj.put("userType", user.getUserType());
                    root.put("profile", userObj);
                }

                JSONArray addrArr = new JSONArray();
                if (addresses != null) {
                    for (Address a : addresses) {
                        JSONObject ao = new JSONObject();
                        ao.put("addressLine1", a.getAddressLine1());
                        ao.put("addressLine2", a.getAddressLine2());
                        ao.put("city", a.getCity());
                        ao.put("postalCode", a.getPostalCode());
                        ao.put("phone", a.getPhone());
                        addrArr.put(ao);
                    }
                }
                root.put("savedAddresses", addrArr);

                JSONArray orderArr = new JSONArray();
                if (orders != null) {
                    for (Order o : orders) {
                        JSONObject oo = new JSONObject();
                        oo.put("orderId", o.getOrderId());
                        oo.put("orderStatus", o.getOrderStatus());
                        oo.put("totalAmountLkr", o.getTotalAmount());
                        oo.put("deliveryMethod", o.getDeliveryMethod());
                        oo.put("paymentMethod", o.getPaymentMethod());
                        orderArr.put(oo);
                    }
                }
                root.put("orders", orderArr);

                File docsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (docsDir != null && !docsDir.exists()) {
                    docsDir.mkdirs();
                }
                File outFile = new File(docsDir, "PrintXpress_GDPR_Export_User_" + userId + ".json");
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    com.printxpress.app.util.PrintXpressToast.success(this, "Exported to " + outFile.getName());
                    try {
                        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", outFile);
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("application/json");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(shareIntent, "Export GDPR Personal Data JSON"));
                    } catch (Exception ignored) {
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> com.printxpress.app.util.PrintXpressToast.error(this, "Export failed: " + e.getMessage()));
            }
        });
    }

    private void deleteCurrentUserAccount() {
        int userId = SessionManager.getUserId(this);
        PrintXpressDatabase db = PrintXpressDatabase.getDatabase(this);
        PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
            User user = db.userDao().getUserById(userId);
            if (user != null) {
                db.userDao().delete(user);
            }
            runOnUiThread(() -> {
                SessionManager.clearSession(this);
                com.printxpress.app.util.PrintXpressToast.info(this, "Account and associated data permanently deleted.");
                startActivity(new Intent(this, LoginActivity.class));
                finishAffinity();
            });
        });
    }

    private void updateThemeSelectorUI(int activeMode) {
        styleThemeOption(binding.btnThemeSystem, activeMode == Constants.THEME_SYSTEM);
        styleThemeOption(binding.btnThemeLight, activeMode == Constants.THEME_LIGHT);
        styleThemeOption(binding.btnThemeDark, activeMode == Constants.THEME_DARK);

        if (activeMode == Constants.THEME_LIGHT) {
            binding.tvThemeSubtitle.setText("Always use light appearance");
        } else if (activeMode == Constants.THEME_DARK) {
            binding.tvThemeSubtitle.setText("Always use dark appearance");
        } else {
            binding.tvThemeSubtitle.setText("Match device dark/light mode");
        }
    }

    private void styleThemeOption(TextView tab, boolean selected) {
        if (selected) {
            tab.setBackgroundResource(R.drawable.bg_pill_location);
            ViewCompat.setBackgroundTintList(
                    tab,
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_container))
            );
            tab.setTextColor(ContextCompat.getColor(this, R.color.on_primary));
        } else {
            tab.setBackground(null);
            ViewCompat.setBackgroundTintList(tab, null);
            tab.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
        updateThemeSelectorUI(SessionManager.getThemeMode(this));
        updateCurrencySelectorUI(SessionManager.getCurrency(this));
        updateLanguageSelectorUI(SessionManager.getLanguage(this));
    }

    private void loadUserProfile() {
        boolean isLoggedIn = SessionManager.isLoggedIn(this);
        boolean isAdmin = SessionManager.isAdmin(this);
        
        if (!isLoggedIn) {
            // Guest User State: remove unwanted analytics & GDPR controls
            binding.tvName.setText("Guest User");
            binding.tvEmail.setText("Browsing as Guest");
            binding.tvPhone.setVisibility(View.GONE);
            binding.btnEditProfile.setEnabled(false);
            binding.btnAddresses.setEnabled(false);
            binding.btnAdminDashboard.setVisibility(View.GONE);
            binding.btnLogout.setText("Sign In / Register");
            
            binding.tvProfileBadge.setText("Guest User");
            binding.tvProfileBadge.setBackgroundResource(R.drawable.bg_search_box);
            binding.tvProfileBadge.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
            binding.layoutCustomerAnalytics.setVisibility(View.GONE);
            binding.layoutGdprSection.setVisibility(View.GONE);
        } else {
            binding.tvPhone.setVisibility(View.VISIBLE);
            binding.btnEditProfile.setEnabled(true);
            binding.btnAddresses.setEnabled(true);
            binding.btnLogout.setText("Logout");
            
            String name = SessionManager.getUserName(this);
            binding.tvName.setText(name);
            binding.tvEmail.setText(SessionManager.getUserEmail(this));
            if (name != null && !name.isEmpty()) {
                binding.tvAvatarInitial.setText(name.substring(0, 1).toUpperCase());
            }

            if (isAdmin) {
                // Admin State: Dedicated system admin badge, portal access, hide consumer spend and self-deletion
                binding.tvProfileBadge.setText("System Administrator");
                binding.tvProfileBadge.setBackgroundResource(R.drawable.bg_badge_ready);
                binding.tvProfileBadge.setTextColor(ContextCompat.getColor(this, R.color.status_ready_text));
                binding.btnAdminDashboard.setVisibility(View.VISIBLE);
                binding.layoutCustomerAnalytics.setVisibility(View.GONE);
                binding.layoutGdprSection.setVisibility(View.VISIBLE);
                binding.btnDeleteAccount.setVisibility(View.GONE);
            } else {
                // Customer State: Commercial pro badge, full analytics and GDPR
                binding.tvProfileBadge.setText("Commercial Pro");
                binding.tvProfileBadge.setBackgroundResource(R.drawable.bg_pill_accent);
                binding.tvProfileBadge.setTextColor(ContextCompat.getColor(this, R.color.secondary));
                binding.btnAdminDashboard.setVisibility(View.GONE);
                binding.layoutCustomerAnalytics.setVisibility(View.VISIBLE);
                binding.layoutGdprSection.setVisibility(View.VISIBLE);
                binding.btnDeleteAccount.setVisibility(View.VISIBLE);
            }
            
            int userId = SessionManager.getUserId(this);
            if (userId != SessionManager.NO_USER) {
                profileViewModel.loadUser(userId);
                profileViewModel.getCurrentUser().observe(this, user -> {
                    if (user != null) {
                        binding.tvName.setText(user.getName());
                        binding.tvEmail.setText(user.getEmail());
                        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                            binding.tvPhone.setText(user.getPhone());
                        }
                    }
                });

                if (!isAdmin) {
                    // Observe live customer spend & order analytics from SQLite
                    orderViewModel.getCustomerTotalSpent(userId).observe(this, spent -> {
                        cachedTotalSpentLkr = spent != null ? spent : 0.0;
                        binding.tvStatTotalSpent.setText(SessionManager.formatPrice(this, cachedTotalSpentLkr));
                    });

                    orderViewModel.getCustomerOrderCount(userId).observe(this, count -> {
                        int c = count != null ? count : 0;
                        binding.tvStatOrderCount.setText(c + (c == 1 ? " Order" : " Orders"));
                    });

                    orderViewModel.getCustomerActiveOrderCount(userId).observe(this, active -> {
                        int a = active != null ? active : 0;
                        binding.tvStatActiveRuns.setText(a + " Active");
                    });
                }
            }
        }
    }
}