package com.printxpress.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.printxpress.app.data.local.entity.Address;
import java.util.List;

@Dao
public interface AddressDao {
    
    @Insert
    long insert(Address address);
    
    @Update
    void update(Address address);
    
    @Delete
    void delete(Address address);

    @Query("UPDATE address SET isDefault = 0 WHERE userId = :userId")
    void clearDefaultAddresses(int userId);

    @Transaction
    default long insertWithDefaultEnforcement(Address address) {
        if (address.isDefault()) {
            clearDefaultAddresses(address.getUserId());
        }
        return insert(address);
    }
    
    @Query("SELECT * FROM address WHERE userId = :userId ORDER BY isDefault DESC, addressId DESC")
    LiveData<List<Address>> getAddressesByUserId(int userId);

    @Query("SELECT * FROM address WHERE userId = :userId ORDER BY isDefault DESC, addressId DESC")
    List<Address> getAddressesByUserIdSync(int userId);
    
    @Query("SELECT * FROM address WHERE addressId = :addressId")
    Address getAddressById(int addressId);
    
    @Query("SELECT * FROM address WHERE userId = :userId AND isDefault = 1 LIMIT 1")
    Address getDefaultAddress(int userId);
}