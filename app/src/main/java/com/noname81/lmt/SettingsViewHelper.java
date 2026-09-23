package com.noname81.lmt;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.Switch;

import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
class SettingsViewHelper implements AdapterView.OnItemClickListener {
    private Activity mActivity;
    private RootContext mRootContext;
    private SettingsValues mSettings;
    private TouchServiceNative mTouchServiceNative;
    int posOffsetTouchservice = 255;
    int posActivateTouchService = 255;
    int posAutostartTouchService = 255;
    int posSetMode = 255;
    int posOffsetGestureRecognition = 255;
    int posAutoConfiguration = 255;
    int posSetInput = 255;
    int posTouchscreenScreenFactorX = 255;
    int posTouchscreenScreenFactorY = 255;
    int posOffsetGestures = 255;
    int posFeedbackStyle = 255;
    int posVibrationTime = 255;
    int posSingleTouchGestures = 255;
    int posMinScore = 255;
    int posMinPathLength = 255;
    int posAddBlacklist = 255;
    int posClearBlacklist = 255;
    int posOffsetIsas = 255;
    int posSingleSwipesBBox = 255;
    int posSingleSwipesAArea = 255;
    int posOffsetPieActivation = 255;
    int posPiePos = 255;
    int posPieAreaX = 255;
    int posPieAreaY = 255;
    int posPieAreaGravity = 255;
    int posPieAreaBehindKeyboard = 255;
    int posPieOnLockScreen = 255;
    int posAddBlacklistPie = 255;
    int posClearBlacklistPie = 255;
    int posOffsetPieStyle = 255;
    int posPieColor = 255;
    int posPieInnerRadius = 255;
    int posPieOuterRadius = 255;
    int posPieShiftSize = 255;
    int posPieOutlineSize = 255;
    int posPieSliceGap = 255;
    int posPieStartGap = 255;
    int posOffsetPieBehavior = 255;
    int posPieLongpress = 255;
    int posPieAnimation = 255;
    int posPieVibrate = 255;
    int posPieVibrationTime = 255;
    int posPieMultiCommand = 255;
    int posPieExpandTriggerArea = 255;
    int posOffsetPieIcons = 255;
    int posPieRotateImages = 255;
    int posPieNavButtonStyle = 255;
    int posPieShowScaleAppImages = 255;
    int posPieShowScaleUserImages = 255;
    int posPieUserImageSearchPath = 255;
    int posOffsetPieExtensions = 255;
    int posPiePointerFromEdges = 255;
    int posPiePointerWarpFactor = 255;
    int posPiePointerColor = 255;
    int posPieShowStatusInfos = 255;
    int posPieStatusInfoColor = 255;
    int posPieStatusInfoFont = 255;

