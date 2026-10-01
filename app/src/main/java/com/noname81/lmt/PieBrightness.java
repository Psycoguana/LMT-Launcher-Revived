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
 * <p>With the Brightness dial setting on, the dial is also always there, faint, just outside the
 * pie, with no slice needed (like the status infos): resting the finger on it for a moment grabs
 * it, so sliding out past it to an outer slice doesn't change the brightness.
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
    private final android.view.View mHost;
    private final SettingsValues mSettings;
    private final Runnable mGrab = this::grab;
    private final float mGrabBand;

    /** The always-there dial (setting on and allowed), and whether its grab is pending. */
    private boolean mAlways;

    private boolean mGrabPending;
    private final Paint mTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKnob = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKnobRing = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * The knob: Material Icons Round's brightness_high in sun gold. Only that glyph is bundled
     * (assets/MaterialIconsRound-Brightness.otf, subset of the Apache 2.0 font); without it the
     * knob falls back to a dot.
     */
    private final Paint mSun = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final String SUN = "\ue1ac";
    private static final int SUN_GOLD = 0xFFFFC107;
    private boolean mHasSun;

    /** Erasers for the gap around the sun: its outline widened, and its center filled. */
    private final Paint mSunGap = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint mSunGapFill = new Paint(Paint.ANTI_ALIAS_FLAG);
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

    PieBrightness(android.view.View host, Context context, int color) {
        this.mContext = context;
        this.mHost = host;
        this.mSettings = SettingsValues.getInstance(context);
        float density = context.getResources().getDisplayMetrics().density;
        this.mGrabBand = 26 * density;
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
        try {
            this.mSun.setTypeface(
                    android.graphics.Typeface.createFromAsset(
                            context.getAssets(), "MaterialIconsRound-Brightness.otf"));
            this.mHasSun = true;
        } catch (RuntimeException e) {
            Log.w(TAG, "No brightness icon font", e);
        }
        this.mSun.setColor(SUN_GOLD);
        this.mSun.setTextAlign(Paint.Align.CENTER);
        this.mSun.setTextSize(this.mStroke * 3.2f);
        this.mSun.setShadowLayer(3 * density, 0, 0, Color.argb(170, 0, 0, 0));
        android.graphics.PorterDuffXfermode clear =
                new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR);
        this.mSunGap.setTypeface(this.mSun.getTypeface());
        this.mSunGap.setTextAlign(Paint.Align.CENTER);
        this.mSunGap.setTextSize(this.mSun.getTextSize());
        this.mSunGap.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mSunGap.setStrokeJoin(Paint.Join.ROUND);
        this.mSunGap.setStrokeWidth(6 * density);
        this.mSunGap.setXfermode(clear);
        this.mSunGapFill.setXfermode(clear);
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
        cancelGrab();
        this.mArmed = false;
        this.mActive = false;
        this.mAlways =
                visible && this.mSettings.loadPieBrightnessDial() != 0 && canWrite(this.mContext);
        if (this.mAlways) {
            currentValue();
        }
    }

    /** Resting on the always-there dial long enough grabs it. */
    private void grab() {
        this.mGrabPending = false;
        if (!this.mAlways || this.mActive) {
            return;
        }
        this.mActive = true;
        this.mWritten = currentValue();
        this.mHost.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        this.mHost.invalidate();
    }

    private void cancelGrab() {
        if (this.mGrabPending) {
            this.mHost.removeCallbacks(this.mGrab);
            this.mGrabPending = false;
        }
    }

    /** The dial's radius, just outside the pie. */
    private float dialRadius() {
        return this.mRimRadius + this.mGap;
    }

    @Override
    public boolean onTouchEvent(
            int action, float x, float y, float deltaX, float deltaY, PointF polar) {
        if (!this.mActive && this.mAlways && action == 2) {
            // Before the pie's own long press (which would fire the slice under the finger).
            boolean onDial =
                    polar.y > this.mRimRadius && Math.abs(polar.y - dialRadius()) < this.mGrabBand;
            if (onDial && !this.mGrabPending) {
                this.mGrabPending = true;
                this.mHost.postDelayed(this.mGrab, this.mSettings.loadPieLongpress() * 4L / 5);
            } else if (!onDial) {
                cancelGrab();
            }
        }
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
        if (this.mCenter == null || !(this.mActive || this.mAlways)) {
            return false;
        }
        float r = dialRadius();
        if (!this.mActive) {
            // Resting: a faint dial showing the level, to grab by holding on it.
            int track = this.mTrack.getAlpha();
            int fill = this.mFill.getAlpha();
            this.mTrack.setAlpha(track / 2);
            this.mFill.setAlpha(110);
            arc(canvas, r, 0f, 1f, this.mTrack);
            arc(canvas, r, 0f, this.mFraction, this.mFill);
            this.mTrack.setAlpha(track);
            this.mFill.setAlpha(fill);
            return false;
        }
        PointF knob = point(r, angleOf(this.mFraction));
        // The dial goes on a layer so a gap following the sun's outline can be cut out of it:
        // no white shows under or through the sun.
        int layer = canvas.saveLayer(null, null);
        arc(canvas, r, 0f, 1f, this.mTrack);
        arc(canvas, r, 0f, this.mFraction, this.mFill);
        if (this.mHasSun) {
            Paint.FontMetrics metrics = this.mSun.getFontMetrics();
            float baseline = knob.y - (metrics.ascent + metrics.descent) / 2;
            canvas.drawText(SUN, knob.x, baseline, this.mSunGap);
            canvas.drawCircle(knob.x, knob.y, this.mSun.getTextSize() * 0.3f, this.mSunGapFill);
        }
        canvas.restoreToCount(layer);
        if (this.mHasSun) {
            Paint.FontMetrics metrics = this.mSun.getFontMetrics();
            canvas.drawText(
                    SUN, knob.x, knob.y - (metrics.ascent + metrics.descent) / 2, this.mSun);
        } else {
            canvas.drawCircle(knob.x, knob.y, this.mStroke * 0.9f, this.mKnob);
            canvas.drawCircle(knob.x, knob.y, this.mStroke * 0.9f, this.mKnobRing);
        }
        // The value sits just outside the knob, away from the finger.
        PointF label = point(r + this.mStroke * 3.4f, angleOf(this.mFraction));
        canvas.drawText(
                Math.round(this.mFraction * 100) + "%",
                label.x,
                label.y + this.mText.getTextSize() / 3,
                this.mText);
        return false;
    }

    @Override
    public void onShrink() {
        cancelGrab();
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
