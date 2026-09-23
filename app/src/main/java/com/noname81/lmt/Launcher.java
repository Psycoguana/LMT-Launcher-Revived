package com.noname81.lmt;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
    private ActivityManager mActivityManager;
    private Context mContext;
    private List<ActivityManager.RecentTaskInfo> mRecentTaskInfo;
    private RootContext mRootContext;
    private List<ActivityManager.RunningTaskInfo> mRunningTaskInfo;
    private SettingsValues mSettings;

    private Launcher(Context context) {
        this.mContext = context;
        this.mRootContext = RootContext.getInstance(context);
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mSettings = SettingsValues.getInstance(context);
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
        if (Build.VERSION.SDK_INT >= 21) {
            this.mRootContext.runCommandRemote("am next-app 0", true);
            return;
        }
        this.mRecentTaskInfo = this.mActivityManager.getRecentTasks(20, 0);
        int nextID = Integer.MAX_VALUE;
        int firstID = Integer.MAX_VALUE;
        int nextPos = 0;
        int firstPos = 0;
        for (int index = 1; index < this.mRecentTaskInfo.size(); index++) {
            if (this.mRecentTaskInfo.get(index).id < nextID
                    && this.mRecentTaskInfo.get(index).id > this.mRecentTaskInfo.get(0).id
                    && !this.mRecentTaskInfo
                            .get(index)
                            .baseIntent
                            .hasCategory("android.intent.category.HOME")) {
                nextID = this.mRecentTaskInfo.get(index).id;
                nextPos = index;
            }
            if (this.mRecentTaskInfo.get(index).id < firstID
                    && !this.mRecentTaskInfo
                            .get(index)
                            .baseIntent
                            .hasCategory("android.intent.category.HOME")
                    && this.mRecentTaskInfo.get(index).id > -1) {
                firstID = this.mRecentTaskInfo.get(index).id;
                firstPos = index;
            }
        }
        List<ActivityManager.RecentTaskInfo> list = this.mRecentTaskInfo;
        if (nextPos > 0) {
            firstPos = nextPos;
        }
        ComponentName component = list.get(firstPos).baseIntent.getComponent();
        component.getClass();
        String strFlattenToString = component.flattenToString();
        if (nextPos <= 0) {
            nextID = firstID;
        }
        moveTaskToFront(strFlattenToString, nextID);
    }

    private void doPrevApp() {
        if (DEBUG) {
            Log.d(TAG, "doPrevApp()");
        }
        if (Build.VERSION.SDK_INT >= 21) {
            this.mRootContext.runCommandRemote("am prev-app 0", true);
            return;
        }
        this.mRecentTaskInfo = this.mActivityManager.getRecentTasks(20, 0);
        int prevID = 0;
        int lastID = 0;
        int prevPos = 0;
        int lastPos = 0;
        for (int index = 1; index < this.mRecentTaskInfo.size(); index++) {
            if (this.mRecentTaskInfo.get(index).id > lastID
                    && this.mRecentTaskInfo.get(index).id < this.mRecentTaskInfo.get(0).id
                    && !this.mRecentTaskInfo
                            .get(index)
                            .baseIntent
                            .hasCategory("android.intent.category.HOME")) {
                lastID = this.mRecentTaskInfo.get(index).id;
                prevID = index;
            }
            if (this.mRecentTaskInfo.get(index).id > lastPos
                    && !this.mRecentTaskInfo
                            .get(index)
                            .baseIntent
                            .hasCategory("android.intent.category.HOME")
                    && this.mRecentTaskInfo.get(index).id > -1) {
                lastPos = this.mRecentTaskInfo.get(index).id;
                prevPos = index;
            }
        }
        List<ActivityManager.RecentTaskInfo> list = this.mRecentTaskInfo;
        if (prevID > 0) {
            prevPos = prevID;
        }
        ComponentName component = list.get(prevPos).baseIntent.getComponent();
        component.getClass();
        String strFlattenToString = component.flattenToString();
        if (prevID <= 0) {
            lastID = lastPos;
        }
        moveTaskToFront(strFlattenToString, lastID);
    }

    private void doLastApp() {
        if (DEBUG) {
            Log.d(TAG, "doLastApp()");
        }
        int index = 0;
        if (Build.VERSION.SDK_INT >= 21) {
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
                return;
            }
            return;
        }
        List<ActivityManager.RecentTaskInfo> recentTasks =
                this.mActivityManager.getRecentTasks(3, 0);
        this.mRecentTaskInfo = recentTasks;
        if (recentTasks.size() > 1
                && !this.mRecentTaskInfo
                        .get(1)
                        .baseIntent
                        .hasCategory("android.intent.category.HOME")) {
            index = 1;
        } else if (this.mRecentTaskInfo.size() > 2
                && !this.mRecentTaskInfo
                        .get(2)
                        .baseIntent
                        .hasCategory("android.intent.category.HOME")) {
            index = 2;
        }
        ComponentName component = this.mRecentTaskInfo.get(index).baseIntent.getComponent();
        component.getClass();
        moveTaskToFront(component.flattenToString(), this.mRecentTaskInfo.get(index).id);
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

    private void moveTaskToFront(String component, int taskId) {
        if (DEBUG) {
            Log.d(TAG, "moveTaskToFront(" + component + ", " + taskId + ")");
        }
        if (Build.VERSION.SDK_INT >= 16) {
            this.mRootContext.runCommandRemote("am move-to-front " + taskId, true);
            return;
        }
        this.mActivityManager.moveTaskToFront(taskId, 0);
    }

    private void startActivity(String component) {
        if (DEBUG) {
            Log.d(TAG, "startActivity(" + component + ")");
        }
        if (Build.VERSION.SDK_INT >= 18) {
            this.mRootContext.runCommandRemote(
                    "am start -a android.intent.action.MAIN -n " + component, true);
            return;
        }
        this.mRootContext.runCommandRoot(
                "am start -a android.intent.action.MAIN -n " + component, true);
    }

    private void doActivityAction(String component) {
        if (DEBUG) {
            Log.d(TAG, "doActivityAction(" + component + ")");
        }
        if (component == null || component.length() <= 0) {
            return;
        }
        if (this.mRootContext.isRootAvailable(false)) {
            if (Build.VERSION.SDK_INT >= 21) {
                this.mRootContext.runCommandRemote("am move-to-front-or-start " + component, true);
                return;
            }
            if (Build.VERSION.SDK_INT >= 16) {
                this.mRunningTaskInfo = this.mActivityManager.getRunningTasks(20);
                for (int index = 1; index < this.mRunningTaskInfo.size(); index++) {
                    if (this.mRunningTaskInfo
                            .get(index)
                            .baseActivity
                            .flattenToString()
                            .equals(component)) {
                        moveTaskToFront(component, this.mRunningTaskInfo.get(index).id);
                        return;
                    }
                }
                startActivity(component);
                return;
            }
            startActivity(component);
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
            if (((WifiManager) this.mContext.getApplicationContext().getSystemService("wifi"))
                    .isWifiEnabled()) {
                this.mRootContext.runCommandRoot("svc wifi disable", true);
                return;
            } else {
                this.mRootContext.runCommandRoot("svc wifi enable", true);
                return;
            }
        }
        WifiManager wifiManager =
                (WifiManager) this.mContext.getApplicationContext().getSystemService("wifi");
        wifiManager.setWifiEnabled(!wifiManager.isWifiEnabled());
    }

    private void doDataToggleAction() {
        if (DEBUG) {
            Log.d(TAG, "doDataToggleAction()");
        }
        boolean enable = true;
        if (Build.VERSION.SDK_INT >= 20) {
            if (((TelephonyManager) this.mContext.getSystemService("phone")).getDataState() == 2) {
                this.mRootContext.runCommandRoot("svc data disable", true);
                return;
            } else {
                this.mRootContext.runCommandRoot("svc data enable", true);
                return;
            }
        }
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) this.mContext.getSystemService("connectivity");
            TelephonyManager telephonyManager =
                    (TelephonyManager) this.mContext.getSystemService("phone");
            Field declaredField =
                    Class.forName(connectivityManager.getClass().getName())
                            .getDeclaredField("mService");
            declaredField.setAccessible(true);
            Object obj = declaredField.get(connectivityManager);
            Method declaredMethod =
                    Class.forName(obj.getClass().getName())
                            .getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            declaredMethod.setAccessible(true);
            Object[] objArr = new Object[1];
            if (telephonyManager.getDataState() == 2) {
                enable = false;
            }
            objArr[0] = Boolean.valueOf(enable);
            declaredMethod.invoke(obj, objArr);
        } catch (Exception unused) {
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
        if (Build.VERSION.SDK_INT >= 21) {
            this.mRootContext.runCommandRemote("am kill-app 0", false);
            return;
        }
        this.mRunningTaskInfo = this.mActivityManager.getRunningTasks(1);
        TimerTask timerTask = new TimerTask() { // from class: com.noname81.lmt.Launcher.1
                    @Override // java.util.TimerTask, java.lang.Runnable
                    public void run() {
                        String packageName =
                                ((ActivityManager.RunningTaskInfo)
                                                Launcher.this.mRunningTaskInfo.get(0))
                                        .baseActivity.getPackageName();
                        if (packageName.contains("com.noname81.lmt")
                                || packageName.contains("com.android.systemui")) {
                            return;
                        }
                        Launcher.this.mRootContext.runCommandRoot(
                                "am force-stop " + packageName, false);
                        if (Launcher.DEBUG) {
                            Log.d(Launcher.TAG, "Kill app " + packageName);
                        }
                    }
                };
        doKeyAction(3);
        new Timer().schedule(timerTask, 1000L);
    }

    private void doKillAllAppsAction() {
        if (DEBUG) {
            Log.d(TAG, "doKillAllAppsAction()");
        }
        if (Build.VERSION.SDK_INT >= 21) {
            this.mRootContext.runCommandRemote("am kill-all-apps 0", false);
            return;
        }
        this.mRunningTaskInfo = this.mActivityManager.getRunningTasks(100);
        TimerTask timerTask = new TimerTask() { // from class: com.noname81.lmt.Launcher.2
                    @Override // java.util.TimerTask, java.lang.Runnable
                    public void run() {
                        for (int index = 0;
                                index < Launcher.this.mRunningTaskInfo.size();
                                index++) {
                            String packageName =
                                    ((ActivityManager.RunningTaskInfo)
                                                    Launcher.this.mRunningTaskInfo.get(index))
                                            .baseActivity.getPackageName();
                            if (!packageName.contains("com.noname81.lmt")
                                    && !packageName.contains("com.android.systemui")) {
                                Launcher.this.mActivityManager.killBackgroundProcesses(packageName);
                                if (Launcher.DEBUG) {
                                    Log.d(Launcher.TAG, "Kill app " + packageName);
                                }
                            }
                        }
                    }
                };
        doKeyAction(3);
        new Timer().schedule(timerTask, 1000L);
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
        this.mContext.sendBroadcast(TaskerIntents.runTask(taskName));
    }

    void doOpenNotificationBar() {
        if (DEBUG) {
            Log.d(TAG, "doOpenNotificationBar()");
        }
        if (AccessibilityHandler.isAccessibilityAvailable(this.mContext, false)) {
            AccessibilityHandler.performAction(4);
            return;
        }
        if (Build.VERSION.SDK_INT >= 17) {
            try {
                Class.forName("android.app.StatusBarManager")
                        .getMethod("expandNotificationsPanel", new Class[0])
                        .invoke(
                                this.mContext.getApplicationContext().getSystemService("statusbar"),
                                new Object[0]);
                return;
            } catch (Exception unused) {
                AccessibilityHandler.isAccessibilityAvailable(this.mContext, true);
                return;
            }
        }
        try {
            Class.forName("android.app.StatusBarManager")
                    .getMethod("expand", new Class[0])
                    .invoke(
                            this.mContext.getApplicationContext().getSystemService("statusbar"),
                            new Object[0]);
        } catch (Exception unused2) {
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
        if (Build.VERSION.SDK_INT >= 17) {
            try {
                Class.forName("android.app.StatusBarManager")
                        .getMethod("expandSettingsPanel", new Class[0])
                        .invoke(
                                this.mContext.getApplicationContext().getSystemService("statusbar"),
                                new Object[0]);
                return;
            } catch (Exception unused) {
                AccessibilityHandler.isAccessibilityAvailable(this.mContext, true);
                return;
            }
        }
        AccessibilityHandler.isAccessibilityAvailable(this.mContext, true);
    }

    private void doOpenKeyboard() {
        if (DEBUG) {
            Log.d(TAG, "doOpenKeyboard()");
        }
        ((InputMethodManager) this.mContext.getSystemService("input_method")).toggleSoftInput(2, 1);
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
        this.mRootContext.runCommandRoot(
                "am start -a com.teslacoilsw.launcher.ACTION -n"
                        + " com.teslacoilsw.launcher/.NovaShortcutHandler",
                true);
    }

    private void doOpenApex() {
        if (DEBUG) {
            Log.d(TAG, "doOpenApex()");
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
        this.mRootContext.runCommandRoot(
                "am start -a com.mobint.hololauncher.ACTION -n com.mobint.hololauncher/.Launcher -e"
                        + " ACTION 'DRAWER'",
                true);
    }
}
