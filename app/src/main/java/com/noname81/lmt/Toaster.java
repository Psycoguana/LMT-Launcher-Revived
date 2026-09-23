package com.noname81.lmt;

import android.content.Context;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * The app's feedback channel: a toast, a vibration, both, or nothing.
 *
 * <p>Which one is the {@code Feedback} setting, so gestures can confirm silently.
 */
class Toaster {
    static final int Debug = 1;
    static final int Image = 2;
    static final int None = 0;
    static final int Vibrate = 3;
    private static Toaster instance;
    private Context mContext;
    private int mMode;
    private SettingsValues mSettings;
    private Vibrator mVibrator;

    private Toaster(Context context) {
        this.mContext = context;
        this.mMode = SettingsValues.getInstance(context).loadFeedbackMode();
        this.mVibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        this.mSettings = SettingsValues.getInstance(this.mContext);
    }

    static Toaster getInstance(Context context) {
        if (instance == null) {
            instance = new Toaster(context);
        }
        return instance;
    }

    int getMode() {
        return this.mMode;
    }

    void setMode(int mode) {
        this.mMode = mode;
        this.mSettings.saveFeedbackMode(mode);
    }

    void show(TouchServiceResult touchServiceResult) {
        int mode = this.mMode;
        if (mode == 1) {
            showDebug(touchServiceResult);
        } else if (mode == 2) {
            showImage(touchServiceResult);
        } else {
            if (mode != 3) {
                return;
            }
            vibrateGestures();
        }
    }

    private void showDebug(TouchServiceResult touchServiceResult) {
        Toast.makeText(this.mContext, touchServiceResult.toString(), 0).show();
    }

    private void showImage(TouchServiceResult touchServiceResult) {
        if (touchServiceResult.getGesture() < 14) {
            try {
                View viewInflate =
                        ((LayoutInflater) this.mContext.getSystemService("layout_inflater"))
                                .inflate(R.layout.toaster, (ViewGroup) null);
                ((ImageView) viewInflate.findViewById(R.id.image))
                        .setImageResource(
                                this.mContext
                                        .getResources()
                                        .getIdentifier(
                                                TouchServiceResult.names[
                                                        touchServiceResult.getGesture()]
                                                        .toLowerCase(),
                                                "drawable",
                                                this.mContext.getPackageName()));
                Toast toast = new Toast(this.mContext.getApplicationContext());
                toast.setGravity(16, 0, 0);
                toast.setDuration(0);
                toast.setView(viewInflate);
                toast.show();
            } catch (Exception unused) {
            }
        }
    }

    private void vibrateGestures() {
        this.mVibrator.vibrate(this.mSettings.loadGestureVibrationTime());
    }

    void vibratePie() {
        this.mVibrator.vibrate(this.mSettings.loadPieVibrationTime());
    }
}
