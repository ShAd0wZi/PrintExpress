package com.printxpress.app.data.repository;

import androidx.lifecycle.LiveData;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.dao.OrderDao;
import com.printxpress.app.data.local.dao.OrderItemDao;
import com.printxpress.app.data.local.entity.Order;
import com.printxpress.app.data.local.entity.OrderItem;
import com.printxpress.app.data.local.model.CartItemWithProduct;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderRepository {
    
    private final PrintXpressDatabase db;
    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    
    public OrderRepository(PrintXpressDatabase db) {
        this.db = db;
        this.orderDao = db.orderDao();
        this.orderItemDao = db.orderItemDao();
    }

    public OrderRepository(OrderDao orderDao, OrderItemDao orderItemDao) {
        this.db = null;
        this.orderDao = orderDao;
        this.orderItemDao = orderItemDao;
    }
    
    public long createOrder(Order order) {
        return orderDao.insert(order);
    }
    
    public long createOrderItem(OrderItem orderItem) {
        return orderItemDao.insert(orderItem);
    }
    
    public void updateOrder(Order order) {
        orderDao.update(order);
    }
    
    public Order getOrderById(int orderId) {
        return orderDao.getOrderById(orderId);
    }
    
    public LiveData<List<Order>> getOrdersByCustomerId(int customerId) {
        return orderDao.getOrdersByCustomerId(customerId);
    }

    public LiveData<List<Order>> getAllOrders() {
        return orderDao.getAllOrders();
    }

    public LiveData<Double> getCustomerTotalSpent(int customerId) {
        return orderDao.getCustomerTotalSpent(customerId);
    }

    public LiveData<Integer> getCustomerOrderCount(int customerId) {
        return orderDao.getCustomerOrderCount(customerId);
    }

    public LiveData<Integer> getCustomerActiveOrderCount(int customerId) {
        return orderDao.getCustomerActiveOrderCount(customerId);
    }

    public LiveData<Double> getTotalPlatformRevenue() {
        return orderDao.getTotalPlatformRevenue();
    }

    public LiveData<Integer> getTotalOrderCount() {
        return orderDao.getTotalOrderCount();
    }

    public LiveData<Integer> getActivePressQueueCount() {
        return orderDao.getActivePressQueueCount();
    }
    
    public List<OrderItem> getOrderItems(int orderId) {
        return orderItemDao.getOrderItemsByOrderId(orderId);
    }
    
    public void cancelOrder(int orderId) {
        if (this.db != null) {
            this.db.runInTransaction(() -> {
                Order existing = orderDao.getOrderById(orderId);
                if (existing != null && !"CANCELLED".equalsIgnoreCase(existing.getOrderStatus())) {
                    List<OrderItem> items = orderItemDao.getOrderItemsByOrderId(orderId);
                    if (items != null) {
                        for (OrderItem item : items) {
                            this.db.productDao().incrementStock(item.getProductId(), item.getQuantity());
                        }
                    }
                }
                orderDao.cancelOrder(orderId);
            });
        } else {
            orderDao.cancelOrder(orderId);
        }
    }
    
    public void updateOrderStatus(int orderId, String status) {
        if ("CANCELLED".equalsIgnoreCase(status)) {
            cancelOrder(orderId);
        } else {
            orderDao.updateOrderStatus(orderId, status);
        }
    }

    public long placeOrderTransaction(int userId, String deliveryMethod,
                                      Integer deliveryAddressId, String paymentMethod,
                                      String paymentStatus, double totalAmount) {
        return placeOrderTransaction(userId, deliveryMethod, deliveryAddressId, paymentMethod, paymentStatus, totalAmount, -1);
    }

    public long placeOrderTransaction(int userId, String deliveryMethod,
                                      Integer deliveryAddressId, String paymentMethod,
                                      String paymentStatus, double totalAmount,
                                      int singleCartItemId) {
        if (this.db == null) {
            throw new IllegalStateException("Database instance required for placeOrderTransaction");
        }
        return placeOrderTransaction(this.db, userId, deliveryMethod, deliveryAddressId, paymentMethod, paymentStatus, totalAmount, singleCartItemId);
    }

    public long placeOrderTransaction(PrintXpressDatabase targetDb, int userId, String deliveryMethod,
                                      Integer deliveryAddressId, String paymentMethod,
                                      String paymentStatus, double totalAmount) {
        return placeOrderTransaction(targetDb, userId, deliveryMethod, deliveryAddressId, paymentMethod, paymentStatus, totalAmount, -1);
    }

    /**
     * Executes the entire order placement workflow inside an atomic Room database transaction.
     * Supports checking out a single selected cart item or all items in the user's cart.
     */
    public long placeOrderTransaction(PrintXpressDatabase targetDb, int userId, String deliveryMethod,
                                      Integer deliveryAddressId, String paymentMethod,
                                      String paymentStatus, double totalAmount,
                                      int singleCartItemId) {
        return targetDb.runInTransaction(() -> {
            List<CartItemWithProduct> cartItems;
            if (singleCartItemId > 0) {
                CartItemWithProduct single = targetDb.cartItemDao().getCartItemWithProductByIdSync(singleCartItemId);
                if (single == null) {
                    throw new IllegalStateException("Selected cart item not found");
                }
                cartItems = java.util.Collections.singletonList(single);
            } else {
                cartItems = targetDb.cartItemDao().getCartItemsWithProductSync(userId);
            }

            if (cartItems == null || cartItems.isEmpty()) {
                throw new IllegalStateException("Cart is empty");
            }

            Order order = new Order();
            order.setCustomerId(userId);
            order.setOrderDate(new Date());
            order.setTotalAmount(totalAmount);
            order.setDeliveryMethod(deliveryMethod != null ? deliveryMethod : "Standard");
            if (deliveryAddressId != null && deliveryAddressId > 0) {
                order.setDeliveryAddressId(deliveryAddressId);
            }
            order.setPaymentMethod(paymentMethod != null ? paymentMethod : "Cash on Delivery");
            order.setPaymentStatus(paymentStatus != null ? paymentStatus : "PENDING");
            order.setOrderStatus("PROCESSING");

            long orderId = orderDao.insert(order);
            if (orderId <= 0) {
                throw new RuntimeException("Failed to insert order");
            }

            for (CartItemWithProduct cp : cartItems) {
                OrderItem item = new OrderItem();
                item.setOrderId((int) orderId);
                item.setProductId(cp.cartItem.getProductId());
                item.setQuantity(cp.cartItem.getQuantity());
                item.setUnitPrice(cp.cartItem.getUnitPrice());
                item.setSubtotal(cp.cartItem.getSubtotal());

                String safeSize = cp.cartItem.getSelectedSize() != null && !cp.cartItem.getSelectedSize().trim().isEmpty()
                        ? cp.cartItem.getSelectedSize().trim() : "Standard";
                String safeMaterial = cp.cartItem.getSelectedMaterial() != null && !cp.cartItem.getSelectedMaterial().trim().isEmpty()
                        ? cp.cartItem.getSelectedMaterial().trim() : "Standard";
                String safeNotes = cp.cartItem.getCustomText() != null && !cp.cartItem.getCustomText().trim().isEmpty()
                        ? cp.cartItem.getCustomText().trim() : "None";

                item.setCustomisationDetails(String.format(Locale.US, "Size: %s, Material: %s | Notes: %s",
                        safeSize, safeMaterial, safeNotes));
                item.setArtworkFilePath(cp.cartItem.getArtworkFilePath());
                
                long itemResult = orderItemDao.insert(item);
                if (itemResult <= 0) {
                    throw new RuntimeException("Failed to insert order item for product " + cp.cartItem.getProductId());
                }

                // Atomically decrement product stock
                targetDb.productDao().decrementStock(cp.cartItem.getProductId(), cp.cartItem.getQuantity());
            }

            // Remove only checked out item(s) from Cart atomically
            if (singleCartItemId > 0) {
                targetDb.cartItemDao().deleteByCartItemId(singleCartItemId);
            } else {
                targetDb.cartItemDao().clearCartByUserId(userId);
            }

            return orderId;
        });
    }
}