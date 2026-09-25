package com.noname81.lmt;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * The radial menu itself: layout, drawing and hit-testing.
 *
 * <p>Slices are arranged in concentric rings, each {@code mRadiusInc} further out. Every item
 * carries its own radii and angular sweep, so {@code findItem} can bracket a touch without knowing
 * how many rings exist — which is why adding the third ring needed no change here.
 *
 * <p>One exception: with "expand trigger area" on, the outermost ring ignores its outer radius so a
 * touch anywhere beyond the pie still selects it. Adding a ring moves that behaviour outwards.
 *
 * <p>Extensions implement {@link PieView} to draw inside the pie and receive its geometry — see
 * {@link PiePointer} and {@link PieStatusInfo}.
 */
class PieMenu extends FrameLayout {
    private static boolean DEBUG = false;
    private static final int MAX_LEVELS = 5;
    private static final String TAG = "LMT::PieMenu";
    private int mAnimationTime;
    private PieBlur mBlur;
    private Point mCenter;
    private int[] mCounts;
    private PieItem mCurrentItem;
    private boolean mExpandTriggerArea;
    private int mIconColor;
    private List<PieItem> mItems;
    private PieItem mLastItem;
    private int mLevels;
    private boolean mLongpress;
    private Handler mLongpressHandler;
    private Runnable mLongpressRunnable;
    private int mNormalGradient;
    private Paint mNormalPaint;
    private boolean mOpen;
    private int mOutlineGradient;
    private Paint mOutlinePaint;
    private PieView mPiePointer;
    private PieBrightness mBrightness;

    /** Pie folders (ROADMAP 1.17): the open folder's slice, its items and the rings before. */
    private PieItem mFolderParent;

    private final List<PieItem> mFolderItems = new ArrayList<>();
    private int mFolderBaseLevels;
    private int mPointerColor;
    private int mRadius;
    private int mRadiusInc;
    private boolean mRotateImages;
    private Paint mSelectedPaint;
    private SettingsValues mSettings;
    private Paint mShiftPaint;
    private int mSlop;
    private PieView mStatusInfo;
    private int mStatusInfoClockColor;
    private int mStatusInfoDimColor;
    private int mStatusInfoNotificationsColor;
    private Toaster mToaster;
    private int mTouchOffset;
    private Rect mWindow;

    public interface PieView {
        void activate();

        boolean drawBackground(Canvas canvas);

        boolean drawForeground(Canvas canvas);

        void layout(
                boolean visible,
                Point center,
                int radius,
                int radiusInc,
                int levels,
                boolean onTheLeft,
                boolean onTheBottom,
                boolean onTheRight);

        void onShrink();

        boolean onTouchEvent(
                int action, float x, float y, float deltaX, float deltaY, PointF polar);
    }

    private float getDegrees(double d) {
        return (float) (270.0d - ((d * 180.0d) / 3.141592653589793d));
    }

