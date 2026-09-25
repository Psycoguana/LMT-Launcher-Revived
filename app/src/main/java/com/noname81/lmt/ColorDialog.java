package com.noname81.lmt;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;

import java.util.Arrays;

/**
 * Dialog for picking the pie's colours: a set of ARGB sliders under a live preview of what they
 * color (a pie slice, the pointer or the status infos).
 *
 * <p>Colours are stored as a comma-separated string rather than an int, so a scheme can specify
 * only the parts it cares about and leave the rest at their defaults — see {@code PieMenu.init} for
 * how the string is parsed.
 */
abstract class ColorDialog extends MaterialAlertDialogBuilder
        implements DialogInterface.OnClickListener, View.OnClickListener, Slider.OnChangeListener {
    private TextView mAlphaLabel;
    private Slider mAlphaSeek;
    private TextView mAlphaValue;
    private TextView mBlueLabel;
    private Slider mBlueSeek;
    private TextView mBlueValue;
    private Button mColorLast;
    private Button mColorNext;
    private int mColorSelectionCurrent;
    private String[] mColorSelectionStrings;
    private int[] mColorSelectionValues;
    private EditText mColorString;
    private TextView mGreenLabel;
    private Slider mGreenSeek;
    private TextView mGreenValue;
    private TextView mRedLabel;
    private ColorPreview mPreview;
    private TextView mColorLabel;
    private boolean mUpdating;
    private Slider mRedSeek;
    private TextView mRedValue;

    /** What the editor previews its colors on (ROADMAP 1.13). */
    static final int PREVIEW_PIE = 0;

    static final int PREVIEW_POINTER = 1;
    static final int PREVIEW_STATUS_INFOS = 2;

    public abstract boolean onOkClicked(String colorString);

    ColorDialog(Context context, String title, String[] labels, String colorString, int preview) {
        super(context);
        setTitle(title);
        View viewInflate =
                LayoutInflater.from(getContext()).inflate(R.layout.color, (ViewGroup) null);
        setView(viewInflate);
        this.mAlphaLabel = (TextView) viewInflate.findViewById(R.id.alpha_label);
        this.mAlphaValue = (TextView) viewInflate.findViewById(R.id.alpha_value);
        this.mAlphaSeek = (Slider) viewInflate.findViewById(R.id.alpha_seek);
        this.mAlphaLabel.setText(R.string.alpha_string);
        this.mAlphaSeek.addOnChangeListener(this);
        this.mRedLabel = (TextView) viewInflate.findViewById(R.id.red_label);
        this.mRedValue = (TextView) viewInflate.findViewById(R.id.red_value);
        this.mRedSeek = (Slider) viewInflate.findViewById(R.id.red_seek);
        this.mRedLabel.setText(R.string.red_string);
        this.mRedSeek.addOnChangeListener(this);
        this.mGreenLabel = (TextView) viewInflate.findViewById(R.id.green_label);
        this.mGreenValue = (TextView) viewInflate.findViewById(R.id.green_value);
        this.mGreenSeek = (Slider) viewInflate.findViewById(R.id.green_seek);
        this.mGreenLabel.setText(R.string.green_string);
        this.mGreenSeek.addOnChangeListener(this);
        this.mBlueLabel = (TextView) viewInflate.findViewById(R.id.blue_label);
        this.mBlueValue = (TextView) viewInflate.findViewById(R.id.blue_value);
        this.mBlueSeek = (Slider) viewInflate.findViewById(R.id.blue_seek);
        this.mBlueLabel.setText(R.string.blue_string);
        this.mBlueSeek.addOnChangeListener(this);
        Button button2 = (Button) viewInflate.findViewById(R.id.color_last);
        this.mColorLast = button2;
        this.mColorLast.setOnClickListener(this);
        if (labels.length == 1) {
            this.mColorLast.setVisibility(4);
        }
        Button button3 = (Button) viewInflate.findViewById(R.id.color_next);
        this.mColorNext = button3;
        this.mColorNext.setOnClickListener(this);
        if (labels.length == 1) {
            this.mColorNext.setVisibility(4);
        }
        EditText editText = (EditText) viewInflate.findViewById(R.id.color_string);
        this.mColorString = editText;
        editText.setText(colorString);
        View previewView =
                viewInflate.findViewById(
                        preview == PREVIEW_POINTER
                                ? R.id.pointer_preview
                                : preview == PREVIEW_STATUS_INFOS
                                        ? R.id.status_info_preview
                                        : R.id.pie_tile);
        previewView.setVisibility(View.VISIBLE);
        this.mPreview = (ColorPreview) previewView;
        this.mColorLabel = viewInflate.findViewById(R.id.color_label);
        // A color string typed or pasted by hand updates the sliders and the preview too.
        editText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (!ColorDialog.this.mUpdating && s.toString().contains("#")) {
                            parseColorString(s.toString());
                            updateColorPreviews();
                        }
                    }
                });
        this.mColorSelectionCurrent = 0;
        this.mColorSelectionStrings = labels;
        this.mColorSelectionValues = new int[labels.length];
        parseColorString(colorString);
        updateColorPreviews();
        setPositiveButton(R.string.dialog_ok, this);
        setNegativeButton(R.string.dialog_cancel, this);
    }

    private void onCancelClicked(DialogInterface dialogInterface) {
        dialogInterface.dismiss();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            if (onOkClicked(this.mColorString.getText().toString())) {
                dialog.dismiss();
            }
        } else if (which == -2) {
            onCancelClicked(dialog);
        } else if (onOkClicked(Integer.toString(which))) {
            dialog.dismiss();
        }
    }

    @Override
    public void onValueChange(Slider slider, float value, boolean fromUser) {
        if (fromUser) {
            int progress = (int) value;
            if (slider.getId() == R.id.alpha_seek) {
                this.mAlphaValue.setText(Integer.toString(progress));
            } else if (slider.getId() == R.id.red_seek) {
                this.mRedValue.setText(Integer.toString(progress));
            } else if (slider.getId() == R.id.green_seek) {
                this.mGreenValue.setText(Integer.toString(progress));
            } else if (slider.getId() == R.id.blue_seek) {
                this.mBlueValue.setText(Integer.toString(progress));
            }
            int iArgb =
                    Color.argb(
                            progressOf(this.mAlphaSeek),
                            progressOf(this.mRedSeek),
                            progressOf(this.mGreenSeek),
                            progressOf(this.mBlueSeek));
            this.mColorSelectionValues[this.mColorSelectionCurrent] = iArgb;
            updateColorPreviews();
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.color_last) {
            int selection = this.mColorSelectionCurrent;
            if (selection == 0) {
                this.mColorSelectionCurrent = this.mColorSelectionStrings.length - 1;
            } else {
                this.mColorSelectionCurrent = selection - 1;
            }
            updateColorPreviews();
            return;
        }
        if (view.getId() == R.id.color_next) {
            int selection = this.mColorSelectionCurrent;
            String[] strArr = this.mColorSelectionStrings;
            int index = 0;
            if (selection >= strArr.length) {
                this.mColorSelectionCurrent = 0;
            } else {
                this.mColorSelectionCurrent = selection + 1;
            }
            int current = this.mColorSelectionCurrent;
            if (current < strArr.length) {
                this.mColorSelectionCurrent = current + 1;
                index = current;
            }
            this.mColorSelectionCurrent = index;
            updateColorPreviews();
        }
    }

    private static int progressOf(Slider slider) {
        return (int) slider.getValue();
    }

    private void parseColorString(String colorString) {
        if (colorString.length() == 1) {
            Arrays.fill(this.mColorSelectionValues, -2);
            return;
        }
        try {
            String[] strArrSplit = colorString.replace(" ", "").split(",");
            for (int index = 0; index < this.mColorSelectionValues.length; index++) {
                if (strArrSplit.length > index && strArrSplit[index].length() != 0) {
                    this.mColorSelectionValues[index] = Color.parseColor(strArrSplit[index]);
                } else {
                    this.mColorSelectionValues[index] = -2;
                }
            }
        } catch (Exception unused) {
            Arrays.fill(this.mColorSelectionValues, -2);
        }
    }

    private void updateColorPreviews() {
        this.mUpdating = true;
        try {
            showColors();
        } finally {
            this.mUpdating = false;
        }
        this.mPreview.setColors(this.mColorSelectionValues, this.mColorSelectionCurrent);
        this.mColorLabel.setText(this.mColorSelectionStrings[this.mColorSelectionCurrent]);
    }

    private void showColors() {
        int color = this.mColorSelectionValues[this.mColorSelectionCurrent];
        if (color == -2) {
            this.mAlphaSeek.setValue(255f);
            this.mAlphaValue.setText(Integer.toString(progressOf(this.mAlphaSeek)));
            this.mRedSeek.setValue(0f);
            this.mRedValue.setText(Integer.toString(progressOf(this.mRedSeek)));
            this.mGreenSeek.setValue(0f);
            this.mGreenValue.setText(Integer.toString(progressOf(this.mGreenSeek)));
            this.mBlueSeek.setValue(0f);
            this.mBlueValue.setText(Integer.toString(progressOf(this.mBlueSeek)));
        } else {
            this.mAlphaValue.setText(Integer.toString(Color.alpha(color)));
            this.mAlphaSeek.setValue((float) Color.alpha(color));
            this.mRedValue.setText(Integer.toString(Color.red(color)));
            this.mRedSeek.setValue((float) Color.red(color));
            this.mGreenValue.setText(Integer.toString(Color.green(color)));
            this.mGreenSeek.setValue((float) Color.green(color));
            this.mBlueValue.setText(Integer.toString(Color.blue(color)));
            this.mBlueSeek.setValue((float) Color.blue(color));
        }
        StringBuilder sb = new StringBuilder();
        boolean anyTransparent = false;
        for (int index = 0; index < this.mColorSelectionValues.length; index++) {
            if (index != 0) {
                sb.append(",");
            }
            int value = this.mColorSelectionValues[index];
            if (value != -2) {
                sb.append(
                        String.format(
                                "#%02x%02x%02x%02x",
                                Integer.valueOf(Color.alpha(value)),
                                Integer.valueOf(Color.red(this.mColorSelectionValues[index])),
                                Integer.valueOf(Color.green(this.mColorSelectionValues[index])),
                                Integer.valueOf(Color.blue(this.mColorSelectionValues[index]))));
                anyTransparent = true;
            }
        }
        // Only when it changed: rewriting what is being typed would move the cursor.
        if (anyTransparent && !sb.toString().equals(this.mColorString.getText().toString())) {
            this.mColorString.setText(sb.toString());
        }
    }
}
