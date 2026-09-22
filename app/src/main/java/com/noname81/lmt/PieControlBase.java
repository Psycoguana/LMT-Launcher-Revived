package com.noname81.lmt;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

/* JADX INFO: loaded from: classes.dex */
abstract class PieControlBase {
    Context mContext;
    private int mItemSize;
    PieMenu mPie;

    protected abstract void populateMenu();

    PieControlBase(Context context) {
        this.mContext = context;
        this.mItemSize = (int) context.getResources().getDimension(R.dimen.qc_item_size);
    }

    protected void attachToContainer(FrameLayout frameLayout) {
        if (this.mPie == null) {
            this.mPie = new PieMenu(this.mContext);
            this.mPie.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
            populateMenu();
        }
        frameLayout.addView(this.mPie);
    }

    void removeFromContainer(FrameLayout frameLayout) {
        frameLayout.removeView(this.mPie);
    }

    void setClickListener(View.OnClickListener onClickListener, PieItem... pieItemArr) {
        for (PieItem pieItem : pieItemArr) {
            pieItem.getView().setOnClickListener(onClickListener);
        }
    }

    void setLongClickListener(View.OnLongClickListener onLongClickListener, PieItem... pieItemArr) {
        for (PieItem pieItem : pieItemArr) {
            pieItem.getView().setOnLongClickListener(onLongClickListener);
        }
    }

    void setKeyListener(View.OnKeyListener onKeyListener, PieItem... pieItemArr) {
        for (PieItem pieItem : pieItemArr) {
            pieItem.getView().setOnKeyListener(onKeyListener);
        }
    }

    PieItem makeItem(Drawable drawable, Drawable drawable2, int i) {
        ImageView imageView = new ImageView(this.mContext);
        if (drawable != null) {
            drawable.mutate();
        }
        if (drawable2 != null) {
            drawable2.mutate();
        }
        imageView.setImageDrawable(drawable);
        imageView.setMinimumWidth(this.mItemSize);
        imageView.setMinimumHeight(this.mItemSize);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        int i2 = this.mItemSize;
        imageView.setLayoutParams(new ViewGroup.LayoutParams(i2, i2));
        PieItem pieItem = new PieItem(imageView, i);
        pieItem.setDrawables(drawable, drawable2);
        return pieItem;
    }
}
