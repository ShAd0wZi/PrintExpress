package com.printxpress.app.ui.support;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.entity.Notification;
import com.printxpress.app.databinding.ActivityNotificationsBinding;
import com.printxpress.app.ui.adapter.NotificationAdapter;
import com.printxpress.app.util.SessionManager;
import java.util.ArrayList;
import java.util.Date;

public class NotificationsActivity extends AppCompatActivity {
    
    private ActivityNotificationsBinding binding;
    private NotificationAdapter adapter;
    private int userId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.requireLogin(this)) {
            finish();
            return;
        }
        userId = SessionManager.getUserId(this);

        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new NotificationAdapter(new ArrayList<>());
        binding.rvNotifications.setAdapter(adapter);
        
        PrintXpressDatabase db = PrintXpressDatabase.getDatabase(this);
        db.notificationDao().getNotificationsByUserId(userId).observe(this, notifications -> {
            binding.pbLoading.setVisibility(android.view.View.GONE);
            if (notifications != null && !notifications.isEmpty()) {
                binding.llEmptyNotifications.setVisibility(android.view.View.GONE);
                binding.rvNotifications.setVisibility(android.view.View.VISIBLE);
                adapter.setNotifications(notifications);
                PrintXpressDatabase.databaseWriteExecutor.execute(() -> db.notificationDao().markAllAsRead(userId));
            } else {
                binding.rvNotifications.setVisibility(android.view.View.GONE);
                binding.llEmptyNotifications.setVisibility(android.view.View.VISIBLE);
                adapter.setNotifications(new ArrayList<>());
            }
        });
    }
}