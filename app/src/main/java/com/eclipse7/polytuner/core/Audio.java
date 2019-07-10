package com.eclipse7.polytuner.core;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.util.ArrayList;
import java.util.List;

import com.eclipse7.polytuner.R;

//work on fSample 44.1kHz or 48kHz
public class Audio implements Runnable {
    private static final int SIZE = 2048;  // audioRecoder buffer

    private static final int SAMPLES = 8192;    // fft size
    private static final int STEP = 1536;  // size of windows step

    private static final double MIN_AMPLITUDE = -70;
    private static final double MID_AMPLITUDE = -55;
    private static final double MIN_POLY_AMPLITUDE = -70;
    private static final double MIN_FREQ = 26.5;
    private static final double MAX_FREQ = 4500;

    private Thread thread;
    private boolean running = false;
    private Context context;
    private AudioRecord audioRecord;

    private RingBuffer ringbuffer;
    private double[] xReal;
    private double[] xImag;
    private double[] amps;
    private double[] dx;
    private int savedChromaNote = 0;
    private boolean chromaticFound = false;

    private FFT fft;
    private Filter filterCent;
    private Filter filterFreq;
    private List<Filter> polyFilter;

    // Preferences
    private boolean polyMode = false;
    private double reference = 440.0;
    public int[] numberPolyNotes = new int[6];
    public final int[][] numberPolyNotesArray = {
            {-17, -24, -7, -2, 2, 7},   // E
            {-31, -24, -7, -2, 2, 7},	// drop D
            {-18, -13, -8, -3, 1, 6},   // Eb
            {-18, -14, -9, -4, 0, 5},   // D
            {-33, -26, -9, -4, 0, 5},   // drop C
            {-20, -15, -10, -5, -1, 4}, // Db
            {-21, -16, -11, -6, -2, 3}, // C
            {-22, -17, -12, -7, -3, 2}  // B
    };

    // Output data
    private float signalRMS;
    private int sampleRate;
    private byte mode;
    private float chromaticFreq = 0;
    private int chromaticNote = 0;
    private float chromaticCent = 0;
    private boolean[] polyNotes = new boolean[6];
    private float[] polyCents = new float[6];
    private int audioFPS = 0;
    private double maxLevel = 0;

    public int getAudioFPS() {
        return audioFPS;
    }
    public void setAudioFPS(int audioFPS) {
        this.audioFPS = audioFPS;
    }

    public int getSampleRate() {
        return sampleRate;
    }
    public void setPolyMode(boolean polyMode) {
        this.polyMode = polyMode;
    }
    public void setReference(double reference) {
        this.reference = reference;
    }
    public float getSignalRMS() {
        return signalRMS;
    }

    public byte getMode() {
        return mode;
    }
    public float getChromaticFreq() {
        return chromaticFreq;
    }
    public int getChromaticNote() {
        return chromaticNote;
    }
    public float getChromaticCent() {
        return chromaticCent;
    }
    public boolean[] getPolyNotes() {
        return polyNotes;
    }
    public float[] getPolyCents() {
        return polyCents;
    }
    public float getMaxLevel() {
        return (float)maxLevel;
    }

    // Constructor
    public Audio(Context context)
    {
        this.context = context;
        ringbuffer = new RingBuffer(SAMPLES, STEP);
        xReal = new double[SAMPLES];
        xImag = new double[SAMPLES];
        amps = new double[SAMPLES/2];
        dx = new double[SAMPLES/2];

        fft = new FFT(SAMPLES);

        // "gauss 3 order"
        double[] a = {0.03728};
        double[] b = {1.0, -2.17266, 1.63647, -0.42653};
        filterCent = new Filter(a, b);
        filterFreq = new Filter(a, b);

        // "gauss 4 order"
        double[] aP = {0.025363};
        double[] bP = {1.0, -2.403709, 2.166681, -0.868012, 0.130403};
        polyFilter = new ArrayList<>();
        for (int i = 0; i < 6; i++)
            polyFilter.add(new Filter(aP, bP));
    }


    public void start() {
        if (running)
            return;
        running = true;
        thread = new Thread(this, "Audio");
        thread.start();
    }

    @Override
    public void run() {
        processAudio();
    }

    public void stop() {
        if (!running)
            return;

        running = false;
        Thread t = thread;
        thread = null;

        // Wait for the thread to exit
        while (t != null && t.isAlive())
            Thread.yield();
    }

    private void processAudio() {
        Resources resources = context.getResources();
        int[] sampleRates = resources.getIntArray(R.array.sample_rates);

        int size = 0;
        int state = 0;
        for (int sampleRate: sampleRates) {
            // Check sample rate
            size =
                    AudioRecord.getMinBufferSize(sampleRate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);

            // Loop if invalid sample rate
            if (size == AudioRecord.ERROR_BAD_VALUE) {
                continue;
            }

            // Check valid input selected, or other error
            if (size == AudioRecord.ERROR) {
                thread = null;
                return;
            }

            // Create the AudioRecord object
            audioRecord =
                    new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                            sampleRate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            Math.max(size, SIZE));

            // Check state
            state = audioRecord.getState();
            if (state != AudioRecord.STATE_INITIALIZED)
            {
                audioRecord.release();
                continue;
            }

            this.sampleRate = sampleRate;
            break;
        }

