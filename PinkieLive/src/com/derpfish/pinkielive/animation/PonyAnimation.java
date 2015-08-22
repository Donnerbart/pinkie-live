package com.derpfish.pinkielive.animation;

import android.graphics.Canvas;

import java.io.File;

public interface PonyAnimation {

    void setResourceDir(final File resourceDir);

    void initialize(int surfaceWidth, int surfaceHeight, float tapX, float tapY);

    void drawAnimation(Canvas canvas, long elapsedTime);

    boolean isComplete();

    void onCreate();

    void onDestroy();
}
