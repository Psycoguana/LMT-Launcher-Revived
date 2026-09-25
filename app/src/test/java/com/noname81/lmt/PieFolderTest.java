package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** ROADMAP 1.17: pie folders are picked for pie slots, edited, saved and backed up. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class PieFolderTest {
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        UiTestSupport.resetSingletons(context);
    }

    @After
    public void tearDown() throws Exception {
        UiTestSupport.resetSingletons(context);
    }

    private boolean pickerOffersFolder() {
        CommandSelectActivity picker =
                Robolectric.buildActivity(CommandSelectActivity.class).setup().get();
        for (UiTestSupport.SectionedRow row :
                UiTestSupport.sectionedActionRows(picker.rowsForTest())) {
            if (row.title.equals("Folder")) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void onlyPieSlotsOfferFolders() {
        SettingsValues settings = SettingsValues.getInstance(context);
        settings.setCurrentPie(4);
        assertTrue(pickerOffersFolder());
        settings.setCurrentGesture(0);
        assertFalse(pickerOffersFolder());
        settings.setCurrentFolderItem(4, 0);
        assertFalse("no folders in folders", pickerOffersFolder());
    }

    @Test
    public void pickingFolderSavesItAndOpensTheEditor() {
        SettingsValues.getInstance(context).setCurrentPie(4);
        CommandSelectActivity picker =
                Robolectric.buildActivity(CommandSelectActivity.class).setup().get();
        int position = -1;
        for (UiTestSupport.SectionedRow row :
                UiTestSupport.sectionedActionRows(picker.rowsForTest())) {
            if (row.title.equals("Folder")) {
                position = row.position;
            }
        }
        picker.clickRowForTest(position);
        UiTestSupport.idle();
        Action slot = SettingsValues.getInstance(context).getPieAction(4);
        assertEquals(Action.Folder, slot.getType());
        assertEquals("Folder", slot.getString());
        Intent started = shadowOf(picker).getNextStartedActivity();
        assertEquals(FolderActivity.class.getName(), started.getComponent().getClassName());
        assertEquals(4, started.getIntExtra(FolderActivity.EXTRA_SLOT, -1));
    }

    @Test
    public void folderItemsAreSavedPerSlotAndItem() throws Exception {
        SettingsValues settings = SettingsValues.getInstance(context);
        settings.setCurrentFolderItem(4, 2);
        settings.setCurrentAction(null, new Action(Action.Flashlight));
        assertEquals(Action.Flashlight, settings.getCurrentAction().getType());
        // A fresh instance reads the prefs back (resetSingletons would also clear them).
        java.lang.reflect.Field instance = SettingsValues.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        SettingsValues reloaded = SettingsValues.getInstance(context);
        assertEquals(Action.Flashlight, reloaded.getFolderAction(4, 2).getType());
        assertEquals(Action.None, reloaded.getFolderAction(4, 1).getType());
        assertEquals(Action.None, reloaded.getFolderAction(6, 2).getType());
    }

    @Test
    public void theEditorPicksAnItemAndKeepsTheName() {
        SettingsValues settings = SettingsValues.getInstance(context);
        settings.setCurrentPie(4);
        settings.setCurrentAction(null, new Action(Action.Folder, "Media"));
        org.robolectric.android.controller.ActivityController<FolderActivity> controller =
                Robolectric.buildActivity(FolderActivity.class, FolderActivity.intent(context, 4))
                        .setup();
        FolderActivity editor = controller.get();
        android.widget.EditText name = editor.findViewById(R.id.folder_name);
        assertEquals("Media", name.getText().toString());
        editor.clickItemForTest(3);
        Intent started = shadowOf(editor).getNextStartedActivity();
        assertEquals(CommandSelectActivity.class.getName(), started.getComponent().getClassName());
        settings.setCurrentAction(null, new Action(Action.KeepScreenOn));
        assertEquals(Action.KeepScreenOn, settings.getFolderAction(4, 3).getType());
        name.setText("Tools");
        controller.pause();
        assertEquals("Tools", settings.getPieAction(4).getString());
    }

    @Test
    public void backupsCarryFolders() throws Exception {
        SettingsValues settings = SettingsValues.getInstance(context);
        settings.setCurrentFolderItem(10, 7);
        settings.setCurrentAction(null, new Action(Action.WebPage, "https://xda"));
        JSONObject json = SettingsBackup.exportSettingsToJson(settings);
        assertEquals(Action.WebPage, json.getInt("PieFolder10Item7 Type"));
        assertEquals("https://xda", json.getString("PieFolder10Item7 String"));
        assertEquals(0, json.getInt("PieFolderOpen"));
    }

}
