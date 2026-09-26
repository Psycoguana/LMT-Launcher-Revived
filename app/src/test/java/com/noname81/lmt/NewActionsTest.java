package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

/** ROADMAP 2.6 and 1.17: Flashlight, Keep screen on, Pop-up window and the Brightness dial. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class NewActionsTest {

    @Test
    public void newActionsAreAppendedSoSavedIdsKeepTheirMeaning() {
        assertEquals(Action.PieRecentApps + 1, Action.Flashlight);
        assertEquals(Action.Flashlight + 1, Action.KeepScreenOn);
        assertEquals(Action.KeepScreenOn + 1, Action.PopUpWindow);
        assertEquals(Action.PopUpWindow + 1, Action.BrightnessDial);
        assertEquals(Action.BrightnessDial + 1, Action.Folder);
        assertEquals(Action.Folder + 1, Action.names.length);
        assertEquals("Flashlight", Action.names[Action.Flashlight]);
        assertEquals("KeepScreenOn", Action.names[Action.KeepScreenOn]);
        assertEquals("PopUpWindow", Action.names[Action.PopUpWindow]);
    }

    @Test
    public void thePickerFilesThemUnderTheirSections() {
        Context context = ApplicationProvider.getApplicationContext();
        SettingsValues.getInstance(context).setCurrentPie(0);
        CommandSelectActivity picker =
                Robolectric.buildActivity(CommandSelectActivity.class).setup().get();
        Map<String, String> sectionOf = new HashMap<>();
        for (UiTestSupport.SectionedRow row :
                UiTestSupport.sectionedActionRows(picker.rowsForTest())) {
            sectionOf.put(row.title, row.section);
        }
        String toggles = context.getString(R.string.commands_toogle_commands);
        String advanced = context.getString(R.string.commands_advanced_commands);
        assertEquals(toggles, sectionOf.get("Flashlight"));
        assertEquals(toggles, sectionOf.get("KeepScreenOn"));
        assertEquals(advanced, sectionOf.get("PopUpWindow"));
        assertEquals(
                context.getString(R.string.commands_pie_action_commands),
                sectionOf.get("BrightnessDial"));
        assertEquals(
                context.getString(R.string.commands_pie_action_commands), sectionOf.get("Folder"));
    }

    @Test
    public void noneOfThemNeedsRoot() {
        assertFalse(Action.needsRoot(Action.Flashlight));
        assertFalse(Action.needsRoot(Action.KeepScreenOn));
        assertFalse(Action.needsRoot(Action.PopUpWindow));
        assertFalse(Action.needsRoot(Action.BrightnessDial));
    }

    @Test
    public void keepScreenOnTogglesAnOverlayOnAndOff() {
        Context context = ApplicationProvider.getApplicationContext();
        KeepScreenOn.toggle(context);
        assertTrue(KeepScreenOn.isOn());
        KeepScreenOn.toggle(context);
        assertFalse(KeepScreenOn.isOn());
    }

    @Test
    public void theAlwaysThereDialIsASettingThatBackupsCarry() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        SettingsValues settings = SettingsValues.getInstance(context);
        assertEquals(0, settings.loadPieBrightnessDial());
        settings.savePieBrightnessDial(1);
        assertEquals(
                1, SettingsBackup.exportSettingsToJson(settings).getInt("PieBrightnessDial"));
        settings.savePieBrightnessDial(0);
    }
}
