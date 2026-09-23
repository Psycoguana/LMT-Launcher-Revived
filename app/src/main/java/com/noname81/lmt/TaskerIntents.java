package com.noname81.lmt;

import android.content.Intent;
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
 * <p>{@code ACTION_TASK_SELECT}, Tasker's documented task-picker intent, was tried and dropped:
 * verified on-device (Tasker 6.6.20) it performs an exact-name lookup against an empty query and
 * immediately toasts "no tasks found" instead of opening a browsable list - it is not the general
 * external picker the reference source describes, at least not in that version. The task name is
 * still typed by hand, as before.
 */
class TaskerIntents {
    private static final String PACKAGE = "net.dinglisch.android.tasker";
    static final String ACTION_TASK = PACKAGE + ".ACTION_TASK";
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

    /** Broadcasts a request to run {@code taskName}. Requires PERMISSION_RUN_TASKS. */
    static Intent runTask(String taskName) {
        return new Intent(ACTION_TASK)
                .setData(Uri.parse(TASK_ID_SCHEME + ":" + RANDOM.nextLong()))
                .putExtra(EXTRA_TASK_NAME, taskName);
    }
}
