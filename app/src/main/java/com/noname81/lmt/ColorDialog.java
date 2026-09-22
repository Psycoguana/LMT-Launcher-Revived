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

/* JADX INFO: loaded from: classes.dex */
abstract class ColorDialog extends AlertDialog.Builder
        implements DialogInterface.OnClickListener,
                View.OnClickListener,
                SeekBar.OnSeekBarChangeListener {
    static final /* synthetic */ boolean $assertionsDisabled = false;
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

    public abstract boolean onOkClicked(String str);

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {}

    ColorDialog(Context context, String str, String[] strArr, String str2) {
        super(context);
        setTitle(str);
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
        if (strArr.length == 1) {
            this.mColorLast.setVisibility(4);
        }
        Button button3 = (Button) viewInflate.findViewById(R.id.color_next);
        this.mColorNext = button3;
        button3.setText(">");
        this.mColorNext.setOnClickListener(this);
        if (strArr.length == 1) {
            this.mColorNext.setVisibility(4);
        }
        EditText editText = (EditText) viewInflate.findViewById(R.id.color_string);
        this.mColorString = editText;
        editText.setText(str2);
        this.mColorSelectionCurrent = 0;
        this.mColorSelectionStrings = strArr;
        this.mColorSelectionValues = new int[strArr.length];
        parseColorString(str2);
        updateColorPreviews();
        setPositiveButton(R.string.dialog_ok, this);
        setNegativeButton(R.string.dialog_cancel, this);
    }

    private void onCancelClicked(DialogInterface dialogInterface) {
        dialogInterface.dismiss();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            if (onOkClicked(this.mColorString.getText().toString())) {
                dialogInterface.dismiss();
            }
        } else if (i == -2) {
            onCancelClicked(dialogInterface);
        } else if (onOkClicked(Integer.toString(i))) {
            dialogInterface.dismiss();
        }
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        if (z) {
            if (seekBar.getId() == R.id.alpha_seek) {
                this.mAlphaValue.setText(Integer.toString(i));
            } else if (seekBar.getId() == R.id.red_seek) {
                this.mRedValue.setText(Integer.toString(i));
            } else if (seekBar.getId() == R.id.green_seek) {
                this.mGreenValue.setText(Integer.toString(i));
            } else if (seekBar.getId() == R.id.blue_seek) {
                this.mBlueValue.setText(Integer.toString(i));
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
            int i = this.mColorSelectionCurrent;
            if (i == 0) {
                this.mColorSelectionCurrent = this.mColorSelectionStrings.length - 1;
            } else {
                this.mColorSelectionCurrent = i - 1;
            }
            updateColorPreviews();
            return;
        }
        if (view.getId() == R.id.color_next) {
            int i2 = this.mColorSelectionCurrent;
            String[] strArr = this.mColorSelectionStrings;
            int i3 = 0;
            if (i2 >= strArr.length) {
                this.mColorSelectionCurrent = 0;
            } else {
                this.mColorSelectionCurrent = i2 + 1;
            }
            int i4 = this.mColorSelectionCurrent;
            if (i4 < strArr.length) {
                this.mColorSelectionCurrent = i4 + 1;
                i3 = i4;
            }
            this.mColorSelectionCurrent = i3;
            updateColorPreviews();
        }
    }

    private void parseColorString(String str) {
        if (str.length() == 1) {
            Arrays.fill(this.mColorSelectionValues, -2);
            return;
        }
        try {
            String[] strArrSplit = str.replace(" ", "").split(",");
            for (int i = 0; i < this.mColorSelectionValues.length; i++) {
                if (strArrSplit.length > i && strArrSplit[i].length() != 0) {
                    this.mColorSelectionValues[i] = Color.parseColor(strArrSplit[i]);
                } else {
                    this.mColorSelectionValues[i] = -2;
                }
            }
        } catch (Exception unused) {
            Arrays.fill(this.mColorSelectionValues, -2);
        }
    }

    private void updateColorPreviews() {
        int i = this.mColorSelectionValues[this.mColorSelectionCurrent];
        if (i == -2) {
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
            this.mAlphaValue.setText(Integer.toString(Color.alpha(i)));
            this.mAlphaSeek.setProgress(Color.alpha(i));
            this.mRedValue.setText(Integer.toString(Color.red(i)));
            this.mRedSeek.setProgress(Color.red(i));
            this.mGreenValue.setText(Integer.toString(Color.green(i)));
            this.mGreenSeek.setProgress(Color.green(i));
            this.mBlueValue.setText(Integer.toString(Color.blue(i)));
            this.mBlueSeek.setProgress(Color.blue(i));
            this.mColorPreview.setBackgroundColor(
                    this.mColorSelectionValues[this.mColorSelectionCurrent]);
        }
        this.mColorPreview.setText(this.mColorSelectionStrings[this.mColorSelectionCurrent]);
        StringBuilder sb = new StringBuilder();
        boolean z = false;
        for (int i2 = 0; i2 < this.mColorSelectionValues.length; i2++) {
            if (i2 != 0) {
                sb.append(",");
            }
            int i3 = this.mColorSelectionValues[i2];
            if (i3 != -2) {
                sb.append(
                        String.format(
                                "#%02x%02x%02x%02x",
                                Integer.valueOf(Color.alpha(i3)),
                                Integer.valueOf(Color.red(this.mColorSelectionValues[i2])),
                                Integer.valueOf(Color.green(this.mColorSelectionValues[i2])),
                                Integer.valueOf(Color.blue(this.mColorSelectionValues[i2]))));
                z = true;
            }
        }
        if (z) {
            this.mColorString.setText(sb.toString());
        }
    }
}
