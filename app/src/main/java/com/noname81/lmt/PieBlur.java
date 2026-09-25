package com.noname81.lmt;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.RecordingCanvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

/**
 * Blurs what is behind the pie while it is open (ROADMAP 1.11).
 *
 * <p>No window blur works on every phone (AOSP cross-window blur is off on Samsung; Samsung's own
 * live blur only exists on flagships), so this takes one accessibility screenshot per pie opening
 * and draws it blurred behind the pie. On the test phone that is a ~20 ms GPU capture and a blurred frame
 * ~75-90 ms after the touch; nothing runs while the pie is closed.
 *
 * <p>The pie is kept invisible until the screenshot arrives, otherwise it could end up in its own
 * background; a timeout reveals it anyway if the capture is slow or refused (the system allows one
 * screenshot per 333 ms, so a quick second opening goes unblurred).
 *
 * <p>In behind-pie mode the blur is a ring: the pie's inner circle is left clear, since there is no
 * pie there to sit on.
 *
 * <p>Needs Android 12 ({@link RenderEffect}) and LMT's accessibility service; without either the
 * pie opens exactly as before.
 */
final class PieBlur extends View {
    static final int MODE_NONE = 0;
    static final int MODE_BACKGROUND = 1;
    static final int MODE_BEHIND_PIE = 2;

    private static final long PIE_REVEAL_TIMEOUT_MS = 150;
    private static final long FADE_IN_MS = 120;

