package com.noname81.lmt;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/** A preference row ending in a colour circle: the Appearance page's Color row (ROADMAP 1.10). */
public class ColorDotPreference extends Preference {
    private Integer mDotColor;

    public ColorDotPreference(Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_widget_color);
    }

    /** The colour to show, or null for no circle. */
    void setDotColor(Integer color) {
        this.mDotColor = color;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View dot = holder.findViewById(R.id.color_dot);
        if (dot == null) {
            return;
        }
        dot.setVisibility(this.mDotColor == null ? View.GONE : View.VISIBLE);
        if (this.mDotColor != null) {
            dot.setBackground(ColorSwatchDialog.circle(getContext(), this.mDotColor));
        }
    }
}
