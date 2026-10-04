package com.printxpress.app.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "cart_items",
    foreignKeys = {
        @ForeignKey(entity = User.class,
            parentColumns = "userId",
            childColumns = "userId",
            onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Product.class,
            parentColumns = "productId",
            childColumns = "productId",
            onDelete = ForeignKey.CASCADE)
    },
    indices = {@Index("userId"), @Index("productId")})
public class CartItem {
    @PrimaryKey(autoGenerate = true)
    private int cartItemId;
    
    private int userId;
    private int productId;
    
    private int quantity;
    private String selectedSize;
    private String selectedMaterial;
    private String artworkFilePath;
    private String customText;
    
    private double unitPrice;
    private double subtotal;

    // Default constructor
    public CartItem() {}

    // Getters and Setters
    public int getCartItemId() { return cartItemId; }
    public void setCartItemId(int cartItemId) { this.cartItemId = cartItemId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getSelectedSize() { return selectedSize; }
    public void setSelectedSize(String selectedSize) { this.selectedSize = selectedSize; }

    public String getSelectedMaterial() { return selectedMaterial; }
    public void setSelectedMaterial(String selectedMaterial) { this.selectedMaterial = selectedMaterial; }

    public String getArtworkFilePath() { return artworkFilePath; }
    public void setArtworkFilePath(String artworkFilePath) { this.artworkFilePath = artworkFilePath; }

    public String getCustomText() { return customText; }
    public void setCustomText(String customText) { this.customText = customText; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
}
