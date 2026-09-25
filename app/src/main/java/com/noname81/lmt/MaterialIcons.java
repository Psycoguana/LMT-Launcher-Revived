package com.noname81.lmt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google's Material Icons font (assets/MaterialIcons-Regular.ttf, Apache 2.0, see
 * assets/MaterialIcons-LICENSE.txt) for pie folder icons (ROADMAP 1.17): about 2,200 icons, found
 * by the names in its codepoints file. A picked icon is saved as {@code symbol:<name>} in the
 * slot's Icon field and drawn white, so the pie tints it like its own icons.
 */
final class MaterialIcons {
    private static final String TAG = "LMT::MaterialIcons";
    static final String PREFIX = "symbol:";

    /** Same size as the bundled action icons (144 px at xxhdpi), glyph two thirds of it. */
    private static final int ICON_DP = 48;

    private static Typeface sTypeface;
    private static List<String> sNames;
    private static Map<String, Integer> sCodes;

    private MaterialIcons() {}

    static synchronized Typeface typeface(Context context) {
        if (sTypeface == null) {
            sTypeface =
                    Typeface.createFromAsset(
                            context.getApplicationContext().getAssets(),
                            "MaterialIcons-Regular.ttf");
        }
        return sTypeface;
    }

    /** Every icon name, in the font's (alphabetical) order. */
    static synchronized List<String> names(Context context) {
        if (sNames == null) {
            List<String> names = new ArrayList<>();
            Map<String, Integer> codes = new HashMap<>();
            try (BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    context.getApplicationContext()
                                            .getAssets()
                                            .open("MaterialIcons-Regular.codepoints"),
                                    StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    int space = line.indexOf(' ');
                    if (space > 0) {
                        String name = line.substring(0, space);
                        names.add(name);
                        codes.put(name, Integer.parseInt(line.substring(space + 1).trim(), 16));
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "No icon list", e);
            }
            sNames = Collections.unmodifiableList(names);
            sCodes = codes;
        }
        return sNames;
    }

    /** The text that renders {@code name} in {@link #typeface}, or null for an unknown name. */
    static String glyph(Context context, String name) {
        names(context);
        Integer code = sCodes.get(name);
        return code == null ? null : new String(Character.toChars(code));
    }

    /** "folder_open" becomes "folder open", for labels and search. */
    static String label(String name) {
        return name.replace('_', ' ');
    }

    /** A white pie icon of {@code name}, or null for an unknown name. */
    static Drawable drawable(Context context, String name) {
        return drawable(context, name, ICON_DP, 2f / 3f, true);
    }

    /** A white {@code dp}-sized icon filling the box, for tinted UI such as a text field's. */
    static Drawable uiDrawable(Context context, String name, int dp) {
        return drawable(context, name, dp, 1f, false);
    }

    private static Drawable drawable(
            Context context, String name, int dp, float glyphShare, boolean shadow) {
        String glyph = glyph(context, name);
        if (glyph == null) {
            return null;
        }
        float density = context.getResources().getDisplayMetrics().density;
        int size = Math.max(1, Math.round(dp * density));
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(typeface(context));
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(size * glyphShare);
        if (shadow) {
            paint.setShadowLayer(size / 48f, 0, 0, Color.argb(115, 0, 0, 0));
        }
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = size / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(glyph, size / 2f, baseline, paint);
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    /** The icon name in a saved Icon field, or null when it holds an image (or nothing). */
    static String nameOf(String encodedIcon) {
        return encodedIcon != null && encodedIcon.startsWith(PREFIX)
                ? encodedIcon.substring(PREFIX.length())
                : null;
    }
}
