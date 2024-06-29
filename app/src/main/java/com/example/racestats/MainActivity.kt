package com.example.racestats

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var ecuInfo: View
    private lateinit var g_meter: View
    private lateinit var maintenance_records: View
    private lateinit var digital_dash: View
    private lateinit var zero_to_sixty: View
    private lateinit var settings: View

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set the activity to full-screen mode
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        // Grab our link variables
        ecuInfo = findViewById(R.id.ecuInfo)
        g_meter = findViewById(R.id.g_meter)
        maintenance_records = findViewById(R.id.maintenance_records)
        digital_dash = findViewById(R.id.digital_dash)
        zero_to_sixty = findViewById(R.id.zero_to_sixty)
        settings = findViewById(R.id.settings)

        // Links to other views
        ecuInfo.setOnClickListener {
            val intent = Intent(this, LapTimerActivity::class.java)
            startActivity(intent)
        }

        g_meter.setOnClickListener {
            val intent = Intent(this, GMeter::class.java)
            startActivity(intent)
        }

        maintenance_records.setOnClickListener {
            val intent = Intent(this, ServiceRecords::class.java)
            startActivity(intent)
        }

        digital_dash.setOnClickListener {
            val intent = Intent(this, BluetoothDeviceFinder::class.java)
            startActivity(intent)
        }

        zero_to_sixty.setOnClickListener {
            val intent = Intent(this, ZeroToSixty::class.java)
            startActivity(intent)
        }

        settings.setOnClickListener {
            val intent = Intent(this, Settings::class.java)
            startActivity(intent)
        }

        // Check and request permission for system boot
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission()
        }
    }

//     Ask User for all needed permissions
//    private val requestPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted: Boolean ->
//        if (isGranted) {
//            Log.i("Permission: ", "Granted")
//        } else {
//            Log.i("Permission: ", "Denied")
//        }
//    }
//
//    private fun requestPermission() {
//        when {
//            ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.BLUETOOTH_CONNECT
//            ) == PackageManager.PERMISSION_GRANTED -> {
//                // Permission is granted
//            }
//            ActivityCompat.shouldShowRequestPermissionRationale(
//                this,
//                Manifest.permission.BLUETOOTH_CONNECT
//            ) -> {
//                // Additional rationale should be displayed
//            }
//            else -> {
//                // Permission has not been asked yet
//            }
//        }
//    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECEIVE_BOOT_COMPLETED
            )
        ) {
            // Show rationale if needed
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This permission is needed to start the app on system boot.")
                .setPositiveButton("OK") { _: DialogInterface, _: Int ->
                    requestSystemBootPermission()
                }
                .setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    // Handle if the user denies permission
                    // You can show a message or take appropriate action here
                    Toast.makeText(
                        this,
                        "Permission denied. App may not start on system boot.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .create()
                .show()
        } else {
            requestSystemBootPermission()
        }
    }

    private fun requestSystemBootPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECEIVE_BOOT_COMPLETED),
            PERMISSION_REQUEST_SYSTEM_BOOT
        )
    }

    companion object {
        private const val PERMISSION_REQUEST_SYSTEM_BOOT = 1001
    }

    // Handle permission request result if needed (not included in the provided code)
    // Override onRequestPermissionsResult to handle the permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_SYSTEM_BOOT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(
                    this,
                    "Permission granted. App can start on system boot.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Permission denied
                Toast.makeText(
                    this,
                    "Permission denied. App may not start on system boot.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}