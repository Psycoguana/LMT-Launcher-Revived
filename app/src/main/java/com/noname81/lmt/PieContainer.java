package com.noname81.lmt;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.Collections;
import java.util.List;

/**
 * Owns the overlay windows the pie lives in.
 *
 * <p>Each configured edge gets a thin, invisible trigger strip added to the WindowManager. Touching
 * one expands that strip to full screen so {@link PieControl} can draw; releasing shrinks it back.
 * Keeping the strips tiny is what stops LMT swallowing touches meant for the app underneath.
 */
public class PieContainer {
    private static boolean DEBUG = false;
    private static final String TAG = "LMT::PieContainer";
    private boolean mAddedLayout1 = false;
    private boolean mAddedLayout2 = false;
    private boolean mAddedLayout3 = false;
    private final PieControl mPieControl;
    private final PieLayout mPieLayout1;
    private final PieLayout mPieLayout2;
    private final PieLayout mPieLayout3;
    private final SettingsValues mSettings;
    private final WindowManager mWindowManager;

    /**
     * The trigger strips are accessibility overlays when LMT's accessibility service is on. An app
     * overlay (TYPE_APPLICATION_OVERLAY) marks every touch in the app below as partially obscured,
     * and apps with tapjacking protection refuse those touches: Chrome's site permission prompt
     * answers "close bubbles or overlays of other apps". An accessibility overlay is a trusted
     * overlay, which the input system doesn't count (ROADMAP 3.8).
     */
    private final boolean mTrusted;

    /**
     * Accessibility overlays also show above the lock screen, which app overlays never did. So
     * while the phone is locked and "pie on lock screen" is off, the strips stop taking touches and
     * swipes reach the lock screen (ROADMAP 3.8).
     */
    private final Context mContext;

    private android.content.BroadcastReceiver mLockReceiver;

    private int getWindowFlags(int position) {
        return (position > 0 ? 131072 : 32) | 32 | 8;
    }

    static void setDebug(boolean enabled) {
        DEBUG = enabled;
    }

    class PieLayout extends FrameLayout {
        boolean mAllowed;
        boolean mBottom;
        boolean mPieControlAdded;
        boolean mShrinking;

        public PieLayout(Context context, boolean expanded) {
            super(context);
            setWillNotDraw(true);
            setEnabled(false);
            this.mPieControlAdded = false;
            this.mShrinking = false;
            this.mBottom = expanded;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent motionEvent) {
            if (PieContainer.DEBUG) {
                Log.d(PieContainer.TAG, "dispatchTouchEvent " + motionEvent);
            }
            if (motionEvent.getToolType(0) == 0) {
                return false;
            }
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == MotionEvent.ACTION_DOWN) {
                boolean zIsNotBlacklistedPie = PieContainer.this.mSettings.isNotBlacklistedPie();
                this.mAllowed = zIsNotBlacklistedPie;
                if (zIsNotBlacklistedPie) {
                    expandLayout();
                }
            } else if (actionMasked == MotionEvent.ACTION_UP) {
                shrinkLayout();
            }
            if (!this.mAllowed) {
                return false;
            }
            PieContainer.this.mPieControl.mPie.onTouchEvent(motionEvent);
            return true;
        }

