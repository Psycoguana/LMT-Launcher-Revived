package com.noname81.lmt;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.Random;

/**
 * The Tasker external-access broadcast LMT uses to run a task without root.
 *
 * <p>Mirrors the relevant constant from Tasker's own {@code
 * net.dinglisch.android.tasker.TaskerIntent} helper (https://tasker.joaoapps.com/code/TaskerIntent.java)
 * rather than vendoring the whole class, which also builds parameter-passing and
 * completion-listening machinery LMT does not use. Running a task needs the {@code
 * PERMISSION_RUN_TASKS} manifest permission and Tasker's own "Allow External Access" preference;
 * neither root nor the accessibility service is involved. See docs/ROADMAP.md 2.3.
 *
 * <p>Two separate gates must both be open for a task to run: Android's runtime grant of {@code
 * PERMISSION_RUN_TASKS} to LMT, and LMT's entry in Tasker's own allowlist (Preferences &gt; Misc
 * &gt; Allow External Access). Either one missing drops the broadcast silently.
 */
class TaskerIntents {
    private static final String PACKAGE = "net.dinglisch.android.tasker";
    // The Play Store build ships under a distinct package name from the direct-purchase one;
    // this is TaskerIntent's own TASKER_PACKAGE_MARKET, and the one installed for testing here.
    private static final String PACKAGE_MARKET = PACKAGE + "m";
    static final String ACTION_TASK = PACKAGE + ".ACTION_TASK";
    static final String ACTION_TASK_SELECT = PACKAGE + ".ACTION_TASK_SELECT";
    static final String EXTRA_TASK_NAME = "task_name";
    static final String PERMISSION_RUN_TASKS = PACKAGE + ".PERMISSION_RUN_TASKS";

    // Tasker's own receiver (net.dinglisch.android.taskerm.ReceiverStaticRunTasks, confirmed via
    // `dumpsys package`) declares an intent-filter data scheme of "id" and nothing else - a plain
    // ACTION_TASK broadcast with no data never matches it and is silently dropped. The reference
    // TaskerIntent's setRandomData() sets exactly this, with a random id so concurrent
    // PendingIntents referencing different tasks don't collide.
    private static final String TASK_ID_SCHEME = "id";
    private static final Random RANDOM = new Random();

    private TaskerIntents() {}

    /**
     * Broadcasts a request to run {@code taskName}. Requires PERMISSION_RUN_TASKS.
     *
     * <p>Targeted explicitly at whichever Tasker package is installed, found via {@code
     * context}. Without this, {@code dumpsys activity broadcasts} shows the OS silently drops
     * the broadcast - "Background execution not allowed" - whenever Tasker isn't already
     * foregrounded: since API 26, an implicit broadcast to a manifest-declared receiver only
     * reaches a background app if the Intent names that app's package explicitly. The old root
     * shell command never hit this, since a root-issued broadcast isn't subject to the same
     * per-app background policy - which is the real reason this needed root, not just the
     * broadcast itself.
     */
    static Intent runTask(Context context, String taskName) {
        Intent intent =
                new Intent(ACTION_TASK)
                        .setData(Uri.parse(TASK_ID_SCHEME + ":" + RANDOM.nextLong()))
                        .putExtra(EXTRA_TASK_NAME, taskName);
        String taskerPackage = resolveInstalledTaskerPackage(context);
        if (taskerPackage != null) {
            intent.setPackage(taskerPackage);
        }
        return intent;
    }

    /**
     * Tasker's task picker, for {@code startActivityForResult}; the chosen task's name comes back
     * via {@link #taskNameFromSelectResult}. Same intent HTTP Shortcuts and TapTap ship. It lists
     * only tasks Tasker has saved - a task still pending under Tasker's unapplied-changes check
     * mark isn't listed, and with none saved the picker toasts "no tasks found" and cancels.
     */
    static Intent selectTaskIntent() {
        return new Intent(ACTION_TASK_SELECT)
                .setFlags(
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
                                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                | Intent.FLAG_ACTIVITY_NO_HISTORY);
    }

    /** The picked task name - Tasker returns it as the result's raw data string. */
    static String taskNameFromSelectResult(Intent data) {
        if (data == null) {
            return null;
        }
        String name = data.getDataString();
        return name == null || name.length() == 0 ? null : name;
    }

    private static String resolveInstalledTaskerPackage(Context context) {
        PackageManager packageManager = context.getPackageManager();
        for (String candidate : new String[] {PACKAGE, PACKAGE_MARKET}) {
            try {
                packageManager.getPackageInfo(candidate, 0);
                return candidate;
            } catch (PackageManager.NameNotFoundException notInstalled) {
                // try the other package name
            }
        }
        return null;
    }
}
