package com.eclipse7.polytuner;

import android.content.Context;

import java.util.Arrays;

import com.eclipse7.polytuner.core.Audio;
import com.eclipse7.polytuner.utils.Bright;
import com.eclipse7.polytuner.utils.Time;
import com.eclipse7.polytuner.core.CategoryFilter;

public class Tuner implements Runnable{

    private static final float UPDATE_RATE = 30.0f;
    private static final float UPDATE_INTERVAL = Time.SECOND / UPDATE_RATE;
    private static final long IDLE_TIME = 20;
    private static final float RISE_SPEED = 0.3f;
    private static final float FALL_SPEED = 0.3f;
    private static final float RISE_SLOW_SPEED = 0.05f;
    private static final float FALL_SLOW_SPEED = 0.07f;

    private Thread tunerThread;
    private boolean running;

    private Audio audio;
    private Display display;

    private float signal = 0;
    private byte modePrev = 0;
    private float strobePosition = 0;

    private boolean flagToRenderD = true;

    // Preferences
    private double reference;
    private String tuneModeString;

    // Output data
    private int fpsOut;
    private int updOut;
    private byte mode;
    private int samplerate;
    private int audioFPS;

    private float brightness;
    private float brightnessCenterLeft;
    private float brightnessCenterRight;
    private float chromaticFreq = 0;
    private int chromaticNote = 0;
    private int needleCent = 0;
    private boolean strobe = false;
    private float strobeCent = 0;

    private boolean[] polyNotes = new boolean[6];
    private float[] polyCents = new float[6];
    private float[] polyBright = new float[6];
    private float noSignalBright;
    private CategoryFilter categoryFilter;

    public Audio getAudio() {
        return audio;
    }

    public void setReference(double reference) {
        this.reference = reference;
    }

    public void setTuneModeString(String tuneModeString) {
        this.tuneModeString = tuneModeString;
    }

    public void setStrobe(boolean strobe) {
        this.strobe = strobe;
    }

    // Constructor
    public Tuner(Context context, Display display) {
        audio = new Audio(context);
        this.display = display;
        running = false;
        categoryFilter = new CategoryFilter();
    }

    public void start() {
        if (running)
            return;
        audio.start();
        running = true;
        tunerThread = new Thread(this);
        tunerThread.start();

    }

    public void stop() {
        if (!running)
            return;
        audio.stop();
        running = false;
        Thread t = tunerThread;
        tunerThread = null;
        while (t != null && t.isAlive())
            Thread.yield();
    }


