package com.noname81.lmt;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Small modal that collects one value — free text, a number, or a single choice.
 *
 * <p>Subclasses override {@code onOkClicked} to validate and store; returning true dismisses.
 */
abstract class InputDialog extends MaterialAlertDialogBuilder implements DialogInterface.OnClickListener {
    Context mContext;
    private final EditText mInput;

    public abstract boolean onOkClicked(String value);

    InputDialog(
            Context context,
            String title,
            String message,
            String initialValue,
            boolean numericOnly) {
        super(context);
        this.mContext = context;
        setTitle(title);
        setMessage(message);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.input_dialog, null);
        EditText editText = view.findViewById(R.id.input_value);
        this.mInput = editText;
        setView(view);
        if (initialValue != null) {
            editText.setText(initialValue);
            if (numericOnly) {
                editText.setInputType(2);
            }
            editText.setSelection(initialValue.length());
        }
        setPositiveButton(R.string.dialog_ok, this);
        setNegativeButton(R.string.dialog_cancel, this);
    }

    InputDialog(Context context, String title, String[] choices, String selectedValue) {
        super(context);
        this.mContext = context;
        setTitle(title);
        int selectedIndex;
        try {
            selectedIndex = Integer.parseInt(selectedValue);
        } catch (NumberFormatException unused) {
            selectedIndex = -1;
        }
        setSingleChoiceItems(choices, selectedIndex, this);
        this.mInput = null;
    }

    private void onCancelClicked(DialogInterface dialogInterface) {
        dialogInterface.dismiss();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            if (onOkClicked(this.mInput.getText().toString())) {
                dialog.dismiss();
            }
        } else if (which == -2) {
            onCancelClicked(dialog);
        } else if (onOkClicked(Integer.toString(which))) {
            dialog.dismiss();
        }
    }
}
