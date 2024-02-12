package com.example.racestats;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.content.SharedPreferences;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

// Import our Gauge view class
import com.example.racestats.DraggableGaugeView;

// Imports for OBD2 classes
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;

public class DigitalDash extends AppCompatActivity {
    private static BluetoothSocket socket;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // Declare member variables here
    private RelativeLayout intakeTempGauge;
    private RelativeLayout coolantTempGauge;

    private ImageButton hamburgerButton;
    private LinearLayout popoutMenu;
    private ImageButton xButton;
    private ImageView backbutton;
    private boolean isHomeIconRotated = false;

    private boolean isMenuOpen = false;

    private CustomProgressBar coolantTemperatureGauge;
    private TextView coolantTemperatureTextOverlay;
    private TextView textTempSimple;
    private TextView coolantTempTextSimple;
    private ImageView coolantLogoSimple;

    private Handler handler = new Handler();
    private boolean isFlashing = false;
    private int flashCount = 0;
    private boolean hasFlashed = false;

    private CustomProgressBar intakeTemperatureGauge;
    private TextView intakeTemperatureTextOverlay;

    private final Handler dataUpdateHandler = new Handler();
    private static final long DATA_UPDATE_INTERVAL = 5000; // Update interval in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the activity to fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.digital_dash);

        // Hide the navigation bar
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        // Initialize gauges and assets for UI
        intakeTempGauge = findViewById(R.id.IntakeTemp);
        coolantTempGauge = findViewById(R.id.coolantTemp);

        // Get the Bluetooth device address from the intent
        String deviceAddress = getIntent().getStringExtra("deviceAddress");

        hamburgerButton = findViewById(R.id.hamburgerButton);
        popoutMenu = findViewById(R.id.popoutMenu);
        xButton = findViewById(R.id.x_button);

        // Initialize coolant temperature gauge and related UI elements
        coolantTemperatureTextOverlay = findViewById(R.id.coolantTemperatureTextOverlay);
        coolantTemperatureGauge = findViewById(R.id.coolantTemperatureGauge);
        coolantTempTextSimple = findViewById(R.id.textTempSimple);
        coolantLogoSimple = findViewById(R.id.coolantLogoSimple);
        textTempSimple = findViewById(R.id.textTempSimple);

        intakeTemperatureGauge = findViewById(R.id.intakeTemperatureGauge);
        intakeTemperatureTextOverlay = findViewById(R.id.intakeTemperatureTextOverlay);

        hamburgerButton.setOnClickListener(view -> {
            if (!isMenuOpen) {
                openMenu();
                hamburgerButton.setVisibility(View.GONE);
                xButton.setVisibility(View.VISIBLE);
            }
        });

        xButton.setOnClickListener(view -> {
            if (isMenuOpen) {
                closeMenu();
                xButton.setVisibility(View.GONE);
                hamburgerButton.setVisibility(View.VISIBLE);
            }
        });


        if (deviceAddress == null) {
//            Show a popup indicating that no Bluetooth device is connected
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle("No Bluetooth Device Detected");
//            builder.setMessage("There is no Bluetooth device connected. Please connect a device and try again.");
//            builder.setPositiveButton("OK", (dialog, which) -> finish());
//            builder.show();

//          add dev test buttons here generate random values
//            rpmGauge.setText("Intake Temp: " + generateRandomNumber(4));
//            coolantTempGauge.setText("Coolant Temperature C°: " + 2);
            updateCoolantTemperature(75);
            updateAirIntakeTemperature(35);
            textTempSimple.setText("75");

        } else {
            BluetoothDevice selectedDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);

