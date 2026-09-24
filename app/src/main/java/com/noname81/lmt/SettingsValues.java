package com.noname81.lmt;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Settings plus the derived state the service needs at runtime.
 *
 * <p>Adds screen geometry, orientation and the recent-apps query on top of the raw accessors in
 * {@link SettingsValuesBase}.
 */
class SettingsValues extends SettingsValuesBase {
    private static SettingsValues instance;
    private int mOrientation;
    private int mScreenHeight;
    private int mScreenWidth;
    private boolean mVisiblePieActivationAreas;

    String getVersion() {
        // Upstream hardcoded this and kept it in step with the APK's versionName by
        // running sed over the smali at build time (see reference/build.sh). Reading
        // BuildConfig removes the drift permanently.
        return "v" + BuildConfig.VERSION_NAME;
    }

    private SettingsValues(Context context) {
        super(context);
        this.mVisiblePieActivationAreas = false;
    }

    static SettingsValues getInstance(Context context) {
        // IconLoader's worker threads call this too; unguarded, two threads can each build an
        // instance and the later write discards state (e.g. the selected slot) set on the first.
        synchronized (SettingsValues.class) {
            if (instance == null) {
                instance = new SettingsValues(context);
            }
            return instance;
        }
    }

    long getDays() {
        long jCurrentTimeMillis = 1641771032000L - System.currentTimeMillis();
        if (jCurrentTimeMillis > 0) {
            return (((jCurrentTimeMillis / 1000) / 60) / 60) / 24;
        }
        return 0L;
    }

    int getOrientation() {
        return this.mOrientation;
    }

    int getScreenHeight() {
        return this.mScreenHeight;
    }

    int getScreenWidth() {
        return this.mScreenWidth;
    }

    void rotate() {
        WindowManager windowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mOrientation = windowManager.getDefaultDisplay().getRotation();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        this.mScreenWidth = displayMetrics.widthPixels;
        this.mScreenHeight = displayMetrics.heightPixels;
    }

    boolean getVisiblePieActivationAreas() {
        return this.mVisiblePieActivationAreas;
    }

    void setVisiblePieActivationAread(boolean visible) {
        this.mVisiblePieActivationAreas = visible;
    }

    /**
     * Maps a swipe to one of the twelve Individual Swipe Areas: three zones on each screen edge,
     * chosen by where the swipe started and which direction it went.
     *
     * @param gesture the recognised swipe (14 left, 15 right, 16 up, 17 down)
     * @param rawX raw touchscreen coordinates, scaled here into screen space
     */
    Action getIsaAction(int gesture, float rawX, float rawY) {
        float x = rawX / this.mTouchscreenScreenFactorX;
        float y = rawY / this.mTouchscreenScreenFactorY;
        int orientation = this.mOrientation;
        if (orientation == 1 || orientation == 3) {
            y = this.mScreenHeight - y;
        }
        if (y > this.mScreenHeight - this.mSingleSwipesAArea && gesture == 16) {
            return getIsaAction((int) ((x * 3.0f) / this.mScreenWidth));
        }
        if (y < this.mSingleSwipesAArea && gesture == 17) {
            return getIsaAction(((int) ((x * 3.0f) / this.mScreenWidth)) + 3);
        }
        if (x < this.mSingleSwipesAArea && gesture == 14) {
            return getIsaAction(((int) ((y * 3.0f) / this.mScreenHeight)) + 6);
        }
        if (x > this.mScreenWidth - this.mSingleSwipesAArea && gesture == 15) {
            return getIsaAction(((int) ((y * 3.0f) / this.mScreenHeight)) + 9);
        }
        return new Action(1);
    }

    String getPackageNamesOfRecentApps(int wanted) {
        String packageName;
        int returned = 0;
        if (Build.VERSION.SDK_INT >= 21) {
            if (this.mRootContext.isRootAvailable(false)) {
                return this.mRootContext.runCommandRemoteResult(
                        "am get-recent-app " + wanted, true);
            }
            UsageStatsManager usageStatsManagerM283m =
                    ((UsageStatsManager) this.mContext.getSystemService("usagestats"));
            long jCurrentTimeMillis = System.currentTimeMillis();
            List listQueryUsageStats =
                    usageStatsManagerM283m.queryUsageStats(
                            0, jCurrentTimeMillis - TimeUnit.DAYS.toMillis(1L), jCurrentTimeMillis);
            if (listQueryUsageStats == null || listQueryUsageStats.size() <= 0) {
                return "";
            }
            if (wanted > 1) {
                TreeMap<Long, UsageStats> treeMap = new TreeMap<>(Collections.reverseOrder());
                Iterator it = listQueryUsageStats.iterator();
                while (it.hasNext()) {
                    UsageStats usageStatsM282m = ((UsageStats) it.next());
                    treeMap.put(Long.valueOf(usageStatsM282m.getLastTimeUsed()), usageStatsM282m);
                }
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<Long, UsageStats> entry : treeMap.entrySet()) {
                    if (returned != 0) {
                        sb.append(" ");
                    }
                    sb.append(entry.getValue().getPackageName());
                    if (returned >= wanted) {
                        break;
                    }
                    returned++;
                }
                packageName = sb.toString();
            } else {
                long lastTimeUsed = 0;
                int index = 0;
                while (returned < listQueryUsageStats.size()) {
                    if (((UsageStats) listQueryUsageStats.get(returned)).getLastTimeUsed()
                            > lastTimeUsed) {
                        lastTimeUsed =
                                ((UsageStats) listQueryUsageStats.get(returned)).getLastTimeUsed();
                        index = returned;
                    }
                    returned++;
                }
                packageName = ((UsageStats) listQueryUsageStats.get(index)).getPackageName();
            }
            return packageName;
        }
        List<ActivityManager.RunningTaskInfo> runningTasks =
                this.mActivityManager.getRunningTasks(wanted);
        if (runningTasks.size() <= 0) {
            return "";
        }
        StringBuilder sb2 = new StringBuilder();
        while (returned < runningTasks.size()) {
            if (returned != 0) {
                sb2.append(" ");
            }
            sb2.append(runningTasks.get(returned).topActivity.getPackageName());
            returned++;
        }
        return sb2.toString();
    }

    boolean isNotBlacklisted() {
        return this.mBlacklist.size() <= 0
                || !this.mBlacklist.contains(getPackageNamesOfRecentApps(1));
    }

    boolean isNotBlacklistedPie() {
        return this.mBlacklistPie.size() <= 0
                || !this.mBlacklistPie.contains(getPackageNamesOfRecentApps(1));
    }
}
