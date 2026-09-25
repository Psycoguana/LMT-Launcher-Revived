package com.noname81.lmt;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

/**
 * Brightness dial extension (ROADMAP 1.17, asked for on XDA): hover the Brightness slice, slide out
 * past the pie's rim and a dial wraps around the pie; the finger's angle sets the screen brightness
 * live. Up is brighter for a pie on a side, right is brighter for a pie at the bottom. Lifting the
 * finger keeps the value and fires nothing; sliding back into the centre hands the touch back to
 * the pie.
 *
 * <p>Writes {@link Settings.System#SCREEN_BRIGHTNESS} (switching adaptive brightness off), so it
 * needs the "Modify system settings" permission; without it the slice asks for it when released.
 */
class PieBrightness implements PieMenu.PieView {
    private static final String TAG = "LMT::PieBrightness";

    /** Settings value range; 0 would turn some panels off. */
    private static final int MIN = 1;

    private static final int MAX = 255;

    /** Brightness feels linear on a gamma curve, like the system slider. */
    private static final double GAMMA = 2.2;

    private final Context mContext;
    private final Paint mTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKnob = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKnobRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mPath = new Path();
    private final float mGap;
    private final float mStroke;

    private Point mCenter;
    private int mRadius;
    private int mRimRadius;
    private boolean mOnTheLeft;
    private boolean mOnTheBottom;

    /** The slice was hovered; the finger takes over once it leaves the pie within its angles. */
    private boolean mArmed;

    private float mSliceStart;
    private float mSliceSweep;

    private boolean mActive;
    private float mFraction;
    private int mWritten = -1;

