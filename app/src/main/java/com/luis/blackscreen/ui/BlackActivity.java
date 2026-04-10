package com.luis.blackscreen.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.luis.blackscreen.service.BlackScreenService;

public class BlackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pantalla completamente negra
        View black = new View(this);
        black.setBackgroundColor(0xFF000000);
        setContentView(black);

        // Brillo = 0 (OLED: píxeles apagados = batería mínima)
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 0f;
        getWindow().setAttributes(lp);

        // Pantalla completa sin barras
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );

        // Evita que la pantalla se apague (audio sigue reproduciéndose)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Toque en cualquier parte → muestra botón de salida
        black.setOnClickListener(v -> showExitButton());
    }

    private void showExitButton() {
        android.widget.Button btn = new android.widget.Button(this);
        btn.setText("Volver / Apagar");
        btn.setBackgroundColor(0xCC333333);
        btn.setTextColor(0xFFFFFFFF);
        btn.setPadding(40, 30, 40, 30);

        android.widget.FrameLayout fl = new android.widget.FrameLayout(this);
        fl.setBackgroundColor(0xFF000000);

        android.widget.FrameLayout.LayoutParams params =
                new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                );
        params.gravity = android.view.Gravity.CENTER;
        fl.addView(btn, params);
        setContentView(fl);

        btn.setOnClickListener(v -> {
            // Detiene el servicio y cierra
            Intent stop = new Intent(this, BlackScreenService.class);
            stop.setAction(BlackScreenService.ACTION_STOP);
            startService(stop);
            finish();
        });

        // Auto-ocultar después de 3 segundos
        fl.postDelayed(() -> {
            View black = new View(this);
            black.setBackgroundColor(0xFF000000);
            black.setOnClickListener(x -> showExitButton());
            setContentView(black);
        }, 3000);
    }

    @Override
    public void onBackPressed() {
        // Bloquear botón atrás para no salir accidentalmente
        showExitButton();
    }
}
