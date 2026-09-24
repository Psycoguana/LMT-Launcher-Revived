package com.noname81.lmt;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * The main settings tab.
 *
 * <p>Every row is a {@link Preference} identified by its actual preference key (or, for pure
 * action rows, none at all) - not by its position in a list, which is how the ~1572-line {@code
 * SettingsViewHelper}/position-dispatch predecessor of this class worked (ROADMAP 1.6 phase 2).
 *
 * <p>Only the 8 genuinely boolean-backed settings ({@link SettingsPreferenceDataStore}) are real
 * {@link SwitchPreferenceCompat} rows backed by a {@code PreferenceDataStore}. Everything else is
 * a non-persistent {@link Preference} whose click listener opens the exact same {@link
 * InputDialog}/{@link ColorDialog} the old dispatcher used and writes through {@link
 * SettingsValues} directly. Both dialog classes build on {@code MaterialAlertDialogBuilder};
 * {@code SettingsScreenUiTest} still sees them through {@code ShadowDialog.getLatestDialog()},
 * which tracks any shown {@code Dialog}, and reads their fields from the real view tree.
 */
public class SettingsViewFragment extends PreferenceFragmentCompat {
    private static final int REQUEST_EXPORT_SETTINGS = 9001;
    private static final int REQUEST_IMPORT_SETTINGS = 9002;

    private SettingsValues mSettings;
    private RootContext mRootContext;
    private TouchServiceNative mTouchServiceNative;
    private SwitchPreferenceCompat mActivateLmtPreference;

    private interface IntGetter {
        int get();
    }

    private interface IntCallback {
        void run(int value);
    }

    private interface StringGetter {
        String get();
    }

    private interface StringCallback {
        void run(String value);
    }

    @Override // androidx.preference.PreferenceFragmentCompat
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = requireContext();
        this.mSettings = SettingsValues.getInstance(context);
        this.mRootContext = RootContext.getInstance(context);
        this.mTouchServiceNative = TouchServiceNative.getInstance(context);
        getPreferenceManager()
                .setPreferenceDataStore(
                        new SettingsPreferenceDataStore(this.mSettings, this.mTouchServiceNative));

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        setPreferenceScreen(screen);

