package com.printxpress.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.printxpress.app.data.local.entity.CartItem;
import com.printxpress.app.data.local.model.CartItemWithProduct;
import java.util.List;

@Dao
public interface CartItemDao {

    @Insert
    long insert(CartItem cartItem);

    @Update
    void update(CartItem cartItem);

    @Delete
    void delete(CartItem cartItem);

    @Transaction
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    LiveData<List<CartItemWithProduct>> getCartItemsWithProduct(int userId);

    @Transaction
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    List<CartItemWithProduct> getCartItemsWithProductSync(int userId);

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    void clearCartByUserId(int userId);

    @Query("SELECT COALESCE(SUM(CASE WHEN ci.unitPrice > 0 THEN (ci.quantity * ci.unitPrice) ELSE (ci.quantity * p.basePrice) END), 0.0) FROM cart_items ci INNER JOIN product p ON ci.productId = p.productId WHERE ci.userId = :userId")
    LiveData<Double> getCartTotal(int userId);
    
    @Query("SELECT COUNT(*) FROM cart_items WHERE userId = :userId")
    LiveData<Integer> getCartItemCount(int userId);

    @Transaction
    @Query("SELECT * FROM cart_items WHERE cartItemId = :cartItemId LIMIT 1")
    CartItemWithProduct getCartItemWithProductByIdSync(int cartItemId);

    @Query("DELETE FROM cart_items WHERE cartItemId = :cartItemId")
    void deleteByCartItemId(int cartItemId);
}
