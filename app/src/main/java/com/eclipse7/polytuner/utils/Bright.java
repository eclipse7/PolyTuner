package com.eclipse7.polytuner.utils;

public class Bright {

    public static float up(float value, float speedUp) {
        value += speedUp;
        if (value > 1) value = 1;
        return value;
    }

    public static float down(float value, float speedDown) {
        value -= speedDown;
        if (value < 0) value = 0;
        return value;
    }

}
