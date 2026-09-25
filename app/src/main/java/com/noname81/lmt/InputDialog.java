package com.noname81.lmt;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
        this(context, title, choices, selectedValue, null);
    }

    /** A single choice where {@code enabled[i] == false} greys out and blocks choice i. */
    InputDialog(
            Context context,
            String title,
            String[] choices,
            String selectedValue,
            boolean[] enabled) {
        super(context);
        this.mContext = context;
        setTitle(title);
        int selectedIndex;
        try {
            selectedIndex = Integer.parseInt(selectedValue);
        } catch (NumberFormatException unused) {
            selectedIndex = -1;
        }
        if (enabled == null) {
            setSingleChoiceItems(choices, selectedIndex, this);
        } else {
            setSingleChoiceItems(
                    new ArrayAdapter<String>(
                            getContext(), R.layout.dialog_singlechoice_item, android.R.id.text1, choices) {
                        @Override
                        public boolean areAllItemsEnabled() {
                            return false;
                        }

                        @Override
                        public boolean isEnabled(int position) {
                            return enabled[position];
                        }

                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            view.setEnabled(enabled[position]);
                            view.setAlpha(
                                    enabled[position] ? 1f : CommandSelectActivity.DISABLED_ALPHA);
                            return view;
                        }
                    },
                    selectedIndex,
                    this);
        }
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
