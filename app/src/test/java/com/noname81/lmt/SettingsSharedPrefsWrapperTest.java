package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Behaviour of the persistence layer every setting goes through.
 *
 * <p>This is the layer import/export (ROADMAP 1.5) will be built on, so its
 * contract is worth pinning down before anything depends on it — in particular the
 * batched-editor path, where a value is invisible until the editor is applied.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class SettingsSharedPrefsWrapperTest {

    private SettingsSharedPrefsWrapper prefs;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        prefs = new SettingsSharedPrefsWrapper(context);
    }

    @Test
    public void stringsRoundTrip() {
        prefs.saveString("PieItem0 String", "com.example.app", null);
        assertEquals("com.example.app", prefs.loadString("PieItem0 String", "fallback"));
    }

    @Test
    public void intsRoundTrip() {
        prefs.saveInt("PieItem0 Type", Action.Back, null);
        assertEquals(Action.Back, prefs.loadInt("PieItem0 Type", Action.None));
    }

    @Test
    public void missingKeysReturnTheSuppliedDefault() {
        assertEquals("default", prefs.loadString("NeverWritten", "default"));
        assertEquals(42, prefs.loadInt("NeverWritten", 42));
    }

    @Test
    public void emptyStringIsPreservedAndNotTreatedAsMissing() {
        // Slots store "" for "no payload"; it must not collapse into the default,
        // or clearing a slot's payload would silently restore the old value.
        prefs.saveString("PieItem1 String", "", null);
        assertEquals("", prefs.loadString("PieItem1 String", "unexpected-default"));
    }

    @Test
    public void aBatchedWriteIsInvisibleUntilTheEditorIsApplied() {
        SharedPreferences.Editor editor = prefs.createAndReturnSharedPreferencesEditor();
        assertNotNull("editor is null only when the Settings.System backend is active",
                editor);

        prefs.saveString("Batched", "written", editor);
        prefs.saveInt("BatchedInt", 7, editor);

        // Not applied yet - nothing is readable.
        assertEquals("not-yet", prefs.loadString("Batched", "not-yet"));
        assertEquals(-1, prefs.loadInt("BatchedInt", -1));

        editor.apply();

        assertEquals("written", prefs.loadString("Batched", "not-yet"));
        assertEquals(7, prefs.loadInt("BatchedInt", -1));
    }

    @Test
    public void writingTheSameKeyTwiceKeepsTheLastValue() {
        prefs.saveInt("PieItem2 Type", Action.Home, null);
        prefs.saveInt("PieItem2 Type", Action.Screenshot, null);
        assertEquals(Action.Screenshot, prefs.loadInt("PieItem2 Type", Action.None));
    }

    @Test
    public void slotKeysFollowTheDocumentedSchema() {
        // loadPieAction(i) reads "PieItem<i> Type|String|Icon"; an exporter has to
        // reproduce these names exactly, so lock the shape in.
        int slot = 5;
        prefs.saveInt("PieItem" + slot + " Type", Action.RecentApps, null);
        prefs.saveString("PieItem" + slot + " String", "payload", null);
        prefs.saveString("PieItem" + slot + " Icon", "icon.png", null);

        assertEquals(Action.RecentApps, prefs.loadInt("PieItem5 Type", Action.None));
        assertEquals("payload", prefs.loadString("PieItem5 String", ""));
        assertEquals("icon.png", prefs.loadString("PieItem5 Icon", ""));
    }
}