    private static final ViewOutlineProvider OVAL =
            new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            };

    private final SettingsValues mSettings;
    private final int[] mLocation = new int[2];
    private final Runnable mRevealPie = this::revealPie;
    private final Path mHole = new Path();
    private RenderNode mNode;
    private float mHoleRadius;
    private Bitmap mBitmap;
    private HardwareBuffer mBuffer;
    private View mPie;
    private int mGeneration;

    /** The open blur's shape, so a pie folder can grow it (ROADMAP 1.17). */
    private boolean mBehindPie;

    private boolean mLive;
    private int mCenterX;
    private int mCenterY;

    PieBlur(Context context) {
        super(context);
        this.mSettings = SettingsValues.getInstance(context);
    }

    static boolean isSupported() {
        return Build.VERSION.SDK_INT >= 31;
    }

    /**
     * Starts the blur for a pie that just opened at {@code centerX/centerY} (in the pie's own
     * coordinates, which are the container's) with the given outer and inner radius.
     */
    void open(View pie, int centerX, int centerY, int radius, int innerRadius) {
        if (!isSupported() || !(pie.getParent() instanceof FrameLayout)) {
            return;
        }
        int mode = this.mSettings.loadPieBlur();
        if (mode == MODE_NONE) {
            return;
        }
        this.mBehindPie = mode == MODE_BEHIND_PIE;
        this.mCenterX = centerX;
        this.mCenterY = centerY;
        this.mLive = SamsungBlur.isAvailable() && this.mSettings.loadPieBlurLive() != 0;
        if (this.mLive) {
            openLive((FrameLayout) pie.getParent(), mode, centerX, centerY, radius);
            return;
        }
        AccessibilityService service = AccessibilityHandler.connectedService();
        if (service == null) {
            return;
        }
        FrameLayout container = (FrameLayout) pie.getParent();
        close();
        int generation = this.mGeneration;

        FrameLayout.LayoutParams params;
        if (mode == MODE_BEHIND_PIE) {
            params = new FrameLayout.LayoutParams(radius * 2, radius * 2);
            params.leftMargin = centerX - radius;
            params.topMargin = centerY - radius;
        } else {
            params =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
        }
        setOutlineProvider(mode == MODE_BEHIND_PIE ? OVAL : null);
        setClipToOutline(mode == MODE_BEHIND_PIE);
        this.mHoleRadius = mode == MODE_BEHIND_PIE ? innerRadius : 0;
        // The blur lives on a RenderNode rather than on this view, so onDraw can clip the hole
        // out of the already-blurred result instead of blurring the hole's edge into the ring.
        if (this.mNode == null) {
            this.mNode = new RenderNode("PieBlur");
        }
        float blur = this.mSettings.loadPieBlurRadius();
        this.mNode.setRenderEffect(
                RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP));
        setAlpha(0f);
        container.addView(this, 0, params);

        this.mPie = pie;
        pie.setAlpha(0f);
        postDelayed(this.mRevealPie, PIE_REVEAL_TIMEOUT_MS);

        service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                getContext().getMainExecutor(),
                new AccessibilityService.TakeScreenshotCallback() {
                    @Override
                    public void onSuccess(AccessibilityService.ScreenshotResult result) {
                        HardwareBuffer buffer = result.getHardwareBuffer();
                        Bitmap bitmap = Bitmap.wrapHardwareBuffer(buffer, result.getColorSpace());
                        if (generation != PieBlur.this.mGeneration
                                || bitmap == null
                                || !isAttachedToWindow()) {
                            if (bitmap != null) {
                                bitmap.recycle();
                            }
                            buffer.close();
                            return;
                        }
                        PieBlur.this.mBitmap = bitmap;
                        PieBlur.this.mBuffer = buffer;
                        revealPie();
                        invalidate();
                        animate().alpha(1f).setDuration(FADE_IN_MS);
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        if (generation == PieBlur.this.mGeneration) {
                            revealPie();
                        }
                    }
                });
    }

    /**
     * Samsung's live window blur (ROADMAP 1.11 upgrade): the system blurs whatever is behind this
     * view, every frame, so there is no screenshot to wait for and the pie shows at once. In
     * behind-pie mode it is a disc: a window blur has no way to leave the centre clear.
     */
    private void openLive(FrameLayout container, int mode, int centerX, int centerY, int radius) {
        close();
        FrameLayout.LayoutParams params;
        float corner;
        if (mode == MODE_BEHIND_PIE) {
            params = new FrameLayout.LayoutParams(radius * 2, radius * 2);
            params.leftMargin = centerX - radius;
            params.topMargin = centerY - radius;
            corner = radius;
        } else {
            params =
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
            corner = 0;
        }
        setOutlineProvider(null);
        setClipToOutline(false);
        this.mHoleRadius = 0;
        setAlpha(1f);
        if (!SamsungBlur.apply(this, this.mSettings.loadPieBlurRadius(), corner)) {
            return;
        }
        container.addView(this, 0, params);
    }

    /**
     * Grows or shrinks the disc behind the pie to {@code radius}, for a pie folder's ring opening
     * or closing (ROADMAP 1.17). The screenshot covers the whole display, so only the view's bounds
     * change; a live blur gets the new corner radius too.
     */
    void resize(int radius) {
        if (!this.mBehindPie || !(getLayoutParams() instanceof FrameLayout.LayoutParams)) {
            return;
        }
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.width = radius * 2;
        params.height = radius * 2;
        params.leftMargin = this.mCenterX - radius;
        params.topMargin = this.mCenterY - radius;
        setLayoutParams(params);
        if (this.mLive) {
            SamsungBlur.apply(this, this.mSettings.loadPieBlurRadius(), radius);
        }
    }

    /** Removes the blur and frees the screenshot; safe to call when nothing is open. */
    void close() {
        this.mGeneration++;
        revealPie();
        animate().cancel();
        if (getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).removeView(this);
        }
        release();
    }

    private void revealPie() {
        removeCallbacks(this.mRevealPie);
        if (this.mPie != null) {
            this.mPie.setAlpha(1f);
            this.mPie = null;
        }
    }

    private void release() {
        if (this.mNode != null) {
            this.mNode.discardDisplayList();
        }
        if (this.mBitmap != null) {
            this.mBitmap.recycle();
            this.mBitmap = null;
        }
        if (this.mBuffer != null) {
            this.mBuffer.close();
            this.mBuffer = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        // The trigger strip is removed and re-added on every shrink (and on rotation), so the
        // screenshot must never outlive the expanded window it was taken for.
        super.onDetachedFromWindow();
        this.mGeneration++;
        revealPie();
        release();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.mBitmap == null) {
            return;
        }
        // The screenshot covers the whole display; shift it so each pixel lands back where it
        // was on screen, whatever this view's (or the window's) offset is.
        getLocationOnScreen(this.mLocation);
        if (this.mNode == null || !canvas.isHardwareAccelerated()) {
            canvas.drawBitmap(this.mBitmap, -this.mLocation[0], -this.mLocation[1], null);
            return;
        }
        this.mNode.setPosition(0, 0, getWidth(), getHeight());
        RecordingCanvas recording = this.mNode.beginRecording();
        recording.drawBitmap(this.mBitmap, -this.mLocation[0], -this.mLocation[1], null);
        this.mNode.endRecording();
        int save = canvas.save();
        if (this.mHoleRadius > 0) {
            this.mHole.rewind();
            this.mHole.addCircle(
                    getWidth() / 2f, getHeight() / 2f, this.mHoleRadius, Path.Direction.CW);
            canvas.clipOutPath(this.mHole);
        }
        canvas.drawRenderNode(this.mNode);
        canvas.restoreToCount(save);
    }
}
