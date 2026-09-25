package com.noname81.lmt;

import static org.junit.Assert.assertEquals;

import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.slider.Slider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

/** ROADMAP 1.13: the pie color editor previews a pie slice; the other color editors a swatch. */
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

    private static AlertDialog open(String[] labels, String colors) {
        FragmentActivity host = UiTestSupport.hostActivity();
        new ColorDialog(host, "Colors", labels, colors) {
            @Override
            public boolean onOkClicked(String colorString) {
                return true;
            }
        }.show();
        UiTestSupport.idle();
        return (AlertDialog) ShadowAlertDialog.getLatestDialog();
    }

    @Test
    public void pieColorsShowASlice() {
        AlertDialog dialog = open(PIE_PARTS, "#99000000,#cb000000,,#ffa70000,,,");
        assertEquals(View.VISIBLE, dialog.findViewById(R.id.pie_tile).getVisibility());
        assertEquals(View.GONE, dialog.findViewById(R.id.color_preview).getVisibility());
    }

    @Test
    public void otherColorsKeepTheSwatch() {
        AlertDialog dialog = open(new String[] {"pointer color"}, "0");
        assertEquals(View.GONE, dialog.findViewById(R.id.pie_tile).getVisibility());
        assertEquals(View.VISIBLE, dialog.findViewById(R.id.color_preview).getVisibility());
    }

    @Test
    public void aTypedColorStringMovesTheSliders() {
        AlertDialog dialog = open(PIE_PARTS, "");
        EditText text = dialog.findViewById(R.id.color_string);
        text.setText("#80102030");
        UiTestSupport.idle();
        assertEquals(0x80, (int) ((Slider) dialog.findViewById(R.id.alpha_seek)).getValue());
        assertEquals(0x10, (int) ((Slider) dialog.findViewById(R.id.red_seek)).getValue());
        assertEquals(0x30, (int) ((Slider) dialog.findViewById(R.id.blue_seek)).getValue());
    }
}