    PieBrightness(Context context, int color) {
        this.mContext = context;
        float density = context.getResources().getDisplayMetrics().density;
        this.mGap = 22 * density;
        this.mStroke = 10 * density;
        this.mTrack.setStyle(Paint.Style.STROKE);
        this.mTrack.setStrokeCap(Paint.Cap.ROUND);
        this.mTrack.setStrokeWidth(this.mStroke);
        // White on a dark track reads on light and dark wallpapers alike.
        this.mTrack.setColor(Color.argb(110, 0, 0, 0));
        this.mFill.setStyle(Paint.Style.STROKE);
        this.mFill.setStrokeCap(Paint.Cap.ROUND);
        this.mFill.setStrokeWidth(this.mStroke);
        this.mFill.setColor(Color.WHITE);
        this.mFill.setShadowLayer(2 * density, 0, 0, Color.argb(120, 0, 0, 0));
        this.mKnob.setColor(Color.WHITE);
        this.mKnobRing.setStyle(Paint.Style.STROKE);
        this.mKnobRing.setStrokeWidth(3 * density);
        this.mKnobRing.setColor(color | 0xFF000000);
        this.mKnob.setShadowLayer(3 * density, 0, 0, Color.argb(120, 0, 0, 0));
        this.mText.setColor(Color.WHITE);
        this.mText.setTextAlign(Paint.Align.CENTER);
        this.mText.setTextSize(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        14,
                        context.getResources().getDisplayMetrics()));
        this.mText.setShadowLayer(3 * density, 0, 0, Color.argb(160, 0, 0, 0));
    }

    /** Not used: the dial arms with {@link #arm}, which knows the slice. */
    @Override
    public void activate() {}

    /** The Brightness slice is hovered: take over once the finger leaves the pie through it. */
    void arm(PieItem slice) {
        if (!canWrite(this.mContext)) {
            return;
        }
        this.mArmed = true;
        this.mSliceStart = slice.getStartAngle();
        this.mSliceSweep = slice.getSweep();
    }

    /** Tapping the slice: adaptive brightness on or off. */
    static void toggleAdaptive(Context context) {
        ContentResolver resolver = context.getContentResolver();
        int mode =
                Settings.System.getInt(
                        resolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        boolean adaptive = mode != Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        try {
            Settings.System.putInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    adaptive
                            ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                            : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Toast.makeText(
                            context,
                            adaptive ? "Adaptive brightness on" : "Adaptive brightness off",
                            Toast.LENGTH_SHORT)
                    .show();
        } catch (RuntimeException e) {
            Log.w(TAG, "Adaptive brightness write refused", e);
        }
    }

    static boolean canWrite(Context context) {
        return Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(context);
    }

    @Override
    public void layout(
            boolean visible,
            Point center,
            int radius,
            int radiusInc,
            int levels,
            boolean onTheLeft,
            boolean onTheBottom,
            boolean onTheRight) {
        this.mCenter = center;
        this.mRadius = radius;
        this.mRimRadius = radius + radiusInc * levels;
        this.mOnTheLeft = onTheLeft;
        this.mOnTheBottom = onTheBottom;
        if (!visible) {
            this.mArmed = false;
            this.mActive = false;
        }
    }

    @Override
    public boolean onTouchEvent(
            int action, float x, float y, float deltaX, float deltaY, PointF polar) {
        if (!this.mActive) {
            if (!this.mArmed || action != 2) {
                return false;
            }
            boolean onSlice =
                    polar.x > this.mSliceStart && polar.x < this.mSliceStart + this.mSliceSweep;
            if (!onSlice) {
                // Moved on to another slice without leaving the pie.
                this.mArmed = false;
                return false;
            }
            if (polar.y <= this.mRimRadius) {
                return false;
            }
            this.mActive = true;
            this.mWritten = currentValue();
        }
        if (polar.y < this.mRadius) {
            // Back in the centre: the pie takes the touch again.
            this.mActive = false;
            this.mArmed = false;
            return false;
        }
        this.mFraction = fractionAt(polar.x);
        write(this.mFraction);
        return true;
    }

    @Override
    public boolean drawBackground(Canvas canvas) {
        return false;
    }

    @Override
    public boolean drawForeground(Canvas canvas) {
        if (!this.mActive || this.mCenter == null) {
            return false;
        }
        float r = this.mRimRadius + this.mGap;
        arc(canvas, r, 0f, 1f, this.mTrack);
        arc(canvas, r, 0f, this.mFraction, this.mFill);
        PointF knob = point(r, angleOf(this.mFraction));
        canvas.drawCircle(knob.x, knob.y, this.mStroke * 0.9f, this.mKnob);
        canvas.drawCircle(knob.x, knob.y, this.mStroke * 0.9f, this.mKnobRing);
        // The value sits just outside the knob, away from the finger.
        PointF label = point(r + this.mStroke * 2.6f, angleOf(this.mFraction));
        canvas.drawText(
                Math.round(this.mFraction * 100) + "%",
                label.x,
                label.y + this.mText.getTextSize() / 3,
                this.mText);
        return false;
    }

    @Override
    public void onShrink() {
        this.mArmed = false;
        this.mActive = false;
    }

    /** The dial's fraction (0 dimmest, 1 brightest) for a polar angle from PieMenu.getPolar. */
    private float fractionAt(float angle) {
        double f = this.mOnTheBottom ? 0.5 - angle / Math.PI : 1.0 - angle / Math.PI;
        return (float) Math.max(0, Math.min(1, f));
    }

    private double angleOf(float fraction) {
        return this.mOnTheBottom ? (0.5 - fraction) * Math.PI : (1.0 - fraction) * Math.PI;
    }

    /** Screen point at radius r and polar angle a, the inverse of PieMenu.getPolar. */
    private PointF point(float r, double a) {
        float dx = (float) (r * Math.sin(a));
        float dy = (float) (r * Math.cos(a));
        float x = this.mOnTheLeft ? this.mCenter.x + dx : this.mCenter.x - dx;
        return new PointF(x, this.mCenter.y - dy);
    }

    private void arc(Canvas canvas, float r, float from, float to, Paint paint) {
        if (to <= from) {
            return;
        }
        this.mPath.reset();
        int steps = 48;
        for (int i = 0; i <= steps; i++) {
            PointF p = point(r, angleOf(from + (to - from) * i / steps));
            if (i == 0) {
                this.mPath.moveTo(p.x, p.y);
            } else {
                this.mPath.lineTo(p.x, p.y);
            }
        }
        canvas.drawPath(this.mPath, paint);
    }

    private int currentValue() {
        try {
            int value =
                    Settings.System.getInt(
                            this.mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            this.mFraction =
                    (float) Math.pow(Math.max(0, value - MIN) / (double) (MAX - MIN), 1 / GAMMA);
            return value;
        } catch (Settings.SettingNotFoundException e) {
            this.mFraction = 0.5f;
            return -1;
        }
    }

    private void write(float fraction) {
        int value = MIN + (int) Math.round((MAX - MIN) * Math.pow(fraction, GAMMA));
        if (value == this.mWritten) {
            return;
        }
        try {
            ContentResolver resolver = this.mContext.getContentResolver();
            Settings.System.putInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, value);
            this.mWritten = value;
        } catch (RuntimeException e) {
            Log.w(TAG, "Brightness write refused", e);
            this.mActive = false;
            this.mArmed = false;
        }
    }
}
