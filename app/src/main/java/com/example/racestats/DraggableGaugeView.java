package com.example.racestats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DraggableGaugeView extends View {
    private float lastTouchX;
    private float lastTouchY;
    private String text = "";

    private Paint textPaint = new Paint();

    public DraggableGaugeView(Context context) {
        super(context);
        init();
    }

    public DraggableGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Set the text color to white
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30); // Set the text size
    }

    // Add a method to set the text
    public void setText(String newText) {
        text = newText;
        invalidate(); // Invalidate the view to trigger a redraw
    }

    public void setTextColor(int color) {
        textPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the text on the canvas
        canvas.drawText(text, 20, 40, textPaint);  // Adjust the coordinates as needed
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = x - lastTouchX;
                float deltaY = y - lastTouchY;

                // Calculate new position and update layout
                float newX = getX() + deltaX;
                float newY = getY() + deltaY;
                setX(newX);
                setY(newY);

                lastTouchX = x;
                lastTouchY = y;
                break;
        }
        return true;
    }
}