    public PieMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public PieMenu(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public PieMenu(Context context) {
        super(context);
        init(context);
    }

    static void setDebug(boolean enabled) {
        DEBUG = enabled;
    }

    private void init(Context context) {
        this.mSettings = SettingsValues.getInstance(context);
        this.mToaster = Toaster.getInstance(context);
        this.mBlur = new PieBlur(context);
        this.mLongpressHandler = new Handler();
        this.mLongpressRunnable = null;
        this.mLongpress = false;
        this.mItems = new ArrayList();
        this.mLevels = 0;
        this.mCounts = new int[5];
        Resources resources = context.getResources();
        this.mRadius =
                (int)
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                this.mSettings.loadPieInnerRadius(),
                                resources.getDisplayMetrics());
        this.mRadiusInc =
                (int)
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                this.mSettings.loadPieOuterRadius(),
                                resources.getDisplayMetrics());
        this.mSlop = this.mSettings.loadPieAreaX();
        this.mTouchOffset = (int) resources.getDimension(R.dimen.qc_touch_offset);
        this.mRotateImages = this.mSettings.loadPieRotateImages() == 1;
        this.mExpandTriggerArea = this.mSettings.loadPieExpandTriggerArea() == 1;
        this.mOpen = false;
        setWillNotDraw(false);
        setDrawingCacheEnabled(false);
        this.mWindow = new Rect();
        this.mCenter = new Point(0, 0);
        Paint paint = new Paint();
        this.mNormalPaint = paint;
        paint.setAntiAlias(true);
        Paint paint2 = new Paint();
        this.mSelectedPaint = paint2;
        paint2.setAntiAlias(true);
        Paint paint3 = new Paint();
        this.mShiftPaint = paint3;
        paint3.setAntiAlias(true);
        this.mShiftPaint.setStyle(Paint.Style.STROKE);
        this.mShiftPaint.setStrokeWidth(this.mSettings.loadPieShiftSize());
        Paint paint4 = new Paint();
        this.mOutlinePaint = paint4;
        paint4.setAntiAlias(true);
        this.mOutlinePaint.setStyle(Paint.Style.STROKE);
        this.mOutlinePaint.setStrokeWidth(this.mSettings.loadPieOutlineSize());
        this.mAnimationTime = this.mSettings.loadPieAnimation();
        String strLoadPieColor = this.mSettings.loadPieColor();
        switch (strLoadPieColor) {
            case "0":
                if (Build.VERSION.SDK_INT >= 31) {
                    this.mNormalPaint.setColor(
                            resources.getColor(android.R.color.system_accent1_500));
                    this.mSelectedPaint.setColor(
                            resources.getColor(android.R.color.system_accent2_700));
                    this.mShiftPaint.setColor(
                            resources.getColor(android.R.color.system_accent2_700));
                    this.mOutlinePaint.setColor(
                            resources.getColor(android.R.color.system_accent2_700));
                    this.mOutlinePaint.setAlpha(0);
                    this.mIconColor = -1;
                    this.mNormalGradient = -2;
                    this.mOutlineGradient = -2;
                    break;
                } else {
                    this.mNormalPaint.setColor(resources.getColor(R.color.qc_normal_blue));
                    this.mSelectedPaint.setColor(resources.getColor(R.color.qc_selected_blue));
                    this.mShiftPaint.setColor(resources.getColor(R.color.qc_selected_blue));
                    this.mOutlinePaint.setColor(resources.getColor(R.color.qc_selected_blue));
                    this.mOutlinePaint.setAlpha(0);
                    this.mIconColor = -1;
                    this.mNormalGradient = -2;
                    this.mOutlineGradient = -2;
                    break;
                }
            case "1":
                this.mNormalPaint.setColor(resources.getColor(R.color.qc_normal_red));
                this.mSelectedPaint.setColor(resources.getColor(R.color.qc_selected_red));
                this.mShiftPaint.setColor(resources.getColor(R.color.qc_selected_red));
                this.mOutlinePaint.setColor(resources.getColor(R.color.qc_selected_white));
                this.mIconColor = -1;
                this.mNormalGradient = -2;
                this.mOutlineGradient = -2;
                break;
            case "2":
                this.mNormalPaint.setColor(resources.getColor(R.color.qc_normal_grey));
                this.mSelectedPaint.setColor(resources.getColor(R.color.qc_selected_grey));
                this.mShiftPaint.setColor(resources.getColor(R.color.qc_selected_grey));
                this.mOutlinePaint.setColor(resources.getColor(R.color.qc_selected_white));
                this.mNormalGradient = -2;
                this.mOutlineGradient = -2;
                break;
            case "3":
                this.mNormalPaint.setColor(resources.getColor(R.color.qc_normal_transparent));
                this.mSelectedPaint.setColor(resources.getColor(R.color.qc_selected_transparent));
                this.mShiftPaint.setColor(resources.getColor(R.color.qc_selected_transparent));
                this.mOutlinePaint.setAlpha(0);
                this.mIconColor = -1;
                this.mNormalGradient = -2;
                this.mOutlineGradient = -2;
                break;
            case "4":
                this.mNormalPaint.setColor(resources.getColor(R.color.qc_normal_white));
                this.mNormalPaint.setShadowLayer(5.0f, 0.0f, 0.0f, -12303292);
                this.mSelectedPaint.setColor(resources.getColor(R.color.qc_selected_white));
                this.mSelectedPaint.setShadowLayer(5.0f, 0.0f, 0.0f, -3355444);
                this.mShiftPaint.setColor(resources.getColor(R.color.qc_selected_white));
                this.mOutlinePaint.setAlpha(0);
                this.mIconColor = -12303292;
                this.mNormalGradient = -2;
                this.mOutlineGradient = -2;
                break;
            case "5":
                this.mNormalPaint.setColor(Color.rgb(190, 35, 90));
                this.mSelectedPaint.setColor(Color.rgb(255, 242, 30));
                this.mShiftPaint.setColor(Color.rgb(255, 242, 30));
                this.mOutlinePaint.setColor(resources.getColor(R.color.qc_selected_white));
                this.mIconColor = -1;
                this.mNormalGradient = -2;
                this.mOutlineGradient = -2;
                break;
            case "6":
                this.mNormalPaint.setColor(Color.argb(127, 0, 0, 0));
                this.mSelectedPaint.setColor(Color.argb(64, 0, 255, 0));
                this.mShiftPaint.setColor(Color.argb(64, 0, 255, 0));
                this.mOutlinePaint.setColor(Color.argb(127, 0, 255, 0));
                this.mIconColor = Color.argb(200, 0, 255, 0);
                this.mNormalGradient = -2;
                this.mOutlineGradient = -2;
                break;
            case "7":
                this.mNormalPaint.setColor(Color.argb(127, 0, 0, 0));
                this.mSelectedPaint.setColor(Color.argb(64, 255, 0, 0));
                this.mShiftPaint.setColor(Color.argb(64, 255, 0, 0));
                this.mOutlinePaint.setColor(Color.argb(127, 255, 0, 0));
                this.mIconColor = Color.argb(200, 255, 0, 0);
                this.mNormalGradient = -2;
                this.mOutlineGradient = -2;
                break;
            default:
                try {
                    String[] strArrSplit = strLoadPieColor.replace(" ", "").split(",");
                    if (strArrSplit.length > 0 && strArrSplit[0].length() != 0) {
                        this.mNormalPaint.setColor(Color.parseColor(strArrSplit[0]));
                    } else {
                        this.mNormalPaint.setColor(resources.getColor(R.color.qc_normal_blue));
                    }
                    if (strArrSplit.length > 1 && strArrSplit[1].length() != 0) {
                        this.mSelectedPaint.setColor(Color.parseColor(strArrSplit[1]));
                    } else {
                        float[] fArr = new float[3];
                        Color.colorToHSV(this.mNormalPaint.getColor(), fArr);
                        fArr[2] = fArr[2] * 0.8f;
                        this.mSelectedPaint.setColor(Color.HSVToColor(fArr));
                    }
                    if (strArrSplit.length > 2 && strArrSplit[2].length() != 0) {
                        this.mIconColor = Color.parseColor(strArrSplit[2]);
                    } else {
                        this.mIconColor = -1;
                    }
                    if (strArrSplit.length > 3 && strArrSplit[3].length() != 0) {
                        this.mOutlinePaint.setColor(Color.parseColor(strArrSplit[3]));
                    } else {
                        this.mOutlinePaint.setAlpha(0);
                    }
                    if (strArrSplit.length > 4 && strArrSplit[4].length() != 0) {
                        this.mNormalGradient = Color.parseColor(strArrSplit[4]);
                    } else {
                        this.mNormalGradient = -2;
                    }
                    if (strArrSplit.length > 5 && strArrSplit[5].length() != 0) {
                        this.mOutlineGradient = Color.parseColor(strArrSplit[5]);
                    } else {
                        this.mOutlineGradient = -2;
                    }
                    if (strArrSplit.length > 6 && strArrSplit[6].length() != 0) {
                        this.mShiftPaint.setColor(Color.parseColor(strArrSplit[6]));
                    } else {
                        this.mShiftPaint.setColor(resources.getColor(R.color.qc_selected_blue));
                    }
                    break;
                } catch (Exception unused) {
                    this.mNormalPaint.setColor(resources.getColor(R.color.qc_normal_blue));
                    this.mSelectedPaint.setColor(resources.getColor(R.color.qc_selected_blue));
                    this.mShiftPaint.setColor(resources.getColor(R.color.qc_selected_blue));
                    this.mIconColor = -1;
                    this.mOutlinePaint.setAlpha(0);
                    this.mIconColor = -1;
                    this.mNormalGradient = -2;
                    this.mOutlineGradient = -2;
                    break;
                }
        }
        String strLoadPieStatusInfoColor = this.mSettings.loadPieStatusInfoColor();
        if (strLoadPieStatusInfoColor.equals("0")) {
            this.mStatusInfoClockColor = -1;
            this.mStatusInfoNotificationsColor = -1;
            this.mStatusInfoDimColor = manipulateColor(this.mNormalPaint.getColor(), 0.2f);
        } else {
            try {
                String[] strArrSplit2 = strLoadPieStatusInfoColor.replace(" ", "").split(",");
                if (strArrSplit2.length > 0 && strArrSplit2[0].length() != 0) {
                    this.mStatusInfoClockColor = Color.parseColor(strArrSplit2[0]);
                } else {
                    this.mStatusInfoClockColor = -1;
                }
                if (strArrSplit2.length > 1 && strArrSplit2[1].length() != 0) {
                    this.mStatusInfoNotificationsColor = Color.parseColor(strArrSplit2[1]);
                } else {
                    this.mStatusInfoNotificationsColor = -1;
                }
                if (strArrSplit2.length > 2 && strArrSplit2[2].length() != 0) {
                    this.mStatusInfoDimColor = Color.parseColor(strArrSplit2[2]);
                } else {
                    this.mStatusInfoDimColor = Color.argb(200, 0, 0, 0);
                }
            } catch (Exception unused2) {
                this.mStatusInfoClockColor = -1;
                this.mStatusInfoNotificationsColor = -1;
                this.mStatusInfoDimColor = Color.argb(200, 0, 0, 0);
            }
        }
        String strLoadPiePointerColor = this.mSettings.loadPiePointerColor();
        if (strLoadPiePointerColor.equals("0")) {
            if (Build.VERSION.SDK_INT >= 31) {
                this.mPointerColor = resources.getColor(android.R.color.system_accent1_700);
            } else {
                this.mPointerColor = resources.getColor(R.color.qc_normal_blue);
            }
        } else {
            try {
                String[] strArrSplit3 = strLoadPiePointerColor.replace(" ", "").split(",");
                if (strArrSplit3.length > 0 && strArrSplit3[0].length() != 0) {
                    this.mPointerColor = Color.parseColor(strArrSplit3[0]);
                } else {
                    this.mPointerColor = resources.getColor(R.color.qc_normal_blue);
                }
            } catch (Exception unused3) {
                this.mPointerColor = resources.getColor(R.color.qc_normal_blue);
            }
        }
        this.mPiePointer = new PiePointer(this, context, this.mPointerColor);
        this.mBrightness = new PieBrightness(context, this.mPointerColor);
        if (this.mSettings.loadPieShowStatusInfos() > 0) {
            this.mStatusInfo =
                    new PieStatusInfo(
                            this,
                            context,
                            this.mStatusInfoClockColor,
                            this.mStatusInfoNotificationsColor,
                            this.mSelectedPaint.getColor(),
                            this.mStatusInfoDimColor);
        }
    }

    public void addItem(PieItem pieItem) {
        this.mItems.add(pieItem);
        int level = pieItem.getLevel();
        this.mLevels = Math.max(this.mLevels, level);
        int[] iArr = this.mCounts;
        iArr[level] = iArr[level] + 1;
    }

    public void removeItem(PieItem pieItem) {
        this.mItems.remove(pieItem);
    }

    public void activatePiePointer() {
        this.mPiePointer.activate();
    }

    int getLevels() {
        return this.mLevels;
    }

    PieItem getOpenFolder() {
        return this.mFolderParent;
    }

    /**
     * Shows a folder's items in a ring one level beyond the outermost, centred on the folder's
     * slice and kept inside the pie's half circle (ROADMAP 1.17, mockup A). The items must have
     * been made with level {@code getLevels() + 1}.
     */
    void openFolder(PieItem parent, List<PieItem> items) {
        if (parent == this.mFolderParent || items.isEmpty()) {
            return;
        }
        closeFolder();
        this.mFolderParent = parent;
        this.mFolderBaseLevels = this.mLevels;
        int level = this.mLevels + 1;
        this.mLevels = level;
        float startGap = this.mSettings.loadPieStartGap() / 100.0f;
        float minAngle = onTheBottom() ? (-1.5707964f) + startGap : startGap;
        float range = (float) (Math.PI - startGap * 2.0f);
        float sliceGap = this.mSettings.loadPieSliceGap() / 10.0f;
        int sliceGapPx = (int) (5.0f * sliceGap);
        int inner = this.mRadius + sliceGapPx + this.mRadiusInc * (level - 1);
        int outer = this.mRadius + this.mRadiusInc - sliceGapPx + this.mRadiusInc * (level - 1);
        int count = items.size();
        float sweep = Math.min(parent.getSweep(), 0.55f);
        if (sweep * count > range) {
            sweep = range / count;
        }
        float start = parent.getStartAngle() + parent.getSweep() / 2.0f - sweep * count / 2.0f;
        start = Math.max(minAngle, Math.min(start, minAngle + range - sweep * count));
        Path path =
                makeSlice(
                        getDegrees(0.0d), getDegrees(sweep), outer, inner, this.mCenter, sliceGap);
        for (PieItem item : items) {
            placeIcon(item.getView(), start + sweep / 2.0f, inner, outer);
            item.setGeometry(start, sweep, inner, outer, path);
            item.setColor(this.mIconColor);
            item.setAlpha(0);
            this.mItems.add(item);
            this.mFolderItems.add(item);
            start += sweep;
        }
        ValueAnimator fade = ValueAnimator.ofFloat(0.0f, 1.0f);
        fade.addUpdateListener(
                animator -> {
                    int alpha = (int) (animator.getAnimatedFraction() * 255.0f);
                    for (PieItem item : items) {
                        item.setAlpha(alpha);
                    }
                    invalidate();
                });
        fade.setDuration(Math.max(1, this.mAnimationTime * 2));
        fade.start();
        this.mBlur.resize(blurRadius());
        invalidate();
    }

    /** The blur disc's radius: out to the outermost ring and its outline. */
    private int blurRadius() {
        return this.mRadius
                + (this.mRadiusInc * this.mLevels)
                + this.mSettings.loadPieOutlineSize();
    }

    /** Takes the open folder's ring away again; hovering another slice or closing does this. */
    void closeFolder() {
        if (this.mFolderParent == null) {
            return;
        }
        if (this.mFolderItems.contains(this.mCurrentItem)) {
            this.mCurrentItem.setSelected(false);
            this.mCurrentItem = null;
        }
        if (this.mFolderItems.contains(this.mLastItem)) {
            this.mLastItem = null;
        }
        this.mItems.removeAll(this.mFolderItems);
        this.mFolderItems.clear();
        this.mLevels = this.mFolderBaseLevels;
        this.mFolderParent = null;
        this.mBlur.resize(blurRadius());
        invalidate();
    }

    /** Centres an item's icon two thirds out in its slice, the way layoutPie does. */
    private void placeIcon(View view, float angle, int inner, int outer) {
        if (view == null) {
            return;
        }
        view.measure(view.getLayoutParams().width, view.getLayoutParams().height);
        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();
        double distance = (((outer - inner) * 2) / 3) + inner;
        int offset = (int) (distance * Math.sin(angle));
        int top = (this.mCenter.y - ((int) (distance * Math.cos(angle)))) - (height / 2);
        int x = onTheLeft() ? this.mCenter.x + offset : this.mCenter.x - offset;
        int left = x - width / 2;
        view.layout(left, top, left + width, top + height);
    }

    /** The Brightness slice is hovered (ROADMAP 1.17). */
    void armBrightness(PieItem slice) {
        this.mBrightness.arm(slice);
    }

    public void clearItems() {
        this.mItems.clear();
        this.mLevels = 0;
        for (int level = 0; level < 5; level++) {
            this.mCounts[level] = 0;
        }
    }

    public void relayoutPie() {
        show(true, true);
    }

    private boolean onTheLeft() {
        return this.mCenter.x - this.mWindow.left <= this.mSlop;
    }

    private boolean onTheRight() {
        return this.mCenter.x >= (this.mWindow.right - this.mWindow.left) - this.mSlop;
    }

    private boolean onTheBottom() {
        return (onTheLeft()
                        || onTheRight()
                        || this.mCenter.y < (this.mWindow.bottom - this.mWindow.top) - this.mSlop)
                ? false
                : true;
    }

    private boolean isValidPosition(int x, int y) {
        return onTheLeft() || onTheRight() || onTheBottom();
    }

    public void show(boolean show, boolean animate) {
        closeFolder();
        this.mBrightness.layout(
                show,
                this.mCenter,
                this.mRadius,
                this.mRadiusInc,
                this.mLevels,
                onTheLeft(),
                onTheBottom(),
                animate);
        if (show) {
            animateOpen();
            layoutPie();
            PieView pieView = this.mPiePointer;
            if (pieView != null) {
                pieView.layout(
                        true,
                        this.mCenter,
                        this.mRadius,
                        this.mRadiusInc,
                        this.mLevels,
                        onTheLeft(),
                        onTheBottom(),
                        animate);
            }
            PieView pieView2 = this.mStatusInfo;
            if (pieView2 != null) {
                pieView2.layout(
                        true,
                        this.mCenter,
                        this.mRadius,
                        this.mRadiusInc,
                        this.mLevels,
                        onTheLeft(),
                        onTheBottom(),
                        animate);
            }
        } else {
            this.mCurrentItem = null;
            this.mLastItem = null;
            PieView pieView3 = this.mPiePointer;
            if (pieView3 != null) {
                pieView3.layout(
                        false,
                        this.mCenter,
                        this.mRadius,
                        this.mRadiusInc,
                        this.mLevels,
                        onTheLeft(),
                        onTheBottom(),
                        animate);
            }
            PieView pieView4 = this.mStatusInfo;
            if (pieView4 != null) {
                pieView4.layout(
                        false,
                        this.mCenter,
                        this.mRadius,
                        this.mRadiusInc,
                        this.mLevels,
                        onTheLeft(),
                        onTheBottom(),
                        animate);
            }
        }
        this.mOpen = show;
        invalidate();
    }

    private void animateOpen() {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        for (PieItem pieItem : PieMenu.this.mItems) {
                            pieItem.setAnimationAngle(
                                    (1.0f - valueAnimator.getAnimatedFraction())
                                            * (-pieItem.getStart()));
                            pieItem.setAlpha((int) (valueAnimator.getAnimatedFraction() * 255.0f));
                        }
                        PieMenu.this.invalidate();
                    }
                });
        valueAnimatorOfFloat.setDuration(this.mAnimationTime * 2);
        valueAnimatorOfFloat.start();
    }

    private void setCenter(int x, int y) {
        this.mCenter.x = x;
        this.mCenter.y = y;
        if (onTheLeft()) {
            this.mCenter.x = 0;
        } else if (onTheRight()) {
            this.mCenter.x = this.mWindow.right - this.mWindow.left;
        } else if (onTheBottom()) {
            this.mCenter.y = this.mWindow.bottom - this.mWindow.top;
            if (this.mSettings.loadPiePos() != 3 && this.mSettings.loadPiePos() != 8) {
                this.mCenter.x = (this.mWindow.right - this.mWindow.left) / 2;
            }
        }
        if (DEBUG) {
            Log.d(
                    TAG,
                    "setCenter "
                            + this.mCenter.x
                            + ","
                            + this.mCenter.y
                            + " inx:"
                            + x
                            + " iny:"
                            + y);
        }
    }

    /** Swaps every slice to its short-press (0) or long-press (1) drawable. */
    private void selectImages(int which) {
        for (int index = 0; index < this.mItems.size(); index++) {
            this.mItems.get(index).selectImage(which);
        }
    }

    private void fireClick(PieItem pieItem) {
        if (this.mSettings.loadPieVibrate() >= 3) {
            this.mToaster.vibratePie();
        }
        if (pieItem == null || pieItem.getView() == null) {
            return;
        }
        if (this.mLongpress) {
            pieItem.getView().performLongClick();
        } else {
            pieItem.getView().performClick();
        }
    }

    private void fireHover(PieItem pieItem) {
        if (pieItem == null || pieItem.getView() == null) {
            return;
        }
        if (this.mLongpress) {
            pieItem.getView().dispatchKeyEvent(new KeyEvent(0, 40));
        } else {
            pieItem.getView().dispatchKeyEvent(new KeyEvent(0, 36));
        }
    }

    private void setLongpressTimer(PointF pointF) {
        if (pointF == null || this.mRadius - this.mTouchOffset > pointF.y) {
            Runnable runnable = this.mLongpressRunnable;
            if (runnable != null) {
                this.mLongpressHandler.removeCallbacks(runnable);
                this.mLongpressRunnable = null;
                this.mLongpress = false;
                selectImages(0);
                postInvalidate();
                return;
            }
            return;
        }
        if (this.mLongpressRunnable == null) {
            Runnable runnable2 =
                    new Runnable() {
                        @Override
                        public void run() {
                            if (PieMenu.this.mSettings.loadPieVibrate() > 0) {
                                PieMenu.this.mToaster.vibratePie();
                            }
                            PieMenu.this.mLongpress = true;
                            PieMenu pieMenu = PieMenu.this;
                            pieMenu.fireHover(pieMenu.mCurrentItem);
                            PieMenu.this.selectImages(1);
                            PieMenu.this.postInvalidate();
                        }
                    };
            this.mLongpressRunnable = runnable2;
            this.mLongpressHandler.postDelayed(runnable2, this.mSettings.loadPieLongpress());
        }
    }

    /**
     * Places every slice: each ring sits {@code mRadiusInc} further out than the last, and the
     * items on it share the half-circle evenly, minus the start and slice gaps.
     */
    private void layoutPie() {
        int nextLevel;
        int iconX;
        int halfWidth;
        float startGap = this.mSettings.loadPieStartGap() / 100.0f;
        float startAngle = onTheBottom() ? (-1.5707964f) + startGap : startGap;
        float sliceGap = this.mSettings.loadPieSliceGap() / 10.0f;
        int sliceGapPx = (int) (5.0f * sliceGap);
        int radius = this.mRadius;
        int firstInner = radius + sliceGapPx;
        int firstOuter = (radius + this.mRadiusInc) - sliceGapPx;
        int outerRadius = firstOuter;
        int innerRadius = firstInner;
        for (int level = 0; level < this.mLevels; level = nextLevel) {
            nextLevel = level + 1;
            double d = startGap * 2.0f;
            float sweep = ((float) (3.141592653589793d - d)) / this.mCounts[nextLevel];
            float halfSweep = sweep / 2.0f;
            Path pathMakeSlice =
                    makeSlice(
                            getDegrees(0.0d),
                            getDegrees(sweep),
                            outerRadius,
                            innerRadius,
                            this.mCenter,
                            sliceGap);
            float angle = startAngle + halfSweep;
            for (PieItem pieItem : this.mItems) {
                if (pieItem.getLevel() == nextLevel) {
                    View view = pieItem.getView();
                    if (view != null) {
                        view.measure(view.getLayoutParams().width, view.getLayoutParams().height);
                        int measuredWidth = view.getMeasuredWidth();
                        int measuredHeight = view.getMeasuredHeight();
                        double d2 = (((outerRadius - innerRadius) * 2) / 3) + innerRadius;
                        double d3 = angle;
                        double dSin = Math.sin(d3);
                        int iconOffset = (int) (d2 * dSin);
                        int centerY = this.mCenter.y;
                        double dCos = Math.cos(d3);
                        int iconTop = (centerY - ((int) (d2 * dCos))) - (measuredHeight / 2);
                        if (onTheLeft()) {
                            iconX = this.mCenter.x + iconOffset;
                            halfWidth = measuredWidth / 2;
                        } else {
                            iconX = this.mCenter.x - iconOffset;
                            halfWidth = measuredWidth / 2;
                        }
                        int iconLeft = iconX - halfWidth;
                        view.layout(
                                iconLeft,
                                iconTop,
                                measuredWidth + iconLeft,
                                measuredHeight + iconTop);
                    }
                    pieItem.setGeometry(
                            angle - halfSweep, sweep, innerRadius, outerRadius, pathMakeSlice);
                    pieItem.setColor(this.mIconColor);
                    angle += sweep;
                } else {
                }
            }
            int radiusInc = this.mRadiusInc;
            innerRadius += radiusInc;
            outerRadius += radiusInc;
        }
        if (this.mNormalGradient != -2) {
            this.mNormalPaint.setShader(
                    new RadialGradient(
                            this.mCenter.x,
                            this.mCenter.y,
                            this.mRadius + (this.mRadiusInc * this.mLevels),
                            this.mNormalPaint.getColor(),
                            this.mNormalGradient,
                            Shader.TileMode.CLAMP));
        }
        if (this.mOutlineGradient != -2) {
            this.mOutlinePaint.setShader(
                    new RadialGradient(
                            this.mCenter.x,
                            this.mCenter.y,
                            this.mRadius + (this.mRadiusInc * this.mLevels),
                            this.mOutlinePaint.getColor(),
                            this.mOutlineGradient,
                            Shader.TileMode.CLAMP));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        PieView pieView;
        PieView pieView2;
        if (this.mOpen) {
            PieView pieView3 = this.mPiePointer;
            boolean zDrawBackground = pieView3 != null ? pieView3.drawBackground(canvas) : false;
            if (!zDrawBackground && (pieView2 = this.mStatusInfo) != null) {
                zDrawBackground = pieView2.drawBackground(canvas);
            }
            if (!zDrawBackground) {
                for (PieItem pieItem : this.mItems) {
                    int iSave = canvas.save();
                    if (onTheLeft()) {
                        canvas.scale(-1.0f, 1.0f);
                    }
                    canvas.rotate(
                            getDegrees(pieItem.getStartAngle()) - 270.0f,
                            this.mCenter.x,
                            this.mCenter.y);
                    drawPath(canvas, pieItem.getPath(), pieItem.isSelected());
                    canvas.restoreToCount(iSave);
                    drawItem(canvas, pieItem);
                }
            }
            this.mBrightness.drawForeground(canvas);
            PieView pieView4 = this.mPiePointer;
            if ((pieView4 != null ? pieView4.drawForeground(canvas) : false)
                    || (pieView = this.mStatusInfo) == null) {
                return;
            }
            pieView.drawForeground(canvas);
        }
    }

    private void drawItem(Canvas canvas, PieItem pieItem) {
        View view = pieItem.getView();
        int iSave = canvas.save();
        if (this.mRotateImages) {
            float degrees =
                    getDegrees(pieItem.getStartAngle() + (pieItem.getSweep() / 2.0f)) - 270.0f;
            canvas.translate(
                    view.getLeft() + (view.getWidth() / 2.0f),
                    view.getTop() + (view.getHeight() / 2.0f));
            if (onTheLeft()) {
                degrees = -degrees;
            }
            canvas.rotate(degrees);
            canvas.translate((-view.getWidth()) / 2.0f, (-view.getHeight()) / 2.0f);
        } else {
            canvas.translate(view.getLeft(), view.getTop());
        }
        view.draw(canvas);
        canvas.restoreToCount(iSave);
    }

    private void drawPath(Canvas canvas, Path path, boolean selected) {
        float strokeWidth = this.mShiftPaint.getStrokeWidth();
        if (strokeWidth > 0.0f) {
            canvas.drawPath(path, this.mShiftPaint);
            float half = (-strokeWidth) / 2.0f;
            canvas.translate(half, half);
        }
        canvas.drawPath(path, selected ? this.mSelectedPaint : this.mNormalPaint);
        if (this.mOutlinePaint.getAlpha() > 0) {
            canvas.drawPath(path, this.mOutlinePaint);
        }
    }

    /** One pie slice: an annulus segment from {@code start} sweeping {@code sweep}. */
    private Path makeSlice(
            float start, float sweep, int inner, int outer, Point center, float gap) {
        RectF rectF =
                new RectF(center.x - inner, center.y - inner, center.x + inner, center.y + inner);
        RectF rectF2 =
                new RectF(center.x - outer, center.y - outer, center.x + outer, center.y + outer);
        Path path = new Path();
        float sweepStart = start - gap;
        float sweepEnd = sweep + gap;
        path.arcTo(rectF, sweepStart, sweepEnd - sweepStart, true);
        path.arcTo(rectF2, sweepEnd, sweepStart - sweepEnd);
        path.close();
        return path;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        PieView pieView;
        PieView pieView2;
        getWindowVisibleDisplayFrame(this.mWindow);
        if (DEBUG) {
            Log.d(
                    TAG,
                    "onTouchEvent base: height: "
                            + getHeight()
                            + ", width: "
                            + getWidth()
                            + ", window:"
                            + this.mWindow);
        }
        float rawX = motionEvent.getRawX() - this.mWindow.left;
        float rawY = motionEvent.getRawY() - this.mWindow.top;
        if (DEBUG) {
            Log.d(
                    TAG,
                    "onTouchEvent patched:"
                            + rawX
                            + ","
                            + rawY
                            + " raw:"
                            + motionEvent.getRawX()
                            + ","
                            + motionEvent.getRawY());
        }
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            int x = (int) rawX;
            int y = (int) rawY;
            setCenter(x, y);
            if (isValidPosition(x, y)) {
                show(true, false);
                if (PieBlur.isSupported()) {
                    this.mBlur.open(
                            this,
                            this.mCenter.x,
                            this.mCenter.y,
                            this.mRadius
                                    + (this.mRadiusInc * this.mLevels)
                                    + this.mSettings.loadPieOutlineSize(),
                            this.mRadius);
                }
                return true;
            }
        } else if (actionMasked == MotionEvent.ACTION_UP) {
            if (this.mOpen) {
                PointF polar = getPolar(rawX, rawY);
                PieView pieView3 = this.mPiePointer;
                // A brightness drag ends here: the value is already set, fire no slice.
                boolean dialing =
                        this.mBrightness.onTouchEvent(
                                actionMasked,
                                rawX,
                                rawY,
                                this.mWindow.left,
                                this.mWindow.top,
                                polar);
                boolean zOnTouchEvent =
                        dialing
                                || (pieView3 != null
                                        && pieView3.onTouchEvent(
                                                actionMasked,
                                                rawX,
                                                rawY,
                                                this.mWindow.left,
                                                this.mWindow.top,
                                                polar));
                if (!zOnTouchEvent && (pieView2 = this.mStatusInfo) != null) {
                    pieView2.onTouchEvent(
                            actionMasked, rawX, rawY, this.mWindow.left, this.mWindow.top, polar);
                }
                PieItem pieItem = this.mCurrentItem;
                deselect();
                show(false, false);
                this.mBlur.close();
                if (!zOnTouchEvent && pieItem != null) {
                    fireClick(pieItem);
                }
                setLongpressTimer(null);
                return true;
            }
        } else if (actionMasked == MotionEvent.ACTION_CANCEL) {
            if (this.mOpen) {
                show(false, false);
                this.mBlur.close();
                deselect();
                return false;
            }
        } else if (actionMasked == MotionEvent.ACTION_MOVE && this.mOpen) {
            PointF polar2 = getPolar(rawX, rawY);
            PieView pieView4 = this.mPiePointer;
            boolean zOnTouchEvent2 =
                    this.mBrightness.onTouchEvent(
                                    actionMasked,
                                    rawX,
                                    rawY,
                                    this.mWindow.left,
                                    this.mWindow.top,
                                    polar2)
                            || (pieView4 != null
                                    && pieView4.onTouchEvent(
                                            actionMasked,
                                            rawX,
                                            rawY,
                                            this.mWindow.left,
                                            this.mWindow.top,
                                            polar2));
            if (!zOnTouchEvent2 && (pieView = this.mStatusInfo) != null) {
                zOnTouchEvent2 =
                        pieView.onTouchEvent(
                                actionMasked,
                                rawX,
                                rawY,
                                this.mWindow.left,
                                this.mWindow.top,
                                polar2);
            }
            if (zOnTouchEvent2) {
                deselect();
                setLongpressTimer(null);
                invalidate();
            } else {
                setLongpressTimer(polar2);
                PieItem pieItemFindItem = findItem(polar2, this.mExpandTriggerArea);
                if (this.mCurrentItem != pieItemFindItem) {
                    onEnter(pieItemFindItem);
                    invalidate();
                }
            }
            return true;
        }
        return false;
    }

    public void onShrink() {
        this.mBrightness.onShrink();
        PieView pieView = this.mPiePointer;
        if (pieView != null) {
            pieView.onShrink();
        }
        PieView pieView2 = this.mStatusInfo;
        if (pieView2 != null) {
            pieView2.onShrink();
        }
    }

    private void onEnter(PieItem pieItem) {
        PieItem pieItem2 = this.mCurrentItem;
        if (pieItem2 != null) {
            pieItem2.setSelected(false);
            this.mLastItem = this.mCurrentItem;
        }
        if (pieItem != null) {
            if (this.mSettings.loadPieVibrate() == 2 || this.mSettings.loadPieVibrate() >= 4) {
                this.mToaster.vibratePie();
            }
            playSoundEffect(0);
            pieItem.setSelected(true);
            fireHover(pieItem);
        }
        if (this.mLastItem == pieItem && this.mSettings.loadPieMultiCommand() > 0) {
            fireClick(pieItem);
        }
        this.mCurrentItem = pieItem;
    }

    private void deselect() {
        PieItem pieItem = this.mCurrentItem;
        if (pieItem != null) {
            pieItem.setSelected(false);
        }
        this.mCurrentItem = null;
    }

    /** Touch point in polar form relative to the pie centre: x is the angle, y the radius. */
    private PointF getPolar(float x, float y) {
        PointF pointF = new PointF();
        pointF.x = 1.5707964f;
        float dx = this.mCenter.x - x;
        if (this.mCenter.x < this.mSlop) {
            dx = -dx;
        }
        float dy = this.mCenter.y - y;
        pointF.y = (float) Math.sqrt((dx * dx) + (dy * dy));
        if (dy > 0.0f) {
            pointF.x = (float) Math.asin(dx / pointF.y);
        } else if (dy < 0.0f) {
            pointF.x = (float) (3.141592653589793d - Math.asin(dx / pointF.y));
        }
        return pointF;
    }

    private PieItem findItem(PointF polar, boolean expandTriggerArea) {
        for (PieItem pieItem : this.mItems) {
            if (pieItem.getLevel() == this.mLevels && expandTriggerArea) {
                if (pieItem.getInnerRadius() - this.mTouchOffset < polar.y
                        && pieItem.getStartAngle() < polar.x
                        && pieItem.getStartAngle() + pieItem.getSweep() > polar.x) {
                    return pieItem;
                }
            } else if (pieItem.getInnerRadius() - this.mTouchOffset < polar.y
                    && pieItem.getOuterRadius() - this.mTouchOffset > polar.y
                    && pieItem.getStartAngle() < polar.x
                    && pieItem.getStartAngle() + pieItem.getSweep() > polar.x) {
                return pieItem;
            }
        }
        return null;
    }

    public static int manipulateColor(int color, float factor) {
        return Color.argb(
                Color.alpha(color),
                Math.min(Math.round(Color.red(color) * factor), 255),
                Math.min(Math.round(Color.green(color) * factor), 255),
                Math.min(Math.round(Color.blue(color) * factor), 255));
    }
}
