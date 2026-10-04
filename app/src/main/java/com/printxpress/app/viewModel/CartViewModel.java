package com.printxpress.app.viewModel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.printxpress.app.PrintXpressApp;
import com.printxpress.app.data.local.entity.CartItem;
import com.printxpress.app.data.local.model.CartItemWithProduct;
import com.printxpress.app.data.repository.CartRepository;
import java.util.List;

public class CartViewModel extends AndroidViewModel {
    
    private final CartRepository repository;
    
    public CartViewModel(@NonNull Application application) {
        super(application);
        repository = PrintXpressApp.getAppContainer(application).getCartRepository();
    }
    
    public LiveData<List<CartItemWithProduct>> getCartItems(int userId) {
        return repository.getCartItems(userId);
    }
    
    public LiveData<Double> getCartTotal(int userId) {
        return repository.getCartTotal(userId);
    }
    
    public void addToCart(CartItem item) {
        repository.addToCart(item);
    }
    
    public void updateCartItem(CartItem item) {
        repository.updateCartItem(item);
    }
    
    public void removeFromCart(CartItem item) {
        repository.removeFromCart(item);
    }
    
    public void clearCart(int userId) {
        repository.clearCart(userId);
    }
}
