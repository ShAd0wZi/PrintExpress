package com.printxpress.app.data.repository;

import androidx.lifecycle.LiveData;
import com.printxpress.app.data.local.dao.NotificationDao;
import com.printxpress.app.data.local.entity.Notification;
import java.util.List;

public class NotificationRepository {

    private final NotificationDao notificationDao;

    public NotificationRepository(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    public long insert(Notification notification) {
        return notificationDao.insert(notification);
    }

    public void update(Notification notification) {
        notificationDao.update(notification);
    }

    public LiveData<List<Notification>> getNotificationsByUserId(int userId) {
        return notificationDao.getNotificationsByUserId(userId);
    }

    public Notification getNotificationById(int notificationId) {
        return notificationDao.getNotificationById(notificationId);
    }

    public void markAsRead(int notificationId) {
        notificationDao.markAsRead(notificationId);
    }

    public int getUnreadCount(int userId) {
        return notificationDao.getUnreadCount(userId);
    }
}