        int mode = this.mSettings.loadTouchServiceMode();
        buildGeneralSection(addCategory(screen, "General"));
        if (mode < 2) {
            buildGestureRecognitionSection(addCategory(screen, "Gesture recognition"));
            buildGesturesSection(addCategory(screen, "Gestures"));
            buildIsasSection(addCategory(screen, "Invisible swipe areas"));
        }
        if (mode > 0) {
            buildPieActivationSection(addCategory(screen, "Pie activation"));
            buildPieStyleSection(addCategory(screen, "Pie style"));
            buildPieBehaviorSection(addCategory(screen, "Pie behavior"));
            buildPieIconsSection(addCategory(screen, "Pie icons"));
            buildPieExtensionsSection(addCategory(screen, "Pie extensions"));
        }
        buildBackupSection(addCategory(screen, "Backup"));
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        if (this.mActivateLmtPreference != null) {
            this.mActivateLmtPreference.setChecked(this.mSettings.getServiceState() > 0);
        }
    }

    private void buildGeneralSection(PreferenceCategory general) {
        SwitchPreferenceCompat activate = new SwitchPreferenceCompat(requireContext());
        activate.setTitle("Activate LMT");
        activate.setSummary("Activate or deactivate LMT");
        activate.setPersistent(false);
        activate.setIconSpaceReserved(false);
        activate.setWidgetLayoutResource(R.layout.preference_widget_material_switch);
        activate.setChecked(this.mSettings.getServiceState() > 0);
        activate.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    if ((Boolean) newValue) {
                        this.mSettings.startService();
                    } else {
                        this.mSettings.stopService();
                    }
                    return true;
                });
        general.addPreference(activate);
        this.mActivateLmtPreference = activate;

        addSwitch(
                general,
                "Autostart LMT",
                "Configure if LMT should run at startup",
                "Autostart",
                true);

        addChoiceDialog(
                general,
                "Set active features",
                "Configure LMT's feature set",
                "Define feature set",
                new String[] {
                    "Gestures [Root] and ISAS [Root] ",
                    "Gestures [Root] , ISAS [Root] and Pie",
                    "Pie"
                },
                this.mSettings::loadTouchServiceMode,
                v -> {
                    this.mSettings.saveTouchServiceMode(v);
                    this.mSettings.restartServiceIfRequired();
                    requireActivity().recreate();
                });

        addAction(
                general,
                getString(R.string.appearance_title),
                "Light or dark theme and app colors",
                preference -> {
                    startActivity(new Intent(requireActivity(), AppearanceActivity.class));
                    return true;
                });
    }

    private void buildGestureRecognitionSection(PreferenceCategory category) {
        addAction(
                category,
                "Auto configuration",
                "If auto configuration fails, set the input device and touchscreen to screen"
                        + " factors below manually",
                preference -> {
                    showAutoConfigurationDialog();
                    return true;
                });

        String[] inputDevices = new String[21];
        for (int i = 0; i < inputDevices.length; i++) {
            inputDevices[i] = "Input device " + i;
        }
        addChoiceDialog(
                category,
                "Set gesture input",
                "Set the input device for gesture recognition",
                "Set input device",
                inputDevices,
                this.mSettings::loadInputDevice,
                v -> {
                    this.mSettings.saveInputDevice(v);
                    this.mTouchServiceNative.setInputDeviceUnlock(
                            this.mSettings.loadInputDeviceString());
                    this.mSettings.restartServiceIfRequired();
                });

        addNumericDialog(
                category,
                "Touchscreen to screen factor X",
                "Resolution factor in %, default = 100%",
                "Touchscreen to screen factor X",
                () -> (int) (this.mSettings.loadTouchscreenScreenFactorX() * 100.0f),
                this.mSettings::saveTouchscreenScreenFactorX);

        addNumericDialog(
                category,
                "Touchscreen to screen factor Y",
                "Resolution factor in %, default = 100%",
                "Touchscreen to screen factor Y",
                () -> (int) (this.mSettings.loadTouchscreenScreenFactorY() * 100.0f),
                this.mSettings::saveTouchscreenScreenFactorY);
    }

    private void buildGesturesSection(PreferenceCategory category) {
        addChoiceDialog(
                category,
                "Feedback style",
                "Define the gesture feedback style",
                "Feedback style",
                new String[] {"No feedback", "Debug overlay", "Image overlay", "Tactile feedback"},
                this.mSettings::loadFeedbackMode,
                v -> Toaster.getInstance(requireContext()).setMode(v));

        addNumericDialog(
                category,
                "Vibration Time",
                "Define the vibration time in ms, default = 30",
                "Vibration time",
                this.mSettings::loadGestureVibrationTime,
                this.mSettings::saveGestureVibrationTime);

        addSwitch(
                category,
                "Single touch gestures",
                "Configure if single touch gestures should be recognized, default = enabled",
                "STSupport",
                true);

        addNumericDialog(
                category,
                "Min gesture score",
                "Min score in %, default = 70%",
                "Min gesture score",
                this.mSettings::loadMinScore,
                v -> {
                    this.mSettings.saveMinScore(v);
                    this.mTouchServiceNative.setMinScore(v);
                });

        addNumericDialog(
                category,
                "Min gesture path length",
                "Min number of touch points, default = 7",
                "Min gesture path length",
                this.mSettings::loadMinPathLength,
                v -> {
                    this.mSettings.saveMinPathLength(v);
                    this.mTouchServiceNative.setMinPathLength(v);
                });

        addAction(
                category,
                "Add to gesture blacklist",
                "Exclude an app from gesture recognition",
                preference -> {
                    MultiSelectActivity.AppSelectMode = 3;
                    startActivity(new Intent(requireActivity(), MultiSelectActivity.class));
                    return true;
                });

        addAction(
                category,
                "Clear gesture blacklist",
                "Clear all excluded apps",
                preference -> {
                    this.mSettings.clearBlacklisted();
                    return true;
                });
    }

    private void buildIsasSection(PreferenceCategory category) {
        addNumericDialog(
                category,
                "Min bounding box size",
                "Min bounding box size in pixel, 0 = disabled, 1 = enabled, >1 = enabled and size"
                        + " in pixel, default = 1",
                "Min bbox size",
                this.mSettings::loadSingleSwipesBBox,
                v -> {
                    this.mSettings.saveSingleSwipesBBox(v);
                    this.mTouchServiceNative.setSingleSwipesBBox(v);
                });

        addNumericDialog(
                category,
                "Activation area thickness",
                "Activation area width in pixel, default = 60",
                "Activation area thickness",
                this.mSettings::loadSingleSwipesAArea,
                this.mSettings::saveSingleSwipesAArea);
    }

    private void buildPieActivationSection(PreferenceCategory category) {
        addChoiceDialog(
                category,
                "Activation area position",
                "Activation area position, default = all",
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
                this.mSettings::loadPiePos,
                v -> {
                    this.mSettings.savePiePos(v);
                    this.mSettings.setVisiblePieActivationAread(true);
                    this.mSettings.restartServiceIfRequired();
                });

        addNumericDialog(
                category,
                "Activation area thickness",
                "Activation area thickness in pixel, default = 50",
                "Activation area thickness",
                this.mSettings::loadPieAreaX,
                v -> {
                    this.mSettings.savePieAreaX(v);
                    this.mSettings.setVisiblePieActivationAread(true);
                    this.mSettings.restartServiceIfRequired();
                });

        addNumericDialog(
                category,
                "Activation area length",
                "Activation area length in pixel, default = 600",
                "Activation area length",
                this.mSettings::loadPieAreaY,
                v -> {
                    this.mSettings.savePieAreaY(v);
                    this.mSettings.setVisiblePieActivationAread(true);
                    this.mSettings.restartServiceIfRequired();
                });

        addChoiceDialog(
                category,
                "Activation area gravity",
                "Configure the gravity, default = center",
                "Activation area gravity",
                new String[] {"Center", "Top", "Bottom"},
                this.mSettings::loadPieAreaGravity,
                v -> {
                    this.mSettings.savePieAreaGravity(v);
                    this.mSettings.setVisiblePieActivationAread(true);
                    this.mSettings.restartServiceIfRequired();
                });

        addSwitch(
                category,
                "Activation area behind keyboard",
                "Configure if the activation area should be placed behind the keyboard, default ="
                        + " deactivated",
                "PieBehindKeyboard",
                false);

        if (Build.VERSION.SDK_INT < 26) {
            addSwitch(
                    category,
                    "Activation area on lockscreen",
                    "Configure if pie should be active on lockscreen, default = deactivated",
                    "PieOnLockScreen",
                    false);
        }

        addAction(
                category,
                "Add to pie blacklist",
                "Exclude an app from pie",
                preference -> {
                    MultiSelectActivity.AppSelectMode = 4;
                    startActivity(new Intent(requireActivity(), MultiSelectActivity.class));
                    return true;
                });

        addAction(
                category,
                "Clear pie blacklist",
                "Clear all excluded apps",
                preference -> {
                    this.mSettings.clearBlacklistedPie();
                    return true;
                });
    }

    private void buildPieStyleSection(PreferenceCategory category) {
        addColorDialog(
                category,
                "Pie colors",
                "0 = blue, 1 = red, 2 = grey, 3 = none, 4 = white, 5 = magenta/yellow, 6 ="
                        + " green/outline, 7 = red/outline, default = 0",
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
                this.mSettings::loadPieColor,
                v -> {
                    this.mSettings.savePieColor(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addNumericDialog(
                category,
                "Pie inner radius",
                "Radius in dip, default = 60",
                "Pie inner radius",
                this.mSettings::loadPieInnerRadius,
                v -> {
                    this.mSettings.savePieInnerRadius(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addNumericDialog(
                category,
                "Pie outer radius",
                "Radius in dip, default = 80",
                "Pie outer radius",
                this.mSettings::loadPieOuterRadius,
                v -> {
                    this.mSettings.savePieOuterRadius(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addNumericDialog(
                category,
                "Pie shift size",
                "Size of the pie shift effect in pixel, default = 0",
                "Pie shift size",
                this.mSettings::loadPieShiftSize,
                v -> {
                    this.mSettings.savePieShiftSize(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addNumericDialog(
                category,
                "Pie outline size",
                "Size of the outline in pixel, default = 3",
                "Pie outline size",
                this.mSettings::loadPieOutlineSize,
                v -> {
                    this.mSettings.savePieOutlineSize(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addNumericDialog(
                category,
                "Pie slice gap",
                "Size of the gap between two pie slices, default = 0",
                "Pie slice gap",
                this.mSettings::loadPieSliceGap,
                v -> {
                    this.mSettings.savePieSliceGap(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addNumericDialog(
                category,
                "Pie start gap",
                "Size of the gap between pie and border, default = 0",
                "Pie start gap",
                this.mSettings::loadPieStartGap,
                v -> {
                    this.mSettings.savePieStartGap(v);
                    this.mSettings.restartServiceIfRequired();
                });

        // Read on every pie opening, so neither row needs a service restart.
        addChoiceDialog(
                category,
                "Blur",
                "Blur the screen while the pie is open. Needs Android 12+ and the accessibility"
                        + " service, default = none",
                "Blur",
                new String[] {"None", "Blur background", "Blur behind pie"},
                this.mSettings::loadPieBlur,
                v -> {
                    this.mSettings.savePieBlur(v);
                    if (v != PieBlur.MODE_NONE) {
                        if (!PieBlur.isSupported()) {
                            Toast.makeText(requireContext(), "Blur needs Android 12 or newer", 0)
                                    .show();
                        } else {
                            AccessibilityHandler.isAccessibilityAvailable(requireContext(), true);
                        }
                    }
                });

        addNumericDialog(
                category,
                "Blur amount",
                "Blur radius in pixels, 1 to 150, default = 50",
                "Blur amount",
                this.mSettings::loadPieBlurRadius,
                this.mSettings::savePieBlurRadius);
    }

    private void buildPieBehaviorSection(PreferenceCategory category) {
        addNumericDialog(
                category,
                "Longpress time",
                "Min time for longpress in ms, default = 500",
                "Longpress time",
                this.mSettings::loadPieLongpress,
                v -> {
                    this.mSettings.savePieLongpress(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addNumericDialog(
                category,
                "Animation time",
                "Time for the pie animation in ms, default = 80",
                "Animation time",
                this.mSettings::loadPieAnimation,
                v -> {
                    this.mSettings.savePieAnimation(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addChoiceDialog(
                category,
                "Feedback type",
                "Configure the pie feedback for diffent actions, default = longpress",
                "Feedback type",
                new String[] {
                    "Disabled",
                    "Only longpress",
                    "Shortpress and longpress",
                    "Longpress and fire",
                    "Shortpress, longpress and fire"
                },
                this.mSettings::loadPieVibrate,
                v -> {
                    this.mSettings.savePieVibrate(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addNumericDialog(
                category,
                "Vibration Time",
                "Define the vibration time in ms, default = 30",
                "Pie vibration time",
                this.mSettings::loadPieVibrationTime,
                this.mSettings::savePieVibrationTime);

        addSwitch(
                category,
                "Multi command",
                "Configure if actions should be fired multiple times when swiping on item, default"
                        + " = disabled",
                "PieMultiCommand",
                false);

        addSwitch(
                category,
                "Expand trigger area",
                "Configure if the action trigger area should be expanded to the full screen for"
                        + " the outer pie ring, default = enabled",
                "PieExpandTriggerArea",
                true);
    }

    private void buildPieIconsSection(PreferenceCategory category) {
        addSwitch(category, "Rotate images", "Rotate icons based on pie angle", "PieRotateImages", true);

        addChoiceDialog(
                category,
                "Set nav button style",
                "Switch between different icon sets for the nav buttons",
                "Set nav button style",
                new String[] {"Android Lollipop", "Android KitKat", "Google Pixel"},
                this.mSettings::loadPieNavButtonStyle,
                v -> {
                    this.mSettings.savePieNavButtonsStyle(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addNumericDialog(
                category,
                "Show and scale app images",
                "0 = disabled, 1 = enabled (no filter), 2 = enabled, >2 = image size in pixel,"
                        + " default = enabled",
                "Show and scale app images",
                this.mSettings::loadPieShowScaleAppImages,
                v -> {
                    this.mSettings.savePieShowScaleAppImages(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addNumericDialog(
                category,
                "Show and scale user images",
                "0 = disabled, 1 = enabled (no filter), 2 = enabled, >2 = image size in pixel,"
                        + " default = disabled",
                "Show and scale user images",
                this.mSettings::loadPieShowScaleUserImages,
                v -> {
                    this.mSettings.savePieShowScaleUserImages(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addTextDialog(
                category,
                "Set user image search path",
                "default = /storage/emulated/0/Android/data/com.noname81.lmt/files/ (Leave blank"
                        + " to set default)",
                "User image search path",
                this.mSettings::loadResourceSearchPath,
                v -> {
                    this.mSettings.saveResourceSearchPath(v);
                    this.mSettings.restartServiceIfRequired();
                });
    }

    private void buildPieExtensionsSection(PreferenceCategory category) {
        addSwitch(
                category,
                "Pie pointer edge activation",
                "Configure if the pie pointer feature can be activated from the edges (Also"
                        + " consider the pie pointer action to activate the feature!), default ="
                        + " disabled",
                "PiePointerFromEdges",
                false);

        addNumericDialog(
                category,
                "Pie pointer warp factor",
                "Configure the warp factor for the pie pointer feature,\ndefault = 300%, min ="
                        + " 200%, max = 1000%",
                "Set pie pointer warp factor",
                this.mSettings::loadPiePointerWarpFactor,
                v -> {
                    this.mSettings.savePiePointerWarpFactor(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addColorDialog(
                category,
                "Pie pointer color",
                "0 = blue, default = 0",
                "Pie pointer colors",
                new String[] {"pointer color"},
                this.mSettings::loadPiePointerColor,
                v -> {
                    this.mSettings.savePiePointerColor(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addChoiceDialog(
                category,
                "Show status infos",
                "Configure if status info feature should be shown, default = enabled",
                "Show status infos",
                new String[] {
                    "Disabled",
                    "Enabled along pie",
                    "Enabled horizontal",
                    "Enabled with sensor control"
                },
                this.mSettings::loadPieShowStatusInfos,
                v -> {
                    this.mSettings.savePieShowStatusInfos(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addColorDialog(
                category,
                "Pie status info colors",
                "0 = white, default = 0",
                "Pie status info colors",
                new String[] {"clock color", "notification color", "dim color"},
                this.mSettings::loadPieStatusInfoColor,
                v -> {
                    this.mSettings.savePieStatusInfoColor(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addChoiceDialog(
                category,
                "Pie status info font",
                "Configure the pie font, default = Roboto Light",
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
                this.mSettings::loadPieFont,
                v -> {
                    this.mSettings.savePieFont(v);
                    this.mSettings.restartServiceIfRequired();
                });
    }

    private void buildBackupSection(PreferenceCategory category) {
        addAction(
                category,
                "Export settings",
                "Save every setting and pie icon to a zip file",
                preference -> {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/zip");
                    intent.putExtra(Intent.EXTRA_TITLE, "lmt_backup.zip");
                    startActivityForResult(intent, REQUEST_EXPORT_SETTINGS);
                    return true;
                });

        addAction(
                category,
                "Import settings",
                "Restore every setting and pie icon from a zip file",
                preference -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/zip");
                    startActivityForResult(intent, REQUEST_IMPORT_SETTINGS);
                    return true;
                });
    }

    // Reproduces SettingsViewHelper.onItemClick's posAutoConfiguration branch verbatim: roots a
    // getevent probe to guess the touchscreen input device and its axis ranges, then confirms
    // before writing anything.
    private void showAutoConfigurationDialog() {
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
                                (WindowManager) requireActivity().getSystemService("window");
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
                                                                        geteventLines[line].indexOf(
                                                                                ",", maxAt)))
                                                        / (float) screenWidth)
                                                * 100.0f);
                        int maxAt2 = geteventLines[nextLine].indexOf("max");
                        factorY =
                                Math.round(
                                        (Integer.parseInt(
                                                                geteventLines[nextLine].substring(
                                                                        maxAt2 + 4,
                                                                        geteventLines[nextLine]
                                                                                .indexOf(
                                                                                        ",",
                                                                                        maxAt2)))
                                                        / (float) screenHeight)
                                                * 100.0f);
                    }
                }
            }
            inputDevice = deviceIndex;
        } catch (Exception unused) {
            factorX = -1;
            factorY = -1;
        }
        final int inputDeviceFinal = inputDevice;
        final int factorXFinal = factorX;
        final int factorYFinal = factorY;
        new MaterialAlertDialogBuilder(requireActivity())
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
                        (DialogInterface dialogInterface, int parsed) -> {
                            dialogInterface.dismiss();
                            if (inputDeviceFinal <= -1 || factorXFinal <= -1 || factorYFinal <= -1) {
                                return;
                            }
                            this.mSettings.saveInputDevice(inputDeviceFinal);
                            this.mSettings.saveTouchscreenScreenFactorX(factorXFinal);
                            this.mSettings.saveTouchscreenScreenFactorY(factorYFinal);
                            this.mTouchServiceNative.setInputDeviceUnlock(
                                    this.mSettings.loadInputDeviceString());
                            this.mSettings.restartServiceIfRequired();
                        })
                .setNegativeButton(
                        "cancel",
                        (DialogInterface dialogInterface, int parsed) -> dialogInterface.dismiss())
                .create()
                .show();
    }

    @Override // androidx.fragment.app.Fragment
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        if (requestCode == REQUEST_EXPORT_SETTINGS) {
            handleExportSettingsPicked(data.getData());
        } else if (requestCode == REQUEST_IMPORT_SETTINGS) {
            handleImportSettingsPicked(data.getData());
        }
    }

    private void handleExportSettingsPicked(Uri uri) {
        try {
            exportSettings(requireActivity().getContentResolver().openOutputStream(uri));
        } catch (Exception e) {
            toastFailure("Export failed: ", e);
        }
    }

    private void handleImportSettingsPicked(Uri uri) {
        try {
            importSettings(requireActivity().getContentResolver().openInputStream(uri));
        } catch (Exception e) {
            toastFailure("Import failed: ", e);
        }
    }

    private void exportSettings(OutputStream destStream) throws Exception {
        SettingsBackup.exportZip(this.mSettings, this.mSettings.loadResourceSearchPath(), destStream);
        Toast.makeText(requireActivity(), "Settings exported", Toast.LENGTH_SHORT).show();
    }

    private void importSettings(InputStream srcStream) throws Exception {
        boolean restored =
                SettingsBackup.importZip(
                        this.mSettings, this.mSettings.loadResourceSearchPath(), srcStream);
        if (restored) {
            this.mSettings.restartServiceIfRequired();
            Toast.makeText(requireActivity(), "Settings imported", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireActivity(), "Not a valid LMT backup", Toast.LENGTH_LONG).show();
        }
    }

    private void toastFailure(String prefix, Exception e) {
        Toast.makeText(requireActivity(), prefix + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    private PreferenceCategory addCategory(PreferenceScreen screen, String title) {
        PreferenceCategory category = new PreferenceCategory(requireContext());
        category.setTitle(title);
        // No row on this screen ever has an icon; the default layout still reserves a gutter
        // for one, which shows up as a large empty left margin on every row and header.
        category.setIconSpaceReserved(false);
        screen.addPreference(category);
        return category;
    }

    private void addSwitch(
            PreferenceCategory category,
            String title,
            String caption,
            String key,
            boolean defaultValue) {
        SwitchPreferenceCompat preference = new SwitchPreferenceCompat(requireContext());
        preference.setTitle(title);
        preference.setSummary(caption);
        preference.setKey(key);
        preference.setDefaultValue(defaultValue);
        preference.setIconSpaceReserved(false);
        preference.setWidgetLayoutResource(R.layout.preference_widget_material_switch);
        category.addPreference(preference);
    }

    private void addAction(
            PreferenceCategory category,
            String title,
            String caption,
            Preference.OnPreferenceClickListener onClick) {
        Preference preference = new Preference(requireContext());
        preference.setTitle(title);
        preference.setSummary(caption);
        preference.setPersistent(false);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(onClick);
        category.addPreference(preference);
    }

    private void addChoiceDialog(
            PreferenceCategory category,
            String title,
            String caption,
            String dialogTitle,
            String[] choices,
            IntGetter current,
            IntCallback onPicked) {
        addAction(
                category,
                title,
                caption,
                preference -> {
                    new InputDialog(
                            requireActivity(),
                            dialogTitle,
                            choices,
                            String.valueOf(current.get())) {
                        @Override // com.noname81.lmt.InputDialog
                        public boolean onOkClicked(String value) {
                            try {
                                onPicked.run(Integer.parseInt(value));
                            } catch (NumberFormatException ignored) {
                            }
                            return true;
                        }
                    }.show();
                    return true;
                });
    }

    private void addNumericDialog(
            PreferenceCategory category,
            String title,
            String caption,
            String dialogTitle,
            IntGetter current,
            IntCallback onSaved) {
        addAction(
                category,
                title,
                caption,
                preference -> {
                    new InputDialog(
                            requireActivity(),
                            dialogTitle,
                            "Set new value",
                            String.valueOf(current.get()),
                            true) {
                        @Override // com.noname81.lmt.InputDialog
                        public boolean onOkClicked(String value) {
                            try {
                                onSaved.run(Integer.parseInt(value));
                            } catch (NumberFormatException ignored) {
                            }
                            return true;
                        }
                    }.show();
                    return true;
                });
    }

    private void addTextDialog(
            PreferenceCategory category,
            String title,
            String caption,
            String dialogTitle,
            StringGetter current,
            StringCallback onSaved) {
        addAction(
                category,
                title,
                caption,
                preference -> {
                    new InputDialog(
                            requireActivity(), dialogTitle, "Set new value", current.get(), false) {
                        @Override // com.noname81.lmt.InputDialog
                        public boolean onOkClicked(String value) {
                            onSaved.run(value);
                            return true;
                        }
                    }.show();
                    return true;
                });
    }

    private void addColorDialog(
            PreferenceCategory category,
            String title,
            String caption,
            String dialogTitle,
            String[] labels,
            StringGetter current,
            StringCallback onSaved) {
        addAction(
                category,
                title,
                caption,
                preference -> {
                    new ColorDialog(requireActivity(), dialogTitle, labels, current.get()) {
                        @Override // com.noname81.lmt.ColorDialog
                        public boolean onOkClicked(String value) {
                            onSaved.run(value);
                            return true;
                        }
                    }.show();
                    return true;
                });
    }
}
