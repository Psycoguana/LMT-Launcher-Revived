package com.noname81.lmt;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Stands in for the native gesture recognizer on the JVM: skips {@code System.loadLibrary}
 * (there is no host build of libTouchServiceNative.so) and makes every native call a no-op.
 * Used by UI tests that construct SettingsViewHelper, which grabs the recognizer eagerly.
 */
@Implements(TouchServiceNative.class)
public class ShadowTouchServiceNative {

    @Implementation
    protected static void __staticInitializer__() {}

    @Implementation
    protected String getServiceVersion() {
        return "test";
    }

    @Implementation
    protected void quit() {}

    @Implementation
    protected TouchServiceResult run() {
        return null;
    }

    @Implementation
    protected int setDebug(int enabled) {
        return 0;
    }

    @Implementation
    protected int setInputDevice(String devicePath) {
        return 0;
    }

    @Implementation
    protected int setMinPathLength(int pixels) {
        return 0;
    }

    @Implementation
    protected int setMinScore(int score) {
        return 0;
    }

    @Implementation
    protected int setOrientation(int rotation, int width, int height) {
        return 0;
    }

    @Implementation
    protected int setSingleSwipesBBox(int pixels) {
        return 0;
    }

    @Implementation
    protected int setSingleTouchGestureSupport(int enabled) {
        return 0;
    }
}
