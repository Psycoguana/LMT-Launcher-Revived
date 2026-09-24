package com.noname81.lmt;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
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
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Handles a tap on any settings row.
 *
 * <p>One large dispatcher keyed by the position indices that {@link SettingsViewFragment} and
 * {@link SettingsViewActivity} assign as they build their lists. Most rows open an {@link
 * InputDialog} or {@link ColorDialog} and write the result back, restarting the service when a
 * setting requires it.
 */
class SettingsViewHelper implements AdapterView.OnItemClickListener {
    // Routed to SettingsViewFragment#onActivityResult, which owns the fragment-scoped
    // startActivityForResult call the Storage Access Framework picker needs.
    static final int REQUEST_EXPORT_SETTINGS = 9001;
    static final int REQUEST_IMPORT_SETTINGS = 9002;

    private Activity mActivity;
    private Fragment mFragment;
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
    int posOffsetBackup = 255;
    int posExportSettings = 255;
    int posImportSettings = 255;

    SettingsViewHelper(Activity activity) {
        this(activity, null);
    }

    // The file-picker flow needs the owning fragment to route its startActivityForResult
    // result back (see canUseFilePicker()); SettingsViewActivity has none to give.
    SettingsViewHelper(Activity activity, Fragment fragment) {
        this.mActivity = activity;
        this.mFragment = fragment;
        this.mSettings = SettingsValues.getInstance(activity);
        this.mRootContext = RootContext.getInstance(activity);
        this.mTouchServiceNative = TouchServiceNative.getInstance(activity);
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == this.posActivateTouchService) {
            if (this.mSettings.getServiceState() == 0) {
                this.mSettings.startService();
                return;
            } else {
                this.mSettings.stopService();
                return;
            }
        }
        if (position == this.posAutostartTouchService) {
            new InputDialog(
                    this.mActivity,
                    "Autostart service",
                    new String[] {"Disabled", "Enabled"},
                    Integer.toString(this.mSettings.loadAutostart())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.1
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.saveAutostart(Integer.parseInt(value));
                        return true;
                    } catch (NumberFormatException unused) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posSetMode) {
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
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.saveTouchServiceMode(
                                Integer.parseInt(value));
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
        if (position == this.posAutoConfiguration) {
            int inputDevice = -1;
            int factorX = -1;
            int factorY = -1;
            try {
                String[] geteventLines =
                        this.mRootContext
                                .runCommandResult(
                                        "getevent -p | grep -E '(device|name|0035|0036)'", true)
                                .split("\n");
                int deviceIndex = -1;
                factorX = -1;
                factorY = -1;
                for (int line = 0; line < geteventLines.length - 1; line++) {
                    if (geteventLines[line].contains("0035")) {
                        int nextLine = line + 1;
                        if (geteventLines[nextLine].contains("0036")) {
                            for (int back = line - 1; back >= 0; back--) {
                                if (geteventLines[back].contains("/dev/input")) {
                                    String deviceLine = geteventLines[back];
                                    deviceIndex =
                                            Integer.parseInt(
                                                    deviceLine.substring(deviceLine.length() - 1));
                                    break;
                                }
                            }
                            WindowManager windowManager =
                                    (WindowManager) this.mActivity.getSystemService("window");
                            DisplayMetrics displayMetrics = new DisplayMetrics();
                            windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
                            int screenWidth = displayMetrics.widthPixels;
                            int screenHeight = displayMetrics.heightPixels;
                            int maxAt = geteventLines[line].indexOf("max");
                            factorX =
                                    Math.round(
                                            (Integer.parseInt(
                                                                    geteventLines[line].substring(
                                                                            maxAt + 4,
                                                                            geteventLines[line]
                                                                                    .indexOf(
                                                                                            ",",
                                                                                            maxAt)))
                                                            / screenWidth)
                                                    * 100.0f);
                            int maxAt2 = geteventLines[nextLine].indexOf("max");
                            factorY =
                                    Math.round(
                                            (Integer.parseInt(
                                                                    geteventLines[nextLine]
                                                                            .substring(
                                                                                    maxAt2 + 4,
                                                                                    geteventLines[
                                                                                            nextLine]
                                                                                            .indexOf(
                                                                                                    ",",
                                                                                                    maxAt2)))
                                                            / screenHeight)
                                                    * 100.0f);
                        }
                    }
                }
                inputDevice = deviceIndex;
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
                                public void onClick(DialogInterface dialogInterface, int parsed) {
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
                                public void onClick(DialogInterface dialogInterface, int parsed) {
                                    dialogInterface.dismiss();
                                }
                            })
                    .create()
                    .show();
            return;
        }
        if (position == this.posSetInput) {
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
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.saveInputDevice(Integer.parseInt(value));
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
        if (position == this.posAddBlacklist) {
            MultiSelectActivity.AppSelectMode = 3;
            this.mActivity.startActivity(
                    new Intent(this.mActivity, (Class<?>) MultiSelectActivity.class));
            return;
        }
        if (position == this.posClearBlacklist) {
            this.mSettings.clearBlacklisted();
            return;
        }
        if (position == this.posAddBlacklistPie) {
            MultiSelectActivity.AppSelectMode = 4;
            this.mActivity.startActivity(
                    new Intent(this.mActivity, (Class<?>) MultiSelectActivity.class));
            return;
        }
        if (position == this.posClearBlacklistPie) {
            this.mSettings.clearBlacklistedPie();
            return;
        }
        if (position == this.posFeedbackStyle) {
            new InputDialog(
                    this.mActivity,
                    "Feedback style",
                    new String[] {
                        "No feedback", "Debug overlay", "Image overlay", "Tactile feedback"
                    },
                    Integer.toString(this.mSettings.loadFeedbackMode())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.6
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        Toaster.getInstance(this.mContext).setMode(Integer.parseInt(value));
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posVibrationTime) {
            new InputDialog(
                    this.mActivity,
                    "Vibration time",
                    "Set new value",
                    Integer.toString(this.mSettings.loadGestureVibrationTime()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.7
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.saveGestureVibrationTime(
                                Integer.parseInt(value));
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posSingleTouchGestures) {
            new InputDialog(
                    this.mActivity,
                    "Single touch gestures",
                    new String[] {"Disbaled", "Enabled"},
                    Integer.toString(
                            this.mSettings.loadSingleTouchGestureSupport())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.8
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        int parsed = Integer.parseInt(value);
                        SettingsViewHelper.this.mSettings.saveSingleTouchGestureSupport(parsed);
                        SettingsViewHelper.this.mTouchServiceNative.setSingleTouchGestureSupport(
                                parsed);
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posMinScore) {
            new InputDialog(
                    this.mActivity,
                    "Min gesture score",
                    "Set new value",
                    Integer.toString(this.mSettings.loadMinScore()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.9
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        int parsed = Integer.parseInt(value);
                        SettingsViewHelper.this.mSettings.saveMinScore(parsed);
                        SettingsViewHelper.this.mTouchServiceNative.setMinScore(parsed);
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posMinPathLength) {
            new InputDialog(
                    this.mActivity,
                    "Min gesture path length",
                    "Set new value",
                    Integer.toString(this.mSettings.loadMinPathLength()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.10
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        int parsed = Integer.parseInt(value);
                        SettingsViewHelper.this.mSettings.saveMinPathLength(parsed);
                        SettingsViewHelper.this.mTouchServiceNative.setMinPathLength(parsed);
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posSingleSwipesBBox) {
            new InputDialog(
                    this.mActivity,
                    "Min bbox size",
                    "Set new value",
                    Integer.toString(this.mSettings.loadSingleSwipesBBox()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.11
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        int parsed = Integer.parseInt(value);
                        SettingsViewHelper.this.mSettings.saveSingleSwipesBBox(parsed);
                        SettingsViewHelper.this.mTouchServiceNative.setSingleSwipesBBox(parsed);
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posSingleSwipesAArea) {
            new InputDialog(
                    this.mActivity,
                    "Activation area thickness",
                    "Set new value",
                    Integer.toString(this.mSettings.loadSingleSwipesAArea()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.12
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.saveSingleSwipesAArea(
                                Integer.parseInt(value));
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posTouchscreenScreenFactorX) {
            new InputDialog(
                    this.mActivity,
                    "Touchscreen to screen factor X",
                    "Set new value",
                    Integer.toString(
                            (int) (this.mSettings.loadTouchscreenScreenFactorX() * 100.0f)),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.13
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.saveTouchscreenScreenFactorX(
                                Integer.parseInt(value));
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posTouchscreenScreenFactorY) {
            new InputDialog(
                    this.mActivity,
                    "Touchscreen to screen factor Y",
                    "Set new value",
                    Integer.toString(
                            (int) (this.mSettings.loadTouchscreenScreenFactorY() * 100.0f)),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.14
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.saveTouchscreenScreenFactorY(
                                Integer.parseInt(value));
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPiePos) {
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
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePiePos(Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.setVisiblePieActivationAread(true);
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    } catch (NumberFormatException unused2) {
                    }
                    return true;
                }
            }.show();
            return;
        }
        if (position == this.posPieAreaX) {
            new InputDialog(
                    this.mActivity,
                    "Activation area thickness",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieAreaX()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.16
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieAreaX(Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.setVisiblePieActivationAread(true);
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    } catch (NumberFormatException unused2) {
                    }
                    return true;
                }
            }.show();
            return;
        }
        if (position == this.posPieAreaY) {
            new InputDialog(
                    this.mActivity,
                    "Activation area length",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieAreaY()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.17
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieAreaY(Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.setVisiblePieActivationAread(true);
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    } catch (NumberFormatException unused2) {
                    }
                    return true;
                }
            }.show();
            return;
        }
        if (position == this.posPieAreaGravity) {
            new InputDialog(
                    this.mActivity,
                    "Activation area gravity",
                    new String[] {"Center", "Top", "Bottom"},
                    Integer.toString(this.mSettings.loadPieAreaGravity())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.18
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieAreaGravity(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.setVisiblePieActivationAread(true);
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    } catch (NumberFormatException unused2) {
                    }
                    return true;
                }
            }.show();
            return;
        }
        if (position == this.posPieAreaBehindKeyboard) {
            new InputDialog(
                    this.mActivity,
                    "Set pie area behind keyboard",
                    new String[] {"Disabled", "Enabled"},
                    Integer.toString(this.mSettings.loadPieAreaBehindKeyboard())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.19
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieAreaBehindKeyboard(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieOnLockScreen) {
            new InputDialog(
                    this.mActivity,
                    "Set pie on lockscreen",
                    new String[] {"Disabled", "Enabled"},
                    Integer.toString(this.mSettings.loadPieOnLockScreen())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.20
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieOnLockScreen(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieColor) {
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
                public boolean onOkClicked(String value) {
                    SettingsViewHelper.this.mSettings.savePieColor(value);
                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    return true;
                }
            }.show();
            return;
        }
        if (position == this.posPieStatusInfoColor) {
            new ColorDialog(
                    this.mActivity,
                    "Pie status info colors",
                    new String[] {"clock color", "notification color", "dim color"},
                    this.mSettings.loadPieStatusInfoColor()) { // from class:
                // com.noname81.lmt.SettingsViewHelper.22
                @Override // com.noname81.lmt.ColorDialog
                public boolean onOkClicked(String value) {
                    SettingsViewHelper.this.mSettings.savePieStatusInfoColor(value);
                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    return true;
                }
            }.show();
            return;
        }
        if (position == this.posPiePointerColor) {
            new ColorDialog(
                    this.mActivity,
                    "Pie pointer colors",
                    new String[] {"pointer color"},
                    this.mSettings.loadPiePointerColor()) { // from class:
                // com.noname81.lmt.SettingsViewHelper.23
                @Override // com.noname81.lmt.ColorDialog
                public boolean onOkClicked(String value) {
                    SettingsViewHelper.this.mSettings.savePiePointerColor(value);
                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    return true;
                }
            }.show();
            return;
        }
        if (position == this.posPieStatusInfoFont) {
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
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieFont(Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieInnerRadius) {
            new InputDialog(
                    this.mActivity,
                    "Pie inner radius",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieInnerRadius()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.25
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieInnerRadius(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieOuterRadius) {
            new InputDialog(
                    this.mActivity,
                    "Pie outer radius",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieOuterRadius()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.26
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieOuterRadius(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieShiftSize) {
            new InputDialog(
                    this.mActivity,
                    "Pie shift size",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieShiftSize()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.27
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieShiftSize(Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieOutlineSize) {
            new InputDialog(
                    this.mActivity,
                    "Pie outline size",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieOutlineSize()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.28
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieOutlineSize(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieSliceGap) {
            new InputDialog(
                    this.mActivity,
                    "Pie slice gap",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieSliceGap()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.29
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieSliceGap(Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieStartGap) {
            new InputDialog(
                    this.mActivity,
                    "Pie start gap",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieStartGap()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.30
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieStartGap(Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieLongpress) {
            new InputDialog(
                    this.mActivity,
                    "Longpress time",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieLongpress()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.31
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieLongpress(Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieAnimation) {
            new InputDialog(
                    this.mActivity,
                    "Animation time",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieAnimation()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.32
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieAnimation(Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieVibrate) {
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
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieVibrate(Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieVibrationTime) {
            new InputDialog(
                    this.mActivity,
                    "Pie vibration time",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieVibrationTime()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.34
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieVibrationTime(
                                Integer.parseInt(value));
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieMultiCommand) {
            new InputDialog(
                    this.mActivity,
                    "Multi command",
                    new String[] {"Disabled", "Enabled"},
                    Integer.toString(this.mSettings.loadPieMultiCommand())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.35
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieMultiCommand(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieExpandTriggerArea) {
            new InputDialog(
                    this.mActivity,
                    "Expand trigger area",
                    new String[] {"Disabled", "Enabled"},
                    Integer.toString(this.mSettings.loadPieExpandTriggerArea())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.36
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieExpandTriggerArea(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPiePointerFromEdges) {
            new InputDialog(
                    this.mActivity,
                    "Show pointer",
                    new String[] {"Disabled", "Enabled"},
                    Integer.toString(this.mSettings.loadPiePointerFromEdges())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.37
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePiePointerFromEdges(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPiePointerWarpFactor) {
            new InputDialog(
                    this.mActivity,
                    "Set pie pointer warp factor",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPiePointerWarpFactor()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.38
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePiePointerWarpFactor(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieShowStatusInfos) {
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
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieShowStatusInfos(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieRotateImages) {
            new InputDialog(
                    this.mActivity,
                    "Set rotate Images",
                    new String[] {"Disabled", "Enabled"},
                    Integer.toString(this.mSettings.loadPieRotateImages())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.40
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieRotateImages(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieNavButtonStyle) {
            new InputDialog(
                    this.mActivity,
                    "Set nav button style",
                    new String[] {"Android Lollipop", "Android KitKat", "Google Pixel"},
                    Integer.toString(this.mSettings.loadPieNavButtonStyle())) { // from class:
                // com.noname81.lmt.SettingsViewHelper.41
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieNavButtonsStyle(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
            return;
        }
        if (position == this.posPieShowScaleAppImages) {
            new InputDialog(
                    this.mActivity,
                    "Show and scale app images",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieShowScaleAppImages()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.42
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieShowScaleAppImages(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
        } else if (position == this.posPieShowScaleUserImages) {
            new InputDialog(
                    this.mActivity,
                    "Show and scale user images",
                    "Set new value",
                    Integer.toString(this.mSettings.loadPieShowScaleUserImages()),
                    true) { // from class: com.noname81.lmt.SettingsViewHelper.43
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    try {
                        SettingsViewHelper.this.mSettings.savePieShowScaleUserImages(
                                Integer.parseInt(value));
                        SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                        return true;
                    } catch (NumberFormatException unused2) {
                        return true;
                    }
                }
            }.show();
        } else if (position == this.posPieUserImageSearchPath) {
            new InputDialog(
                    this.mActivity,
                    "User image search path",
                    "Set new value",
                    this.mSettings.loadResourceSearchPath(),
                    false) { // from class: com.noname81.lmt.SettingsViewHelper.44
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String value) {
                    SettingsViewHelper.this.mSettings.saveResourceSearchPath(value);
                    SettingsViewHelper.this.mSettings.restartServiceIfRequired();
                    return true;
                }
            }.show();
        } else if (position == this.posExportSettings) {
            if (canUseFilePicker()) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                intent.putExtra(Intent.EXTRA_TITLE, "lmt_backup.zip");
                this.mFragment.startActivityForResult(intent, REQUEST_EXPORT_SETTINGS);
            } else {
                new InputDialog(
                        this.mActivity,
                        "Export settings",
                        "Zip file to write",
                        this.mSettings.loadResourceSearchPath() + "lmt_backup.zip",
                        false) {
                    @Override // com.noname81.lmt.InputDialog
                    public boolean onOkClicked(String value) {
                        try {
                            exportSettings(
                                    new FileOutputStream(new File(value)),
                                    SettingsViewHelper.this.mSettings.loadResourceSearchPath()
                                            + value);
                        } catch (Exception e) {
                            toastFailure("Export failed: ", e);
                        }
                        return true;
                    }
                }.show();
            }
        } else if (position == this.posImportSettings) {
            if (canUseFilePicker()) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                this.mFragment.startActivityForResult(intent, REQUEST_IMPORT_SETTINGS);
            } else {
                new InputDialog(
                        this.mActivity,
                        "Import settings",
                        "Zip file to read",
                        this.mSettings.loadResourceSearchPath() + "lmt_backup.zip",
                        false) {
                    @Override // com.noname81.lmt.InputDialog
                    public boolean onOkClicked(String value) {
                        try {
                            importSettings(new FileInputStream(new File(value)), value);
                        } catch (Exception e) {
                            toastFailure("Import failed: ", e);
                        }
                        return true;
                    }
                }.show();
            }
        }
    }

    // The file picker (Storage Access Framework) needs API 19 and a fragment to route the
    // result back to - both true for every live caller (SettingsViewFragment), but
    // SettingsViewActivity's dead code path only ever supplies an Activity.
    private boolean canUseFilePicker() {
        return Build.VERSION.SDK_INT >= 19 && this.mFragment != null;
    }

    /** Called by {@link SettingsViewFragment#onActivityResult} once the user picked a zip to write. */
    void handleExportSettingsPicked(Uri uri) {
        try {
            exportSettings(this.mActivity.getContentResolver().openOutputStream(uri), "" + uri);
        } catch (Exception e) {
            toastFailure("Export failed: ", e);
        }
    }

    /** Called by {@link SettingsViewFragment#onActivityResult} once the user picked a zip to read. */
    void handleImportSettingsPicked(Uri uri) {
        try {
            importSettings(this.mActivity.getContentResolver().openInputStream(uri), "" + uri);
        } catch (Exception e) {
            toastFailure("Import failed: ", e);
        }
    }

    private void exportSettings(OutputStream destStream, String label) throws Exception {
        SettingsBackup.exportZip(
                this.mSettings, this.mSettings.loadResourceSearchPath(), destStream);
        Toast.makeText(this.mActivity, "Exported settings to " + label, Toast.LENGTH_LONG).show();
    }

    private void importSettings(InputStream srcStream, String label) throws Exception {
        boolean restored =
                SettingsBackup.importZip(
                        this.mSettings, this.mSettings.loadResourceSearchPath(), srcStream);
        if (restored) {
            this.mSettings.restartServiceIfRequired();
            Toast.makeText(this.mActivity, "Imported settings from " + label, Toast.LENGTH_LONG)
                    .show();
        } else {
            Toast.makeText(this.mActivity, "Not a valid LMT backup: " + label, Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void toastFailure(String prefix, Exception e) {
        Toast.makeText(this.mActivity, prefix + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    public class SettingsSimpleAdapter extends SimpleAdapter {
        static final int listitem_description_button = 2131034368;
        private Context mContext;
        private int mOffset;
        private boolean mSimpleUI;

        SettingsSimpleAdapter(
                Context context,
                List<? extends Map<String, ?>> items,
                int resource,
                boolean showSwitches) {
            super(
                    context,
                    items,
                    R.layout.listitem_description,
                    new String[] {"title", "caption"},
                    new int[] {R.id.listitem_description_text, R.id.listitem_description_caption});
            this.mContext = context;
            this.mSimpleUI = showSwitches;
            this.mOffset = resource;
        }

        @Override // android.widget.SimpleAdapter, android.widget.Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            View view2 = super.getView(position, convertView, parent);
            LinearLayout linearLayout = (LinearLayout) view2;
            int settingIndex = position + this.mOffset + 1;
            if (settingIndex == SettingsViewHelper.this.posActivateTouchService) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.getServiceState() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.1
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton button, boolean checked) {
                                if (!checked
                                        || SettingsViewHelper.this.mSettings.getServiceState()
                                                != 0) {
                                    if (checked
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
            } else if (settingIndex == SettingsViewHelper.this.posAutostartTouchService) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadAutostart() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.2
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton button, boolean checked) {
                                if (!checked
                                        || SettingsViewHelper.this.mSettings.loadAutostart() != 0) {
                                    if (checked
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
            } else if (settingIndex == SettingsViewHelper.this.posSingleTouchGestures) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadSingleTouchGestureSupport() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.3
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton button, boolean checked) {
                                if (!checked
                                        || SettingsViewHelper.this.mSettings
                                                        .loadSingleTouchGestureSupport()
                                                != 0) {
                                    if (checked
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
            } else if (settingIndex == SettingsViewHelper.this.posPieAreaBehindKeyboard) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadPieAreaBehindKeyboard() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.4
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton button, boolean checked) {
                                if (!checked
                                        || SettingsViewHelper.this.mSettings
                                                        .loadPieAreaBehindKeyboard()
                                                != 0) {
                                    if (checked
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
            } else if (settingIndex == SettingsViewHelper.this.posPieOnLockScreen) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadPieOnLockScreen() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.5
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton button, boolean checked) {
                                if (!checked
                                        || SettingsViewHelper.this.mSettings.loadPieOnLockScreen()
                                                != 0) {
                                    if (checked
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
            } else if (settingIndex == SettingsViewHelper.this.posPieRotateImages) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadPieRotateImages() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.6
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton button, boolean checked) {
                                if (!checked
                                        || SettingsViewHelper.this.mSettings.loadPieRotateImages()
                                                != 0) {
                                    if (checked
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
            } else if (settingIndex == SettingsViewHelper.this.posPieExpandTriggerArea) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadPieExpandTriggerArea() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.7
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton button, boolean checked) {
                                if (!checked
                                        || SettingsViewHelper.this.mSettings
                                                        .loadPieExpandTriggerArea()
                                                != 0) {
                                    if (checked
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
            } else if (settingIndex == SettingsViewHelper.this.posPiePointerFromEdges) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadPiePointerFromEdges() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.8
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton button, boolean checked) {
                                if (!checked
                                        || SettingsViewHelper.this.mSettings
                                                        .loadPiePointerFromEdges()
                                                != 0) {
                                    if (checked
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
            } else if (settingIndex == SettingsViewHelper.this.posPieMultiCommand) {
                addButton(
                        this.mContext,
                        linearLayout,
                        SettingsViewHelper.this.mSettings.loadPieMultiCommand() > 0,
                        new CompoundButton.OnCheckedChangeListener() { // from class:
                            // com.noname81.lmt.SettingsViewHelper.SettingsSimpleAdapter.9
                            @Override // android.widget.CompoundButton.OnCheckedChangeListener
                            public void onCheckedChanged(CompoundButton button, boolean checked) {
                                if (!checked
                                        || SettingsViewHelper.this.mSettings.loadPieMultiCommand()
                                                != 0) {
                                    if (checked
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
                LinearLayout row,
                boolean checked,
                CompoundButton.OnCheckedChangeListener listener) {
            if (row.findViewById(listitem_description_button) == null) {
                CompoundButton checkBox =
                        this.mSimpleUI ? new CheckBox(context) : new Switch(context);
                checkBox.setId(listitem_description_button);
                checkBox.setPadding(0, 0, 30, 0);
                checkBox.setOnCheckedChangeListener(listener);
                checkBox.setChecked(checked);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
                layoutParams.gravity = 8388629;
                checkBox.setLayoutParams(layoutParams);
                row.addView(checkBox);
            }
        }
    }
}
