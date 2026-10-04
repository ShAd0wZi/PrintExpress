package com.printxpress.app.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "order_table",
    foreignKeys = {
        @ForeignKey(entity = User.class, 
            parentColumns = "userId", 
            childColumns = "customerId", 
            onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Address.class, 
            parentColumns = "addressId", 
            childColumns = "deliveryAddressId", 
            onDelete = ForeignKey.SET_NULL)
    },
    indices = {
        @Index("customerId"),
        @Index("deliveryAddressId"),
        @Index("orderDate"),
        @Index("orderStatus")
    })
public class Order {
    
    @PrimaryKey(autoGenerate = true)
    private int orderId;
    
    private int customerId;
    private Date orderDate;
    private double totalAmount;
    private String deliveryMethod; // 'DELIVERY' or 'PICKUP'
    private Integer deliveryAddressId; // Nullable
    private String paymentMethod; // 'CASH', 'CARD', 'ONLINE'
    private String paymentStatus; // 'PENDING', 'PAID', 'FAILED'
    private String orderStatus; // 'PENDING', 'PROCESSING', 'PRINTING', 'READY', 'COMPLETED', 'CANCELLED'
    private Date scheduledPickupDate;
    
    public Order() {}
    
    // Getters and Setters
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    
    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }
    
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    
    public String getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }
    
    public Integer getDeliveryAddressId() { return deliveryAddressId; }
    public void setDeliveryAddressId(Integer deliveryAddressId) { this.deliveryAddressId = deliveryAddressId; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    
    public Date getScheduledPickupDate() { return scheduledPickupDate; }
    public void setScheduledPickupDate(Date scheduledPickupDate) { this.scheduledPickupDate = scheduledPickupDate; }
}
