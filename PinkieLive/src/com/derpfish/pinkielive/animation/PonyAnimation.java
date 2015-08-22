package com.derpfish.pinkielive.animation;

import android.graphics.Canvas;

import java.io.File;

public interface PonyAnimation {
    public void setResourceDir(final File resourceDir);

    public void initialize(int surfaceWidth, int surfaceHeight, float tapX, float tapY);

    public void drawAnimation(Canvas canvas, long elapsedTime);

    public boolean isComplete();

    public void onCreate();

    public void onDestroy();

}
