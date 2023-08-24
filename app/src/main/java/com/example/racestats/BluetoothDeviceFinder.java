package com.example.racestats;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.S)
public class BluetoothDeviceFinder extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> devicesArrayAdapter;
    private ArrayList<String> devicesList;

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.S)
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions();
                }

                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                String deviceInfo = deviceName + "\n" + deviceAddress;

                devicesList.add(deviceInfo);
                devicesArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.paired_blue_tooth_list);

        requestPermissions();

        ListView devicesListView = findViewById(R.id.devicesListView);
        devicesList = new ArrayList<>();
        devicesArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, devicesList);
        devicesListView.setAdapter(devicesArrayAdapter);

        Button refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshBluetoothDevices();
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.d("bluetooth error", "Device Does Not Support Bluetooth");
            // TODO need to add text view for the users to see letting them know that Bluetooth is not supported on there device
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Error in permission check here", Toast.LENGTH_SHORT).show();
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        Log.d("Test", "onCreate: Test 106");
        for (BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();
            String deviceAddress = device.getAddress();
            String deviceInfo = deviceName + "\n" + deviceAddress;
            devicesList.add(deviceInfo);
        }
        devicesArrayAdapter.notifyDataSetChanged();


        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothReceiver, filter);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsBlueToothScan(new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
        }


        bluetoothAdapter.startDiscovery();


        /**
         * Will redirect user to the Digital Dash Board View
         */
        devicesListView.setOnItemClickListener((parent, view, position, id) -> {
            String deviceInfo = devicesArrayAdapter.getItem(position);
            String[] deviceInfoArray = deviceInfo.split("\n");
            String deviceAddress = deviceInfoArray[1];

            Intent intent = new Intent(BluetoothDeviceFinder.this, DigitalDash.class);
            intent.putExtra("deviceAddress", deviceAddress); // Pass the device address to DigitalDash activity
            startActivity(intent);
        });

    }

//    private String parseOilPressureResponse(String response) {
//        // Assuming the response contains the oil pressure value in the form "PID:VALUE"
//        String[] parts = response.split(":");
//        if (parts.length >= 2) {
//            return parts[1].trim(); // Extract the value part
//        }
//        return "N/A"; // Return "Not Available" if parsing fails
//    }

    /**
     * @param strings
     * @param i
     */
    private void requestPermissionsBlueToothScan(String[] strings, int i) {
        if (getApplicationContext().checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            // Access granted
            Log.d("Permissions", "Permission Already granted");
        } else {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
        }
    }

    /**
     *
     */
    private void requestPermissions() {
        if (getApplicationContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            // Access granted
            Log.d("Permissions", "Permission Already granted");
        } else {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
    }

    /**
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Access granted
                Log.d("Permissions", "Permission Granted to BluetoothConnect");
            } else {
                Log.d("Permissions", "Denied Permission to BluetoothConnect");
            }
        }
    }


    /**
     * In charge of destroying the bluetooth connection between the device and the OBD2 scanner
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceiver);
    }

    /**
     * This will refresh the paired devices that are showing up in the UI
     */
    private void refreshBluetoothDevices() {
        devicesList.clear();
        devicesArrayAdapter.notifyDataSetChanged();
        startBluetoothDiscovery();
    }

    /**
     * This will check to make sure all of the appropriate permissions are granted by the user
     */
    private void startBluetoothDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Error Permissions", "229 Permissions error");
            return;
        }

        if (!bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.startDiscovery();
        }
    }

}
