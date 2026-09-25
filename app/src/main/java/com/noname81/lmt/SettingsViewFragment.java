package com.noname81.lmt;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriPermission;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
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
    private static final int REQUEST_IMAGE_FOLDER = 9003;

    private SettingsValues mSettings;
    private RootContext mRootContext;
    private TouchServiceNative mTouchServiceNative;
    private SwitchPreferenceCompat mActivateLmtPreference;
    private Preference mFeaturesPreference;
    private Preference mImageFolderPreference;
    private Preference mClearGestureBlacklist;
    private Preference mClearPieBlacklist;

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
        // Apps are added to the blacklists in another screen.
        if (this.mClearGestureBlacklist != null) {
            this.mClearGestureBlacklist.setSummary(
                    blacklistSummary(this.mSettings.mBlacklist.size()));
        }
        if (this.mClearPieBlacklist != null) {
            this.mClearPieBlacklist.setSummary(
                    blacklistSummary(this.mSettings.mBlacklistPie.size()));
        }
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

        this.mClearGestureBlacklist =
                addClearBlacklist(
                        category,
                        "Clear gesture blacklist",
                        "gesture recognition",
                        () -> this.mSettings.mBlacklist.size(),
                        this.mSettings::clearBlacklisted);
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

        this.mClearPieBlacklist =
                addClearBlacklist(
                        category,
                        "Clear pie blacklist",
                        "the pie",
                        () -> this.mSettings.mBlacklistPie.size(),
                        this.mSettings::clearBlacklistedPie);
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
                ColorDialog.PREVIEW_PIE,
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

        // Shown everywhere, but only phones with Samsung's live window blur (the Samsung flagship, not the
        // TEST_PHONE) can turn it on; there it is on by default.
        addSwitch(
                        category,
                        "Samsung Live Blur",
                        SamsungBlur.isAvailable()
                                ? "Apply real time blur. Does not blur pie's inner radius."
                                : "Apply real time blur. Does not blur pie's inner radius.\n"
                                        + "Not available on this device",
                        "PieBlurLive",
                        SamsungBlur.isAvailable())
                .setEnabled(SamsungBlur.isAvailable());
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

        // Picked with the system file picker (ROADMAP 1.13): a typed path was never used, and
        // since Android 11 most paths outside the app folder can't be written anyway.
        this.mImageFolderPreference =
                addAction(
                        category,
                        "Set user image search path",
                        imageFolderSummary(),
                        preference -> {
                            new InputDialog(
                                    requireActivity(),
                                    "User image folder",
                                    new String[] {
                                        "Choose a folder…", "Use the app folder (default)"
                                    },
                                    IconFolder.isPicked(this.mSettings.loadResourceSearchPath())
                                            ? "0"
                                            : "1") {
                                @Override
                                public boolean onOkClicked(String value) {
                                    if (value.equals("0")) {
                                        startActivityForResult(
                                                new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),
                                                REQUEST_IMAGE_FOLDER);
                                    } else {
                                        setImageFolder(null);
                                    }
                                    return true;
                                }
                            }.show();
                            return true;
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
                ColorDialog.PREVIEW_POINTER,
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
                ColorDialog.PREVIEW_STATUS_INFOS,
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
                },
                fontRowStyle());
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
        } else if (requestCode == REQUEST_IMAGE_FOLDER && data.getData() != null) {
            setImageFolder(data.getData());
        }
    }

    /** Keeps access to a picked folder across reboots; null goes back to the app folder. */
    private void setImageFolder(Uri folder) {
        ContentResolver resolver = requireActivity().getContentResolver();
        int access = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        // Only one folder is ever used: let go of the previous one.
        for (UriPermission permission : resolver.getPersistedUriPermissions()) {
            if (folder == null || !permission.getUri().equals(folder)) {
                try {
                    resolver.releasePersistableUriPermission(permission.getUri(), access);
                } catch (SecurityException ignored) {
                }
            }
        }
        if (folder != null) {
            resolver.takePersistableUriPermission(folder, access);
        }
        this.mSettings.saveResourceSearchPath(folder != null ? folder.toString() : null);
        this.mSettings.restartServiceIfRequired();
        if (this.mImageFolderPreference != null) {
            this.mImageFolderPreference.setSummary(imageFolderSummary());
        }
    }

    private String imageFolderSummary() {
        String folder = this.mSettings.loadResourceSearchPath();
        return IconFolder.isPicked(folder)
                ? "Folder with your own pie images: " + IconFolder.describe(folder)
                : "Folder with your own pie images: the app folder (default)";
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
        SettingsBackup.exportZip(this.mSettings, IconFolder.of(requireActivity()), destStream);
        Toast.makeText(requireActivity(), "Settings exported", Toast.LENGTH_SHORT).show();
    }

    private void importSettings(InputStream srcStream) throws Exception {
        boolean restored =
                SettingsBackup.importZip(
                        this.mSettings, IconFolder.of(requireActivity()), srcStream);
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

    private SwitchPreferenceCompat addSwitch(
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
        return preference;
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

    private static final String[] FEATURE_NAMES = {
        "Gestures and ISAs", "Gestures, ISAs and pie", "Pie"
    };

    private String featuresSummary(RootContext rootContext) {
        int mode = this.mSettings.loadTouchServiceMode();
        String caption =
                mode < 2 && rootContext.isRootDenied()
                        ? "Gestures and ISAs need root, which isn't granted"
                        : "Configure LMT's feature set";
        return withCurrent(caption, choiceLabel(FEATURE_NAMES, mode));
    }

    /**
     * Clears a blacklist after asking (ROADMAP 1.13: it used to go on a single tap), and shows how
     * many apps it holds.
     */
    private Preference addClearBlacklist(
            PreferenceCategory category,
            String title,
            String from,
            IntGetter count,
            Runnable clear) {
        return addAction(
                category,
                title,
                blacklistSummary(count.get()),
                preference -> {
                    int apps = count.get();
                    if (apps == 0) {
                        Toast.makeText(requireContext(), "No apps are excluded", Toast.LENGTH_SHORT)
                                .show();
                        return true;
                    }
                    new MaterialAlertDialogBuilder(requireActivity())
                            .setTitle(title + "?")
                            .setMessage(
                                    (apps == 1 ? "1 app" : apps + " apps")
                                            + " will work with "
                                            + from
                                            + " again.")
                            .setPositiveButton(
                                    "Clear",
                                    (d, w) -> {
                                        clear.run();
                                        preference.setSummary(blacklistSummary(count.get()));
                                    })
                            .setNegativeButton(R.string.dialog_cancel, null)
                            .show();
                    return true;
                });
    }

    private static String blacklistSummary(int apps) {
        return withCurrent(
                "Clear all excluded apps",
                apps == 0
                        ? "no apps excluded"
                        : apps == 1 ? "1 app excluded" : apps + " apps excluded");
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
        addChoiceDialog(category, title, caption, dialogTitle, choices, current, onPicked, null);
    }

    private void addChoiceDialog(
            PreferenceCategory category,
            String title,
            String caption,
            String dialogTitle,
            String[] choices,
            IntGetter current,
            IntCallback onPicked,
            InputDialog.ItemStyle style) {
        addAction(
                category,
                title,
                withCurrent(caption, choiceLabel(choices, current.get())),
                preference -> {
                    new InputDialog(
                            requireActivity(),
                            dialogTitle,
                            choices,
                            String.valueOf(current.get()),
                            null,
                            style) {
                        @Override
                        public boolean onOkClicked(String value) {
                            try {
                                onPicked.run(Integer.parseInt(value));
                                preference.setSummary(
                                        withCurrent(caption, choiceLabel(choices, current.get())));
                            } catch (NumberFormatException ignored) {
                            }
                            return true;
                        }
                    }.show();
                    return true;
                });
    }

    /**
     * Shows each "Pie status info font" choice in its font (ROADMAP 1.13): Roboto Thin, Roboto
     * Light or the system font, a size step smaller or larger for small and big.
     */
    private InputDialog.ItemStyle fontRowStyle() {
        Typeface[] typefaces = new Typeface[3];
        for (int i = 0; i < typefaces.length; i++) {
            typefaces[i] = PieStatusInfoPreviewView.typeface(requireContext(), i * 3);
        }
        float[] sizes = {14, 17, 20};
        return (position, row) -> {
            row.setTypeface(typefaces[position / 3]);
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizes[position % 3]);
        };
    }

    /**
     * A row's description plus what it is set to now (ROADMAP 1.13), so you don't have to open
     * every dialog to see the settings.
     */
    private static String withCurrent(String caption, String current) {
        return caption + "\nCurrently: " + current;
    }

    private static String choiceLabel(String[] choices, int index) {
        // Multi-line choices ("Bottom\n(not centered)") read better on one line here.
        return index >= 0 && index < choices.length
                ? choices[index].replace('\n', ' ').replace("…", "")
                : String.valueOf(index);
    }

    /** "500 ms (default)" or "300 ms, default 500 ms". */
    private static String sliderLabel(int value, int defaultValue, String unit) {
        return value == defaultValue
                ? withUnit(value, unit) + " (default)"
                : withUnit(value, unit) + ", default " + withUnit(defaultValue, unit);
    }

    /** Slider captions say "..., default = 50"; the summary now shows that on its own line. */
    static String withoutDefault(String caption) {
        return caption.replaceFirst("(?i),?\\s*default\\s*=.*$", "").trim();
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
        String description = withoutDefault(caption);
        addAction(
                category,
                title,
                withCurrent(description, sliderLabel(current.get(), defaultValue, unit)),
                preference -> {
                    showSliderDialog(
                            dialogTitle,
                            unit,
                            min,
                            max,
                            step,
                            defaultValue,
                            current.get(),
                            v -> {
                                onSaved.run(v);
                                preference.setSummary(
                                        withCurrent(
                                                description,
                                                sliderLabel(current.get(), defaultValue, unit)));
                            });
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
                withCurrent(caption, codedLabel(choices, values, unit, current.get())),
                preference -> {
                    int value = current.get();
                    int checked = values.length;
                    for (int i = 0; i < values.length; i++) {
                        if (values[i] == value) {
                            checked = i;
                        }
                    }
                    IntCallback save =
                            v -> {
                                onSaved.run(v);
                                preference.setSummary(
                                        withCurrent(
                                                caption,
                                                codedLabel(choices, values, unit, current.get())));
                            };
                    new InputDialog(requireActivity(), title, choices, String.valueOf(checked)) {
                        @Override
                        public boolean onOkClicked(String picked) {
                            int which = Integer.parseInt(picked);
                            if (which < values.length) {
                                save.run(values[which]);
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
                                        save);
                            }
                            return true;
                        }
                    }.show();
                    return true;
                });
    }

    /** The fixed choice's label, or "Custom size, tinted (96 px)". */
    private static String codedLabel(String[] choices, int[] values, String unit, int value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return choiceLabel(choices, i);
            }
        }
        return choiceLabel(choices, values.length) + " (" + withUnit(value, unit) + ")";
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
            int preview,
            StringGetter current,
            StringCallback onSaved) {
        String[] choices = new String[presets.length + 1];
        System.arraycopy(presets, 0, choices, 0, presets.length);
        choices[presets.length] = "Custom…";
        addAction(
                category,
                title,
                withCurrent(caption, choiceLabel(choices, presetIndex(presets, current.get()))),
                preference -> {
                    String value = current.get();
                    int checked = presetIndex(presets, value);
                    boolean custom = checked == presets.length;
                    StringCallback save =
                            v -> {
                                onSaved.run(v);
                                preference.setSummary(
                                        withCurrent(
                                                caption,
                                                choiceLabel(
                                                        choices,
                                                        presetIndex(presets, current.get()))));
                            };
                    new InputDialog(
                            requireActivity(), dialogTitle, choices, String.valueOf(checked)) {
                        @Override
                        public boolean onOkClicked(String picked) {
                            int which = Integer.parseInt(picked);
                            if (which < presets.length) {
                                save.run(String.valueOf(which));
                            } else {
                                // A preset code means nothing to the editor: start it empty.
                                new ColorDialog(
                                        requireActivity(),
                                        dialogTitle,
                                        labels,
                                        custom ? value : "",
                                        preview) {
                                    @Override
                                    public boolean onOkClicked(String colorString) {
                                        save.run(colorString);
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

    /** The preset a stored color string names ("0", "1", ...), or presets.length for custom. */
    private static int presetIndex(String[] presets, String value) {
        for (int i = 0; i < presets.length; i++) {
            if (String.valueOf(i).equals(value)) {
                return i;
            }
        }
        return presets.length;
    }

    /** Preset 0 follows the wallpaper's accent on Android 12+ (see PieMenu.init). */
    private static String defaultColorName(String before12) {
        return Build.VERSION.SDK_INT >= 31 ? "System accent" : before12;
    }
}
