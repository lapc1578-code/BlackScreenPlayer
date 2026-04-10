package com.luis.blackscreen.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.luis.blackscreen.databinding.ActivityMainBinding;
import com.luis.blackscreen.service.BlackScreenService;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnToggle.setOnClickListener(v -> {
            // Inicia el servicio WakeLock y abre la pantalla negra
            startForegroundService(new Intent(this, BlackScreenService.class));
            startActivity(new Intent(this, BlackActivity.class));
        });

        binding.btnSettings.setVisibility(android.view.View.GONE); // ya no se necesita
        binding.statusText.setText("⚫ Abre YouTube, reproduce, y pulsa Activar");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Si volvemos aquí desde BlackActivity el servicio ya está corriendo
        if (BlackScreenService.isRunning) {
            binding.btnToggle.setText("Ya activo — toca para volver a oscurecer");
        } else {
            binding.btnToggle.setText("Activar pantalla negra");
        }
    }
}
