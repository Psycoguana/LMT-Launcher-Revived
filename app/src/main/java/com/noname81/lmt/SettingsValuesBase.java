package com.noname81.lmt;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.ArrayList;
import java.util.Vector;

class SettingsValuesBase extends SettingsSharedPrefsWrapper {
    static final String path = "/Android/data/com.noname81.lmt/files/";
    ActivityManager mActivityManager;
    ArrayList<String> mBlacklist;
    ArrayList<String> mBlacklistPie;
    private int mCurrentGesture;
    private int mCurrentIsa;
    private int mCurrentPie;
    private Vector<Action> mGestureActions;
    private Vector<Action> mIsaActions;
    private Vector<Action> mPieActions;
    RootContext mRootContext;
    private int mServiceState;
    int mSingleSwipesAArea;
    float mTouchscreenScreenFactorX;
    float mTouchscreenScreenFactorY;

    private boolean getIsSmallScreen() {
        return false;
    }

    String loadResourceSearchPath() {
        String path2 = this.mContext.getExternalFilesDir(null).getPath();
        if (path2.endsWith("/")) {
            return path2;
        }
        return path2 + "/";
    }

    SettingsValuesBase(Context context) {
        super(context);
        this.mServiceState = 0;
        this.mRootContext = RootContext.getInstance(this.mContext);
        this.mCurrentGesture = -1;
        this.mGestureActions = new Vector<>();
        this.mCurrentPie = -1;
        this.mPieActions = new Vector<>();
        this.mCurrentIsa = -1;
        this.mIsaActions = new Vector<>();
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mBlacklist = new ArrayList<>();
        this.mBlacklistPie = new ArrayList<>();
        this.mSingleSwipesAArea = loadSingleSwipesAArea();
        this.mTouchscreenScreenFactorX = loadTouchscreenScreenFactorX();
        this.mTouchscreenScreenFactorY = loadTouchscreenScreenFactorY();
        loadActions();
        loadBlacklist();
        loadBlacklistPie();
    }

    int getServiceState() {
        return this.mServiceState;
    }

    void setServiceState(int state) {
        this.mServiceState = state;
    }

    void startService() {
        if (Build.VERSION.SDK_INT >= 26) {
            this.mContext.startForegroundService(
                    new Intent(this.mContext, (Class<?>) TouchService.class));
        } else {
            this.mContext.startService(new Intent(this.mContext, (Class<?>) TouchService.class));
        }
    }

    void stopService() {
        this.mContext.stopService(new Intent(this.mContext, (Class<?>) TouchService.class));
    }

    void restartServiceIfRequired() {
        if (this.mServiceState > 0) {
            stopService();
            startService();
        }
    }

    Action getGestureAction(int slot) {
        if (slot < this.mGestureActions.size()) {
            return this.mGestureActions.get(slot);
        }
        return new Action(1);
    }

    Action getPieAction(int slot) {
        if (slot < this.mPieActions.size()) {
            return this.mPieActions.get(slot);
        }
        return new Action(1);
    }

    Action getIsaAction(int slot) {
        if (slot < this.mIsaActions.size()) {
            return this.mIsaActions.get(slot);
        }
        return new Action(1);
    }

    Action getCurrentAction() {
        int gestureSlot = this.mCurrentGesture;
        if (gestureSlot >= 0) {
            return this.mGestureActions.get(gestureSlot);
        }
        int pieSlot = this.mCurrentPie;
        if (pieSlot >= 0) {
            return this.mPieActions.get(pieSlot);
        }
        int isaSlot = this.mCurrentIsa;
        if (isaSlot >= 0) {
            return this.mIsaActions.get(isaSlot);
        }
        return new Action(-1);
    }

    void setCurrentGesture(int slot) {
        this.mCurrentGesture = slot;
        this.mCurrentPie = -1;
        this.mCurrentIsa = -1;
    }

    void setCurrentPie(int slot) {
        this.mCurrentGesture = -1;
        this.mCurrentPie = slot;
        this.mCurrentIsa = -1;
    }

    void setCurrentIsa(int slot) {
        this.mCurrentGesture = -1;
        this.mCurrentPie = -1;
        this.mCurrentIsa = slot;
    }

