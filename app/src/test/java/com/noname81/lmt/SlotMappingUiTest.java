package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.view.View;
import android.widget.ListView;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pre-UI-rewrite safety net: every Pie / Gesture / ISA row must edit the slot its label names.
 *
 * <p>Today each screen turns a list position into a slot index by skipping section headers with
 * hand-written arithmetic (PieFragment: {@code position--} three times at 0/10/24), and a second,
 * separate offset table picks which {@code pieN.png} a row's icon button edits. Both break
 * silently when rows move. The expectations here come from the visible labels and from how the
 * engine consumes each slot ({@link PieControl} ring layout, {@link TouchServiceResult#names},
 * {@link SettingsValues#getIsaAction} zone geometry) - never from the fragments' own arithmetic.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class SlotMappingUiTest {

    // PieControl.populateMenu(): ring 1 walks slots 0..9, ring 2 10..23, ring 3 24..37;
    // each item is an even (tap) slot followed by its odd (long-press) slot.
    private static final int[] RING_START = {0, 10, 24};
    private static final Pattern PIE_LEVEL = Pattern.compile("Level (\\d) for the pie");
    private static final Pattern PIE_ITEM = Pattern.compile("Pie item (\\d)( longpress)?");

    private Context context;
    private FragmentActivity host;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        UiTestSupport.resetSingletons(context);
        host = UiTestSupport.hostActivity();
    }

    @After
    public void tearDown() throws Exception {
        // SettingsValues caches slots in a static; later test classes must not inherit ours.
        UiTestSupport.resetSingletons(context);
    }

    @Test
    public void everyPieRowEditsTheSlotItsLabelNames() throws Exception {
        PieFragment fragment = new PieFragment();
        ListView list = UiTestSupport.show(host, fragment);
        TreeSet<Integer> seen = new TreeSet<>();
        for (UiTestSupport.Row row : UiTestSupport.rows(list)) {
            int expected = pieSlotFromLabel(row);
            UiTestSupport.click(list, row);
            int actual =
                    UiTestSupport.privateInt(
                            SettingsValues.getInstance(context),
                            SettingsValuesBase.class,
                            "mCurrentPie");
            assertEquals(row.toString(), expected, actual);
            seen.add(actual);
        }
        assertEquals("every one of the 38 pie slots is reachable exactly once", 38, seen.size());
        assertEquals(0, (int) seen.first());
        assertEquals(37, (int) seen.last());
    }

    @Test
    public void everyPieRowsIconButtonEditsThatSlotsIconFile() throws Exception {
        PieFragment fragment = new PieFragment();
        ListView list = UiTestSupport.show(host, fragment);
        for (UiTestSupport.Row row : UiTestSupport.rows(list)) {
            View rowView = UiTestSupport.rowView(list, row);
            View iconButton = rowView.findViewById(R.id.listitem_icondescriptionicon_icon2);
            iconButton.performClick();
            UiTestSupport.idle();
            int actual = UiTestSupport.privateInt(fragment, PieFragment.class, "mCurrentPos");
            // The file PieControl loads for slot N is IconUtils.getNamePie(N).
            assertEquals(row.toString(), pieSlotFromLabel(row), actual);
        }
    }

    @Test
    public void everyGestureRowEditsTheGestureItIsNamedAfter() throws Exception {
        GesturesFragment fragment = new GesturesFragment();
        UiTestSupport.showRecyclerView(host, fragment);
        List<UiTestSupport.SectionedRow> rows = UiTestSupport.sectionedRows(fragment.rowsForTest());
        assertEquals(TouchServiceResult.names.length, rows.size());
        for (UiTestSupport.SectionedRow row : rows) {
            fragment.clickRowForTest(row.position);
            int actual =
                    UiTestSupport.privateInt(
                            SettingsValues.getInstance(context),
                            SettingsValuesBase.class,
                            "mCurrentGesture");
            // The recognizer reports a gesture as an index into names[], and the action is
            // stored under that name - so the row labelled names[i] must edit slot i.
            assertEquals(row.toString(), row.title, TouchServiceResult.names[actual]);
        }
    }

    @Test
    public void everyIsaRowEditsTheZoneItsLabelAndCaptionName() throws Exception {
        IsasFragment fragment = new IsasFragment();
        UiTestSupport.showRecyclerView(host, fragment);
        List<UiTestSupport.SectionedRow> rows = UiTestSupport.sectionedRows(fragment.rowsForTest());
        assertEquals(12, rows.size());
        // SettingsValues.getIsaAction: slot/3 picks the edge (bottom, top, left, right) and
        // slot%3 the third along it - left..right for horizontal edges, top..bottom for vertical.
        String[] edge = {"bottom", "top", "left", "right"};
        String[][] third = {
            {"left", "center", "right"}, {"left", "center", "right"},
            {"top", "center", "bottom"}, {"top", "center", "bottom"}
        };
        for (UiTestSupport.SectionedRow row : rows) {
            fragment.clickRowForTest(row.position);
            int slot =
                    UiTestSupport.privateInt(
                            SettingsValues.getInstance(context),
                            SettingsValuesBase.class,
                            "mCurrentIsa");
            assertEquals(row.toString(), "Isa " + (slot + 1), row.title);
            String zone = edge[slot / 3] + " " + third[slot / 3][slot % 3];
            assertTrue(
                    row + ": caption '" + row.caption + "' should name zone '" + zone + "'",
                    row.caption.toLowerCase().endsWith(zone));
        }
    }

    private static int pieSlotFromLabel(UiTestSupport.Row row) {
        Matcher level = PIE_LEVEL.matcher(row.section);
        Matcher item = PIE_ITEM.matcher(row.title);
        if (!level.matches() || !item.matches()) {
            fail("unrecognised pie row label: " + row);
        }
        int ring = Integer.parseInt(level.group(1)) - 1;
        int index = Integer.parseInt(item.group(1)) - 1;
        return RING_START[ring] + 2 * index + (item.group(2) != null ? 1 : 0);
    }
}
