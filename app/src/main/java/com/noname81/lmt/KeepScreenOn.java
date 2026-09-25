package com.noname81.lmt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * The Keep screen on action (ROADMAP 2.6): a 1-pixel, see-through, untouchable overlay window with
 * {@code FLAG_KEEP_SCREEN_ON}, the same overlay permission the pie uses. It goes away when toggled
 * again or when the screen is turned off, so it can't be forgotten on.
 */
final class KeepScreenOn {
    private static final String TAG = "LMT::KeepScreenOn";
    private static View sView;
    private static BroadcastReceiver sScreenOff;

    private KeepScreenOn() {}

    static boolean isOn() {
        return sView != null;
    }

    static void toggle(Context caller) {
        // The overlay and the receiver outlive whatever component fired the action.
        Context context = caller.getApplicationContext();
        if (sView != null) {
            stop(context);
            Toast.makeText(context, "The screen can turn off again", Toast.LENGTH_SHORT).show();
            return;
        }
        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        1,
                        1,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                        PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        View view = new View(context);
        try {
            context.getSystemService(WindowManager.class).addView(view, params);
        } catch (RuntimeException e) {
            Log.w(TAG, "Overlay refused", e);
            Toast.makeText(context, "LMT needs the draw over apps permission", Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        sView = view;
        sScreenOff =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context receiverContext, Intent intent) {
                        stop(context);
                    }
                };
        context.registerReceiver(sScreenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        Toast.makeText(context, "The screen stays on", Toast.LENGTH_SHORT).show();
    }

    /** Lets the screen turn off again; also called when the service stops. */
    static void stop(Context caller) {
        Context context = caller.getApplicationContext();
        if (sView != null) {
            try {
                context.getSystemService(WindowManager.class).removeView(sView);
            } catch (RuntimeException e) {
                Log.w(TAG, "Overlay already gone", e);
            }
            sView = null;
        }
        if (sScreenOff != null) {
            try {
                context.unregisterReceiver(sScreenOff);
            } catch (RuntimeException e) {
                Log.w(TAG, "Receiver already gone", e);
            }
            sScreenOff = null;
        }
    }
}
