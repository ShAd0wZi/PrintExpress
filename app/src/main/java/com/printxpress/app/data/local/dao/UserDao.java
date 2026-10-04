package com.printxpress.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.printxpress.app.data.local.entity.User;
import java.util.List;

@Dao
public interface UserDao {
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(User user);
    
    @Update
    void update(User user);
    
    @Delete
    void delete(User user);
    
    @Query("SELECT * FROM user WHERE userId = :userId")
    User getUserById(int userId);
    
    @Query("SELECT * FROM user WHERE userType = 'CUSTOMER'")
    List<User> getAllCustomers();
    
    @Query("SELECT EXISTS(SELECT 1 FROM user WHERE email = :email)")
    boolean isEmailRegistered(String email);
    
    @Query("SELECT * FROM user WHERE email = :email")
    User getUserByEmail(String email);
}