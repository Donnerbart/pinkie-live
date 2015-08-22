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

public class TwilightAnimation implements PonyAnimation {

    private static final long FRAME_DELAY = 50;

    private final Paint mPaint = new Paint();

    private float surfaceWidth;
    private float surfaceHeight;

    private float xPos;
    private float animTargetHeight;
    private boolean flipAnimation;

    private boolean completed = true;
    private long accumulatedTime;

    private Bitmap[] bmBubbles = null;
    private Bitmap[] bmTwilights = null;

    private File resourceDir;

    public TwilightAnimation() {
    }

    @Override
    public void initialize(int surfaceWidth, int surfaceHeight, float tapX, float tapY) {
        this.surfaceWidth = surfaceWidth;
        this.surfaceHeight = surfaceHeight;

        completed = false;
        accumulatedTime = 0L;

        // Target getting middle of image onto tap height
        animTargetHeight = Math.min(surfaceHeight / 2, tapY);

        xPos = tapX;
        // These images happen to already be flipped
        flipAnimation = (tapX <= surfaceWidth / 2.0f);
    }

    @Override
    public void drawAnimation(Canvas canvas, long elapsedTime) {
        int numFrames = bmTwilights.length + 6;
        // Decide new position and velocity.
        if (!completed) {
            accumulatedTime += elapsedTime;
            int currentFrame = (int) (accumulatedTime / FRAME_DELAY);

            double yPos;
            if (currentFrame < 7) {
                yPos = animTargetHeight;
            } else {
                yPos = surfaceHeight + (animTargetHeight - surfaceHeight) * (1 - Math.pow((2.0 * accumulatedTime) / ((double) (numFrames * FRAME_DELAY)) - 1.0, 4.0));
            }

            if (currentFrame < numFrames) {
                Bitmap bmTwilight = (currentFrame < 7 ? bmTwilights[0] : bmTwilights[currentFrame - 6]);
                Bitmap bmBubble = (currentFrame < bmBubbles.length ? bmBubbles[currentFrame] : null);

                for (Object[] frameDef : new Object[][]{{0.75f, bmTwilight}, {2.125f, bmBubble}}) {
                    float relSize = (Float) frameDef[0];
                    Bitmap bitmap = (Bitmap) frameDef[1];
                    if (bitmap == null) {
                        continue;
                    }

                    int ANIM_WIDTH = (int) Math.min(surfaceWidth * relSize, surfaceHeight * relSize);
                    float scale = (float) ANIM_WIDTH / (float) bitmap.getWidth();
                    int ANIM_HEIGHT = (int) (((float) bitmap.getHeight()) * scale);

                    Matrix matrix = new Matrix();
                    if (flipAnimation) {
                        matrix.postScale(-1.0f, 1.0f);
                        matrix.postTranslate(bitmap.getWidth(), 0.0f);
                    }
                    matrix.postScale(scale, scale);
                    matrix.postTranslate(xPos - ANIM_WIDTH / 2, (float) yPos - ANIM_HEIGHT / 2);
                    canvas.drawBitmap(bitmap, matrix, mPaint);
                }
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
        bmBubbles = loadFromZip("bubbles.zip");
        bmTwilights = loadFromZip("twilights.zip");
    }

    private Bitmap[] loadFromZip(String filename) {
        Bitmap[] result;
        try {
            InputStream inputStream = new FileInputStream(resourceDir.getAbsolutePath() + File.separator + filename);
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
            result = new Bitmap[bitmaps.size()];
            for (int i = 0; i < names.size(); i++) {
                result[i] = bitmaps.get(names.get(i));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not load animation: " + filename);
        }
        return result;
    }

    @Override
    public void onDestroy() {
        if (bmBubbles != null) {
            for (Bitmap bmBubble : bmBubbles) {
                bmBubble.recycle();
            }
        }
        if (bmTwilights != null) {
            for (Bitmap bmTwilight : bmTwilights) {
                bmTwilight.recycle();
            }
        }
    }

    @Override
    public void setResourceDir(File resourceDir) {
        this.resourceDir = resourceDir;
    }
}
