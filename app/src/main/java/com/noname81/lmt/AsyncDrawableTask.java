package com.noname81.lmt;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/* JADX INFO: loaded from: classes.dex */
abstract class AsyncDrawableTask extends AsyncTask<Void, Void, Drawable> {
    private static final int FADE_IN_TIME = 500;
    private final WeakReference<ImageView> mImageViewReference;

    @Override // android.os.AsyncTask
    protected abstract Drawable doInBackground(Void... voidArr);

    AsyncDrawableTask(ImageView imageView, int i) {
        this.mImageViewReference = new WeakReference<>(imageView);
        imageView.setImageDrawable(IconUtils.getDrawable(imageView.getContext(), i));
    }

    @Override // android.os.AsyncTask
    protected void onPostExecute(Drawable drawable) {
        ImageView imageView;
        if (isCancelled()) {
            drawable = null;
        }
        WeakReference<ImageView> weakReference = this.mImageViewReference;
        if (weakReference == null || (imageView = weakReference.get()) == null) {
            return;
        }
        Drawable drawable2 = imageView.getDrawable();
        if (drawable != null) {
            TransitionDrawable transitionDrawable =
                    new TransitionDrawable(new Drawable[] {drawable2, drawable});
            imageView.setImageDrawable(transitionDrawable);
            transitionDrawable.setCrossFadeEnabled(true);
            transitionDrawable.startTransition(FADE_IN_TIME);
            return;
        }
        imageView.setImageDrawable(drawable2);
    }
}