        // Check valid sample rate
        if (size == AudioRecord.ERROR_BAD_VALUE) {
            thread = null;
            return;
        }

        // Check AudioRecord initialised
        if (state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release();
            thread = null;
            return;
        }

        // Create buffer for input data
        short[] data = new short[STEP];
        double[] dataDecimate = new double[STEP];

        audioRecord.startRecording();

        while (thread != null)
        {
            size = audioRecord.read(data, 0, STEP);
            if (size == 0) {
                thread = null;
                break;
            }

            double sum = 0;
            for (int i = 0; i < STEP; i++) {
                dataDecimate[i] = data[i] / 32768.0f;
                double v = data[i] / 32768.0f;
                sum += v * v;
            }
            signalRMS = (float) Math.sqrt(sum / STEP);

            ringbuffer.writeToRing(dataDecimate);

            // main Algorithm input - buffer[SAMPLES]  30fps
            // output - mode, chromaticFreq, chromaticNote, chromaticCent, PolyNotes[], PolyCents[]

            ringbuffer.readFromRing(xReal, xImag);
            fft.calcAmpSpectrum(xReal, xImag, amps);

            amps[0] = 0;
            int max = 0;
            int maxF = (int) (MAX_FREQ * SAMPLES / sampleRate);
            for (int i = 1; i < maxF; i++) {
                dx[i] = amps[i] - amps[i-1];
                if (amps[i] > amps[max]) max = i;
            }
            maxLevel = (20.0 * Math.log(amps[max])/Math.log(10.0));

            ///// Polyphonic
            int countPolyNotes = 0;
            for (int i = 0; i < 6; i++) {
                int left = (int) Math.round(reference *
                        Math.pow(2.0, (numberPolyNotes[i] - 0.5) / 12.0) * SAMPLES / sampleRate) - 1;
                int right = (int) Math.round(reference *
                        Math.pow(2.0, (numberPolyNotes[i] + 0.5) / 12.0) * SAMPLES / sampleRate) + 1;

                if (left < 1) left = 1;
                if (right > (SAMPLES/2 - 1)) right = SAMPLES/2 - 1;
                int maxBin = 0;

                for (int j = left; j < right; j++) {
                    if (((dx[j] > 0.0) && (dx[j + 1] < 0.0)) ||
                            ((dx[j] >= 0.0) && (dx[j + 1] < 0.0)) ||
                            ((dx[j] > 0.0) && (dx[j + 1] <= 0.0))) {
                        if (amps[j] > amps[maxBin]) maxBin = j;
                    }
                }

                polyNotes[i] = true;
                if (((20.0 * Math.log(amps[maxBin]) / Math.log(10.0)) <= MIN_POLY_AMPLITUDE)
                        || (maxBin == 0))
                    polyNotes[i] = false;


                if (polyNotes[i]) {
                    double deltaPoly = gaussInterpolation(amps[maxBin - 1], amps[maxBin],
                            amps[maxBin + 1]);
                    double freqPoly = (maxBin + deltaPoly) * sampleRate / SAMPLES;

                    double numberOfNotePoly = 12 * log2(freqPoly / reference);

                    if (!(Double.isNaN(numberOfNotePoly))) {
                        while (numberOfNotePoly < 0) {
                            numberOfNotePoly += 12;
                        }
                    } else {
                        numberOfNotePoly = 0;
                    }

                    numberOfNotePoly = numberOfNotePoly - 12 * (int) (numberOfNotePoly / 12.0);

                    int noteP = (int) Math.round(numberOfNotePoly);
                    if (noteP == 12) noteP = 0;

                    int noteTarget = numberPolyNotes[i];
                    while (noteTarget < 0) noteTarget += 12;
                    while (noteTarget >= 12) noteTarget -= 12;

                    if (noteP == noteTarget) {
                        double centPoly = (numberOfNotePoly - Math.round(numberOfNotePoly)) * 100;
                        polyCents[i] = (float) polyFilter.get(i).filtering(centPoly);
                    } else {
                        polyNotes[i] = false;
                    }
                }
                if (polyNotes[i]) countPolyNotes++;
            }

            ///// Chromatic
            // Finding 3 max
            double maxAmp = (20.0 * Math.log(amps[max]) / Math.log(10.0)) - 30;
            int[] setOfMax = new int[2];
            int count = 0;

            double amp;
            double minFreq, max_Freq;

            minFreq = MIN_FREQ;
            max_Freq = MAX_FREQ;

            for (int i = (int) (minFreq * SAMPLES / sampleRate);
                 i < (int) (max_Freq * SAMPLES / sampleRate); i++) {
                if (count < 2) {
                    amp = 20.0 * Math.log(amps[i]) / Math.log(10.0);
                    if ((((dx[i] > 0.0) && (dx[i + 1] < 0.0)) ||
                            ((dx[i] >= 0.0) && (dx[i + 1] < 0.0)) ||
                            ((dx[i] > 0.0) && (dx[i + 1] <= 0.0))) &&
                            (amp > MIN_AMPLITUDE) && (amp > maxAmp)) {
                        setOfMax[count] = i;
                        count++;
                    }
                } else {
                    break;
                }
            }

            // Finding 2nd harmonic
            int harmonic2 = setOfMax[0];
            float freqDivider = 1;
            double f0 = setOfMax[0];
            double f1 = setOfMax[1];

            if ((setOfMax[0] > 0) && (setOfMax[1] > 0)){
                if ((Math.abs(f1 / f0 - 2.0)) < 0.1) {
                    harmonic2 = setOfMax[1];
                    freqDivider = 2;
                    chromaticFound = true;
                }
                if ((Math.abs(f1 / f0 - 1.5)) < 0.1) {
                    harmonic2 = setOfMax[0];
                    freqDivider = 2;
                    chromaticFound = true;
                }
                if ((Math.abs(f1 / f0 - 1.333)) < 0.1) {
                    harmonic2 = setOfMax[1];
                    freqDivider = 4;
                    chromaticFound = true;
                }
            }

            if (((20.0 * Math.log(amps[harmonic2])/Math.log(10.0)) > MID_AMPLITUDE)
                    && (harmonic2 >= 2)){
                chromaticFound = true;

                double delta = gaussInterpolation(amps[harmonic2 - 1], amps[harmonic2],
                        amps[harmonic2 + 1]);
                double freq = (harmonic2 + delta) * sampleRate / SAMPLES;
                savedChromaNote = (int) Math.round( 12 * log2(freq/reference));

                if (Double.isNaN(savedChromaNote)) {
                    chromaticFound = false;
                    savedChromaNote = 0;
                }
            }

            if (chromaticFound) {
                int lower = (int) Math.round(reference *
                        Math.pow(2.0, (savedChromaNote - 0.5) / 12.0) * SAMPLES / sampleRate) - 1;
                int higher = (int) Math.round(reference *
                        Math.pow(2.0, (savedChromaNote + 0.5) / 12.0) * SAMPLES / sampleRate) + 1;
                max = 0;

                if (lower < 1) lower = 1;
                if (higher > (SAMPLES/2 - 1)) higher = SAMPLES/2 - 1;

                for (int i = lower; i < higher; i++){
                    if (((dx[i] > 0.0) && (dx[i + 1] < 0.0)) ||
                            ((dx[i] >= 0.0) && (dx[i + 1] < 0.0)) ||
                            ((dx[i] > 0.0) && (dx[i + 1] <= 0.0))){
                        if (amps[i] > amps[max]) max = i;
                    }
                }
                harmonic2 = max;

//                maxLevel = (20.0 * Math.log(amps[harmonic2])/Math.log(10.0));

                chromaticFound = ((20.0 * Math.log(amps[harmonic2])/Math.log(10.0)) > MIN_AMPLITUDE)
                        && (harmonic2 != 0);
            }

            if (chromaticFound) {
                double delta = gaussInterpolation(amps[harmonic2 - 1], amps[harmonic2],
                        amps[harmonic2 + 1]);
                double freq = (harmonic2 + delta) * sampleRate / SAMPLES;
                double numberOfNote = 12 * log2(freq/reference);

                if (!(Double.isNaN(numberOfNote))) {
                    while (numberOfNote < 0) {
                        numberOfNote = numberOfNote + 12;
                    }
                }else{
                    numberOfNote = 0;
                }
                numberOfNote = numberOfNote - 12 * (int) (numberOfNote / 12.0);

                chromaticNote = (int) Math.round(numberOfNote);
                if (chromaticNote == 12) chromaticNote = 0;

                double cents = (numberOfNote - Math.round(numberOfNote)) * 100;

                chromaticCent = (float) filterCent.filtering(cents);
                chromaticFreq = (float) filterFreq.filtering(freq) / freqDivider;
            }

            mode = 0;
            if (polyMode) {
                if (countPolyNotes >= 4) mode = 2;
                else if (chromaticFound) mode = 1;
            } else {
                if (chromaticFound) mode = 1;
            }

            audioFPS++;
        }

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
    }

    private double gaussInterpolation(double a, double b, double c) {
        return Math.log(c / a) / (2.0 * Math.log((b * b) / (a * c)));
    }

    private double log2(double d) {
        return Math.log(d) / Math.log(2.0);
    }

}