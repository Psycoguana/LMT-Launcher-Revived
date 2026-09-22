package com.noname81.lmt;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.ArrayList;
import java.util.Vector;

/* JADX INFO: loaded from: classes.dex */
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

    void setServiceState(int i) {
        this.mServiceState = i;
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

    Action getGestureAction(int i) {
        if (i < this.mGestureActions.size()) {
            return this.mGestureActions.get(i);
        }
        return new Action(1);
    }

    Action getPieAction(int i) {
        if (i < this.mPieActions.size()) {
            return this.mPieActions.get(i);
        }
        return new Action(1);
    }

    Action getIsaAction(int i) {
        if (i < this.mIsaActions.size()) {
            return this.mIsaActions.get(i);
        }
        return new Action(1);
    }

    Action getCurrentAction() {
        int i = this.mCurrentGesture;
        if (i >= 0) {
            return this.mGestureActions.get(i);
        }
        int i2 = this.mCurrentPie;
        if (i2 >= 0) {
            return this.mPieActions.get(i2);
        }
        int i3 = this.mCurrentIsa;
        if (i3 >= 0) {
            return this.mIsaActions.get(i3);
        }
        return new Action(-1);
    }

    void setCurrentGesture(int i) {
        this.mCurrentGesture = i;
        this.mCurrentPie = -1;
        this.mCurrentIsa = -1;
    }

    void setCurrentPie(int i) {
        this.mCurrentGesture = -1;
        this.mCurrentPie = i;
        this.mCurrentIsa = -1;
    }

    void setCurrentIsa(int i) {
        this.mCurrentGesture = -1;
        this.mCurrentPie = -1;
        this.mCurrentIsa = i;
    }

    void setCurrentAction(Activity activity, Action action) {
        SharedPreferences.Editor editorCreateAndReturnSharedPreferencesEditor =
                createAndReturnSharedPreferencesEditor();
        int i = this.mCurrentGesture;
        if (i >= 0) {
            this.mGestureActions.setElementAt(action, i);
            saveGestureAction(
                    this.mCurrentGesture, action, editorCreateAndReturnSharedPreferencesEditor);
        } else {
            int i2 = this.mCurrentPie;
            if (i2 >= 0) {
                this.mPieActions.setElementAt(action, i2);
                savePieAction(
                        this.mCurrentPie, action, editorCreateAndReturnSharedPreferencesEditor);
                restartServiceIfRequired();
            } else {
                int i3 = this.mCurrentIsa;
                if (i3 >= 0) {
                    this.mIsaActions.setElementAt(action, i3);
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

    private void loadActions() {
        for (int i = 0; i < TouchServiceResult.names.length; i++) {
            this.mGestureActions.add(loadGestureAction(i));
        }
        for (int i2 = 0; i2 < 38; i2++) {
            this.mPieActions.add(loadPieAction(i2));
        }
        for (int i3 = 0; i3 < 12; i3++) {
            this.mIsaActions.add(loadIsaAction(i3));
        }
    }

    private Action loadGestureAction(int i) {
        return new Action(
                loadInt(TouchServiceResult.names[i] + " Type", 1),
                loadString(TouchServiceResult.names[i] + " String", ""),
                loadString(TouchServiceResult.names[i] + " Icon", ""),
                this.mContext);
    }

    private Action loadPieAction(int i) {
        return new Action(
                loadInt("PieItem" + i + " Type", 1),
                loadString("PieItem" + i + " String", ""),
                loadString("PieItem" + i + " Icon", ""),
                this.mContext);
    }

    private Action loadIsaAction(int i) {
        return new Action(
                loadInt("IsaItem" + i + " Type", 1),
                loadString("IsaItem" + i + " String", ""),
                loadString("IsaItem" + i + " Icon", ""),
                this.mContext);
    }

    private void saveGestureAction(int i, Action action, SharedPreferences.Editor editor) {
        saveInt(TouchServiceResult.names[i] + " Type", action.getType(), editor);
        saveString(TouchServiceResult.names[i] + " String", action.getString(), editor);
        saveString(
                TouchServiceResult.names[i] + " Icon",
                IconUtils.convertDrawableToBase64String(action.getIcon()),
                editor);
    }

    private void savePieAction(int i, Action action, SharedPreferences.Editor editor) {
        saveInt("PieItem" + i + " Type", action.getType(), editor);
        saveString("PieItem" + i + " String", action.getString(), editor);
        saveString(
                "PieItem" + i + " Icon",
                IconUtils.convertDrawableToBase64String(action.getIcon()),
                editor);
    }

    private void saveIsaAction(int i, Action action, SharedPreferences.Editor editor) {
        saveInt("IsaItem" + i + " Type", action.getType(), editor);
        saveString("IsaItem" + i + " String", action.getString(), editor);
        saveString(
                "IsaItem" + i + " Icon",
                IconUtils.convertDrawableToBase64String(action.getIcon()),
                editor);
    }

    int loadFeedbackMode() {
        return loadInt("Feedback", 3);
    }

    void saveFeedbackMode(int i) {
        saveInt("Feedback", i, null);
    }

    int loadGestureVibrationTime() {
        return loadInt("Vibration", 30);
    }

    void saveGestureVibrationTime(int i) {
        saveInt("Vibration", i, null);
    }

    int loadPieVibrationTime() {
        return loadInt("PieVibration", 30);
    }

    void savePieVibrationTime(int i) {
        saveInt("PieVibration", i, null);
    }

    int loadInputDevice() {
        return loadInt("Input", 4);
    }

    String loadInputDeviceString() {
        return "/dev/input/event" + loadInt("Input", 4);
    }

    void saveInputDevice(int i) {
        saveInt("Input", i, null);
    }

    void saveResourceSearchPath(String str) {
        if (str == null || str.length() == 0) {
            str = this.mContext.getExternalFilesDir(null).getPath();
        }
        if (!str.endsWith("/")) {
            str = str + "/";
        }
        saveString("ResourceSearchPath", str, null);
    }

    int loadSingleTouchGestureSupport() {
        return loadInt("STSupport", 1);
    }

    void saveSingleTouchGestureSupport(int i) {
        saveInt("STSupport", i, null);
    }

    int loadSingleSwipesBBox() {
        return loadInt("SingleSwipesBBox", 1);
    }

    void saveSingleSwipesBBox(int i) {
        saveInt("SingleSwipesBBox", i, null);
    }

    int loadSingleSwipesAArea() {
        int iLoadInt = loadInt("SingleSwipesAArea", 60);
        this.mSingleSwipesAArea = iLoadInt;
        return iLoadInt;
    }

    void saveSingleSwipesAArea(int i) {
        saveInt("SingleSwipesAArea", i, null);
        this.mSingleSwipesAArea = i;
    }

    float loadTouchscreenScreenFactorX() {
        float fLoadInt = loadInt("TouchscreenScreenFactorX", 100) / 100.0f;
        this.mTouchscreenScreenFactorX = fLoadInt;
        return fLoadInt;
    }

    void saveTouchscreenScreenFactorX(int i) {
        saveInt("TouchscreenScreenFactorX", i, null);
        this.mTouchscreenScreenFactorX = i / 100.0f;
    }

    float loadTouchscreenScreenFactorY() {
        float fLoadInt = loadInt("TouchscreenScreenFactorY", 100) / 100.0f;
        this.mTouchscreenScreenFactorY = fLoadInt;
        return fLoadInt;
    }

    void saveTouchscreenScreenFactorY(int i) {
        saveInt("TouchscreenScreenFactorY", i, null);
        this.mTouchscreenScreenFactorY = i / 100.0f;
    }

    int loadMinScore() {
        return loadInt("MinScore", 70);
    }

    void saveMinScore(int i) {
        saveInt("MinScore", i, null);
    }

    int loadMinPathLength() {
        return loadInt("MinPathLength", 7);
    }

    void saveMinPathLength(int i) {
        saveInt("MinPathLength", i, null);
    }

    int loadAutostart() {
        return loadInt("Autostart", 1);
    }

    void saveAutostart(int i) {
        saveInt("Autostart", i, null);
    }

    private void loadBlacklist() {
        for (String str : loadString("Blacklist", "").split(";")) {
            if (str.length() > 0) {
                this.mBlacklist.add(str);
            }
        }
    }

    private void loadBlacklistPie() {
        for (String str : loadString("BlacklistPie", "").split(";")) {
            if (str.length() > 0) {
                this.mBlacklistPie.add(str);
            }
        }
    }

    private void saveBlacklist() {
        StringBuilder sb = new StringBuilder();
        if (this.mBlacklist.size() > 0) {
            sb.append(this.mBlacklist.get(0));
            for (int i = 1; i < this.mBlacklist.size(); i++) {
                sb.append(";");
                sb.append(this.mBlacklist.get(i));
            }
        }
        saveString("Blacklist", sb.toString(), null);
    }

    private void saveBlacklistPie() {
        StringBuilder sb = new StringBuilder();
        if (this.mBlacklistPie.size() > 0) {
            sb.append(this.mBlacklistPie.get(0));
            for (int i = 1; i < this.mBlacklistPie.size(); i++) {
                sb.append(";");
                sb.append(this.mBlacklistPie.get(i));
            }
        }
        saveString("BlacklistPie", sb.toString(), null);
    }

    void setBlacklisted(String str) {
        this.mBlacklist.add(str);
        saveBlacklist();
    }

    void setBlacklistedPie(String str) {
        this.mBlacklistPie.add(str);
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

    void saveTouchServiceMode(int i) {
        saveInt("TSMode", i, null);
    }

    int loadPiePos() {
        return loadInt("PiePos", 7);
    }

    void savePiePos(int i) {
        saveInt("PiePos", i, null);
    }

    int loadPieAreaX() {
        return loadInt("PieAreaX", 50);
    }

    void savePieAreaX(int i) {
        saveInt("PieAreaX", i, null);
    }

    int loadPieAreaY() {
        return loadInt("PieAreaY", getIsSmallScreen() ? 300 : 600);
    }

    void savePieAreaY(int i) {
        saveInt("PieAreaY", i, null);
    }

    int loadPieAreaGravity() {
        return loadInt("PieAreaGravity", 0);
    }

    void savePieAreaGravity(int i) {
        saveInt("PieAreaGravity", i, null);
    }

    String loadPieColor() {
        return loadString("PieColorString", "0");
    }

    void savePieColor(String str) {
        saveString("PieColorString", str, null);
    }

    String loadPieStatusInfoColor() {
        return loadString("PieStatusInfoColorString", "0");
    }

    void savePieStatusInfoColor(String str) {
        saveString("PieStatusInfoColorString", str, null);
    }

    String loadPiePointerColor() {
        return loadString("PiePointerColorString", "0");
    }

    void savePiePointerColor(String str) {
        saveString("PiePointerColorString", str, null);
    }

    int loadPieFont() {
        return loadInt("PieFont", getIsSmallScreen() ? 3 : 4);
    }

    void savePieFont(int i) {
        saveInt("PieFont", i, null);
    }

    int loadPieInnerRadius() {
        return loadInt("PieInnerRadius", getIsSmallScreen() ? 40 : 60);
    }

    void savePieInnerRadius(int i) {
        saveInt("PieInnerRadius", i, null);
    }

    int loadPieOuterRadius() {
        return loadInt("PieOuterRadius", getIsSmallScreen() ? 60 : 80);
    }

    void savePieOuterRadius(int i) {
        saveInt("PieOuterRadius", i, null);
    }

    int loadPieShiftSize() {
        return loadInt("PieShiftSize", 0);
    }

    void savePieShiftSize(int i) {
        saveInt("PieShiftSize", i, null);
    }

    int loadPieOutlineSize() {
        return loadInt("PieOutlineSize", 3);
    }

    void savePieOutlineSize(int i) {
        saveInt("PieOutlineSize", i, null);
    }

    int loadPieSliceGap() {
        return loadInt("PieSliceGap", 0);
    }

    void savePieSliceGap(int i) {
        saveInt("PieSliceGap", i, null);
    }

    int loadPieStartGap() {
        return loadInt("PieStartGap", 0);
    }

    void savePieStartGap(int i) {
        saveInt("PieStartGap", i, null);
    }

    int loadPieRotateImages() {
        return loadInt("PieRotateImages", 1);
    }

    void savePieRotateImages(int i) {
        saveInt("PieRotateImages", i, null);
    }

    int loadPieLongpress() {
        return loadInt("PieLongpress", 500);
    }

    void savePieLongpress(int i) {
        saveInt("PieLongpress", i, null);
    }

    int loadPieAnimation() {
        return loadInt("PieAnimation", 80);
    }

    void savePieAnimation(int i) {
        saveInt("PieAnimation", i, null);
    }

    int loadPieVibrate() {
        return loadInt("PieVibrate", 1);
    }

    void savePieVibrate(int i) {
        saveInt("PieVibrate", i, null);
    }

    int loadPieMultiCommand() {
        return loadInt("PieMultiCommand", 0);
    }

    void savePieMultiCommand(int i) {
        saveInt("PieMultiCommand", i, null);
    }

    int loadPiePointerFromEdges() {
        return loadInt("PiePointerFromEdges", 0);
    }

    void savePiePointerFromEdges(int i) {
        saveInt("PiePointerFromEdges", i, null);
    }

    int loadPiePointerWarpFactor() {
        return loadInt("PiePointerWarpFactorPercent", 300);
    }

    void savePiePointerWarpFactor(int i) {
        if (i < 200) {
            i = 200;
        }
        if (i > 1000) {
            i = 1000;
        }
        saveInt("PiePointerWarpFactorPercent", i, null);
    }

    int loadPieShowStatusInfos() {
        return loadInt("PieShowStatusInfos", 0);
    }

    void savePieShowStatusInfos(int i) {
        saveInt("PieShowStatusInfos", i, null);
    }

    int loadPieShowScaleAppImages() {
        return loadInt("PieShowAppImages", 1);
    }

    void savePieShowScaleAppImages(int i) {
        saveInt("PieShowAppImages", i, null);
    }

    int loadPieShowScaleUserImages() {
        return loadInt("PieUserImageScaling", 0);
    }

    void savePieShowScaleUserImages(int i) {
        saveInt("PieUserImageScaling", i, null);
    }

    int loadPieNavButtonStyle() {
        return loadInt("NavButtonStyle", 0);
    }

    void savePieNavButtonsStyle(int i) {
        saveInt("NavButtonStyle", i, null);
    }

    int loadPieExpandTriggerArea() {
        return loadInt("PieExpandTriggerArea", 1);
    }

    void savePieExpandTriggerArea(int i) {
        saveInt("PieExpandTriggerArea", i, null);
    }

    int loadPieAreaBehindKeyboard() {
        return loadInt("PieBehindKeyboard", 0);
    }

    void savePieAreaBehindKeyboard(int i) {
        saveInt("PieBehindKeyboard", i, null);
    }

    int loadPieOnLockScreen() {
        return loadInt("PieOnLockScreen", 0);
    }

    void savePieOnLockScreen(int i) {
        saveInt("PieOnLockScreen", i, null);
    }
}