    SettingsViewHelper(Activity activity) {
        this.mActivity = activity;
        this.mSettings = SettingsValues.getInstance(activity);
        this.mRootContext = RootContext.getInstance(activity);
        this.mTouchServiceNative = TouchServiceNative.getInstance(activity);
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        if (i == this.posActivateTouchService) {
            if (this.mSettings.getServiceState() == 0) {
                this.mSettings.startService();
                return;
            } else {
                this.mSettings.stopService();
                return;
            }
        }
        if (i == this.posAutostartTouchService) {
            new InputDialog(
                    this.mActivity,
                    "Autostart service",
                    new String[] {"Disabled", "Enabled"},
                    Integer.toString(this.mSettings.loadAutostart())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.1
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str) {
                    try {
                        SettingsViewHelper.this.mSettings.saveAutostart(Integer.parseInt(str));
                        return true;
                    } catch (NumberFormatException unused) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posSetMode) {
            new InputDialog(
                    this.mActivity,
                    "Define feature set",
                    new String[] {
                        "Gestures [Root] and ISAS [Root] ",
                        "Gestures [Root] , ISAS [Root] and Pie",
                        "Pie"
                    },
                    Integer.toString(this.mSettings.loadTouchServiceMode())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.2
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str) {
                    try {
                        SettingsViewHelper.this.mSettings.saveTouchServiceMode(
                                Integer.parseInt(str));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        SettingsViewHelper.this.mActivity.recreate();
                        return true;
                    } catch (NumberFormatException unused) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posAutoConfiguration) {
            int inputDevice = -1;
            int factorX = -1;
            int factorY = -1;
            try {
                String[] strArrSplit =
                        this.mRootContext
                                .runCommandResult(
                                        "getevent -p | grep -E '(device|name|0035|0036)'", true)
                                .split("\n");
                int i3 = -1;
                factorX = -1;
                factorY = -1;
                for (int i4 = 0; i4 < strArrSplit.length - 1; i4++) {
                    if (strArrSplit[i4].contains("0035")) {
                        int i5 = i4 + 1;
                        if (strArrSplit[i5].contains("0036")) {
                            for (int i6 = i4 - 1; i6 >= 0; i6--) {
                                if (strArrSplit[i6].contains("/dev/input")) {
                                    String str = strArrSplit[i6];
                                    i3 = Integer.parseInt(str.substring(str.length() - 1));
                                    break;
                                }
                            }
                            WindowManager windowManager =
                                    (WindowManager) this.mActivity.getSystemService("window");
                            DisplayMetrics displayMetrics = new DisplayMetrics();
                            windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
                            int i7 = displayMetrics.widthPixels;
                            int i8 = displayMetrics.heightPixels;
                            int iIndexOf = strArrSplit[i4].indexOf("max");
                            factorX =
                                    Math.round(
                                            (Integer.parseInt(
                                                                    strArrSplit[i4].substring(
                                                                            iIndexOf + 4,
                                                                            strArrSplit[i4].indexOf(
                                                                                    ",", iIndexOf)))
                                                            / i7)
                                                    * 100.0f);
                            int iIndexOf2 = strArrSplit[i5].indexOf("max");
                            factorY =
                                    Math.round(
                                            (Integer.parseInt(
                                                                    strArrSplit[i5].substring(
                                                                            iIndexOf2 + 4,
                                                                            strArrSplit[i5].indexOf(
                                                                                    ",",
                                                                                    iIndexOf2)))
                                                            / i8)
                                                    * 100.0f);
                        }
                    }
                }
                inputDevice = i3;
            } catch (Exception unused) {
                factorX = -1;
                factorY = -1;
            }
            // Captured by the dialog listener below, so snapshot into finals.
            // These three names are the original author's: the reference dex still
            // carries them in the listener's synthetic capture fields
            // (val$inputDeviceFinal / val$factorXFinal / val$factorYFinal).
            final int inputDeviceFinal = inputDevice;
            final int factorXFinal = factorX;
            final int factorYFinal = factorY;
            new AlertDialog.Builder(this.mActivity)
                    .setTitle("Do you want to use these values?")
                    .setMessage(
                            "Input device: "
                                    + inputDeviceFinal
                                    + "\nTouchscreen/screen factor x: "
                                    + factorXFinal
                                    + "\nTouchscreen/screen factor y: "
                                    + factorYFinal)
                    .setCancelable(false)
                    .setPositiveButton(
                            "ok",
                            new DialogInterface.OnClickListener() { // from class:
                                // com.noname81.lmt.SettingsViewHelper.4
                                @Override // android.content.DialogInterface.OnClickListener
                                public void onClick(DialogInterface dialogInterface, int i9) {
                                    dialogInterface.dismiss();
                                    if (inputDeviceFinal <= -1
                                            || factorXFinal <= -1
                                            || factorYFinal <= -1) {
                                        return;
                                    }
                                    SettingsViewHelper.this.mSettings.saveInputDevice(
                                            inputDeviceFinal);
                                    SettingsViewHelper.this.mSettings.saveTouchscreenScreenFactorX(
                                            factorXFinal);
                                    SettingsViewHelper.this.mSettings.saveTouchscreenScreenFactorY(
                                            factorYFinal);
                                    SettingsViewHelper.this.mTouchServiceNative
                                            .setInputDeviceUnlock(
                                                    SettingsViewHelper.this.mSettings
                                                            .loadInputDeviceString());
                                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                                }
                            })
                    .setNegativeButton(
                            "cancel",
                            new DialogInterface.OnClickListener() { // from class:
                                // com.noname81.lmt.SettingsViewHelper.3
                                @Override // android.content.DialogInterface.OnClickListener
                                public void onClick(DialogInterface dialogInterface, int i9) {
                                    dialogInterface.dismiss();
                                }
                            })
                    .create()
                    .show();
            return;
        }
        if (i == this.posSetInput) {
            new InputDialog(
                    this.mActivity,
                    "Set input device",
                    new String[] {
                        "Input device 0",
                        "Input device 1",
                        "Input device 2",
                        "Input device 3",
                        "Input device 4",
                        "Input device 5",
                        "Input device 6",
                        "Input device 7",
                        "Input device 8",
                        "Input device 9",
                        "Input device 10",
                        "Input device 11",
                        "Input device 12",
                        "Input device 13",
                        "Input device 14",
                        "Input device 15",
                        "Input device 16",
                        "Input device 17",
                        "Input device 18",
                        "Input device 19",
                        "Input device 20"
                    },
                    Integer.toString(this.mSettings.loadInputDevice())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.5
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.saveInputDevice(Integer.parseInt(str2));
                        SettingsViewHelper.this.mTouchServiceNative.setInputDeviceUnlock(
                                SettingsViewHelper.this.mSettings.loadInputDeviceString());
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posAddBlacklist) {
            MultiSelectActivity.AppSelectMode = 3;
            this.mActivity.startActivity(
                    new Intent(this.mActivity, (Class<?>) MultiSelectActivity.class));
            return;
        }
        if (i == this.posClearBlacklist) {
            this.mSettings.clearBlacklisted();
            return;
        }
        if (i == this.posAddBlacklistPie) {
            MultiSelectActivity.AppSelectMode = 4;
            this.mActivity.startActivity(
                    new Intent(this.mActivity, (Class<?>) MultiSelectActivity.class));
            return;
        }
        if (i == this.posClearBlacklistPie) {
            this.mSettings.clearBlacklistedPie();
            return;
        }
        if (i == this.posFeedbackStyle) {
            new InputDialog(
                    this.mActivity,
                    "Feedback style",
                    new String[] {
                        "No feedback", "Debug overlay", "Image overlay", "Tactile feedback"
                    },
                    Integer.toString(this.mSettings.loadFeedbackMode())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.6
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        Toaster.getInstance(this.mContext).setMode(Integer.parseInt(str2));
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posVibrationTime) {
            new InputDialog(
                    this.mActivity,
                    "Vibration time",
                    "Set new value",
                    Integer.toString(this.mSettings.loadGestureVibrationTime()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.7
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.saveGestureVibrationTime(
                                Integer.parseInt(str2));
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posSingleTouchGestures) {
            new InputDialog(
                    this.mActivity,
                    "Single touch gestures",
                    new String[] {"Disbaled", "Enabled"},
                    Integer.toString(
                            this.mSettings.loadSingleTouchGestureSupport())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.8
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        int i9 = Integer.parseInt(str2);
                        SettingsViewHelper.this.mSettings.saveSingleTouchGestureSupport(i9);
                        SettingsViewHelper.this.mTouchServiceNative.setSingleTouchGestureSupport(
                                i9);
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posMinScore) {
            new InputDialog(
                    this.mActivity,
                    "Min gesture score",
                    "Set new value",
                    Integer.toString(this.mSettings.loadMinScore()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.9
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        int i9 = Integer.parseInt(str2);
                        SettingsViewHelper.this.mSettings.saveMinScore(i9);
                        SettingsViewHelper.this.mTouchServiceNative.setMinScore(i9);
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posMinPathLength) {
            new InputDialog(
                    this.mActivity,
                    "Min gesture path length",
                    "Set new value",
                    Integer.toString(this.mSettings.loadMinPathLength()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.10
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        int i9 = Integer.parseInt(str2);
                        SettingsViewHelper.this.mSettings.saveMinPathLength(i9);
                        SettingsViewHelper.this.mTouchServiceNative.setMinPathLength(i9);
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posSingleSwipesBBox) {
            new InputDialog(
                    this.mActivity,
                    "Min bbox size",
                    "Set new value",
                    Integer.toString(this.mSettings.loadSingleSwipesBBox()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.11
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        int i9 = Integer.parseInt(str2);
                        SettingsViewHelper.this.mSettings.saveSingleSwipesBBox(i9);
                        SettingsViewHelper.this.mTouchServiceNative.setSingleSwipesBBox(i9);
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posSingleSwipesAArea) {
            new InputDialog(
                    this.mActivity,
                    "Activation area thickness",
                    "Set new value",
                    Integer.toString(this.mSettings.loadSingleSwipesAArea()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.12
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.saveSingleSwipesAArea(
                                Integer.parseInt(str2));
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posTouchscreenScreenFactorX) {
            new InputDialog(
                    this.mActivity,
                    "Touchscreen to screen factor X",
                    "Set new value",
                    Integer.toString(
                            (int) (this.mSettings.loadTouchscreenScreenFactorX() * 100.0f)),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.13
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.saveTouchscreenScreenFactorX(
                                Integer.parseInt(str2));
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posTouchscreenScreenFactorY) {
            new InputDialog(
                    this.mActivity,
                    "Touchscreen to screen factor Y",
                    "Set new value",
                    Integer.toString(
                            (int) (this.mSettings.loadTouchscreenScreenFactorY() * 100.0f)),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.14
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.saveTouchscreenScreenFactorY(
                                Integer.parseInt(str2));
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPiePos) {
            new InputDialog(
                    this.mActivity,
                    "Activation area position",
                    new String[] {
                        "Right",
                        "Left",
                        "Bottom",
                        "Bottom\n(not centered)",
                        "Left and right",
                        "Left and bottom",
                        "Right and bottom",
                        "Left, right and bottom",
                        "Left, right and bottom\n(not centered)"
                    },
                    Integer.toString(this.mSettings.loadPiePos())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.15
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePiePos(Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.setVisiblePieActivationAread(true);
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    } catch (NumberFormatException unused2) {
                    }
                    return true;
                }
            }.show();
            return;
        }
        if (i == this.posPieAreaX) {
            new InputDialog(
                    this.mActivity,
                    "Activation area thickness",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieAreaX()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.16
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieAreaX(Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.setVisiblePieActivationAread(true);
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    } catch (NumberFormatException unused2) {
                    }
                    return true;
                }
            }.show();
            return;
        }
        if (i == this.posPieAreaY) {
            new InputDialog(
                    this.mActivity,
                    "Activation area length",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieAreaY()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.17
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieAreaY(Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.setVisiblePieActivationAread(true);
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    } catch (NumberFormatException unused2) {
                    }
                    return true;
                }
            }.show();
            return;
        }
        if (i == this.posPieAreaGravity) {
            new InputDialog(
                    this.mActivity,
                    "Activation area gravity",
                    new String[] {"Center", "Top", "Bottom"},
                    Integer.toString(this.mSettings.loadPieAreaGravity())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.18
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieAreaGravity(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.setVisiblePieActivationAread(true);
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    } catch (NumberFormatException unused2) {
                    }
                    return true;
                }
            }.show();
            return;
        }
        if (i == this.posPieAreaBehindKeyboard) {
            new InputDialog(
                    this.mActivity,
                    "Set pie area behind keyboard",
                    new String[] {"Disabled", "Enabled"},
                    Integer.toString(this.mSettings.loadPieAreaBehindKeyboard())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.19
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieAreaBehindKeyboard(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieOnLockScreen) {
            new InputDialog(
                    this.mActivity,
                    "Set pie on lockscreen",
                    new String[] {"Disabled", "Enabled"},
                    Integer.toString(this.mSettings.loadPieOnLockScreen())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.20
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieOnLockScreen(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieColor) {
            new ColorDialog(
                    this.mActivity,
                    "Pie colors",
                    new String[] {
                        "normal color",
                        "selected color",
                        "icon color",
                        "outline color",
                        "gradient color",
                        "gradient outline color",
                        "shift color"
                    },
                    this.mSettings
                            .loadPieColor()) { // from class: com.noname81.lmt.SettingsViewHelper.21
                @Override // com.noname81.lmt.ColorDialog
                public boolean onOkClicked(String str2) {
                    SettingsViewHelper.this.mSettings.savePieColor(str2);
                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    return true;
                }
            }.show();
            return;
        }
        if (i == this.posPieStatusInfoColor) {
            new ColorDialog(
                    this.mActivity,
                    "Pie status info colors",
                    new String[] {"clock color", "notification color", "dim color"},
                    this.mSettings.loadPieStatusInfoColor()) { // from class:
                // com.noname81.lmt.SettingsViewHelper.22
                @Override // com.noname81.lmt.ColorDialog
                public boolean onOkClicked(String str2) {
                    SettingsViewHelper.this.mSettings.savePieStatusInfoColor(str2);
                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    return true;
                }
            }.show();
            return;
        }
        if (i == this.posPiePointerColor) {
            new ColorDialog(
                    this.mActivity,
                    "Pie pointer colors",
                    new String[] {"pointer color"},
                    this.mSettings.loadPiePointerColor()) { // from class:
                // com.noname81.lmt.SettingsViewHelper.23
                @Override // com.noname81.lmt.ColorDialog
                public boolean onOkClicked(String str2) {
                    SettingsViewHelper.this.mSettings.savePiePointerColor(str2);
                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    return true;
                }
            }.show();
            return;
        }
        if (i == this.posPieStatusInfoFont) {
            new InputDialog(
                    this.mActivity,
                    "Pie font",
                    new String[] {
                        "Roboto Thin (small)",
                        "Roboto Thin (normal)",
                        "Roboto Thin (big)",
                        "Roboto Light (small)",
                        "Roboto Light (normal)",
                        "Roboto Light (big)",
                        "System font (small)",
                        "System font (normal)",
                        "System font (big)"
                    },
                    Integer.toString(this.mSettings.loadPieFont())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.24
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieFont(Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieInnerRadius) {
            new InputDialog(
                    this.mActivity,
                    "Pie inner radius",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieInnerRadius()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.25
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieInnerRadius(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieOuterRadius) {
            new InputDialog(
                    this.mActivity,
                    "Pie outer radius",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieOuterRadius()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.26
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieOuterRadius(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieShiftSize) {
            new InputDialog(
                    this.mActivity,
                    "Pie shift size",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieShiftSize()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.27
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieShiftSize(Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieOutlineSize) {
            new InputDialog(
                    this.mActivity,
                    "Pie outline size",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieOutlineSize()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.28
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieOutlineSize(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieSliceGap) {
            new InputDialog(
                    this.mActivity,
                    "Pie slice gap",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieSliceGap()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.29
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieSliceGap(Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieStartGap) {
            new InputDialog(
                    this.mActivity,
                    "Pie start gap",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieStartGap()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.30
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieStartGap(Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieLongpress) {
            new InputDialog(
                    this.mActivity,
                    "Longpress time",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieLongpress()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.31
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieLongpress(Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieAnimation) {
            new InputDialog(
                    this.mActivity,
                    "Animation time",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieAnimation()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.32
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieAnimation(Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieVibrate) {
            new InputDialog(
                    this.mActivity,
                    "Feedback type",
                    new String[] {
                        "Disabled",
                        "Only longpress",
                        "Shortpress and longpress",
                        "Longpress and fire",
                        "Shortpress, longpress and fire"
                    },
                    Integer.toString(this.mSettings.loadPieVibrate())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.33
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieVibrate(Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieVibrationTime) {
            new InputDialog(
                    this.mActivity,
                    "Pie vibration time",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieVibrationTime()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.34
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieVibrationTime(
                                Integer.parseInt(str2));
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieMultiCommand) {
            new InputDialog(
                    this.mActivity,
                    "Multi command",
                    new String[] {"Disabled", "Enabled"},
                    Integer.toString(this.mSettings.loadPieMultiCommand())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.35
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieMultiCommand(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieExpandTriggerArea) {
            new InputDialog(
                    this.mActivity,
                    "Expand trigger area",
                    new String[] {"Disabled", "Enabled"},
                    Integer.toString(this.mSettings.loadPieExpandTriggerArea())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.36
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieExpandTriggerArea(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPiePointerFromEdges) {
            new InputDialog(
                    this.mActivity,
                    "Show pointer",
                    new String[] {"Disabled", "Enabled"},
                    Integer.toString(this.mSettings.loadPiePointerFromEdges())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.37
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePiePointerFromEdges(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPiePointerWarpFactor) {
            new InputDialog(
                    this.mActivity,
                    "Set pie pointer warp factor",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPiePointerWarpFactor()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.38
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePiePointerWarpFactor(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieShowStatusInfos) {
            new InputDialog(
                    this.mActivity,
                    "Show status infos",
                    new String[] {
                        "Disabled",
                        "Enabled along pie",
                        "Enabled horizontal",
                        "Enabled with sensor control"
                    },
                    Integer.toString(this.mSettings.loadPieShowStatusInfos())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.39
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieShowStatusInfos(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieRotateImages) {
            new InputDialog(
                    this.mActivity,
                    "Set rotate Images",
                    new String[] {"Disabled", "Enabled"},
                    Integer.toString(this.mSettings.loadPieRotateImages())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.40
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieRotateImages(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieNavButtonStyle) {
            new InputDialog(
                    this.mActivity,
                    "Set nav button style",
                    new String[] {"Android Lollipop", "Android KitKat", "Google Pixel"},
                    Integer.toString(this.mSettings.loadPieNavButtonStyle())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.41
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieNavButtonsStyle(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (i == this.posPieShowScaleAppImages) {
            new InputDialog(
                    this.mActivity,
                    "Show and scale app images",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieShowScaleAppImages()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.42
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieShowScaleAppImages(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
        } else if (i == this.posPieShowScaleUserImages) {
            new InputDialog(
                    this.mActivity,
                    "Show and scale user images",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieShowScaleUserImages()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.43
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieShowScaleUserImages(
                                Integer.parseInt(str2));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
        } else if (i == this.posPieUserImageSearchPath) {
            new InputDialog(
                    this.mActivity,
                    "User image search path",
                    "Set new value",
                    this.mSettings.loadResourceSearchPath(),
                    false) { // from class: com.noname81.lmt.SettingsViewHelper.44
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str2) {
                    SettingsViewHelper.this.mSettings.saveResourceSearchPath(str2);
                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    return true;
                }
            }.show();
        }
    }

    public class SettingsSimpleAdapter extends SimpleAdapter {
        static final int listitem_description_button = 2131034368;
        private Context mContext;
        private int mOffset;
        private boolean mSimpleUI;

        SettingsSimpleAdapter(
                Context context, List<? extends Map<String, ?>> list, int i, boolean z) {
            super(
                    context,
                    list,
                    R.layout.listitem_description,
                    new String[] {"title", "caption"},
                    new int[] {R.id.listitem_description_text, R.id.listitem_description_caption});
            this.mContext = context;
            this.mSimpleUI = z;
            this.mOffset = i;
        }

        @Override // android.widget.SimpleAdapter, android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            View view2 = super.getView(i, view, viewGroup);
            LinearLayout linearLayout = (LinearLayout) view2;
            int i2 = i + this.mOffset + 1;
            if (i2 == SettingsViewHelper.this.posActivateTouchService) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.getServiceState() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.1
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                                if (!z
                                        || SettingsViewHelper.this.mSettings.getServiceState()
                                                != 0) {
                                    if (z
                                            || SettingsViewHelper.this.mSettings.getServiceState()
                                                    != 1) {
                                        return;
                                    }
                                    SettingsViewHelper.this.mSettings.stopService();
                                    return;
                                }
                                SettingsViewHelper.this.mSettings.startService();
                            }
                        });
            } else if (i2 == SettingsViewHelper.this.posAutostartTouchService) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadAutostart() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.2
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                                if (!z || SettingsViewHelper.this.mSettings.loadAutostart() != 0) {
                                    if (z
                                            || SettingsViewHelper.this.mSettings.loadAutostart()
                                                    != 1) {
                                        return;
                                    }
                                    SettingsViewHelper.this.mSettings.saveAutostart(0);
                                    return;
                                }
                                SettingsViewHelper.this.mSettings.saveAutostart(1);
                            }
                        });
            } else if (i2 == SettingsViewHelper.this.posSingleTouchGestures) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadSingleTouchGestureSupport() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.3
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                                if (!z
                                        || SettingsViewHelper.this.mSettings
                                                        .loadSingleTouchGestureSupport()
                                                != 0) {
                                    if (z
                                            || SettingsViewHelper.this.mSettings
                                                            .loadSingleTouchGestureSupport()
                                                    != 1) {
                                        return;
                                    }
                                    SettingsViewHelper.this.mSettings.saveSingleTouchGestureSupport(
                                            0);
                                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                                    return;
                                }
                                SettingsViewHelper.this.mSettings.saveSingleTouchGestureSupport(1);
                                SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                            }
                        });
            } else if (i2 == SettingsViewHelper.this.posPieAreaBehindKeyboard) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadPieAreaBehindKeyboard() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.4
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                                if (!z
                                        || SettingsViewHelper.this.mSettings
                                                        .loadPieAreaBehindKeyboard()
                                                != 0) {
                                    if (z
                                            || SettingsViewHelper.this.mSettings
                                                            .loadPieAreaBehindKeyboard()
                                                    != 1) {
                                        return;
                                    }
                                    SettingsViewHelper.this.mSettings.savePieAreaBehindKeyboard(0);
                                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                                    return;
                                }
                                SettingsViewHelper.this.mSettings.savePieAreaBehindKeyboard(1);
                                SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                            }
                        });
            } else if (i2 == SettingsViewHelper.this.posPieOnLockScreen) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadPieOnLockScreen() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.5
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                                if (!z
                                        || SettingsViewHelper.this.mSettings.loadPieOnLockScreen()
                                                != 0) {
                                    if (z
                                            || SettingsViewHelper.this.mSettings
                                                            .loadPieOnLockScreen()
                                                    != 1) {
                                        return;
                                    }
                                    SettingsViewHelper.this.mSettings.savePieOnLockScreen(0);
                                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                                    return;
                                }
                                SettingsViewHelper.this.mSettings.savePieOnLockScreen(1);
                                SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                            }
                        });
            } else if (i2 == SettingsViewHelper.this.posPieRotateImages) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadPieRotateImages() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.6
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                                if (!z
                                        || SettingsViewHelper.this.mSettings.loadPieRotateImages()
                                                != 0) {
                                    if (z
                                            || SettingsViewHelper.this.mSettings
                                                            .loadPieRotateImages()
                                                    != 1) {
                                        return;
                                    }
                                    SettingsViewHelper.this.mSettings.savePieRotateImages(0);
                                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                                    return;
                                }
                                SettingsViewHelper.this.mSettings.savePieRotateImages(1);
                                SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                            }
                        });
            } else if (i2 == SettingsViewHelper.this.posPieExpandTriggerArea) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadPieExpandTriggerArea() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.7
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                                if (!z
                                        || SettingsViewHelper.this.mSettings
                                                        .loadPieExpandTriggerArea()
                                                != 0) {
                                    if (z
                                            || SettingsViewHelper.this.mSettings
                                                            .loadPieExpandTriggerArea()
                                                    != 1) {
                                        return;
                                    }
                                    SettingsViewHelper.this.mSettings.savePieExpandTriggerArea(0);
                                    return;
                                }
                                SettingsViewHelper.this.mSettings.savePieExpandTriggerArea(1);
                                SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                            }
                        });
            } else if (i2 == SettingsViewHelper.this.posPiePointerFromEdges) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadPiePointerFromEdges() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.8
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                                if (!z
                                        || SettingsViewHelper.this.mSettings
                                                        .loadPiePointerFromEdges()
                                                != 0) {
                                    if (z
                                            || SettingsViewHelper.this.mSettings
                                                            .loadPiePointerFromEdges()
                                                    != 1) {
                                        return;
                                    }
                                    SettingsViewHelper.this.mSettings.savePiePointerFromEdges(0);
                                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                                    return;
                                }
                                SettingsViewHelper.this.mSettings.savePiePointerFromEdges(1);
                                SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                            }
                        });
            } else if (i2 == SettingsViewHelper.this.posPieMultiCommand) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadPieMultiCommand() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.9
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                                if (!z
                                        || SettingsViewHelper.this.mSettings.loadPieMultiCommand()
                                                != 0) {
                                    if (z
                                            || SettingsViewHelper.this.mSettings
                                                            .loadPieMultiCommand()
                                                    != 1) {
                                        return;
                                    }
                                    SettingsViewHelper.this.mSettings.savePieMultiCommand(0);
                                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                                    return;
                                }
                                SettingsViewHelper.this.mSettings.savePieMultiCommand(1);
                                SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                            }
                        });
            }
            return view2;
        }

        void addButton(
                Context context,
                LinearLayout linearLayout,
                boolean z,
                CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
            if (linearLayout.findViewById(listitem_description_button) == null) {
                CompoundButton checkBox =
                        this.mSimpleUI ? new CheckBox(context) : new Switch(context);
                checkBox.setId(listitem_description_button);
                checkBox.setPadding(0, 0, 30, 0);
                checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
                checkBox.setChecked(z);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
                layoutParams.gravity = 8388629;
                checkBox.setLayoutParams(layoutParams);
                linearLayout.addView(checkBox);
            }
        }
    }
}
