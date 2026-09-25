package com.noname81.lmt;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

/**
 * Light/dark mode and colour scheme (ROADMAP 1.10). Each change is saved, then applied: night mode
 * through {@link AppCompatDelegate}, which recreates every open AppCompat activity itself, and a
 * colour change by recreating this activity - {@code LMT} notices the new theme stamp when the user
 * comes back and recreates too.
 */
public class AppearanceFragment extends PreferenceFragmentCompat {
    /** A little over MaterialSwitch's thumb animation. */
    private static final long SWITCH_ANIMATION_MS = 300;

    private SettingsValues mSettings;
    private Preference mThemeRow;
    private SwitchPreferenceCompat mFollowSystemRow;
    private ColorDotPreference mColorRow;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = requireContext();
        this.mSettings = SettingsValues.getInstance(context.getApplicationContext());
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        setPreferenceScreen(screen);

        this.mThemeRow =
                addRow(
                        screen,
                        "Theme",
                        preference -> {
                            pickThemeMode();
                            return true;
                        });
        this.mFollowSystemRow = new SwitchPreferenceCompat(context);
        this.mFollowSystemRow.setTitle("Follow system color");
        this.mFollowSystemRow.setSummary("Use the colors Android picks from your wallpaper");
        this.mFollowSystemRow.setPersistent(false);
        this.mFollowSystemRow.setIconSpaceReserved(false);
        this.mFollowSystemRow.setWidgetLayoutResource(R.layout.preference_widget_material_switch);
        this.mFollowSystemRow.setChecked(this.mSettings.loadThemeFollowSystemColor() > 0);
        this.mFollowSystemRow.setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    this.mSettings.saveThemeFollowSystemColor((Boolean) newValue ? 1 : 0);
                    // Recreating straight away tears the switch down mid-animation; let the thumb
                    // finish sliding first.
                    getListView()
                            .postDelayed(
                                    () -> {
                                        if (isAdded()) {
                                            requireActivity().recreate();
                                        }
                                    },
                                    SWITCH_ANIMATION_MS);
                    return true;
                });
        screen.addPreference(this.mFollowSystemRow);

        this.mColorRow = new ColorDotPreference(context);
        this.mColorRow.setTitle("Color");
        this.mColorRow.setPersistent(false);
        this.mColorRow.setIconSpaceReserved(false);
        this.mColorRow.setOnPreferenceClickListener(
                preference -> {
                    pickColor();
                    return true;
                });
        screen.addPreference(this.mColorRow);
        if (!ThemeHelper.colorsAvailable()) {
            this.mFollowSystemRow.setEnabled(false);
            this.mColorRow.setEnabled(false);
        }
        updateSummaries();
    }

    private Preference addRow(
            PreferenceScreen screen, String title, Preference.OnPreferenceClickListener onClick) {
        Preference preference = new Preference(requireContext());
        preference.setTitle(title);
        preference.setPersistent(false);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(onClick);
        screen.addPreference(preference);
        return preference;
    }

    private void updateSummaries() {
        this.mThemeRow.setSummary(
                ThemeHelper.MODE_NAMES[
                        clamp(this.mSettings.loadThemeMode(), ThemeHelper.MODE_NAMES.length)]);
        if (!ThemeHelper.colorsAvailable()) {
            this.mFollowSystemRow.setSummary("Needs Android 12 or newer");
            this.mColorRow.setSummary("Needs Android 12 or newer");
            this.mColorRow.setDotColor(null);
            return;
        }
        if (this.mSettings.loadThemeFollowSystemColor() > 0) {
            this.mColorRow.setSummary("System color");
            this.mColorRow.setDotColor(ThemeHelper.systemAccent(requireContext()));
            return;
        }
        int color = clamp(this.mSettings.loadThemeColor(), ThemeHelper.COLOR_CUSTOM + 1);
        int custom = this.mSettings.loadThemeCustomColor();
        this.mColorRow.setDotColor(ThemeHelper.swatchColor(color, custom));
        String summary = ThemeHelper.colorNames()[color];
        if (color == ThemeHelper.COLOR_CUSTOM) {
            summary += " (" + hex(custom) + ")";
        }
        this.mColorRow.setSummary(summary);
    }

    private void pickThemeMode() {
        new InputDialog(
                requireActivity(),
                "Theme",
                ThemeHelper.MODE_NAMES,
                Integer.toString(this.mSettings.loadThemeMode())) {
            @Override
            public boolean onOkClicked(String value) {
                int mode = Integer.parseInt(value);
                AppearanceFragment.this.mSettings.saveThemeMode(mode);
                updateSummaries();
                AppCompatDelegate.setDefaultNightMode(ThemeHelper.nightModeFor(mode));
                return true;
            }
        }.show();
    }

    private void pickColor() {
        ColorSwatchDialog.show(
                requireActivity(),
                "Color",
                clamp(this.mSettings.loadThemeColor(), ThemeHelper.COLOR_CUSTOM + 1),
                this.mSettings.loadThemeCustomColor(),
                choice -> {
                    if (choice == ThemeHelper.COLOR_CUSTOM) {
                        pickCustomColor();
                    } else {
                        applyColor(choice);
                    }
                });
    }

    private void pickCustomColor() {
        ThemeColorDialog.show(
                requireActivity(),
                "Custom color",
                this.mSettings.loadThemeCustomColor(),
                color -> {
                    this.mSettings.saveThemeCustomColor(color);
                    applyColor(ThemeHelper.COLOR_CUSTOM);
                });
    }

    /** A picked color replaces the system one, so this also turns Follow system color off. */
    private void applyColor(int color) {
        this.mSettings.saveThemeFollowSystemColor(0);
        this.mSettings.saveThemeColor(color);
        updateSummaries();
        requireActivity().recreate();
    }

    private static int clamp(int value, int size) {
        return value >= 0 && value < size ? value : 0;
    }

    private static String hex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }
}
