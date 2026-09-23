package com.noname81.lmt;

import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

/**
 * One slice: its geometry, its two drawables, and its selected state.
 *
 * <p>Geometry is set by {@code PieMenu.layoutPie} and read back during hit-testing, so a slice
 * knows the ring it is on and the angular range it occupies.
 */
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

    PieItem(View view, int level) {
        this.mView = view;
        this.level = level;
    }

    void setColor(int color) {
        if (color != this.mColor) {
            if (color != 0) {
                Drawable drawable = this.mDrawableShort;
                if (drawable != null && drawable.getChangingConfigurations() != 1) {
                    this.mDrawableShort.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
                }
                Drawable drawable2 = this.mDrawableLong;
                if (drawable2 != null && drawable2.getChangingConfigurations() != 1) {
                    this.mDrawableLong.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
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
            this.mColor = color;
        }
    }

    void setSelected(boolean selected) {
        this.mSelected = selected;
        View view = this.mView;
        if (view != null) {
            view.setSelected(selected);
        }
    }

    void setAnimationAngle(float angle) {
        this.animate = angle;
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

    void setGeometry(float start, float sweep, int inner, int outer, Path path) {
        this.start = start;
        this.sweep = sweep;
        this.inner = inner;
        this.outer = outer;
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

    void setAlpha(int alpha) {
        Drawable drawable = this.mDrawableShort;
        if (drawable != null) {
            drawable.setAlpha(alpha);
        }
        Drawable drawable2 = this.mDrawableLong;
        if (drawable2 != null) {
            drawable2.setAlpha(alpha);
        }
    }

    void selectImage(int which) {
        Drawable drawable;
        ImageView imageView = (ImageView) this.mView;
        if (which == 0 && (drawable = this.mDrawableShort) != null) {
            imageView.setImageDrawable(drawable);
            return;
        }
        Drawable drawable2 = this.mDrawableLong;
        if (drawable2 != null) {
            imageView.setImageDrawable(drawable2);
        }
    }
}
