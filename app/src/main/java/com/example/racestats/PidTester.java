package com.example.racestats;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class PidTester extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pid_tester);

        long startTime = System.currentTimeMillis();

        for (int firstDigit = 0; firstDigit < 10; firstDigit++) {
            for (int secondDigit = 0; secondDigit < 36; secondDigit++) {
                for (int thirdDigit = 0; thirdDigit < 10; thirdDigit++) {
                    for (int fourthDigit = 0; fourthDigit < 10; fourthDigit++) {
                        String code = generateCode(firstDigit, secondDigit, thirdDigit, fourthDigit);
                        System.out.println(code);
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Time taken: " + elapsedTime + " milliseconds");
    }

    public static String generateCode(int first, int second, int third, int fourth) {
        char secondChar = (second < 10) ? (char) ('0' + second) : (char) ('A' + second - 10);
        return "" + first + secondChar + third + fourth;
    }
}
