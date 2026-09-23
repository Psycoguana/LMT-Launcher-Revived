package com.noname81.lmt;

import static org.junit.Assert.assertEquals;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** ROADMAP 2.3: TaskerTask, de-rooted. Pins the broadcast action and extra Tasker documents. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class TaskerIntentsTest {

    @Test
    public void runTaskBroadcastsTheDocumentedActionAndExtra() {
        Intent intent = TaskerIntents.runTask("My Task");
        assertEquals("net.dinglisch.android.tasker.ACTION_TASK", intent.getAction());
        assertEquals("My Task", intent.getStringExtra("task_name"));
    }

    @Test
    public void runTaskSetsAnIdSchemeDataUri() {
        // Tasker's receiver (net.dinglisch.android.taskerm.ReceiverStaticRunTasks, confirmed
        // via `dumpsys package`) declares an intent-filter data scheme of "id" - a broadcast
        // without it never matches and is silently dropped. Verified the hard way: a hand-sent
        // am broadcast with no data scheme reached no logcat activity at all in Tasker's process.
        Intent intent = TaskerIntents.runTask("My Task");
        assertEquals("id", intent.getData().getScheme());
    }
}
