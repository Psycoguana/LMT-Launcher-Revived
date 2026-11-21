package com.noname81.lmt;

import static org.junit.Assert.assertNotEquals;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** A shortcut app returning no shortcut intent is ignored instead of crashing LMT. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ShortcutResultTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() throws Exception {
        UiTestSupport.resetSingletons(mContext);
    }

    @Test
    public void aResultWithoutAShortcutIntentIsIgnored() {
        SettingsValues.getInstance(mContext).setCurrentPie(5);
        MultiSelectActivity.AppSelectMode = MultiSelectActivity.SelectShortcut;
        MultiSelectActivity activity =
                Robolectric.buildActivity(MultiSelectActivity.class).setup().get();

        activity.onActivityResult(1, Activity.RESULT_OK, new Intent());
        activity.onActivityResult(1, Activity.RESULT_OK, null);

        assertNotEquals(
                Action.Shortcut, SettingsValues.getInstance(mContext).getPieAction(5).getType());
    }
}
