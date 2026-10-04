package com.printxpress.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.printxpress.app.data.local.entity.Notification;
import java.util.List;

@Dao
public interface NotificationDao {
    
    @Insert
    long insert(Notification notification);
    
    @Update
    void update(Notification notification);
    
    @Query("SELECT * FROM notification WHERE userId = :userId ORDER BY timestamp DESC")
    LiveData<List<Notification>> getNotificationsByUserId(int userId);
    
    @Query("SELECT * FROM notification WHERE notificationId = :notificationId")
    Notification getNotificationById(int notificationId);
    
    @Query("UPDATE notification SET isRead = 1 WHERE notificationId = :notificationId")
    void markAsRead(int notificationId);

    @Query("UPDATE notification SET isRead = 1 WHERE userId = :userId")
    void markAllAsRead(int userId);
    
    @Query("SELECT COUNT(*) FROM notification WHERE userId = :userId AND isRead = 0")
    int getUnreadCount(int userId);

    @Query("SELECT COUNT(*) FROM notification WHERE userId = :userId AND isRead = 0")
    LiveData<Integer> getUnreadCountLive(int userId);
}