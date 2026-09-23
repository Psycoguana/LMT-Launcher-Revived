package com.noname81.lmt;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/** Loads an icon off the main thread and cross-fades it in over a placeholder. */
abstract class AsyncDrawableTask extends AsyncTask<Void, Void, Drawable> {
    private static final int FADE_IN_TIME = 500;
    private final WeakReference<ImageView> mImageViewReference;

    @Override // android.os.AsyncTask
    protected abstract Drawable doInBackground(Void... unused);

    /**
     * @param placeholderResId shown until {@link #doInBackground} produces the real icon.
     */
    AsyncDrawableTask(ImageView imageView, int placeholderResId) {
        this.mImageViewReference = new WeakReference<>(imageView);
        imageView.setImageDrawable(IconUtils.getDrawable(imageView.getContext(), placeholderResId));
    }

    @Override // android.os.AsyncTask
    protected void onPostExecute(Drawable loaded) {
        ImageView imageView;
        if (isCancelled()) {
            loaded = null;
        }
        WeakReference<ImageView> weakReference = this.mImageViewReference;
        if (weakReference == null || (imageView = weakReference.get()) == null) {
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
