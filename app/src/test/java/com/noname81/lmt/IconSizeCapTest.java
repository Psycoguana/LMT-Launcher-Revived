package com.noname81.lmt;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Saved icons are scaled down, and drawing a drawable is capped, so prefs stay small. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class IconSizeCapTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private Bitmap saved(android.graphics.drawable.Drawable drawable) {
        return IconUtils.convertBase64StringToDrawable(
                        mContext, IconUtils.convertDrawableToBase64String(drawable))
                .getBitmap();
    }

    @Test
    public void aLargeShortcutIconIsSavedAtTheCapKeepingItsShape() {
        Bitmap large = Bitmap.createBitmap(1024, 512, Bitmap.Config.ARGB_8888);

        Bitmap saved = saved(new BitmapDrawable(mContext.getResources(), large));

        assertEquals(IconUtils.SAVED_ICON_MAX, saved.getWidth());
        assertEquals(IconUtils.SAVED_ICON_MAX / 2, saved.getHeight());
    }

    @Test
    public void aSmallIconIsSavedAsIs() {
        Bitmap small = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);

        assertEquals(96, saved(new BitmapDrawable(mContext.getResources(), small)).getWidth());
    }

    @Test
    public void aDrawableClaimingAHugeSizeIsDrawnAtTheCap() {
        ColorDrawable huge =
                new ColorDrawable(Color.GREEN) {
                    @Override
                    public int getIntrinsicWidth() {
                        return 100_000;
                    }

                    @Override
                    public int getIntrinsicHeight() {
                        return 100_000;
                    }
                };

        assertEquals(IconUtils.DRAWN_ICON_MAX, IconUtils.toBitmap(huge).getWidth());
    }
}
