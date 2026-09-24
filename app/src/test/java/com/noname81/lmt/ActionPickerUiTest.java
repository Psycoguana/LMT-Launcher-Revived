package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pre-UI-rewrite safety net for the action picker: the row labelled {@code Action.names[t]}
 * must assign action t, or route to the right payload input.
 *
 * <p>Today the list position <em>is</em> the action id (headers happen to sit exactly on the
 * {@code Offset*} ids), so a reordered or regrouped list would bind the wrong action with no
 * error. This test knows nothing about positions: it finds each row by label.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ActionPickerUiTest {

    /** Actions whose payload is typed into a text dialog. TaskerTask falls back to one when
     * Tasker isn't installed, as it isn't here. */
    private static final Set<Integer> TYPED_PAYLOAD =
            new HashSet<>(
                    Arrays.asList(
                            Action.Key, Action.WebPage, Action.Script, Action.TaskerTask));

    /** Actions whose payload comes from MultiSelectActivity, and the mode each opens it in. */
    private static final Map<Integer, Integer> APP_PICKER_MODE = new HashMap<>();

    static {
        APP_PICKER_MODE.put(Action.App, 0);
        APP_PICKER_MODE.put(Action.Activity, 1);
        APP_PICKER_MODE.put(Action.Shortcut, 5);
    }

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

    @Test
    public void everyRowAssignsTheActionItIsLabelledWith() throws Exception {
        List<UiTestSupport.SectionedRow> rows = rowsOfAFreshPicker();
        Set<Integer> covered = new HashSet<>();
        for (UiTestSupport.SectionedRow row : rows) {
            int type = Arrays.asList(Action.names).indexOf(row.title);
            assertEquals(row + ": caption", Action.captions[type], row.caption);
            covered.add(type);

            UiTestSupport.resetSingletons(context);
            SettingsValues.getInstance(context).setCurrentPie(0);
            CommandSelectActivity picker =
                    Robolectric.buildActivity(CommandSelectActivity.class).setup().get();
            Object dialogBefore = ShadowAlertDialog.getLatestDialog();
            picker.clickRowForTest(findRowPosition(picker, row.title));
            UiTestSupport.idle();
            // getLatestDialog() survives across activities, so only a *new* one counts.
            AlertDialog shown =
                    ShadowAlertDialog.getLatestDialog() == dialogBefore
                            ? null
                            : (AlertDialog) ShadowAlertDialog.getLatestDialog();

            if (APP_PICKER_MODE.containsKey(type)) {
                Intent started = shadowOf(picker).getNextStartedActivity();
                assertNotNull(row + " should open the app picker", started);
                assertEquals(
                        row.toString(),
                        MultiSelectActivity.class.getName(),
                        started.getComponent().getClassName());
                assertEquals(
                        row + ": app picker mode",
                        (int) APP_PICKER_MODE.get(type),
                        MultiSelectActivity.AppSelectMode);
            } else if (TYPED_PAYLOAD.contains(type)) {
                AlertDialog dialog = shown;
                assertNotNull(row + " should ask for its payload", dialog);
                EditText input = UiTestSupport.editTextIn(dialog);
                input.setText("payload-" + type);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                UiTestSupport.idle();
                assertFalse(row + ": dialog should close on OK", dialog.isShowing());
                Action action = SettingsValues.getInstance(context).getPieAction(0);
                assertEquals(row.toString(), type, action.getType());
                assertEquals(row.toString(), "payload-" + type, action.getString());
            } else {
                AlertDialog unexpected = shown;
                assertNull(
                        row + " should not ask for a payload, but showed: "
                                + (unexpected == null ? "" : UiTestSupport.textsIn(unexpected)),
                        unexpected);
                assertEquals(
                        row.toString(),
                        type,
                        SettingsValues.getInstance(context).getPieAction(0).getType());
            }
        }
        // Every real action is offered; the Offset* section markers never are.
        for (int type = 1; type < Action.names.length; type++) {
            boolean isMarker = Action.names[type].startsWith("Offset");
            assertEquals(Action.names[type], !isMarker, covered.contains(type));
        }
    }

    private List<UiTestSupport.SectionedRow> rowsOfAFreshPicker() {
        CommandSelectActivity picker =
                Robolectric.buildActivity(CommandSelectActivity.class).setup().get();
        return UiTestSupport.sectionedActionRows(picker.rowsForTest());
    }

    private static int findRowPosition(CommandSelectActivity picker, String title) {
        for (UiTestSupport.SectionedRow row : UiTestSupport.sectionedActionRows(picker.rowsForTest())) {
            if (row.title.equals(title)) {
                return row.position;
            }
        }
        throw new AssertionError("no row labelled " + title);
    }
}
