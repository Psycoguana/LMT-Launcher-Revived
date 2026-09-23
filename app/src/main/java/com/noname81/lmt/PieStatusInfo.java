package com.noname81.lmt;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class PieStatusInfo implements SensorEventListener, PieMenu.PieView {
    private static final int AIContentInvalid = 0;
    private static final int AIContentOverview = 1;
    private static final int AINotification1 = 2;
    private static final int AINotification2 = 3;
    private static final int AINotification3 = 4;
    private static final int AINotification4 = 5;
    private static final int AINotification5 = 6;
    private float mAIAnimationIn;
    private float mAIAnimationInOut;
    private boolean mAIAnimationInOutReversed;
    private ValueAnimator mAIAnimatorIn;
    private ValueAnimator mAIAnimatorOutIn;
    private int mAIContent;
    private final Path mAIPath;
    private boolean mAISelected;
    private final String[] mAIStrings;
    private final Paint mAITextPaint;
    private final int mAITextSize;
    private final boolean mAnimationActive;
    private final int mAnimationTime;
    private Timer mAnimationTimer;
    private final Paint mBackgroundPaint;
    private final int mBackroundAlpha;
    private Point mCenter;
    private float mClockAnimationIn;
    private ValueAnimator mClockAnimatorIn;
    private final Path mClockPath;
    private boolean mClockSelected;
    private final String[] mClockStrings;
    private final Paint mClockTextPaint;
    private final int mClockTextSizeBig;
    private final int mClockTextSizeSmall;
    private final int mColorClock;
    private final int mColorNotifications;
    private final int mColorSelected;
    private final Context mContext;
    private final String mDateFormat;
    private final Launcher mLauncher;
    private int mLevels;
    private final NotificationDataHelper mNotificationDataHelper;
    private boolean mOnTheBottom;
    private boolean mOnTheLeft;
    private final FrameLayout mPieMenu;
    private final int mPositionOffset;
    private int mRadius;
    private int mRadiusInc;
    private final int mRadiusOffset;
    private final RootContext mRootContext;
    private final boolean mSensorActivated;
    private final float mSensorAlpha;
    private float mSensorAnimation;
    private ValueAnimator mSensorAnimator;
    private final float[] mSensorGravity;
    private long mSensorLastUpdate;
    private final float[] mSensorLinearAcceleration;
    private final SensorManager mSensorManager;
    private final SettingsValues mSettings;
    private final int mTwentyFour;

    @Override // com.noname81.lmt.PieMenu.PieView
    public void activate() {}

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override // com.noname81.lmt.PieMenu.PieView
    public void onShrink() {}

    PieStatusInfo(
            FrameLayout frameLayout,
            Context context,
            int colorClock,
            int colorNotifications,
            int colorSelected,
            int colorDim) {
        int textSizeBig;
        int textSizeSmall;
        Typeface typefaceCreateFromAsset;
        this.mPieMenu = frameLayout;
        this.mContext = context;
        SettingsValues settingsValues = SettingsValues.getInstance(context);
        this.mSettings = settingsValues;
        this.mNotificationDataHelper = NotificationDataHelper.getInstance();
        this.mLauncher = Launcher.getInstance(context);
        this.mRootContext = RootContext.getInstance(context);
        this.mColorClock = colorClock;
        this.mColorNotifications = colorNotifications;
        this.mColorSelected = colorSelected;
        this.mAnimationActive = true;
        this.mAnimationTime = settingsValues.loadPieAnimation();
        this.mSensorActivated = settingsValues.loadPieShowStatusInfos() == 3 ? true : false;
        this.mSensorManager = (SensorManager) context.getSystemService("sensor");
        this.mSensorAnimator = new ValueAnimator();
        this.mSensorAnimation = 0.0f;
        this.mSensorAlpha = 0.8f;
        this.mSensorLinearAcceleration = new float[3];
        this.mSensorGravity = new float[3];
        Resources resources = context.getResources();
        this.mRadiusOffset =
                (int) TypedValue.applyDimension(1, 10.0f, resources.getDisplayMetrics());
        this.mPositionOffset =
                (int) TypedValue.applyDimension(1, 5.0f, resources.getDisplayMetrics());
        int iLoadPieFont = settingsValues.loadPieFont();
        if (iLoadPieFont < 3) {
            textSizeBig = iLoadPieFont != 0 ? 65 : 45;
            textSizeSmall = iLoadPieFont != 0 ? 13 : 8;
            textSizeBig = iLoadPieFont == 2 ? textSizeBig + 15 : textSizeBig;
            textSizeSmall = iLoadPieFont == 2 ? textSizeSmall + 4 : textSizeSmall;
            typefaceCreateFromAsset =
                    Typeface.createFromAsset(context.getAssets(), "Roboto-Thin.ttf");
        } else if (iLoadPieFont < 6) {
            textSizeBig = iLoadPieFont != 3 ? 65 : 45;
            textSizeSmall = iLoadPieFont != 3 ? 13 : 8;
            textSizeBig = iLoadPieFont == 5 ? textSizeBig + 15 : textSizeBig;
            textSizeSmall = iLoadPieFont == 5 ? textSizeSmall + 4 : textSizeSmall;
            typefaceCreateFromAsset =
                    Typeface.createFromAsset(context.getAssets(), "Roboto-Light.ttf");
        } else {
            textSizeBig = iLoadPieFont != 6 ? 65 : 45;
            int smallSize = iLoadPieFont == 6 ? 8 : 13;
            textSizeBig = iLoadPieFont == 8 ? textSizeBig + 15 : textSizeBig;
            textSizeSmall = iLoadPieFont == 8 ? smallSize + 4 : smallSize;
            typefaceCreateFromAsset = Typeface.DEFAULT;
        }
        this.mClockPath = new Path();
        this.mClockAnimatorIn = new ValueAnimator();
        this.mClockAnimationIn = 0.0f;
        this.mClockTextSizeBig =
                (int) TypedValue.applyDimension(1, textSizeBig, resources.getDisplayMetrics());
        float smallTextSize = textSizeSmall;
        this.mClockTextSizeSmall =
                (int) TypedValue.applyDimension(1, smallTextSize, resources.getDisplayMetrics());
        Paint paint = new Paint();
        this.mClockTextPaint = paint;
        paint.setColor(colorClock);
        paint.setAlpha(0);
        paint.setAntiAlias(true);
        paint.setTypeface(typefaceCreateFromAsset);
        paint.setTextAlign(Paint.Align.RIGHT);
        this.mClockStrings = new String[2];
        this.mClockSelected = false;
        this.mAIPath = new Path();
        this.mAIAnimatorIn = new ValueAnimator();
        this.mAIAnimationIn = 0.0f;
        this.mAIAnimatorOutIn = new ValueAnimator();
        this.mAIAnimationInOut = 0.0f;
        this.mAIAnimationInOutReversed = false;
        int iApplyDimension =
                (int) TypedValue.applyDimension(1, smallTextSize, resources.getDisplayMetrics());
        this.mAITextSize = iApplyDimension;
        Paint paint2 = new Paint();
        this.mAITextPaint = paint2;
        paint2.setColor(colorNotifications);
        paint2.setAlpha(0);
        paint2.setAntiAlias(true);
        paint2.setTypeface(typefaceCreateFromAsset);
        paint2.setTextAlign(Paint.Align.LEFT);
        paint2.setTextSize(iApplyDimension);
        this.mAIStrings = new String[3];
        this.mAIContent = 0;
        this.mAISelected = false;
        Paint paint3 = new Paint();
        this.mBackgroundPaint = paint3;
        paint3.setColor(colorDim);
        paint3.setAlpha(0);
        this.mBackroundAlpha = Color.alpha(colorDim);
        this.mTwentyFour = DateFormat.is24HourFormat(context) ? 24 : 12;
        this.mDateFormat = new String(DateFormat.getDateFormatOrder(context));
    }

    @Override // com.noname81.lmt.PieMenu.PieView
    public void layout(
            boolean visible,
            Point center,
            int radius,
            int radiusInc,
            int levels,
            boolean onTheLeft,
            boolean onTheBottom,
            boolean onTheRight) {
        Timer timer;
        if (this.mAnimationActive) {
            this.mClockAnimatorIn.cancel();
        }
        this.mClockAnimationIn = 0.0f;
        this.mClockTextPaint.setAlpha(0);
        if (this.mAnimationActive) {
            this.mAIAnimatorIn.cancel();
        }
        this.mAIAnimationIn = 0.0f;
        if (this.mAnimationActive) {
            this.mAIAnimatorOutIn.cancel();
        }
        this.mAIAnimationInOut = 0.0f;
        this.mAIAnimationInOutReversed = false;
        this.mAITextPaint.setAlpha(0);
        this.mAIContent = 0;
        this.mBackgroundPaint.setAlpha(0);
        if (!this.mAnimationActive && (timer = this.mAnimationTimer) != null) {
            timer.cancel();
        }
        handleSensorListener(visible);
        if (visible) {
            this.mCenter = center;
            this.mRadius = radius;
            this.mRadiusInc = radiusInc;
            this.mLevels = levels;
            this.mOnTheLeft = onTheLeft;
            this.mOnTheBottom = onTheBottom;
            makePath(this.mSettings.loadPieShowStatusInfos() == 2 ? 1.0f : 0.0f);
            fillClockStrings();
            fillAdditionalInfoStrings();
            if (this.mAnimationActive && !onTheRight) {
                ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
                this.mClockAnimatorIn = valueAnimatorOfFloat;
                valueAnimatorOfFloat.addUpdateListener(
                        new ValueAnimator.AnimatorUpdateListener() { // from class:
                            // com.noname81.lmt.PieStatusInfo.1
                            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                PieStatusInfo.this.mClockAnimationIn =
                                        valueAnimator.getAnimatedFraction();
                                PieStatusInfo.this.mClockTextPaint.setAlpha(
                                        (int)
                                                (PieStatusInfo.this.mClockAnimationIn
                                                        * Color.alpha(
                                                                PieStatusInfo.this.mColorClock)));
                                PieStatusInfo.this.mBackgroundPaint.setAlpha(
                                        (int)
                                                (PieStatusInfo.this.mClockAnimationIn
                                                        * PieStatusInfo.this.mBackroundAlpha));
                                PieStatusInfo.this.mPieMenu.invalidate();
                            }
                        });
                this.mClockAnimatorIn.setStartDelay(((long) this.mAnimationTime) * 8);
                this.mClockAnimatorIn.setDuration(((long) this.mAnimationTime) * 6);
                this.mClockAnimatorIn.setInterpolator(new DecelerateInterpolator());
                this.mClockAnimatorIn.start();
                ValueAnimator valueAnimatorOfFloat2 = ValueAnimator.ofFloat(0.0f, 1.0f);
                this.mAIAnimatorIn = valueAnimatorOfFloat2;
                valueAnimatorOfFloat2.addUpdateListener(
                        new ValueAnimator.AnimatorUpdateListener() { // from class:
                            // com.noname81.lmt.PieStatusInfo.2
                            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                PieStatusInfo.this.mAIAnimationIn =
                                        valueAnimator.getAnimatedFraction();
                                PieStatusInfo.this.mAITextPaint.setAlpha(
                                        (int)
                                                (PieStatusInfo.this.mAIAnimationIn
                                                        * Color.alpha(
                                                                PieStatusInfo.this
                                                                        .mColorNotifications)));
                                PieStatusInfo.this.mPieMenu.invalidate();
                            }
                        });
                this.mAIAnimatorIn.setStartDelay(((long) this.mAnimationTime) * 10);
                this.mAIAnimatorIn.setDuration(((long) this.mAnimationTime) * 14);
                this.mAIAnimatorIn.start();
                ValueAnimator valueAnimatorOfFloat3 = ValueAnimator.ofFloat(0.0f, 1.0f);
                this.mAIAnimatorOutIn = valueAnimatorOfFloat3;
                valueAnimatorOfFloat3.addUpdateListener(
                        new ValueAnimator.AnimatorUpdateListener() { // from class:
                            // com.noname81.lmt.PieStatusInfo.3
                            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                float fMax;
                                if (valueAnimator.getAnimatedFraction() <= 0.2f) {
                                    if (valueAnimator.getAnimatedFraction() < 0.1f) {
                                        fMax =
                                                Math.max(
                                                        valueAnimator.getAnimatedFraction() * 10.0f,
                                                        0.0f);
                                    } else {
                                        fMax =
                                                Math.max(
                                                        (0.18f
                                                                        - valueAnimator
                                                                                .getAnimatedFraction())
                                                                * 10.0f,
                                                        0.0f);
                                    }
                                    PieStatusInfo.this.mAITextPaint.setAlpha(
                                            (int)
                                                    ((1.0f - fMax)
                                                            * Color.alpha(
                                                                    PieStatusInfo.this
                                                                            .mColorNotifications)));
                                    if (PieStatusInfo.this.mAIAnimationInOut <= fMax
                                            || PieStatusInfo.this.mAIAnimationInOutReversed) {
                                        if (PieStatusInfo.this.mAIAnimationInOut < fMax
                                                && PieStatusInfo.this.mAIAnimationInOutReversed) {
                                            PieStatusInfo.this.mAIAnimationInOutReversed = false;
                                        }
                                    } else {
                                        PieStatusInfo.this.fillAdditionalInfoStrings();
                                        PieStatusInfo.this.mAIAnimationInOutReversed = true;
                                    }
                                    PieStatusInfo.this.mAIAnimationInOut = fMax;
                                    PieStatusInfo.this.mPieMenu.invalidate();
                                }
                            }
                        });
                this.mAIAnimatorOutIn.setStartDelay(3000L);
                this.mAIAnimatorOutIn.setDuration(3000L);
                this.mAIAnimatorOutIn.setRepeatCount(-1);
                this.mAIAnimatorOutIn.setInterpolator(new LinearInterpolator());
                this.mAIAnimatorOutIn.start();
                return;
            }
            this.mClockAnimationIn = 1.0f;
            this.mClockTextPaint.setAlpha(Color.alpha(this.mColorClock));
            this.mAIAnimationIn = 1.0f;
            this.mAITextPaint.setAlpha(Color.alpha(this.mColorNotifications));
            this.mBackgroundPaint.setAlpha(200);
            if (onTheRight) {
                return;
            }
            TimerTask timerTask = new TimerTask() { // from class: com.noname81.lmt.PieStatusInfo.4
                        @Override // java.util.TimerTask, java.lang.Runnable
                        public void run() {
                            PieStatusInfo.this.fillAdditionalInfoStrings();
                            PieStatusInfo.this.mPieMenu.postInvalidate();
                        }
                    };
            Timer timer2 = new Timer();
            this.mAnimationTimer = timer2;
            timer2.schedule(timerTask, 3000L, 3000L);
        }
    }

    @Override // com.noname81.lmt.PieMenu.PieView
    public boolean drawBackground(Canvas canvas) {
        canvas.drawPaint(this.mBackgroundPaint);
        return false;
    }

    @Override // com.noname81.lmt.PieMenu.PieView
    public boolean drawForeground(Canvas canvas) {
        float direction = this.mOnTheLeft ? -1.0f : 1.0f;
        this.mClockTextPaint.setTextSize(this.mClockTextSizeBig);
        canvas.drawTextOnPath(
                this.mClockStrings[0],
                this.mClockPath,
                (this.mClockAnimationIn - 1.0f) * direction * this.mClockTextSizeBig,
                -this.mRadiusOffset,
                this.mClockTextPaint);
        this.mClockTextPaint.setTextSize(this.mClockTextSizeSmall);
        String clockText = this.mClockStrings[1];
        Path path = this.mClockPath;
        float offset = direction * (-this.mPositionOffset);
        float clockAnimation = this.mClockAnimationIn - 1.0f;
        int clockTextSize = this.mClockTextSizeBig;
        canvas.drawTextOnPath(
                clockText,
                path,
                offset,
                (clockAnimation * clockTextSize) - clockTextSize,
                this.mClockTextPaint);
        float aiAnimation = (this.mAnimationTime > 80 ? this.mAIAnimationInOut : 0.0f) * 400.0f;
        canvas.drawTextOnPath(
                this.mAIStrings[0],
                this.mAIPath,
                direction * (this.mPositionOffset + aiAnimation),
                ((-this.mAITextSize) * 2.4f) - this.mRadiusOffset,
                this.mAITextPaint);
        canvas.drawTextOnPath(
                this.mAIStrings[1],
                this.mAIPath,
                direction * (this.mPositionOffset + aiAnimation),
                ((-this.mAITextSize) * 1.2f) - this.mRadiusOffset,
                this.mAITextPaint);
        canvas.drawTextOnPath(
                this.mAIStrings[2],
                this.mAIPath,
                direction * (this.mPositionOffset + aiAnimation),
                -this.mRadiusOffset,
                this.mAITextPaint);
        return true;
    }

    @Override // com.noname81.lmt.PieMenu.PieView
    public boolean onTouchEvent(
            int action, float x, float y, float deltaX, float deltaY, PointF polar) {
        if (2 == action) {
            if (polar.y > this.mRadius + (this.mRadiusInc * this.mLevels)
                    && (this.mClockAnimationIn >= 1.0f || !this.mAnimationActive)) {
                if (this.mAnimationActive && this.mAIAnimatorOutIn.getRepeatCount() == -1) {
                    if (this.mAIAnimationIn < 1.0f && this.mAIAnimatorOutIn.isStarted()) {
                        this.mAIAnimatorOutIn.setRepeatCount(0);
                        this.mAIAnimatorOutIn.end();
                    } else if (this.mAIAnimationIn >= 1.0f) {
                        this.mAIAnimatorOutIn.setRepeatCount(0);
                    }
                }
                if (isClockArea(polar)) {
                    Paint paint = this.mClockTextPaint;
                    paint.setColor(
                            Color.argb(
                                    paint.getAlpha(),
                                    Color.red(this.mColorSelected),
                                    Color.green(this.mColorSelected),
                                    Color.blue(this.mColorSelected)));
                    Paint paint2 = this.mAITextPaint;
                    paint2.setColor(
                            Color.argb(
                                    paint2.getAlpha(),
                                    Color.red(this.mColorNotifications),
                                    Color.green(this.mColorNotifications),
                                    Color.blue(this.mColorNotifications)));
                    if (!this.mClockSelected) {
                        this.mPieMenu.invalidate();
                    }
                    this.mClockSelected = true;
                    this.mAISelected = false;
                    return true;
                }
                Paint paint3 = this.mAITextPaint;
                paint3.setColor(
                        Color.argb(
                                paint3.getAlpha(),
                                Color.red(this.mColorSelected),
                                Color.green(this.mColorSelected),
                                Color.blue(this.mColorSelected)));
                Paint paint4 = this.mClockTextPaint;
                paint4.setColor(
                        Color.argb(
                                paint4.getAlpha(),
                                Color.red(this.mColorClock),
                                Color.green(this.mColorClock),
                                Color.blue(this.mColorClock)));
                if (!this.mAISelected) {
                    this.mPieMenu.invalidate();
                }
                this.mAISelected = true;
                this.mClockSelected = false;
                return true;
            }
            if (this.mAnimationActive && !this.mAIAnimatorOutIn.isStarted()) {
                this.mAIAnimatorOutIn.setRepeatCount(-1);
                this.mAIAnimatorOutIn.setStartDelay(0L);
                this.mAIAnimatorOutIn.start();
            }
            Paint paint5 = this.mClockTextPaint;
            paint5.setColor(
                    Color.argb(
                            paint5.getAlpha(),
                            Color.red(this.mColorClock),
                            Color.green(this.mColorClock),
                            Color.blue(this.mColorClock)));
            Paint paint6 = this.mAITextPaint;
            paint6.setColor(
                    Color.argb(
                            paint6.getAlpha(),
                            Color.red(this.mColorNotifications),
                            Color.green(this.mColorNotifications),
                            Color.blue(this.mColorNotifications)));
            if (this.mClockSelected || this.mAISelected) {
                this.mPieMenu.invalidate();
            }
            this.mClockSelected = false;
            this.mAISelected = false;
        } else if (1 == action
                && polar.y > this.mRadius + (this.mRadiusInc * this.mLevels)
                && (this.mClockAnimationIn >= 1.0f || !this.mAnimationActive)) {
            if (isClockArea(polar)) {
                this.mLauncher.doOpenNotificationBar();
                return true;
            }
            int content = this.mAIContent;
            if (content == 1) {
                this.mLauncher.doOpenQuickSettings();
                return true;
            }
            if (content <= 1) {
                return true;
            }
            PendingIntent notificationDataStartIntent =
                    this.mNotificationDataHelper.getNotificationDataStartIntent(content - 2);
            if (notificationDataStartIntent != null) {
                try {
                    notificationDataStartIntent.send();
                } catch (Exception unused) {
                }
            }
            this.mNotificationDataHelper.removeNotificationData(this.mAIContent - 2);
            return true;
        }
        return false;
    }

    private boolean isClockArea(PointF pointF) {
        if (this.mOnTheBottom) {
            if (pointF.x > 0.0f) {
                return true;
            }
            return false;
        }
        if (pointF.x > 1.5707963267948966d) {
            return true;
        }
        return false;
    }

    /** Rebuilds the clock and info arcs for the current ring geometry. */
    private void makePath(float sweep) {
        int radius = this.mRadius;
        int radiusInc = this.mRadiusInc;
        float outerRadius = (this.mLevels * radiusInc) + radius;
        float innerRadius = radius + radiusInc;
        this.mClockPath.reset();
        this.mAIPath.reset();
        if (this.mOnTheLeft) {
            RectF rectF =
                    new RectF(
                            this.mCenter.x - outerRadius,
                            this.mCenter.y - outerRadius,
                            this.mCenter.x + outerRadius,
                            this.mCenter.y + outerRadius);
            RectF rectF2 =
                    new RectF(
                            this.mCenter.x - outerRadius,
                            this.mCenter.y - outerRadius,
                            mix(
                                    this.mCenter.x + outerRadius,
                                    this.mCenter.x + (innerRadius * 2.0f),
                                    sweep),
                            mix(this.mCenter.y + outerRadius, this.mCenter.y - outerRadius, sweep));
            this.mClockPath.arcTo(rectF, 0.0f, 180.0f);
            this.mAIPath.arcTo(rectF2, 180.0f, 180.0f);
            this.mClockTextPaint.setTextAlign(Paint.Align.LEFT);
            this.mAITextPaint.setTextAlign(Paint.Align.RIGHT);
            return;
        }
        if (this.mOnTheBottom) {
            RectF rectF3 =
                    new RectF(
                            this.mCenter.x - outerRadius,
                            this.mCenter.y - outerRadius,
                            this.mCenter.x + outerRadius,
                            this.mCenter.y + outerRadius);
            float doubleOuter = 2.0f * outerRadius;
            RectF rectF4 =
                    new RectF(
                            this.mCenter.x - outerRadius,
                            mix(this.mCenter.y - outerRadius, this.mCenter.y - doubleOuter, sweep),
                            mix(
                                    this.mCenter.x + outerRadius,
                                    this.mCenter.x + (3.0f * outerRadius),
                                    sweep),
                            mix(this.mCenter.y + outerRadius, this.mCenter.y - doubleOuter, sweep));
            float fMix = mix(270.0f, 225.0f, sweep);
            this.mClockPath.arcTo(rectF3, 90.0f, 180.0f);
            this.mAIPath.arcTo(rectF4, fMix, 180.0f);
            this.mClockTextPaint.setTextAlign(Paint.Align.RIGHT);
            this.mAITextPaint.setTextAlign(Paint.Align.LEFT);
            return;
        }
        RectF rectF5 =
                new RectF(
                        this.mCenter.x - outerRadius,
                        this.mCenter.y - outerRadius,
                        this.mCenter.x + outerRadius,
                        this.mCenter.y + outerRadius);
        RectF rectF6 =
                new RectF(
                        mix(
                                this.mCenter.x - outerRadius,
                                this.mCenter.x - (innerRadius * 2.0f),
                                sweep),
                        this.mCenter.y - outerRadius,
                        mix(
                                this.mCenter.x + outerRadius,
                                this.mCenter.x + (2.0f * outerRadius),
                                sweep),
                        mix(this.mCenter.y + outerRadius, this.mCenter.y - outerRadius, sweep));
        this.mClockPath.arcTo(rectF5, 0.0f, 180.0f);
        this.mAIPath.arcTo(rectF6, 180.0f, 180.0f);
        this.mClockTextPaint.setTextAlign(Paint.Align.RIGHT);
        this.mAITextPaint.setTextAlign(Paint.Align.LEFT);
    }

    private float mix(float from, float to, float fraction) {
        float fAbs = Math.abs((from - to) * fraction);
        return from < to ? from + fAbs : from - fAbs;
    }

    private void fillClockStrings() {
        Calendar calendar = Calendar.getInstance();
        if (this.mTwentyFour == 24) {
            this.mClockStrings[0] =
                    String.format(
                            "%02d:%02d",
                            Integer.valueOf(calendar.get(11)), Integer.valueOf(calendar.get(12)));
            this.mClockStrings[1] = "24h";
            return;
        }
        String[] strArr = this.mClockStrings;
        Object[] objArr = new Object[2];
        objArr[0] = Integer.valueOf(calendar.get(10) == 0 ? 12 : calendar.get(10));
        objArr[1] = Integer.valueOf(calendar.get(12));
        strArr[0] = String.format("%02d:%02d", objArr);
        this.mClockStrings[1] = calendar.get(9) == 0 ? "AM" : "PM";
    }

    private void fillAdditionalInfoStrings() {
        Calendar calendar = Calendar.getInstance();
        int content = this.mAIContent;
        if (content == 0) {
            fillOverview(calendar);
            this.mAIContent = 1;
            return;
        }
        if (content == 1) {
            fillNotification(calendar, 0);
            this.mAIContent = 2;
            return;
        }
        if (content == 2 && this.mNotificationDataHelper.getNotificationDataSize() > 1) {
            fillNotification(calendar, 1);
            this.mAIContent = 3;
            return;
        }
        if (this.mAIContent == 3 && this.mNotificationDataHelper.getNotificationDataSize() > 2) {
            fillNotification(calendar, 2);
            this.mAIContent = 4;
            return;
        }
        if (this.mAIContent == 4 && this.mNotificationDataHelper.getNotificationDataSize() > 3) {
            fillNotification(calendar, 3);
            this.mAIContent = 5;
        } else if (this.mAIContent == 5
                && this.mNotificationDataHelper.getNotificationDataSize() > 4) {
            fillNotification(calendar, 4);
            this.mAIContent = 6;
        } else {
            fillOverview(calendar);
            this.mAIContent = 1;
        }
    }

    private void fillOverview(Calendar calendar) {
        String dateFormat = this.mDateFormat;
        if (dateFormat == null || !dateFormat.equals("dMy")) {
            String pattern = this.mDateFormat;
            if (pattern == null || !pattern.equals("yMd")) {
                this.mAIStrings[0] =
                        String.format(
                                "%02d | %02d | %04d",
                                Integer.valueOf(calendar.get(2) + 1),
                                Integer.valueOf(calendar.get(5)),
                                Integer.valueOf(calendar.get(1)));
            } else {
                this.mAIStrings[0] =
                        String.format(
                                "%04d | %02d | %02d",
                                Integer.valueOf(calendar.get(1)),
                                Integer.valueOf(calendar.get(2) + 1),
                                Integer.valueOf(calendar.get(5)));
            }
        } else {
            this.mAIStrings[0] =
                    String.format(
                            "%02d | %02d | %04d",
                            Integer.valueOf(calendar.get(5)),
                            Integer.valueOf(calendar.get(2) + 1),
                            Integer.valueOf(calendar.get(1)));
        }
        Intent intentRegisterReceiver =
                this.mContext.registerReceiver(
                        null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        this.mAIStrings[1] =
                String.format(
                        "Battery %02d%% | %s",
                        Integer.valueOf(
                                (intentRegisterReceiver.getIntExtra("level", 0) * 100)
                                        / intentRegisterReceiver.getIntExtra("scale", 100)),
                        getRingerModeString());
        this.mAIStrings[2] = getConnectivityString();
    }

    private void fillNotification(Calendar calendar, int index) {
        if (this.mNotificationDataHelper.getNotificationDataSize() > index) {
            calendar.setTimeInMillis(this.mNotificationDataHelper.getNotificationDataTime(index));
            if (this.mTwentyFour == 24) {
                this.mAIStrings[0] =
                        String.format(
                                "%02d:%02d",
                                Integer.valueOf(calendar.get(11)),
                                Integer.valueOf(calendar.get(12)));
            } else {
                String[] strArr = this.mAIStrings;
                Object[] objArr = new Object[3];
                objArr[0] = Integer.valueOf(calendar.get(10));
                objArr[1] = Integer.valueOf(calendar.get(12));
                objArr[2] = calendar.get(9) == 0 ? "AM" : "PM";
                strArr[0] = String.format("%02d:%02d %s", objArr);
            }
            this.mAIStrings[1] = this.mNotificationDataHelper.getNotificationDataName(index);
            if (this.mAIStrings[1].length() > 25) {
                this.mAIStrings[1] = this.mAIStrings[1].substring(0, 24) + "...";
            }
            this.mAIStrings[2] = this.mNotificationDataHelper.getNotificationDataText(index);
            if (this.mAIStrings[2].length() > 25) {
                this.mAIStrings[2] = this.mAIStrings[2].substring(0, 24) + "...";
                return;
            }
            return;
        }
        String[] strArr2 = this.mAIStrings;
        strArr2[0] = "No notifications";
        strArr2[1] = getCPUInfo();
        this.mAIStrings[2] = getMemInfo();
    }

    private String getConnectivityString() {
        NetworkInfo activeNetworkInfo;
        ConnectivityManager connectivityManager =
                (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (connectivityManager == null
                || (activeNetworkInfo = connectivityManager.getActiveNetworkInfo()) == null) {
            return "Not connected";
        }
        String state = activeNetworkInfo.isConnectedOrConnecting() ? "Connected" : "Not connected";
        if (activeNetworkInfo.getTypeName() != null
                && activeNetworkInfo.getTypeName().length() != 0) {
            state = state + " | " + activeNetworkInfo.getTypeName();
        }
        if (activeNetworkInfo.getType() == 1) {
            WifiManager wifiManager =
                    (WifiManager) this.mContext.getApplicationContext().getSystemService("wifi");
            if (wifiManager.getWifiState() != 3) {
                return state;
            }
            return state + " | " + wifiManager.getConnectionInfo().getRssi() + " dBm";
        }
        if (activeNetworkInfo.getSubtypeName() == null
                || activeNetworkInfo.getSubtypeName().length() == 0) {
            return state;
        }
        return state + " | " + activeNetworkInfo.getSubtypeName();
    }

    private String getRingerModeString() {
        AudioManager audioManager = (AudioManager) this.mContext.getSystemService("audio");
        if (audioManager == null) {
            return "";
        }
        int ringerMode = audioManager.getRingerMode();
        if (ringerMode == 0) {
            return "Ringer | silent";
        }
        if (ringerMode == 1) {
            return "Ringer | vibrate";
        }
        if (ringerMode != 2) {
            return "Ringer";
        }
        return "Ringer | " + audioManager.getStreamVolume(2);
    }

    private String getCPUInfo() {
        String strRunCommandResult =
                this.mRootContext.runCommandResult(
                        "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", false);
        if (strRunCommandResult.length() > 4) {
            strRunCommandResult =
                    strRunCommandResult.substring(0, strRunCommandResult.length() - 4) + "MHz | ";
        }
        String strReplace =
                this.mRootContext
                        .runCommandResult("cat /sys/devices/system/cpu/online", false)
                        .replace("\n", "");
        if (strRunCommandResult.length() <= 0) {
            return strRunCommandResult;
        }
        return strRunCommandResult + "Core " + strReplace + " active";
    }

    private String getMemInfo() {
        ActivityManager activityManager =
                (ActivityManager) this.mContext.getSystemService("activity");
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        if (Build.VERSION.SDK_INT >= 16) {
            return "Used "
                    + ((memoryInfo.totalMem - memoryInfo.availMem) / (1024L * 1024L))
                    + " MB | Free "
                    + (memoryInfo.availMem / (1024L * 1024L))
                    + " MB";
        }
        return "Free " + (memoryInfo.availMem / (1024L * 1024L)) + " MB";
    }

    private void handleSensorListener(boolean enable) {
        if (this.mAnimationActive && this.mSensorActivated) {
            if (enable) {
                SensorManager sensorManager = this.mSensorManager;
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(1), 3);
                this.mSensorLastUpdate = System.currentTimeMillis() + 1000;
            } else {
                this.mSensorManager.unregisterListener(this);
                this.mSensorAnimator.cancel();
                this.mSensorAnimation = 0.0f;
                this.mAITextPaint.setTextSize(this.mAITextSize);
            }
        }
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == 1) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (jCurrentTimeMillis - this.mSensorLastUpdate < 400) {
                return;
            }
            float[] fArr = this.mSensorGravity;
            // Low-pass filter isolating gravity from the raw accelerometer, one
            // axis at a time; alpha is the same coefficient for all three.
            float alphaX = this.mSensorAlpha;
            fArr[0] = (fArr[0] * alphaX) + ((1.0f - alphaX) * sensorEvent.values[0]);
            float[] fArr2 = this.mSensorGravity;
            float alphaY = this.mSensorAlpha;
            fArr2[1] = (fArr2[1] * alphaY) + ((1.0f - alphaY) * sensorEvent.values[1]);
            float[] fArr3 = this.mSensorGravity;
            float alphaZ = this.mSensorAlpha;
            fArr3[2] = (fArr3[2] * alphaZ) + ((1.0f - alphaZ) * sensorEvent.values[2]);
            this.mSensorLinearAcceleration[0] = sensorEvent.values[0] - this.mSensorGravity[0];
            this.mSensorLinearAcceleration[1] = sensorEvent.values[1] - this.mSensorGravity[1];
            this.mSensorLinearAcceleration[2] = sensorEvent.values[2] - this.mSensorGravity[2];
            if (Math.abs(this.mSensorLinearAcceleration[0]) > 4.903325f) {
                this.mAIAnimatorOutIn.setStartDelay(0L);
                this.mAIAnimatorOutIn.end();
                this.mAIAnimatorOutIn.start();
                this.mAIAnimationInOut = 0.0f;
                this.mAIAnimationInOutReversed = false;
                this.mAITextPaint.setAlpha(Color.alpha(this.mColorNotifications));
                this.mNotificationDataHelper.removeNotificationData(this.mAIContent - 2);
                this.mSensorLastUpdate = jCurrentTimeMillis;
            }
            if (jCurrentTimeMillis - this.mSensorLastUpdate >= ((long) this.mAnimationTime) * 12
                    && Math.abs(this.mSensorLinearAcceleration[2]) > 4.903325f) {
                ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
                this.mSensorAnimator = valueAnimatorOfFloat;
                valueAnimatorOfFloat.addUpdateListener(
                        new ValueAnimator.AnimatorUpdateListener() { // from class:
                            // com.noname81.lmt.PieStatusInfo.5
                            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                float animatedFraction = valueAnimator.getAnimatedFraction();
                                PieStatusInfo.this.makePath(animatedFraction);
                                PieStatusInfo.this.mSensorAnimation = animatedFraction;
                                PieStatusInfo.this.mAITextPaint.setTextSize(
                                        PieStatusInfo.this.mAITextSize
                                                + ((PieStatusInfo.this.mAITextSize
                                                                * animatedFraction)
                                                        / 6.0f));
                                PieStatusInfo.this.mPieMenu.invalidate();
                            }
                        });
                this.mSensorAnimator.setDuration(((long) this.mAnimationTime) * 12);
                if (this.mSensorAnimation < 0.5f) {
                    this.mSensorAnimator.start();
                } else {
                    this.mSensorAnimator.reverse();
                }
                this.mAIAnimatorOutIn.setStartDelay(((long) this.mAnimationTime) * 24);
                this.mAIAnimatorOutIn.end();
                this.mAIAnimatorOutIn.start();
                this.mAIAnimationInOut = 0.0f;
                this.mAITextPaint.setAlpha(Color.alpha(this.mColorNotifications));
                this.mAIAnimationInOutReversed = false;
                this.mSensorLastUpdate = jCurrentTimeMillis;
            }
        }
    }
}
