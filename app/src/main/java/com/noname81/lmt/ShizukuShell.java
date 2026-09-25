package com.noname81.lmt;

import android.content.pm.PackageManager;
import android.util.Log;

import rikka.shizuku.Shizuku;

import java.lang.reflect.Method;

/**
 * Runs a shell command with Shizuku's shell-user rights (ROADMAP 2.6), for phones without root. The
 * user installs and starts Shizuku and allows LMT once.
 */
final class ShizukuShell {
    private static final String TAG = "LMT::Shizuku";
    private static final int REQUEST_CODE = 81;

    static final int RAN = 0;
    static final int NOT_RUNNING = 1;
    static final int ASKED_PERMISSION = 2;
    static final int FAILED = 3;

    private ShizukuShell() {}

    static int run(String command) {
        try {
            if (!Shizuku.pingBinder() || Shizuku.isPreV11()) {
                return NOT_RUNNING;
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST_CODE);
                return ASKED_PERMISSION;
            }
            // Shizuku 13 made newProcess private in favour of user services; one shell command
            // does not need a service.
            Method newProcess =
                    Shizuku.class.getDeclaredMethod(
                            "newProcess", String[].class, String[].class, String.class);
            newProcess.setAccessible(true);
            Process process =
                    (Process)
                            newProcess.invoke(null, new String[] {"sh", "-c", command}, null, null);
            new Thread(
                            () -> {
                                try {
                                    process.waitFor();
                                } catch (InterruptedException ignored) {
                                }
                            },
                            "LMT-Shizuku")
                    .start();
            return RAN;
        } catch (Exception | LinkageError e) {
            Log.w(TAG, "Shizuku command failed", e);
            return FAILED;
        }
    }
}
