package com.luis.blackscreen.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import com.luis.blackscreen.ui.BlackActivity;
import com.luis.blackscreen.ui.MainActivity;

public class BlackScreenService extends Service {

    private static final String CHANNEL_ID = "black_screen_channel";
    private static final int NOTIF_ID = 1;

    public static final String ACTION_STOP = "com.luis.blackscreen.STOP";
    public static boolean isRunning = false;

    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        isRunning = true;
        startForeground(NOTIF_ID, buildNotification());
        return START_STICKY;
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "BlackScreen::AudioWakeLock"
        );
        wakeLock.acquire();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Black Screen",
                NotificationManager.IMPORTANCE_LOW
        );
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        // Tap → vuelve a BlackActivity
        Intent blackIntent = new Intent(this, BlackActivity.class);
        blackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent blackPi = PendingIntent.getActivity(
                this, 0, blackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Acción detener
        Intent stopIntent = new Intent(this, BlackScreenService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(
                this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_power_off)
                .setContentTitle("Black Screen activo")
                .setContentText("Toca para volver a oscurecer")
                .setContentIntent(blackPi)
                .addAction(android.R.drawable.ic_delete, "Detener", stopPi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
