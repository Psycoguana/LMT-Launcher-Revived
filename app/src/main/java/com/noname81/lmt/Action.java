package com.noname81.lmt;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import java.io.File;

/* JADX INFO: loaded from: classes.dex */
class Action {
    static final int Activity = 27;
    static final int Apex = 41;
    static final int App = 2;
    static final int Assistant = 37;
    static final int Back = 7;
    static final int BackLongpress = 8;
    static final int BluetoothToggle = 22;
    static final int DataToggle = 21;
    static final int GPSToggle = 23;
    static final int Holo = 42;
    static final int Home = 3;
    static final int HomeLongpress = 4;
    static final int ImmersiveModeToggle = 24;
    static final int Key = 26;
    static final int KillAllApps = 31;
    static final int KillApp = 30;
    static final int LastApp = 14;
    static final int Menu = 5;
    static final int MenuLongpress = 6;
    static final int NextApp = 12;
    static final int None = 1;
    static final int Nova = 40;
    static final int NowOnTap = 36;
    static final int OffsetAdvancedCommands = 25;
    static final int OffsetAppDrawer = 39;
    static final int OffsetNormalCommands = 0;
    static final int OffsetPieActions = 43;
    static final int OffsetToggleCommands = 19;
    static final int OpenKeyboard = 17;
    static final int OpenNotificationBar = 15;
    static final int OpenPowerMenu = 18;
    static final int OpenQuickSettings = 16;
    static final int PiePointer = 44;
    static final int PieRecentApps = 45;
    static final int PrevApp = 13;
    static final int RecentApps = 9;
    static final int Screenshot = 34;
    static final int Script = 29;
    static final int Search = 10;
    static final int SearchLongpress = 11;
    static final int Shortcut = 33;
    static final int SplitScreen = 38;
    private static final String TAG = "LMT::Action";
    static final int TaskerTask = 32;
    static final int UnpinApp = 35;
    static final int WebPage = 28;
    static final int WifiToggle = 20;
    private Drawable mIcon;
    private String mString;
    private int mType;
    static final String[] names = {
        "OffsetNormalCommands",
        "None",
        "App",
        "Home",
        "HomeLongpress",
        "Menu",
        "MenuLongpress",
        "Back",
        "BackLongpress",
        "RecentApps",
        "Search",
        "SearchLongpress",
        "NextApp",
        "PrevApp",
        "LastApp",
        "OpenNotificationBar",
        "OpenQuickSettings",
        "OpenKeyboard",
        "OpenPowerMenu",
        "OffsetToggleCommands",
        "Wifi",
        "Data",
        "Bluetooth",
        "GPS",
        "ImmersiveMode",
        "OffsetAdvancedCommands",
        "Key",
        "Activity",
        "WebPage",
        "Script",
        "KillApp",
        "KillAllApps",
        "TaskerTask",
        "Shortcut",
        "Screenshot",
        "UnpinApp",
        "NowOnTap",
        "Assistant",
        "SplitScreen",
        "OffsetAppDrawer",
        "NovaLauncher",
        "ApexLauncher",
        "HoloLauncher",
        "OffsetPieActions",
        "PiePointer",
        "PieRecentApps"
    };
    static final String[] captions = {
        "OffsetNormalCommands",
        "Trigger no command",
        "Start an app [Root|None]",
        "Trigger home key [Accessibility|Root|None]",
        "Trigger longpress home key [Root]",
        "Trigger menu key [Root]",
        "Trigger longpress menu key [Root]",
        "Trigger back key [Accessibility|Root]",
        "Trigger longpress back key [Root]",
        "Trigger recent apps [Accessibility|Root]",
        "Activate search [Root|None]",
        "Activate voice search [None]",
        "Switch to next active app [Root]",
        "Switch to previous active app [Root]",
        "Switch to last active app [Root]",
        "Open the notification bar [Accessibility]",
        "Open the quick settings [Accessibility]",
        "Open the keyboard [None]",
        "Open the power menu [Accessibility|Root]",
        "OffsetToggleCommands",
        "Toggle Wifi mode [None]",
        "Toggle mobile data mode [Root]",
        "Toggle Bluetooth mode [None]",
        "Toggle GPS mode [None]",
        "Toggle Immersive mode [Root]",
        "OffsetAdvancedCommands",
        "Trigger an arbitrary key [Root]",
        "Start an activity [Root|None]",
        "Open an arbitrary webpage [None]",
        "Trigger an arbitrary script [Root]",
        "Kill the current app and switch to homescreen [Root]",
        "Kill all apps and switch to homescreen [Root]",
        "Trigger a tasker task [Root]",
        "Trigger a shortcut [None]",
        "Trigger a screenshot [Root]",
        "Unpin pinned app [Root]",
        "Activate Google Now On Tap [Root]",
        "Activate Google Assistant [Root]",
        "Toggle Split Screen [Accessibility]",
        "OffsetAppDrawer",
        "Open AppDrawer from NovaLauncher [Root]",
        "Open AppDrawer from ApexLauncher [Root]",
        "Open AppDrawer from HoloLauncher [Root]",
        "OffsetPieActions",
        "Activate pie pointer extension [Accessibility|Root]",
        "Activate Pie Recent Apps [Root|UsageStats]"
    };

