package com.noname81.lmt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Restarts the service after boot when autostart is enabled. */
public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "LMT::StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            startIfRequested(context);
        }
    }

    void startIfRequested(Context context) {
        SettingsValues settingsValues = SettingsValues.getInstance(context);
        if (settingsValues.loadAutostart() > 0) {
            Log.d(TAG, "Restarting TouchService");
            settingsValues.startService();
        }
    }
}