            // Check Bluetooth connect permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermissions();
                }
            }

            // Create Bluetooth socket
            socket = createBluetoothSocket(selectedDevice);

            try {
                // Connect socket and execute OBD2 commands in a separate thread
                socket.connect();

                // Run OBD2 commands in new Thread
                Executors.newSingleThreadExecutor().execute(this::obd2CommandsToCall);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }

        // Start the data update loop
        startDataUpdateLoop();

        backbutton = findViewById(R.id.backArrow);
        // Set a click listener for the home icon
        backbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Rotate and change the drawable based on the rotation state
                if (isHomeIconRotated) {
                    ObjectAnimator rotationAnim = ObjectAnimator.ofFloat(backbutton, "rotation", 55f, 0f);
                    ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(backbutton, "scaleX", 1.0f, 0.8f);
                    ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(backbutton, "scaleY", 1.0f, 0.8f);
                    AnimatorSet set = new AnimatorSet();
                    set.playTogether(rotationAnim, scaleXAnim, scaleYAnim);
                    set.setDuration(300);
                    set.start();

                    // Set the bars icon drawable to the ImageView
                    Drawable barsIconDrawable = VectorDrawableCompat.create(getResources(), R.drawable.home_icon, getTheme());
                    backbutton.setImageDrawable(barsIconDrawable);
                } else {
                    ObjectAnimator rotationAnim = ObjectAnimator.ofFloat(backbutton, "rotation", 0f, 335f);
                    ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(backbutton, "scaleX", 1.0f, 1.0f);
                    ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(backbutton, "scaleY", 1.0f, 1.0f);
                    AnimatorSet set = new AnimatorSet();
                    set.playTogether(rotationAnim, scaleXAnim, scaleYAnim);
                    set.setDuration(650);
                    set.start();

                    // Set the X icon drawable to the ImageView
                    Drawable xIconDrawable = VectorDrawableCompat.create(getResources(), R.drawable.ic_x_icon, getTheme());
                    backbutton.setImageDrawable(xIconDrawable);
                }

                // Toggle the rotation state
                isHomeIconRotated = !isHomeIconRotated;

                Intent intent = new Intent(DigitalDash.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void startDataUpdateLoop() {
        dataUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Call the function to update OBD2 data and UI
                obd2CommandsToCall();

                // Schedule the next data update
                dataUpdateHandler.postDelayed(this, DATA_UPDATE_INTERVAL);
            }
        }, DATA_UPDATE_INTERVAL);
    }

    public void onGaugeOptionClick(View view) {
        switch (view.getId()) {
            case R.id.gaugeOptionIntakeTemp:
                toggleGaugeVisibility(intakeTempGauge);
                break;
            case R.id.gaugeOptionCoolant:
                toggleGaugeVisibility(coolantTempGauge);
                break;
            // Add cases for more gauge options
        }
    }

    private void toggleGaugeVisibility(RelativeLayout gaugeView) {
        int newVisibility = gaugeView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
        gaugeView.setVisibility(newVisibility);
    }


    private void openMenu() {
        isMenuOpen = true;
        popoutMenu.setVisibility(View.VISIBLE);

        ObjectAnimator rotation = ObjectAnimator.ofFloat(hamburgerButton, "rotation", 0f, 45f);
        rotation.setDuration(300);
        rotation.start();
    }

    private void closeMenu() {
        isMenuOpen = false;
        popoutMenu.setVisibility(View.INVISIBLE);

        ObjectAnimator rotation = ObjectAnimator.ofFloat(hamburgerButton, "rotation", 45f, 0f);
        rotation.setDuration(300);
        rotation.start();
    }

    /**
     * Random number generator for testing purposes
     *
     * @param numberOfDigits
     * @return
     */
    public static int generateRandomNumber(int numberOfDigits) {
        if (numberOfDigits <= 0) {
            throw new IllegalArgumentException("Number of digits must be a positive integer.");
        }

        Random random = new Random();
        int minBound = (int) Math.pow(10, numberOfDigits - 1);
        int maxBound = (int) Math.pow(10, numberOfDigits) - 1;

        return random.nextInt(maxBound - minBound + 1) + minBound;
    }

    /**
     * Create a Bluetooth socket for the selected device
     */
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermissions();
                }
            }
            return device.createInsecureRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This function will check and see what PIDs are available for the selected ECU
     */
    private int[] avaliablePIDs() {
        return null;
    }

    private void updateAirIntakeTemperature(int temperature) {
        intakeTemperatureGauge.setProgress(temperature);
        intakeTemperatureTextOverlay.setText(String.valueOf(temperature) + " °C");
        intakeTemperatureGauge.setProgressDrawable(getResources().getDrawable(R.drawable.horizontal_gauge));
    }

    private void updateCoolantTemperature(int temperature) {
        // Implement the logic to update coolant temperature gauge and UI based on temperature
        // This will replace the updateGaugeColor method from Settings class
        // You can adapt the logic from updateGaugeColor to this method

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
        coolantTemperatureTextOverlay.setText(String.valueOf(temperature) + " °C");
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

    /**
     * Class that will make calls to the obd2 scanner based off of the selected gauges of the user
     */
    private void obd2CommandsToCall() {
        long startTime = System.currentTimeMillis();

        try {
            // Initialize OBD2 communication
            new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
            new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
            new TimeoutCommand(125).run(socket.getInputStream(), socket.getOutputStream());
            new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());

            // Batch multiple commands in a single request
            List<ObdCommand> commandsToRun = new ArrayList<>();
            commandsToRun.add(new EngineCoolantTemperatureCommand());
            commandsToRun.add(new AirIntakeTemperatureCommand());

            String coolantTempResult = null;
            String intakeTempResult = null;

            // Execute commands in a single request
            //  adding a counter so we know how what request we are on
            int counter = 0;

            for (ObdCommand command : commandsToRun) {
                command.run(socket.getInputStream(), socket.getOutputStream());
                String result = command.getFormattedResult();

                // Extract numeric part from the result (remove non-numeric characters)
                String numericPart = result.replaceAll("[^0-9]", "");

                // Check if the numeric part is a valid integer
                if (!numericPart.isEmpty()) {
                    try {
                        int temperature = Integer.parseInt(numericPart);
                        if (counter == 0) {
                            // set coolant temp test
                            coolantTempResult = numericPart;
                        } else if (counter == 1) {
                            intakeTempResult = numericPart;
                        }
                        counter++;
                    } catch (NumberFormatException e) {
                        // Handle the case where the numeric part is not a valid integer
                        Log.e("NumberFormatException", "Invalid temperature value: " + result);
                    }
                }

            }

            // This will actually set our values in the UI need to test first.
            // Get results
            // Update the coolant temperature gauge
            if (coolantTempResult != null) {
                int coolantTemp = Integer.parseInt(coolantTempResult);
                updateCoolantTemperature(coolantTemp);
            }


            if (intakeTempResult != null) {
                int intakeTemp = Integer.parseInt(intakeTempResult);
                updateAirIntakeTemperature(intakeTemp);
            }

//            updateCoolantTemperature(coolantTemp);
//            updateAirIntakeTemperature(intakeTemp);

//            Log.d("Coolant Temp", coolantTempResult);
//            Log.d("Intake Temp", intakeTempResult);


            // ... Add more OBD2 commands here ...

        } catch (Exception e) {
            e.printStackTrace();
        }
//        finally {
//            try {
//                socket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        long endTime = System.currentTimeMillis();
        double elapsedTimeSeconds = (endTime - startTime) / 1000.0;
        Log.d("Execution Time", "Total execution time: " + elapsedTimeSeconds + " seconds");
    }

