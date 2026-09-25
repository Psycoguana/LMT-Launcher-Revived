package com.noname81.lmt;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

/**
 * The Pop-up window action (ROADMAP 2.6): starts the app on screen again in the freeform windowing
 * mode, which moves its running task into a floating window (Samsung's pop-up view). The app comes
 * from the accessibility service. Tries the launch option directly first (Android blocks it for
 * apps; kept for phones that allow it), then {@code am start --windowingMode 5} as root, then the
 * same through Shizuku.
 */
final class PopUpWindow {
    private static final String TAG = "LMT::PopUpWindow";

    /** WindowConfiguration.WINDOWING_MODE_FREEFORM. */
    static final int WINDOWING_MODE_FREEFORM = 5;

    private PopUpWindow() {}

    static void open(Context context, RootContext root) {
        if (!AccessibilityHandler.isAccessibilityAvailable(context, true)) {
            return;
        }
        String pkg = AccessibilityHandler.foregroundPackage();
        PackageManager pm = context.getPackageManager();
        Intent launch = pkg != null ? pm.getLaunchIntentForPackage(pkg) : null;
        if (launch == null || launch.getComponent() == null || isHome(pm, pkg)) {
            toast(context, "Open an app first");
            return;
        }
        ComponentName component = launch.getComponent();
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (startFreeform(context, launch)) {
            return;
        }
        String command =
                "am start --windowingMode "
                        + WINDOWING_MODE_FREEFORM
                        + " -n "
                        + component.flattenToShortString();
        if (root.isRootAvailable(false)) {
            // The su shell, not runCommandRemote: InputContext starts activities its own way and
            // drops --windowingMode.
            root.runCommandRoot(command, false);
            return;
        }
        switch (ShizukuShell.run(command)) {
            case ShizukuShell.RAN:
                return;
            case ShizukuShell.ASKED_PERMISSION:
                toast(context, "Allow LMT in Shizuku, then try again");
                return;
            case ShizukuShell.NOT_RUNNING:
                toast(context, "Pop-up windows need root, or Shizuku installed and running");
                return;
            default:
                toast(context, "Shizuku couldn't open the pop-up window");
        }
    }

    private static void toast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    /** ActivityOptions.setLaunchWindowingMode is hidden, so it is looked up by name. */
    private static boolean startFreeform(Context context, Intent launch) {
        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            Method setMode = ActivityOptions.class.getMethod("setLaunchWindowingMode", int.class);
            setMode.invoke(options, WINDOWING_MODE_FREEFORM);
            context.startActivity(launch, options.toBundle());
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Freeform launch option unavailable", e);
            return false;
        }
    }

    private static boolean isHome(PackageManager pm, String pkg) {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo info = pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return info != null
                && info.activityInfo != null
                && pkg.equals(info.activityInfo.packageName);
    }
}
