package com.printxpress.app.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.printxpress.app.databinding.ActivityCartBinding;
import com.printxpress.app.ui.adapter.CartAdapter;
import com.printxpress.app.util.PricingCalculator;
import com.printxpress.app.util.SessionManager;
import com.printxpress.app.viewModel.CartViewModel;
import java.util.ArrayList;
import java.util.Locale;

public class CartActivity extends AppCompatActivity {
    
    private ActivityCartBinding binding;
    private CartViewModel cartViewModel;
    private CartAdapter adapter;
    private int userId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SessionManager.requireLogin(this)) {
            finish();
            return;
        }

        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnBack.setOnClickListener(v -> finish());
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnBack);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnCheckout);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnExploreProducts);

        binding.btnExploreProducts.setOnClickListener(v -> {
            Intent intent = new Intent(CartActivity.this, com.printxpress.app.ui.product.ProductListActivity.class);
            startActivity(intent);
            finish();
        });

        userId = SessionManager.getUserId(this);
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
        
        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(new ArrayList<>(), item -> {
            cartViewModel.removeFromCart(item.cartItem);
            com.printxpress.app.util.PrintXpressToast.info(this, "Item removed from studio cart");
        });
        adapter.setOnQuantityChangeListener((item, newQty) -> {
            if (item != null && item.cartItem != null) {
                double basePrice = item.product != null ? item.product.getBasePrice() : item.cartItem.getUnitPrice();
                double updatedUnitPrice = PricingCalculator.calculateItemPrice(
                        basePrice,
                        item.cartItem.getSelectedSize(),
                        item.cartItem.getSelectedMaterial(),
                        newQty
                );
                item.cartItem.setQuantity(newQty);
                item.cartItem.setUnitPrice(updatedUnitPrice);
                item.cartItem.setSubtotal(updatedUnitPrice * newQty);
                cartViewModel.updateCartItem(item.cartItem);
            }
        });
        adapter.setOnCheckoutSingleItemListener(item -> {
            if (item != null && item.cartItem != null) {
                Intent intent = new Intent(CartActivity.this, DeliverySelectionActivity.class);
                intent.putExtra("SINGLE_CART_ITEM_ID", item.cartItem.getCartItemId());
                startActivity(intent);
            }
        });
        binding.rvCartItems.setAdapter(adapter);
        
        observeCartData();
        
        binding.btnSignInCart.setOnClickListener(v -> {
            Intent intent = new Intent(CartActivity.this, com.printxpress.app.ui.auth.LoginActivity.class);
            startActivity(intent);
        });
        
        binding.btnCheckout.setOnClickListener(v -> {
            if (!SessionManager.requireLogin(this)) {
                return;
            }
            if (adapter.getItemCount() == 0) {
                com.printxpress.app.util.PrintXpressToast.info(this, "Your cart is empty — explore catalog");
                return;
            }
            Intent intent = new Intent(CartActivity.this, DeliverySelectionActivity.class);
            intent.putExtra("SINGLE_CART_ITEM_ID", -1);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        int currentId = SessionManager.getUserId(this);
        if (currentId != userId) {
            userId = currentId;
            if (userId != SessionManager.NO_USER) {
                observeCartData();
            } else {
                showEmptyState(true);
            }
        }
    }

    private void observeCartData() {
        cartViewModel.getCartItems(userId).observe(this, items -> {
            binding.pbLoading.setVisibility(View.GONE);
            if (items != null && !items.isEmpty()) {
                adapter.setItems(items);
                showEmptyState(false);
            } else {
                adapter.setItems(new ArrayList<>());
                showEmptyState(true);
            }
        });
        
        cartViewModel.getCartTotal(userId).observe(this, total -> {
            if (total != null && total > 0) {
                binding.tvSubtotal.setText(SessionManager.formatPrice(this, total));
            } else {
                binding.tvSubtotal.setText(SessionManager.formatPrice(this, 0.0));
            }
        });
    }

    private void showEmptyState(boolean isEmpty) {
        binding.pbLoading.setVisibility(View.GONE);
        if (isEmpty) {
            binding.rvCartItems.setVisibility(View.GONE);
            binding.llEmptyCart.setVisibility(View.VISIBLE);
            binding.tvEmptyCart.setVisibility(View.VISIBLE);
            binding.btnCheckout.setEnabled(false);
            binding.tvSubtotal.setText(SessionManager.formatPrice(this, 0.0));
            binding.btnSignInCart.setVisibility(userId == SessionManager.NO_USER ? View.VISIBLE : View.GONE);
        } else {
            binding.rvCartItems.setVisibility(View.VISIBLE);
            binding.llEmptyCart.setVisibility(View.GONE);
            binding.tvEmptyCart.setVisibility(View.GONE);
            binding.btnCheckout.setEnabled(true);
            binding.btnSignInCart.setVisibility(View.GONE);
        }
    }
}