    private void update() {
        if (audio != null) {
            float newSignal = audio.getSignalRMS();
            signal = ((signal * 9.0f) + newSignal) / 10.0f;

            polyNotes = Arrays.copyOf(audio.getPolyNotes(), audio.getPolyNotes().length);
            polyCents = Arrays.copyOf(audio.getPolyCents(), audio.getPolyCents().length);

            samplerate = audio.getSampleRate();
            chromaticNote = audio.getChromaticNote();
            chromaticFreq = audio.getChromaticFreq();

            float chromaticCent = audio.getChromaticCent();

            // NEEDLE
            int cent;
            if (Math.abs(chromaticCent) <= 1) cent = 0;
            else cent = Math.round(chromaticCent);
            needleCent = cent;

            //// STROBE +/-0.1 cent
            strobeCent = chromaticCent;
            switch (((int)(strobeCent * 10)) % 2){
                case -1: strobeCent = ((int)(strobeCent * 10) - 1) / 10.0f;
                    break;
                case 1: strobeCent = ((int)(strobeCent * 10) + 1) / 10.0f;
                    break;
                case 0: strobeCent = ((int)(strobeCent * 10)) / 10.0f;
                    break;
            }
            float speed = strobeCent / 20.0f;
            if (speed >= 1) speed = 1;
            if (speed <= -1) speed = -1;
            strobePosition -= speed;
            strobePosition = strobePosition % 10;

            // brightness
            byte modeNew = audio.getMode();

            if ((modeNew == 0) && ((Math.abs(noSignalBright - 1.0f)) < 0.01f)) {
                flagToRenderD = false;
                flagToRenderD = true;
            } else {
                flagToRenderD = true;
            }

            switch (modePrev){
                case 1:{
                    if (modePrev == modeNew){
                        brightness = Bright.up(brightness, RISE_SPEED);
                        if (needleCent == 0) {
                            brightnessCenterLeft =  Bright.up(brightnessCenterLeft,
                                    RISE_SLOW_SPEED);
                            brightnessCenterRight =  Bright.up(brightnessCenterRight,
                                    RISE_SLOW_SPEED);
                        }
                        if ((needleCent > 0) && (needleCent <= 2)) {
                            brightnessCenterLeft =  Bright.down(brightnessCenterLeft,
                                    FALL_SLOW_SPEED);
                            brightnessCenterRight =  Bright.up(brightnessCenterRight,
                                    RISE_SLOW_SPEED);
                        }
                        if ((needleCent >= -2) && (needleCent < 0)) {
                            brightnessCenterLeft =  Bright.up(brightnessCenterLeft,
                                    RISE_SLOW_SPEED);
                            brightnessCenterRight =  Bright.down(brightnessCenterRight,
                                    FALL_SLOW_SPEED);
                        }
                        if ((needleCent < -2) || (needleCent > 2)) {
                            brightnessCenterLeft =  Bright.down(brightnessCenterLeft,
                                    FALL_SLOW_SPEED);
                            brightnessCenterRight =  Bright.down(brightnessCenterRight,
                                    FALL_SLOW_SPEED);
                        }
                    } else {
                        if (brightness <= 0.001f) {
                            modePrev = modeNew;
                        }
                        else {
                            brightness = Bright.down(brightness, FALL_SPEED);
                            brightnessCenterLeft =  Bright.down(brightnessCenterLeft, FALL_SPEED);
                            brightnessCenterRight =  Bright.down(brightnessCenterRight, FALL_SPEED);
                        }
                    }
                    break;
                }

                case 2:{
                    if (modePrev == modeNew){
                        for(int i = 0; i < 6; i++) {
                            if (polyNotes[i]) polyBright[i] = Bright.up(polyBright[i], RISE_SPEED);
                            else polyBright[i] = Bright.down(polyBright[i], FALL_SPEED);
                        }
                    } else {
                        float maxPolyBright = 0;
                        for(int i = 0; i < 6; i++) {
                            if (polyBright[i] >= maxPolyBright) maxPolyBright = polyBright[i];
                        }
                        if (maxPolyBright <= 0.001f) {
                            modePrev = modeNew;
                        } else {
                            for(int i = 0; i < 6; i++) {
                                polyBright[i] = Bright.down(polyBright[i], FALL_SPEED);
                            }
                        }
                    }
                    break;
                }

                case 0:{
                    if (modePrev == modeNew){
                        noSignalBright = Bright.up(noSignalBright, RISE_SPEED);
                    }else {
                        if (noSignalBright <= 0.001f) {
                            modePrev = modeNew;
                        } else {
                            noSignalBright = Bright.down(noSignalBright, FALL_SPEED);
                        }
                    }
                    break;
                }

            }
            mode = modePrev;
        }

        if (display != null){
            display.setAudioFPS(audioFPS);

            display.setSampleRate(samplerate);
            display.setReference(reference);
            display.setTuneModeString(tuneModeString);

            display.setChromaticFreq(chromaticFreq);
            chromaticNote = categoryFilter.filtering(chromaticNote);
            display.setChromaticNote(chromaticNote);
            display.setStrobeCent(strobeCent);

            float v = (float)(20.0 * Math.log(signal) / Math.log(10.0));
            display.setSignalRMS(v);
            display.setLevelH2(audio.getMaxLevel());

            display.setMode(mode);
            display.setBrightness(brightness);
            display.setBrightnessCenterLeft(brightnessCenterLeft);
            display.setBrightnessCenterRight(brightnessCenterRight);
            display.setNeedleCent(needleCent);
            display.setStrobe(strobe);
            display.setStrobePosition(strobePosition);
            display.setPolyCents(polyCents);
            display.setPolyBright(polyBright);
            display.setNoSignalBright(noSignalBright);
        }
    }

    private void render(){
        if (flagToRenderD) {
            if (display != null) display.postInvalidate();
        }
    }

    @Override
    public void run() {
        int upd = 0;
        int updl = 0;
        long count = 0;
        float delta = 0;

        long lastTime = Time.getTime();
        while(running) {
            long nowTime = Time.getTime();
            long elapsedTime = nowTime - lastTime;
            lastTime = nowTime;

            count += elapsedTime;

            boolean render = false;
            delta += elapsedTime / UPDATE_INTERVAL;
            while (delta >= 1){
                update();
                upd++;
                delta--;
                if (render) {
                    updl++;
                } else {
                    render = true;
                }
            }

            if (render) {
                render();
            } else {
                try {
                    Thread.sleep(IDLE_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (count >= Time.SECOND) {
                if (audio != null) {
                    audioFPS = audio.getAudioFPS();
                    audio.setAudioFPS(0);
                }
                else fpsOut = 0;
                updOut = upd;
                upd = 0;
                updl = 0;
                count = 0;
            }
        }
    }
}
