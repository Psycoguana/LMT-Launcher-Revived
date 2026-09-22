package com.noname81.lmt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "LMT::StartupReceiver";

    @Override // android.content.BroadcastReceiver
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
