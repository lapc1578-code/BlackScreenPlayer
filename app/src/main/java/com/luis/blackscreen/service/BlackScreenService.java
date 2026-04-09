package com.luis.blackscreen.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.core.app.NotificationCompat;

import com.luis.blackscreen.R;
import com.luis.blackscreen.ui.MainActivity;

public class BlackScreenService extends Service {

    private static final String CHANNEL_ID = "black_screen_channel";
    private static final int NOTIF_ID = 1;

    private WindowManager windowManager;
    private View blackOverlay;
    private View controlButton;
    private PowerManager.WakeLock wakeLock;

    // Acciones para controlar el servicio desde notificación
    public static final String ACTION_STOP = "com.luis.blackscreen.STOP";
    public static final String ACTION_TOGGLE = "com.luis.blackscreen.TOGGLE";

    private boolean isOverlayVisible = true;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        acquireWakeLock();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_TOGGLE.equals(intent.getAction())) {
            toggleOverlay();
            return START_STICKY;
        }

        startForeground(NOTIF_ID, buildNotification());
        showBlackOverlay();
        showControlButton();

        return START_STICKY;
    }

    // ── Overlay negro ─────────────────────────────────────────────────────────

    private void showBlackOverlay() {
        blackOverlay = new View(this);
        blackOverlay.setBackgroundColor(Color.BLACK);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // FLAG_NOT_FOCUSABLE: no interrumpe teclado/inputs de la app de fondo
                // FLAG_NOT_TOUCH_MODAL: los toques pasan a la app debajo si no son en el botón
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE
        );
        params.gravity = Gravity.TOP | Gravity.START;

        windowManager.addView(blackOverlay, params);
    }

    // ── Botón flotante para quitar la pantalla negra ──────────────────────────

    private void showControlButton() {
        controlButton = new LinearLayout(this);
        ImageButton btn = new ImageButton(this);
        btn.setImageResource(android.R.drawable.ic_media_play);
        btn.setBackgroundColor(Color.argb(200, 30, 30, 30));
        btn.setPadding(24, 24, 24, 24);
        ((LinearLayout) controlButton).addView(btn);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.x = 32;
        params.y = 120;

        // Permitir arrastrar el botón
        controlButton.setOnTouchListener(new DragTouchListener(params));

        btn.setOnClickListener(v -> toggleOverlay());

        windowManager.addView(controlButton, params);
    }

    private void toggleOverlay() {
        if (blackOverlay == null) return;
        isOverlayVisible = !isOverlayVisible;
        blackOverlay.setVisibility(isOverlayVisible ? View.VISIBLE : View.GONE);
        // Actualiza notificación
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIF_ID, buildNotification());
    }

    // ── WakeLock ──────────────────────────────────────────────────────────────

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "BlackScreen::AudioWakeLock"
        );
        wakeLock.acquire();
    }

    // ── Notificación foreground ───────────────────────────────────────────────

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Black Screen",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Mantiene pantalla negra mientras reproduce audio/video");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        // Acción: quitar/mostrar overlay
        Intent toggleIntent = new Intent(this, BlackScreenService.class);
        toggleIntent.setAction(ACTION_TOGGLE);
        PendingIntent togglePi = PendingIntent.getService(
                this, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Acción: detener servicio
        Intent stopIntent = new Intent(this, BlackScreenService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(
                this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Tap en notificación → abre MainActivity
        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent mainPi = PendingIntent.getActivity(
                this, 2, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String toggleLabel = isOverlayVisible ? "Mostrar pantalla" : "Oscurecer";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_power_off)
                .setContentTitle("Black Screen activo")
                .setContentText(isOverlayVisible ? "Pantalla oscura — audio reproduciéndose" : "Pantalla visible")
                .setContentIntent(mainPi)
                .addAction(android.R.drawable.ic_media_play, toggleLabel, togglePi)
                .addAction(android.R.drawable.ic_delete, "Detener", stopPi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (blackOverlay != null) {
            windowManager.removeView(blackOverlay);
            blackOverlay = null;
        }
        if (controlButton != null) {
            windowManager.removeView(controlButton);
            controlButton = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ── DragTouchListener: permite mover el botón flotante ────────────────────

    private class DragTouchListener implements View.OnTouchListener {
        private final WindowManager.LayoutParams params;
        private float initialX, initialY, initialTouchX, initialTouchY;

        DragTouchListener(WindowManager.LayoutParams params) {
            this.params = params;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = (int) (initialX - (event.getRawX() - initialTouchX));
                    params.y = (int) (initialY + (event.getRawY() - initialTouchY));
                    windowManager.updateViewLayout(controlButton, params);
                    return true;
            }
            return false;
        }
    }
}
