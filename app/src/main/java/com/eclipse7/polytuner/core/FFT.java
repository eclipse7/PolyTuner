package com.eclipse7.polytuner.core;

public class FFT {
    private int n, m;
    private double scaleWindow;
    private double[] cos;
    private double[] sin;
    private double[] window;

    static {
        System.loadLibrary("fft-lib");
    }

    FFT(int n) {
            this.n = n;
            this.m = (int)(Math.log(n) / Math.log(2));

        // Make sure n is a power of 2
        if(n != (1<<m))
            throw new RuntimeException("FFT length must be power of 2");

        // precompute tables
        cos = new double[n/2];
        sin = new double[n/2];
        for(int i=0; i<n/2; i++) {
            cos[i] = Math.cos(-2*Math.PI*i/n);
            sin[i] = Math.sin(-2*Math.PI*i/n);
        }

        window = new double[n];
        double a = (n-1)/2.0;
        double r = 7.0;
        double scale = 0;
        for(int i = 0; i < n; i++){
            window[i] = Math.exp(-0.5 * Math.pow(r*(i-a)/n, 2.0));
            scale += window[i];
        }
        scaleWindow = n / scale ;
    }

    void calcAmpSpectrum(double[] real, double[] imag, double[] ampSpectrum) {
        mulWindow(real, imag);
        fft(real, imag);
        calcMagnitude(real, imag, ampSpectrum);
    }

    private native void fft(double[] x, double[] y);
    private native void calcMagnitude(double[] x, double[] y, double[] amps);
    private native void mulWindow(double[] x, double[] y);
}
