package com.derpfish.pinkielive;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;

class BitmapLoader {

    public static Bitmap decodeSampledBitmapFromInputStream(InputStream inputStream, int sampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeStream(inputStream, null, options);
    }

    public static int getSampleSizeFromInputStream(InputStream inputStream, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);

        // Calculate inSampleSize
        return calculateInSampleSize(options, reqWidth, reqHeight);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        int height = options.outHeight;
        int width = options.outWidth;

        return Math.max(1, Math.max(
                Math.round((float) height / (float) reqHeight),
                Math.round((float) width / (float) reqWidth)));
    }
}
