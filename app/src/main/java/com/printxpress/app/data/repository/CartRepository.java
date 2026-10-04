package com.printxpress.app.data.repository;

import androidx.lifecycle.LiveData;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.dao.CartItemDao;
import com.printxpress.app.data.local.entity.CartItem;
import com.printxpress.app.data.local.model.CartItemWithProduct;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class CartRepository {

    private final CartItemDao cartItemDao;
    private final ExecutorService executorService = PrintXpressDatabase.databaseWriteExecutor;

    public CartRepository(CartItemDao cartItemDao) {
        this.cartItemDao = cartItemDao;
    }

    public LiveData<List<CartItemWithProduct>> getCartItems(int userId) {
        return cartItemDao.getCartItemsWithProduct(userId);
    }

    public LiveData<Double> getCartTotal(int userId) {
        return cartItemDao.getCartTotal(userId);
    }

    public LiveData<Integer> getCartItemCount(int userId) {
        return cartItemDao.getCartItemCount(userId);
    }

    public void addToCart(CartItem item) {
        if (item == null) return;
        executorService.execute(() -> cartItemDao.insert(item));
    }

    public void updateCartItem(CartItem item) {
        if (item == null) return;
        executorService.execute(() -> cartItemDao.update(item));
    }

    public void removeFromCart(CartItem item) {
        if (item == null) return;
        executorService.execute(() -> cartItemDao.delete(item));
    }

    public void clearCart(int userId) {
        executorService.execute(() -> cartItemDao.clearCartByUserId(userId));
    }
}
