package com.printxpress.app.data.local.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "promotion", indices = {@Index(value = "code", unique = true)})
public class Promotion {

    @PrimaryKey(autoGenerate = true)
    private int promotionId;

    private String code;
    private String title;
    private String description;
    private double discountPercentage;
    private double minOrderAmount;
    private Date validUntil;
    private boolean isActive;

    public Promotion() {}

    @Ignore
    public Promotion(String code, String title, String description, double discountPercentage, double minOrderAmount, Date validUntil, boolean isActive) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.discountPercentage = discountPercentage;
        this.minOrderAmount = minOrderAmount;
        this.validUntil = validUntil;
        this.isActive = isActive;
    }

    // Getters and Setters
    public int getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(int promotionId) {
        this.promotionId = promotionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public double getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(double minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public Date getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Date validUntil) {
        this.validUntil = validUntil;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
