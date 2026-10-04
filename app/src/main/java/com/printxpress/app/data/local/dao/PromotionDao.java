package com.printxpress.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.printxpress.app.data.local.entity.Promotion;
import java.util.List;

@Dao
public interface PromotionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Promotion promotion);

    @Update
    void update(Promotion promotion);

    @Delete
    void delete(Promotion promotion);

    @Query("SELECT * FROM promotion WHERE isActive = 1 ORDER BY promotionId DESC")
    LiveData<List<Promotion>> getActivePromotions();

    @Query("SELECT * FROM promotion WHERE UPPER(code) = UPPER(:code) AND isActive = 1 LIMIT 1")
    Promotion getPromotionByCode(String code);

    @Query("SELECT * FROM promotion ORDER BY promotionId DESC")
    LiveData<List<Promotion>> getAllPromotions();
}
