package com.printxpress.app.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    public static final String CHANNEL_ID_ORDERS = "orders_channel";
    public static final String CHANNEL_NAME_ORDERS = "Order Updates";
    public static final String CHANNEL_DESC_ORDERS = "Notifications for order status changes";

    public static final String CHANNEL_ID_PROMOS = "promos_channel";
    public static final String CHANNEL_NAME_PROMOS = "Promotions & Offers";
    public static final String CHANNEL_DESC_PROMOS = "Special discounts and promotional announcements";

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel orderChannel = new NotificationChannel(
                        CHANNEL_ID_ORDERS,
                        CHANNEL_NAME_ORDERS,
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                orderChannel.setDescription(CHANNEL_DESC_ORDERS);
                manager.createNotificationChannel(orderChannel);

                NotificationChannel promoChannel = new NotificationChannel(
                        CHANNEL_ID_PROMOS,
                        CHANNEL_NAME_PROMOS,
                        NotificationManager.IMPORTANCE_LOW
                );
                promoChannel.setDescription(CHANNEL_DESC_PROMOS);
                manager.createNotificationChannel(promoChannel);
            }
        }
    }

    public static void showNotification(Context context, int notificationId, String title, String message, Intent intent) {
        createNotificationChannels(context);

        PendingIntent pendingIntent = null;
        if (intent != null) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            pendingIntent = PendingIntent.getActivity(context, notificationId, intent, flags);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_ORDERS)
                .setSmallIcon(com.printxpress.app.R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            android.util.Log.w("NotificationHelper", "Notification permission not granted", e);
        }
    }
}
