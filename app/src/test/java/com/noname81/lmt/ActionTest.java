package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Invariants of the action catalogue.
 *
 * <p>{@code Action} is the extension point of the whole app: the pie, gestures and
 * ISAs all read from {@link Action#names} / {@link Action#captions}, and
 * {@code CommandSelectActivity} groups the picker using the {@code Offset*}
 * sentinels. Adding an action means touching several parallel structures at once,
 * and nothing in the code enforces that they stay in step. These tests do.
 */
public class ActionTest {

    /** The id constants, keyed by name, discovered reflectively. */
    private static Map<String, Integer> idConstants() {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Field f : Action.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != int.class) {
                continue;
            }
            f.setAccessible(true);
            try {
                out.put(f.getName(), f.getInt(null));
            } catch (IllegalAccessException e) {
                fail("could not read " + f.getName() + ": " + e);
            }
        }
        return out;
    }

    @Test
    public void namesAndCaptionsStayInStep() {
        assertEquals(
                "names[] and captions[] must have one entry per action",
                Action.names.length,
                Action.captions.length);
    }

    @Test
    public void everyIdConstantMatchesItsSlotInNames() {
        // The id IS the index into names[]; Launcher.fireAction switches on it.
        // A mismatch would silently fire the wrong action.
        for (Map.Entry<String, Integer> e : idConstants().entrySet()) {
            String constant = e.getKey();
            int id = e.getValue();
            assertTrue(
                    constant + " = " + id + " is outside names[] (length "
                            + Action.names.length + ")",
                    id >= 0 && id < Action.names.length);
        }
    }

    @Test
    public void sentinelsAreOrderedAndGroupThePicker() {
        // CommandSelectActivity slices names[] between these to build its sections,
        // so they must stay strictly ascending or sections silently overlap/vanish.
        int[] sentinels = {
            Action.OffsetNormalCommands,
            Action.OffsetToggleCommands,
            Action.OffsetAdvancedCommands,
            Action.OffsetAppDrawer,
            Action.OffsetPieActions,
        };
        for (int i = 1; i < sentinels.length; i++) {
            assertTrue(
                    "Offset sentinels must be strictly ascending, but "
                            + sentinels[i - 1] + " >= " + sentinels[i],
                    sentinels[i - 1] < sentinels[i]);
        }
        for (int s : sentinels) {
            assertTrue(
                    "sentinel " + s + " must name an Offset* entry in names[], found "
                            + Action.names[s],
                    Action.names[s].startsWith("Offset"));
        }
    }

    @Test
    public void nonSentinelNamesAreNotOffsets() {
        // Anything that is not a sentinel must be a real, selectable action.
        for (Map.Entry<String, Integer> e : idConstants().entrySet()) {
            if (e.getKey().startsWith("Offset")) {
                continue;
            }
            assertTrue(
                    e.getKey() + " points at sentinel slot " + e.getValue(),
                    !Action.names[e.getValue()].startsWith("Offset"));
        }
    }

    @Test
    public void toStringReportsTypeAndPayload() {
        Action a = new Action(Action.Back, "payload");
        assertEquals("type: Back, string: payload", a.toString());
    }

    @Test
    public void toStringFallsBackToNoneForAnEmptyPayload() {
        assertEquals("type: Home, string: none", new Action(Action.Home).toString());
        assertEquals("type: Home, string: none", new Action(Action.Home, "").toString());
    }

    @Test
    public void toStringSurvivesATypeEqualToTheArrayLength() {
        // Regression test. The original bound was `mType <= names.length`, which
        // indexes one past the end - reachable from a corrupted or hand-edited
        // prefs file, where it threw ArrayIndexOutOfBoundsException.
        Action a = new Action(Action.names.length, "x");
        assertEquals("type: none, string: x", a.toString());
    }

    @Test
    public void toStringHandlesOutOfRangeTypes() {
        assertEquals("type: none, string: none", new Action(-1).toString());
        assertEquals("type: none, string: none", new Action(9999).toString());
        // 0 is the first Offset sentinel, not a real action
        assertEquals("type: none, string: none", new Action(0).toString());
    }
}
