package com.printxpress.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.printxpress.app.data.local.entity.Product;
import java.util.List;

@Dao
public interface ProductDao {
    
    @Insert
    long insert(Product product);

    @Update
    void update(Product product);

    @Delete
    void delete(Product product);
    
    @Query("SELECT * FROM product WHERE productId = :productId")
    Product getProductById(int productId);

    @Query("SELECT * FROM product WHERE name = :name LIMIT 1")
    Product getProductByName(String name);

    @Query("SELECT * FROM product WHERE productId = :productId")
    LiveData<Product> getProductLiveData(int productId);
    
    @Query("SELECT * FROM product WHERE categoryId = :categoryId ORDER BY productId")
    LiveData<List<Product>> getProductsByCategory(int categoryId);
    
    @Query("SELECT * FROM product ORDER BY name")
    LiveData<List<Product>> getAllProducts();
    
    @Query("SELECT * FROM product WHERE name LIKE '%' || :searchTerm || '%' OR description LIKE '%' || :searchTerm || '%' ORDER BY name")
    LiveData<List<Product>> searchProducts(String searchTerm);

    @Query("SELECT COUNT(*) FROM product")
    int getProductCount();

    @Query("UPDATE product SET stockQuantity = MAX(0, stockQuantity - :quantity) WHERE productId = :productId")
    void decrementStock(int productId, int quantity);

    @Query("UPDATE product SET stockQuantity = stockQuantity + :quantity WHERE productId = :productId")
    void incrementStock(int productId, int quantity);

    @Query("UPDATE product SET stockQuantity = :stockQuantity WHERE productId = :productId")
    void updateStock(int productId, int stockQuantity);

    @Query("SELECT * FROM product WHERE stockQuantity <= :threshold ORDER BY stockQuantity ASC")
    LiveData<List<Product>> getLowStockProducts(int threshold);
}