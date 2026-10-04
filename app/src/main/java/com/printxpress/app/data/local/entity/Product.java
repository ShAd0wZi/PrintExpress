package com.printxpress.app.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "product",
    foreignKeys = @ForeignKey(entity = Category.class,
        parentColumns = "categoryId",
        childColumns = "categoryId",
        onDelete = ForeignKey.CASCADE),
    indices = {@Index("categoryId")})
public class Product {
    
    @PrimaryKey(autoGenerate = true)
    private int productId;
    
    private int categoryId;
    private String name;
    private String description;
    private double basePrice;
    private String imageUrl;
    private String availableSizes;
    private String availableMaterials;
    private int stockQuantity = 100;
    
    public Product() {}
    
    // Getters and Setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getAvailableSizes() { return availableSizes; }
    public void setAvailableSizes(String availableSizes) { this.availableSizes = availableSizes; }
    
    public String getAvailableMaterials() { return availableMaterials; }
    public void setAvailableMaterials(String availableMaterials) { this.availableMaterials = availableMaterials; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = Math.max(0, stockQuantity); }
}