package com.noname81.lmt;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Arrays;

abstract class ColorDialog extends AlertDialog.Builder
        implements DialogInterface.OnClickListener,
                View.OnClickListener,
                SeekBar.OnSeekBarChangeListener {
    private TextView mAlphaLabel;
    private SeekBar mAlphaSeek;
    private TextView mAlphaValue;
    private TextView mBlueLabel;
    private SeekBar mBlueSeek;
    private TextView mBlueValue;
    private Button mColorLast;
    private Button mColorNext;
    private Button mColorPreview;
    private int mColorSelectionCurrent;
    private String[] mColorSelectionStrings;
    private int[] mColorSelectionValues;
    private EditText mColorString;
    private TextView mGreenLabel;
    private SeekBar mGreenSeek;
    private TextView mGreenValue;
    private TextView mRedLabel;
    private SeekBar mRedSeek;
    private TextView mRedValue;

    public abstract boolean onOkClicked(String colorString);

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {}

    ColorDialog(Context context, String title, String[] labels, String colorString) {
        super(context);
        setTitle(title);
        View viewInflate =
                ((LayoutInflater) context.getSystemService("layout_inflater"))
                        .inflate(R.layout.color, (ViewGroup) null);
        setView(viewInflate);
        this.mAlphaLabel = (TextView) viewInflate.findViewById(R.id.alpha_label);
        this.mAlphaValue = (TextView) viewInflate.findViewById(R.id.alpha_value);
        this.mAlphaSeek = (SeekBar) viewInflate.findViewById(R.id.alpha_seek);
        this.mAlphaLabel.setText(R.string.alpha_string);
        this.mAlphaSeek.setOnSeekBarChangeListener(this);
        this.mRedLabel = (TextView) viewInflate.findViewById(R.id.red_label);
        this.mRedValue = (TextView) viewInflate.findViewById(R.id.red_value);
        this.mRedSeek = (SeekBar) viewInflate.findViewById(R.id.red_seek);
        this.mRedLabel.setText(R.string.red_string);
        this.mRedSeek.setOnSeekBarChangeListener(this);
        this.mGreenLabel = (TextView) viewInflate.findViewById(R.id.green_label);
        this.mGreenValue = (TextView) viewInflate.findViewById(R.id.green_value);
        this.mGreenSeek = (SeekBar) viewInflate.findViewById(R.id.green_seek);
        this.mGreenLabel.setText(R.string.green_string);
        this.mGreenSeek.setOnSeekBarChangeListener(this);
        this.mBlueLabel = (TextView) viewInflate.findViewById(R.id.blue_label);
        this.mBlueValue = (TextView) viewInflate.findViewById(R.id.blue_value);
        this.mBlueSeek = (SeekBar) viewInflate.findViewById(R.id.blue_seek);
        this.mBlueLabel.setText(R.string.blue_string);
        this.mBlueSeek.setOnSeekBarChangeListener(this);
        Button button = (Button) viewInflate.findViewById(R.id.color_preview);
        this.mColorPreview = button;
        button.setOnClickListener(this);
        Button button2 = (Button) viewInflate.findViewById(R.id.color_last);
        this.mColorLast = button2;
        button2.setText("<");
        this.mColorLast.setOnClickListener(this);
        if (labels.length == 1) {
            this.mColorLast.setVisibility(4);
        }
        Button button3 = (Button) viewInflate.findViewById(R.id.color_next);
        this.mColorNext = button3;
        button3.setText(">");
        this.mColorNext.setOnClickListener(this);
        if (labels.length == 1) {
            this.mColorNext.setVisibility(4);
        }
        EditText editText = (EditText) viewInflate.findViewById(R.id.color_string);
        this.mColorString = editText;
        editText.setText(colorString);
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

    @Override // android.content.DialogInterface.OnClickListener
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

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (seekBar.getId() == R.id.alpha_seek) {
                this.mAlphaValue.setText(Integer.toString(progress));
            } else if (seekBar.getId() == R.id.red_seek) {
                this.mRedValue.setText(Integer.toString(progress));
            } else if (seekBar.getId() == R.id.green_seek) {
                this.mGreenValue.setText(Integer.toString(progress));
            } else if (seekBar.getId() == R.id.blue_seek) {
                this.mBlueValue.setText(Integer.toString(progress));
            }
            int iArgb =
                    Color.argb(
                            this.mAlphaSeek.getProgress(),
                            this.mRedSeek.getProgress(),
                            this.mGreenSeek.getProgress(),
                            this.mBlueSeek.getProgress());
            this.mColorPreview.setBackgroundColor(iArgb);
            this.mColorSelectionValues[this.mColorSelectionCurrent] = iArgb;
            updateColorPreviews();
        }
    }

    @Override // android.view.View.OnClickListener
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
        int color = this.mColorSelectionValues[this.mColorSelectionCurrent];
        if (color == -2) {
            this.mAlphaSeek.setProgress(255);
            this.mAlphaValue.setText(Integer.toString(this.mAlphaSeek.getProgress()));
            this.mRedSeek.setProgress(0);
            this.mRedValue.setText(Integer.toString(this.mRedSeek.getProgress()));
            this.mGreenSeek.setProgress(0);
            this.mGreenValue.setText(Integer.toString(this.mGreenSeek.getProgress()));
            this.mBlueSeek.setProgress(0);
            this.mBlueValue.setText(Integer.toString(this.mBlueSeek.getProgress()));
            this.mColorPreview.setBackgroundColor(0xFF000000);
        } else {
            this.mAlphaValue.setText(Integer.toString(Color.alpha(color)));
            this.mAlphaSeek.setProgress(Color.alpha(color));
            this.mRedValue.setText(Integer.toString(Color.red(color)));
            this.mRedSeek.setProgress(Color.red(color));
            this.mGreenValue.setText(Integer.toString(Color.green(color)));
            this.mGreenSeek.setProgress(Color.green(color));
            this.mBlueValue.setText(Integer.toString(Color.blue(color)));
            this.mBlueSeek.setProgress(Color.blue(color));
            this.mColorPreview.setBackgroundColor(
                    this.mColorSelectionValues[this.mColorSelectionCurrent]);
        }
        this.mColorPreview.setText(this.mColorSelectionStrings[this.mColorSelectionCurrent]);
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
        if (anyTransparent) {
            this.mColorString.setText(sb.toString());
        }
    }
}
