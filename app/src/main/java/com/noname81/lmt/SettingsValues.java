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

/* JADX INFO: loaded from: classes.dex */
class SettingsValues extends SettingsValuesBase {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static SettingsValues instance;
    private int mOrientation;
    private int mScreenHeight;
    private int mScreenWidth;
    private boolean mVisiblePieActivationAreas;

    String getVersion() {
        return "v3.11";
    }

    private SettingsValues(Context context) {
        super(context);
        this.mVisiblePieActivationAreas = false;
    }

    static SettingsValues getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsValues(context);
        }
        return instance;
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

    void setVisiblePieActivationAread(boolean z) {
        this.mVisiblePieActivationAreas = z;
    }

    Action getIsaAction(int i, float f, float f2) {
        float f3 = f / this.mTouchscreenScreenFactorX;
        float f4 = f2 / this.mTouchscreenScreenFactorY;
        int i2 = this.mOrientation;
        if (i2 == 1 || i2 == 3) {
            f4 = this.mScreenHeight - f4;
        }
        if (f4 > this.mScreenHeight - this.mSingleSwipesAArea && i == 16) {
            return getIsaAction((int) ((f3 * 3.0f) / this.mScreenWidth));
        }
        if (f4 < this.mSingleSwipesAArea && i == 17) {
            return getIsaAction(((int) ((f3 * 3.0f) / this.mScreenWidth)) + 3);
        }
        if (f3 < this.mSingleSwipesAArea && i == 14) {
            return getIsaAction(((int) ((f4 * 3.0f) / this.mScreenHeight)) + 6);
        }
        if (f3 > this.mScreenWidth - this.mSingleSwipesAArea && i == 15) {
            return getIsaAction(((int) ((f4 * 3.0f) / this.mScreenHeight)) + 9);
        }
        return new Action(1);
    }

    String getPackageNamesOfRecentApps(int i) {
        String packageName;
        int i2 = 0;
        if (Build.VERSION.SDK_INT >= 21) {
            if (this.mRootContext.isRootAvailable(false)) {
                return this.mRootContext.runCommandRemoteResult("am get-recent-app " + i, true);
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
            if (i > 1) {
                TreeMap<Long, UsageStats> treeMap = new TreeMap<>(Collections.reverseOrder());
                Iterator it = listQueryUsageStats.iterator();
                while (it.hasNext()) {
                    UsageStats usageStatsM282m = ((UsageStats) it.next());
                    treeMap.put(Long.valueOf(usageStatsM282m.getLastTimeUsed()), usageStatsM282m);
                }
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<Long, UsageStats> entry : treeMap.entrySet()) {
                    if (i2 != 0) {
                        sb.append(" ");
                    }
                    sb.append(entry.getValue().getPackageName());
                    if (i2 >= i) {
                        break;
                    }
                    i2++;
                }
                packageName = sb.toString();
            } else {
                long lastTimeUsed = 0;
                int i3 = 0;
                while (i2 < listQueryUsageStats.size()) {
                    if (((UsageStats) listQueryUsageStats.get(i2)).getLastTimeUsed()
                            > lastTimeUsed) {
                        lastTimeUsed = ((UsageStats) listQueryUsageStats.get(i2)).getLastTimeUsed();
                        i3 = i2;
                    }
                    i2++;
                }
                packageName = ((UsageStats) listQueryUsageStats.get(i3)).getPackageName();
            }
            return packageName;
        }
        List<ActivityManager.RunningTaskInfo> runningTasks =
                this.mActivityManager.getRunningTasks(i);
        if (runningTasks.size() <= 0) {
            return "";
        }
        StringBuilder sb2 = new StringBuilder();
        while (i2 < runningTasks.size()) {
            if (i2 != 0) {
                sb2.append(" ");
            }
            sb2.append(runningTasks.get(i2).topActivity.getPackageName());
            i2++;
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
