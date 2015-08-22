package com.derpfish.pinkielive;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.derpfish.pinkielive.animation.PinkieAnimation;
import com.derpfish.pinkielive.animation.PonyAnimation;
import com.derpfish.pinkielive.download.PonyAnimationContainer;
import com.derpfish.pinkielive.download.PonyDownloader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PinkiePieLiveWallpaper extends WallpaperService {

    // Round-trip time for a jump to complete in ms.
    public static final double TIME_FOR_JUMP = 1500.0;
    public static final String SHARED_PREFS_NAME = "livewallpapersettings";

    private Bitmap defaultBg;

    // Settings
    private Bitmap selectedBg;
    private boolean useDefaultBg = true;
    private long targetFrameRate = 60L;
    private boolean enableParallax = true;
    private PonyAnimation selectedPony;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if (defaultBg != null) {
            defaultBg.recycle();
            defaultBg = null;
        }
        if (selectedBg != null) {
            selectedBg.recycle();
            selectedBg = null;
        }
        if (selectedPony != null) {
            selectedPony.onDestroy();
            selectedPony = null;
        }

        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new PonyEngine();
    }

    class PonyEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {

        private final Handler mHandler = new Handler();
        private final Paint mPaint = new Paint();
        private final Runnable mDrawPattern = this::drawFrame;

        private final SharedPreferences mPreferences;
        private final BroadcastReceiver broadcastReceiver;

        private int surfaceWidth;
        private int surfaceHeight;
        private float offsetX;
        private float offsetY;

        private boolean mVisible;
        private long lastUpdate;

        private PonyEngine() {
            Paint paint = mPaint;
            paint.setColor(0xffffffff);
            paint.setAntiAlias(true);
            paint.setStrokeWidth(2);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);

            mPreferences = PinkiePieLiveWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
            mPreferences.registerOnSharedPreferenceChangeListener(this);

			/*
             * If the media scanner finishes a scan, reload the preferences
			 * since this means a previously unavailable background is now
			 * available.
			 */
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
            iFilter.addDataScheme("file");

            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                        onSharedPreferenceChanged(mPreferences, null);
                    }
                }
            };
            registerReceiver(broadcastReceiver, iFilter);

            // Load saved preferences
            onSharedPreferenceChanged(mPreferences, null);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            useDefaultBg = prefs.getBoolean("livewallpaper_defaultbg", true);
            if (useDefaultBg) {
                if (selectedBg != null) {
                    selectedBg.recycle();
                    selectedBg = null;
                }
                try {
                    WallpaperManager wmMan = WallpaperManager.getInstance(getApplicationContext());
                    AssetManager assetManager = getAssets();

                    InputStream inputStream = assetManager.open("defaultbg.jpg");
                    int sampleSize = BitmapLoader.getSampleSizeFromInputStream(inputStream,
                            wmMan.getDesiredMinimumWidth(), wmMan.getDesiredMinimumHeight());
                    inputStream.close();
                    inputStream = assetManager.open("defaultbg.jpg");
                    defaultBg = BitmapLoader.decodeSampledBitmapFromInputStream(inputStream, sampleSize);
                    inputStream.close();
                } catch (IOException e) {
                    throw new IllegalStateException("Could not find background image");
                }
            } else {
                if (defaultBg != null) {
                    defaultBg.recycle();
                    defaultBg = null;
                }

                String imageUriStr = prefs.getString("livewallpaper_image", null);
                if (imageUriStr != null) {
                    if (selectedBg != null) {
                        selectedBg.recycle();
                    }

                    Uri bgImage = Uri.parse(imageUriStr);
                    try {
                        ContentResolver contentResolver = getContentResolver();
                        WallpaperManager wmMan = WallpaperManager.getInstance(getApplicationContext());

                        InputStream inputStream = contentResolver.openInputStream(bgImage);
                        int sampleSize = BitmapLoader.getSampleSizeFromInputStream(inputStream,
                                wmMan.getDesiredMinimumWidth(), wmMan.getDesiredMinimumHeight());
                        inputStream.close();

                        inputStream = contentResolver.openInputStream(bgImage);
                        selectedBg = BitmapLoader.decodeSampledBitmapFromInputStream(inputStream, sampleSize);
                        inputStream.close();
                    } catch (IOException e) {
                        selectedBg = null;
                        Log.w("PinkieLive", e);
                    }
                }
            }

            // Frame rate
            String frameRatePref = prefs.getString("livewallpaper_framerate", "60");
            try {
                targetFrameRate = Long.parseLong(frameRatePref);
            } catch (NumberFormatException e) {
                Log.e("PinkieLive", e.getMessage());
            }

            // Parallax
            enableParallax = prefs.getBoolean("livewallpaper_enableparallax", true);
            if (!enableParallax) {
                offsetX = offsetY = 0.0f;
            }

            // Change selected pony
            if (selectedPony != null) {
                selectedPony.onDestroy();
            }
            // Refresh pony animations; preference change could have updated them
            Map<String, PonyAnimation> ponyAnimations = new HashMap<>();
            ponyAnimations.put("pinkie", new PinkieAnimation(getAssets()));
            try {
                for (PonyAnimationContainer container : PonyDownloader.getPonyAnimations(
                        getFilesDir(), getCacheDir(), true)) {
                    ponyAnimations.put(container.getId(), container.getPonyAnimation());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // Set new pony animation
            String selectedPonyId = prefs.getString("livewallpaper_pony", "pinkie");
            selectedPony = ponyAnimations.containsKey(selectedPonyId) ? ponyAnimations.get(selectedPonyId) : ponyAnimations.get("pinkie");
            selectedPony.onCreate();

            drawFrame();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            mHandler.removeCallbacks(mDrawPattern);
            unregisterReceiver(broadcastReceiver);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                drawFrame();
            } else {
                mHandler.removeCallbacks(mDrawPattern);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            surfaceWidth = width;
            surfaceHeight = height;
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawPattern);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
            if (enableParallax) {
                offsetX = xPixels;
                offsetY = yPixels;
                drawFrame();
            }
        }

        /*
         * Store the position of the touch event so we can use it for drawing
         * later
         */
        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // If the length of time pressed was less than 0.5 seconds,
                // trigger a new drawing
                if (event.getEventTime() - event.getDownTime() < 500) {
                    if (selectedPony.isComplete()) {
                        selectedPony.initialize(surfaceWidth, surfaceHeight, event.getX(), event.getY());
                        lastUpdate = SystemClock.elapsedRealtime();
                        drawFrame();
                    }
                }
            }

            super.onTouchEvent(event);
        }

        /*
         * Draw one frame of the animation. This method gets called repeatedly
         * by posting a delayed Runnable. You can do any drawing you want in
         * here. This example draws a wireframe cube.
         */
        private void drawFrame() {
            SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    // Blank canvas
                    Paint paintBlack = new Paint();
                    paintBlack.setColor(0xff000000);
                    c.drawRect(0.0f, 0.0f, surfaceWidth, surfaceHeight, paintBlack);

                    // draw something
                    if (useDefaultBg || selectedBg != null) {
                        Bitmap actualBg = selectedBg != null ? selectedBg : defaultBg;

                        if (enableParallax) {
                            WallpaperManager wmMan = WallpaperManager.getInstance(getApplicationContext());
                            int minWidth = wmMan.getDesiredMinimumWidth();
                            int minHeight = wmMan.getDesiredMinimumHeight();
                            float bgScale = Math.min(((float) actualBg.getWidth()) / ((float) minWidth), ((float) actualBg.getHeight()) / ((float) minHeight));
                            int centeringOffsetX = (int) ((float) actualBg.getWidth() - bgScale * minWidth) / 2;
                            int centeringOffsetY = (int) ((float) actualBg.getHeight() - bgScale * minHeight) / 2;

                            c.drawBitmap(actualBg,
                                    new Rect(centeringOffsetX - (int) (offsetX * bgScale),
                                            centeringOffsetY - (int) (offsetY * bgScale),
                                            centeringOffsetX + (int) ((-offsetX + surfaceWidth) * bgScale),
                                            centeringOffsetY + (int) ((-offsetY + surfaceHeight) * bgScale)),
                                    new Rect(0, 0, surfaceWidth, surfaceHeight), mPaint);
                        } else {
                            float bgScale = Math.min(((float) actualBg.getWidth()) / ((float) surfaceWidth), ((float) actualBg.getHeight()) / ((float) surfaceHeight));
                            int centeringOffsetX = (int) ((float) actualBg.getWidth() - bgScale * surfaceWidth) / 2;
                            int centeringOffsetY = (int) ((float) actualBg.getHeight() - bgScale * surfaceHeight) / 2;
                            c.drawBitmap(actualBg,
                                    new Rect(centeringOffsetX, centeringOffsetY,
                                            centeringOffsetX + (int) (surfaceWidth * bgScale),
                                            centeringOffsetY + (int) (surfaceHeight * bgScale)),
                                    new Rect(0, 0, surfaceWidth, surfaceHeight), mPaint);
                        }

                    }

                    // Decide new position and velocity.
                    if (!selectedPony.isComplete()) {
                        long now = SystemClock.elapsedRealtime();
                        long elapsedTime = now - lastUpdate;
                        lastUpdate = now;
                        selectedPony.drawAnimation(c, elapsedTime);
                    }
                }
            } finally {
                if (c != null) {
                    holder.unlockCanvasAndPost(c);
                }
            }

            mHandler.removeCallbacks(mDrawPattern);
            // Only queue another frame if we're still animating pinkie
            if (mVisible && !selectedPony.isComplete()) {
                mHandler.postDelayed(mDrawPattern, 1000 / targetFrameRate);
            }
        }
    }
}