        @Override
        public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            if (PieContainer.DEBUG) {
                Log.d(
                        PieContainer.TAG,
                        "onSizeChanged new:"
                                + width
                                + ","
                                + height
                                + " old:"
                                + oldWidth
                                + ","
                                + oldHeight);
            }
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (this.mShrinking) {
                PieContainer.this.mPieControl.mPie.onShrink();
                this.mShrinking = false;
            }
            if (oldWidth == 0 && oldHeight == 0 && Build.VERSION.SDK_INT >= 29) {
                setExclusionRects();
            }
        }

        private void setExclusionRects() {
            Rect rect = new Rect();
            List listSingletonList = Collections.singletonList(rect);
            rect.set(getLeft(), getTop(), getRight(), getBottom());
            if (PieContainer.DEBUG) {
                Log.d(PieContainer.TAG, "setExclusionRects:" + rect);
            }
            setSystemGestureExclusionRects(listSingletonList);
        }

        public void expandLayout() {
            if (PieContainer.DEBUG) {
                Log.d(PieContainer.TAG, "expandLayout");
            }
            WindowManager.LayoutParams layoutParams =
                    (WindowManager.LayoutParams) getLayoutParams();
            if (layoutParams != null
                    && PieContainer.this.mSettings != null
                    && PieContainer.this.mWindowManager != null) {
                layoutParams.width = -1;
                layoutParams.height = -1;
                layoutParams.format = -2;
                try {
                    PieContainer.this.mWindowManager.updateViewLayout(this, layoutParams);
                } catch (Exception unused) {
                }
            }
            if (PieContainer.this.mPieControl == null || this.mPieControlAdded) {
                return;
            }
            PieContainer.this.mPieControl.attachToContainer(this);
            this.mPieControlAdded = true;
        }

        public void shrinkLayout() {
            if (PieContainer.DEBUG) {
                Log.d(PieContainer.TAG, "shrinkLayout");
            }
            WindowManager.LayoutParams layoutParams =
                    (WindowManager.LayoutParams) getLayoutParams();
            if (layoutParams == null
                    || PieContainer.this.mSettings == null
                    || PieContainer.this.mWindowManager == null) {
                return;
            }
            try {
                PieContainer.this.mWindowManager.removeViewImmediate(this);
            } catch (Exception unused) {
            }
            if (PieContainer.this.mPieControl != null && this.mPieControlAdded) {
                PieContainer.this.mPieControl.removeFromContainer(this);
                this.mPieControlAdded = false;
                this.mShrinking = true;
            }
            layoutParams.width =
                    this.mBottom
                            ? PieContainer.this.mSettings.loadPieAreaY()
                            : PieContainer.this.mSettings.loadPieAreaX();
            layoutParams.height =
                    this.mBottom
                            ? PieContainer.this.mSettings.loadPieAreaX()
                            : PieContainer.this.mSettings.loadPieAreaY();
            layoutParams.format = -2;
            try {
                PieContainer.this.mWindowManager.addView(this, layoutParams);
            } catch (Exception unused2) {
            }
        }

        public void debug(boolean enabled) {
            WindowManager.LayoutParams layoutParams =
                    (WindowManager.LayoutParams) getLayoutParams();
            if (layoutParams == null
                    || PieContainer.this.mSettings == null
                    || PieContainer.this.mWindowManager == null
                    || PieContainer.this.mPieControl == null) {
                return;
            }
            if (enabled) {
                layoutParams.format = -1;
            } else {
                layoutParams.format = -2;
            }
            try {
                PieContainer.this.mWindowManager.updateViewLayout(this, layoutParams);
            } catch (Exception unused) {
            }
        }
    }

    PieContainer(Context context) {
        this.mSettings = SettingsValues.getInstance(context);
        this.mContext = context;
        android.accessibilityservice.AccessibilityService service =
                Build.VERSION.SDK_INT >= 26 ? AccessibilityHandler.connectedService() : null;
        this.mTrusted = service != null;
        this.mWindowManager =
                (WindowManager)
                        (this.mTrusted ? service : context)
                                .getSystemService(Context.WINDOW_SERVICE);
        this.mPieControl = new PieControl(context);
        this.mPieLayout1 = new PieLayout(context, false);
        this.mPieLayout2 = new PieLayout(context, false);
        this.mPieLayout3 = new PieLayout(context, true);
    }

    void attachToWindowManager() {
        int verticalInset;
        int gravity;
        if (DEBUG) {
            Log.d(TAG, "attachToWindowManager");
        }
        int piePos = this.mSettings.loadPiePos();
        int behindKeyboard = this.mSettings.loadPieAreaBehindKeyboard();
        int onLockScreen = this.mSettings.loadPieOnLockScreen();
        int areaGravity = this.mSettings.loadPieAreaGravity();
        if (1 == areaGravity) {
            verticalInset = this.mSettings.getScreenHeight() / 10;
            gravity = 48;
        } else {
            verticalInset = 0;
            gravity = 0;
        }
        if (2 == areaGravity) {
            verticalInset = this.mSettings.getScreenHeight() / 10;
            gravity = 80;
        }
        if (piePos == 0 || piePos == 4 || piePos >= 6) {
            this.mWindowManager.addView(
                    this.mPieLayout1,
                    new WindowManager.LayoutParams(
                            -2,
                            -2,
                            getWindowType(onLockScreen),
                            getWindowFlags(behindKeyboard),
                            -2));
            WindowManager.LayoutParams layoutParams =
                    (WindowManager.LayoutParams) this.mPieLayout1.getLayoutParams();
            layoutParams.width = this.mSettings.loadPieAreaX();
            layoutParams.height = this.mSettings.loadPieAreaY();
            layoutParams.gravity = Gravity.END | gravity;
            layoutParams.y = verticalInset;
            this.mWindowManager.updateViewLayout(this.mPieLayout1, layoutParams);
            this.mAddedLayout1 = true;
        }
        if (piePos == 1 || piePos == 4 || piePos == 5 || piePos >= 7) {
            this.mWindowManager.addView(
                    this.mPieLayout2,
                    new WindowManager.LayoutParams(
                            -2,
                            -2,
                            getWindowType(onLockScreen),
                            getWindowFlags(behindKeyboard),
                            -2));
            WindowManager.LayoutParams layoutParams2 =
                    (WindowManager.LayoutParams) this.mPieLayout2.getLayoutParams();
            layoutParams2.width = this.mSettings.loadPieAreaX();
            layoutParams2.height = this.mSettings.loadPieAreaY();
            layoutParams2.gravity = gravity | Gravity.START;
            layoutParams2.y = verticalInset;
            this.mWindowManager.updateViewLayout(this.mPieLayout2, layoutParams2);
            this.mAddedLayout2 = true;
        }
        if (piePos == 2 || piePos == 3 || piePos >= 5) {
            this.mWindowManager.addView(
                    this.mPieLayout3,
                    new WindowManager.LayoutParams(
                            -2,
                            -2,
                            getWindowType(onLockScreen),
                            getWindowFlags(behindKeyboard),
                            -2));
            WindowManager.LayoutParams layoutParams3 =
                    (WindowManager.LayoutParams) this.mPieLayout3.getLayoutParams();
            layoutParams3.width = this.mSettings.loadPieAreaY();
            layoutParams3.height = this.mSettings.loadPieAreaX();
            layoutParams3.gravity = 81;
            this.mWindowManager.updateViewLayout(this.mPieLayout3, layoutParams3);
            this.mAddedLayout3 = true;
        }
        watchLock();
    }

    void removeFromWindowManager() {
        if (this.mLockReceiver != null) {
            try {
                this.mContext.unregisterReceiver(this.mLockReceiver);
            } catch (RuntimeException e) {
                Log.w(TAG, "Lock receiver already gone", e);
            }
            this.mLockReceiver = null;
        }
        if (DEBUG) {
            Log.d(TAG, "removeFromWindowManager");
        }
        // An accessibility overlay is already gone when the accessibility service went away.
        if (this.mAddedLayout1) {
            removeQuietly(this.mPieLayout1);
            this.mAddedLayout1 = false;
        }
        if (this.mAddedLayout2) {
            removeQuietly(this.mPieLayout2);
            this.mAddedLayout2 = false;
        }
        if (this.mAddedLayout3) {
            removeQuietly(this.mPieLayout3);
            this.mAddedLayout3 = false;
        }
    }

    private void removeQuietly(View layout) {
        try {
            this.mWindowManager.removeView(layout);
        } catch (RuntimeException e) {
            Log.w(TAG, "Pie window already removed", e);
        }
    }

    /** Follows locking and unlocking while the strips can show above the lock screen. */
    private void watchLock() {
        if (!this.mTrusted || this.mSettings.loadPieOnLockScreen() != 0) {
            return;
        }
        this.mLockReceiver =
                new android.content.BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, android.content.Intent intent) {
                        applyLock();
                    }
                };
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(android.content.Intent.ACTION_SCREEN_OFF);
        filter.addAction(android.content.Intent.ACTION_SCREEN_ON);
        filter.addAction(android.content.Intent.ACTION_USER_PRESENT);
        this.mContext.registerReceiver(this.mLockReceiver, filter);
        applyLock();
    }

    /** Untouchable strips while the keyguard is up; touchable again once unlocked. */
    private void applyLock() {
        android.app.KeyguardManager keyguard =
                this.mContext.getSystemService(android.app.KeyguardManager.class);
        boolean locked = keyguard != null && keyguard.isKeyguardLocked();
        setTouchable(this.mAddedLayout1, this.mPieLayout1, !locked);
        setTouchable(this.mAddedLayout2, this.mPieLayout2, !locked);
        setTouchable(this.mAddedLayout3, this.mPieLayout3, !locked);
    }

    private void setTouchable(boolean added, View layout, boolean touchable) {
        if (!added || !(layout.getLayoutParams() instanceof WindowManager.LayoutParams)) {
            return;
        }
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) layout.getLayoutParams();
        int flags =
                touchable
                        ? params.flags & ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        : params.flags | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        if (flags == params.flags) {
            return;
        }
        params.flags = flags;
        try {
            this.mWindowManager.updateViewLayout(layout, params);
        } catch (RuntimeException e) {
            Log.w(TAG, "Pie window not attached", e);
        }
    }

    /** Whether the strips were added as accessibility overlays. */
    boolean isTrusted() {
        return this.mTrusted;
    }

    void rotate() {
        if (DEBUG) {
            Log.d(TAG, "rotate");
        }
        if (this.mAddedLayout1) {
            this.mPieLayout1.shrinkLayout();
        }
        if (this.mAddedLayout2) {
            this.mPieLayout2.shrinkLayout();
        }
        if (this.mAddedLayout3) {
            this.mPieLayout3.shrinkLayout();
        }
    }

    public void debug() {
        if (this.mAddedLayout1) {
            this.mPieLayout1.debug(true);
        }
        if (this.mAddedLayout2) {
            this.mPieLayout2.debug(true);
        }
        if (this.mAddedLayout3) {
            this.mPieLayout3.debug(true);
        }
    }

    private int getWindowType(int position) {
        if (Build.VERSION.SDK_INT < 26) {
            return position > 0 ? 2010 : 2003;
        }
        return this.mTrusted ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY : 2038;
    }
}
