package com.printxpress.app.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "address",
    foreignKeys = @ForeignKey(entity = User.class, 
        parentColumns = "userId", 
        childColumns = "userId", 
        onDelete = ForeignKey.CASCADE),
    indices = {@Index("userId")})
public class Address {
    
    @PrimaryKey(autoGenerate = true)
    private int addressId;
    
    private int userId;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String postalCode;
    private String phone;
    private boolean isDefault;
    
    public Address() {}
    
    // Getters and Setters
    public int getAddressId() { return addressId; }
    public void setAddressId(int addressId) { this.addressId = addressId; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
}