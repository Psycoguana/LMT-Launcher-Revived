package com.noname81.lmt;

import android.content.Context;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
class TouchServiceNative {
    private static final String TAG = "LMT::TouchServiceNative";
    private static TouchServiceNative instance;
    private RootContext mRootContext;

    native String getServiceVersion();

    native void quit();

    native TouchServiceResult run();

    native int setDebug(int i);

    native int setInputDevice(String str);

    native int setMinPathLength(int i);

    native int setMinScore(int i);

    native int setOrientation(int i, int i2, int i3);

    native int setSingleSwipesBBox(int i);

    native int setSingleTouchGestureSupport(int i);

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

    int setInputDeviceUnlock(String str) {
        this.mRootContext.runCommandRemote("version", false);
        Log.d(TAG, "Lib version " + getServiceVersion());
        Log.d(TAG, "Set input device");
        return setInputDevice(str);
    }
}
