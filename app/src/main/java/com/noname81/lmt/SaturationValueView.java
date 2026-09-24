package com.noname81.lmt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 * The square of a hue/saturation/value picker (ROADMAP 1.10's custom theme color): saturation
 * grows to the right, value (brightness) grows upwards, for the hue set with {@link #setHue}.
 */
public class SaturationValueView extends View {
    interface OnChanged {
        void changed(float saturation, float value);
    }

    private final Paint mSaturationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mThumbFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mThumbRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mThumbEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBounds = new RectF();
    private final Path mClip = new Path();
    private final float mCorner;
    private final float mThumbRadius;
    private float mHue;
    private float mSaturation = 1f;
    private float mValue = 1f;
    private OnChanged mOnChanged;

    public SaturationValueView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCorner = dp(16);
        this.mThumbRadius = dp(10);
        this.mThumbRing.setStyle(Paint.Style.STROKE);
        this.mThumbRing.setStrokeWidth(dp(3));
        this.mThumbRing.setColor(Color.WHITE);
        this.mThumbEdge.setStyle(Paint.Style.STROKE);
        this.mThumbEdge.setStrokeWidth(dp(1));
        this.mThumbEdge.setColor(0x66000000);
    }

    void setOnChanged(OnChanged onChanged) {
        this.mOnChanged = onChanged;
    }

    void setHue(float hue) {
        this.mHue = hue;
        updateShaders();
        invalidate();
    }

    void setSaturationValue(float saturation, float value) {
        this.mSaturation = saturation;
        this.mValue = value;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Inset by the thumb radius so the thumb stays whole at the edges.
        this.mBounds.set(this.mThumbRadius, this.mThumbRadius, w - this.mThumbRadius, h - this.mThumbRadius);
        this.mClip.reset();
        this.mClip.addRoundRect(this.mBounds, this.mCorner, this.mCorner, Path.Direction.CW);
        updateShaders();
    }

    private void updateShaders() {
        if (this.mBounds.isEmpty()) {
            return;
        }
        int pure = Color.HSVToColor(new float[] {this.mHue, 1f, 1f});
        this.mSaturationPaint.setShader(new LinearGradient(
                this.mBounds.left, 0, this.mBounds.right, 0, Color.WHITE, pure, Shader.TileMode.CLAMP));
        this.mValuePaint.setShader(new LinearGradient(
                0, this.mBounds.top, 0, this.mBounds.bottom, Color.TRANSPARENT, Color.BLACK,
                Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.clipPath(this.mClip);
        canvas.drawRect(this.mBounds, this.mSaturationPaint);
        canvas.drawRect(this.mBounds, this.mValuePaint);
        canvas.restore();

        float x = this.mBounds.left + this.mSaturation * this.mBounds.width();
        float y = this.mBounds.top + (1f - this.mValue) * this.mBounds.height();
        this.mThumbFill.setColor(Color.HSVToColor(new float[] {this.mHue, this.mSaturation, this.mValue}));
        canvas.drawCircle(x, y, this.mThumbRadius, this.mThumbFill);
        canvas.drawCircle(x, y, this.mThumbRadius, this.mThumbRing);
        canvas.drawCircle(x, y, this.mThumbRadius + dp(1.5f), this.mThumbEdge);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // Keep the dialog's scroll container from stealing vertical drags.
                getParent().requestDisallowInterceptTouchEvent(true);
                // fall through
            case MotionEvent.ACTION_MOVE:
                this.mSaturation = clamp((event.getX() - this.mBounds.left) / this.mBounds.width());
                this.mValue = 1f - clamp((event.getY() - this.mBounds.top) / this.mBounds.height());
                invalidate();
                if (this.mOnChanged != null) {
                    this.mOnChanged.changed(this.mSaturation, this.mValue);
                }
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private static float clamp(float f) {
        return Math.max(0f, Math.min(1f, f));
    }

    private float dp(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
