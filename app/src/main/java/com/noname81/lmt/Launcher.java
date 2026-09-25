package com.noname81.lmt;

import android.accessibilityservice.AccessibilityService;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

/**
 * Turns an {@link Action} into an effect. The single dispatcher for every trigger.
 *
 * <p>{@code fireAction} is the switch that all three trigger systems funnel into. Most actions try
 * paths in order of preference: accessibility first where it can do the job, then root, then a
 * plain public-API fallback. That ordering is why home, back and recents still work while the
 * root-only actions do not — see docs/ROADMAP.md 3.1.
 *
 * <p>Root arrives by two different routes. {@code runCommandRemote} goes through the long-lived
 * {@code InputContext} helper; {@code runCommandRoot} runs a one-off command in a plain {@code su}
 * shell.
 */
class Launcher {
    private static boolean DEBUG = false;
    private static final String TAG = "LMT::Launcher";
    private static Launcher instance;
    private Context mContext;
    private RootContext mRootContext;
    private SettingsValues mSettings;

    private Launcher(Context context) {
        this.mContext = context;
        this.mRootContext = RootContext.getInstance(context);
        this.mSettings = SettingsValues.getInstance(context);
        Torch.watch(context);
    }

    static Launcher getInstance(Context context) {
        if (instance == null) {
            instance = new Launcher(context);
        }
        return instance;
    }

    static void setDebug(int mode) {
        DEBUG = mode != 1 ? false : true;
    }

    void fireAction(Action action) {
        Log.d(TAG, "Fire action " + action.toString());
        switch (action.getType()) {
            case Action.App:
                doAppAction(action.getString());
                break;
            case Action.Home:
                doHomeAction();
                break;
            case Action.HomeLongpress:
                doHomeLongpressAction();
                break;
            case Action.Menu:
                doMenuAction();
                break;
            case Action.MenuLongpress:
                doMenuLongpressAction();
                break;
            case Action.Back:
                doBackAction();
                break;
            case Action.BackLongpress:
                doBackLongpressAction();
                break;
            case Action.RecentApps:
                doRecentAppsAction();
                break;
            case Action.Search:
                doSearchAction();
                break;
            case Action.SearchLongpress:
                doSearchLongpressAction();
                break;
            case Action.NextApp:
                doNextApp();
                break;
            case Action.PrevApp:
                doPrevApp();
                break;
            case Action.LastApp:
                doLastApp();
                break;
            case Action.OpenNotificationBar:
                doOpenNotificationBar();
                break;
            case Action.OpenQuickSettings:
                doOpenQuickSettings();
                break;
            case Action.OpenKeyboard:
                doOpenKeyboard();
                break;
            case Action.OpenPowerMenu:
                doPowerMenuAction();
                break;
            case Action.WifiToggle:
                doWifiToggleAction();
                break;
            case Action.DataToggle:
                doDataToggleAction();
                break;
            case Action.BluetoothToggle:
                doBluetoothToggleAction();
                break;
            case Action.GPSToggle:
                doGPSToggleAction();
                break;
            case Action.ImmersiveModeToggle:
                doImmersiveModeToggleAction();
                break;
            case Action.Key:
                doKeyAction(action.getString());
                break;
            case Action.Activity:
                doActivityAction(action.getString());
                break;
            case Action.WebPage:
                doWebPageAction(action.getString());
                break;
            case Action.Script:
                doScriptAction(action.getString());
                break;
            case Action.KillApp:
                doKillAppAction();
                break;
            case Action.KillAllApps:
                doKillAllAppsAction();
                break;
            case Action.TaskerTask:
                doTaskerAction(action.getString());
                break;
            case Action.Shortcut:
                doShortcutAction(action.getString());
                break;
            case Action.Screenshot:
                doScreenshotAction();
                break;
            case Action.UnpinApp:
                doUnpinAppAction();
                break;
            case Action.NowOnTap:
                doNowOnTapAction();
                break;
            case Action.Assistant:
                doAssistantAction();
                break;
            case Action.SplitScreen:
                doSplitScreenAction();
                break;
            case Action.Flashlight:
                Torch.toggle(this.mContext);
                break;
            case Action.KeepScreenOn:
                KeepScreenOn.toggle(this.mContext);
                break;
            case Action.PopUpWindow:
                PopUpWindow.open(this.mContext, this.mRootContext);
                break;
            case Action.BrightnessDial:
                // Released on the slice without dialing (ROADMAP 1.17).
                if (PermissionChecker.checkAndRequestWriteSettingsPermission(this.mContext)) {
                    PieBrightness.toggleAdaptive(this.mContext);
                }
                break;
            case Action.Nova:
                doOpenNova();
                break;
            case Action.Apex:
                doOpenApex();
                break;
            case Action.Holo:
                doOpenHolo();
                break;
        }
    }

