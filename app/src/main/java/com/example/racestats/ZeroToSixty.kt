package com.example.racestats

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Color.parseColor
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import java.io.*
import java.util.*


class ZeroToSixty : AppCompatActivity() {
    // Import the variables we will be editing from the UI XML file
    private lateinit var cpuTemp: TextView
    private lateinit var speed: TextView
    private lateinit var mph: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var timer: TextView
    private lateinit var startStopTimer: androidx.appcompat.widget.AppCompatButton
    private lateinit var recordedTimes: TextView
    private lateinit var yellowTimesBar: View
    private lateinit var recordedTimeOne: TextView
    private lateinit var recordedTimeTwo: TextView
    private lateinit var recordedTimeThree: TextView
    private lateinit var homeIcon: ImageView

    // deals with 0-60 timer
    private var targetTime: Double = 120.0
    private var methodRunning = false

    // will help us update cpu temp
    private val handler = Handler()
    private lateinit var cpuTempUpdateRunnable: Runnable
    private val COLOR_YELLOW = Color.YELLOW
    private val COLOR_ORANGE = Color.rgb(255, 165, 0) // Orange color
    private val COLOR_RED = Color.RED

    // deals with menu animation
    private var isHomeIconRotated = false


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.zerotosixty)

        // Set the activity to full-screen mode
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        // Declare are imported variables from the XML file
        homeIcon = findViewById(R.id.homeIcon)

        cpuTemp = findViewById(R.id.cpuTemp)
        speed = findViewById(R.id.speed)
        mph = findViewById(R.id.mph)
        progressBar = findViewById(R.id.progressBar)
        timer = findViewById(R.id.timer)
        startStopTimer = findViewById(R.id.startStopTimer)

        recordedTimes = findViewById(R.id.recordedTimes)
        yellowTimesBar = findViewById(R.id.yellowTimesBar)
        recordedTimeOne = findViewById(R.id.recordedTimeOne)
        recordedTimeTwo = findViewById(R.id.recordedTimeTwo)
        recordedTimeThree = findViewById(R.id.recordedTimeThree)


        // cpuTemp update runnable
        cpuTempUpdateRunnable = object : Runnable {
            override fun run() {
                getCpuTemperature { cpuTemperature ->
                    // Round the cpuTemperature to the nearest integer
                    val roundedTemp = cpuTemperature.toInt()

                    // Update the cpuTemp TextView with the rounded temperature
                    cpuTemp.text = "CPU Temp: ${roundedTemp}°C"

                    // Update the text color based on the temperature range
                    when {
                        roundedTemp >= 85 -> cpuTemp.setTextColor(COLOR_RED)
                        roundedTemp >= 76 -> cpuTemp.setTextColor(COLOR_ORANGE)
                        roundedTemp >= 70 -> cpuTemp.setTextColor(COLOR_YELLOW)
                        else -> cpuTemp.setTextColor(Color.WHITE) // Default color for temperatures below 70°C
                    }
                }

                // Schedule the next update after 10 seconds
                handler.postDelayed(this, 10000)
            }
        }

        // Start the periodic updates for cpuTemp
        handler.post(cpuTempUpdateRunnable)


        /**
         * Navigation animation
         */
        // Set a click listener for the home icon
        homeIcon.setOnClickListener {
            // Rotate and change the drawable based on the rotation state
            if (isHomeIconRotated) {
                val rotationAnim = ObjectAnimator.ofFloat(homeIcon, "rotation", 55f, 0f)
                val scaleXAnim = ObjectAnimator.ofFloat(homeIcon, "scaleX", 1.0f, 0.8f)
                val scaleYAnim = ObjectAnimator.ofFloat(homeIcon, "scaleY", 1.0f, 0.8f)
                val set = AnimatorSet()
                set.playTogether(rotationAnim, scaleXAnim, scaleYAnim)
                set.duration = 300
                set.start()

                // Set the bars icon drawable to the ImageView
                val barsIconDrawable = VectorDrawableCompat.create(resources, R.drawable.home_icon, theme)
                homeIcon.setImageDrawable(barsIconDrawable)
            } else {
                val rotationAnim = ObjectAnimator.ofFloat(homeIcon, "rotation", 0f, 90f)
                val scaleXAnim = ObjectAnimator.ofFloat(homeIcon, "scaleX", 1.0f, 1.0f)
                val scaleYAnim = ObjectAnimator.ofFloat(homeIcon, "scaleY", 1.0f, 1.0f)
                val set = AnimatorSet()
                set.playTogether(rotationAnim, scaleXAnim, scaleYAnim)
                set.duration = 500
                set.start()

                // Set the X icon drawable to the ImageView
                val xIconDrawable = VectorDrawableCompat.create(resources, R.drawable.ic_x_icon, theme)
                homeIcon.setImageDrawable(xIconDrawable)
            }
            // Toggle the rotation state
            isHomeIconRotated = !isHomeIconRotated

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        /**
         * Class that is in charge of the pop dialog that allows the user to set the target mph for the timer.
         */
        speed.setOnClickListener {
            val dialog = SpacedNumberPicker(this)
            dialog.minValue = 1
            dialog.maxValue = 200
            dialog.value = targetTime.toInt()
            dialog.textColor = Color.WHITE // Set the text color to white


            val screenHeight = Resources.getSystem().displayMetrics.heightPixels
            val pickerHeight = (screenHeight * 0.75).toInt()

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                pickerHeight
            )

            dialog.setOnValueChangedListener { _, _, newVal ->
                targetTime = newVal.toDouble()
            }

            dialog.layoutParams = lp

            val title = TextView(this).apply {
                text = "Select Target Speed (mph)"
                textSize = 30f // Set the text size of the title
                setTextColor(Color.WHITE) // Set the text color to white
                gravity = Gravity.CENTER // Center the title text
            }

            val alertDialog: AlertDialog = AlertDialog.Builder(this)
                .setCustomTitle(title) // Set the customized title
                .setView(dialog)
                .setPositiveButton("SET", null)
                .create()

            alertDialog.show()

            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
            alertDialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            ) // Set the dialog window to take up the entire screen

            val positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)

            // Set layout parameters for the positive button
            val layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Set the gravity of the button to center and set the margins
            layoutParams.gravity = Gravity.CENTER
            layoutParams.setMargins(430, 10, 430, 10) // Set the margins (left, top, right, bottom)

            // Set the layout parameters of the positive button
            positiveButton.layoutParams = layoutParams

            positiveButton.setTextColor(Color.BLACK)
            positiveButton.setBackgroundColor(parseColor("#FFE222"))
            positiveButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f) // Set the text size of the positive button
        }


        // This will handle our event when a user clicks the start or stop button
        startStopTimer.setOnClickListener {
            // Check to see if start button has already been clicked or not
            if (!methodRunning) {
                methodRunning = true
                startStopTimer.text = "STOP"
                // This wil handle our event when a user clicks the start or stop button
                startStopTimer.setOnClickListener{
                    uiAnimations(targetTime, progressBar, speed, timer)
                }
                methodRunning = false
                startStopTimer.text = "START"
            } else {
                methodRunning = false
                startStopTimer.text = "START"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the periodic updates when the activity is destroyed
        handler.removeCallbacks(cpuTempUpdateRunnable)
    }
}

