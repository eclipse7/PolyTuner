package com.eclipse7.polytuner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class Display extends View {

	private static final int COLOR_GRAY = Color.rgb(200, 200, 200);
	private static final int COLOR_GREEN = hsvToRgb2(0.4361f, 1f, 0.9f);
	private static final int COLOR_RED = hsvToRgb2(0.995f, 0.75f, 1.0f);
//	private static final int COLOR_RED = Color.rgb(255, 87, 34);  // ff5722

	private final float leds[] = {0.8f, 0.5f, 0.3f, 0.0f, 0.0f, 0.0f, 0.3f, 0.5f, 0.8f, 1.0f};

	private Bitmap bitmap;
	private Bitmap dots;
	private Canvas dotsCanvas;
	private int[][] colorM1;
	private byte[][] fillM1;
	private int[][] colorM2;
	private byte[][] fillM2;
	private float[][][] ledMap1;
	private float[][][] ledMap2;
	private float radiusOfCircle;
	private float radiusOfCircleMid;
	private float radiusOfCircleBig;
	private int width;
	private int height;
	private Paint paint;
	private Path path;

	private double reference = 0;
	private String tuneModeString;

	private int sampleRate;
	private int audioFPS;

	private int mode;
	private float brightness;
	private float brightnessCenterLeft;
	private float brightnessCenterRight;

	private float chromaticFreq = 0;
	private int chromaticNote = 0;
	private int needleCent = 0;
	private boolean strobe = false;
	private float strobePosition = 0;
	private float strobeCent = 0;

	private float polyCents[] = new float[6];
	private float polyBright[] = new float[6];
	private float noSignalBright = 0;

	private float signalRMS = 0;
	private float levelH2 = 0;

	// setters and getters
	public void setLevelH2(float levelH2) {
		this.levelH2 = levelH2;
	}

	public void setAudioFPS(int audioFPS) {
		this.audioFPS = audioFPS;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public void setReference(double reference) {
		this.reference = reference;
	}

	public void setTuneModeString(String tuneModeString) {
		this.tuneModeString = tuneModeString;
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	public void setBrightness(float brightness) {
		this.brightness = brightness;
	}
	public void setBrightnessCenterLeft(float brightnessCenterLeft) {
		this.brightnessCenterLeft = brightnessCenterLeft;
	}
	public void setBrightnessCenterRight(float brightnessCenterRight) {
		this.brightnessCenterRight = brightnessCenterRight;
	}

	public void setChromaticFreq(float chromaticFreq) {
		this.chromaticFreq = chromaticFreq;
	}
	public void setChromaticNote(int chromaticNote) {
		this.chromaticNote = chromaticNote;
	}
	public void setNeedleCent(int needleCent) {
		this.needleCent = needleCent;
	}

	public void setStrobe(boolean strobe) {
		this.strobe = strobe;
	}
	public void setStrobePosition(float strobePosition) {
		this.strobePosition = strobePosition;
	}
	public void setStrobeCent(float strobeCent) {
		this.strobeCent = strobeCent;
	}

	public void setPolyCents(float[] polyCents) {
		this.polyCents = polyCents;
	}
	public void setPolyBright(float[] polyBright) {
		this.polyBright = polyBright;
	}
	public void setNoSignalBright(float noSignalBright) {
		this.noSignalBright = noSignalBright;
	}

	// Constructor
    public Display(Context context, AttributeSet attrs) {
		super(context, attrs);
		paint = new Paint();
		path = new Path();
		ledMap1 = new float[2][5][17];
		colorM1 = new int[5][17];
		fillM1 = new byte[5][17];
		ledMap2 = new float[2][6][4];
		colorM2 = new int[6][4];
		fillM2 = new byte[6][4];
	}

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
		super.onSizeChanged(w, h, oldw, oldh);

		// Save the new width and height
		width = w;
		height = h;

		// Create a bitmap to draw on
		bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas source;
		source = new Canvas(bitmap);

		paint.setAntiAlias(true);
		paint.setStyle(Style.FILL);
		paint.setColor(Color.rgb(48, 48, 48));
		source.drawRect(new RectF(0, 0, width, height), paint);
		dots = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		dotsCanvas = new Canvas(dots);

		if (height >= width) {

			float radius;
			float alpha;
			float deltaR;
			float h1;
			float step;
			float h2;
			float diezXstep;

			radius = width * 1.3f;
			alpha = 2.2f;
			deltaR = width * 0.06f;
			h1 = width * 0.12f;
			step = width * 0.055f;
			h2 = width * 0.13f;
			diezXstep = width * 0.037f;
			radiusOfCircle = width * 0.014f;
			radiusOfCircleMid = width * 0.016f;
			radiusOfCircleBig = width * 0.016f;


			// Set coordinates matrix1
			for(int i = 0; i < 5; i++){
				for(int j = 0; j < 8; j++){
					ledMap1[0][i][j] = (float)((radius + i*deltaR)*Math.cos(-Math.PI/2.0 - Math.PI / 180 * alpha * (8-j)) + width/2.0);
					ledMap1[1][i][j] = (float)((radius + i*deltaR)*Math.sin(-Math.PI/2.0 - Math.PI / 180 * alpha * (8 - j)) + radius + width/2.0 - h1);
					ledMap1[0][i][j+9] = (float)((radius + i*deltaR)*Math.cos(-Math.PI/2.0 + Math.PI / 180 * alpha*(j+1)) + width/2.0);
					ledMap1[1][i][j+9] = (float)((radius + i*deltaR)*Math.sin(-Math.PI/2.0 + Math.PI / 180 * alpha * (j+1)) + radius + width/2.0 - h1);
				}
				ledMap1[0][i][8] = (float)((radius + i*deltaR)*Math.cos(-Math.PI/2.0) + width/2.0);
				ledMap1[1][i][8] = (float)((radius + i*deltaR)*Math.sin(-Math.PI/2.0) + radius + width/2.0 - h1);
			}

			// Set coordinates matrix2
			for(int i = 0; i < 5; i++){
				for(int j = 0; j < 4; j++){
					ledMap2[0][i][j] = (float)(width/2.0 - 1.5*step) + j*step;
					ledMap2[1][i][j] = (float)(width/2.0 + h2 + i*step);
				}
			}
			for(int i = 0; i < 2; i++){
				ledMap2[0][5][i] = (float)(width/2.0 + 2*step + diezXstep) + i*step;
				ledMap2[1][5][i] = (float)(width/2.0 + h2 - step/5.0);
				ledMap2[0][5][i+2] = (float)(width/2.0 + 2*step + diezXstep) + i*step;
				ledMap2[1][5][i+2] = (float)(width/2.0 + h2 - step - step/5.0);
			}
		}
		else {

//			float xOffset = width * 0.07f;
			float xOffset = width * 0.075f;
			float yOffset = height * 0.25f;
//			float xOffset2 = width * 0.8f;
			float xOffset2 = width * 0.77f;
// 			float xStep = width * 0.04f;
//			float yStep	= width * 0.04f;

			float xStep = width * 0.038f;
			float yStep	= width * 0.038f;
//			radiusOfCircle = height * 0.018f;
//			radiusOfCircleMid = height * 0.022f;
//			radiusOfCircleBig = height * 0.022f;

			radiusOfCircle = height * 0.018f;
			radiusOfCircleMid = height * 0.021f;
			radiusOfCircleBig = height * 0.021f;

			// Set coordinates matrix1
			for(int i = 0; i < 5; i++) {
				for (int j = 0; j < 17; j++) {
					ledMap1[0][i][j] =  xOffset + j * xStep;
					ledMap1[1][i][j] =  yOffset + (4 - i) * yStep;
				}
			}

			// Set coordinates matrix2
			for(int i = 0; i < 5; i++) {
				for (int j = 0; j < 4; j++) {
					ledMap2[0][i][j] = xOffset2 + j * xStep;
					ledMap2[1][i][j] = yOffset + i * yStep;
				}
			}
			ledMap2[0][5][0] = xOffset2 + 4.2f * xStep;
			ledMap2[1][5][0] = yOffset + 2 * yStep;


			float s = 0.5f * xStep;
			float x = xOffset + 8 * xStep;
			float y = yOffset - yStep - width * 0.005f;
			float m = (float) (s * Math.sqrt(3.0) / 2);
			float k = m / 3;
			float l = 2 * m / 3;

			path.moveTo(x - s/2.0f, y - k);		// vertex a
			path.lineTo(x + s/2.0f, y - k);		// vertex b
			path.lineTo(x, y + l);				// vertex c
			path.lineTo(x - s/2.0f, y - k);		// vertex a
			path.close();

		}

    }

    @Override
    protected void onDraw(Canvas canvas) {
	//super.onDraw(canvas);

		//clear canvas
		dotsCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

		for(int i = 0; i < 5; i++) {
			for (int j = 0; j < 8; j++) {
				colorM1[i][j] = COLOR_GRAY;
				fillM1[i][j] = 0;
				colorM1[i][j+9] = COLOR_GRAY;
				fillM1[i][j+9] = 0;
			}
			colorM1[i][8] = COLOR_GRAY;
			fillM1[i][8] = 0;
		}
		//set note matrix
		for(int i = 0; i < 6; i++) {
			for (int j = 0; j < 4; j++) {
				if (height >= width) {
					colorM2[i][j] = 0;
				} else {
					//colorM2[i][j] = COLOR_GRAY;
					colorM2[i][j] = 0;
				}
				fillM2[i][j] = 0;
			}
		}

		switch(mode){
			case 1:
				drawChromatic();
				break;
			case 2:
				drawPoly();
				break;
			case 0:
				drawNoSignal(noSignalBright);
				break;
		}



		//paint dots
		for(int i = 0; i < 5; i++) {
			for (int j = 0; j < 8; j++) {

				//if (fillM1[i][j] == 0) {
					paint.setStyle(Style.STROKE);  // STROKE
					paint.setColor(colorM1[i][j]);
					dotsCanvas.drawCircle(ledMap1[0][i][j], ledMap1[1][i][j], radiusOfCircle, paint);
				//}
				if (fillM1[i][j] == 1) {
					paint.setStyle(Style.FILL_AND_STROKE);                    // FILL_AND_STROKE
					paint.setColor(colorM1[i][j]);
					dotsCanvas.drawCircle(ledMap1[0][i][j], ledMap1[1][i][j], radiusOfCircleBig, paint);
				}

				//if (fillM1[i][j+9] == 0) {
					paint.setStyle(Style.STROKE);
					paint.setColor(colorM1[i][j+9]);
					dotsCanvas.drawCircle(ledMap1[0][i][j+9], ledMap1[1][i][j+9], radiusOfCircle, paint);
				//}
				if (fillM1[i][j+9] == 1) {
					paint.setStyle(Style.FILL_AND_STROKE);
					paint.setColor(colorM1[i][j+9]);
					dotsCanvas.drawCircle(ledMap1[0][i][j+9], ledMap1[1][i][j+9], radiusOfCircleBig, paint);
				}
			}

			//if (fillM1[i][8] == 0) {
				paint.setStyle(Style.STROKE);
				paint.setColor(colorM1[i][8]);
				dotsCanvas.drawCircle(ledMap1[0][i][8], ledMap1[1][i][8], radiusOfCircle, paint);
			//}
			if (fillM1[i][8] == 1) {
				paint.setStyle(Style.FILL_AND_STROKE);
				paint.setColor(colorM1[i][8]);
				dotsCanvas.drawCircle(ledMap1[0][i][8], ledMap1[1][i][8], radiusOfCircleBig, paint);
			}
		}

		if (height >= width) {
			for(int i = 0; i < 5; i++) {
				for (int j = 0; j < 4; j++) {
					if (fillM2[i][j] == 0) paint.setStyle(Style.STROKE);
					else paint.setStyle(Style.FILL_AND_STROKE);
					if (colorM2[i][j] != 0) {
						paint.setColor(colorM2[i][j]);
						dotsCanvas.drawCircle(ledMap2[0][i][j], ledMap2[1][i][j], radiusOfCircleMid, paint);
					}
				}
			}
			for(int i = 0; i < 2; i++){
				if (fillM2[5][i] == 0) paint.setStyle(Style.STROKE);
				else paint.setStyle(Style.FILL_AND_STROKE);
				if (colorM2[5][i] != 0) {
					paint.setColor(colorM2[5][i]);
					dotsCanvas.drawCircle(ledMap2[0][5][i], ledMap2[1][5][i], radiusOfCircleMid, paint);
				}

				if (fillM2[5][i+2] == 0) paint.setStyle(Style.STROKE);
				else paint.setStyle(Style.FILL_AND_STROKE);
				if (colorM2[5][i+2] != 0) {
					paint.setColor(colorM2[5][i + 2]);
					dotsCanvas.drawCircle(ledMap2[0][5][i + 2], ledMap2[1][5][i + 2], radiusOfCircleMid, paint);
				}
			}

		} else {
			for(int i = 0; i < 5; i++) {
				for (int j = 0; j < 4; j++) {
					paint.setStyle(Style.STROKE);
					paint.setColor(colorM2[i][j]);
					dotsCanvas.drawCircle(ledMap2[0][i][j], ledMap2[1][i][j], radiusOfCircle, paint);
					if (fillM2[i][j] == 1) {
						paint.setStyle(Style.FILL_AND_STROKE);
						paint.setColor(colorM2[i][j]);
						dotsCanvas.drawCircle(ledMap2[0][i][j], ledMap2[1][i][j], radiusOfCircleMid, paint);
					}
				}
			}

			paint.setStyle(Style.STROKE);
			paint.setColor(colorM2[5][0]);
			dotsCanvas.drawCircle(ledMap2[0][5][0], ledMap2[1][5][0], radiusOfCircle, paint);
			if (fillM2[5][0] == 1) {
				paint.setStyle(Style.FILL_AND_STROKE);
				paint.setColor(colorM2[5][0]);
				dotsCanvas.drawCircle(ledMap2[0][5][0], ledMap2[1][5][0], radiusOfCircleMid, paint);
			}
		}


		// Draw the result on the canvas
		canvas.drawBitmap(bitmap, 0, 0, null);

		paint.setColor(Color.rgb(180, 180, 180));
		paint.setStrokeWidth(1.0f);
		paint.setStyle(Style.FILL);


		if (height >= width) {
			paint.setTextSize(width / 22);

			canvas.drawText( "rms: " + (int) signalRMS + " db", width*0.73f, height*0.05f, paint);
//			canvas.drawText( "H2: " + (int) levelH2 + " db", width*0.73f, height*0.10f, paint);
			canvas.drawText("Tuning " + tuneModeString, width*0.07f, height*0.84f, paint);
			canvas.drawText("Pitch " + (int) reference + " Hz", width*0.07f, height*0.89f, paint);
			canvas.drawText("Cent " + String.format("%.1f", strobeCent), width*0.7f, height*0.84f, paint);
			canvas.drawText("Freq " + String.format("%.1f", chromaticFreq) + " Hz", width*0.7f, height*0.89f, paint);


			// Draw dots on the canvas
			canvas.drawBitmap(dots, 0, height/2.0f - width/1.7f, null);
		} else {
			paint.setTextSize(height / 20);

			canvas.drawText( "rms: " + (int) signalRMS + " db", width*0.85f, height*0.07f, paint);
//			canvas.drawText( "H2: " + (int) levelH2 + " db", width*0.85f, height*0.15f, paint);
			canvas.drawText("Tuning " + tuneModeString, width*0.067f, height*0.81f, paint);
			canvas.drawText("Pitch " + (int) reference + " Hz", width*0.067f, height*0.89f, paint);
			canvas.drawText("Cent " + String.format("%.1f", strobeCent), width*0.77f, height*0.81f, paint);
			canvas.drawText("Freq " + String.format("%.1f", chromaticFreq) + " Hz", width*0.77f, height*0.89f, paint);

			// Draw dots on the canvas
			canvas.drawBitmap(dots, 0, 0, null);

			// Draw green triangle
			paint.setColor(COLOR_GREEN);
			paint.setAntiAlias(true);
			paint.setStyle(Style.FILL_AND_STROKE);

			canvas.drawPath(path, paint);

		}

    }

	private void drawChromatic() {
		setChromaticCent(needleCent, strobePosition, brightness, brightnessCenterLeft, brightnessCenterRight);
		if (chromaticNote >= 0) setNote(chromaticNote, brightness);
		else drawNoSignal(brightness);
	}

	private void drawPoly() {
		setPolyCents(polyCents, polyBright);
		drawNoSignal(noSignalBright);
	}

	private void drawNoSignal(float alpha) {
		int colorRed = COLOR_RED + ((int) (255 * alpha + 1) << 24);
		if (alpha > 0.001f) {
			colorM2[3][1] = colorRed;
			colorM2[3][2] = colorRed;
			fillM2[3][1] = 1;
			fillM2[3][2] = 1;

			colorM2[4][1] = colorRed;
			colorM2[4][2] = colorRed;
			fillM2[4][1] = 1;
			fillM2[4][2] = 1;
		}
	}

	private void setPolyCents(float[] polyCents, float[] polyBright) {
		for(int i = 0; i < 6; i++){
			if (polyBright[i] > 0.001f) {
				float cent = polyCents[i];
				if (cent > 40.0f) cent = 40.0f;
				else if (cent < -40.0f) cent = -40.0f;

				int column = Math.round(cent) / 20 + 2;
				int m = Math.round(Math.abs(cent)) % 20;
				if ((cent >= -1.0f) && ((cent <= 1.0f))) m = 0;

				float v0;
				float v1;

				if (m == 0) {
					v0 = 1.0f;
					v1 = 0;
				} else {
					v0 = 1.0f - 0.02f * m;
					v1 = 0.62f + 0.02f * (m-1);
				}

				if (column == 2) centerPoly(i, v0*polyBright[i]);
				else {
					scalePoly(column, i, v0*polyBright[i]);
				}

				if (cent >= 1.0f) scalePoly(column + 1, i, v1*polyBright[i]);
				else if (cent <= -1.0f) scalePoly(column - 1, i, v1*polyBright[i]);

			}
		}
	}

	private void centerPoly(int numberNote, float alpha) {
		if (alpha > 0.001f){
			int colorGreen = COLOR_GREEN + ((int) (255 * alpha + 1) << 24);
			colorM1[2][numberNote * 3] = colorGreen;
			colorM1[2][numberNote * 3 + 1] = colorGreen;
			fillM1[2][numberNote * 3] = 1;
			fillM1[2][numberNote * 3 + 1] = 1;
		}
	}

	private void scalePoly(int i, int numberNote, float alpha) {
		if (alpha > 0.001f){
			int colorRed = COLOR_RED + ((int) (255 * alpha + 1) << 24);

			colorM1[i][numberNote * 3] = colorRed;
			colorM1[i][numberNote * 3 + 1] = colorRed;
			fillM1[i][numberNote * 3] = 1;
			fillM1[i][numberNote * 3 + 1] = 1;
		}
	}

	private void setChromaticCent(int cent, float strobePosition, float alpha, float alphaCenterLeft, float alphaCenterRight) {
		if (alpha > 0.001f) {
			if (cent > 40) cent = 40;
			else if (cent < -40) cent = -40;

			int column = cent / 5 + 8;
			int m = Math.abs(cent) % 5;

			float v0 = 0;
			float v1 = 0;
			switch (m) {
				case 0:
					v0 = 1;
					v1 = 0;
					break;
				case 1:
					v0 = 0.9f;
					v1 = 0.6f;
					break;
				case 2:
					v0 = 0.8f;
					v1 = 0.7f;
					break;
				case 3:
					v0 = 0.7f;
					v1 = 0.8f;
					break;
				case 4:
					v0 = 0.6f;
					v1 = 0.9f;
					break;
			}

			if (!strobe) {
				if (column == 8) center(v0*alpha);
				else scale(column, v0*alpha);

				if (cent >= 1) scale(column + 1, v1*alpha);
				else if (cent <= -1) scale(column - 1, v1*alpha);

				if (alphaCenterRight > 0.001f) {
					centerRight(alphaCenterRight);
				}
				if (alphaCenterLeft > 0.001f) {
					centerLeft(alphaCenterLeft);
				}
			} else {
				if (column == 8) centerStrobe(v0*alpha);
				else scaleStrobe(column, v0*alpha);

				if (cent >= 1.0f) scaleStrobe(column + 1, v1*alpha);
				else if (cent <= -1.0f) scaleStrobe(column - 1, v1*alpha);

				displayStrobe(strobePosition, alpha);
			}
		}
	}

	private void center(float alpha){
		if (alpha > 0.001f) {
			int colorGreen = COLOR_GREEN + ((int) (255 * alpha + 1) << 24);

			for (int i = 0; i < 5; i++) {
				colorM1[i][8] = colorGreen;
				fillM1[i][8] = 1;
			}
		}
	}

	private void centerLeft(float alpha){
		if (alpha > 0.001f) {
			int colorGreen = COLOR_GREEN;
			for (int i = 0; i < 5; i++) {
				colorM1[2][i] = colorGreen + ((int) (255 * alpha * (1.0f - 0.137f * i) + 1) << 24);
				fillM1[2][i] = 1;
			}
		}
	}

	private void centerRight(float alpha){
		if (alpha > 0.001f) {
			int colorGreen = COLOR_GREEN;
			for(int i = 12; i < 17; i++){
				colorM1[2][i] = colorGreen + ((int) (255 * alpha * (0.451f + 0.137f * (i-12)) + 1) << 24);
				fillM1[2][i] = 1;
			}
		}
	}

	private void scale(int i, float alpha){
		if (alpha > 0.001f){
			int colorGreen = COLOR_GREEN + ((int) (255 * alpha + 1) << 24);
			int colorRed = COLOR_RED + ((int) (255 * alpha + 1) << 24);

			colorM1[0][i] = colorRed;
			colorM1[1][i] = colorRed;
			colorM1[2][i] = colorGreen;
			colorM1[3][i] = colorRed;
			colorM1[4][i] = colorRed;
			for(int j = 0; j < 5; j++) fillM1[j][i] = 1;
		}
	}

	private void centerStrobe(float alpha){
		if (alpha > 0.001f) {
			int colorGreen = COLOR_GREEN + ((int) (255 * alpha + 1) << 24);

			colorM1[0][8] = colorGreen;
			fillM1[0][8] = 1;
			colorM1[4][8] = colorGreen;
			fillM1[4][8] = 1;
		}
	}

	private void scaleStrobe(int i, float alpha){
		if (alpha > 0.001f){
			int colorRed = COLOR_RED + ((int) (255 * alpha + 1) << 24);

			colorM1[0][i] = colorRed;
			fillM1[0][i] = 1;
			colorM1[4][i] = colorRed;
			fillM1[4][i] = 1;
		}
	}

	private void displayStrobe(float position, float alpha) {
		int colorGreen = COLOR_GREEN;

		for (int i = 0; i < 17; i++) {
			float strobeValue = strobeInterpolator(leds, (i + position + 40) % 10);

			if ((strobeValue * alpha) > 0.001f) {
				colorM1[2][i] = colorGreen + ((int) (255 * strobeValue * alpha  + 1) << 24);
				fillM1[2][i] = 1;
			}
		}
	}

	private float strobeInterpolator(float[] leds, float in) {
		float fraction = in - (int) in;
		return leds[(int) in] * (1.0f - fraction) + leds[((int) in + 1) % 10] * fraction;
	}

	private void setNote(int note, float alpha) {
		if (alpha > 0.001f) {
			note = note % 12;
			int colorRed = COLOR_RED + ((int) (255 * alpha + 1) << 24);

			//set A
			if (note == 0){
				colorM2[0][1] = colorRed;
				colorM2[0][2] = colorRed;
				fillM2[0][1] = 1;
				fillM2[0][2] = 1;

				colorM2[1][0] = colorRed;
				colorM2[1][3] = colorRed;
				fillM2[1][0] = 1;
				fillM2[1][3] = 1;

				colorM2[2][0] = colorRed;
				colorM2[2][1] = colorRed;
				colorM2[2][2] = colorRed;
				colorM2[2][3] = colorRed;
				fillM2[2][0] = 1;
				fillM2[2][1] = 1;
				fillM2[2][2] = 1;
				fillM2[2][3] = 1;

				colorM2[3][0] = colorRed;
				colorM2[3][3] = colorRed;
				fillM2[3][0] = 1;
				fillM2[3][3] = 1;

				colorM2[4][0] = colorRed;
				colorM2[4][3] = colorRed;
				fillM2[4][0] = 1;
				fillM2[4][3] = 1;
			}

			//set A#
			if (note == 1){
				colorM2[0][1] = colorRed;
				colorM2[0][2] = colorRed;
				fillM2[0][1] = 1;
				fillM2[0][2] = 1;

				colorM2[1][0] = colorRed;
				colorM2[1][3] = colorRed;
				fillM2[1][0] = 1;
				fillM2[1][3] = 1;

				colorM2[2][0] = colorRed;
				colorM2[2][1] = colorRed;
				colorM2[2][2] = colorRed;
				colorM2[2][3] = colorRed;
				fillM2[2][0] = 1;
				fillM2[2][1] = 1;
				fillM2[2][2] = 1;
				fillM2[2][3] = 1;

				colorM2[3][0] = colorRed;
				colorM2[3][3] = colorRed;
				fillM2[3][0] = 1;
				fillM2[3][3] = 1;

				colorM2[4][0] = colorRed;
				colorM2[4][3] = colorRed;
				fillM2[4][0] = 1;
				fillM2[4][3] = 1;

				for(int i = 0; i < 4; i++){
					colorM2[5][i] = colorRed;
					fillM2[5][i] = 1;
				}
			}

			//set Bright
			if (note == 2){
				colorM2[0][0] = colorRed;
				colorM2[0][1] = colorRed;
				colorM2[0][2] = colorRed;
				fillM2[0][0] = 1;
				fillM2[0][1] = 1;
				fillM2[0][2] = 1;

				colorM2[1][0] = colorRed;
				colorM2[1][3] = colorRed;
				fillM2[1][0] = 1;
				fillM2[1][3] = 1;

				colorM2[2][0] = colorRed;
				colorM2[2][1] = colorRed;
				colorM2[2][2] = colorRed;
				fillM2[2][0] = 1;
				fillM2[2][1] = 1;
				fillM2[2][2] = 1;

				colorM2[3][0] = colorRed;
				colorM2[3][3] = colorRed;
				fillM2[3][0] = 1;
				fillM2[3][3] = 1;

				colorM2[4][0] = colorRed;
				colorM2[4][1] = colorRed;
				colorM2[4][2] = colorRed;
				fillM2[4][0] = 1;
				fillM2[4][1] = 1;
				fillM2[4][2] = 1;
			}

			//set C
			if (note == 3){
				colorM2[0][1] = colorRed;
				colorM2[0][2] = colorRed;
				colorM2[0][3] = colorRed;
				fillM2[0][1] = 1;
				fillM2[0][2] = 1;
				fillM2[0][3] = 1;

				colorM2[1][0] = colorRed;
				fillM2[1][0] = 1;

				colorM2[2][0] = colorRed;
				fillM2[2][0] = 1;

				colorM2[3][0] = colorRed;
				fillM2[3][0] = 1;

				colorM2[4][1] = colorRed;
				colorM2[4][2] = colorRed;
				colorM2[4][3] = colorRed;
				fillM2[4][1] = 1;
				fillM2[4][2] = 1;
				fillM2[4][3] = 1;

			}

			//set C#
			if (note == 4){
				colorM2[0][1] = colorRed;
				colorM2[0][2] = colorRed;
				colorM2[0][3] = colorRed;
				fillM2[0][1] = 1;
				fillM2[0][2] = 1;
				fillM2[0][3] = 1;

				colorM2[1][0] = colorRed;
				fillM2[1][0] = 1;

				colorM2[2][0] = colorRed;
				fillM2[2][0] = 1;

				colorM2[3][0] = colorRed;
				fillM2[3][0] = 1;

				colorM2[4][1] = colorRed;
				colorM2[4][2] = colorRed;
				colorM2[4][3] = colorRed;
				fillM2[4][1] = 1;
				fillM2[4][2] = 1;
				fillM2[4][3] = 1;

				for(int i = 0; i < 4; i++){
					colorM2[5][i] = colorRed;
					fillM2[5][i] = 1;
				}
			}

			//set D
			if (note == 5){
				colorM2[0][0] = colorRed;
				colorM2[0][1] = colorRed;
				colorM2[0][2] = colorRed;
				fillM2[0][0] = 1;
				fillM2[0][1] = 1;
				fillM2[0][2] = 1;

				colorM2[1][0] = colorRed;
				colorM2[1][3] = colorRed;
				fillM2[1][0] = 1;
				fillM2[1][3] = 1;

				colorM2[2][0] = colorRed;
				colorM2[2][3] = colorRed;
				fillM2[2][0] = 1;
				fillM2[2][3] = 1;

				colorM2[3][0] = colorRed;
				colorM2[3][3] = colorRed;
				fillM2[3][0] = 1;
				fillM2[3][3] = 1;

				colorM2[4][0] = colorRed;
				colorM2[4][1] = colorRed;
				colorM2[4][2] = colorRed;
				fillM2[4][0] = 1;
				fillM2[4][1] = 1;
				fillM2[4][2] = 1;
			}

			//set D#
			if (note == 6){
				colorM2[0][0] = colorRed;
				colorM2[0][1] = colorRed;
				colorM2[0][2] = colorRed;
				fillM2[0][0] = 1;
				fillM2[0][1] = 1;
				fillM2[0][2] = 1;

				colorM2[1][0] = colorRed;
				colorM2[1][3] = colorRed;
				fillM2[1][0] = 1;
				fillM2[1][3] = 1;

				colorM2[2][0] = colorRed;
				colorM2[2][3] = colorRed;
				fillM2[2][0] = 1;
				fillM2[2][3] = 1;

				colorM2[3][0] = colorRed;
				colorM2[3][3] = colorRed;
				fillM2[3][0] = 1;
				fillM2[3][3] = 1;

				colorM2[4][0] = colorRed;
				colorM2[4][1] = colorRed;
				colorM2[4][2] = colorRed;
				fillM2[4][0] = 1;
				fillM2[4][1] = 1;
				fillM2[4][2] = 1;

				for(int i = 0; i < 4; i++){
					colorM2[5][i] = colorRed;
					fillM2[5][i] = 1;
				}
			}

			//set E
			if (note == 7){
				colorM2[0][0] = colorRed;
				colorM2[0][1] = colorRed;
				colorM2[0][2] = colorRed;
				colorM2[0][3] = colorRed;
				fillM2[0][0] = 1;
				fillM2[0][1] = 1;
				fillM2[0][2] = 1;
				fillM2[0][3] = 1;

				colorM2[1][0] = colorRed;
				fillM2[1][0] = 1;

				colorM2[2][0] = colorRed;
				colorM2[2][1] = colorRed;
				colorM2[2][2] = colorRed;
				colorM2[2][3] = colorRed;
				fillM2[2][0] = 1;
				fillM2[2][1] = 1;
				fillM2[2][2] = 1;
				fillM2[2][3] = 1;

				colorM2[3][0] = colorRed;
				fillM2[3][0] = 1;

				colorM2[4][0] = colorRed;
				colorM2[4][1] = colorRed;
				colorM2[4][2] = colorRed;
				colorM2[4][3] = colorRed;
				fillM2[4][0] = 1;
				fillM2[4][1] = 1;
				fillM2[4][2] = 1;
				fillM2[4][3] = 1;
			}

			//set F
			if (note == 8){
				colorM2[0][0] = colorRed;
				colorM2[0][1] = colorRed;
				colorM2[0][2] = colorRed;
				colorM2[0][3] = colorRed;
				fillM2[0][0] = 1;
				fillM2[0][1] = 1;
				fillM2[0][2] = 1;
				fillM2[0][3] = 1;

				colorM2[1][0] = colorRed;
				fillM2[1][0] = 1;

				colorM2[2][0] = colorRed;
				colorM2[2][1] = colorRed;
				colorM2[2][2] = colorRed;
				colorM2[2][3] = colorRed;
				fillM2[2][0] = 1;
				fillM2[2][1] = 1;
				fillM2[2][2] = 1;
				fillM2[2][3] = 1;

				colorM2[3][0] = colorRed;
				fillM2[3][0] = 1;

				colorM2[4][0] = colorRed;
				fillM2[4][0] = 1;
			}

			//set F#
			if (note == 9){
				colorM2[0][0] = colorRed;
				colorM2[0][1] = colorRed;
				colorM2[0][2] = colorRed;
				colorM2[0][3] = colorRed;
				fillM2[0][0] = 1;
				fillM2[0][1] = 1;
				fillM2[0][2] = 1;
				fillM2[0][3] = 1;

				colorM2[1][0] = colorRed;
				fillM2[1][0] = 1;

				colorM2[2][0] = colorRed;
				colorM2[2][1] = colorRed;
				colorM2[2][2] = colorRed;
				colorM2[2][3] = colorRed;
				fillM2[2][0] = 1;
				fillM2[2][1] = 1;
				fillM2[2][2] = 1;
				fillM2[2][3] = 1;

				colorM2[3][0] = colorRed;
				fillM2[3][0] = 1;

				colorM2[4][0] = colorRed;
				fillM2[4][0] = 1;

				for(int i = 0; i < 4; i++){
					colorM2[5][i] = colorRed;
					fillM2[5][i] = 1;
				}
			}

			//set G
			if (note == 10){
				colorM2[0][1] = colorRed;
				colorM2[0][2] = colorRed;
				colorM2[0][3] = colorRed;
				fillM2[0][1] = 1;
				fillM2[0][2] = 1;
				fillM2[0][3] = 1;

				colorM2[1][0] = colorRed;
				fillM2[1][0] = 1;

				colorM2[2][0] = colorRed;
				colorM2[2][2] = colorRed;
				colorM2[2][3] = colorRed;
				fillM2[2][0] = 1;
				fillM2[2][2] = 1;
				fillM2[2][3] = 1;

				colorM2[3][0] = colorRed;
				colorM2[3][3] = colorRed;
				fillM2[3][0] = 1;
				fillM2[3][3] = 1;

				colorM2[4][1] = colorRed;
				colorM2[4][2] = colorRed;
				colorM2[4][3] = colorRed;
				fillM2[4][1] = 1;
				fillM2[4][2] = 1;
				fillM2[4][3] = 1;
			}

			//set G#
			if (note == 11){
				colorM2[0][1] = colorRed;
				colorM2[0][2] = colorRed;
				colorM2[0][3] = colorRed;
				fillM2[0][1] = 1;
				fillM2[0][2] = 1;
				fillM2[0][3] = 1;

				colorM2[1][0] = colorRed;
				fillM2[1][0] = 1;

				colorM2[2][0] = colorRed;
				colorM2[2][2] = colorRed;
				colorM2[2][3] = colorRed;
				fillM2[2][0] = 1;
				fillM2[2][2] = 1;
				fillM2[2][3] = 1;

				colorM2[3][0] = colorRed;
				colorM2[3][3] = colorRed;
				fillM2[3][0] = 1;
				fillM2[3][3] = 1;

				colorM2[4][1] = colorRed;
				colorM2[4][2] = colorRed;
				colorM2[4][3] = colorRed;
				fillM2[4][1] = 1;
				fillM2[4][2] = 1;
				fillM2[4][3] = 1;

				for(int i = 0; i < 4; i++){
					colorM2[5][i] = colorRed;
					fillM2[5][i] = 1;
				}
			}
		}
	}

	private static int hsvToRgb2(float h, float s, float v) {

		if (h > 1) v = 1;
		if (h < 0) v = 0;
		if (s > 1) s = 1;
		if (s < 0) s = 0;
		if (v > 1) s = 1;
		if (v < 0) s = 0;

		double r = 0, g = 0, b = 0;
		double i = (int) Math.floor(h * 6);
		double f = h * 6 - i;
		double p = v * (1 - s);
		double q = v * (1 - f * s);
		double t = v * (1 - (1 - f) * s);
		switch ((int) i % 6) {
			case 0:
				r = v;
				g = t;
				b = p;
				break;
			case 1:
				r = q;
				g = v;
				b = p;
				break;
			case 2:
				r = p;
				g = v;
				b = t;
				break;
			case 3:
				r = p;
				g = q;
				b = v;
				break;
			case 4:
				r = t;
				g = p;
				b = v;
				break;
			case 5:
				r = v;
				g = p;
				b = q;
				break;
		}
		return Color.rgb((int) (r * 255), (int) (g * 255), (int) (b * 255));
	}

	public void setSignalRMS(float signalRMS) {
		this.signalRMS = signalRMS;
	}

}
