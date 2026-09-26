package com.noname81.lmt;

import androidx.preference.PreferenceDataStore;

/**
 * Backs the 8 boolean-toggle preferences in {@link SettingsViewFragment} with {@link
 * SettingsValues}, reproducing the exact save call and side effects {@code SettingsViewHelper} used
 * to perform per tapped position. Every other preference on the settings screen is non-persistent
 * and reads/writes {@link SettingsValues} directly from its own click listener - see {@link
 * SettingsViewFragment} for why only these 8 go through a real {@code PreferenceDataStore}.
 */
class SettingsPreferenceDataStore extends PreferenceDataStore {
    private final SettingsValues mSettings;
    private final TouchServiceNative mTouchServiceNative;

    SettingsPreferenceDataStore(SettingsValues settings, TouchServiceNative touchServiceNative) {
        this.mSettings = settings;
        this.mTouchServiceNative = touchServiceNative;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        switch (key) {
            case "Autostart":
                return this.mSettings.loadAutostart() != 0;
            case "STSupport":
                return this.mSettings.loadSingleTouchGestureSupport() != 0;
            case "PieBehindKeyboard":
                return this.mSettings.loadPieAreaBehindKeyboard() != 0;
            case "PieOnLockScreen":
                return this.mSettings.loadPieOnLockScreen() != 0;
            case "PieRotateImages":
                return this.mSettings.loadPieRotateImages() != 0;
            case "PieExpandTriggerArea":
                return this.mSettings.loadPieExpandTriggerArea() != 0;
            case "PiePointerFromEdges":
                return this.mSettings.loadPiePointerFromEdges() != 0;
            case "PieMultiCommand":
                return this.mSettings.loadPieMultiCommand() != 0;
            case "PieBrightnessDial":
                return this.mSettings.loadPieBrightnessDial() != 0;
            case "PieBlurLive":
                // Off wherever it can't work, whatever a restored backup says.
                return SamsungBlur.isAvailable() && this.mSettings.loadPieBlurLive() != 0;
            default:
                return defValue;
        }
    }

    @Override
    public void putBoolean(String key, boolean value) {
        int v = value ? 1 : 0;
        switch (key) {
            case "Autostart":
                // Never restarted the service in either of its two old write paths.
                this.mSettings.saveAutostart(v);
                return;
            case "STSupport":
                this.mSettings.saveSingleTouchGestureSupport(v);
                this.mTouchServiceNative.setSingleTouchGestureSupport(v);
                this.mSettings.restartServiceIfRequired();
                return;
            case "PieBehindKeyboard":
                this.mSettings.savePieAreaBehindKeyboard(v);
                this.mSettings.restartServiceIfRequired();
                return;
            case "PieOnLockScreen":
                this.mSettings.savePieOnLockScreen(v);
                this.mSettings.restartServiceIfRequired();
                return;
            case "PieRotateImages":
                this.mSettings.savePieRotateImages(v);
                this.mSettings.restartServiceIfRequired();
                return;
            case "PieExpandTriggerArea":
                this.mSettings.savePieExpandTriggerArea(v);
                this.mSettings.restartServiceIfRequired();
                return;
            case "PiePointerFromEdges":
                this.mSettings.savePiePointerFromEdges(v);
                this.mSettings.restartServiceIfRequired();
                return;
            case "PieMultiCommand":
                this.mSettings.savePieMultiCommand(v);
                this.mSettings.restartServiceIfRequired();
                return;
            case "PieBrightnessDial":
                // Read each time the pie opens, so no restart.
                this.mSettings.savePieBrightnessDial(v);
                return;
            case "PieBlurLive":
                // The live blur stays set on the pie's blur view once applied, so switching
                // back to the screenshot only takes effect with a fresh pie: restart.
                this.mSettings.savePieBlurLive(v);
                this.mSettings.restartServiceIfRequired();
                return;
            default:
        }
    }
}
