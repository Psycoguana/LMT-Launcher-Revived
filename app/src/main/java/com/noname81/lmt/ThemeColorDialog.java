package com.noname81.lmt;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Picks the custom theme color (ROADMAP 1.10): a saturation/brightness square and a hue bar, with
 * a preview circle and a hex field that all follow each other. The color is always opaque - it
 * seeds a scheme, so alpha would mean nothing.
 */
final class ThemeColorDialog {
    interface OnPicked {
        void picked(int color);
    }

    private final SaturationValueView mSaturationValue;
    private final HueBarView mHue;
    private final View mPreview;
    private final EditText mHex;
    private final float[] mHsv = new float[3];
    private int mColor;
    /** Set while this class writes the hex field itself, so its watcher ignores the change. */
    private boolean mWritingHex;

    private ThemeColorDialog(View view, int color) {
        this.mSaturationValue = view.findViewById(R.id.saturation_value);
        this.mHue = view.findViewById(R.id.hue);
        this.mPreview = view.findViewById(R.id.color_preview_circle);
        this.mHex = view.findViewById(R.id.hex);
        this.mHue.setOnChanged(hue -> {
            this.mHsv[0] = hue;
            this.mSaturationValue.setHue(hue);
            fromPicker();
        });
        this.mSaturationValue.setOnChanged((saturation, value) -> {
            this.mHsv[1] = saturation;
            this.mHsv[2] = value;
            fromPicker();
        });
        this.mHex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!ThemeColorDialog.this.mWritingHex) {
                    fromHex(s.toString());
                }
            }
        });
        setColor(color | 0xFF000000, true);
    }

    static AlertDialog show(Context context, String title, int color, OnPicked onPicked) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View view =
                LayoutInflater.from(builder.getContext()).inflate(R.layout.theme_color_picker, null);
        ThemeColorDialog picker = new ThemeColorDialog(view, color);
        AlertDialog dialog =
                builder.setTitle(title)
                        .setView(view)
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .setPositiveButton(
                                R.string.dialog_ok, (d, which) -> onPicked.picked(picker.mColor))
                        .create();
        dialog.show();
        return dialog;
    }

    private void fromPicker() {
        this.mColor = Color.HSVToColor(this.mHsv);
        showColor(true);
    }

    private void fromHex(String hex) {
        if (hex.length() != 6) {
            return;
        }
        try {
            setColor(0xFF000000 | Integer.parseInt(hex, 16), false);
        } catch (NumberFormatException e) {
            // Not hex yet; the picker keeps the last valid color.
        }
    }

    private void setColor(int color, boolean writeHex) {
        this.mColor = color;
        Color.colorToHSV(color, this.mHsv);
        this.mHue.setHue(this.mHsv[0]);
        this.mSaturationValue.setHue(this.mHsv[0]);
        this.mSaturationValue.setSaturationValue(this.mHsv[1], this.mHsv[2]);
        showColor(writeHex);
    }

    private void showColor(boolean writeHex) {
        this.mPreview.setBackground(ColorSwatchDialog.circle(this.mPreview.getContext(), this.mColor));
        if (writeHex) {
            this.mWritingHex = true;
            this.mHex.setText(String.format("%06X", this.mColor & 0xFFFFFF));
            this.mWritingHex = false;
        }
    }
}
