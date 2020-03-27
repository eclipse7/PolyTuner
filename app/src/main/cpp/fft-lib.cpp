#include <jni.h>
#include <string>

extern "C" JNIEXPORT void JNICALL
Java_com_eclipse7_polytuner_core_FFT_fft(
        JNIEnv* env,
        jobject obj,
        jdoubleArray xArray,
        jdoubleArray yArray) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID nId = env->GetFieldID(cls, "n", "I");
    jint n = env->GetIntField(obj, nId);
    jfieldID mId = env->GetFieldID(cls, "m", "I");
    jint m = env->GetIntField(obj, mId);

    jfieldID cosId = env->GetFieldID(cls, "cos", "[D");
    jfieldID sinId = env->GetFieldID(cls, "sin", "[D");
    jobject objCosArray = env->GetObjectField(obj, cosId);
    jobject objSinArray = env->GetObjectField(obj, sinId);
    jdoubleArray *cosArray = reinterpret_cast<jdoubleArray *>(&objCosArray);
    jdoubleArray *sinArray = reinterpret_cast<jdoubleArray *>(&objSinArray);

    double *cos = env->GetDoubleArrayElements(*cosArray, 0);
    double *sin = env->GetDoubleArrayElements(*sinArray, 0);
    double *x = env->GetDoubleArrayElements(xArray, 0);
    double *y = env->GetDoubleArrayElements(yArray, 0);

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

    env->ReleaseDoubleArrayElements(*cosArray, cos, 0);
    env->ReleaseDoubleArrayElements(*sinArray, sin, 0);
    env->ReleaseDoubleArrayElements(xArray, x, 0);
    env->ReleaseDoubleArrayElements(yArray, y, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_eclipse7_polytuner_core_FFT_calcMagnitude(
        JNIEnv* env,
        jobject obj,
        jdoubleArray xArray,
        jdoubleArray yArray,
        jdoubleArray ampsArray) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID nId = env->GetFieldID(cls, "n", "I");
    jint n = env->GetIntField(obj, nId);

    double *x = env->GetDoubleArrayElements(xArray, 0);
    double *y = env->GetDoubleArrayElements(yArray, 0);
    double *amps = env->GetDoubleArrayElements(ampsArray, 0);

    double norm = 1.0 / n;
    for(int i = 0; i < n/2; i++) {
        amps[i] = sqrt(x[i]*x[i] + y[i]*y[i]) * norm;
    }

    env->ReleaseDoubleArrayElements(xArray, x, 0);
    env->ReleaseDoubleArrayElements(yArray, y, 0);
    env->ReleaseDoubleArrayElements(ampsArray, amps, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_eclipse7_polytuner_core_FFT_mulWindow(
        JNIEnv* env,
        jobject obj,
        jdoubleArray xArray,
        jdoubleArray yArray) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID nId = env->GetFieldID(cls, "n", "I");
    jint n = env->GetIntField(obj, nId);

    jfieldID windowId = env->GetFieldID(cls, "window", "[D");
    jobject objWindowArray = env->GetObjectField(obj, windowId);
    jdoubleArray *windowArray = reinterpret_cast<jdoubleArray *>(&objWindowArray);

    double *window = env->GetDoubleArrayElements(*windowArray, 0);
    double *x = env->GetDoubleArrayElements(xArray, 0);
    double *y = env->GetDoubleArrayElements(yArray, 0);

    for(int i = 0; i < n; i++) {
        x[i] *= window[i];
        y[i] = 0;
    }

    env->ReleaseDoubleArrayElements(xArray, x, 0);
    env->ReleaseDoubleArrayElements(yArray, y, 0);
    env->ReleaseDoubleArrayElements(*windowArray, window, 0);
}
