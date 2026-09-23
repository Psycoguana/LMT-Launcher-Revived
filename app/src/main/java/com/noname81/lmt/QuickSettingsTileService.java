package com.noname81.lmt;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick Settings tile that starts and stops the service. */
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

    /**
     * @param state one of {@link Tile#STATE_INACTIVE} / {@link Tile#STATE_ACTIVE}.
     */
    private void updateTileState(int state) {
        Tile qsTile = getQsTile();
        if (qsTile != null) {
            qsTile.setState(state);
            qsTile.updateTile();
        }
    }
}
