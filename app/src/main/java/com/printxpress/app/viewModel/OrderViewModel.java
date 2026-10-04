package com.printxpress.app.viewModel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.printxpress.app.PrintXpressApp;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.entity.Order;
import com.printxpress.app.data.local.entity.OrderItem;
import com.printxpress.app.data.repository.OrderRepository;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class OrderViewModel extends AndroidViewModel {
    
    private final OrderRepository repository;
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Long> orderPlacementResult = new MutableLiveData<>();
    private final ExecutorService executorService = PrintXpressDatabase.databaseWriteExecutor;
    
    public OrderViewModel(@NonNull Application application) {
        super(application);
        repository = PrintXpressApp.getAppContainer(application).getOrderRepository();
    }
    
    public void placeOrder(int userId, String deliveryMethod, Integer deliveryAddressId,
                           String paymentMethod, String paymentStatus, double grandTotal) {
        placeOrder(userId, deliveryMethod, deliveryAddressId, paymentMethod, paymentStatus, grandTotal, -1);
    }

    public void placeOrder(int userId, String deliveryMethod, Integer deliveryAddressId,
                           String paymentMethod, String paymentStatus, double grandTotal, int singleCartItemId) {
        executorService.execute(() -> {
            try {
                long orderId = repository.placeOrderTransaction(
                        userId, deliveryMethod, deliveryAddressId, paymentMethod, paymentStatus, grandTotal, singleCartItemId);
                orderPlacementResult.postValue(orderId);
            } catch (Exception e) {
                message.postValue("Order placement failed: " + e.getMessage());
                orderPlacementResult.postValue(-1L);
            }
        });
    }

    public void createOrder(Order order) {
        executorService.execute(() -> {
            long orderId = repository.createOrder(order);
            if (orderId > 0) {
                message.postValue("Order placed successfully! Order ID: " + orderId);
            } else {
                message.postValue("Failed to place order");
            }
        });
    }
    
    public void createOrderItem(OrderItem orderItem) {
        executorService.execute(() -> {
            repository.createOrderItem(orderItem);
        });
    }
    
    public LiveData<List<Order>> getOrdersByCustomerId(int customerId) {
        return repository.getOrdersByCustomerId(customerId);
    }

    public LiveData<List<Order>> getAllOrders() {
        return repository.getAllOrders();
    }

    public LiveData<Double> getCustomerTotalSpent(int customerId) {
        return repository.getCustomerTotalSpent(customerId);
    }

    public LiveData<Integer> getCustomerOrderCount(int customerId) {
        return repository.getCustomerOrderCount(customerId);
    }

    public LiveData<Integer> getCustomerActiveOrderCount(int customerId) {
        return repository.getCustomerActiveOrderCount(customerId);
    }

    public LiveData<Double> getTotalPlatformRevenue() {
        return repository.getTotalPlatformRevenue();
    }

    public LiveData<Integer> getTotalOrderCount() {
        return repository.getTotalOrderCount();
    }

    public LiveData<Integer> getActivePressQueueCount() {
        return repository.getActivePressQueueCount();
    }

    public void updateOrderStatus(int orderId, String status) {
        executorService.execute(() -> {
            repository.updateOrderStatus(orderId, status);
            message.postValue("Order #" + orderId + " updated to " + status);
        });
    }
    
    public void cancelOrder(int orderId) {
        executorService.execute(() -> {
            repository.cancelOrder(orderId);
            message.postValue("Order cancelled and stock restored");
        });
    }
    
    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<Long> getOrderPlacementResult() {
        return orderPlacementResult;
    }
}