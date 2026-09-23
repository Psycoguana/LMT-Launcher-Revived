package com.noname81.lmt;

import android.content.Context;
import android.util.Log;

/**
 * JNI bridge to libTouchServiceNative.so.
 *
 * <p>The native side reads the touchscreen device directly and blocks until it recognises a
 * gesture. Only used when the feature set includes gestures; the pie does not need it. See
 * docs/ROADMAP.md 1.9 — this library is a candidate for deletion.
 */
class TouchServiceNative {
    private static final String TAG = "LMT::TouchServiceNative";
    private static TouchServiceNative instance;
    private RootContext mRootContext;

    native String getServiceVersion();

    native void quit();

    native TouchServiceResult run();

    native int setDebug(int enabled);

    native int setInputDevice(String devicePath);

    native int setMinPathLength(int pixels);

    native int setMinScore(int score);

    native int setOrientation(int rotation, int width, int height);

    native int setSingleSwipesBBox(int pixels);

    native int setSingleTouchGestureSupport(int enabled);

    static {
        System.loadLibrary("TouchServiceNative");
    }

    private TouchServiceNative(Context context) {
        this.mRootContext = RootContext.getInstance(context);
    }

    static TouchServiceNative getInstance(Context context) {
        if (instance == null) {
            instance = new TouchServiceNative(context);
        }
        return instance;
    }

    int setInputDeviceUnlock(String devicePath) {
        this.mRootContext.runCommandRemote("version", false);
        Log.d(TAG, "Lib version " + getServiceVersion());
        Log.d(TAG, "Set input device");
        return setInputDevice(devicePath);
    }
}
