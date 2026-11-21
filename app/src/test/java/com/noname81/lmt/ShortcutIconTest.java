package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Issue #1: Shortcut Maker returns its shortcut's icon as a resource, which loads as an adaptive
 * icon. Saving it cast the icon to BitmapDrawable, LMT crashed in onActivityResult, the shortcut
 * was lost, and the restarted process toasted "Failed to get root permissions!".
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ShortcutIconTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() throws Exception {
        UiTestSupport.resetSingletons(mContext);
    }

    private static AdaptiveIconDrawable adaptiveIcon() {
        ColorDrawable background = new ColorDrawable(Color.BLUE);
        ColorDrawable foreground = new ColorDrawable(Color.WHITE);
        return new AdaptiveIconDrawable(background, foreground);
    }

    @Test
    public void anAdaptiveIconIsEncoded() {
        String encoded = IconUtils.convertDrawableToBase64String(adaptiveIcon());

        assertNotNull(encoded);
        BitmapDrawable decoded = IconUtils.convertBase64StringToDrawable(mContext, encoded);
        assertNotNull(decoded.getBitmap());
    }

    @Test
    public void aDrawableWithoutASizeIsEncodedAtIconSize() {
        BitmapDrawable decoded =
                IconUtils.convertBase64StringToDrawable(
                        mContext,
                        IconUtils.convertDrawableToBase64String(new ColorDrawable(Color.RED)));

        assertEquals(144, decoded.getBitmap().getWidth());
    }

    @Test
    public void noIconEncodesToNothing() {
        assertNull(IconUtils.convertDrawableToBase64String(null));
    }

    @Test
    public void aShortcutWithAnAdaptiveIconIsSaved() {
        SettingsValues settings = SettingsValues.getInstance(mContext);
        settings.setCurrentPie(5);

        settings.setCurrentAction(null, new Action(Action.Shortcut, "#Intent;end", adaptiveIcon()));

        assertEquals(Action.Shortcut, settings.getPieAction(5).getType());
        assertNotNull(mContext.getSharedPreferences("LMT", 0).getString("PieItem5 Icon", null));
    }
}
