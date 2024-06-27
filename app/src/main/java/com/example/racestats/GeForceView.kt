package com.example.racestats

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.TextView

// TODO Make UI work on all screen sizes and fix Gtext offset

class GeForceView : View {

    // Dot properties
    private val dotRadius = 15f
    private val dotPaint = Paint().apply {
        color = Color.RED
    }

    // Current forces
    private var leftRightForce = 0f
    private var upDownForce = 0f
    // Add a property to track max G-force
    private var maxGForce = 0f

    // Calibration offsets to reset forces to 0
    private var calibrationOffsetLeftRight = 0f
    private var calibrationOffsetUpDown = 0f

    // Code for Gdot trail
    private val trailPaint = Paint().apply {
        color = Color.parseColor("#C60000")
        alpha = 75 // Adjust the transparency here (0 to 255)
        style = Paint.Style.FILL
    }

    private val trailSize = 35
    private val trailDotRadius = dotRadius / 2 + 2  // Adjust the size of the trail dots here
    private val trailDots = mutableListOf<Pair<Float, Float>>()

    // Constructors
    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        // Initialization code if needed
        leftRightForce = 0f
        upDownForce = 0f
    }

    // Drawing method
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculate center coordinates
        val padding = 20.dpToPx() // Convert dp to pixels
        val centerX = (width - padding * 2) / 2f + padding
        val centerY = (height - padding * 2) / 2f + padding

        // Adjusted radii for better visibility
        val circleRadius1 = (width - padding * 4) / 10f * 0.9f // Scale down by about 10%
        val circleRadius2 = (width - padding * 4) / 5f * 0.9f // Scale down by about 10%
        val circleRadius3 = (width - padding * 4) / 3.5f * 0.9f // Scale down by about 10%


        // Draw the trail dots
        for (i in 0 until trailDots.size) {
            val trailDot = trailDots[i]
            val dotX = centerX + trailDot.first * circleRadius3
            val dotY = centerY - trailDot.second * circleRadius3

            val alpha = (255 - (i * (255 / trailSize))).coerceAtLeast(0)
            trailPaint.alpha = alpha

            canvas.drawCircle(dotX, dotY, trailDotRadius, trailPaint)
        }

        // Draw circles
        drawCircle(canvas, centerX, centerY, circleRadius1, 0.5f)
        drawCircle(canvas, centerX, centerY, circleRadius2, 1f)
        drawCircle(canvas, centerX, centerY, circleRadius3, 1.5f)

        // Draw dot based on forces
        val dotX = centerX + leftRightForce * circleRadius3
        val dotY = centerY - upDownForce * circleRadius3
        canvas.drawCircle(dotX, dotY, dotRadius, dotPaint)

        // Update trail dots
        trailDots.add(0, Pair(leftRightForce, upDownForce))
        if (trailDots.size > trailSize) {
            trailDots.removeAt(trailDots.size - 1)
        }
    }


    // Calibration method to reset forces to 0
    fun calibrate() {
        calibrationOffsetLeftRight += -leftRightForce
        calibrationOffsetUpDown += -upDownForce
        maxGForce = 0f
    }

    // Method to update forces
    fun updateForces(leftRightForce: Float, upDownForce: Float) {
        this.leftRightForce = leftRightForce + calibrationOffsetLeftRight
        this.upDownForce = upDownForce + calibrationOffsetUpDown

        // Update the TextViews directly
        (context as? Activity)?.runOnUiThread {
            updateTextViews()
        }

        invalidate()
    }

    // Method to update the TextViews
    private fun updateTextViews() {
        // Access the TextViews from the parent activity
        val brakingForceTextView = (context as? GMeter)?.findViewById<TextView>(R.id.brakingForceTextView)
        val accelerationForceTextView = (context as? GMeter)?.findViewById<TextView>(R.id.accelerationForceTextView)
        val rightForceTextView = (context as? GMeter)?.findViewById<TextView>(R.id.rightForceTextView)
        val leftForceTextView = (context as? GMeter)?.findViewById<TextView>(R.id.leftForceTextView)
        val maxGForceTextView = (context as? GMeter)?.findViewById<TextView>(R.id.maxGForceTextView)

        // Calculate the forces for TextViews
        val brakingG = upDownForce * 1.5f
        val accelerationG = if (upDownForce > 0) 0f else -upDownForce * 1.5f
        val rightForceG = leftRightForce * 1.5f
        val leftForceG = if (leftRightForce > 0) 0f else -leftRightForce * 1.5f

        // Calculate the max G-Force
        val newMaxGForce = maxOf(
            brakingG + leftForceG,
            brakingG + rightForceG,
            accelerationG + leftForceG,
            accelerationG + rightForceG,
            accelerationG,
            brakingG,
            leftForceG,
            rightForceG
        )

        // Update the max G-Force if needed
        if (newMaxGForce > maxGForce) {
            maxGForce = newMaxGForce
        }

        // TODO need to flip the accleration and braking Gs on the Gdot
        // Update the TextViews with the calculated values
        brakingForceTextView?.text = "Braking Force: ${String.format("%.2f G", if (accelerationG < 0) 0f else accelerationG)}"
        accelerationForceTextView?.text = "Acceleration Force: ${String.format("%.2f G", if (brakingG < 0) 0f else brakingG)}"
        rightForceTextView?.text = "Right Force: ${String.format("%.2f G", if (rightForceG < 0) 0f else rightForceG)}"
        leftForceTextView?.text = "Left Force: ${String.format("%.2f G", if (leftForceG < 0) 0f else leftForceG)}"
        maxGForceTextView?.text = "Max G-Force: ${String.format("%.2f G", maxGForce)}"
    }

    // Draw a circle with a label
    private fun drawCircle(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, label: Float) {
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = 5f
        }

        canvas.drawCircle(centerX, centerY, radius, paint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 30f
        }

        val textX = centerX - 26f
        val textY = centerY + radius + 40f

        canvas.drawText("$label G", textX, textY, textPaint)
    }

    // Extension function to convert dp to pixels
    private fun Int.dpToPx(): Float {
        return this * resources.displayMetrics.density
    }
}