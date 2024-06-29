package com.example.racestats

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class GMeter : AppCompatActivity() {
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var geForceView: GeForceView
    private lateinit var calibrateButton: Button
    private lateinit var backbutton: ImageButton

    // TextViews for Forces
    private lateinit var leftForceTextView: TextView
    private lateinit var rightForceTextView: TextView
    private lateinit var brakingForceTextView: TextView
    private lateinit var accelerationForceTextView: TextView
    private lateinit var maxGForceTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gmeter)

        // Set the activity to full-screen mode
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!

        geForceView = findViewById(R.id.geForceView)
        calibrateButton = findViewById(R.id.calibrateButton)
        backbutton = findViewById(R.id.backArrow)

        // Initialize TextViews
        leftForceTextView = findViewById(R.id.leftForceTextView)
        rightForceTextView = findViewById(R.id.rightForceTextView)
        brakingForceTextView = findViewById(R.id.brakingForceTextView)
        accelerationForceTextView = findViewById(R.id.accelerationForceTextView)
        maxGForceTextView = findViewById(R.id.maxGForceTextView)

        // Set click listener for calibrate button
        calibrateButton.setOnClickListener { onCalibrateClick(it) }

        // Set a click listener for the back button
        backbutton.setOnClickListener {
            // Rotate and change the drawable based on the rotation state
            val rotation = if (backbutton.rotation == 0f) 355f else 0f
            val scaleX = if (backbutton.rotation == 0f) 0.8f else 1.0f
            val scaleY = if (backbutton.rotation == 0f) 0.8f else 1.0f

            val rotationAnim = ObjectAnimator.ofFloat(backbutton, "rotation", rotation)
            val scaleXAnim = ObjectAnimator.ofFloat(backbutton, "scaleX", scaleX)
            val scaleYAnim = ObjectAnimator.ofFloat(backbutton, "scaleY", scaleY)

            val set = AnimatorSet()
            set.playTogether(rotationAnim, scaleXAnim, scaleYAnim)
            set.duration = 450
            set.start()

            val intent = Intent(this@GMeter, MainActivity::class.java)
            startActivity(intent)
        }

        // Handles initial calibration of Gmeter
        Handler().postDelayed({
            geForceView.calibrate()
        }, 100)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            accelerometerListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(accelerometerListener)
    }

    private val MAX_GRAVITY = 9.81f  // Maximum gravity on Earth

    private val accelerometerListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent) {
            // Assuming event.values[0] is left/right, event.values[1] is up/down
            val leftRightForce = event.values[0] / MAX_GRAVITY
            val upDownForce = event.values[1] / MAX_GRAVITY

            // Assuming your custom view has appropriate methods to update the forces
            geForceView.updateForces(leftRightForce, upDownForce)
        }
    }

    // Calibrate button click handler
    private fun onCalibrateClick(view: View) {
        // Calibrate the GeForceView
        geForceView.calibrate()
    }
}