package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import java.lang.reflect.Field;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * The stored-slot schema: how many action slots exist for each trigger system, and
 * which preference keys they map to.
 *
 * <p>This is the most fragile thing in the codebase to refactor. Slots are addressed
 * by bare integer index — {@code PieItem<i> Type}, and {@code PieControl} walks them
 * in hardcoded ranges (ring 1 = 0..9, ring 2 = 10..23, ring 3 = 24..37, stepping by
 * two because each position stores a click action and a long-press action). Nothing
 * in the code relates those ranges to the total, so an off-by-one would not fail to
 * compile or throw — it would quietly bind the wrong action to the wrong slice.
 *
 * <p>It is also where the third-ring patch lives: the pie count went from 24 to 38.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class SettingsSlotSchemaTest {

    private SettingsValues settings;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        // A fresh singleton: tests elsewhere may have cached slots in the static instance.
        UiTestSupport.resetSingletons(context);
        settings = SettingsValues.getInstance(context);
    }

    @SuppressWarnings("unchecked")
    private Vector<Action> vector(String field) throws Exception {
        Field f = SettingsValuesBase.class.getDeclaredField(field);
        f.setAccessible(true);
        return (Vector<Action>) f.get(settings);
    }

    @Test
    public void pieHasThirtyEightSlots() throws Exception {
        // 5 + 7 + 7 positions across three rings, each with a click and a long-press
        // action. Ring 3 is the patched addition; before it this was 24.
        assertEquals(38, vector("mPieActions").size());
    }

    @Test
    public void pieSlotCountCoversEveryRingRange() throws Exception {
        int slots = vector("mPieActions").size();
        // PieControl walks 0..9, 10..23 and 24..37; the last index touched must exist.
        assertEquals("ring 1 occupies slots 0-9", true, slots > 9);
        assertEquals("ring 2 occupies slots 10-23", true, slots > 23);
        assertEquals("ring 3 occupies slots 24-37", true, slots > 37 - 1);
        assertEquals("and nothing beyond ring 3", 38, slots);
    }

    @Test
    public void oneGestureSlotPerRecognisedGesture() throws Exception {
        assertEquals(TouchServiceResult.names.length, vector("mGestureActions").size());
    }

    @Test
    public void twelveIsaSlots() throws Exception {
        // Four screen edges x three zones.
        assertEquals(12, vector("mIsaActions").size());
    }

    @Test
    public void everySlotLoadsAnActionRatherThanNull() throws Exception {
        // getPieAction is called straight from the pie's draw path; a null here would
        // be a crash while the overlay is on screen.
        for (int i = 0; i < 38; i++) {
            assertNotNull("pie slot " + i, settings.getPieAction(i));
        }
        for (int i = 0; i < TouchServiceResult.names.length; i++) {
            assertNotNull("gesture slot " + i, settings.getGestureAction(i));
        }
        for (int i = 0; i < 12; i++) {
            assertNotNull("isa slot " + i, settings.getIsaAction(i));
        }
    }

    @Test
    public void unsetSlotsDefaultToNone() throws Exception {
        // A fresh install has no stored slots; they must come back as the no-op
        // action rather than as type 0, which is the OffsetNormalCommands sentinel.
        assertEquals(Action.None, settings.getPieAction(0).getType());
    }

    @Test
    public void anOutOfRangeSlotIsHandledNotThrown() {
        // getPieAction guards on size; callers rely on that rather than checking.
        assertNotNull(settings.getPieAction(9999));
    }
}
