package com.derpfish.pinkielive.animation;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LyraAnimation implements PonyAnimation {

    private static final long FRAME_DELAY = 50;

    private final Paint mPaint = new Paint();

    private float surfaceWidth;
    private float surfaceHeight;

    private float xPos;
    private float animTargetHeight;
    private boolean flipAnimation;

    private boolean completed = true;
    private long accumulatedTime;

    private Bitmap[] bmAnimation = null;

    private File resourceDir;

    public LyraAnimation() {
    }

    @Override
    public void initialize(int surfaceWidth, int surfaceHeight, float tapX, float tapY) {
        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;

        completed = false;
        accumulatedTime = 0L;

        // Target getting 2/3 of the way up the image to this position
        int ANIM_WIDTH = (int) Math.min(surfaceWidth * 0.75, surfaceHeight * 0.75);
        float scale = (float) ANIM_WIDTH / (float) bmAnimation[0].getWidth();
        animTargetHeight = Math.min(surfaceHeight / 2, tapY) - scale * (bmAnimation[0].getHeight() / 3.0f);

        xPos = tapX;
        flipAnimation = (tapX > surfaceWidth / 2.0f);
    }

    @Override
    public void drawAnimation(Canvas canvas, long elapsedTime) {
        // Decide new position and velocity.
        if (!completed) {
            accumulatedTime += elapsedTime;
            int currentFrame = (int) (accumulatedTime / FRAME_DELAY);

            double yPos = surfaceHeight + (animTargetHeight - surfaceHeight) * (1 - Math.pow((2.0 * accumulatedTime) / ((double) (bmAnimation.length * FRAME_DELAY)) - 1.0, 4.0));

            if (currentFrame < bmAnimation.length && yPos <= surfaceHeight) {
                Bitmap bitmap = bmAnimation[currentFrame];
                int ANIM_WIDTH = (int) Math.min(surfaceWidth * 0.75, surfaceHeight * 0.75);
                float scale = (float) ANIM_WIDTH / (float) bitmap.getWidth();

                Matrix matrix = new Matrix();
                if (flipAnimation) {
                    matrix.postScale(-1.0f, 1.0f);
                    matrix.postTranslate(bitmap.getWidth(), 0.0f);
                }
                matrix.postScale(scale, scale);
                matrix.postTranslate(xPos - ANIM_WIDTH / 2, (float) yPos);
                canvas.drawBitmap(bitmap, matrix, mPaint);
            } else {
                completed = true;
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
            InputStream inputStream = new FileInputStream(resourceDir.getAbsolutePath() + File.separator + "lyra.zip");
            ZipInputStream zis = new ZipInputStream(inputStream);
            Map<String, Bitmap> bitmaps = new HashMap<>();
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                bitmaps.put(zipEntry.getName(), BitmapFactory.decodeStream(zis));
            }
            zis.close();
            inputStream.close();

            List<String> names = new ArrayList<>(bitmaps.keySet());
            Collections.sort(names);
            bmAnimation = new Bitmap[bitmaps.size()];
            for (int i = 0; i < names.size(); i++) {
                bmAnimation[i] = bitmaps.get(names.get(i));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not load animation: lyra.zip");
        }
    }

    @Override
    public void onDestroy() {
        if (bmAnimation != null) {
            for (Bitmap aBmAnimation : bmAnimation) {
                aBmAnimation.recycle();
            }
        }
    }

    @Override
    public void setResourceDir(File resourceDir) {
        this.resourceDir = resourceDir;
    }
}
