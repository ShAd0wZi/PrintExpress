package com.printxpress.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.printxpress.app.data.local.entity.OrderItem;
import java.util.List;

@Dao
public interface OrderItemDao {
    
    @Insert
    long insert(OrderItem orderItem);
    
    @Query("SELECT * FROM order_item WHERE orderId = :orderId")
    List<OrderItem> getOrderItemsByOrderId(int orderId);
    
    @Query("DELETE FROM order_item WHERE orderId = :orderId")
    void deleteOrderItemsByOrderId(int orderId);
}