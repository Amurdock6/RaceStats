package com.example.racestats;

import android.util.Log;

import com.github.pires.obd.commands.ObdCommand;

import java.io.IOException;
import java.io.OutputStream;

public class CustomPIDCommand extends ObdCommand {

    private final String customName;
    private int rawValue;

    public CustomPIDCommand(String customName) {
        super(customName);
        this.customName = customName;
    }

    @Override
    protected void performCalculations() {
        // Assuming the raw data is at index 2
        rawValue = buffer.get(2) & 0xFF; // Convert to unsigned byte
    }

    @Override
    public String getFormattedResult() {
        // Format the result for display
        return "Raw Result: " + rawValue;
    }

    @Override
    public String getCalculatedResult() {
        // Return the raw result as a string
        return String.valueOf(rawValue);
    }

    @Override
    public String getName() {
        return customName;
    }

    @Override
    protected void sendCommand(OutputStream out) throws IOException, InterruptedException {
        // Include any additional formatting required for your specific OBD-II device
        Log.d("CustomPIDCommand", "Sending command: " + cmd);
        super.sendCommand(out);
    }
}
