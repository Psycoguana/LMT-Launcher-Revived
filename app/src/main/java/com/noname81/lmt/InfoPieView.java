package com.noname81.lmt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.material.color.MaterialColors;

/**
 * The Info tab header's pie (ROADMAP 1.18): four slices of a ring coming in from the card's right
 * edge like the real pie from the screen's, one of them in the accent color. Purely decorative.
 */
public class InfoPieView extends View {
    private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGap = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mSlice = new Path();
    private final RectF mOuter = new RectF();
    private final RectF mInner = new RectF();

    public InfoPieView(Context context) {
        this(context, null);
    }

    public InfoPieView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mGap.setStyle(Paint.Style.STROKE);
        this.mGap.setStrokeWidth(3 * context.getResources().getDisplayMetrics().density);
    }

    /** As tall as the card's text: it asks for no height of its own and fills what it gets. */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height =
                MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY
                        ? MeasureSpec.getSize(heightMeasureSpec)
                        : 0;
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec), height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth();
        float cy = getHeight() / 2f;
        float outer = Math.min(getHeight() * 0.55f, getWidth() * 0.68f);
        float inner = outer * 0.44f;
        this.mOuter.set(cx - outer, cy - outer, cx + outer, cy + outer);
        this.mInner.set(cx - inner, cy - inner, cx + inner, cy + inner);
        this.mGap.setColor(
                MaterialColors.getColor(
                        this, com.google.android.material.R.attr.colorSurfaceContainerHigh));
        int[] colors = {
            com.google.android.material.R.attr.colorSurfaceContainerHighest,
            com.google.android.material.R.attr.colorSecondaryContainer,
            com.google.android.material.R.attr.colorPrimary,
            com.google.android.material.R.attr.colorSurfaceContainerHighest
        };
        // Four 45 degree slices from straight up (270) round the left side to straight down.
        for (int i = 0; i < 4; i++) {
            float start = 270 - 45 * (i + 1);
            this.mSlice.reset();
            this.mSlice.arcTo(this.mOuter, start, 45, true);
            this.mSlice.arcTo(this.mInner, start + 45, -45);
            this.mSlice.close();
            this.mFill.setColor(MaterialColors.getColor(this, colors[i]));
            canvas.drawPath(this.mSlice, this.mFill);
            canvas.drawPath(this.mSlice, this.mGap);
        }
    }
}