    void setCurrentAction(Activity activity, Action action) {
        SharedPreferences.Editor editorCreateAndReturnSharedPreferencesEditor =
                createAndReturnSharedPreferencesEditor();
        int gestureSlot = this.mCurrentGesture;
        if (gestureSlot >= 0) {
            this.mGestureActions.setElementAt(action, gestureSlot);
            saveGestureAction(
                    this.mCurrentGesture, action, editorCreateAndReturnSharedPreferencesEditor);
        } else {
            int pieSlot = this.mCurrentPie;
            if (pieSlot >= 0) {
                this.mPieActions.setElementAt(action, pieSlot);
                savePieAction(
                        this.mCurrentPie, action, editorCreateAndReturnSharedPreferencesEditor);
                restartServiceIfRequired();
            } else {
                int isaSlot = this.mCurrentIsa;
                if (isaSlot >= 0) {
                    this.mIsaActions.setElementAt(action, isaSlot);
                    saveIsaAction(
                            this.mCurrentIsa, action, editorCreateAndReturnSharedPreferencesEditor);
                    restartServiceIfRequired();
                }
            }
        }
        if (editorCreateAndReturnSharedPreferencesEditor != null) {
            editorCreateAndReturnSharedPreferencesEditor.commit();
        }
        action.checkNeededPermissions(activity, true);
    }

    /**
     * Builds the in-memory slot vectors: one per recognised gesture, 38 pie slots (5 + 7 + 7
     * positions across three rings, each with a click and a long-press action) and 12 ISA zones.
     * {@code PieControl} walks the pie slots in hardcoded ranges, so these counts are a contract -
     * see SettingsSlotSchemaTest.
     */
    private void loadActions() {
        for (int gestureSlot = 0; gestureSlot < TouchServiceResult.names.length; gestureSlot++) {
            this.mGestureActions.add(loadGestureAction(gestureSlot));
        }
        for (int pieSlot = 0; pieSlot < 38; pieSlot++) {
            this.mPieActions.add(loadPieAction(pieSlot));
        }
        for (int isaSlot = 0; isaSlot < 12; isaSlot++) {
            this.mIsaActions.add(loadIsaAction(isaSlot));
        }
    }

    private Action loadGestureAction(int slot) {
        return new Action(
                loadInt(TouchServiceResult.names[slot] + " Type", 1),
                loadString(TouchServiceResult.names[slot] + " String", ""),
                loadString(TouchServiceResult.names[slot] + " Icon", ""),
                this.mContext);
    }

    private Action loadPieAction(int slot) {
        return new Action(
                loadInt("PieItem" + slot + " Type", 1),
                loadString("PieItem" + slot + " String", ""),
                loadString("PieItem" + slot + " Icon", ""),
                this.mContext);
    }

    private Action loadIsaAction(int slot) {
        return new Action(
                loadInt("IsaItem" + slot + " Type", 1),
                loadString("IsaItem" + slot + " String", ""),
                loadString("IsaItem" + slot + " Icon", ""),
                this.mContext);
    }

    private void saveGestureAction(int slot, Action action, SharedPreferences.Editor editor) {
        saveInt(TouchServiceResult.names[slot] + " Type", action.getType(), editor);
        saveString(TouchServiceResult.names[slot] + " String", action.getString(), editor);
        saveString(
                TouchServiceResult.names[slot] + " Icon",
                IconUtils.convertDrawableToBase64String(action.getIcon()),
                editor);
    }

    private void savePieAction(int slot, Action action, SharedPreferences.Editor editor) {
        saveInt("PieItem" + slot + " Type", action.getType(), editor);
        saveString("PieItem" + slot + " String", action.getString(), editor);
        saveString(
                "PieItem" + slot + " Icon",
                IconUtils.convertDrawableToBase64String(action.getIcon()),
                editor);
    }

    private void saveIsaAction(int slot, Action action, SharedPreferences.Editor editor) {
        saveInt("IsaItem" + slot + " Type", action.getType(), editor);
        saveString("IsaItem" + slot + " String", action.getString(), editor);
        saveString(
                "IsaItem" + slot + " Icon",
                IconUtils.convertDrawableToBase64String(action.getIcon()),
                editor);
    }

    int loadFeedbackMode() {
        return loadInt("Feedback", 3);
    }

    void saveFeedbackMode(int value) {
        saveInt("Feedback", value, null);
    }

    int loadGestureVibrationTime() {
        return loadInt("Vibration", 30);
    }

    void saveGestureVibrationTime(int value) {
        saveInt("Vibration", value, null);
    }

    int loadPieVibrationTime() {
        return loadInt("PieVibration", 30);
    }

    void savePieVibrationTime(int value) {
        saveInt("PieVibration", value, null);
    }

    int loadInputDevice() {
        return loadInt("Input", 4);
    }

    String loadInputDeviceString() {
        return "/dev/input/event" + loadInt("Input", 4);
    }

    void saveInputDevice(int value) {
        saveInt("Input", value, null);
    }

    void saveResourceSearchPath(String value) {
        if (value == null || value.length() == 0) {
            value = this.mContext.getExternalFilesDir(null).getPath();
        }
        if (!value.endsWith("/")) {
            value = value + "/";
        }
        saveString("ResourceSearchPath", value, null);
    }

    int loadSingleTouchGestureSupport() {
        return loadInt("STSupport", 1);
    }

    void saveSingleTouchGestureSupport(int value) {
        saveInt("STSupport", value, null);
    }

