package com.noname81.lmt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.widget.Toast;

import java.io.FileInputStream;
import java.lang.ref.WeakReference;

/**
 * The foreground service that runs the whole engine.
 *
 * <p>Owns the pie overlay, and — only when the feature set includes gestures ({@code TSMode < 2}) —
 * the thread that blocks in the native recogniser waiting for a gesture. It must be foreground
 * because it has to survive in the background indefinitely.
 */
public class TouchService extends Service {
    private static final String TAG = "LMT::TouchService";
    private Launcher mLauncher;
    private PieContainer mPieContainer;
    private SettingsValues mSettings;
    private Toaster mToaster;
    private TouchServiceNative mTouchServiceNative;
    private TouchServiceThread mTouchServiceThread;
    private String NOTIFICATION_CHANNEL_ID = "com.noname81.lmt.TouchService";
    private String NOTIFICATION_CHANNEL_NAME = "TouchService";
    private MessageHandler mMessageHandler = new MessageHandler(this);

    /**
     * Watches the gesture thread for a busy loop (ROADMAP 1.13). When its input source dies - e.g.
     * root is denied, so the {@code su getevent} it reads exits - the native recogniser keeps
     * calling read() on the closed pipe and burns a whole core, forever. Checked on uptime, so it
     * never wakes a sleeping phone.
     */
    /** Kills stale readers, then lists devices for {@link TouchscreenDetector}. */
    private static final String PREPARE_INPUT =
            "pkill -f '^cat /dev/input/event'; getevent -p | grep -E '(device|name|0035|0036)'";

    private static final long SPIN_BASELINE_MS = 2000;

    private static final long SPIN_FIRST_CHECK_MS = 10000;
    private static final long SPIN_CHECK_MS = 60000;
    private long mSpinCpuTicks = -1;
    private long mSpinUptime;
    private boolean mDestroyed;
    private final Runnable mSpinCheck =
            new Runnable() {
                @Override
                public void run() {
                    checkTouchServiceThread();
                }
            };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class TouchServiceThread extends HandlerThread {
        boolean stopped;
        volatile int tid = -1; // run() is overridden, so HandlerThread.getThreadId() stays -1

        TouchServiceThread(String name) {
            super(name);
            this.stopped = false;
        }

        @Override
        public void run() {
            this.tid = android.os.Process.myTid();
            while (!this.stopped) {
                TouchService.this.mMessageHandler.sendMessage(
                        Message.obtain(
                                TouchService.this.mMessageHandler,
                                0,
                                TouchService.this.mTouchServiceNative.run()));
            }
        }

        @Override
        public boolean quit() {
            this.stopped = true;
            return true;
        }
    }

    private static class MessageHandler extends Handler {
        private WeakReference<TouchService> mTouchServiceReference;

        MessageHandler(TouchService touchService) {
            this.mTouchServiceReference = new WeakReference<>(touchService);
        }

        @Override
        public void handleMessage(Message message) {
            TouchServiceResult touchServiceResult = (TouchServiceResult) message.obj;
            TouchService touchService = this.mTouchServiceReference.get();
            if (touchService != null) {
                if (touchServiceResult.getGesture() != 23
                        && touchServiceResult.getGesture() <= 13) {
                    Action gestureAction =
                            touchService.mSettings.getGestureAction(
                                    touchServiceResult.getGesture());
                    if (gestureAction.getType() <= 1
                            || !touchService.mSettings.isNotBlacklisted()) {
                        return;
                    }
                    Log.d(TouchService.TAG, "Result: " + touchServiceResult.toDebugString());
                    touchService.mToaster.show(touchServiceResult);
                    touchService.mLauncher.fireAction(gestureAction);
                    return;
                }
                if (touchServiceResult.getGesture() == 23 || touchServiceResult.getGesture() < 14) {
                    return;
                }
                Action isaAction =
                        touchService.mSettings.getIsaAction(
                                touchServiceResult.getGesture(),
                                touchServiceResult.getStartX(),
                                touchServiceResult.getStartY());
                if (isaAction.getType() <= 1 || !touchService.mSettings.isNotBlacklisted()) {
                    return;
                }
                Log.d(TouchService.TAG, "Result: " + touchServiceResult.toDebugString());
                touchService.mToaster.show(touchServiceResult);
                touchService.mLauncher.fireAction(isaAction);
            }
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "TouchService created");
        this.mSettings = SettingsValues.getInstance(this);
        this.mToaster = Toaster.getInstance(this);
        this.mLauncher = Launcher.getInstance(this);
        Log.d(TAG, "App version " + this.mSettings.getVersion());
        this.mSettings.rotate();
        if (this.mSettings.loadTouchServiceMode() < 2) {
            // Gestures read the touchscreen through su: without root the reader just spins.
            RootContext rootContext = RootContext.getInstance(this);
            rootContext.recheckRoot(); // root may have been granted since the last answer
            rootContext.whenRootKnown(this::startGestures);
        }
        if (this.mSettings.loadTouchServiceMode() > 0
                && PermissionChecker.getInstance().hasDrawOverAppsPermission(this, false)) {
            if (this.mPieContainer == null) {
                this.mPieContainer = new PieContainer(this);
            }
            this.mPieContainer.attachToWindowManager();
            if (this.mSettings.getVisiblePieActivationAreas()) {
                this.mPieContainer.debug();
                this.mSettings.setVisiblePieActivationAread(false);
            }
        }
        int count = IconFolder.of(this).count();
        Log.d(TAG, "Resource path " + this.mSettings.loadResourceSearchPath());
        Log.d(
                TAG,
                "Resource path is "
                        + (count >= 0 ? "available containing " + count : "not available with 0")
                        + " file(s)");
        this.mSettings.setServiceState(1);
        startForeground(777, createNotification());
    }

