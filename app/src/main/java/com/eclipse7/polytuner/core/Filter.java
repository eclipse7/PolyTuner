package com.eclipse7.polytuner.core;

public class Filter {
    private int p, q;
    private double[] a;
    private double[] b;
    private double[] xMem;
    private double[] yMem;

    public Filter(double[] a, double[] b) {
        this.p = a.length;
        this.q = b.length;
        this.a = a.clone();
        this.b = b.clone();
        xMem = new double[p];
        yMem = new double[q];
        for (int i = 0; i < p; i++) xMem[i] = 0;
        for (int i = 0; i < q; i++) yMem[i] = 0;
    }

    public final double filtering(double x) {
        if (Double.isNaN(x)) x = 0;

        //shift
        if (p >= 2) {
            for (int i = (p - 1); i > 0; i--)
                xMem[i] = xMem[i - 1];
        }
        if (q >= 2) {
            for (int i = (q - 1); i > 0; i--)
                yMem[i] = yMem[i - 1];
        }

        xMem[0] = x;
        yMem[0] = 0;
        for (int i = 0; i < p; i++)
            yMem[0] += a[i] * xMem[i];
        for (int i = 1; i < q; i++)
            yMem[0] +=  (-1) * b[i] * yMem[i];

        return yMem[0];
    }
}
