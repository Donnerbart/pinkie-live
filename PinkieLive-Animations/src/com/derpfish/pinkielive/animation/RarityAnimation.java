package com.derpfish.pinkielive.animation;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.derpfish.pinkielive.PinkiePieLiveWallpaper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RarityAnimation implements PonyAnimation {

    private final Paint mPaint = new Paint();

    private Bitmap bmRarity;

    private float surfaceWidth;
    private float surfaceHeight;

    private float locX;
    private double velocityX;
    private float locY;
    private boolean flipBitmap;

    private boolean completed = true;

    private File resourceDir;

    @Override
    public void initialize(int surfaceWidth, int surfaceHeight, float tapX, float tapY) {
        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;

        locY = tapY;
        if (tapX <= surfaceWidth / 2) {
            flipBitmap = false;
            locX = surfaceWidth;
            velocityX = -surfaceWidth / PinkiePieLiveWallpaper.TIME_FOR_JUMP;
        } else {
            flipBitmap = true;
            locX = 0;
            velocityX = surfaceWidth / PinkiePieLiveWallpaper.TIME_FOR_JUMP;
        }
        completed = false;
    }

    @Override
    public void drawAnimation(Canvas canvas, long elapsedTime) {
        // Decide new position and velocity.
        if (!completed) {
            locX = (int) (velocityX * elapsedTime + locX);

            int ANIMATION_WIDTH = (int) Math.min(surfaceWidth * 0.6, surfaceHeight * 0.6);
            float scale = (float) ANIMATION_WIDTH / (float) bmRarity.getWidth();

            float locY = (int) (this.locY + scale * bmRarity.getHeight() / 2.0f * Math.sin(Math.PI / surfaceWidth * locX));

            if (locX < -ANIMATION_WIDTH || locX > surfaceWidth + ANIMATION_WIDTH) {
                completed = true;
            } else {
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);
                if (flipBitmap) {
                    matrix.postScale(-1.0f, 1.0f);
                }
                matrix.postTranslate(locX, locY - scale * bmRarity.getHeight() / 2.0f);
                canvas.drawBitmap(bmRarity, matrix, mPaint);
            }
        }
    }

    @Override
    public boolean isComplete() {
        return completed;
    }

    @Override
    public void onCreate() {
        completed = true;
        try {
            InputStream inputStream = new FileInputStream(resourceDir.getAbsolutePath() + File.separator + "rarity.png");
            bmRarity = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
        } catch (IOException e) {
            throw new IllegalStateException("Could not load asset");
        }
    }

    @Override
    public void onDestroy() {
        bmRarity.recycle();
    }

    @Override
    public void setResourceDir(File resourceDir) {
        this.resourceDir = resourceDir;
    }
}