    int loadSingleSwipesBBox() {
        return loadInt("SingleSwipesBBox", 1);
    }

    void saveSingleSwipesBBox(int value) {
        saveInt("SingleSwipesBBox", value, null);
    }

    int loadSingleSwipesAArea() {
        int iLoadInt = loadInt("SingleSwipesAArea", 60);
        this.mSingleSwipesAArea = iLoadInt;
        return iLoadInt;
    }

    void saveSingleSwipesAArea(int value) {
        saveInt("SingleSwipesAArea", value, null);
        this.mSingleSwipesAArea = value;
    }

    float loadTouchscreenScreenFactorX() {
        float fLoadInt = loadInt("TouchscreenScreenFactorX", 100) / 100.0f;
        this.mTouchscreenScreenFactorX = fLoadInt;
        return fLoadInt;
    }

    void saveTouchscreenScreenFactorX(int value) {
        saveInt("TouchscreenScreenFactorX", value, null);
        this.mTouchscreenScreenFactorX = value / 100.0f;
    }

    float loadTouchscreenScreenFactorY() {
        float fLoadInt = loadInt("TouchscreenScreenFactorY", 100) / 100.0f;
        this.mTouchscreenScreenFactorY = fLoadInt;
        return fLoadInt;
    }

    void saveTouchscreenScreenFactorY(int value) {
        saveInt("TouchscreenScreenFactorY", value, null);
        this.mTouchscreenScreenFactorY = value / 100.0f;
    }

    int loadMinScore() {
        return loadInt("MinScore", 70);
    }

    void saveMinScore(int value) {
        saveInt("MinScore", value, null);
    }

    int loadMinPathLength() {
        return loadInt("MinPathLength", 7);
    }

    void saveMinPathLength(int value) {
        saveInt("MinPathLength", value, null);
    }

    int loadAutostart() {
        return loadInt("Autostart", 1);
    }

    void saveAutostart(int value) {
        saveInt("Autostart", value, null);
    }

    private void loadBlacklist() {
        for (String packageName : loadString("Blacklist", "").split(";")) {
            if (packageName.length() > 0) {
                this.mBlacklist.add(packageName);
            }
        }
    }

    private void loadBlacklistPie() {
        for (String packageName : loadString("BlacklistPie", "").split(";")) {
            if (packageName.length() > 0) {
                this.mBlacklistPie.add(packageName);
            }
        }
    }

    private void saveBlacklist() {
        StringBuilder sb = new StringBuilder();
        if (this.mBlacklist.size() > 0) {
            sb.append(this.mBlacklist.get(0));
            for (int index = 1; index < this.mBlacklist.size(); index++) {
                sb.append(";");
                sb.append(this.mBlacklist.get(index));
            }
        }
        saveString("Blacklist", sb.toString(), null);
    }

    private void saveBlacklistPie() {
        StringBuilder sb = new StringBuilder();
        if (this.mBlacklistPie.size() > 0) {
            sb.append(this.mBlacklistPie.get(0));
            for (int index = 1; index < this.mBlacklistPie.size(); index++) {
                sb.append(";");
                sb.append(this.mBlacklistPie.get(index));
            }
        }
        saveString("BlacklistPie", sb.toString(), null);
    }

    void setBlacklisted(String packageName) {
        this.mBlacklist.add(packageName);
        saveBlacklist();
    }

    void setBlacklistedPie(String packageName) {
        this.mBlacklistPie.add(packageName);
        saveBlacklistPie();
    }

    void clearBlacklisted() {
        this.mBlacklist.clear();
        saveBlacklist();
    }

    void clearBlacklistedPie() {
        this.mBlacklistPie.clear();
        saveBlacklistPie();
    }

    int loadTouchServiceMode() {
        return loadInt("TSMode", 2);
    }

    void saveTouchServiceMode(int value) {
        saveInt("TSMode", value, null);
    }

    int loadPiePos() {
        return loadInt("PiePos", 7);
    }

    void savePiePos(int value) {
        saveInt("PiePos", value, null);
    }

    int loadPieAreaX() {
        return loadInt("PieAreaX", 50);
    }

    void savePieAreaX(int value) {
        saveInt("PieAreaX", value, null);
    }

    int loadPieAreaY() {
        return loadInt("PieAreaY", getIsSmallScreen() ? 300 : 600);
    }

    void savePieAreaY(int value) {
        saveInt("PieAreaY", value, null);
    }

    int loadPieAreaGravity() {
        return loadInt("PieAreaGravity", 0);
    }

    void savePieAreaGravity(int value) {
        saveInt("PieAreaGravity", value, null);
    }

    String loadPieColor() {
        return loadString("PieColorString", "0");
    }

    void savePieColor(String value) {
        saveString("PieColorString", value, null);
    }

