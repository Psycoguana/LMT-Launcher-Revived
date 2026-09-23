package com.noname81.lmt;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/* JADX INFO: loaded from: classes.dex */
class PermissionChecker {
    static int DRAW_OVER_APPS_REQUEST_CODE = 5469;
    private static final String TAG = "LMT::PermissionChecker";
    static int USAGE_STATS_REQUEST_CODE = 5470;
    private static PermissionChecker instance;

    private PermissionChecker() {}

    public static PermissionChecker getInstance() {
        if (instance == null) {
            instance = new PermissionChecker();
        }
        return instance;
    }

    boolean hasPhoneCallPermission(Context context, boolean z) {
        boolean z2 = true;
        if (Build.VERSION.SDK_INT >= 23
                && context.checkSelfPermission("android.permission.CALL_PHONE") != 0) {
            z2 = false;
        }
        if (z) {
            Log.d(TAG, "hasPhoneCallPermission(" + z2 + ")");
        }
        return z2;
    }

    boolean checkAndRequestPhoneCallPermission(Activity activity, boolean z) {
        if (Build.VERSION.SDK_INT < 23 || hasPhoneCallPermission(activity, z)) {
            return true;
        }
        Toast.makeText(activity, R.string.app_please_grant_phone_call_permission, 1).show();
        activity.requestPermissions(new String[] {"android.permission.CALL_PHONE"}, 0);
        return hasPhoneCallPermission(activity, z);
    }

    boolean hasExternalStorageReadPermission(Context context, boolean z) {
        boolean z2 = true;
        if (Build.VERSION.SDK_INT >= 23
                && context.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != 0) {
            z2 = false;
        }
        if (z) {
            Log.d(TAG, "hasExternalStorageReadPermission(" + z2 + ")");
        }
        return z2;
    }

    boolean checkAndRequestExternalStorageReadPermission(Activity activity, boolean z) {
        if (Build.VERSION.SDK_INT >= 33 || hasExternalStorageReadPermission(activity, z)) {
            return true;
        }
        Toast.makeText(activity, R.string.app_please_grant_external_storage_read_permission, 1)
                .show();
        activity.requestPermissions(new String[] {"android.permission.READ_EXTERNAL_STORAGE"}, 0);
        return hasExternalStorageReadPermission(activity, z);
    }

    boolean hasExternalStorageWritePermission(Context context, boolean z) {
        boolean z2 = true;
        if (Build.VERSION.SDK_INT >= 23
                && context.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
            z2 = false;
        }
        if (z) {
            Log.d(TAG, "hasExternalStorageWritePermission(" + z2 + ")");
        }
        return z2;
    }

    boolean checkAndRequestExternalStorageWritePermission(Activity activity, boolean z) {
        if (Build.VERSION.SDK_INT < 23 || hasExternalStorageWritePermission(activity, z)) {
            return true;
        }
        Toast.makeText(activity, R.string.app_please_grant_external_storage_write_permission, 1)
                .show();
        activity.requestPermissions(new String[] {"android.permission.WRITE_EXTERNAL_STORAGE"}, 0);
        return hasExternalStorageWritePermission(activity, z);
    }

    boolean hasDrawOverAppsPermission(Context context, boolean z) {
        boolean z2 =
                (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)) ? true : false;
        if (z) {
            Log.d(TAG, "hasDrawOverAppsPermission(" + z2 + ")");
        }
        return z2;
    }

    boolean checkAndRequestDrawOverAppsPermission(Activity activity, boolean z) {
        if (Build.VERSION.SDK_INT < 23 || hasDrawOverAppsPermission(activity, z)) {
            return true;
        }
        Toast.makeText(activity, R.string.app_please_grant_draw_over_apps_permission, 1).show();
        Intent intent =
                new Intent(
                        "android.settings.action.MANAGE_OVERLAY_PERMISSION",
                        Uri.parse("package:" + activity.getPackageName()));
        intent.addFlags(268435456);
        activity.startActivityForResult(intent, DRAW_OVER_APPS_REQUEST_CODE);
        return false;
    }

    boolean hasUsageStatsPermission(Context context, boolean z) {
        boolean z2 = true;
        if (Build.VERSION.SDK_INT >= 23
                && ((AppOpsManager) context.getSystemService("appops"))
                                .checkOpNoThrow(
                                        "android:get_usage_stats",
                                        Process.myUid(),
                                        context.getPackageName())
                        != 0) {
            z2 = false;
        }
        if (z) {
            Log.d(TAG, "hasUsageStatsPermission(" + z2 + ")");
        }
        return z2;
    }

    boolean checkAndRequestUsageStatsPermission(Activity activity, boolean z) {
        if (Build.VERSION.SDK_INT < 23 || hasUsageStatsPermission(activity, z)) {
            return true;
        }
        Toast.makeText(activity, R.string.app_please_grant_usage_stats_permission, 1).show();
        Intent intent = new Intent("android.settings.USAGE_ACCESS_SETTINGS");
        intent.addFlags(268435456);
        activity.startActivityForResult(intent, USAGE_STATS_REQUEST_CODE);
        return false;
    }
}
