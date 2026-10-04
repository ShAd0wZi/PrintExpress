package com.printxpress.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.printxpress.app.data.local.entity.Category;
import java.util.List;

@Dao
public interface CategoryDao {
    
    @Insert
    long insert(Category category);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);
    
    @Query("SELECT * FROM category ORDER BY categoryId")
    LiveData<List<Category>> getAllCategories();
    
    @Query("SELECT * FROM category WHERE categoryId = :categoryId")
    Category getCategoryById(int categoryId);

    @Query("SELECT * FROM category WHERE name = :name LIMIT 1")
    Category getCategoryByName(String name);
    
    @Query("SELECT COUNT(*) FROM category")
    int getCategoryCount();
}