    private void startGestures() {
        if (this.mDestroyed) {
            return;
        }
        if (!RootContext.getInstance(this).isRootAvailable(false)) {
            Log.w(TAG, "No root, gestures and ISAs stay off");
            Toast.makeText(this, "LMT gestures need root, which isn't granted", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        // One su call on a worker thread, before the reader starts: end readers that earlier runs
        // left behind (stopping one closes its pipe but never kills the `cat` under su), and list
        // the input devices to check the saved one is really the touchscreen.
        RootContext rootContext = RootContext.getInstance(this);
        new Thread(
                        () -> {
                            String getevent = rootContext.runCommandResult(PREPARE_INPUT, true);
                            this.mMessageHandler.post(
                                    () -> {
                                        if (!this.mDestroyed) {
                                            checkInputDevice(getevent);
                                            startGestureThread();
                                        }
                                    });
                        },
                        "GestureInput")
                .start();
    }

    /**
     * Switches to the touchscreen when the saved device doesn't report touch positions - e.g. the
     * default, event4, which is a proximity sensor on some phones.
     */
    private void checkInputDevice(String getevent) {
        if (getevent == null
                || TouchscreenDetector.reportsTouch(getevent, this.mSettings.loadInputDevice())) {
            return;
        }
        boolean rotated = this.mSettings.getOrientation() % 2 == 1;
        int width = rotated ? this.mSettings.getScreenHeight() : this.mSettings.getScreenWidth();
        int height = rotated ? this.mSettings.getScreenWidth() : this.mSettings.getScreenHeight();
        int[] touchscreen = TouchscreenDetector.detect(getevent, width, height);
        if (touchscreen == null) {
            Log.w(TAG, "No touchscreen found in getevent output");
            return;
        }
        Log.i(
                TAG,
                "Input device "
                        + this.mSettings.loadInputDevice()
                        + " isn't a touchscreen,"
                        + " using "
                        + touchscreen[0]);
        this.mSettings.saveInputDevice(touchscreen[0]);
        this.mSettings.saveTouchscreenScreenFactorX(touchscreen[1]);
        this.mSettings.saveTouchscreenScreenFactorY(touchscreen[2]);
        Toast.makeText(this, "Input device configured automatically", Toast.LENGTH_LONG).show();
    }

    private void startGestureThread() {
        if (this.mTouchServiceThread == null) {
            this.mTouchServiceThread = new TouchServiceThread("TouchServiceThread");
        }
        if (this.mTouchServiceNative == null) {
            this.mTouchServiceNative = TouchServiceNative.getInstance(this);
        }
        if (!this.mTouchServiceThread.isAlive()
                && this.mTouchServiceNative.setInputDeviceUnlock(
                                this.mSettings.loadInputDeviceString())
                        >= 0) {
            this.mTouchServiceNative.setSingleTouchGestureSupport(
                    this.mSettings.loadSingleTouchGestureSupport());
            this.mTouchServiceNative.setSingleSwipesBBox(this.mSettings.loadSingleSwipesBBox());
            this.mTouchServiceNative.setMinScore(this.mSettings.loadMinScore());
            this.mTouchServiceNative.setMinPathLength(this.mSettings.loadMinPathLength());
            this.mTouchServiceNative.setOrientation(
                    this.mSettings.getOrientation(),
                    (int)
                            (this.mSettings.getScreenWidth()
                                    * this.mSettings.loadTouchscreenScreenFactorX()),
                    (int)
                            (this.mSettings.getScreenHeight()
                                    * this.mSettings.loadTouchscreenScreenFactorY()));
            this.mTouchServiceThread.start();
            this.mSpinCpuTicks = -1;
            this.mMessageHandler.postDelayed(this.mSpinCheck, SPIN_BASELINE_MS);
        }
    }

    private void checkTouchServiceThread() {
        TouchServiceThread thread = this.mTouchServiceThread;
        if (thread == null || !thread.isAlive()) {
            return;
        }
        long ticks = threadCpuTicks(thread.tid);
        if (ticks < 0) {
            return; // Can't measure it: leave the thread alone.
        }
        long now = SystemClock.uptimeMillis();
        boolean baseline = this.mSpinCpuTicks < 0;
        if (!baseline
                && isSpinning(
                        ticks - this.mSpinCpuTicks,
                        now - this.mSpinUptime,
                        Os.sysconf(OsConstants._SC_CLK_TCK))) {
            Log.e(TAG, "Gesture thread is spinning without input, stopping it");
            Toast.makeText(
                            this,
                            "LMT gestures stopped: no touch input (root denied?)",
                            Toast.LENGTH_LONG)
                    .show();
            thread.quit();
            this.mTouchServiceNative.quit();
            return;
        }
        this.mSpinCpuTicks = ticks;
        this.mSpinUptime = now;
        this.mMessageHandler.postDelayed(
                this.mSpinCheck, baseline ? SPIN_FIRST_CHECK_MS : SPIN_CHECK_MS);
    }

    /** More than half a core over the window: a blocked reader uses next to nothing. */
    static boolean isSpinning(long cpuTicks, long elapsedMs, long ticksPerSecond) {
        return elapsedMs > 0
                && ticksPerSecond > 0
                && cpuTicks * 1000 / ticksPerSecond * 2 > elapsedMs;
    }

    /** utime + stime of one of our threads, in clock ticks, or -1 when unreadable. */
    static long threadCpuTicks(int tid) {
        byte[] buffer = new byte[512];
        int length;
        try (FileInputStream in = new FileInputStream("/proc/self/task/" + tid + "/stat")) {
            length = in.read(buffer);
        } catch (Exception e) {
            return -1;
        }
        return parseCpuTicks(length > 0 ? new String(buffer, 0, length) : "");
    }

    /** Fields 14 and 15 of /proc/.../stat; the name in field 2 may contain spaces. */
    static long parseCpuTicks(String stat) {
        int end = stat.lastIndexOf(')');
        if (end < 0) {
            return -1;
        }
        String[] fields = stat.substring(end + 2).split(" ");
        if (fields.length < 13) {
            return -1;
        }
        try {
            return Long.parseLong(fields[11]) + Long.parseLong(fields[12]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    Notification createNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(
                            new NotificationChannel(
                                    this.NOTIFICATION_CHANNEL_ID,
                                    this.NOTIFICATION_CHANNEL_NAME,
                                    0));
        }
        PendingIntent activity =
                PendingIntent.getActivity(
                        this,
                        777,
                        new Intent(this, (Class<?>) LMT.class),
                        PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= 26) {
            return new Notification.Builder(this)
                    .setContentTitle("LMT")
                    .setContentText("Touch to open LMT")
                    .setContentIntent(activity)
                    .setSmallIcon(R.drawable.piewhite_s)
                    .setChannelId(this.NOTIFICATION_CHANNEL_ID)
                    .build();
        }
        return new Notification.Builder(this)
                .setContentTitle("LMT")
                .setContentText("Touch to open LMT")
                .setContentIntent(activity)
                .setSmallIcon(R.drawable.piewhite_s)
                .setPriority(-2)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "LMT started", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "TouchService started");
        return 1;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "LMT stopped", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "TouchService stopped");
        this.mDestroyed = true;
        this.mMessageHandler.removeCallbacks(this.mSpinCheck);
        TouchServiceNative touchServiceNative = this.mTouchServiceNative;
        if (touchServiceNative != null) {
            touchServiceNative.quit();
        }
        TouchServiceThread touchServiceThread = this.mTouchServiceThread;
        if (touchServiceThread != null && touchServiceThread.isAlive()) {
            this.mTouchServiceThread.quit();
        }
        PieContainer pieContainer = this.mPieContainer;
        if (pieContainer != null) {
            pieContainer.removeFromWindowManager();
        }
        this.mSettings.setServiceState(0);
        stopForeground(true);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        PieContainer pieContainer;
        TouchServiceNative touchServiceNative;
        super.onConfigurationChanged(configuration);
        this.mSettings.rotate();
        if (this.mSettings.loadTouchServiceMode() < 2
                && (touchServiceNative = this.mTouchServiceNative) != null) {
            touchServiceNative.setOrientation(
                    this.mSettings.getOrientation(),
                    (int)
                            (this.mSettings.getScreenWidth()
                                    * this.mSettings.loadTouchscreenScreenFactorX()),
                    (int)
                            (this.mSettings.getScreenHeight()
                                    * this.mSettings.loadTouchscreenScreenFactorY()));
        }
        if (this.mSettings.loadTouchServiceMode() <= 0
                || (pieContainer = this.mPieContainer) == null) {
            return;
        }
        pieContainer.rotate();
    }
}