/**
 * custom number picker I had to make to space the numbers out when selecting a target speed.
 */
class SpacedNumberPicker @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NumberPicker(context, attrs, defStyleAttr) {
    @RequiresApi(Build.VERSION_CODES.Q)
    private val spacing: Int = (textSize * 0.25f).toInt()
    @RequiresApi(Build.VERSION_CODES.Q)
    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = this@SpacedNumberPicker.textSize
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            val x = width / 20f
            val y = height / 20f

            it.drawText(" ", x, y - spacing, textPaint)
            it.drawText(" ", x, y + spacing, textPaint)
        }
    }
}


/**
 * Handles the progress bar, mph, and timer UI display as the speed increases or decreases
 * @param progressBar takes the progressBar variable as a param so we can manipulate it in the UI
 */
fun uiAnimations(targetTime: Double, progressBar: ProgressBar, speedTextView: TextView, timerTextView: TextView) {
    val timer = Timer()
    var speed = 0
    var time = 0

    timer.scheduleAtFixedRate(object : TimerTask() {
        override fun run() {
            speed += 1
            time += 132

            val progress = (speed / targetTime) * 100

            // updates the mph
            speedTextView.text = "$speed"

            // updates progress bar code
            progressBar.progress = progress.toInt()

            // updates the display time
            val minutes = time / 60000
            val seconds = (time % 60000) / 1000
            val milliseconds = time % 1000
            timerTextView.text = "${formatTime(minutes)}:${formatTime(seconds)}.${formatMilliseconds(milliseconds)}"

            println("Speed: $speed mph\tTime: $time ms\tProgress: $progress%")

            if (speed >= targetTime) {

                println("Reached ${targetTime.toInt()} mph in ${formatTime(minutes)}:${formatTime(seconds)}.${formatMilliseconds(milliseconds)}")
                timer.cancel()
            }
        }
    }, 0, 100)
}

fun formatMilliseconds(time: Int): String {
    return String.format("%02d", time/10)
}

fun formatTime(time: Int): String {
    return if (time < 10) {
        "0$time"
    } else {
        "$time"
    }
}

/**
 * Will grab the CPU temp of the system for it to be displayed in the UI.
 */
fun getCpuTemperature(callback: (Float) -> Unit) {
    return try {
        val reader = RandomAccessFile("/sys/devices/virtual/thermal/thermal_zone0/temp", "r")
        val line: String = reader.readLine()
        if (line != null) {
            val temp = line.toFloat() / 1000 // Divide by 1000 to get the temperature in degrees Celsius
            callback(temp) // Invoke the callback with the CPU temperature value
        } else {
            // Handle the case when reading the temperature fails
        }
        reader.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

