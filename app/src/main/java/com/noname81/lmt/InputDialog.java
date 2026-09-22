package com.noname81.lmt;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

/* JADX INFO: loaded from: classes.dex */
abstract class InputDialog extends AlertDialog.Builder implements DialogInterface.OnClickListener {
    Context mContext;
    private final EditText mInput;

    public abstract boolean onOkClicked(String str);

    InputDialog(Context context, String str, String str2, String str3, boolean z) {
        super(context);
        this.mContext = context;
        setTitle(str);
        setMessage(str2);
        EditText editText = new EditText(context);
        this.mInput = editText;
        setView(editText);
        if (str3 != null) {
            editText.setText(str3);
            if (z) {
                editText.setInputType(2);
            }
            editText.setSelection(str3.length());
        }
        setPositiveButton(R.string.dialog_ok, this);
        setNegativeButton(R.string.dialog_cancel, this);
    }

    InputDialog(Context context, String str, String[] strArr, String str2) {
        super(context);
        this.mContext = context;
        setTitle(str);
        int i;
        try {
            i = Integer.parseInt(str2);
        } catch (NumberFormatException unused) {
            i = -1;
        }
        setSingleChoiceItems(strArr, i, this);
        this.mInput = null;
    }

    private void onCancelClicked(DialogInterface dialogInterface) {
        dialogInterface.dismiss();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            if (onOkClicked(this.mInput.getText().toString())) {
                dialogInterface.dismiss();
            }
        } else if (i == -2) {
            onCancelClicked(dialogInterface);
        } else if (onOkClicked(Integer.toString(i))) {
            dialogInterface.dismiss();
        }
    }
}
