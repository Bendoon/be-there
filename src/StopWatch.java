package com.example.combiningprojects;

/**
 * Created by brend on 15/03/2017.
 */

public class StopWatch {

    long startTime =0;
    long stopTime = 0;
    double disconnectTime;

    public void StartTimeInterval()
    {
        startTime = 0;
        stopTime = 0;
        startTime = System.nanoTime();
    }

    public void StopTimeInterval(String messageTimePrefix)
    {
        stopTime = System.nanoTime();
        disconnectTime = (stopTime - startTime) / 1000000000.0;
        System.out.println(messageTimePrefix + ": " + disconnectTime);
    }
}
