package com.noname81.lmt;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.util.TypedValue;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

/**
 * Icon loading, scaling and the base64 encoding used to store custom icons in prefs.
 *
 * <p>Note {@link #getNamePie}: user icons are files named after the slot, looked up in the
 * configured resource search path.
 */
class IconUtils {
    IconUtils() {}

    static String convertDrawableToBase64String(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), 0);
    }

    static BitmapDrawable convertBase64StringToDrawable(Context context, String base64) {
        if (base64 == null || base64.length() <= 0) {
            return null;
        }
        try {
            byte[] bArrDecode = Base64.decode(base64, 0);
            return new BitmapDrawable(
                    context.getResources(),
                    BitmapFactory.decodeByteArray(bArrDecode, 0, bArrDecode.length));
        } catch (Exception unused) {
            return null;
        }
    }

    static Drawable resizeImage(Context context, Drawable drawable, int scaleMode) {
        Resources resources = context.getResources();
        boolean scaleToFit = scaleMode == 1;
        if (scaleMode > 0 && scaleMode <= 2) {
            scaleMode =
                    (int)
                            TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP,
                                    35.0f,
                                    resources.getDisplayMetrics());
        }
        if (scaleMode <= 0) {
            if (scaleToFit) {
                drawable.setLevel(1);
            }
            return drawable;
        }
        BitmapDrawable bitmapDrawable =
                new BitmapDrawable(
                        resources,
                        Bitmap.createScaledBitmap(
                                getBitmapFromDrawable(drawable), scaleMode, scaleMode, true));
        if (scaleToFit) {
            bitmapDrawable.setChangingConfigurations(1);
        }
        return bitmapDrawable;
    }

    static void saveImage(Bitmap bitmap, IconFolder folder, String fileName) {
        try (java.io.OutputStream out = folder.create(fileName)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Bitmap getScaledBitmapFromUri(Context context, Uri uri, int maxSize) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            int sampleSize = 1;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(
                    context.getContentResolver().openInputStream(uri), null, options);
            int width = options.outWidth;
            int height = options.outHeight;
            while (width / 2 >= maxSize && height / 2 >= maxSize) {
                width /= 2;
                height /= 2;
                sampleSize *= 2;
            }
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inSampleSize = sampleSize;
            return BitmapFactory.decodeStream(
                    context.getContentResolver().openInputStream(uri), null, options2);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static Drawable getDrawable(Context context, int resId) {
        return context.getResources().getDrawable(resId, context.getTheme());
    }

    static Drawable getIconForPieShortPress(Context context) {
        return getDrawable(context, R.drawable.pie_s);
    }

    static Drawable getIconForPieLongPress(Context context) {
        return getDrawable(context, R.drawable.pielp_s);
    }

    static Drawable getIconForGesture(Context context, String gestureName) {
        return getDrawable(
                context,
                context.getResources()
                        .getIdentifier(gestureName + "_s", "drawable", context.getPackageName()));
    }

    static Drawable getIconForISA(Context context) {
        return getDrawable(context, R.drawable.isa_s);
    }

    static Drawable getIconForOK(Context context) {
        return getDrawable(context, R.drawable.ok);
    }

    static Drawable getIconForAction(Context context, Action action, String userIconName) {
        return action.getDrawable(context, userIconName, 0, 1, true);
    }

    static void setMaxSizeForImageView(Context context, ImageView imageView) {
        Resources resources = context.getResources();
        imageView.setAdjustViewBounds(true);
        imageView.setMaxHeight(
                (int)
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 70.0f, resources.getDisplayMetrics()));
        imageView.setMaxWidth(
                (int)
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 70.0f, resources.getDisplayMetrics()));
    }

    /** File name of the user-supplied PNG for pie slot {@code slot}. */
    static String getNamePie(int slot) {
        return "pie" + slot + ".png";
    }

    private static Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (Build.VERSION.SDK_INT >= 26) {
            return getBitmapFromDrawableV26(drawable);
        }
        return ((BitmapDrawable) drawable).getBitmap();
    }

    private static Bitmap getBitmapFromDrawableV26(Drawable drawable) {
        try {
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            }
            if (!(drawable instanceof AdaptiveIconDrawable)) {
                return null;
            }
            AdaptiveIconDrawable adaptiveIconDrawableM284m = ((AdaptiveIconDrawable) drawable);
            Bitmap bitmapCreateBitmap =
                    Bitmap.createBitmap(
                            adaptiveIconDrawableM284m.getIntrinsicWidth(),
                            adaptiveIconDrawableM284m.getIntrinsicHeight(),
                            Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            adaptiveIconDrawableM284m.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            adaptiveIconDrawableM284m.draw(canvas);
            return bitmapCreateBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
