package com.noname81.lmt;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

/* JADX INFO: loaded from: classes.dex */
class SettingsSharedPrefsWrapper {
    protected Context mContext;
    private SharedPreferences mSharedPreferences;

    SettingsSharedPrefsWrapper(Context context) {
        this.mContext = context;
        this.mSharedPreferences = null;
        if (Settings.System.getInt(context.getContentResolver(), "LMTExternalConfig", 0) == 0) {
            this.mSharedPreferences = this.mContext.getSharedPreferences("LMT", 0);
        }
    }

    String loadString(String str, String str2) {
        SharedPreferences sharedPreferences = this.mSharedPreferences;
        if (sharedPreferences != null) {
            return sharedPreferences.getString(str, str2);
        }
        String string = Settings.System.getString(this.mContext.getContentResolver(), str);
        return string == null ? str2 : string;
    }

    void saveString(String str, String str2, SharedPreferences.Editor editor) {
        SharedPreferences sharedPreferences = this.mSharedPreferences;
        if (sharedPreferences == null) {
            Settings.System.putString(this.mContext.getContentResolver(), str, str2);
        } else {
            if (editor == null) {
                SharedPreferences.Editor editorEdit = sharedPreferences.edit();
                editorEdit.putString(str, str2);
                editorEdit.apply();
                return;
            }
            editor.putString(str, str2);
        }
    }

    int loadInt(String str, int i) {
        SharedPreferences sharedPreferences = this.mSharedPreferences;
        if (sharedPreferences != null) {
            return sharedPreferences.getInt(str, i);
        }
        try {
            return Settings.System.getInt(this.mContext.getContentResolver(), str);
        } catch (Exception unused) {
            return i;
        }
    }

    void saveInt(String str, int i, SharedPreferences.Editor editor) {
        SharedPreferences sharedPreferences = this.mSharedPreferences;
        if (sharedPreferences == null) {
            Settings.System.putInt(this.mContext.getContentResolver(), str, i);
        } else {
            if (editor == null) {
                SharedPreferences.Editor editorEdit = sharedPreferences.edit();
                editorEdit.putInt(str, i);
                editorEdit.apply();
                return;
            }
            editor.putInt(str, i);
        }
    }

    SharedPreferences.Editor createAndReturnSharedPreferencesEditor() {
        SharedPreferences sharedPreferences = this.mSharedPreferences;
        if (sharedPreferences != null) {
            return sharedPreferences.edit();
        }
        return null;
    }
}
