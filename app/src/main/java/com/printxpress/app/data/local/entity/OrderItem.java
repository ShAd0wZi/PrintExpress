package com.printxpress.app.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "order_item",
    foreignKeys = {
        @ForeignKey(entity = Order.class,
            parentColumns = "orderId",
            childColumns = "orderId",
            onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Product.class,
            parentColumns = "productId",
            childColumns = "productId",
            onDelete = ForeignKey.CASCADE)
    },
    indices = {@Index("orderId"), @Index("productId")})
public class OrderItem {
    
    @PrimaryKey(autoGenerate = true)
    private int orderItemId;
    
    private int orderId;
    private int productId;
    private int quantity;
    private double unitPrice;
    private String customisationDetails;
    private String artworkFilePath;
    private double subtotal;
    
    public OrderItem() {}
    
    // Getters and Setters
    public int getOrderItemId() { return orderItemId; }
    public void setOrderItemId(int orderItemId) { this.orderItemId = orderItemId; }
    
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    
    public String getCustomisationDetails() { return customisationDetails; }
    public void setCustomisationDetails(String customisationDetails) { this.customisationDetails = customisationDetails; }
    
    public String getArtworkFilePath() { return artworkFilePath; }
    public void setArtworkFilePath(String artworkFilePath) { this.artworkFilePath = artworkFilePath; }
    
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
}
