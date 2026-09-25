package com.noname81.lmt;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * The main settings tab.
 *
 * <p>Every row is a {@link Preference} identified by its actual preference key (or, for pure action
 * rows, none at all) - not by its position in a list, which is how the ~1572-line {@code
 * SettingsViewHelper}/position-dispatch predecessor of this class worked (ROADMAP 1.6 phase 2).
 *
 * <p>Only the 8 genuinely boolean-backed settings ({@link SettingsPreferenceDataStore}) are real
 * {@link SwitchPreferenceCompat} rows backed by a {@code PreferenceDataStore}. Everything else is a
 * non-persistent {@link Preference} whose click listener opens the exact same {@link
 * InputDialog}/{@link ColorDialog} the old dispatcher used and writes through {@link
 * SettingsValues} directly. Both dialog classes build on {@code MaterialAlertDialogBuilder}; {@code
 * SettingsScreenUiTest} still sees them through {@code ShadowDialog.getLatestDialog()}, which
 * tracks any shown {@code Dialog}, and reads their fields from the real view tree.
 */
public class SettingsViewFragment extends PreferenceFragmentCompat {
    private static final int REQUEST_EXPORT_SETTINGS = 9001;
    private static final int REQUEST_IMPORT_SETTINGS = 9002;

    private SettingsValues mSettings;
    private RootContext mRootContext;
    private TouchServiceNative mTouchServiceNative;
    private SwitchPreferenceCompat mActivateLmtPreference;
    private Preference mFeaturesPreference;

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

    @Override
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

