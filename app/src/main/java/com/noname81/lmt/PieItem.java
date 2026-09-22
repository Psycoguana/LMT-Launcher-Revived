package com.noname81.lmt;

import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

/* JADX INFO: loaded from: classes.dex */
class PieItem {
    private float animate;
    private int inner;
    private int level;
    private int mColor = -1;
    private Drawable mDrawableLong;
    private Drawable mDrawableShort;
    private Path mPath;
    private boolean mSelected;
    private View mView;
    private int outer;
    private float start;
    private float sweep;

    PieItem(View view, int i) {
        this.mView = view;
        this.level = i;
    }

    void setColor(int i) {
        if (i != this.mColor) {
            if (i != 0) {
                Drawable drawable = this.mDrawableShort;
                if (drawable != null && drawable.getChangingConfigurations() != 1) {
                    this.mDrawableShort.setColorFilter(i, PorterDuff.Mode.MULTIPLY);
                }
                Drawable drawable2 = this.mDrawableLong;
                if (drawable2 != null && drawable2.getChangingConfigurations() != 1) {
                    this.mDrawableLong.setColorFilter(i, PorterDuff.Mode.MULTIPLY);
                }
            } else {
                Drawable drawable3 = this.mDrawableShort;
                if (drawable3 != null) {
                    drawable3.clearColorFilter();
                }
                Drawable drawable4 = this.mDrawableLong;
                if (drawable4 != null) {
                    drawable4.clearColorFilter();
                }
            }
            this.mColor = i;
        }
    }

    void setSelected(boolean z) {
        this.mSelected = z;
        View view = this.mView;
        if (view != null) {
            view.setSelected(z);
        }
    }

    void setAnimationAngle(float f) {
        this.animate = f;
    }

    float getAnimationAngle() {
        return this.animate;
    }

    boolean isSelected() {
        return this.mSelected;
    }

    int getLevel() {
        return this.level;
    }

    void setGeometry(float f, float f2, int i, int i2, Path path) {
        this.start = f;
        this.sweep = f2;
        this.inner = i;
        this.outer = i2;
        this.mPath = path;
    }

    float getStart() {
        return this.start;
    }

    float getStartAngle() {
        return this.start + this.animate;
    }

    float getSweep() {
        return this.sweep;
    }

    int getInnerRadius() {
        return this.inner;
    }

    int getOuterRadius() {
        return this.outer;
    }

    View getView() {
        return this.mView;
    }

    Path getPath() {
        return this.mPath;
    }

    void setDrawables(Drawable drawable, Drawable drawable2) {
        this.mDrawableShort = drawable;
        this.mDrawableLong = drawable2;
    }

    void setAlpha(int i) {
        Drawable drawable = this.mDrawableShort;
        if (drawable != null) {
            drawable.setAlpha(i);
        }
        Drawable drawable2 = this.mDrawableLong;
        if (drawable2 != null) {
            drawable2.setAlpha(i);
        }
    }

    void selectImage(int i) {
        Drawable drawable;
        ImageView imageView = (ImageView) this.mView;
        if (i == 0 && (drawable = this.mDrawableShort) != null) {
            imageView.setImageDrawable(drawable);
            return;
        }
        Drawable drawable2 = this.mDrawableLong;
        if (drawable2 != null) {
            imageView.setImageDrawable(drawable2);
        }
    }
}
