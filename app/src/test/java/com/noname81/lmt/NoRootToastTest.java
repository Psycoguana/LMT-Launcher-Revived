package com.noname81.lmt;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

/** A phone without root is not a failure: no "Failed to get root permissions!" toast. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class NoRootToastTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() throws Exception {
        UiTestSupport.resetSingletons(mContext);
    }

    @Test
    public void noSuBinaryMeansNoRootToast() {
        // Like an unrooted phone; macOS ships a su, so this only runs where there is none.
        assumeFalse(RootContext.suExists());

        RootContext root = RootContext.getInstance(mContext);

        assertTrue(root.isRootDenied());
        assertNull(ShadowToast.getLatestToast());
    }
}
