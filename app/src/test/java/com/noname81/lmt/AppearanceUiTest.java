package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

/** The Appearance page (ROADMAP 1.10): each choice is saved and applied. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = ShadowTouchServiceNative.class, instrumentedPackages = "com.noname81.lmt")
public class AppearanceUiTest {
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        UiTestSupport.resetSingletons(context);
    }

    @After
    public void tearDown() throws Exception {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        UiTestSupport.resetSingletons(context);
    }

    @Test
    public void everyThemeChoiceIsSavedAndSetsTheNightMode() {
        for (int mode = 0; mode < ThemeHelper.MODE_NAMES.length; mode++) {
            AppearanceActivity page = openPage();
            pick(row(page, "Theme"), mode);
            assertEquals(mode, SettingsValues.getInstance(context).loadThemeMode());
            assertEquals(
                    ThemeHelper.MODE_NAMES[mode],
                    ThemeHelper.nightModeFor(mode),
                    AppCompatDelegate.getDefaultNightMode());
        }
    }

    @Test
    public void followSystemColorIsSavedAndPickingAColorTurnsItOff() {
        SwitchPreferenceCompat follow = (SwitchPreferenceCompat) row(openPage(), "Follow system color");
        assertTrue(follow.isEnabled());
        follow.performClick();
        UiTestSupport.idle();
        assertEquals(1, SettingsValues.getInstance(context).loadThemeFollowSystemColor());
        // Recreating ran ThemedActivity with the system colors; the fresh page reports them.
        AppearanceActivity page = openPage();
        assertTrue(((SwitchPreferenceCompat) row(page, "Follow system color")).isChecked());
        assertEquals("System color", String.valueOf(row(page, "Color").getSummary()));

        // Default, not a preset: seeded schemes are applied through a ResourcesLoader color
        // override that Robolectric's resource engine can't apply (verified on-device instead).
        pickSwatch(row(page, "Color"), ThemeHelper.COLOR_DEFAULT);
        assertEquals(0, SettingsValues.getInstance(context).loadThemeFollowSystemColor());
        assertEquals(ThemeHelper.COLOR_DEFAULT, SettingsValues.getInstance(context).loadThemeColor());
        page = openPage();
        assertFalse(((SwitchPreferenceCompat) row(page, "Follow system color")).isChecked());
        assertEquals("Default", String.valueOf(row(page, "Color").getSummary()));
    }

    @Test
    @Config(sdk = 30)
    public void colorIsUnavailableBeforeAndroid12() {
        AppearanceActivity page = openPage();
        assertFalse(row(page, "Follow system color").isEnabled());
        Preference color = row(page, "Color");
        assertFalse(color.isEnabled());
        assertEquals("Needs Android 12 or newer", String.valueOf(color.getSummary()));
    }

    private static AppearanceActivity openPage() {
        AppearanceActivity page = Robolectric.buildActivity(AppearanceActivity.class).setup().get();
        UiTestSupport.idle();
        return page;
    }

    private static Preference row(AppearanceActivity page, String title) {
        PreferenceFragmentCompat fragment =
                (PreferenceFragmentCompat)
                        page.getSupportFragmentManager().findFragmentById(R.id.appearance_container);
        for (int i = 0; i < fragment.getPreferenceScreen().getPreferenceCount(); i++) {
            Preference preference = fragment.getPreferenceScreen().getPreference(i);
            if (title.contentEquals(preference.getTitle())) {
                return preference;
            }
        }
        throw new AssertionError("no row titled " + title);
    }

    /** Taps the Color row, the circle at this index in its grid, then OK. */
    private static void pickSwatch(Preference row, int index) {
        row.performClick();
        UiTestSupport.idle();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        RecyclerView grid = dialog.findViewById(R.id.swatch_grid);
        assertEquals(ThemeHelper.COLOR_CUSTOM + 1, grid.getAdapter().getItemCount());
        grid.findViewHolderForAdapterPosition(index).itemView.findViewById(R.id.swatch).performClick();
        UiTestSupport.idle();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        UiTestSupport.idle();
    }

    /** Taps a row, then the choice at this index in the list dialog it opens. */
    private static void pick(Preference row, int index) {
        row.performClick();
        UiTestSupport.idle();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        dialog.getListView().performItemClick(null, index, index);
        UiTestSupport.idle();
    }
}
