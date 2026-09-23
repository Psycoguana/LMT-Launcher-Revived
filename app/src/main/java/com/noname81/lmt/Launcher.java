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

/* JADX INFO: loaded from: classes.dex */
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

    static void setDebug(int i) {
        DEBUG = i != 1 ? false : true;
    }

    void fireAction(Action action) {
        Log.d(TAG, "Fire action " + action.toString());
        switch (action.getType()) {
            case 2:
                doAppAction(action.getString());
                break;
            case 3:
                doHomeAction();
                break;
            case 4:
                doHomeLongpressAction();
                break;
            case 5:
                doMenuAction();
                break;
            case 6:
                doMenuLongpressAction();
                break;
            case 7:
                doBackAction();
                break;
            case 8:
                doBackLongpressAction();
                break;
            case 9:
                doRecentAppsAction();
                break;
            case 10:
                doSearchAction();
                break;
            case 11:
                doSearchLongpressAction();
                break;
            case 12:
                doNextApp();
                break;
            case 13:
                doPrevApp();
                break;
            case 14:
                doLastApp();
                break;
            case 15:
                doOpenNotificationBar();
                break;
            case 16:
                doOpenQuickSettings();
                break;
            case 17:
                doOpenKeyboard();
                break;
            case 18:
                doPowerMenuAction();
                break;
            case 20:
                doWifiToggleAction();
                break;
            case 21:
                doDataToggleAction();
                break;
            case 22:
                doBluetoothToggleAction();
                break;
            case 23:
                doGPSToggleAction();
                break;
            case 24:
                doImmersiveModeToggleAction();
                break;
            case 26:
                doKeyAction(action.getString());
                break;
            case 27:
                doActivityAction(action.getString());
                break;
            case 28:
                doWebPageAction(action.getString());
                break;
            case 29:
                doScriptAction(action.getString());
                break;
            case 30:
                doKillAppAction();
                break;
            case 31:
                doKillAllAppsAction();
                break;
            case 32:
                doTaskerAction(action.getString());
                break;
            case 33:
                doShortcutAction(action.getString());
                break;
            case 34:
                doScreenshotAction();
                break;
            case 35:
                doUnpinAppAction();
                break;
            case 36:
                doNowOnTapAction();
                break;
            case 37:
                doAssistantAction();
                break;
            case 38:
                doSplitScreenAction();
                break;
            case 40:
                doOpenNova();
                break;
            case 41:
                doOpenApex();
                break;
            case 42:
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
        int i = Integer.MAX_VALUE;
        int i2 = Integer.MAX_VALUE;
        int i3 = 0;
        int i4 = 0;
        for (int i5 = 1; i5 < this.mRecentTaskInfo.size(); i5++) {
            if (this.mRecentTaskInfo.get(i5).id < i
                    && this.mRecentTaskInfo.get(i5).id > this.mRecentTaskInfo.get(0).id
                    && !this.mRecentTaskInfo
                            .get(i5)
                            .baseIntent
                            .hasCategory("android.intent.category.HOME")) {
                i = this.mRecentTaskInfo.get(i5).id;
                i3 = i5;
            }
            if (this.mRecentTaskInfo.get(i5).id < i2
                    && !this.mRecentTaskInfo
                            .get(i5)
                            .baseIntent
                            .hasCategory("android.intent.category.HOME")
                    && this.mRecentTaskInfo.get(i5).id > -1) {
                i2 = this.mRecentTaskInfo.get(i5).id;
                i4 = i5;
            }
        }
        List<ActivityManager.RecentTaskInfo> list = this.mRecentTaskInfo;
        if (i3 > 0) {
            i4 = i3;
        }
        ComponentName component = list.get(i4).baseIntent.getComponent();
        component.getClass();
        String strFlattenToString = component.flattenToString();
        if (i3 <= 0) {
            i = i2;
        }
        moveTaskToFront(strFlattenToString, i);
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
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        for (int i5 = 1; i5 < this.mRecentTaskInfo.size(); i5++) {
            if (this.mRecentTaskInfo.get(i5).id > i2
                    && this.mRecentTaskInfo.get(i5).id < this.mRecentTaskInfo.get(0).id
                    && !this.mRecentTaskInfo
                            .get(i5)
                            .baseIntent
                            .hasCategory("android.intent.category.HOME")) {
                i2 = this.mRecentTaskInfo.get(i5).id;
                i = i5;
            }
            if (this.mRecentTaskInfo.get(i5).id > i4
                    && !this.mRecentTaskInfo
                            .get(i5)
                            .baseIntent
                            .hasCategory("android.intent.category.HOME")
                    && this.mRecentTaskInfo.get(i5).id > -1) {
                i4 = this.mRecentTaskInfo.get(i5).id;
                i3 = i5;
            }
        }
        List<ActivityManager.RecentTaskInfo> list = this.mRecentTaskInfo;
        if (i > 0) {
            i3 = i;
        }
        ComponentName component = list.get(i3).baseIntent.getComponent();
        component.getClass();
        String strFlattenToString = component.flattenToString();
        if (i <= 0) {
            i2 = i4;
        }
        moveTaskToFront(strFlattenToString, i2);
    }

    private void doLastApp() {
        if (DEBUG) {
            Log.d(TAG, "doLastApp()");
        }
        int i = 0;
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
            i = 1;
        } else if (this.mRecentTaskInfo.size() > 2
                && !this.mRecentTaskInfo
                        .get(2)
                        .baseIntent
                        .hasCategory("android.intent.category.HOME")) {
            i = 2;
        }
        ComponentName component = this.mRecentTaskInfo.get(i).baseIntent.getComponent();
        component.getClass();
        moveTaskToFront(component.flattenToString(), this.mRecentTaskInfo.get(i).id);
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

    private void doKeyAction(String str) {
        if (DEBUG) {
            Log.d(TAG, "doKeyAction(" + str + ")");
        }
        this.mRootContext.runCommandRemote("input keyevent " + str, true);
    }

    private void doKeyAction(int i) {
        if (DEBUG) {
            Log.d(TAG, "doKeyAction()");
        }
        this.mRootContext.runCommandRemote("input keyevent " + i, true);
    }

    private boolean doAppAction(String str) {
        Intent launchIntentForPackage;
        if (DEBUG) {
            Log.d(TAG, "doAppAction(" + str + ")");
        }
        if (str == null
                || str.length() <= 0
                || (launchIntentForPackage =
                                this.mContext.getPackageManager().getLaunchIntentForPackage(str))
                        == null) {
            return false;
        }
        doActivityAction(launchIntentForPackage.getComponent().flattenToString());
        return true;
    }

    private void moveTaskToFront(String str, int i) {
        if (DEBUG) {
            Log.d(TAG, "moveTaskToFront(" + str + ", " + i + ")");
        }
        if (Build.VERSION.SDK_INT >= 16) {
            this.mRootContext.runCommandRemote("am move-to-front " + i, true);
            return;
        }
        this.mActivityManager.moveTaskToFront(i, 0);
    }

    private void startActivity(String str) {
        if (DEBUG) {
            Log.d(TAG, "startActivity(" + str + ")");
        }
        if (Build.VERSION.SDK_INT >= 18) {
            this.mRootContext.runCommandRemote(
                    "am start -a android.intent.action.MAIN -n " + str, true);
            return;
        }
        this.mRootContext.runCommandRoot("am start -a android.intent.action.MAIN -n " + str, true);
    }

    private void doActivityAction(String str) {
        if (DEBUG) {
            Log.d(TAG, "doActivityAction(" + str + ")");
        }
        if (str == null || str.length() <= 0) {
            return;
        }
        if (this.mRootContext.isRootAvailable(false)) {
            if (Build.VERSION.SDK_INT >= 21) {
                this.mRootContext.runCommandRemote("am move-to-front-or-start " + str, true);
                return;
            }
            if (Build.VERSION.SDK_INT >= 16) {
                this.mRunningTaskInfo = this.mActivityManager.getRunningTasks(20);
                for (int i = 1; i < this.mRunningTaskInfo.size(); i++) {
                    if (this.mRunningTaskInfo.get(i).baseActivity.flattenToString().equals(str)) {
                        moveTaskToFront(str, this.mRunningTaskInfo.get(i).id);
                        return;
                    }
                }
                startActivity(str);
                return;
            }
            startActivity(str);
            return;
        }
        Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
        intent.setComponent(ComponentName.unflattenFromString(str));
        intent.setFlags(268435456);
        this.mContext.startActivity(intent);
    }

    private void doWebPageAction(String str) {
        if (DEBUG) {
            Log.d(TAG, "doWebPageAction(" + str + ")");
        }
        if (!str.contains("http")) {
            if (!str.contains("www")) {
                str = "http://www." + str;
            } else {
                str = "http://" + str;
            }
        }
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addFlags(268435456);
        intent.setData(Uri.parse(str));
        try {
            this.mContext.startActivity(intent);
        } catch (ActivityNotFoundException unused) {
        }
    }

    private void doScriptAction(String str) {
        if (DEBUG) {
            Log.d(TAG, "doScriptAction(" + str + ")");
        }
        if (str == null || str.length() <= 0) {
            return;
        }
        if (str.contains(".sh")) {
            this.mRootContext.runCommandRoot("sh " + str, true);
            return;
        }
        this.mRootContext.runCommandRoot(str, true);
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
        boolean z = true;
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
                z = false;
            }
            objArr[0] = Boolean.valueOf(z);
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
                        for (int i = 0; i < Launcher.this.mRunningTaskInfo.size(); i++) {
                            String packageName =
                                    ((ActivityManager.RunningTaskInfo)
                                                    Launcher.this.mRunningTaskInfo.get(i))
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

    private void doTaskerAction(String str) {
        if (DEBUG) {
            Log.d(TAG, "doTaskerAction()");
        }
        if (str == null || str.length() <= 0) {
            return;
        }
        this.mRootContext.runCommandRoot(
                "am broadcast -a net.dinglisch.android.tasker.ACTION_TASK -e task_name '"
                        + str
                        + "'",
                true);
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

    private void doShortcutAction(String str) {
        if (DEBUG) {
            Log.d(TAG, "doShortcutAction()");
        }
        try {
            Intent uri = Intent.parseUri(str, 0);
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
