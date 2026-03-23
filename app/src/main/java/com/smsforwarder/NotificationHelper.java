package com.smsforwarder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public final class NotificationHelper {
    public static final String SERVICE_CHANNEL_ID = "forwarder_service";
    public static final String EVENT_CHANNEL_ID = "forwarder_events";
    public static final int SERVICE_NOTIFICATION_ID = 1001;

    private NotificationHelper() {
    }

    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel serviceChannel = new NotificationChannel(
                SERVICE_CHANNEL_ID,
                context.getString(R.string.service_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        serviceChannel.setDescription(context.getString(R.string.service_channel_description));

        NotificationChannel eventChannel = new NotificationChannel(
                EVENT_CHANNEL_ID,
                context.getString(R.string.event_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        eventChannel.setDescription(context.getString(R.string.event_channel_description));

        manager.createNotificationChannel(serviceChannel);
        manager.createNotificationChannel(eventChannel);
    }

    public static Notification buildServiceNotification(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle(context.getString(R.string.notification_service_title))
                .setContentText(context.getString(R.string.notification_service_text))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    public static void showForwardedNotification(Context context, String title, String content) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        Notification notification = new NotificationCompat.Builder(context, EVENT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setAutoCancel(true)
                .build();

        manager.notify((int) System.currentTimeMillis(), notification);
    }
}