//    private void obd2CommandsToCall() {
//        try {
//            // Initialize OBD2 communication
//            new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
//            new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
//            new TimeoutCommand(125).run(socket.getInputStream(), socket.getOutputStream());
//            new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
//
//            // Initialize the OBD2 commands
//            EngineCoolantTemperatureCommand coolantTempCommand = new EngineCoolantTemperatureCommand();
//            AirIntakeTemperatureCommand intakeTempCommand = new AirIntakeTemperatureCommand();
//
//            // Execute the OBD2 commands
//            coolantTempCommand.run(socket.getInputStream(), socket.getOutputStream());
//            intakeTempCommand.run(socket.getInputStream(), socket.getOutputStream());
//
//            // Get the results from the commands
//            String coolantTempResult = coolantTempCommand.getFormattedResult();
//            String intakeTempResult = intakeTempCommand.getFormattedResult();
//
//            if (coolantTempResult != null && intakeTempResult != null) {
//                int coolantTemp = Integer.parseInt(coolantTempResult);
//                int intakeTemp = Integer.parseInt(intakeTempResult);
//
//                // Update UI based on OBD2 data
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        updateCoolantTemperature(coolantTemp);
//                        updateAirIntakeTemperature(intakeTemp);
//
//                        // Update the values in the DraggableGaugeView instances
//                        rpmGauge.setText("Intake Temp: " + intakeTemp);
//                        coolantTempGauge.setText("Coolant Temperature C°: " + coolantTemp);
//                    }
//                });
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }


    private int getUpdatedCoolantTemperature() {
        // Simulated coolant temperature (replace this with your OBD2 logic)
        return generateRandomNumber(100);
    }

    private int getUpdatedIntakeTemperature() {
        // Simulated intake temperature (replace this with your OBD2 logic)
        return generateRandomNumber(100);
    }

    /**
     * Cleanup when the activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the data update handler callbacks when the activity is destroyed
        dataUpdateHandler.removeCallbacksAndMessages(null);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Request Bluetooth permissions for scanning
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestPermissionsBlueToothScan(String[] strings, int i) {
        if (getApplicationContext().checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            // Access granted
            Log.d("Permissions", "Permission Already granted");
        } else {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
        }
    }

    /**
     * Request Bluetooth permissions for connection
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestPermissions() {
        if (getApplicationContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            // Access granted
            Log.d("Permissions", "Permission Already granted");
        } else {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
    }
}
