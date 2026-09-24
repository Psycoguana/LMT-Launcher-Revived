package com.noname81.lmt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/** A horizontal rainbow bar picking a hue from 0 to 360 (ROADMAP 1.10's custom theme color). */
public class HueBarView extends View {
    interface OnChanged {
        void changed(float hue);
    }

    private static final int[] RAINBOW = {
        0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
    };

    private final Paint mTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mThumbFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mThumbRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mThumbEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mTrackBounds = new RectF();
    private final float mThumbRadius;
    private float mHue;
    private OnChanged mOnChanged;

    public HueBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mThumbRadius = dp(12);
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
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float trackHeight = dp(12);
        this.mTrackBounds.set(
                this.mThumbRadius, (h - trackHeight) / 2f, w - this.mThumbRadius, (h + trackHeight) / 2f);
        this.mTrack.setShader(new LinearGradient(
                this.mTrackBounds.left, 0, this.mTrackBounds.right, 0, RAINBOW, null,
                Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = this.mTrackBounds.height() / 2f;
        canvas.drawRoundRect(this.mTrackBounds, r, r, this.mTrack);
        float x = this.mTrackBounds.left + this.mHue / 360f * this.mTrackBounds.width();
        float y = getHeight() / 2f;
        this.mThumbFill.setColor(Color.HSVToColor(new float[] {this.mHue, 1f, 1f}));
        canvas.drawCircle(x, y, this.mThumbRadius, this.mThumbFill);
        canvas.drawCircle(x, y, this.mThumbRadius, this.mThumbRing);
        canvas.drawCircle(x, y, this.mThumbRadius + dp(1.5f), this.mThumbEdge);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                // fall through
            case MotionEvent.ACTION_MOVE:
                float f = (event.getX() - this.mTrackBounds.left) / this.mTrackBounds.width();
                this.mHue = Math.max(0f, Math.min(1f, f)) * 360f;
                invalidate();
                if (this.mOnChanged != null) {
                    this.mOnChanged.changed(this.mHue);
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

    private float dp(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
