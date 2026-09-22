package com.noname81.lmt;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/* JADX INFO: loaded from: classes.dex */
public class QuickSettingsTileService extends TileService {
    @Override // android.service.quicksettings.TileService
    public void onStartListening() {
        super.onStartListening();
        if (SettingsValues.getInstance(this).getServiceState() == 0) {
            updateTileState(1);
        } else {
            updateTileState(2);
        }
    }

    @Override // android.service.quicksettings.TileService
    public void onClick() {
        super.onClick();
        SettingsValues settingsValues = SettingsValues.getInstance(this);
        if (settingsValues.getServiceState() == 0) {
            settingsValues.startService();
            updateTileState(2);
        } else {
            settingsValues.stopService();
            updateTileState(1);
        }
    }

    private void updateTileState(int i) {
        Tile qsTile = getQsTile();
        if (qsTile != null) {
            qsTile.setState(i);
            qsTile.updateTile();
        }
    }
}
