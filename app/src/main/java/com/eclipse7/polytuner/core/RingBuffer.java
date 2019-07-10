package com.eclipse7.polytuner.core;

public class RingBuffer {
    private double buffer[];
    private int bufferSize;
    private int stepSize;
    private int beginOfRing = 0;

    public RingBuffer(int bufferSize, int stepSize) {
        this.bufferSize = bufferSize;
        this.stepSize = stepSize;
        buffer = new double[bufferSize];
    }

    public void writeToRing(double[] data) {
        if (stepSize != data.length)
            return;

        int startRec = beginOfRing + bufferSize - stepSize;
        for (int i = 0; i < data.length; i++) {
            buffer[(startRec + i) % bufferSize] = data[i];
        }
    }

    public void readFromRing(double[] real, double[] imag) {
        if ((real.length != bufferSize) || (imag.length != bufferSize))
            return;

        for (int i = 0; i < bufferSize; i++) {
            real[i] = buffer[(beginOfRing + i) % bufferSize];
            imag[i] = 0;
        }
        beginOfRing = (beginOfRing + stepSize) % bufferSize;
    }
}
