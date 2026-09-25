package com.noname81.lmt;

import static org.junit.Assert.assertEquals;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.slider.Slider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

/**
 * ROADMAP 1.13: each color editor previews what it colors: a pie slice, the pointer, the status
 * infos.
 */
@RunWith(RobolectricTestRunner.class)
@Config(
        sdk = 33,
        shadows = ShadowTouchServiceNative.class,
        instrumentedPackages = "com.noname81.lmt")
public class PieColorDialogTest {
    private static final String[] PIE_PARTS = {
        "normal color",
        "selected color",
        "icon color",
        "outline color",
        "gradient color",
        "gradient outline color",
        "shift color"
    };

    private static AlertDialog open(String[] labels, String colors, int preview) {
        FragmentActivity host = UiTestSupport.hostActivity();
        new ColorDialog(host, "Colors", labels, colors, preview) {
            @Override
            public boolean onOkClicked(String colorString) {
                return true;
            }
        }.show();
        UiTestSupport.idle();
        return (AlertDialog) ShadowAlertDialog.getLatestDialog();
    }

    private static void assertOnlyVisible(AlertDialog dialog, int shown) {
        for (int id : new int[] {R.id.pie_tile, R.id.pointer_preview, R.id.status_info_preview}) {
            assertEquals(
                    id == shown ? View.VISIBLE : View.GONE,
                    dialog.findViewById(id).getVisibility());
        }
    }

    @Test
    public void pieColorsShowASlice() {
        AlertDialog dialog =
                open(PIE_PARTS, "#99000000,#cb000000,,#ffa70000,,,", ColorDialog.PREVIEW_PIE);
        assertOnlyVisible(dialog, R.id.pie_tile);
        assertEquals("normal color", ((TextView) dialog.findViewById(R.id.color_label)).getText());
    }

    @Test
    public void pointerColorShowsThePointer() {
        AlertDialog dialog = open(new String[] {"pointer color"}, "", ColorDialog.PREVIEW_POINTER);
        assertOnlyVisible(dialog, R.id.pointer_preview);
        assertEquals("pointer color", ((TextView) dialog.findViewById(R.id.color_label)).getText());
    }

    @Test
    public void statusInfoColorsShowTheStatusInfos() {
        AlertDialog dialog =
                open(
                        new String[] {"clock color", "notification color", "dim color"},
                        "#ffff0000,,#80000000",
                        ColorDialog.PREVIEW_STATUS_INFOS);
        assertOnlyVisible(dialog, R.id.status_info_preview);
        dialog.findViewById(R.id.color_next).performClick();
        assertEquals(
                "notification color", ((TextView) dialog.findViewById(R.id.color_label)).getText());
    }

    @Test
    public void aTypedColorStringMovesTheSliders() {
        AlertDialog dialog = open(PIE_PARTS, "", ColorDialog.PREVIEW_PIE);
        EditText text = dialog.findViewById(R.id.color_string);
        text.setText("#80102030");
        UiTestSupport.idle();
        assertEquals(0x80, (int) ((Slider) dialog.findViewById(R.id.alpha_seek)).getValue());
        assertEquals(0x10, (int) ((Slider) dialog.findViewById(R.id.red_seek)).getValue());
        assertEquals(0x30, (int) ((Slider) dialog.findViewById(R.id.blue_seek)).getValue());
    }

    @Test
    public void styledChoiceRowsGetTheirStyle() {
        FragmentActivity host = UiTestSupport.hostActivity();
        new InputDialog(
                host,
                "Pie font",
                new String[] {"small", "big"},
                "0",
                null,
                (position, row) -> row.setTextSize(position == 0 ? 10 : 30)) {
            @Override
            public boolean onOkClicked(String value) {
                return true;
            }
        }.show();
        UiTestSupport.idle();
        AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        android.widget.ListView list = dialog.getListView();
        list.measure(0, 0);
        list.layout(0, 0, 1000, 1000);
        TextView big = (TextView) list.getAdapter().getView(1, null, list);
        assertEquals(30f, big.getTextSize(), 0.01f);
    }
}
