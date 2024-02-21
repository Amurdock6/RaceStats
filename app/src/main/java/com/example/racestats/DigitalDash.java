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
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

// multithreading imports
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;


// Imports for OBD2 classes
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.fuel.FuelTrimCommand;
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

public class DigitalDash extends AppCompatActivity {
    private static BluetoothSocket socket;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // Declare member variables here
    private RelativeLayout intakeTempGauge;
    private RelativeLayout coolantTempGauge;
    private RelativeLayout fuelTrim;

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
    private CustomProgressBar fuelTrimGauge;
    private TextView intakeTemperatureTextOverlay;
    private TextView fuelTrimTextOverlay;

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
        fuelTrim = findViewById(R.id.fuelTrim);

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

        fuelTrimGauge = findViewById(R.id.fuelTrimGauge);
        fuelTrimTextOverlay = findViewById(R.id.fuelTrimTextOverlay);

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
            updateFuelTrim(35);
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
            case R.id.gaugeOptionFuelTrim:
                toggleGaugeVisibility(fuelTrim);
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
     *
     * @return
     */
//    private int[] avaliablePIDs() {
//
//        return null;
//    }
//
//    /**
//     * This class is used to retrive the value that is at the given pid
//     *
//     * @param pidToRetrive
//     * @return
//     */
//    private int getPID(int pidToRetrive) {     // should make this generic
//
//        // look into these
////        PID           Bytes A   B C D Name Description
////        1101 4353 1       61          Engine Coolant Temperature °C = A-50, °F = (A - 50)*9/5+32, Same as Mode 01:05, but +10
////        1102 4354 1       00          Vehicle Speed kph = A*2, mph = (A*2)/1.60934
////        1103 4355 1       96          Battery Voltage V = A/12.5 or V=A*.08
////        111F 4383 1       5F          Oil Temperature °C = A-50, °F = (A - 50)*9/5+32, Same as 580:E
//        return -1;
//    }

    /**
     * This function will check and see what PIDs are available for the selected ECU
     *
     * @return an array of available PIDs
     */
    private int[] availablePIDs() {
        try {
            // Initialize OBD2 communication
            new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
            new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
            new TimeoutCommand(125).run(socket.getInputStream(), socket.getOutputStream());
            new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());

            // Create an array to store available PIDs
            int[] availablePIDs = new int[251];

            // Iterate through PIDs 0-250 and check availability
            for (int i = 0; i <= 250; i++) {
                ObdCommand pidCommand = new CustomPIDCommand(i, "PID name");  // Replace CustomPIDCommand with the appropriate command
                pidCommand.run(socket.getInputStream(), socket.getOutputStream());
                if (!pidCommand.getResult().equals("NO DATA")) {
                    availablePIDs[i] = i;
                }
            }

            return availablePIDs;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This class is used to retrieve the value that is at the given PID
     *
     * @param pidToRetrieve the PID to retrieve
     * @return the value of the specified PID
     */
    private int getPID(int pidToRetrieve) {
        try {
            // Initialize OBD2 communication
            new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
            new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
            new TimeoutCommand(125).run(socket.getInputStream(), socket.getOutputStream());
            new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());

            // Create a command for the specified PID
            CustomPIDCommand customCommand = new CustomPIDCommand(pidToRetrieve, "Custom Command Name");
            ObdCommand pidCommand = new CustomPIDCommand(pidToRetrieve, "Custom Command Name");  // Replace CustomPIDCommand with the appropriate command
            pidCommand.run(socket.getInputStream(), socket.getOutputStream());

            // Parse the result and return the value
            String result = pidCommand.getFormattedResult();
            return Integer.parseInt(result);

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates intake temp
     *
     * @param temperature
     */
    private void updateAirIntakeTemperature(int temperature) {
        intakeTemperatureGauge.setProgress(temperature);
        intakeTemperatureTextOverlay.setText(String.valueOf(temperature) + " °C");
        intakeTemperatureGauge.setProgressDrawable(getResources().getDrawable(R.drawable.horizontal_gauge));
    }

    /**
     * Handles coolant logo and flashing effects when coolant temp hits certian target temps
     *
     * @param temperature
     */
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

//    private void updateAfr(int temperature) {
//        afrGauge.setProgress(temperature);
//        afrTextOverlay.setText(String.valueOf(temperature));
//        afrGauge.setProgressDrawable(getResources().getDrawable(R.drawable.horizontal_gauge));
//    }

    /**
     * Updates fuel trim value
     *
     * @param temperature
     */
    private void updateFuelTrim(int temperature) {
        fuelTrimGauge.setProgress(temperature);
        fuelTrimTextOverlay.setText(String.valueOf(temperature));
        fuelTrimGauge.setProgressDrawable(getResources().getDrawable(R.drawable.horizontal_gauge));
    }


    /**
     * This method starts the flashing effect on coolant temp
     */
    private void startFlashingEffect() {
        isFlashing = true;
        flashCount = 0;
        handler.postDelayed(flashingRunnable, 500); // Start flashing every 500ms
    }

    /**
     * This method stops the flashing effect on the coolant temp
     */
    private void stopFlashingEffect() {
        isFlashing = false;
        handler.removeCallbacks(flashingRunnable); // Stop the flashing effect
    }

    /**
     * This method is used to create the actual flashing of the coolant variable
     */
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
     * Class that will make calls to the obd2 scanner based off of the selected gauges in the UI
     * Will use multi threading on each command to improve runtimes
     */
    private void obd2CommandsToCall() {
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
            commandsToRun.add(new FuelTrimCommand());

            ExecutorService executorService = Executors.newFixedThreadPool(commandsToRun.size());
            List<Future<String>> futures = new ArrayList<>();

            for (ObdCommand command : commandsToRun) {
                Future<String> future = executorService.submit(() -> {
                    try {
                        command.run(socket.getInputStream(), socket.getOutputStream());
                        return command.getFormattedResult();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });

                futures.add(future);
            }

            // Wait for all threads to finish and retrieve the results
            List<String> results = new ArrayList<>();

            for (Future<String> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            executorService.shutdown();

            // Process the results
            processResults(results);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method process the results from the obd2CommandsToCall method and interprets them to update values of the gauges
     *
     * @param results
     */
    private void processResults(List<String> results) {
        // Process the results and update UI

        // Example: Update the coolant temperature gauge
        String coolantTempResult = extractNumericPart(results.get(0));
        if (coolantTempResult != null) {
            int coolantTemp = Integer.parseInt(coolantTempResult);
            updateCoolantTemperature(coolantTemp);
        }

        // Example: Update the intake temperature gauge
        String intakeTempResult = extractNumericPart(results.get(1));
        if (intakeTempResult != null) {
            int intakeTemp = Integer.parseInt(intakeTempResult);
            updateAirIntakeTemperature(intakeTemp);
        }

        // Example: Update the fuel trim gauge
        String fuelTrimResult = extractNumericPart(results.get(2));
        if (fuelTrimResult != null) {
            int fuelTrimResultVal = Integer.parseInt(fuelTrimResult);
            updateFuelTrim(fuelTrimResultVal);
        }

        // ... Process other OBD2 commands here ...
    }

    /**
     * extracts only the numeric parts of our obd2 commands
     *
     * @param result
     * @return
     */
    private String extractNumericPart(String result) {
        // Extract numeric part from the result (remove non-numeric characters)
        return result != null ? result.replaceAll("[^0-9]", "") : null;
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
