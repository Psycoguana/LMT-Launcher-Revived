package com.noname81.lmt;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Picks a Material icon for a pie folder (ROADMAP 1.17): a search field over a grid of every icon
 * with its name. Tapping one picks it; "Default" goes back to the folder icon.
 */
final class IconPickerDialog {
    interface OnPicked {
        /** The icon's name, or null for the default. */
        void picked(String name);
    }

    private static final int COLUMNS = 4;

    /** Shown first while the search is empty: icons that suit a folder of actions. */
    static final String[] SUGGESTED = {
        "folder",
        "folder_open",
        "apps",
        "star",
        "favorite",
        "home",
        "work",
        "build",
        "settings",
        "tune",
        "bolt",
        "flash_on",
        "music_note",
        "headphones",
        "movie",
        "photo_camera",
        "image",
        "videogame_asset",
        "sports_esports",
        "chat",
        "mail",
        "call",
        "people",
        "shopping_cart",
        "school",
        "fitness_center",
        "restaurant",
        "local_cafe",
        "flight",
        "directions_car",
        "map",
        "public",
        "cloud",
        "code",
        "terminal",
        "lightbulb",
        "palette",
        "bookmark",
        "pets",
        "lock",
        "wifi",
        "bluetooth",
        "brightness_6",
        "notifications",
        "schedule",
        "calculate",
        "sell",
        "account_balance_wallet"
    };

    private IconPickerDialog() {}

    static AlertDialog show(Context context, String current, OnPicked onPicked) {
        float density = context.getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * density);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, Math.round(8 * density), pad, 0);

        TextInputLayout searchLayout =
                new TextInputLayout(
                        context, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        searchLayout.setHint("Search icons");
        searchLayout.setStartIconDrawable(R.drawable.ic_search);
        searchLayout.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);
        TextInputEditText search = new TextInputEditText(searchLayout.getContext());
        search.setId(R.id.icon_search);
        search.setSingleLine(true);
        searchLayout.addView(search);
        root.addView(searchLayout);

        RecyclerView grid = new RecyclerView(context);
        grid.setId(R.id.icon_grid);
        grid.setLayoutManager(new GridLayoutManager(context, COLUMNS));
        // A fixed height, so the dialog doesn't jump while the results change.
        LinearLayout.LayoutParams gridParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.round(360 * density));
        gridParams.topMargin = Math.round(8 * density);
        root.addView(grid, gridParams);

        AlertDialog[] dialog = new AlertDialog[1];
        Adapter adapter =
                new Adapter(
                        context,
                        current,
                        name -> {
                            onPicked.picked(name);
                            if (dialog[0] != null) {
                                dialog[0].dismiss();
                            }
                        });
        grid.setAdapter(adapter);
        search.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        adapter.filter(s.toString());
                        grid.scrollToPosition(0);
                    }
                });

        dialog[0] =
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Folder icon")
                        .setView(root)
                        .setNeutralButton("Default", (d, which) -> onPicked.picked(null))
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show();
        int selected = adapter.indexOf(current);
        if (selected > 0) {
            grid.scrollToPosition(selected);
        }
        return dialog[0];
    }

    static final class Adapter extends RecyclerView.Adapter<Adapter.Cell> {
        private final Context mContext;
        private final List<String> mAll;
        private final List<String> mShown = new ArrayList<>();
        private final String mCurrent;
        private final OnPicked mOnPicked;

        Adapter(Context context, String current, OnPicked onPicked) {
            this.mContext = context;
            this.mAll = MaterialIcons.names(context);
            filter("");
            this.mCurrent = current;
            this.mOnPicked = onPicked;
        }

        /** Keeps the icons whose name has every word typed, in any order. */
        void filter(String query) {
            String[] words = query.trim().toLowerCase(Locale.ROOT).split("[\\s_]+");
            this.mShown.clear();
            if (query.trim().isEmpty()) {
                for (String name : SUGGESTED) {
                    if (MaterialIcons.glyph(this.mContext, name) != null) {
                        this.mShown.add(name);
                    }
                }
                for (String name : this.mAll) {
                    if (!this.mShown.contains(name)) {
                        this.mShown.add(name);
                    }
                }
                notifyDataSetChanged();
                return;
            }
            for (String name : this.mAll) {
                boolean matches = true;
                for (String word : words) {
                    if (!word.isEmpty() && !name.contains(word)) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    this.mShown.add(name);
                }
            }
            notifyDataSetChanged();
        }

        int indexOf(String name) {
            return name == null ? -1 : this.mShown.indexOf(name);
        }

        List<String> shownForTest() {
            return this.mShown;
        }

        @NonNull
        @Override
        public Cell onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            float density = this.mContext.getResources().getDisplayMetrics().density;
            LinearLayout cell = new LinearLayout(this.mContext);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            int pad = Math.round(6 * density);
            cell.setPadding(pad, Math.round(10 * density), pad, Math.round(8 * density));
            TypedValue ripple = new TypedValue();
            this.mContext
                    .getTheme()
                    .resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
            cell.setForeground(this.mContext.getDrawable(ripple.resourceId));

            TextView glyph = new TextView(this.mContext);
            glyph.setTypeface(MaterialIcons.typeface(this.mContext));
            glyph.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            glyph.setGravity(Gravity.CENTER);
            glyph.setTextColor(
                    MaterialColors.getColor(
                            cell, com.google.android.material.R.attr.colorOnSurface));
            cell.addView(glyph);

            TextView label = new TextView(this.mContext);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setTextColor(
                    MaterialColors.getColor(
                            cell, com.google.android.material.R.attr.colorOnSurfaceVariant));
            cell.addView(label);
            cell.setLayoutParams(
                    new RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
            return new Cell(cell, glyph, label);
        }

        @Override
        public void onBindViewHolder(@NonNull Cell holder, int position) {
            String name = this.mShown.get(position);
            holder.glyph.setText(MaterialIcons.glyph(this.mContext, name));
            holder.label.setText(MaterialIcons.label(name));
            holder.itemView.setContentDescription(MaterialIcons.label(name));
            boolean selected = name.equals(this.mCurrent);
            if (selected) {
                GradientDrawable mark = new GradientDrawable();
                mark.setCornerRadius(12 * this.mContext.getResources().getDisplayMetrics().density);
                mark.setColor(
                        ColorStateList.valueOf(
                                MaterialColors.getColor(
                                        holder.itemView,
                                        com.google.android.material.R.attr
                                                .colorSecondaryContainer)));
                holder.itemView.setBackground(mark);
            } else {
                holder.itemView.setBackground(null);
            }
            holder.itemView.setOnClickListener(v -> this.mOnPicked.picked(name));
        }

        @Override
        public int getItemCount() {
            return this.mShown.size();
        }

        static final class Cell extends RecyclerView.ViewHolder {
            final TextView glyph;
            final TextView label;

            Cell(View itemView, TextView glyph, TextView label) {
                super(itemView);
                this.glyph = glyph;
                this.label = label;
            }
        }
    }
}
