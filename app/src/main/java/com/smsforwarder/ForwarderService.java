package com.smsforwarder;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class ForwarderService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.ensureChannels(this);
        startForeground(
                NotificationHelper.SERVICE_NOTIFICATION_ID,
                NotificationHelper.buildServiceNotification(this)
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationHelper.ensureChannels(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NotificationHelper.SERVICE_NOTIFICATION_ID,
                    NotificationHelper.buildServiceNotification(this),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            );
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
