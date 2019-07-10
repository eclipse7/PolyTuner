package com.eclipse7.polytuner.core;

public class FFT {
    private int n, m;
    private double scaleWindow;
    private double[] cos;
    private double[] sin;
    private double[] window;

    public FFT(int n) {
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

    public void calcAmpSpectrum(double[] real, double[] imag, double[] ampSpectrum) {
        double normalize = 2.0 / n;

        for(int i = 0; i < n; i++){
            real[i] *= window[i];
            imag[i] = 0;
        }

        fft(real, imag);
        for(int i = 0; i < n/2; i++) {
            ampSpectrum[i] = Math.sqrt(real[i]*real[i] + imag[i]*imag[i]) * normalize;
        }
    }

    public void fft(double[] x, double[] y) {
        int n1,a;
        double c,s,t1,t2;

        // Bit-reverse
        int j = 0;
        int n2 = n/2;
        for (int i = 1; i < n - 1; i++) {
            n1 = n2;
            while ( j >= n1 ) {
                j = j - n1;
                n1 = n1/2;
            }
            j = j + n1;

            if (i < j) {
                t1 = x[i];
                x[i] = x[j];
                x[j] = t1;
                t1 = y[i];
                y[i] = y[j];
                y[j] = t1;
            }
        }

        // FFT
        n1 = 0;
        n2 = 1;

        for (int i=0; i < m; i++) {
            n1 = n2;
            n2 = n2 + n2;
            a = 0;

            for (j=0; j < n1; j++) {
                c = cos[a];
                s = sin[a];
                a +=  1 << (m-i-1);

                for (int k = j; k < n; k=k+n2) {
                    t1 = c*x[k+n1] - s*y[k+n1];
                    t2 = s*x[k+n1] + c*y[k+n1];
                    x[k+n1] = x[k] - t1;
                    y[k+n1] = y[k] - t2;
                    x[k] = x[k] + t1;
                    y[k] = y[k] + t2;
                }
            }
        }
    }
}
