package com.example.racestats;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Settings extends AppCompatActivity {
    private CustomProgressBar coolantTemperatureGauge;
    private TextView coolantTemperatureTextOverlay;

    private TextView coolantTempTextSimple;
    private ImageView coolantLogoSimple;

    // Handles coolant flash event
    private Handler handler = new Handler();
    private boolean isFlashing = false;
    private int flashCount = 0;
    private boolean hasFlashed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gaugetest);

        coolantTemperatureTextOverlay = findViewById(R.id.coolantTemperatureTextOverlay); // Initialize the TextView

        // Set the coolant temperature (0 to 126, adjust as needed)
        int coolantTemperature = 88; // Change this temperature value

        // Set the coolant temperature in your CustomProgressBar
        coolantTemperatureGauge = findViewById(R.id.coolantTemperatureGauge);
        coolantTemperatureGauge.setCoolantTemperatureTextOverlay(coolantTemperatureTextOverlay);
        coolantTemperatureGauge.setCoolantTemperature(coolantTemperature);

        coolantTempTextSimple = findViewById(R.id.textTempSimple);
        coolantLogoSimple = findViewById(R.id.coolantLogoSimple);

        // Update the temperature text
        coolantTemperatureTextOverlay.setText(coolantTemperature + "°C");
        coolantTempTextSimple.setText(coolantTemperature + "°C");

        // Update the gauge color based on temperature
        updateGaugeColor(coolantTemperature);
    }

    // Method to update the gauge color based on temperature
    private void updateGaugeColor(int temperature) {
        if (temperature > 104) {
            coolantTemperatureGauge.setProgressDrawable(getResources().getDrawable(R.drawable.horizontal_gauge_high));

            // Start the flashing effect if it's not already running
            if (!isFlashing && !hasFlashed) {
                startFlashingEffect();
                hasFlashed = true;
            }

            coolantLogoSimple.setImageResource(R.drawable.coolant_logo_red);
            coolantTempTextSimple.setTextColor(Color.parseColor("#ff0000"));
        } else if (temperature > 96) {
            coolantTemperatureGauge.setProgressDrawable(getResources().getDrawable(R.drawable.horizontal_gauge_medium));

            // Stop the flashing effect if it's running
            stopFlashingEffect();

            coolantTempTextSimple.setTextColor(Color.parseColor("#ffe222"));
            coolantLogoSimple.setImageResource(R.drawable.coolant_logo_yellow);
            hasFlashed = false;
        } else {
            coolantTemperatureGauge.setProgressDrawable(getResources().getDrawable(R.drawable.horizontal_gauge));

            // Stop the flashing effect if it's running
            stopFlashingEffect();

            coolantTempTextSimple.setTextColor(Color.parseColor("#FFFFFF"));
            coolantLogoSimple.setImageResource(R.drawable.coolant_logo_white);
            hasFlashed = false;
        }

        coolantTemperatureGauge.setProgress(temperature);
    }

    private void startFlashingEffect() {
        isFlashing = true;
        flashCount = 0;
        handler.postDelayed(flashingRunnable, 500); // Start flashing every 500ms
    }

    private void stopFlashingEffect() {
        isFlashing = false;
        handler.removeCallbacks(flashingRunnable); // Stop the flashing effect
    }

    private Runnable flashingRunnable = new Runnable() {
        @Override
        public void run() {
            if (flashCount < 8) { // 8 times to flash (4 times on, 4 times off)
                if (coolantLogoSimple.getVisibility() == View.VISIBLE) {
                    coolantLogoSimple.setVisibility(View.INVISIBLE);
                } else {
                    coolantLogoSimple.setVisibility(View.VISIBLE);
                }
                flashCount++;
                handler.postDelayed(this, 500); // Repeat the flashing every 500ms
            } else {
                coolantLogoSimple.setVisibility(View.VISIBLE); // Ensure it's visible when done flashing
                isFlashing = false;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopFlashingEffect(); // Stop the flashing effect when the activity is destroyed
    }
}