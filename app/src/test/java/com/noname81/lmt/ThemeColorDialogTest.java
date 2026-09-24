package com.noname81.lmt;

import static org.junit.Assert.assertEquals;

import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** ROADMAP 1.10: the custom theme color picker's hex field and OK button. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, shadows = ShadowTouchServiceNative.class, instrumentedPackages = "com.noname81.lmt")
public class ThemeColorDialogTest {

    @Test
    public void opensOnTheCurrentColorAndReturnsATypedOne() {
        FragmentActivity host = UiTestSupport.hostActivity();
        int[] picked = {0};
        AlertDialog dialog =
                ThemeColorDialog.show(host, "Custom color", 0xFF6750A4, c -> picked[0] = c);
        UiTestSupport.idle();
        EditText hex = dialog.findViewById(R.id.hex);
        assertEquals("6750A4", hex.getText().toString());

        hex.setText("FF5722");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        UiTestSupport.idle();
        assertEquals(0xFFFF5722, picked[0]);
    }

    @Test
    public void incompleteHexKeepsTheLastValidColor() {
        FragmentActivity host = UiTestSupport.hostActivity();
        int[] picked = {0};
        AlertDialog dialog =
                ThemeColorDialog.show(host, "Custom color", 0xFF123456, c -> picked[0] = c);
        UiTestSupport.idle();
        EditText hex = dialog.findViewById(R.id.hex);
        hex.setText("12G");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        UiTestSupport.idle();
        assertEquals(0xFF123456, picked[0]);
    }
}
