package com.noname81.lmt;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

abstract class InputDialog extends AlertDialog.Builder implements DialogInterface.OnClickListener {
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
        EditText editText = new EditText(context);
        this.mInput = editText;
        setView(editText);
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
