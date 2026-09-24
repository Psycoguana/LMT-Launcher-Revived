package com.noname81.lmt;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Import/export of every setting, plus the pie slots' custom icon files, as one zip.
 *
 * <p>Reads and writes go through {@link SettingsSharedPrefsWrapper}'s loadInt/loadString/saveInt/
 * saveString directly - the same layer every setting already uses - so this works unmodified
 * whether the device stores settings in {@code LMT.xml} or, when {@code LMTExternalConfig} is
 * set, through {@code Settings.System} instead (see the wrapper's javadoc). The scalar defaults
 * below are copied from {@link SettingsValuesBase}'s load* methods so a setting the user never
 * touched round-trips to the same effective value instead of an arbitrary placeholder.
 *
 * <p>Icon storage is two mechanisms, and both are covered: a custom gesture/pie/ISA icon chosen
 * from the icon picker is a base64 PNG embedded directly under that slot's "Icon" key, which
 * {@link #exportSettingsToJson} already picks up as an ordinary string; a custom pie-ring icon
 * dropped into the resource search path as {@code pieN.png} ({@link IconUtils#getNamePie}) is a
 * file, so {@link #exportZip}/{@link #importZip} copy those into/out of the zip's {@code icons/}
 * entry explicitly.
 */
class SettingsBackup {
    private static final String SETTINGS_ENTRY = "settings.json";
    private static final String ICON_ENTRY_PREFIX = "icons/";
    private static final int PIE_SLOTS = 38;
    private static final int ISA_SLOTS = 12;

    /** {key, default} pairs, copied verbatim from SettingsValuesBase's load*() methods. */
    private static final String[][] INT_DEFAULTS = {
        {"Feedback", "3"},
        {"Vibration", "30"},
        {"PieVibration", "30"},
        {"Input", "4"},
        {"STSupport", "1"},
        {"SingleSwipesBBox", "1"},
        {"SingleSwipesAArea", "60"},
        {"TouchscreenScreenFactorX", "100"},
        {"TouchscreenScreenFactorY", "100"},
        {"MinScore", "70"},
        {"MinPathLength", "7"},
        {"Autostart", "1"},
        {"TSMode", "2"},
        {"PiePos", "7"},
        {"PieAreaX", "50"},
        // getIsSmallScreen() is hardcoded to return false, so the small-screen
        // branch of these four defaults is unreachable; the large-screen value
        // is the only one that can ever apply.
        {"PieAreaY", "600"},
        {"PieAreaGravity", "0"},
        {"PieFont", "4"},
        {"PieInnerRadius", "60"},
        {"PieOuterRadius", "80"},
        {"PieShiftSize", "0"},
        {"PieOutlineSize", "3"},
        {"PieSliceGap", "0"},
        {"PieStartGap", "0"},
        {"PieRotateImages", "1"},
        {"PieLongpress", "500"},
        {"PieAnimation", "80"},
        {"PieVibrate", "1"},
        {"PieMultiCommand", "0"},
        {"PiePointerFromEdges", "0"},
        {"PiePointerWarpFactorPercent", "300"},
        {"PieShowStatusInfos", "0"},
        {"PieShowAppImages", "1"},
        {"PieUserImageScaling", "0"},
        {"ShowSystemApps", "0"},
        {"NavButtonStyle", "0"},
        {"PieExpandTriggerArea", "1"},
        {"PieBehindKeyboard", "0"},
        {"PieOnLockScreen", "0"},
        {"ThemeMode", String.valueOf(ThemeHelper.MODE_SYSTEM)},
        {"ThemeFollowSystemColor", "0"},
        {"ThemeColor", String.valueOf(ThemeHelper.COLOR_DEFAULT)},
        {"ThemeCustomColor", String.valueOf(ThemeHelper.DEFAULT_CUSTOM_COLOR)},
    };

    private static final String[][] STRING_DEFAULTS = {
        {"Blacklist", ""},
        {"BlacklistPie", ""},
        {"PieColorString", "0"},
        {"PieStatusInfoColorString", "0"},
        {"PiePointerColorString", "0"},
        {"ResourceSearchPath", ""},
    };

    private SettingsBackup() {}

    static JSONObject exportSettingsToJson(SettingsSharedPrefsWrapper prefs) throws JSONException {
        JSONObject json = new JSONObject();
        for (String[] entry : INT_DEFAULTS) {
            json.put(entry[0], prefs.loadInt(entry[0], Integer.parseInt(entry[1])));
        }
        for (String[] entry : STRING_DEFAULTS) {
            json.put(entry[0], prefs.loadString(entry[0], entry[1]));
        }
        for (int slot = 0; slot < TouchServiceResult.names.length; slot++) {
            exportSlot(json, prefs, TouchServiceResult.names[slot]);
        }
        for (int slot = 0; slot < PIE_SLOTS; slot++) {
            exportSlot(json, prefs, "PieItem" + slot);
        }
        for (int slot = 0; slot < ISA_SLOTS; slot++) {
            exportSlot(json, prefs, "IsaItem" + slot);
        }
        return json;
    }

    private static void exportSlot(JSONObject json, SettingsSharedPrefsWrapper prefs, String prefix)
            throws JSONException {
        json.put(prefix + " Type", prefs.loadInt(prefix + " Type", Action.None));
        json.put(prefix + " String", prefs.loadString(prefix + " String", ""));
        json.put(prefix + " Icon", prefs.loadString(prefix + " Icon", ""));
    }

    static void importSettingsFromJson(SettingsSharedPrefsWrapper prefs, JSONObject json) {
        SharedPreferences.Editor editor = prefs.createAndReturnSharedPreferencesEditor();
        for (String[] entry : INT_DEFAULTS) {
            prefs.saveInt(entry[0], json.optInt(entry[0], Integer.parseInt(entry[1])), editor);
        }
        for (String[] entry : STRING_DEFAULTS) {
            prefs.saveString(entry[0], json.optString(entry[0], entry[1]), editor);
        }
        for (int slot = 0; slot < TouchServiceResult.names.length; slot++) {
            importSlot(json, prefs, TouchServiceResult.names[slot], editor);
        }
        for (int slot = 0; slot < PIE_SLOTS; slot++) {
            importSlot(json, prefs, "PieItem" + slot, editor);
        }
        for (int slot = 0; slot < ISA_SLOTS; slot++) {
            importSlot(json, prefs, "IsaItem" + slot, editor);
        }
        if (editor != null) {
            editor.apply();
        }
    }

    private static void importSlot(
            JSONObject json,
            SettingsSharedPrefsWrapper prefs,
            String prefix,
            SharedPreferences.Editor editor) {
        prefs.saveInt(prefix + " Type", json.optInt(prefix + " Type", Action.None), editor);
        prefs.saveString(prefix + " String", json.optString(prefix + " String", ""), editor);
        prefs.saveString(prefix + " Icon", json.optString(prefix + " Icon", ""), editor);
    }

    /**
     * Writes every setting plus any existing pieN.png files from {@code iconDirectory} to {@code
     * destStream}, which is closed on return whether or not the write succeeds.
     */
    static void exportZip(
            SettingsSharedPrefsWrapper prefs, String iconDirectory, OutputStream destStream)
            throws IOException, JSONException {
        JSONObject json = exportSettingsToJson(prefs);
        try (ZipOutputStream zip = new ZipOutputStream(destStream)) {
            zip.putNextEntry(new ZipEntry(SETTINGS_ENTRY));
            zip.write(json.toString().getBytes("UTF-8"));
            zip.closeEntry();
            for (int slot = 0; slot < PIE_SLOTS; slot++) {
                File icon = new File(iconDirectory, IconUtils.getNamePie(slot));
                if (icon.exists()) {
                    zip.putNextEntry(new ZipEntry(ICON_ENTRY_PREFIX + icon.getName()));
                    copyIntoOpenStream(new FileInputStream(icon), zip);
                    zip.closeEntry();
                }
            }
        }
    }

    /**
     * Restores every setting plus any pieN.png icons from {@code srcStream}, written into {@code
     * iconDirectory}. {@code srcStream} is closed on return whether or not the read succeeds.
     *
     * @return false if {@code srcStream} has no settings.json entry, i.e. it is not one of ours
     */
    static boolean importZip(
            SettingsSharedPrefsWrapper prefs, String iconDirectory, InputStream srcStream)
            throws IOException, JSONException {
        FileUtils.createFolder(iconDirectory);
        JSONObject json = null;
        try (ZipInputStream zip = new ZipInputStream(srcStream)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (SETTINGS_ENTRY.equals(entry.getName())) {
                    json = new JSONObject(readAll(zip));
                } else if (!entry.isDirectory() && entry.getName().startsWith(ICON_ENTRY_PREFIX)) {
                    File target = new File(iconDirectory, entry.getName().substring(ICON_ENTRY_PREFIX.length()));
                    if (isWithinDirectory(target, iconDirectory)) {
                        copyIntoNewFile(zip, target);
                    }
                }
                zip.closeEntry();
            }
        }
        if (json == null) {
            return false;
        }
        importSettingsFromJson(prefs, json);
        return true;
    }

    // Guards against a zip entry named e.g. "icons/../../../etc/hosts" writing
    // outside iconDirectory (zip-slip).
    private static boolean isWithinDirectory(File file, String directory) throws IOException {
        String canonicalDir = new File(directory).getCanonicalPath() + File.separator;
        return file.getCanonicalPath().startsWith(canonicalDir);
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toString("UTF-8");
    }

    // Copies a whole file into an already-open stream (a ZipOutputStream entry), closing
    // only the file side - the zip stream stays open for the entries that follow.
    private static void copyIntoOpenStream(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            in.close();
        }
    }

    // Copies an already-open stream (a ZipInputStream entry) into a new file, closing only
    // the file side - the zip stream stays open for the entries that follow.
    private static void copyIntoNewFile(InputStream in, File target) throws IOException {
        try (FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
}