    Action(int i) {
        this.mType = i;
    }

    Action(int i, String str) {
        this.mType = i;
        this.mString = str;
        this.mIcon = null;
    }

    Action(int i, String str, Drawable drawable) {
        this.mType = i;
        this.mString = str;
        this.mIcon = drawable;
    }

    Action(int i, String str, String str2, Context context) {
        this.mType = i;
        this.mString = str;
        this.mIcon = IconUtils.convertBase64StringToDrawable(context, str2);
    }

    int getType() {
        return this.mType;
    }

    public void setType(int i) {
        this.mType = i;
    }

    public String getString() {
        return this.mString;
    }

    public void setString(String str) {
        this.mString = str;
    }

    public Drawable getIcon() {
        return this.mIcon;
    }

    public void setIcon(Drawable drawable) {
        this.mIcon = drawable;
    }

    /* JADX WARN: Code duplicated, block: B:7:0x000e  */
    public String toString() {
        String str;
        int i = this.mType;
        String str2 = "none";
        if (i > 0) {
            String[] strArr = names;
            if (i <= strArr.length) {
                str = strArr[i];
            } else {
                str = "none";
            }
        } else {
            str = "none";
        }
        String str3 = this.mString;
        if (str3 != null && str3.length() > 0) {
            str2 = this.mString;
        }
        return "type: " + str + ", string: " + str2;
    }

    Drawable getDrawable(Context context, String str, int i, int i2, boolean z) {
        if (this.mType == 1 && !z) {
            return null;
        }
        int identifier = 0;
        try {
            String strLoadResourceSearchPath =
                    SettingsValues.getInstance(context).loadResourceSearchPath();
            String str2 = names[this.mType].toLowerCase() + ".png";
            if (str != null) {
                File file = new File(strLoadResourceSearchPath + str);
                if (file.exists()) {
                    file.setReadable(true, false);
                    Drawable drawableCreateFromPath =
                            Drawable.createFromPath(strLoadResourceSearchPath + str);
                    if (drawableCreateFromPath != null) {
                        return IconUtils.resizeImage(context, drawableCreateFromPath, i);
                    }
                    Log.e(TAG, "Failed to load available image " + strLoadResourceSearchPath + str);
                }
            }
            if (new File(strLoadResourceSearchPath + str2).exists()) {
                Drawable drawableCreateFromPath2 =
                        Drawable.createFromPath(strLoadResourceSearchPath + str2);
                if (drawableCreateFromPath2 != null) {
                    return IconUtils.resizeImage(context, drawableCreateFromPath2, i);
                }
                Log.e(TAG, "Failed to load available image " + strLoadResourceSearchPath + str2);
            }
        } catch (Exception unused) {
        }
        Drawable drawable = this.mIcon;
        if (drawable != null) {
            return IconUtils.resizeImage(context, drawable, i2);
        }
        if (this.mType == 2 && i2 > 0) {
            try {
                return IconUtils.resizeImage(
                        context, context.getPackageManager().getApplicationIcon(this.mString), i2);
            } catch (Exception unused2) {
            }
        }
        if (this.mType == 27 && i2 > 0) {
            PackageManager packageManager = context.getPackageManager();
            try {
                String str3 = this.mString;
                return IconUtils.resizeImage(
                        context,
                        packageManager.getApplicationIcon(str3.substring(0, str3.lastIndexOf(47))),
                        i2);
            } catch (Exception unused3) {
            }
        }
        try {
            Resources resources = context.getResources();
            int iLoadPieNavButtonStyle =
                    SettingsValues.getInstance(context).loadPieNavButtonStyle();
            if (iLoadPieNavButtonStyle == 1) {
                identifier =
                        resources.getIdentifier(
                                names[this.mType].toLowerCase() + "_kitkat",
                                "drawable",
                                context.getPackageName());
            } else if (iLoadPieNavButtonStyle == 2) {
                identifier =
                        resources.getIdentifier(
                                names[this.mType].toLowerCase() + "_pixel",
                                "drawable",
                                context.getPackageName());
            }
            if (identifier == 0) {
                identifier =
                        resources.getIdentifier(
                                names[this.mType].toLowerCase(),
                                "drawable",
                                context.getPackageName());
            }
            return IconUtils.getDrawable(context, identifier);
        } catch (Exception unused4) {
            return null;
        }
    }

    void checkNeededPermissions(Activity activity, boolean z) {
        int type = getType();
        if (type == 33) {
            if (z && this.mString.contains("android.intent.action.CALL")) {
                PermissionChecker.getInstance().checkAndRequestPhoneCallPermission(activity, true);
                return;
            }
            return;
        }
        if (type == 45
                && z
                && Build.VERSION.SDK_INT >= 21
                && !RootContext.getInstance(activity).isRootAvailable(false)) {
            PermissionChecker.getInstance().checkAndRequestUsageStatsPermission(activity, true);
        }
    }
}
