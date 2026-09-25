package com.noname81.lmt;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;

/**
 * A color editor preview (ROADMAP 1.13): draws what the colors being edited color, the way LMT
 * draws it. Colors come in the color string's order, with {@link #UNSET} (-2) for a part left
 * empty, which falls back the way {@code PieMenu.init} does.
 */
interface ColorPreview {
    int UNSET = -2;

    /** {@code current} is the index of the part being edited. */
    void setColors(int[] colors, int current);

    /** Grey checkerboard behind a preview, so see-through colors show as such. */
    static BitmapShader checkerboard(float density) {
        int cell = Math.max(1, (int) (8 * density));
        Bitmap bitmap = Bitmap.createBitmap(cell * 2, cell * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFF9E9E9E);
        Paint dark = new Paint();
        dark.setColor(0xFF616161);
        canvas.drawRect(0, 0, cell, cell, dark);
        canvas.drawRect(cell, cell, cell * 2, cell * 2, dark);
        return new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    }
}
