package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

/** ROADMAP 2.3: TaskerTask, de-rooted. Pins the broadcast action, extra and target Tasker needs. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class TaskerIntentsTest {

    private Context context;
    private ShadowPackageManager shadowPackageManager;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        shadowPackageManager = Shadows.shadowOf(context.getPackageManager());
    }

    @Test
    public void runTaskBroadcastsTheDocumentedActionAndExtra() {
        Intent intent = TaskerIntents.runTask(context, "My Task");
        assertEquals("net.dinglisch.android.tasker.ACTION_TASK", intent.getAction());
        assertEquals("My Task", intent.getStringExtra("task_name"));
    }

    @Test
    public void runTaskSetsAnIdSchemeDataUri() {
        // Tasker's receiver (net.dinglisch.android.taskerm.ReceiverStaticRunTasks, confirmed
        // via `dumpsys package`) declares an intent-filter data scheme of "id" - a broadcast
        // without it never matches and is silently dropped. Verified the hard way: a hand-sent
        // am broadcast with no data scheme reached no logcat activity at all in Tasker's process.
        Intent intent = TaskerIntents.runTask(context, "My Task");
        assertEquals("id", intent.getData().getScheme());
    }

    @Test
    public void runTaskTargetsTheInstalledTaskerPackage() {
        installPackage("net.dinglisch.android.taskerm");
        Intent intent = TaskerIntents.runTask(context, "My Task");
        assertEquals("net.dinglisch.android.taskerm", intent.getPackage());
    }

    @Test
    public void runTaskPrefersTheDirectPurchasePackageWhenBothAreInstalled() {
        installPackage("net.dinglisch.android.tasker");
        installPackage("net.dinglisch.android.taskerm");
        Intent intent = TaskerIntents.runTask(context, "My Task");
        assertEquals("net.dinglisch.android.tasker", intent.getPackage());
    }

    @Test
    public void runTaskLeavesThePackageUnsetWhenTaskerIsNotInstalled() {
        // Verified on device: without an explicit package, `dumpsys activity broadcasts` shows
        // the OS silently drops this broadcast - "Background execution not allowed" - whenever
        // Tasker isn't already foregrounded. Since API 26, an implicit broadcast to a
        // manifest-declared receiver only reaches a background app if the Intent names that
        // app's package explicitly; a root-issued broadcast (the old implementation) isn't
        // subject to that policy at all, which is the real reason this needed root.
        Intent intent = TaskerIntents.runTask(context, "My Task");
        assertNull(intent.getPackage());
    }

    @Test
    public void selectTaskIntentUsesTheDocumentedAction() {
        assertEquals(
                "net.dinglisch.android.tasker.ACTION_TASK_SELECT",
                TaskerIntents.selectTaskIntent().getAction());
    }

    @Test
    public void thePickedTaskNameIsTheResultsRawDataString() {
        // Tasker returns the bare name as the data string - no scheme prefix. Same parsing as
        // HTTP Shortcuts' TaskerTaskPickerContract (`intent?.dataString`).
        Intent result = new Intent();
        result.setData(android.net.Uri.parse("LMT Test"));
        assertEquals("LMT Test", TaskerIntents.taskNameFromSelectResult(result));
    }

    @Test
    public void aResultWithNoDataYieldsNoTaskName() {
        assertNull(TaskerIntents.taskNameFromSelectResult(new Intent()));
        assertNull(TaskerIntents.taskNameFromSelectResult(null));
    }

    private void installPackage(String packageName) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.applicationInfo = new ApplicationInfo();
        packageInfo.applicationInfo.packageName = packageName;
        packageInfo.applicationInfo.sourceDir = "/data/app/" + packageName + "/base.apk";
        shadowPackageManager.installPackage(packageInfo);
    }
}
