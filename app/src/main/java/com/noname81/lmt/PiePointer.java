package com.noname81.lmt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.widget.FrameLayout;

class PiePointer implements PieMenu.PieView {
    private static final float EMPTY_ANGLE = 0.19634955f;
    private static final int STROKE_SIZE = 4;
    private boolean mActivated;
    private Point mCenter;
    private final Context mContext;
    private final boolean mFromTheEgdes;
    private int mLevels;
    private final float mMargin;
    private boolean mOnTheBottom;
    private final Paint mPointerPaint;
    private final Paint mPointerPaintOutline;
    private PointF mPolar;
    private int mRadius;
    private int mRadiusInc;
    private final RootContext mRootContext;
    private final SettingsValues mSettings;
    private final float mWarpFactor;
    private float mX;
    private float mXO;
    private float mY;
    private float mYO;

    PiePointer(FrameLayout frameLayout, Context context, int color) {
        this.mContext = context;
        SettingsValues settingsValues = SettingsValues.getInstance(context);
        this.mSettings = settingsValues;
        this.mRootContext = RootContext.getInstance(context);
        Paint paint = new Paint();
        this.mPointerPaint = paint;
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4.0f);
        paint.setColor(color);
        Paint paint2 = new Paint();
        this.mPointerPaintOutline = paint2;
        paint2.setAntiAlias(true);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeWidth(4.0f);
        paint2.setColor(-1);
        paint2.setAlpha(100);
        this.mActivated = false;
        this.mFromTheEgdes = settingsValues.loadPiePointerFromEdges() > 0;
        double dLoadPiePointerWarpFactor = settingsValues.loadPiePointerWarpFactor();
        Double.isNaN(dLoadPiePointerWarpFactor);
        this.mWarpFactor = (float) (dLoadPiePointerWarpFactor / 100.0d);
        this.mMargin =
                ((int)
                                Math.sqrt(
                                        settingsValues.getScreenWidth()
                                                * settingsValues.getScreenHeight()))
                        / 30;
    }

    @Override // com.noname81.lmt.PieMenu.PieView
    public void activate() {
        this.mActivated = true;
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
        this.mCenter = center;
        this.mRadius = radius;
        this.mRadiusInc = radiusInc;
        this.mLevels = levels;
        this.mOnTheBottom = onTheBottom;
        this.mPointerPaint.setShader(
                new RadialGradient(
                        this.mCenter.x,
                        this.mCenter.y,
                        this.mRadius + (this.mRadiusInc * (this.mLevels + 1)),
                        Color.argb(
                                0,
                                Color.red(this.mPointerPaint.getColor()),
                                Color.green(this.mPointerPaint.getColor()),
                                Color.blue(this.mPointerPaint.getColor())),
                        this.mPointerPaint.getColor(),
                        Shader.TileMode.CLAMP));
    }

    @Override // com.noname81.lmt.PieMenu.PieView
    public boolean drawForeground(Canvas canvas) {
        return this.mActivated;
    }

    @Override // com.noname81.lmt.PieMenu.PieView
    public boolean drawBackground(Canvas canvas) {
        Point point;
        if (this.mActivated && (point = this.mCenter) != null && this.mPolar != null) {
            canvas.drawLine(
                    point.x + 4,
                    this.mCenter.y + 4,
                    this.mX + 4.0f,
                    this.mY + 4.0f,
                    this.mPointerPaintOutline);
            canvas.drawCircle(
                    this.mCenter.x + 4,
                    this.mCenter.y + 4,
                    this.mPolar.y,
                    this.mPointerPaintOutline);
            canvas.drawCircle(this.mX + 4.0f, this.mY + 4.0f, 16.0f, this.mPointerPaintOutline);
            canvas.drawLine(this.mCenter.x, this.mCenter.y, this.mX, this.mY, this.mPointerPaint);
            canvas.drawCircle(this.mCenter.x, this.mCenter.y, this.mPolar.y, this.mPointerPaint);
            canvas.drawCircle(this.mX, this.mY, 16.0f, this.mPointerPaint);
        }
        return this.mActivated;
    }

    @Override // com.noname81.lmt.PieMenu.PieView
    public boolean onTouchEvent(
            int action, float x, float y, float deltaX, float deltaY, PointF polar) {
        if (this.mFromTheEgdes && polar.y > this.mRadius) {
            if ((this.mOnTheBottom && polar.x > 1.3744469f) || polar.x < -1.3744469f) {
                this.mActivated = true;
            } else if (!this.mOnTheBottom && (polar.x > 2.9452431f || polar.x < EMPTY_ANGLE)) {
                this.mActivated = true;
            }
        }
        if (2 == action && this.mActivated) {
            this.mX = x + ((this.mWarpFactor - 1.0f) * (x - this.mCenter.x));
            this.mY = y + ((this.mWarpFactor - 1.0f) * (y - this.mCenter.y));
            this.mXO = deltaX;
            this.mYO = deltaY;
            this.mPolar = polar;
            polar.y *= this.mWarpFactor;
            float pointerX = this.mX;
            float marginX = this.mMargin;
            if (pointerX < marginX) {
                this.mX = marginX;
            }
            if (this.mX + this.mXO > this.mSettings.getScreenWidth() - this.mMargin) {
                this.mX = (this.mSettings.getScreenWidth() - this.mXO) - this.mMargin;
            }
            float pointerY = this.mY;
            float marginY = this.mMargin;
            if (pointerY < marginY) {
                this.mY = marginY;
            }
            if (this.mY + this.mYO > this.mSettings.getScreenHeight() - this.mMargin) {
                this.mY = (this.mSettings.getScreenHeight() - this.mYO) - this.mMargin;
            }
            double d = polar.y;
            double d2 = this.mRadius;
            double d3 = this.mRadiusInc;
            Double.isNaN(d3);
            Double.isNaN(d2);
            if (d < d2 + (d3 * 0.9d)) {
                this.mActivated = false;
            }
        }
        return this.mActivated;
    }

    @Override // com.noname81.lmt.PieMenu.PieView
    public void onShrink() {
        PointF pointF;
        if (this.mActivated
                && (pointF = this.mPolar) != null
                && pointF.y > this.mRadius + (this.mRadiusInc * this.mLevels)) {
            new Handler()
                    .postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    sendTap();
                                }
                            },
                            50L);
        }
        this.mActivated = false;
        this.mPolar = null;
    }

    private void sendTap() {
        if (AccessibilityHandler.isAccessibilityAvailable(this.mContext, false)) {
            AccessibilityHandler.performClick(this.mX + this.mXO, this.mY + this.mYO);
            return;
        }
        if (this.mRootContext.isRootAvailable(false)) {
            this.mRootContext.runCommandRemote(
                    "input tap "
                            + ((int) (this.mX + this.mXO))
                            + " "
                            + ((int) (this.mY + this.mYO)),
                    true);
            return;
        }
        AccessibilityHandler.isAccessibilityAvailable(this.mContext, true);
    }
}
