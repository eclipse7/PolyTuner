package com.eclipse7.polytuner.utils;

public class Time {

    public static final long    SECOND = 1000000000l;

    public static long getTime() {
        return System.nanoTime();
    }

}
