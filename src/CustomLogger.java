package com.example.combiningprojects;

/**
 * Created by brend on 27/11/2016.
 */
//Class for setting print statements to be used for debugging
public class CustomLogger {

    private static final boolean useDebug = true;

    static public void println2(String print)
    {
        if(useDebug) {
            System.out.println(print);
        }
    }
}
