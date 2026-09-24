package com.noname81.lmt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import com.google.android.material.color.MaterialColors;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Loads an icon off the main thread and cross-fades it in, replacing the {@code AsyncTask}-based
 * {@code AsyncDrawableTask} (ROADMAP 1.6 phase 3). Nothing in the app ever calls {@code cancel()}
 * on one of those tasks - no view-recycling hook exists that would - so unlike
 * {@code AsyncDrawableTask} this has no cancellation concept; that branch was already dead code.
 *
 * <p>The bundled command/gesture icons are white-on-transparent, drawn for the pie's dark
 * background. On a light theme they vanish against the list, so any grey-only, mostly white icon is
 * tinted {@code colorOnSurfaceVariant}; app icons and other coloured images are left alone.
 */
final class IconLoader {
    private static final int FADE_IN_TIME = 500;
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private IconLoader() {}

    /**
     * Shows {@code loader}'s icon in {@code imageView}. {@code rowKey} names the row being bound:
     * rebinding a view to the same row (a tab coming back into view, a return from the command
     * picker) keeps its current icon up until the reload lands and then swaps it in without the
     * placeholder and fade, so unchanged icons don't flash. A view recycled for a different row
     * starts from the placeholder and fades in, and a load that finishes after the view has been
     * rebound again is dropped.
     */
    static void load(
            ImageView imageView, int placeholderResId, Object rowKey, Callable<Drawable> loader) {
        Context context = imageView.getContext();
        int tint = whiteIconTint(context);
        Object previous = imageView.getTag(R.id.icon_loader_request);
        boolean sameRow =
                previous instanceof Request
                        && ((Request) previous).loaded
                        && Objects.equals(((Request) previous).rowKey, rowKey);
        Request request = new Request(rowKey);
        imageView.setTag(R.id.icon_loader_request, request);
        Drawable placeholder = tintIfWhite(IconUtils.getDrawable(context, placeholderResId), tint);
        if (!sameRow) {
            imageView.setImageDrawable(placeholder);
        }
        WeakReference<ImageView> reference = new WeakReference<>(imageView);
        EXECUTOR.execute(
                () -> {
                    Drawable loaded;
                    try {
                        loaded = tintIfWhite(loader.call(), tint);
                    } catch (Exception e) {
                        loaded = null;
                    }
                    Drawable result = loaded;
                    MAIN.post(() -> apply(reference, request, placeholder, result, sameRow));
                });
    }

    private static void apply(
            WeakReference<ImageView> reference,
            Request request,
            Drawable placeholder,
            Drawable loaded,
            boolean sameRow) {
        ImageView imageView = reference.get();
        if (imageView == null || imageView.getTag(R.id.icon_loader_request) != request) {
            return;
        }
        request.loaded = true;
        if (loaded == null) {
            imageView.setImageDrawable(placeholder);
            return;
        }
        if (sameRow) {
            imageView.setImageDrawable(loaded);
            return;
        }
        TransitionDrawable transitionDrawable =
                new TransitionDrawable(new Drawable[] {placeholder, loaded});
        imageView.setImageDrawable(transitionDrawable);
        transitionDrawable.setCrossFadeEnabled(true);
        transitionDrawable.startTransition(FADE_IN_TIME);
    }

    /** One bind of an ImageView; stored as its tag so stale loads can be recognised. */
    private static final class Request {
        final Object rowKey;
        boolean loaded;

        Request(Object rowKey) {
            this.rowKey = rowKey;
        }
    }

    /** The colour to tint all-white icons with, or {@code 0} on a dark theme where they show. */
    private static int whiteIconTint(Context context) {
        int surface =
                MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorSurface, Color.BLACK);
        if (!MaterialColors.isColorLight(surface)) {
            return 0;
        }
        return MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK);
    }

    private static Drawable tintIfWhite(Drawable drawable, int tint) {
        if (tint == 0 || !isAllWhite(drawable)) {
            return drawable;
        }
        Drawable tinted = drawable.mutate();
        tinted.setTint(tint);
        return tinted;
    }

    /**
     * True for a grey-only bitmap icon that is mostly white (alpha-weighted mean brightness of at
     * least 200). Anti-aliased edges can be darker, so single pixels are not required to be white.
     * Samples a coarse grid.
     */
    private static boolean isAllWhite(Drawable drawable) {
        if (!(drawable instanceof BitmapDrawable)) {
            return false;
        }
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        if (bitmap == null
                || (Build.VERSION.SDK_INT >= 26 && bitmap.getConfig() == Bitmap.Config.HARDWARE)) {
            return false;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int step = Math.max(1, Math.max(width, height) / 64);
        long weight = 0;
        long brightness = 0;
        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int pixel = bitmap.getPixel(x, y);
                int alpha = Color.alpha(pixel);
                if (alpha < 64) {
                    continue;
                }
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > 16) {
                    return false;
                }
                weight += alpha;
                brightness += (long) alpha * (r + g + b) / 3;
            }
        }
        return weight > 0 && brightness >= 200 * weight;
    }
}
