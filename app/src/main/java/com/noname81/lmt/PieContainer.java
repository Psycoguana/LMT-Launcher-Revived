package com.noname81.lmt;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
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

        @Override // android.view.ViewGroup, android.view.View
        public boolean dispatchTouchEvent(MotionEvent motionEvent) {
            if (PieContainer.DEBUG) {
                Log.d(PieContainer.TAG, "dispatchTouchEvent " + motionEvent);
            }
            if (motionEvent.getToolType(0) == 0) {
                return false;
            }
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == 0) {
                boolean zIsNotBlacklistedPie = PieContainer.this.mSettings.isNotBlacklistedPie();
                this.mAllowed = zIsNotBlacklistedPie;
                if (zIsNotBlacklistedPie) {
                    expandLayout();
                }
            } else if (1 == actionMasked) {
                shrinkLayout();
            }
            if (!this.mAllowed) {
                return false;
            }
            PieContainer.this.mPieControl.mPie.onTouchEvent(motionEvent);
            return true;
        }

        @Override // android.view.View
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
        this.mWindowManager = (WindowManager) context.getSystemService("window");
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
            layoutParams.gravity = 8388613 | gravity;
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
    }

    void removeFromWindowManager() {
        if (DEBUG) {
            Log.d(TAG, "removeFromWindowManager");
        }
        if (this.mAddedLayout1) {
            this.mWindowManager.removeView(this.mPieLayout1);
            this.mAddedLayout1 = false;
        }
        if (this.mAddedLayout2) {
            this.mWindowManager.removeView(this.mPieLayout2);
            this.mAddedLayout2 = false;
        }
        if (this.mAddedLayout3) {
            this.mWindowManager.removeView(this.mPieLayout3);
            this.mAddedLayout3 = false;
        }
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
        if (Build.VERSION.SDK_INT < 16) {
            removeFromWindowManager();
            attachToWindowManager();
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
        return 2038;
    }
}
