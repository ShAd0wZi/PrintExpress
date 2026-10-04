package com.printxpress.app.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "user", indices = {@Index(value = "email", unique = true)})
public class User {
    
    @PrimaryKey(autoGenerate = true)
    private int userId;
    
    private String name;
    private String email;
    private String phone;
    private String passwordHash;
    private Date registrationDate;
    private String userType; // 'CUSTOMER' or 'ADMIN'
    
    public User() {}
    
    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public Date getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(Date registrationDate) { this.registrationDate = registrationDate; }
    
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
}