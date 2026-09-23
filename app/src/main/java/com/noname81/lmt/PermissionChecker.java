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

/**
 * Runtime permission checks and prompts.
 *
 * <p>Each permission has a {@code has…} query and a {@code checkAndRequest…} that prompts if
 * missing. The {@code log} flag on both exists because these are called from hot paths where
 * logging every check would flood logcat.
 */
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

    boolean hasPhoneCallPermission(Context context, boolean log) {
        boolean granted = true;
        if (Build.VERSION.SDK_INT >= 23
                && context.checkSelfPermission("android.permission.CALL_PHONE") != 0) {
            granted = false;
        }
        if (log) {
            Log.d(TAG, "hasPhoneCallPermission(" + granted + ")");
        }
        return granted;
    }

    boolean checkAndRequestPhoneCallPermission(Activity activity, boolean log) {
        if (Build.VERSION.SDK_INT < 23 || hasPhoneCallPermission(activity, log)) {
            return true;
        }
        Toast.makeText(activity, R.string.app_please_grant_phone_call_permission, 1).show();
        activity.requestPermissions(new String[] {"android.permission.CALL_PHONE"}, 0);
        return hasPhoneCallPermission(activity, log);
    }

    boolean hasExternalStorageReadPermission(Context context, boolean log) {
        boolean granted = true;
        if (Build.VERSION.SDK_INT >= 23
                && context.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != 0) {
            granted = false;
        }
        if (log) {
            Log.d(TAG, "hasExternalStorageReadPermission(" + granted + ")");
        }
        return granted;
    }

    boolean checkAndRequestExternalStorageReadPermission(Activity activity, boolean log) {
        if (Build.VERSION.SDK_INT >= 33 || hasExternalStorageReadPermission(activity, log)) {
            return true;
        }
        Toast.makeText(activity, R.string.app_please_grant_external_storage_read_permission, 1)
                .show();
        activity.requestPermissions(new String[] {"android.permission.READ_EXTERNAL_STORAGE"}, 0);
        return hasExternalStorageReadPermission(activity, log);
    }

    boolean hasExternalStorageWritePermission(Context context, boolean log) {
        boolean granted = true;
        if (Build.VERSION.SDK_INT >= 23
                && context.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
            granted = false;
        }
        if (log) {
            Log.d(TAG, "hasExternalStorageWritePermission(" + granted + ")");
        }
        return granted;
    }

    boolean checkAndRequestExternalStorageWritePermission(Activity activity, boolean log) {
        if (Build.VERSION.SDK_INT < 23 || hasExternalStorageWritePermission(activity, log)) {
            return true;
        }
        Toast.makeText(activity, R.string.app_please_grant_external_storage_write_permission, 1)
                .show();
        activity.requestPermissions(new String[] {"android.permission.WRITE_EXTERNAL_STORAGE"}, 0);
        return hasExternalStorageWritePermission(activity, log);
    }

    boolean hasDrawOverAppsPermission(Context context, boolean log) {
        boolean granted =
                (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)) ? true : false;
        if (log) {
            Log.d(TAG, "hasDrawOverAppsPermission(" + granted + ")");
        }
        return granted;
    }

    boolean checkAndRequestDrawOverAppsPermission(Activity activity, boolean log) {
        if (Build.VERSION.SDK_INT < 23 || hasDrawOverAppsPermission(activity, log)) {
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

    boolean hasUsageStatsPermission(Context context, boolean log) {
        boolean granted = true;
        if (Build.VERSION.SDK_INT >= 23
                && ((AppOpsManager) context.getSystemService("appops"))
                                .checkOpNoThrow(
                                        "android:get_usage_stats",
                                        Process.myUid(),
                                        context.getPackageName())
                        != 0) {
            granted = false;
        }
        if (log) {
            Log.d(TAG, "hasUsageStatsPermission(" + granted + ")");
        }
        return granted;
    }

    boolean checkAndRequestUsageStatsPermission(Activity activity, boolean log) {
        if (Build.VERSION.SDK_INT < 23 || hasUsageStatsPermission(activity, log)) {
            return true;
        }
        Toast.makeText(activity, R.string.app_please_grant_usage_stats_permission, 1).show();
        Intent intent = new Intent("android.settings.USAGE_ACCESS_SETTINGS");
        intent.addFlags(268435456);
        activity.startActivityForResult(intent, USAGE_STATS_REQUEST_CODE);
        return false;
    }
}
