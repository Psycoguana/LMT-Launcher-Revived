package com.noname81.lmt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

/**
 * One pie slice drawn the way {@link PieMenu} draws it, as the preview in the pie color editor
 * (ROADMAP 1.13): fill with its radial gradient, outline with its gradient, the shift stroke, and
 * an icon tinted with the icon color, at the user's radii, outline, shift and slice gap sizes. A
 * checkerboard behind it shows how see-through the colors are.
 *
 * <p>Colors come in the color string's order - normal, selected, icon, outline, gradient, gradient
 * outline, shift - with {@link #UNSET} (-2) for a part left empty, which falls back the way {@code
 * PieMenu.init} does.
 */
public class PieTileView extends View {
    static final int UNSET = -2;
    static final int COLOR_COUNT = 7;

    /** One slice of a four-item ring, the shape the Pie tab's rows show. */
    private static final float SWEEP_DEGREES = 45f;

    private final int[] mColors = new int[COLOR_COUNT];
    private boolean mSelected;
    private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mChecker = new Paint();
    private final Path mSlice = new Path();
    private final RectF mRect = new RectF();
    private final Drawable mIcon;
    private final float mInnerPx;
    private final float mRingPx;
    private final float mOutlinePx;
    private final float mShiftPx;
    private final float mGapDegrees;
    private final float mGapPx;

    public PieTileView(Context context) {
        this(context, null);
    }

    public PieTileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        java.util.Arrays.fill(this.mColors, UNSET);
        float density = context.getResources().getDisplayMetrics().density;
        SettingsValues settings = SettingsValues.getInstance(context);
        this.mInnerPx = settings.loadPieInnerRadius() * density;
        this.mRingPx = settings.loadPieOuterRadius() * density;
        this.mOutlinePx = settings.loadPieOutlineSize();
        this.mShiftPx = settings.loadPieShiftSize();
        float sliceGap = settings.loadPieSliceGap() / 10.0f;
        this.mGapDegrees = sliceGap;
        this.mGapPx = (int) (5.0f * sliceGap);
        this.mStroke.setStyle(Paint.Style.STROKE);
        Drawable back = IconUtils.getIconForAction(context, new Action(Action.Back), null);
        this.mIcon = back != null ? back.mutate() : null;
        this.mChecker.setShader(checkerboard(density));
    }

    void setColors(int[] colors, boolean selected) {
        System.arraycopy(colors, 0, this.mColors, 0, Math.min(colors.length, COLOR_COUNT));
        this.mSelected = selected;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float corner = height / 8;
        canvas.drawRoundRect(0, 0, width, height, corner, corner, this.mChecker);

        // Fit the slice into the view, narrow end pointing right like the app icon: the pie's
        // center sits to its right, the outer arc on the left.
        float half = (float) Math.toRadians(SWEEP_DEGREES / 2);
        float outer = this.mInnerPx + this.mRingPx;
        float sliceWidth = outer - this.mInnerPx * (float) Math.cos(half);
        float sliceHeight = 2 * outer * (float) Math.sin(half);
        float pad = height / 10;
        float scale = Math.min((height - 2 * pad) / sliceHeight, (width - 2 * pad) / sliceWidth);
        float cx = (width - sliceWidth * scale) / 2 + outer * scale;
        float cy = height / 2;
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);

        int normal = color(0, ContextCompat.getColor(getContext(), R.color.qc_normal_blue));
        int selected = color(1, darker(normal));
        int icon = color(2, -1);
        int outline = color(3, Color.TRANSPARENT);
        int shift = color(6, ContextCompat.getColor(getContext(), R.color.qc_selected_blue));

        float inner = this.mInnerPx + this.mGapPx;
        float outerEdge = outer - this.mGapPx;
        float sliceHalf = SWEEP_DEGREES / 2 - this.mGapDegrees;
        this.mSlice.reset();
        this.mRect.set(-inner, -inner, inner, inner);
        this.mSlice.arcTo(this.mRect, 180 + sliceHalf, -2 * sliceHalf, true);
        this.mRect.set(-outerEdge, -outerEdge, outerEdge, outerEdge);
        this.mSlice.arcTo(this.mRect, 180 - sliceHalf, 2 * sliceHalf);
        this.mSlice.close();

        // Same order as PieMenu.drawPath: shift stroke, fill offset by half of it, outline.
        if (this.mShiftPx > 0) {
            this.mStroke.setShader(null);
            this.mStroke.setColor(shift);
            this.mStroke.setStrokeWidth(this.mShiftPx);
            canvas.drawPath(this.mSlice, this.mStroke);
            canvas.translate(-this.mShiftPx / 2, -this.mShiftPx / 2);
        }
        this.mFill.setColor(this.mSelected ? selected : normal);
        this.mFill.setShader(
                !this.mSelected && this.mColors[4] != UNSET
                        ? new RadialGradient(
                                0, 0, outer, normal, this.mColors[4], Shader.TileMode.CLAMP)
                        : null);
        canvas.drawPath(this.mSlice, this.mFill);
        if (Color.alpha(outline) > 0 && this.mOutlinePx > 0) {
            this.mStroke.setColor(outline);
            this.mStroke.setStrokeWidth(this.mOutlinePx);
            this.mStroke.setShader(
                    this.mColors[5] != UNSET
                            ? new RadialGradient(
                                    0, 0, outer, outline, this.mColors[5], Shader.TileMode.CLAMP)
                            : null);
            canvas.drawPath(this.mSlice, this.mStroke);
        }

        // The icon sits two thirds out in the ring, 35 dp like the pie's scaled images.
        if (this.mIcon != null) {
            float iconSize = 35 * getResources().getDisplayMetrics().density;
            float at = (outer - this.mInnerPx) * 2 / 3 + this.mInnerPx;
            int left = (int) (-at - iconSize / 2);
            int top = (int) (-iconSize / 2);
            this.mIcon.setBounds(left, top, left + (int) iconSize, top + (int) iconSize);
            if (icon != 0) {
                this.mIcon.setColorFilter(icon, PorterDuff.Mode.MULTIPLY);
            } else {
                this.mIcon.clearColorFilter();
            }
            this.mIcon.draw(canvas);
        }
        canvas.restore();
    }

    private int color(int index, int fallback) {
        return this.mColors[index] != UNSET ? this.mColors[index] : fallback;
    }

    /** PieMenu's default selected color: the normal one at 80% brightness, opaque. */
    private static int darker(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    private static BitmapShader checkerboard(float density) {
        int cell = Math.max(1, (int) (8 * density));
        Bitmap bitmap = Bitmap.createBitmap(cell * 2, cell * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFF9E9E9E);
        Paint dark = new Paint();
        dark.setColor(0xFF616161);
        canvas.drawRect(0, 0, cell, cell, dark);
        canvas.drawRect(cell, cell, cell * 2, cell * 2, dark);
        return new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    }
}
