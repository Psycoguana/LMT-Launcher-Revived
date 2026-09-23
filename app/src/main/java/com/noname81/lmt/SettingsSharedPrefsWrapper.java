package com.noname81.lmt;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

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

    String loadString(String key, String fallback) {
        SharedPreferences sharedPreferences = this.mSharedPreferences;
        if (sharedPreferences != null) {
            return sharedPreferences.getString(key, fallback);
        }
        String string = Settings.System.getString(this.mContext.getContentResolver(), key);
        return string == null ? fallback : string;
    }

    void saveString(String key, String value, SharedPreferences.Editor editor) {
        SharedPreferences sharedPreferences = this.mSharedPreferences;
        if (sharedPreferences == null) {
            Settings.System.putString(this.mContext.getContentResolver(), key, value);
        } else {
            if (editor == null) {
                SharedPreferences.Editor editorEdit = sharedPreferences.edit();
                editorEdit.putString(key, value);
                editorEdit.apply();
                return;
            }
            editor.putString(key, value);
        }
    }

    int loadInt(String key, int fallback) {
        SharedPreferences sharedPreferences = this.mSharedPreferences;
        if (sharedPreferences != null) {
            return sharedPreferences.getInt(key, fallback);
        }
        try {
            return Settings.System.getInt(this.mContext.getContentResolver(), key);
        } catch (Exception unused) {
            return fallback;
        }
    }

    void saveInt(String key, int value, SharedPreferences.Editor editor) {
        SharedPreferences sharedPreferences = this.mSharedPreferences;
        if (sharedPreferences == null) {
            Settings.System.putInt(this.mContext.getContentResolver(), key, value);
        } else {
            if (editor == null) {
                SharedPreferences.Editor editorEdit = sharedPreferences.edit();
                editorEdit.putInt(key, value);
                editorEdit.apply();
                return;
            }
            editor.putInt(key, value);
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
