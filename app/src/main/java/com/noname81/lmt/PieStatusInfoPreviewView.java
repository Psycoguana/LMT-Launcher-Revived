package com.noname81.lmt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;
import java.util.Locale;

/**
 * The pie's status infos drawn the way {@link PieStatusInfo} draws them for a pie on the right
 * edge, as the preview in the status info color editor (ROADMAP 1.13): the screen dimmed with the
 * dim color, the clock and its "24h" / "AM" line along the pie's lower arc, and the overview lines
 * along its upper arc (or straight above it, "Enabled horizontal"), in the user's pie font. Scaled
 * down to fit.
 *
 * <p>Colors: clock, notifications, dim.
 */
public class PieStatusInfoPreviewView extends View implements ColorPreview {
    private final int[] mColors = {UNSET, UNSET, UNSET};
    private final Paint mChecker = new Paint();
    private final Paint mDim = new Paint();
    private final Paint mRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mClockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mInfoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mClip = new Path();
    private final Path mClockPath = new Path();
    private final Path mInfoPath = new Path();
    private final RectF mRect = new RectF();
    private final float mInnerPx;
    private final float mRingPx;
    private final float mRadiusOffset;
    private final float mPositionOffset;
    private final float mBigText;
    private final float mSmallText;
    private final boolean mHorizontal;
    private final String[] mClockStrings;
    private final String[] mInfoStrings;

    public PieStatusInfoPreviewView(Context context) {
        this(context, null);
    }

    public PieStatusInfoPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = context.getResources().getDisplayMetrics().density;
        SettingsValues settings = SettingsValues.getInstance(context);
        this.mInnerPx = settings.loadPieInnerRadius() * density;
        this.mRingPx = settings.loadPieOuterRadius() * density;
        this.mRadiusOffset = 10 * density;
        this.mPositionOffset = 5 * density;
        this.mHorizontal = settings.loadPieShowStatusInfos() == 2;

        // PieStatusInfo's fonts: Thin, Light, default, each small / normal / large.
        int font = settings.loadPieFont();
        int size = font % 3;
        this.mBigText = (size == 0 ? 45 : size == 1 ? 65 : 80) * density;
        this.mSmallText = (size == 0 ? 8 : size == 1 ? 13 : 17) * density;
        Typeface typeface = typeface(context, font);
        this.mClockPaint.setTypeface(typeface);
        this.mClockPaint.setTextAlign(Paint.Align.RIGHT);
        this.mInfoPaint.setTypeface(typeface);
        this.mInfoPaint.setTextAlign(Paint.Align.LEFT);
        this.mInfoPaint.setTextSize(this.mSmallText);
        this.mRing.setColor(0x40FFFFFF);
        this.mChecker.setShader(ColorPreview.checkerboard(density));

        Calendar now = Calendar.getInstance();
        boolean twentyFour = DateFormat.is24HourFormat(context);
        int hour = now.get(twentyFour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        this.mClockStrings =
                new String[] {
                    String.format(
                            Locale.US,
                            "%02d:%02d",
                            !twentyFour && hour == 0 ? 12 : hour,
                            now.get(Calendar.MINUTE)),
                    twentyFour ? "24h" : now.get(Calendar.AM_PM) == 0 ? "AM" : "PM"
                };
        this.mInfoStrings =
                new String[] {
                    date(new String(DateFormat.getDateFormatOrder(context)), now),
                    "Battery 85% | Ringer | vibrate",
                    "Connected | WIFI | -55 dBm"
                };
    }

    /** PieStatusInfo's typeface for a "Pie status info font" choice. */
    static Typeface typeface(Context context, int font) {
        try {
            if (font < 3) {
                return Typeface.createFromAsset(context.getAssets(), "Roboto-Thin.ttf");
            }
            if (font < 6) {
                return Typeface.createFromAsset(context.getAssets(), "Roboto-Light.ttf");
            }
        } catch (RuntimeException e) {
            // Missing font: fall back to the system one, as below.
        }
        return Typeface.DEFAULT;
    }

    /** The overview's date line, in PieStatusInfo's order for the phone's date format. */
    private static String date(String order, Calendar now) {
        int day = now.get(Calendar.DAY_OF_MONTH);
        int month = now.get(Calendar.MONTH) + 1;
        int year = now.get(Calendar.YEAR);
        if (order.equals("dMy")) {
            return String.format(Locale.US, "%02d | %02d | %04d", day, month, year);
        }
        if (order.equals("yMd")) {
            return String.format(Locale.US, "%04d | %02d | %02d", year, month, day);
        }
        return String.format(Locale.US, "%02d | %02d | %04d", month, day, year);
    }

    @Override
    public void setColors(int[] colors, int current) {
        System.arraycopy(colors, 0, this.mColors, 0, Math.min(colors.length, this.mColors.length));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float corner = height / 16;
        this.mClip.reset();
        this.mClip.addRoundRect(0, 0, width, height, corner, corner, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(this.mClip);
        canvas.drawPaint(this.mChecker);
        this.mDim.setColor(color(2, Color.argb(200, 0, 0, 0)));
        canvas.drawPaint(this.mDim);

        // The pie's center on the right edge; everything it draws scaled to fit the height. The
        // clock and the info lines stop short of straight down / up: ~90% of their radius.
        float outer = this.mInnerPx + this.mRingPx;
        float extent = (outer + this.mRadiusOffset + this.mBigText) * 0.9f;
        float scale = Math.min(1f, (height / 2 - height / 20) / extent);
        canvas.translate(width, height / 2);
        canvas.scale(scale, scale);

        this.mRing.setStyle(Paint.Style.STROKE);
        this.mRing.setStrokeWidth(this.mRingPx);
        float middle = this.mInnerPx + this.mRingPx / 2;
        canvas.drawCircle(0, 0, middle, this.mRing);

        // PieStatusInfo.makePath for a pie that is neither on the left nor at the bottom.
        this.mRect.set(-outer, -outer, outer, outer);
        this.mClockPath.reset();
        this.mClockPath.arcTo(this.mRect, 0, 180);
        this.mInfoPath.reset();
        if (this.mHorizontal) {
            // The arc's oval flattens into a line above the pie.
            this.mInfoPath.moveTo(-2 * outer, -outer);
            this.mInfoPath.lineTo(2 * outer, -outer);
        } else {
            this.mInfoPath.arcTo(this.mRect, 180, 180);
        }

        // PieStatusInfo.drawForeground with its animations finished.
        this.mClockPaint.setColor(color(0, Color.WHITE));
        this.mClockPaint.setTextSize(this.mBigText);
        canvas.drawTextOnPath(
                this.mClockStrings[0], this.mClockPath, 0, -this.mRadiusOffset, this.mClockPaint);
        this.mClockPaint.setTextSize(this.mSmallText);
        canvas.drawTextOnPath(
                this.mClockStrings[1],
                this.mClockPath,
                -this.mPositionOffset,
                -this.mBigText,
                this.mClockPaint);
        this.mInfoPaint.setColor(color(1, Color.WHITE));
        float[] lines = {2.4f, 1.2f, 0f};
        for (int i = 0; i < lines.length; i++) {
            canvas.drawTextOnPath(
                    this.mInfoStrings[i],
                    this.mInfoPath,
                    this.mPositionOffset,
                    -this.mSmallText * lines[i] - this.mRadiusOffset,
                    this.mInfoPaint);
        }
        canvas.restore();
    }

    private int color(int index, int fallback) {
        return this.mColors[index] != UNSET ? this.mColors[index] : fallback;
    }
}
