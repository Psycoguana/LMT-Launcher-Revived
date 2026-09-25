package com.noname81.lmt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

/**
 * The pie pointer drawn the way {@link PiePointer} draws it, as the preview in the pointer color
 * editor (ROADMAP 1.13): the line from the pie's center, fading in with the pointer's radial
 * gradient, the circle around the center and the tip circle, each over its white shadow. The pie
 * sits on the right edge, like the pie color editor's slice.
 *
 * <p>Colors: pointer.
 */
public class PiePointerPreviewView extends View implements ColorPreview {
    /** Same stroke, tip size and shadow offset as PiePointer, in pixels. */
    private static final float STROKE = 4f;

    private static final float TIP = 16f;
    private static final float SHADOW = 4f;

    private int mColor = UNSET;
    private final Paint mPointer = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mChecker = new Paint();
    private final Path mClip = new Path();
    private final float mWarpFactor;

    public PiePointerPreviewView(Context context) {
        this(context, null);
    }

    public PiePointerPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mWarpFactor = SettingsValues.getInstance(context).loadPiePointerWarpFactor() / 100f;
        this.mPointer.setStyle(Paint.Style.STROKE);
        this.mPointer.setStrokeWidth(STROKE);
        this.mShadow.setStyle(Paint.Style.STROKE);
        this.mShadow.setStrokeWidth(STROKE);
        this.mShadow.setColor(Color.WHITE);
        this.mShadow.setAlpha(100);
        this.mChecker.setShader(
                ColorPreview.checkerboard(context.getResources().getDisplayMetrics().density));
    }

    @Override
    public void setColors(int[] colors, int current) {
        this.mColor = colors.length > 0 ? colors[0] : UNSET;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float corner = height / 8;
        this.mClip.reset();
        this.mClip.addRoundRect(0, 0, width, height, corner, corner, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(this.mClip);
        canvas.drawPaint(this.mChecker);

        int color =
                this.mColor != UNSET
                        ? this.mColor
                        : ContextCompat.getColor(getContext(), R.color.qc_normal_blue);
        float cx = width;
        float cy = height / 2;
        // The tip most of the way across, a little above the center; the circle through it runs
        // off the preview like it runs off the screen.
        float reach = width * 0.75f;
        float y = cy - height / 5;
        float x = cx - (float) Math.sqrt(reach * reach - (cy - y) * (cy - y));
        // PiePointer's gradient ends at the pie's rim; the tip is that far out times the warp.
        this.mPointer.setShader(
                new RadialGradient(
                        cx,
                        cy,
                        reach / Math.max(1f, this.mWarpFactor),
                        color & 0x00FFFFFF,
                        color,
                        Shader.TileMode.CLAMP));

        canvas.drawLine(cx + SHADOW, cy + SHADOW, x + SHADOW, y + SHADOW, this.mShadow);
        canvas.drawCircle(cx + SHADOW, cy + SHADOW, reach, this.mShadow);
        canvas.drawCircle(x + SHADOW, y + SHADOW, TIP, this.mShadow);
        canvas.drawLine(cx, cy, x, y, this.mPointer);
        canvas.drawCircle(cx, cy, reach, this.mPointer);
        canvas.drawCircle(x, y, TIP, this.mPointer);
        canvas.restore();
    }
}
