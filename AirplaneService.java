package com.example.airplanetoggle;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Toggles airplane mode every N seconds while running in the foreground. */
public class AirplaneService extends Service {

    static volatile boolean running = false;
    static final String ACTION_STOP = "com.example.airplanetoggle.STOP";
    static final String EXTRA_INTERVAL = "interval";
    private static final String CHANNEL_ID = "airplane_toggle";
    private static final int NOTIFICATION_ID = 1;

    private AirplaneController controller;
    private ScheduledExecutorService scheduler;
    private int toggles = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        controller = new AirplaneController(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Airplane Toggle", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        int seconds = intent != null ? intent.getIntExtra(EXTRA_INTERVAL, 20) : 20;
        if (seconds < 1) seconds = 1;

        Notification notification = buildNotification("يعمل كل " + seconds + " ثانية");
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        if (scheduler != null) scheduler.shutdownNow();
        toggles = 0;
        running = true;
        final int interval = seconds;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> toggle(interval), interval, interval, TimeUnit.SECONDS);
        return START_NOT_STICKY;
    }

    private void toggle(int interval) {
        try {
            boolean next = !controller.isOn();
            boolean ok = controller.set(next);
            toggles++;
            String state = next ? "وضع الطيارة: مفعّل" : "وضع الطيارة: متوقف";
            String text = ok
                    ? state + " | عدد النقرات: " + toggles
                    : "تعذّر التغيير، راجع الصلاحيات | عدد المحاولات: " + toggles;
            getSystemService(NotificationManager.class)
                    .notify(NOTIFICATION_ID, buildNotification(text));
        } catch (Throwable ignored) {
            // Keep the schedule alive even if one toggle fails.
        }
    }

    private Notification buildNotification(String text) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setContentTitle("Airplane Toggle")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        if (scheduler != null) scheduler.shutdownNow();
        running = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
