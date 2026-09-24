package com.noname81.lmt;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Loads an icon off the main thread and cross-fades it in, replacing the {@code AsyncTask}-based
 * {@code AsyncDrawableTask} (ROADMAP 1.6 phase 3). Nothing in the app ever calls {@code cancel()}
 * on one of those tasks - no view-recycling hook exists that would - so unlike
 * {@code AsyncDrawableTask} this has no cancellation concept; that branch was already dead code.
 */
final class IconLoader {
    private static final int FADE_IN_TIME = 500;
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private IconLoader() {}

    static void load(ImageView imageView, int placeholderResId, Callable<Drawable> loader) {
        imageView.setImageDrawable(IconUtils.getDrawable(imageView.getContext(), placeholderResId));
        WeakReference<ImageView> reference = new WeakReference<>(imageView);
        EXECUTOR.execute(
                () -> {
                    Drawable loaded;
                    try {
                        loaded = loader.call();
                    } catch (Exception e) {
                        loaded = null;
                    }
                    Drawable result = loaded;
                    MAIN.post(() -> apply(reference, result));
                });
    }

    private static void apply(WeakReference<ImageView> reference, Drawable loaded) {
        ImageView imageView = reference.get();
        if (imageView == null) {
            return;
        }
        Drawable placeholder = imageView.getDrawable();
        if (loaded != null) {
            TransitionDrawable transitionDrawable =
                    new TransitionDrawable(new Drawable[] {placeholder, loaded});
            imageView.setImageDrawable(transitionDrawable);
            transitionDrawable.setCrossFadeEnabled(true);
            transitionDrawable.startTransition(FADE_IN_TIME);
            return;
        }
        imageView.setImageDrawable(placeholder);
    }
}