    @Override
    public void onResume() {
        super.onResume();
        if (this.mFeaturesPreference != null && this.mSettings.loadTouchServiceMode() < 2) {
            // Coming back from Magisk after granting root: ask su again (only after a "no").
            RootContext rootContext =
                    RootContext.getInstance(requireContext().getApplicationContext());
            rootContext.recheckRoot();
            Preference featuresPreference = this.mFeaturesPreference;
            rootContext.whenRootKnown(
                    () -> featuresPreference.setSummary(featuresSummary(rootContext)));
        }
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

        // Gestures and ISAs read the touchscreen through su: grey them out without root.
        String[] features = {
            "Gestures [Root] and ISAS [Root] ", "Gestures [Root] , ISAS [Root] and Pie", "Pie"
        };
        RootContext rootContext = RootContext.getInstance(requireContext().getApplicationContext());
        Preference featuresPreference =
                addAction(
                        general,
                        "Set active features",
                        featuresSummary(rootContext),
                        preference -> {
                            showFeaturesDialog(features, !rootContext.isRootDenied());
                            return true;
                        });
        this.mFeaturesPreference = featuresPreference;
        rootContext.whenRootKnown(
                () -> featuresPreference.setSummary(featuresSummary(rootContext)));

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
                    // The service opens the device when it restarts; opening it here as well
                    // left a second reader running under su (ROADMAP 1.13).
                    this.mSettings.saveInputDevice(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addSliderDialog(
                category,
                "Touchscreen to screen factor X",
                "Resolution factor in %, default = 100%",
                "Touchscreen to screen factor X",
                "%",
                50,
                300,
                1,
                100,
                () -> (int) (this.mSettings.loadTouchscreenScreenFactorX() * 100.0f),
                this.mSettings::saveTouchscreenScreenFactorX);

        addSliderDialog(
                category,
                "Touchscreen to screen factor Y",
                "Resolution factor in %, default = 100%",
                "Touchscreen to screen factor Y",
                "%",
                50,
                300,
                1,
                100,
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

        addSliderDialog(
                category,
                "Vibration Time",
                "Define the vibration time in ms, default = 30",
                "Vibration time",
                "ms",
                0,
                200,
                5,
                30,
                this.mSettings::loadGestureVibrationTime,
                this.mSettings::saveGestureVibrationTime);

        addSwitch(
                category,
                "Single touch gestures",
                "Configure if single touch gestures should be recognized, default = enabled",
                "STSupport",
                true);

        addSliderDialog(
                category,
                "Min gesture score",
                "Min score in %, default = 70%",
                "Min gesture score",
                "%",
                0,
                100,
                1,
                70,
                this.mSettings::loadMinScore,
                v -> {
                    this.mSettings.saveMinScore(v);
                    this.mTouchServiceNative.setMinScore(v);
                });

        addSliderDialog(
                category,
                "Min gesture path length",
                "Min number of touch points, default = 7",
                "Min gesture path length",
                "",
                2,
                50,
                1,
                7,
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
        addCodedChoice(
                category,
                "Min bounding box size",
                "Smallest swipe that counts, default = enabled",
                new String[] {"Disabled", "Enabled", "Custom size…"},
                new int[] {0, 1},
                "px",
                2,
                300,
                50,
                this.mSettings::loadSingleSwipesBBox,
                v -> {
                    this.mSettings.saveSingleSwipesBBox(v);
                    this.mTouchServiceNative.setSingleSwipesBBox(v);
                });

        addSliderDialog(
                category,
                "Activation area thickness",
                "Activation area width in pixel, default = 60",
                "Activation area thickness",
                "px",
                10,
                300,
                1,
                60,
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

        addSliderDialog(
                category,
                "Activation area thickness",
                "Activation area thickness in pixel, default = 50",
                "Activation area thickness",
                "px",
                10,
                300,
                1,
                50,
                this.mSettings::loadPieAreaX,
                v -> {
                    this.mSettings.savePieAreaX(v);
                    this.mSettings.setVisiblePieActivationAread(true);
                    this.mSettings.restartServiceIfRequired();
                });

        addSliderDialog(
                category,
                "Activation area length",
                "Activation area length in pixel, default = 600",
                "Activation area length",
                "px",
                100,
                screenLongSide(),
                10,
                600,
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
        addColorChoice(
                category,
                "Pie colors",
                "Color scheme of the pie, default = " + defaultColorName("Blue"),
                "Pie colors",
                new String[] {
                    defaultColorName("Blue"),
                    "Red",
                    "Grey",
                    "Transparent",
                    "White",
                    "Magenta and yellow",
                    "Green outline",
                    "Red outline"
                },
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

        addSliderDialog(
                category,
                "Pie inner radius",
                "Radius in dip, default = 60",
                "Pie inner radius",
                "dp",
                20,
                200,
                1,
                60,
                this.mSettings::loadPieInnerRadius,
                v -> {
                    this.mSettings.savePieInnerRadius(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addSliderDialog(
                category,
                "Pie outer radius",
                "Radius in dip, default = 80",
                "Pie outer radius",
                "dp",
                20,
                200,
                1,
                80,
                this.mSettings::loadPieOuterRadius,
                v -> {
                    this.mSettings.savePieOuterRadius(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addSliderDialog(
                category,
                "Pie shift size",
                "Size of the pie shift effect in pixel, default = 0",
                "Pie shift size",
                "px",
                0,
                30,
                1,
                0,
                this.mSettings::loadPieShiftSize,
                v -> {
                    this.mSettings.savePieShiftSize(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addSliderDialog(
                category,
                "Pie outline size",
                "Size of the outline in pixel, default = 3",
                "Pie outline size",
                "px",
                0,
                20,
                1,
                3,
                this.mSettings::loadPieOutlineSize,
                v -> {
                    this.mSettings.savePieOutlineSize(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addSliderDialog(
                category,
                "Pie slice gap",
                "Size of the gap between two pie slices, default = 0",
                "Pie slice gap",
                "",
                0,
                50,
                1,
                0,
                this.mSettings::loadPieSliceGap,
                v -> {
                    this.mSettings.savePieSliceGap(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addSliderDialog(
                category,
                "Pie start gap",
                "Size of the gap between pie and border, default = 0",
                "Pie start gap",
                "",
                0,
                50,
                1,
                0,
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
                            Toast.makeText(
                                            requireContext(),
                                            "Blur needs Android 12 or newer",
                                            Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            AccessibilityHandler.isAccessibilityAvailable(requireContext(), true);
                        }
                    }
                });

        addSliderDialog(
                category,
                "Blur amount",
                "Blur radius in pixels, 1 to 150, default = 50",
                "Blur amount",
                "px",
                1,
                150,
                1,
                50,
                this.mSettings::loadPieBlurRadius,
                this.mSettings::savePieBlurRadius);
    }

    private void buildPieBehaviorSection(PreferenceCategory category) {
        addSliderDialog(
                category,
                "Longpress time",
                "Min time for longpress in ms, default = 500",
                "Longpress time",
                "ms",
                100,
                2000,
                50,
                500,
                this.mSettings::loadPieLongpress,
                v -> {
                    this.mSettings.savePieLongpress(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addSliderDialog(
                category,
                "Animation time",
                "Time for the pie animation in ms, default = 80",
                "Animation time",
                "ms",
                0,
                500,
                10,
                80,
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

        addSliderDialog(
                category,
                "Vibration Time",
                "Define the vibration time in ms, default = 30",
                "Pie vibration time",
                "ms",
                0,
                200,
                5,
                30,
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
        addSwitch(
                category,
                "Rotate images",
                "Rotate icons based on pie angle",
                "PieRotateImages",
                true);

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

        // 1 and 2 both scale to 35 dp; 2 and custom sizes are tinted with the pie's icon color.
        addCodedChoice(
                category,
                "Show and scale app images",
                "Show app icons on the pie slices, default = enabled, original colors",
                new String[] {
                    "Disabled",
                    "Enabled, original colors",
                    "Enabled, tinted with the icon color",
                    "Custom size, tinted…"
                },
                new int[] {0, 1, 2},
                "px",
                3,
                256,
                96,
                this.mSettings::loadPieShowScaleAppImages,
                v -> {
                    this.mSettings.savePieShowScaleAppImages(v);
                    this.mSettings.restartServiceIfRequired();
                });

        // 1 and 2 both scale to 35 dp; 2 and custom sizes are tinted with the pie's icon color.
        addCodedChoice(
                category,
                "Show and scale user images",
                "Show your own images on the pie slices, default = disabled",
                new String[] {
                    "Disabled",
                    "Enabled, original colors",
                    "Enabled, tinted with the icon color",
                    "Custom size, tinted…"
                },
                new int[] {0, 1, 2},
                "px",
                3,
                256,
                96,
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

        addSliderDialog(
                category,
                "Pie pointer warp factor",
                "Configure the warp factor for the pie pointer feature, default = 300%",
                "Set pie pointer warp factor",
                "%",
                200,
                1000,
                10,
                300,
                this.mSettings::loadPiePointerWarpFactor,
                v -> {
                    this.mSettings.savePiePointerWarpFactor(v);
                    this.mSettings.restartServiceIfRequired();
                });

        addColorChoice(
                category,
                "Pie pointer color",
                "Color of the pie pointer, default = " + defaultColorName("Blue"),
                "Pie pointer colors",
                new String[] {defaultColorName("Blue")},
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

        addColorChoice(
                category,
                "Pie status info colors",
                "Colors of the clock and notifications, default = White",
                "Pie status info colors",
                new String[] {"White"},
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
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) requireActivity().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay()
                .getRealMetrics(displayMetrics);
        int[] touchscreen =
                TouchscreenDetector.detect(
                        this.mRootContext.runCommandResult(
                                "getevent -p | grep -E '(device|name|0035|0036)'", true),
                        displayMetrics.widthPixels,
                        displayMetrics.heightPixels);
        if (touchscreen != null) {
            inputDevice = touchscreen[0];
            factorX = touchscreen[1];
            factorY = touchscreen[2];
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
                            if (inputDeviceFinal <= -1
                                    || factorXFinal <= -1
                                    || factorYFinal <= -1) {
                                return;
                            }
                            this.mSettings.saveInputDevice(inputDeviceFinal);
                            this.mSettings.saveTouchscreenScreenFactorX(factorXFinal);
                            this.mSettings.saveTouchscreenScreenFactorY(factorYFinal);
                            this.mSettings.restartServiceIfRequired();
                        })
                .setNegativeButton(
                        "cancel",
                        (DialogInterface dialogInterface, int parsed) -> dialogInterface.dismiss())
                .create()
                .show();
    }

    @Override
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
        SettingsBackup.exportZip(
                this.mSettings, this.mSettings.loadResourceSearchPath(), destStream);
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

    private void showFeaturesDialog(String[] features, boolean root) {
        new InputDialog(
                requireActivity(),
                "Define feature set",
                features,
                String.valueOf(this.mSettings.loadTouchServiceMode()),
                new boolean[] {root, root, true}) {
            @Override
            public boolean onOkClicked(String value) {
                SettingsViewFragment.this.mSettings.saveTouchServiceMode(Integer.parseInt(value));
                SettingsViewFragment.this.mSettings.restartServiceIfRequired();
                requireActivity().recreate();
                return true;
            }
        }.show();
    }

    private String featuresSummary(RootContext rootContext) {
        if (this.mSettings.loadTouchServiceMode() < 2 && rootContext.isRootDenied()) {
            return "Gestures and ISAs need root, which isn't granted";
        }
        return "Configure LMT's feature set";
    }

    private Preference addAction(
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
        return preference;
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
                        @Override
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

    /** A number picked with a slider over [min, max]; a stored value outside widens it. */
    private void addSliderDialog(
            PreferenceCategory category,
            String title,
            String caption,
            String dialogTitle,
            String unit,
            int min,
            int max,
            int step,
            int defaultValue,
            IntGetter current,
            IntCallback onSaved) {
        addAction(
                category,
                title,
                caption,
                preference -> {
                    showSliderDialog(
                            dialogTitle,
                            unit,
                            min,
                            max,
                            step,
                            defaultValue,
                            current.get(),
                            onSaved);
                    return true;
                });
    }

    private void showSliderDialog(
            String title,
            String unit,
            int min,
            int max,
            int step,
            int defaultValue,
            int value,
            IntCallback onSaved) {
        int from = Math.min(min, value);
        int to = Math.max(max, value);
        // The slider insists every value sits on the step grid; fall back to 1 when it wouldn't.
        int stepSize =
                (to - from) % step == 0
                                && (value - from) % step == 0
                                && (defaultValue - from) % step == 0
                        ? step
                        : 1;
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.slider_dialog, null);
        TextView label = view.findViewById(R.id.slider_value);
        Slider slider = view.findViewById(R.id.slider);
        slider.setValueFrom(from);
        slider.setValueTo(to);
        slider.setStepSize(stepSize);
        slider.setTickVisible(false); // dozens of step dots just clutter the track
        slider.setValue(value);
        slider.setLabelFormatter(v -> withUnit((int) v, unit));
        label.setText(withUnit(value, unit));
        slider.addOnChangeListener((s, v, fromUser) -> label.setText(withUnit((int) v, unit)));
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(title)
                        .setView(view)
                        .setPositiveButton(
                                R.string.dialog_ok, (d, w) -> onSaved.run((int) slider.getValue()))
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .setNeutralButton("Default", null)
                        .show();
        // Set here so the button moves the slider instead of closing the dialog.
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(
                        v -> {
                            if (defaultValue >= from && defaultValue <= to) {
                                slider.setValue(defaultValue);
                            }
                        });
    }

    private static String withUnit(int value, String unit) {
        if (unit.isEmpty()) {
            return String.valueOf(value);
        }
        return unit.equals("%") ? value + "%" : value + " " + unit;
    }

    /** The screen's longer side in pixels, rounded up to 10: the most an activation area needs. */
    private int screenLongSide() {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) requireActivity().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay()
                .getRealMetrics(metrics);
        return (Math.max(metrics.widthPixels, metrics.heightPixels) + 9) / 10 * 10;
    }

    /**
     * A choice among fixed stored values, plus a last "Custom" entry that asks for a number with a
     * slider. Replaces rows whose number mixed modes and sizes ("0 = disabled, 1 = ..., >2 =
     * size").
     */
    private void addCodedChoice(
            PreferenceCategory category,
            String title,
            String caption,
            String[] choices,
            int[] values,
            String unit,
            int customMin,
            int customMax,
            int customDefault,
            IntGetter current,
            IntCallback onSaved) {
        addAction(
                category,
                title,
                caption,
                preference -> {
                    int value = current.get();
                    int checked = values.length;
                    for (int i = 0; i < values.length; i++) {
                        if (values[i] == value) {
                            checked = i;
                        }
                    }
                    new InputDialog(requireActivity(), title, choices, String.valueOf(checked)) {
                        @Override
                        public boolean onOkClicked(String picked) {
                            int which = Integer.parseInt(picked);
                            if (which < values.length) {
                                onSaved.run(values[which]);
                            } else {
                                int start = value >= customMin ? value : customDefault;
                                showSliderDialog(
                                        title,
                                        unit,
                                        customMin,
                                        customMax,
                                        1,
                                        customDefault,
                                        start,
                                        onSaved);
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
                        @Override
                        public boolean onOkClicked(String value) {
                            onSaved.run(value);
                            return true;
                        }
                    }.show();
                    return true;
                });
    }

    /**
     * Preset color schemes by their stored code ("0", "1", ...), plus "Custom..." for the color
     * editor, which stores a comma-separated color string instead.
     */
    private void addColorChoice(
            PreferenceCategory category,
            String title,
            String caption,
            String dialogTitle,
            String[] presets,
            String[] labels,
            StringGetter current,
            StringCallback onSaved) {
        String[] choices = new String[presets.length + 1];
        System.arraycopy(presets, 0, choices, 0, presets.length);
        choices[presets.length] = "Custom…";
        addAction(
                category,
                title,
                caption,
                preference -> {
                    String value = current.get();
                    int checked = presets.length;
                    for (int i = 0; i < presets.length; i++) {
                        if (value.equals(String.valueOf(i))) {
                            checked = i;
                        }
                    }
                    boolean custom = checked == presets.length;
                    new InputDialog(
                            requireActivity(), dialogTitle, choices, String.valueOf(checked)) {
                        @Override
                        public boolean onOkClicked(String picked) {
                            int which = Integer.parseInt(picked);
                            if (which < presets.length) {
                                onSaved.run(String.valueOf(which));
                            } else {
                                // A preset code means nothing to the editor: start it empty.
                                new ColorDialog(
                                        requireActivity(),
                                        dialogTitle,
                                        labels,
                                        custom ? value : "") {
                                    @Override
                                    public boolean onOkClicked(String colorString) {
                                        onSaved.run(colorString);
                                        return true;
                                    }
                                }.show();
                            }
                            return true;
                        }
                    }.show();
                    return true;
                });
    }

    /** Preset 0 follows the wallpaper's accent on Android 12+ (see PieMenu.init). */
    private static String defaultColorName(String before12) {
        return Build.VERSION.SDK_INT >= 31 ? "System accent" : before12;
    }
}
