package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.slider.Slider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * UI safety net for the Settings tab: what each row, found by its label, changes.
 *
 * <p>Each row is driven generically: click it (via {@link Preference#performClick()}), answer
 * whatever dialog appears (a number, the second choice, or a colour), then diff the stored settings
 * and note any activity or service it started. The resulting "label -&gt; effect" table is the
 * contract the Settings screen has to meet, independent of the widget backing a row (ROADMAP 1.6
 * phase 2 rebuilt it as a {@code PreferenceFragmentCompat}; this table did not change).
 */
@RunWith(RobolectricTestRunner.class)
@Config(
        sdk = 33,
        shadows = ShadowTouchServiceNative.class,
        instrumentedPackages = "com.noname81.lmt")
public class SettingsScreenUiTest {

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        UiTestSupport.resetSingletons(context);
    }

    @After
    public void tearDown() throws Exception {
        // SettingsValues caches slots in a static; later test classes must not inherit ours.
        UiTestSupport.resetSingletons(context);
    }

    /**
     * Row label -&gt; what it changes: the settings keys it writes (by dialog, choice or switch -
     * the widget is deliberately not part of the contract) plus anything it starts.
     */
    private static final String[][] EXPECTED = {
        {"General / Activate LMT", "service:TouchService"},
        {"General / Autostart LMT", "Autostart"},
        {"General / Set active features", "TSMode"},
        {"General / Appearance", "activity:AppearanceActivity"},
        // Reads the touchscreen through root `getevent`; without root it offers -1 values and
        // saves nothing. (It is also buggy with root - see docs/ROADMAP.md 3.7.)
        {"Gesture recognition / Auto configuration", ""},
        {"Gesture recognition / Set gesture input", "Input"},
        {"Gesture recognition / Touchscreen to screen factor X", "TouchscreenScreenFactorX"},
        {"Gesture recognition / Touchscreen to screen factor Y", "TouchscreenScreenFactorY"},
        {"Gestures / Feedback style", "Feedback"},
        {"Gestures / Vibration Time", "Vibration"},
        {"Gestures / Single touch gestures", "STSupport"},
        {"Gestures / Min gesture score", "MinScore"},
        {"Gestures / Min gesture path length", "MinPathLength"},
        {"Gestures / Add to gesture blacklist", "activity:MultiSelectActivity"},
        {"Gestures / Clear gesture blacklist", "Blacklist"},
        {"Invisible swipe areas / Min bounding box size", "SingleSwipesBBox"},
        {"Invisible swipe areas / Activation area thickness", "SingleSwipesAArea"},
        {"Pie activation / Activation area position", "PiePos"},
        {"Pie activation / Activation area thickness", "PieAreaX"},
        {"Pie activation / Activation area length", "PieAreaY"},
        {"Pie activation / Activation area gravity", "PieAreaGravity"},
        {"Pie activation / Activation area behind keyboard", "PieBehindKeyboard"},
        {"Pie activation / Add to pie blacklist", "activity:MultiSelectActivity"},
        {"Pie activation / Clear pie blacklist", "BlacklistPie"},
        {"Pie style / Pie colors", "PieColorString"},
        {"Pie style / Pie inner radius", "PieInnerRadius"},
        {"Pie style / Pie outer radius", "PieOuterRadius"},
        {"Pie style / Pie shift size", "PieShiftSize"},
        {"Pie style / Pie outline size", "PieOutlineSize"},
        {"Pie style / Pie slice gap", "PieSliceGap"},
        {"Pie style / Pie start gap", "PieStartGap"},
        {"Pie style / Blur", "PieBlur"},
        {"Pie style / Blur amount", "PieBlurRadius"},
        // Greyed out except on Samsungs with live window blur (not Robolectric).
        {"Pie style / Samsung Live Blur", ""},
        {"Pie behavior / Longpress time", "PieLongpress"},
        {"Pie behavior / Animation time", "PieAnimation"},
        {"Pie behavior / Feedback type", "PieVibrate"},
        {"Pie behavior / Vibration Time", "PieVibration"},
        {"Pie behavior / Multi command", "PieMultiCommand"},
        {"Pie behavior / Expand trigger area", "PieExpandTriggerArea"},
        {"Pie icons / Rotate images", "PieRotateImages"},
        {"Pie icons / Set nav button style", "NavButtonStyle"},
        {"Pie icons / Show and scale app images", "PieShowAppImages"},
        {"Pie icons / Show and scale user images", "PieUserImageScaling"},
        // Picks the folder with the system file picker (ROADMAP 1.13).
        {
            "Pie icons / Set user image search path",
            "activity:android.intent.action.OPEN_DOCUMENT_TREE"
        },
        {"Pie extensions / Pie pointer edge activation", "PiePointerFromEdges"},
        {"Pie extensions / Pie pointer warp factor", "PiePointerWarpFactorPercent"},
        {"Pie extensions / Pie pointer color", "PiePointerColorString"},
        {"Pie extensions / Show status infos", "PieShowStatusInfos"},
        {"Pie extensions / Pie status info colors", "PieStatusInfoColorString"},
        {"Pie extensions / Pie status info font", "PieFont"},
        {"Backup / Export settings", "activity:android.intent.action.CREATE_DOCUMENT"},
        {"Backup / Import settings", "activity:android.intent.action.OPEN_DOCUMENT"},
    };

    @Test
    public void everyRowChangesTheSettingItIsLabelledFor() throws Exception {
        Map<String, String> raw = new LinkedHashMap<>();
        Map<String, String> actual = new LinkedHashMap<>();
        for (UiTestSupport.PreferenceRow label : freshScreen().rows) {
            // Fresh store and screen per row: some rows recreate the activity or change the
            // feature mode, and none may leak into the next.
            Screen screen = freshScreen();
            UiTestSupport.PreferenceRow row = screen.find(label);
            String effect = effectOf(screen.host, row.preference);
            raw.put(row.section + " / " + row.title, effect);
            actual.put(row.section + " / " + row.title, normalise(effect));
        }
        Map<String, String> expected = new LinkedHashMap<>();
        for (String[] e : EXPECTED) {
            expected.put(e[0], e[1]);
        }
        StringBuilder observed = new StringBuilder("observed (raw):\n");
        for (Map.Entry<String, String> e : raw.entrySet()) {
            observed.append("  ")
                    .append(e.getKey())
                    .append(" -> ")
                    .append(e.getValue())
                    .append('\n');
        }
        assertEquals(observed.toString(), expected, actual);
    }

    /** Keeps what a row changes, drops how (dialog kind, switch vs tap) and duplicates. */
    private static String normalise(String effect) {
        TreeSet<String> parts = new TreeSet<>();
        for (String token : effect.split(" ")) {
            if (token.startsWith("tap:") || token.startsWith("switch:")) {
                parts.addAll(
                        java.util.Arrays.asList(
                                token.substring(token.indexOf(':') + 1).split(",")));
            } else if (token.startsWith("activity:")
                    || token.startsWith("service:")
                    || token.startsWith("stopped:")) {
                parts.add(token);
            }
        }
        return String.join(" ", parts);
    }

    private static final class Screen {
        FragmentActivity host;
        java.util.List<UiTestSupport.PreferenceRow> rows;

        UiTestSupport.PreferenceRow find(UiTestSupport.PreferenceRow label) {
            for (UiTestSupport.PreferenceRow row : rows) {
                if (row.section.equals(label.section) && row.title.equals(label.title)) {
                    return row;
                }
            }
            throw new AssertionError("row disappeared: " + label);
        }
    }

    /** TSMode 1 = gestures, ISAs and pie, so every section is shown (0 omits the pie). */
    private Screen freshScreen() throws Exception {
        UiTestSupport.resetSingletons(context);
        SettingsValues.getInstance(context).saveTouchServiceMode(1);
        // One excluded app each, so the clear rows have something to clear (with nothing, they
        // only say so).
        SettingsValues.getInstance(context).setBlacklisted("com.example.gesture");
        SettingsValues.getInstance(context).setBlacklistedPie("com.example.pie");
        Screen screen = new Screen();
        screen.host = UiTestSupport.hostActivity();
        SettingsViewFragment fragment = new SettingsViewFragment();
        UiTestSupport.showPreferences(screen.host, fragment);
        screen.rows = UiTestSupport.preferenceRows(fragment);
        return screen;
    }

    private String effectOf(Activity host, Preference preference) {
        Application app = (Application) context;
        drainStarted(app, host);
        Map<String, ?> before = new HashMap<String, Object>(prefs().getAll());
        Object dialogBefore = ShadowAlertDialog.getLatestDialog();

        preference.performClick();
        UiTestSupport.idle();

        StringBuilder effect = new StringBuilder();
        Object dialog = ShadowAlertDialog.getLatestDialog();
        if (dialog != dialogBefore && dialog instanceof AlertDialog) {
            effect.append(answer((AlertDialog) dialog));
        }
        effect.append(changedKeys(before, "tap"));
        effect.append(started(app, host));
        return effect.length() == 0 ? "nothing" : effect.toString().trim();
    }

    /**
     * Answers a dialog the way a user would, and says what kind it was. A choice can open a second
     * dialog ("Custom..." leads to a slider or the color editor), which is answered too.
     */
    private static String answer(AlertDialog dialog) {
        StringBuilder kinds = new StringBuilder();
        for (int round = 0; round < 3 && dialog != null; round++) {
            kinds.append(answerOne(dialog));
            Object latest = ShadowAlertDialog.getLatestDialog();
            dialog =
                    latest != dialog
                                    && latest instanceof AlertDialog
                                    && ((AlertDialog) latest).isShowing()
                            ? (AlertDialog) latest
                            : null;
        }
        return kinds.toString();
    }

    private static String answerOne(AlertDialog dialog) {
        if (dialog.getListView() != null && dialog.getListView().getCount() > 1) {
            // Pick an option other than the current one, or nothing would change.
            int count = dialog.getListView().getCount();
            int pick = (dialog.getListView().getCheckedItemPosition() + 1) % count;
            dialog.getListView().performItemClick(null, pick, pick);
            UiTestSupport.idle();
            return "[choice] ";
        }
        Slider slider = UiTestSupport.sliderIn(dialog);
        if (slider != null) {
            // Move it off the current value, or nothing would change.
            float current = slider.getValue();
            slider.setValue(
                    current != slider.getValueFrom() ? slider.getValueFrom() : slider.getValueTo());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            UiTestSupport.idle();
            return "[slider] ";
        }
        EditText input = UiTestSupport.editTextIn(dialog);
        if (input != null) {
            boolean colour = input.getId() == R.id.color_string;
            input.setText(colour ? "#FF102030" : "37");
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            UiTestSupport.idle();
            return colour ? "[colour] " : "[value] ";
        }
        // A plain confirmation: accept it.
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            UiTestSupport.idle();
        }
        return "[confirm] ";
    }

    private String changedKeys(Map<String, ?> before, String how) {
        TreeSet<String> changed = new TreeSet<>();
        Map<String, ?> after = prefs().getAll();
        for (String key : after.keySet()) {
            Object was = before.get(key);
            if (was == null ? after.get(key) != null : !was.equals(after.get(key))) {
                changed.add(key);
            }
        }
        for (String key : before.keySet()) {
            if (!after.containsKey(key)) {
                changed.add(key);
            }
        }
        return changed.isEmpty() ? "" : how + ":" + String.join(",", changed) + " ";
    }

    private static String started(Application app, Activity host) {
        StringBuilder s = new StringBuilder();
        for (Intent i = shadowOf(host).getNextStartedActivity();
                i != null;
                i = shadowOf(host).getNextStartedActivity()) {
            s.append("activity:").append(describe(i)).append(' ');
        }
        for (Intent i = shadowOf(app).getNextStartedActivity();
                i != null;
                i = shadowOf(app).getNextStartedActivity()) {
            s.append("activity:").append(describe(i)).append(' ');
        }
        for (Intent i = shadowOf(app).getNextStartedService();
                i != null;
                i = shadowOf(app).getNextStartedService()) {
            s.append("service:").append(describe(i)).append(' ');
        }
        for (Intent i = shadowOf(app).getNextStoppedService();
                i != null;
                i = shadowOf(app).getNextStoppedService()) {
            s.append("stopped:").append(describe(i)).append(' ');
        }
        return s.toString();
    }

    private static void drainStarted(Application app, Activity host) {
        started(app, host);
    }

    private static String describe(Intent i) {
        if (i.getComponent() != null) {
            return i.getComponent().getShortClassName().replaceFirst("^\\.", "");
        }
        return String.valueOf(i.getAction());
    }

    private android.content.SharedPreferences prefs() {
        return context.getSharedPreferences("LMT", 0);
    }
}
