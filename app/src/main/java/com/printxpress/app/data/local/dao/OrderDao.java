package com.printxpress.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.printxpress.app.data.local.entity.Order;
import java.util.List;

@Dao
public interface OrderDao {
    
    @Insert
    long insert(Order order);
    
    @Update
    void update(Order order);
    
    @Query("SELECT * FROM order_table WHERE orderId = :orderId")
    Order getOrderById(int orderId);

    @Query("SELECT * FROM order_table WHERE orderId = :orderId")
    LiveData<Order> getOrderLiveData(int orderId);
    
    @Query("SELECT * FROM order_table WHERE customerId = :customerId ORDER BY orderDate DESC")
    LiveData<List<Order>> getOrdersByCustomerId(int customerId);

    @Query("SELECT * FROM order_table WHERE customerId = :customerId ORDER BY orderDate DESC")
    List<Order> getOrdersByCustomerIdSync(int customerId);
    
    @Query("SELECT * FROM order_table WHERE orderStatus = :status")
    List<Order> getOrdersByStatus(String status);
    
    @Query("UPDATE order_table SET orderStatus = :status WHERE orderId = :orderId")
    void updateOrderStatus(int orderId, String status);
    
    @Query("UPDATE order_table SET orderStatus = 'CANCELLED' WHERE orderId = :orderId")
    void cancelOrder(int orderId);
    
    @Query("SELECT * FROM order_table ORDER BY orderDate DESC")
    LiveData<List<Order>> getAllOrders();

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM order_table WHERE customerId = :customerId AND orderStatus != 'CANCELLED'")
    LiveData<Double> getCustomerTotalSpent(int customerId);

    @Query("SELECT COUNT(*) FROM order_table WHERE customerId = :customerId")
    LiveData<Integer> getCustomerOrderCount(int customerId);

    @Query("SELECT COUNT(*) FROM order_table WHERE customerId = :customerId AND orderStatus IN ('PENDING', 'PROCESSING', 'PRINTING', 'READY')")
    LiveData<Integer> getCustomerActiveOrderCount(int customerId);

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM order_table WHERE orderStatus != 'CANCELLED'")
    LiveData<Double> getTotalPlatformRevenue();

    @Query("SELECT COUNT(*) FROM order_table")
    LiveData<Integer> getTotalOrderCount();

    @Query("SELECT COUNT(*) FROM order_table WHERE orderStatus IN ('PENDING', 'PROCESSING', 'PRINTING', 'READY')")
    LiveData<Integer> getActivePressQueueCount();
}