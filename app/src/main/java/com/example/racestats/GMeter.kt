package com.example.racestats

//import android.os.Bundle
//import android.view.animation.AlphaAnimation
//import android.view.animation.Animation
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import kotlin.math.sin
//
//class GMeter : AppCompatActivity() {
//    private lateinit var gdot: ImageView
//    private var timeElapsed: Float = 0f
//    private var maxGForce: Float = 1.5f
//    private var scaleFactor: Int = 1000
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.gmeter)
//
//        gdot = findViewById(R.id.gdot)
//
//        // Start a thread to simulate sensor events and update the red dot position
//        Thread {
//            while (true) {
//                timeElapsed += 0.1f  // Simulated time increment
//                val gForceX = calculateGForce(timeElapsed, 1.5f)  // Simulated X-axis g-force
//                val gForceY = calculateGForce(timeElapsed, 1.5f)  // Simulated Y-axis g-force
//                runOnUiThread {
//                    updateGdotPosition(gForceX, gForceY)
//                }
//                Thread.sleep(100)  // Simulated sensor delay
//            }
//        }.start()
//    }
//
//    private fun calculateGForce(timeElapsed: Float, frequency: Float): Float {
//        // Simulate a periodic sine wave to create a changing g-force
//        // Adjust this logic based on your specific requirements for testing
//        val amplitude = 2.0f  // Adjust the amplitude of the sine wave
//        return amplitude * sin(2 * Math.PI * frequency * timeElapsed).toFloat()
//    }
//
//    private fun updateGdotPosition(gForceX: Float, gForceY: Float) {
//        // Calculate the translation based on the available space
//        val translationX =
//            gForceX * dpToPx(150) / maxGForce  // Adjust the factor based on the available space
//        val translationY =
//            gForceY * dpToPx(150) / maxGForce  // Adjust the factor based on the available space
//
//        // Set the translation on both X and Y axes
//        gdot.translationX = translationX
//        gdot.translationY = translationY
//    }
//
//    private fun dpToPx(dp: Int): Float {
//        val density = resources.displayMetrics.density
//        return dp * density
//    }
//}


import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class GMeter : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var gdot: ImageView
    private var maxGForceRecorded: Float = 0.0f // Variable to store highest G-force

    private lateinit var accelGsTextView: TextView
    private lateinit var brakingGsTextView: TextView
    private lateinit var leftGsTextView: TextView
    private lateinit var rightGsTextView: TextView
    private lateinit var calibrateButton: Button

    // Variables to store the calibration offsets
    private var xOffset: Float = 0.0f
    private var yOffset: Float = 0.0f
    private var zOffset: Float = 0.0f
    private var isCalibrated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gmeter)

        // Set the activity to full-screen mode
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        gdot = findViewById(R.id.gdot)
        accelGsTextView = findViewById(R.id.AccelGs)
        brakingGsTextView = findViewById(R.id.BrakingGs)
        leftGsTextView = findViewById(R.id.LeftGs)
        rightGsTextView = findViewById(R.id.RightGs)

        calibrateButton = findViewById(R.id.calibrateButton);

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        calibrateButton.setOnClickListener {
            onCalibrateButtonClick()
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Log.d("G-Meter Error", "onResume: No Accelerometer Detected")
            showNoAccelerometerDialog()
        }
    }

    private fun showNoAccelerometerDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("No Accelerometer Detected")
            .setMessage("This device does not have an accelerometer, so the G-Meter functionality is not available.")
            .setPositiveButton("Back") { _, _ ->
                // Handle the 'Back' button click
                // Navigate back to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)

        val dialog = builder.create()
        dialog.show()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }

    private var initialSensorEvent: SensorEvent? = null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            // Store the initial sensor event for calibration
            if (!isCalibrated && initialSensorEvent == null) {
                initialSensorEvent = event
            }

            var accelX = event.values[0]
            var accelY = event.values[1]

            // Apply calibration offsets if calibrated
            if (isCalibrated) {
                accelX -= xOffset
                accelY -= yOffset
            }

            val leftGs = calculateLeftGs(accelX)
            val rightGs = calculateRightGs(accelX)
            val accelGs = calculateAccelGs(accelY)
            val brakingGs = calculateBrakingGs(accelY)

            updateGdotPosition(leftGs, rightGs, accelGs, brakingGs)
            updateHighScore(leftGs) // Update high score with leftGs for demonstration

            // Update UI to display the G-forces
            leftGsTextView.text = "Left G's: $leftGs"
            rightGsTextView.text = "Right G's: $rightGs"
            accelGsTextView.text = "Acceleration G's: $accelGs"
            brakingGsTextView.text = "Braking G's: $brakingGs"
        }
    }

    private fun calculateLeftGs(accelX: Float): Float {
        // Calculate G-force in the negative x direction
        return accelX.coerceIn(0f, Float.MAX_VALUE)
    }

    private fun calculateRightGs(accelX: Float): Float {
        // Calculate G-force in the positive x direction
        return -accelX.coerceIn(-Float.MAX_VALUE, 0f)
    }

    private fun calculateAccelGs(accelY: Float): Float {
        // Calculate G-force in the positive y direction (acceleration)
        return accelY.coerceIn(0f, Float.MAX_VALUE)
    }

    private fun calculateBrakingGs(accelY: Float): Float {
        // Calculate G-force in the negative y direction (braking)
        return -accelY.coerceIn(-Float.MIN_VALUE, 0f)
    }

    private fun updateGdotPosition(
        leftGs: Float,
        rightGs: Float,
        accelGs: Float,
        brakingGs: Float
    ) {
        // Set the position of the red dot based on G-forces
        // Adjust this logic based on your specific requirements and scaling
        val maxGForce = 1.5f  // Maximum g-force to display
        val scaleFactor = 1000  // Scale factor for dot movement

        // Calculate total G-force
        val totalGForce =
            kotlin.math.sqrt(leftGs * leftGs + rightGs * rightGs + accelGs * accelGs + brakingGs * brakingGs)
        val normalizedGForce = (totalGForce / maxGForce).coerceIn(0.0f, 1.0f)
        val translationY = normalizedGForce * scaleFactor

        gdot.translationY = translationY
    }

    private fun updateHighScore(gForce: Float) {
        // Update the high score if the current G-force is higher
        if (gForce > maxGForceRecorded) {
            maxGForceRecorded = gForce
            // Update UI to display the highest G-force
            // Assuming you have a TextView with the id 'highScoreTextView'
            findViewById<TextView>(R.id.highScoreTextView).text =
                "Highest G-force: $maxGForceRecorded"
        }
    }

    // Function to handle calibration button click
    fun onCalibrateButtonClick() {
        // Measure initial readings and set as offsets for calibration
        // Assuming the device is in the desired calibration position at this point

        // Handle the initial sensor event
        val initialEvent = initialSensorEvent

        if (initialEvent != null) {
            val initialX = initialEvent.values[0]
            val initialY = initialEvent.values[1]
            val initialZ = initialEvent.values[2]

            // Store the initial readings as offsets for calibration
            xOffset = initialX
            yOffset = initialY
            zOffset = initialZ

            // Set calibration flag to true
            isCalibrated = true
        } else {
            Log.e("Calibration Error", "onCalibrateButtonClick: No initial sensor event")
        }
    }
}