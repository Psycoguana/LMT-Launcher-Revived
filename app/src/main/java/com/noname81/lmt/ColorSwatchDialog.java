package com.noname81.lmt;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * The Appearance page's colour picker (ROADMAP 1.10): a grid of circles, one per {@code
 * ThemeHelper.COLOR_*} choice, the selected one checked. Picking a circle only moves the check;
 * OK applies it. Custom is the exception - it needs a colour first, so it reports straight away
 * and the caller opens the custom colour dialog.
 */
final class ColorSwatchDialog {
    private static final int COLUMNS = 5;

    interface OnPicked {
        void picked(int choice);
    }

    private ColorSwatchDialog() {}

    static AlertDialog show(
            Context context, String title, int selected, int customColor, OnPicked onPicked) {
        int[] choice = {selected};
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        Context themed = builder.getContext();
        RecyclerView grid =
                (RecyclerView)
                        LayoutInflater.from(themed).inflate(R.layout.color_swatch_grid, null);
        grid.setLayoutManager(new GridLayoutManager(themed, COLUMNS));
        AlertDialog dialog =
                builder.setTitle(title)
                        .setView(grid)
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .setPositiveButton(
                                R.string.dialog_ok, (d, which) -> onPicked.picked(choice[0]))
                        .create();
        grid.setAdapter(new Adapter(themed, choice, customColor, picked -> {
            if (picked == ThemeHelper.COLOR_CUSTOM) {
                dialog.dismiss();
                onPicked.picked(picked);
            }
        }));
        dialog.show();
        return dialog;
    }

    /** A filled circle with a hairline outline, so light colours still read on light surfaces. */
    static GradientDrawable circle(Context context, int color) {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(color);
        int outline =
                MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorOutlineVariant, Color.GRAY);
        int hairline =
                (int)
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                1,
                                context.getResources().getDisplayMetrics());
        circle.setStroke(hairline, outline);
        return circle;
    }

    /**
     * The circle as a touch target: the ripple is masked to the oval. The stock borderless ripple
     * attribute draws a square highlight on some devices (seen on One UI).
     */
    static RippleDrawable rippleCircle(Context context, int color) {
        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.OVAL);
        mask.setColor(Color.WHITE);
        int highlight =
                MaterialColors.getColor(
                        context, androidx.appcompat.R.attr.colorControlHighlight, Color.GRAY);
        return new RippleDrawable(ColorStateList.valueOf(highlight), circle(context, color), mask);
    }

    private static final class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private final Context mContext;
        private final int[] mChoice;
        private final int mCustomColor;
        private final OnPicked mOnTapped;
        private final String[] mNames = ThemeHelper.colorNames();

        Adapter(Context context, int[] choice, int customColor, OnPicked onTapped) {
            this.mContext = context;
            this.mChoice = choice;
            this.mCustomColor = customColor;
            this.mOnTapped = onTapped;
        }

        @Override
        public int getItemCount() {
            return this.mNames.length;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.color_swatch, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            int color = ThemeHelper.swatchColor(position, this.mCustomColor);
            boolean selected = position == this.mChoice[0];
            holder.swatch.setBackground(rippleCircle(this.mContext, color));
            holder.swatch.setSelected(selected);
            holder.swatch.setContentDescription(this.mNames[position]);
            TooltipCompat.setTooltipText(holder.swatch, this.mNames[position]);

            int icon = 0;
            if (selected) {
                icon = R.drawable.ic_check;
            } else if (position == ThemeHelper.COLOR_CUSTOM) {
                icon = R.drawable.ic_edit;
            }
            holder.icon.setImageResource(icon);
            holder.icon.setColorFilter(
                    ColorUtils.calculateLuminance(color) > 0.5 ? Color.BLACK : Color.WHITE);

            holder.swatch.setOnClickListener(
                    v -> {
                        int tapped = holder.getBindingAdapterPosition();
                        if (tapped == RecyclerView.NO_POSITION) {
                            return;
                        }
                        int previous = this.mChoice[0];
                        this.mChoice[0] = tapped;
                        notifyItemChanged(previous);
                        notifyItemChanged(tapped);
                        this.mOnTapped.picked(tapped);
                    });
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final View swatch;
            final ImageView icon;

            Holder(View itemView) {
                super(itemView);
                this.swatch = itemView.findViewById(R.id.swatch);
                this.icon = itemView.findViewById(R.id.swatch_icon);
            }
        }
    }
}