    String loadPieStatusInfoColor() {
        return loadString("PieStatusInfoColorString", "0");
    }

    void savePieStatusInfoColor(String value) {
        saveString("PieStatusInfoColorString", value, null);
    }

    String loadPiePointerColor() {
        return loadString("PiePointerColorString", "0");
    }

    void savePiePointerColor(String value) {
        saveString("PiePointerColorString", value, null);
    }

    int loadPieFont() {
        return loadInt("PieFont", getIsSmallScreen() ? 3 : 4);
    }

    void savePieFont(int value) {
        saveInt("PieFont", value, null);
    }

    int loadPieInnerRadius() {
        return loadInt("PieInnerRadius", getIsSmallScreen() ? 40 : 60);
    }

    void savePieInnerRadius(int value) {
        saveInt("PieInnerRadius", value, null);
    }

    int loadPieOuterRadius() {
        return loadInt("PieOuterRadius", getIsSmallScreen() ? 60 : 80);
    }

    void savePieOuterRadius(int value) {
        saveInt("PieOuterRadius", value, null);
    }

    int loadPieShiftSize() {
        return loadInt("PieShiftSize", 0);
    }

    void savePieShiftSize(int value) {
        saveInt("PieShiftSize", value, null);
    }

    int loadPieOutlineSize() {
        return loadInt("PieOutlineSize", 3);
    }

    void savePieOutlineSize(int value) {
        saveInt("PieOutlineSize", value, null);
    }

    int loadPieSliceGap() {
        return loadInt("PieSliceGap", 0);
    }

    void savePieSliceGap(int value) {
        saveInt("PieSliceGap", value, null);
    }

    int loadPieStartGap() {
        return loadInt("PieStartGap", 0);
    }

    void savePieStartGap(int value) {
        saveInt("PieStartGap", value, null);
    }

    int loadPieRotateImages() {
        return loadInt("PieRotateImages", 1);
    }

    void savePieRotateImages(int value) {
        saveInt("PieRotateImages", value, null);
    }

    int loadPieLongpress() {
        return loadInt("PieLongpress", 500);
    }

    void savePieLongpress(int value) {
        saveInt("PieLongpress", value, null);
    }

    int loadPieAnimation() {
        return loadInt("PieAnimation", 80);
    }

    void savePieAnimation(int value) {
        saveInt("PieAnimation", value, null);
    }

    int loadPieVibrate() {
        return loadInt("PieVibrate", 1);
    }

    void savePieVibrate(int value) {
        saveInt("PieVibrate", value, null);
    }

    int loadPieMultiCommand() {
        return loadInt("PieMultiCommand", 0);
    }

    void savePieMultiCommand(int value) {
        saveInt("PieMultiCommand", value, null);
    }

    int loadPiePointerFromEdges() {
        return loadInt("PiePointerFromEdges", 0);
    }

    void savePiePointerFromEdges(int value) {
        saveInt("PiePointerFromEdges", value, null);
    }

    int loadPiePointerWarpFactor() {
        return loadInt("PiePointerWarpFactorPercent", 300);
    }

    void savePiePointerWarpFactor(int value) {
        if (value < 200) {
            value = 200;
        }
        if (value > 1000) {
            value = 1000;
        }
        saveInt("PiePointerWarpFactorPercent", value, null);
    }

    int loadPieShowStatusInfos() {
        return loadInt("PieShowStatusInfos", 0);
    }

    void savePieShowStatusInfos(int value) {
        saveInt("PieShowStatusInfos", value, null);
    }

    int loadPieShowScaleAppImages() {
        return loadInt("PieShowAppImages", 1);
    }

    void savePieShowScaleAppImages(int value) {
        saveInt("PieShowAppImages", value, null);
    }

    int loadPieShowScaleUserImages() {
        return loadInt("PieUserImageScaling", 0);
    }

    void savePieShowScaleUserImages(int value) {
        saveInt("PieUserImageScaling", value, null);
    }

    int loadPieNavButtonStyle() {
        return loadInt("NavButtonStyle", 0);
    }

    void savePieNavButtonsStyle(int value) {
        saveInt("NavButtonStyle", value, null);
    }

    int loadPieExpandTriggerArea() {
        return loadInt("PieExpandTriggerArea", 1);
    }

    void savePieExpandTriggerArea(int value) {
        saveInt("PieExpandTriggerArea", value, null);
    }

    int loadPieAreaBehindKeyboard() {
        return loadInt("PieBehindKeyboard", 0);
    }

    void savePieAreaBehindKeyboard(int value) {
        saveInt("PieBehindKeyboard", value, null);
    }

    int loadPieOnLockScreen() {
        return loadInt("PieOnLockScreen", 0);
    }

    void savePieOnLockScreen(int value) {
        saveInt("PieOnLockScreen", value, null);
    }
}
