package com.android.camera.vaas;

import android.graphics.ImageFormat;
import android.graphics.Rect;

import com.android.camera.one.v2.camera2proxy.ImageProxy;
import com.google.common.base.Preconditions;

import java.nio.ByteBuffer;
import java.util.List;

public class VAASNative {
    static {
        System.loadLibrary("vaas_jni");
    }

    private native void vaasInit();

    private native void vaasProcess(
            int width, int height,
            Object yBuf, int yPStride, int yRStride,
            Object cbBuf, int cbPStride, int cbRStride,
            Object crBuf, int crPStride, int crRStride,
            int cropLeft, int cropTop, int cropRight, int cropBottom,
            int rot90);

    private native void vaasDeinit();

    public void init() {
        vaasInit();
    }

    public void process(ImageProxy img, Rect crop, int degrees) {
        Preconditions.checkState((degrees % 90) == 0, "Rotation must be a multiple of 90 degrees," +
                " was " + degrees);
        // Handle negative angles by converting to positive.
        degrees = ((degrees % 360) + (360 * 2)) % 360;
        Preconditions.checkState(crop.left < crop.right, "Invalid crop rectangle: " +
                crop.toString());
        Preconditions.checkState(crop.top < crop.bottom, "Invalid crop rectangle: " +
                crop.toString());
        final int NUM_PLANES = 3;
        Preconditions.checkState(img.getFormat() == ImageFormat.YUV_420_888, "Only " +
                "ImageFormat.YUV_420_888 is supported, found " + img.getFormat());
        final List<ImageProxy.Plane> planeList = img.getPlanes();
        Preconditions.checkState(planeList.size() == NUM_PLANES);

        ByteBuffer[] planeBuf = new ByteBuffer[NUM_PLANES];
        int[] pixelStride = new int[NUM_PLANES];
        int[] rowStride = new int[NUM_PLANES];

        for (int i = 0; i < NUM_PLANES; i++) {
            ImageProxy.Plane plane = planeList.get(i);

            Preconditions.checkState(plane.getBuffer().isDirect());

            planeBuf[i] = plane.getBuffer();
            pixelStride[i] = plane.getPixelStride();
            rowStride[i] = plane.getRowStride();
        }

        int cropLeft = crop.left;
        cropLeft = Math.max(cropLeft, 0);
        cropLeft = Math.min(cropLeft, img.getWidth() - 1);

        int cropRight = crop.right;
        cropRight = Math.max(cropRight, 0);
        cropRight = Math.min(cropRight, img.getWidth());

        int cropTop = crop.top;
        cropTop = Math.max(cropTop, 0);
        cropTop = Math.min(cropTop, img.getHeight() - 1);

        int cropBot = crop.bottom;
        cropBot = Math.max(cropBot, 0);
        cropBot = Math.min(cropBot, img.getHeight());

        degrees = degrees % 360;
        // Convert from clockwise to counter-clockwise.
        int rot90 = (360 - degrees) / 90;

        vaasProcess(
                img.getWidth(), img.getHeight(),
                planeBuf[0], pixelStride[0], rowStride[0],
                planeBuf[1], pixelStride[1], rowStride[1],
                planeBuf[2], pixelStride[2], rowStride[2],
                cropLeft, cropTop, cropRight, cropBot,
                rot90);
    }

    public void deinit() {
        vaasDeinit();
    }

    static public VAASNative getInstance() {
        return mVAASNative;
    }

    private static VAASNative mVAASNative = new VAASNative();
}
