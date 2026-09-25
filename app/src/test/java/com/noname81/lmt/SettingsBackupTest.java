package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ROADMAP 1.5: import/export. Pins the two things that would otherwise silently corrupt a restore -
 * a never-touched setting exporting as the wrong default, and an icon file escaping the target
 * directory on import.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class SettingsBackupTest {

    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private SettingsSharedPrefsWrapper prefs;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        prefs = new SettingsSharedPrefsWrapper(context);
    }

    @Test
    public void exportCapturesTheRealDefaultForAnUntouchedSetting() throws Exception {
        // SingleSwipesAArea defaults to 60 (SettingsValuesBase#loadSingleSwipesAArea), not 0 -
        // an exporter using the wrong fallback would silently zero out the trigger strip width
        // for anyone who never touched this setting.
        JSONObject json = SettingsBackup.exportSettingsToJson(prefs);
        assertEquals(60, json.getInt("SingleSwipesAArea"));
        assertEquals(3, json.getInt("Feedback"));
        assertEquals("0", json.getString("PieColorString"));
    }

    @Test
    public void scalarSettingsRoundTripThroughJson() throws Exception {
        prefs.saveInt("Feedback", 2, null);
        prefs.saveInt("SingleSwipesAArea", 42, null);
        prefs.saveString("PieColorString", "5", null);

        JSONObject json = SettingsBackup.exportSettingsToJson(prefs);

        // Corrupt the live values, then restore from the export.
        prefs.saveInt("Feedback", 0, null);
        prefs.saveInt("SingleSwipesAArea", 0, null);
        prefs.saveString("PieColorString", "0", null);
        SettingsBackup.importSettingsFromJson(prefs, json);

        assertEquals(2, prefs.loadInt("Feedback", -1));
        assertEquals(42, prefs.loadInt("SingleSwipesAArea", -1));
        assertEquals("5", prefs.loadString("PieColorString", "unexpected"));
    }

    @Test
    public void pieGestureAndIsaSlotsRoundTripThroughJson() throws Exception {
        prefs.saveInt("PieItem5 Type", Action.RecentApps, null);
        prefs.saveString("PieItem5 String", "payload", null);
        prefs.saveString("PieItem5 Icon", "base64icon", null);
        prefs.saveInt(TouchServiceResult.names[0] + " Type", Action.Home, null);
        prefs.saveInt("IsaItem3 Type", Action.Back, null);

        JSONObject json = SettingsBackup.exportSettingsToJson(prefs);
        prefs.saveInt("PieItem5 Type", Action.None, null);
        prefs.saveString("PieItem5 String", "", null);
        SettingsBackup.importSettingsFromJson(prefs, json);

        assertEquals(Action.RecentApps, prefs.loadInt("PieItem5 Type", Action.None));
        assertEquals("payload", prefs.loadString("PieItem5 String", ""));
        assertEquals("base64icon", prefs.loadString("PieItem5 Icon", ""));
        assertEquals(
                Action.Home, prefs.loadInt(TouchServiceResult.names[0] + " Type", Action.None));
        assertEquals(Action.Back, prefs.loadInt("IsaItem3 Type", Action.None));
    }

    @Test
    public void aBackupMissingAKeyFallsBackToTheRealDefaultNotZero() throws Exception {
        // Simulates restoring an older/partial backup: a key absent from the JSON
        // must not import as 0 when its real default is something else.
        JSONObject json = new JSONObject();
        SettingsBackup.importSettingsFromJson(prefs, json);
        assertEquals(60, prefs.loadInt("SingleSwipesAArea", -1));
        assertEquals(3, prefs.loadInt("Feedback", -1));
        assertEquals(Action.None, prefs.loadInt("PieItem0 Type", -1));
    }

    @Test
    public void exportZipThenImportZipRestoresSettingsAndIcons() throws Exception {
        File iconDir = tmp.newFolder("icons");
        File zip = new File(tmp.getRoot(), "backup.zip");
        prefs.saveInt("PiePos", 3, null);
        writeFile(new File(iconDir, IconUtils.getNamePie(0)), "fake-png-bytes");

        SettingsBackup.exportZip(
                prefs, IconFolder.forPath(iconDir.getPath()), new FileOutputStream(zip));

        prefs.saveInt("PiePos", 0, null);
        assertTrue(new File(iconDir, IconUtils.getNamePie(0)).delete());

        boolean restored =
                SettingsBackup.importZip(
                        prefs, IconFolder.forPath(iconDir.getPath()), new FileInputStream(zip));

        assertTrue(restored);
        assertEquals(3, prefs.loadInt("PiePos", -1));
        assertTrue(new File(iconDir, IconUtils.getNamePie(0)).exists());
    }

    @Test
    public void importZipRejectsAZipWithNoSettingsEntry() throws Exception {
        File iconDir = tmp.newFolder("icons");
        File zip = new File(tmp.getRoot(), "not-a-backup.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("random.txt"));
            out.write("hello".getBytes("UTF-8"));
            out.closeEntry();
        }

        assertFalse(
                SettingsBackup.importZip(
                        prefs, IconFolder.forPath(iconDir.getPath()), new FileInputStream(zip)));
    }

    @Test
    public void importZipCannotEscapeTheIconDirectory() throws Exception {
        File iconDir = tmp.newFolder("icons");
        File outside = new File(tmp.getRoot(), "evil.png");
        File zip = new File(tmp.getRoot(), "malicious.zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("settings.json"));
            out.write(new JSONObject().toString().getBytes("UTF-8"));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("icons/../evil.png"));
            out.write("payload".getBytes("UTF-8"));
            out.closeEntry();
        }

        SettingsBackup.importZip(
                prefs, IconFolder.forPath(iconDir.getPath()), new FileInputStream(zip));

        assertFalse(outside.exists());
    }

    private static void writeFile(File file, String content) throws Exception {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes("UTF-8"));
        }
    }
}