    private void doHomeAction() {
        if (DEBUG) {
            Log.d(TAG, "doHomeAction()");
        }
        if (AccessibilityHandler.isAccessibilityAvailable(this.mContext, false)) {
            AccessibilityHandler.performAction(2);
            return;
        }
        if (this.mRootContext.isRootAvailable(false)) {
            doKeyAction(3);
            return;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        intent.setFlags(268435456);
        this.mContext.startActivity(intent);
    }

    private void doHomeLongpressAction() {
        if (DEBUG) {
            Log.d(TAG, "doHomeLongpressAction()");
        }
        doKeyAction(2003);
    }

    private void doRecentAppsAction() {
        if (DEBUG) {
            Log.d(TAG, "doRecentAppsAction()");
        }
        if (AccessibilityHandler.isAccessibilityAvailable(this.mContext, false)) {
            AccessibilityHandler.performAction(3);
        } else if (this.mRootContext.isRootAvailable(false)) {
            doKeyAction(187);
        } else {
            AccessibilityHandler.isAccessibilityAvailable(this.mContext, true);
        }
    }

    private void doMenuAction() {
        if (DEBUG) {
            Log.d(TAG, "doMenuAction()");
        }
        if (performAccessibilityFallback(Action.Menu)) {
            return;
        }
        doKeyAction(82);
    }

    private void doMenuLongpressAction() {
        if (DEBUG) {
            Log.d(TAG, "doMenuLongpressAction()");
        }
        doKeyAction(2082);
    }

    private void doBackAction() {
        if (DEBUG) {
            Log.d(TAG, "doBackAction()");
        }
        if (AccessibilityHandler.isAccessibilityAvailable(this.mContext, false)) {
            AccessibilityHandler.performAction(1);
        } else if (this.mRootContext.isRootAvailable(false)) {
            doKeyAction(4);
        } else {
            AccessibilityHandler.isAccessibilityAvailable(this.mContext, true);
        }
    }

    private void doBackLongpressAction() {
        if (DEBUG) {
            Log.d(TAG, "doBackLongpressAction()");
        }
        doKeyAction(2004);
    }

    private void doSearchAction() {
        if (DEBUG) {
            Log.d(TAG, "doSearchAction()");
        }
        if (this.mRootContext.isRootAvailable(false)) {
            doKeyAction(84);
            return;
        }
        Intent intent = new Intent("android.intent.action.SEARCH");
        intent.addFlags(268435456);
        try {
            this.mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void doSearchLongpressAction() {
        if (DEBUG) {
            Log.d(TAG, "doSearchLongpressAction()");
        }
        Intent intent = new Intent("android.intent.action.SEARCH_LONG_PRESS");
        intent.addFlags(268435456);
        try {
            this.mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void doNextApp() {
        if (DEBUG) {
            Log.d(TAG, "doNextApp()");
        }
        this.mRootContext.runCommandRemote("am next-app 0", true);
    }

    private void doPrevApp() {
        if (DEBUG) {
            Log.d(TAG, "doPrevApp()");
        }
        this.mRootContext.runCommandRemote("am prev-app 0", true);
    }

    private void doLastApp() {
        if (DEBUG) {
            Log.d(TAG, "doLastApp()");
        }
        if (this.mRootContext.isRootAvailable(false)) {
            this.mRootContext.runCommandRemote("am last-app 0", true);
            return;
        }
        String packageNamesOfRecentApps = this.mSettings.getPackageNamesOfRecentApps(3);
        if (packageNamesOfRecentApps.length() > 1) {
            String[] strArrSplit = packageNamesOfRecentApps.split(" ");
            if (doAppAction(strArrSplit[1]) || packageNamesOfRecentApps.length() <= 2) {
                return;
            }
            doAppAction(strArrSplit[2]);
        }
    }

    private void doPowerMenuAction() {
        if (DEBUG) {
            Log.d(TAG, "doPowerMenuAction()");
        }
        if (AccessibilityHandler.isAccessibilityAvailable(this.mContext, false)) {
            AccessibilityHandler.performAction(6);
        } else if (this.mRootContext.isRootAvailable(false)) {
            doKeyAction(1026);
        } else {
            AccessibilityHandler.isAccessibilityAvailable(this.mContext, true);
        }
    }

    private void doKeyAction(String keycodes) {
        if (DEBUG) {
            Log.d(TAG, "doKeyAction(" + keycodes + ")");
        }
        this.mRootContext.runCommandRemote("input keyevent " + keycodes, true);
    }

    private void doKeyAction(int keycode) {
        if (DEBUG) {
            Log.d(TAG, "doKeyAction()");
        }
        this.mRootContext.runCommandRemote("input keyevent " + keycode, true);
    }

    private boolean doAppAction(String packageName) {
        Intent launchIntentForPackage;
        if (DEBUG) {
            Log.d(TAG, "doAppAction(" + packageName + ")");
        }
        if (packageName == null
                || packageName.length() <= 0
                || (launchIntentForPackage =
                                this.mContext
                                        .getPackageManager()
                                        .getLaunchIntentForPackage(packageName))
                        == null) {
            return false;
        }
        doActivityAction(launchIntentForPackage.getComponent().flattenToString());
        return true;
    }

    private void doActivityAction(String component) {
        if (DEBUG) {
            Log.d(TAG, "doActivityAction(" + component + ")");
        }
        if (component == null || component.length() <= 0) {
            return;
        }
        if (this.mRootContext.isRootAvailable(false)) {
            this.mRootContext.runCommandRemote("am move-to-front-or-start " + component, true);
            return;
        }
        Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
        intent.setComponent(ComponentName.unflattenFromString(component));
        intent.setFlags(268435456);
        this.mContext.startActivity(intent);
    }

    private void doWebPageAction(String url) {
        if (DEBUG) {
            Log.d(TAG, "doWebPageAction(" + url + ")");
        }
        if (!url.contains("http")) {
            if (!url.contains("www")) {
                url = "http://www." + url;
            } else {
                url = "http://" + url;
            }
        }
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addFlags(268435456);
        intent.setData(Uri.parse(url));
        try {
            this.mContext.startActivity(intent);
        } catch (ActivityNotFoundException unused) {
        }
    }

    private void doScriptAction(String script) {
        if (DEBUG) {
            Log.d(TAG, "doScriptAction(" + script + ")");
        }
        if (script == null || script.length() <= 0) {
            return;
        }
        if (script.contains(".sh")) {
            this.mRootContext.runCommandRoot("sh " + script, true);
            return;
        }
        this.mRootContext.runCommandRoot(script, true);
    }

    private void doWifiToggleAction() {
        if (DEBUG) {
            Log.d(TAG, "doWifiToggleAction()");
        }
        if (Build.VERSION.SDK_INT >= 29) {
            if (((WifiManager)
                            this.mContext
                                    .getApplicationContext()
                                    .getSystemService(Context.WIFI_SERVICE))
                    .isWifiEnabled()) {
                this.mRootContext.runCommandRoot("svc wifi disable", true);
                return;
            } else {
                this.mRootContext.runCommandRoot("svc wifi enable", true);
                return;
            }
        }
        WifiManager wifiManager =
                (WifiManager)
                        this.mContext
                                .getApplicationContext()
                                .getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(!wifiManager.isWifiEnabled());
    }

    private void doDataToggleAction() {
        if (DEBUG) {
            Log.d(TAG, "doDataToggleAction()");
        }
        if (((TelephonyManager) this.mContext.getSystemService(Context.TELEPHONY_SERVICE))
                        .getDataState()
                == 2) {
            this.mRootContext.runCommandRoot("svc data disable", true);
        } else {
            this.mRootContext.runCommandRoot("svc data enable", true);
        }
    }

    private void doBluetoothToggleAction() {
        if (DEBUG) {
            Log.d(TAG, "doBluetoothToggleAction()");
        }
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!defaultAdapter.isEnabled()) {
            defaultAdapter.enable();
        } else {
            defaultAdapter.disable();
        }
    }

    private void doGPSToggleAction() {
        if (DEBUG) {
            Log.d(TAG, "doGPSToggleAction()");
        }
        Intent intent = new Intent("android.settings.LOCATION_SOURCE_SETTINGS");
        intent.addFlags(268435456);
        this.mContext.startActivity(intent);
    }

    private void doImmersiveModeToggleAction() {
        if (DEBUG) {
            Log.d(TAG, "doImmersiveModeToggleAction()");
        }
        boolean zIsRootAvailable = this.mRootContext.isRootAvailable(false);
        String strRunCommandResult =
                this.mRootContext.runCommandResult(
                        "settings get global policy_control", zIsRootAvailable);
        if (strRunCommandResult.contains("null")) {
            this.mRootContext.runCommandResult(
                    "settings put global policy_control"
                            + " immersive.navigation=*,-com.android.systemui,-com.whatsapp",
                    zIsRootAvailable);
        } else if (strRunCommandResult.contains("navigation")) {
            this.mRootContext.runCommandResult(
                    "settings put global policy_control"
                            + " immersive.full=*,-com.android.systemui,-com.whatsapp",
                    zIsRootAvailable);
        } else {
            this.mRootContext.runCommandResult(
                    "settings put global policy_control null*", zIsRootAvailable);
        }
    }

    private void doKillAppAction() {
        if (DEBUG) {
            Log.d(TAG, "doKillAppAction()");
        }
        this.mRootContext.runCommandRemote("am kill-app 0", false);
    }

    private void doKillAllAppsAction() {
        if (DEBUG) {
            Log.d(TAG, "doKillAllAppsAction()");
        }
        this.mRootContext.runCommandRemote("am kill-all-apps 0", false);
    }

    private void doTaskerAction(String taskName) {
        if (DEBUG) {
            Log.d(TAG, "doTaskerAction()");
        }
        if (taskName == null || taskName.length() <= 0) {
            return;
        }
        // Tasker's own external-access broadcast, needing PERMISSION_RUN_TASKS - neither
        // root nor the accessibility service is involved. See docs/ROADMAP.md 2.3.
        this.mContext.sendBroadcast(TaskerIntents.runTask(this.mContext, taskName));
    }

    void doOpenNotificationBar() {
        if (DEBUG) {
            Log.d(TAG, "doOpenNotificationBar()");
        }
        if (AccessibilityHandler.isAccessibilityAvailable(this.mContext, false)) {
            AccessibilityHandler.performAction(4);
            return;
        }
        try {
            Class.forName("android.app.StatusBarManager")
                    .getMethod("expandNotificationsPanel", new Class[0])
                    .invoke(
                            this.mContext.getApplicationContext().getSystemService("statusbar"),
                            new Object[0]);
        } catch (Exception unused) {
            AccessibilityHandler.isAccessibilityAvailable(this.mContext, true);
        }
    }

    void doOpenQuickSettings() {
        if (DEBUG) {
            Log.d(TAG, "doOpenQuickSettings()");
        }
        if (AccessibilityHandler.isAccessibilityAvailable(this.mContext, false)) {
            AccessibilityHandler.performAction(5);
            return;
        }
        try {
            Class.forName("android.app.StatusBarManager")
                    .getMethod("expandSettingsPanel", new Class[0])
                    .invoke(
                            this.mContext.getApplicationContext().getSystemService("statusbar"),
                            new Object[0]);
        } catch (Exception unused) {
            AccessibilityHandler.isAccessibilityAvailable(this.mContext, true);
        }
    }

    private void doOpenKeyboard() {
        if (DEBUG) {
            Log.d(TAG, "doOpenKeyboard()");
        }
        ((InputMethodManager) this.mContext.getSystemService(Context.INPUT_METHOD_SERVICE))
                .toggleSoftInput(2, 1);
    }

    private void doShortcutAction(String shortcutUri) {
        if (DEBUG) {
            Log.d(TAG, "doShortcutAction()");
        }
        try {
            Intent uri = Intent.parseUri(shortcutUri, 0);
            uri.addFlags(268435456);
            this.mContext.startActivity(uri);
        } catch (Exception unused) {
        }
    }

    private void doScreenshotAction() {
        if (DEBUG) {
            Log.d(TAG, "doScreenshotAction()");
        }
        if (performAccessibilityFallback(Action.Screenshot)) {
            return;
        }
        doKeyAction("1000 120");
    }

    private void doUnpinAppAction() {
        if (DEBUG) {
            Log.d(TAG, "doUnpinAppAction()");
        }
        this.mRootContext.runCommandRoot("am task lock stop", true);
    }

    private void doNowOnTapAction() {
        if (DEBUG) {
            Log.d(TAG, "doNowOnTapAction()");
        }
        doKeyAction("219");
    }

    private void doAssistantAction() {
        if (DEBUG) {
            Log.d(TAG, "doAssistantAction()");
        }
        doKeyAction("219");
    }

    private void doSplitScreenAction() {
        if (DEBUG) {
            Log.d(TAG, "doSplitScreenAction()");
        }
        if (AccessibilityHandler.isAccessibilityAvailable(this.mContext, false)) {
            AccessibilityHandler.performAction(7);
        } else {
            AccessibilityHandler.isAccessibilityAvailable(this.mContext, true);
        }
    }

    private void doOpenNova() {
        if (DEBUG) {
            Log.d(TAG, "doOpenNova()");
        }
        if (!this.mRootContext.isRootAvailable(false)
                && performAccessibilityFallback(Action.Nova)) {
            return;
        }
        this.mRootContext.runCommandRoot(
                "am start -a com.teslacoilsw.launcher.ACTION -n"
                        + " com.teslacoilsw.launcher/.NovaShortcutHandler",
                true);
    }

    private void doOpenApex() {
        if (DEBUG) {
            Log.d(TAG, "doOpenApex()");
        }
        if (!this.mRootContext.isRootAvailable(false)
                && performAccessibilityFallback(Action.Apex)) {
            return;
        }
        this.mRootContext.runCommandRoot(
                "am start -a com.anddoes.launcher.ACTION -n com.anddoes.launcher/.Launcher -e"
                        + " LAUNCHER_ACTION 'APP_DRAWER'",
                true);
    }

    private void doOpenHolo() {
        if (DEBUG) {
            Log.d(TAG, "doOpenHolo()");
        }
        if (!this.mRootContext.isRootAvailable(false)
                && performAccessibilityFallback(Action.Holo)) {
            return;
        }
        this.mRootContext.runCommandRoot(
                "am start -a com.mobint.hololauncher.ACTION -n com.mobint.hololauncher/.Launcher -e"
                        + " ACTION 'DRAWER'",
                true);
    }

    /**
     * ROADMAP 2.1: the accessibility global action that stands in for a root-only {@code
     * actionType} on this Android version, or 0 when there is none. Screenshot and Menu send the
     * same thing the root key event did; the launcher drawers open the system's all-apps screen
     * rather than that launcher's own drawer, so those still prefer root.
     */
    static int accessibilityFallback(int actionType) {
        int sdk = Build.VERSION.SDK_INT;
        switch (actionType) {
            case Action.Screenshot:
                return sdk >= 28 ? AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT : 0;
            case Action.Nova:
            case Action.Apex:
            case Action.Holo:
                return sdk >= 31 ? AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS : 0;
            case Action.Menu:
                return sdk >= 36 ? AccessibilityService.GLOBAL_ACTION_MENU : 0;
            default:
                return 0;
        }
    }

    /** Runs {@link #accessibilityFallback} if there is one and the service is connected. */
    private boolean performAccessibilityFallback(int actionType) {
        int globalAction = accessibilityFallback(actionType);
        if (globalAction == 0
                || !AccessibilityHandler.isAccessibilityAvailable(this.mContext, false)) {
            return false;
        }
        AccessibilityHandler.performAction(globalAction);
        return true;
    }
}
