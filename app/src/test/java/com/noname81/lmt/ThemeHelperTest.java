package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.utilities.Contrast;
import com.google.android.material.color.utilities.Hct;

import org.junit.Test;

/** ROADMAP 1.10: the stored Appearance values map to the right night mode and colour seed. */
public class ThemeHelperTest {

    @Test
    public void storedModesMapToNightModes() {
        assertEquals(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                ThemeHelper.nightModeFor(ThemeHelper.MODE_SYSTEM));
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemeHelper.nightModeFor(ThemeHelper.MODE_LIGHT));
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeHelper.nightModeFor(ThemeHelper.MODE_DARK));
        // A corrupted or hand-edited value falls back to following the system.
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeHelper.nightModeFor(7));
    }

    @Test
    public void onlyPresetsAndCustomHaveASeed() {
        int custom = 0xFF123456;
        assertNull(ThemeHelper.seedFor(ThemeHelper.COLOR_DEFAULT, custom));
        for (int color = ThemeHelper.COLOR_DEFAULT + 1; color < ThemeHelper.COLOR_CUSTOM; color++) {
            assertNotNull(ThemeHelper.colorNames()[color], ThemeHelper.seedFor(color, custom));
        }
        assertEquals(Integer.valueOf(custom), ThemeHelper.seedFor(ThemeHelper.COLOR_CUSTOM, custom));
        assertNull(ThemeHelper.seedFor(-1, custom));
        assertNull(ThemeHelper.seedFor(ThemeHelper.COLOR_CUSTOM + 1, custom));
    }

    @Test
    public void everyColorChoiceHasALabel() {
        String[] names = ThemeHelper.colorNames();
        assertEquals(ThemeHelper.COLOR_CUSTOM + 1, names.length);
        for (String name : names) {
            assertNotNull(name);
        }
        assertEquals("Custom", names[ThemeHelper.COLOR_CUSTOM]);
    }

    /** ROADMAP 1.12: the dark primary is more saturated than M3's tone 80, and still readable. */
    @Test
    @SuppressWarnings("RestrictedApi")
    public void darkPrimaryIsVividAndReadable() {
        Hct red = Hct.fromInt(0xFFF44336);
        Hct vivid = Hct.fromInt(ThemeHelper.vividPrimary(red.getHue(), red.getChroma()));
        Hct tone80 = Hct.from(red.getHue(), red.getChroma(), 80);
        assertEquals(ThemeHelper.DARK_PRIMARY_TONE, vivid.getTone(), 1);
        assertTrue(vivid.getChroma() > tone80.getChroma() * 1.5);
        // Dark surface is tone 6, on-primary text tone 20: both need WCAG AA (4.5:1).
        assertTrue(Contrast.ratioOfTones(vivid.getTone(), 6) >= 4.5);
        assertTrue(Contrast.ratioOfTones(vivid.getTone(), 20) >= 4.5);
    }
}
