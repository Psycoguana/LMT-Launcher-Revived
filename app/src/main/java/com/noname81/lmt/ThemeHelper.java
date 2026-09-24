package com.noname81.lmt;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

/**
 * Applies the Appearance settings (ROADMAP 1.10) - light/dark mode and the colour scheme - to an
 * activity. {@link ThemedActivity} calls both halves at the right moments.
 *
 * <p>Every colour choice other than the default goes through Material's {@link DynamicColors},
 * which builds a full M3 scheme either from the wallpaper or from a single seed colour. It only
 * works on Android 12+; below that the app keeps the default palette and the colour row says so.
 */
final class ThemeHelper {
    static final int MODE_SYSTEM = 0;
    static final int MODE_LIGHT = 1;
    static final int MODE_DARK = 2;
    static final String[] MODE_NAMES = {"Follow system", "Light", "Dark"};

    static final int COLOR_DEFAULT = 0;
    private static final int COLOR_FIRST_PRESET = 1;
    // Material palette 500s, as in most colour pickers. No deep purple: Default already is one.
    private static final String[] PRESET_NAMES = {
        "Red", "Pink", "Purple", "Indigo", "Blue", "Light blue", "Cyan",
        "Teal", "Green", "Lime", "Amber", "Orange", "Deep orange", "Brown"
    };
    private static final int[] PRESET_SEEDS = {
        0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4,
        0xFF009688, 0xFF4CAF50, 0xFFCDDC39, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722, 0xFF795548
    };
    static final int COLOR_CUSTOM = COLOR_FIRST_PRESET + PRESET_SEEDS.length;

    /** Material 3's baseline primary, i.e. what the default palette is built from. */
    static final int DEFAULT_CUSTOM_COLOR = 0xFF6750A4;

    private ThemeHelper() {}

    /** Before AppCompat attaches the base context; see {@link ThemedActivity}. */
    static void applyNightMode(Context context) {
        SettingsValues settings = SettingsValues.getInstance(context.getApplicationContext());
        AppCompatDelegate.setDefaultNightMode(nightModeFor(settings.loadThemeMode()));
    }

    /** Before {@code super.onCreate}, so nothing has been inflated with the old colours. */
    static void applyColors(Activity activity) {
        SettingsValues settings = SettingsValues.getInstance(activity.getApplicationContext());
        if (settings.loadThemeFollowSystemColor() > 0) {
            // Material You: the scheme Android derives from the wallpaper.
            DynamicColors.applyToActivityIfAvailable(activity);
            return;
        }
        Integer seed = seedFor(settings.loadThemeColor(), settings.loadThemeCustomColor());
        if (seed != null) {
            DynamicColors.applyToActivityIfAvailable(
                    activity, new DynamicColorsOptions.Builder().setContentBasedSource(seed).build());
        }
    }

    static int nightModeFor(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case MODE_DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    /** The seed for a colour choice, or null when it has none (default, unknown). */
    static Integer seedFor(int color, int customColor) {
        if (color == COLOR_CUSTOM) {
            return customColor;
        }
        int preset = color - COLOR_FIRST_PRESET;
        if (preset >= 0 && preset < PRESET_SEEDS.length) {
            return PRESET_SEEDS[preset];
        }
        return null;
    }

    /** Labels for the colour choices, indexed by the {@code COLOR_*} value. */
    static String[] colorNames() {
        String[] names = new String[COLOR_CUSTOM + 1];
        names[COLOR_DEFAULT] = "Default";
        System.arraycopy(PRESET_NAMES, 0, names, COLOR_FIRST_PRESET, PRESET_NAMES.length);
        names[COLOR_CUSTOM] = "Custom";
        return names;
    }

    /**
     * The colour a choice is shown as in the picker and on the Color row: its seed, or the
     * default palette's primary for Default.
     */
    static int swatchColor(int color, int customColor) {
        Integer seed = seedFor(color, customColor);
        return seed != null ? seed : DEFAULT_CUSTOM_COLOR;
    }

    /** The wallpaper's accent, shown on the Color row while following the system color. */
    static int systemAccent(Context context) {
        if (Build.VERSION.SDK_INT >= 31) {
            return ContextCompat.getColor(context, android.R.color.system_accent1_500);
        }
        return DEFAULT_CUSTOM_COLOR;
    }

    static boolean colorsAvailable() {
        return DynamicColors.isDynamicColorAvailable();
    }

    /**
     * Identifies the theme an activity was created with. Night mode changes recreate AppCompat
     * activities by themselves; colour changes don't, so a long-lived screen compares this in
     * onResume and recreates itself when it no longer matches.
     */
    static String stamp(Context context) {
        SettingsValues settings = SettingsValues.getInstance(context.getApplicationContext());
        return settings.loadThemeMode()
                + ":"
                + settings.loadThemeFollowSystemColor()
                + ":"
                + settings.loadThemeColor()
                + ":"
                + settings.loadThemeCustomColor();
    }
}
