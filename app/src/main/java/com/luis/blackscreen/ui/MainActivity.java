package com.luis.blackscreen.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.luis.blackscreen.R;
import com.luis.blackscreen.databinding.ActivityMainBinding;
import com.luis.blackscreen.service.BlackScreenService;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private boolean serviceRunning = false;

    private final ActivityResultLauncher<Intent> overlayPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (Settings.canDrawOverlays(this)) {
                    startBlackScreen();
                } else {
                    Toast.makeText(this,
                            "Permiso necesario para mostrar la pantalla negra",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnToggle.setOnClickListener(v -> {
            if (!serviceRunning) {
                checkAndStart();
            } else {
                stopBlackScreen();
            }
        });

        binding.btnSettings.setOnClickListener(v -> {
            // Abre configuración de overlay del sistema
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });

        updateUI();
    }

    private void checkAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this,
                    "Necesitas dar permiso para mostrar sobre otras apps",
                    Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayPermissionLauncher.launch(intent);
        } else {
            startBlackScreen();
        }
    }

    private void startBlackScreen() {
        Intent intent = new Intent(this, BlackScreenService.class);
        startForegroundService(intent);
        serviceRunning = true;
        updateUI();
        // Minimiza la app para que YouTube/etc quede al frente
        moveTaskToBack(true);
    }

    private void stopBlackScreen() {
        Intent stopIntent = new Intent(this, BlackScreenService.class);
        stopIntent.setAction(BlackScreenService.ACTION_STOP);
        startService(stopIntent);
        serviceRunning = false;
        updateUI();
    }

    private void updateUI() {
        if (serviceRunning) {
            binding.btnToggle.setText("Quitar pantalla negra");
            binding.statusText.setText("🟢 Pantalla negra activa");
        } else {
            binding.btnToggle.setText("Activar pantalla negra");
            binding.statusText.setText("⚫ Inactivo — abre YouTube y luego pulsa aquí");
        }
    }
}
