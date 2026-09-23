package com.noname81.lmt;

import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * A command server that runs outside the app's sandbox, as root.
 *
 * <p>Android forbids an app from pressing Home, force-stopping another app or
 * reordering the task stack — which is most of what LMT does. {@code RootContext}
 * therefore copies this out of assets, starts it once under {@code su} via
 * {@code app_process}, and keeps it alive, writing newline-delimited commands to its
 * stdin. Starting a fresh {@code su} per action would cost hundreds of milliseconds,
 * which a gesture launcher cannot afford.
 *
 * <p>Protocol — one command per line:
 *
 * <pre>
 *   input keyevent &lt;code[,code2]&gt;...   input text &lt;s&gt;   input tap &lt;x&gt; &lt;y&gt;
 *   am start|move-to-front|move-to-front-or-start|last-app|next-app|prev-app
 *   am kill-app|kill-all-apps|get-recent-app
 *   telephone        toggle mobile data
 *   debug 0|1        version     ping -&gt; pong      quit
 * </pre>
 *
 * <p>A keycode above 500/1000/2000 encodes a pre-release wait of 100/600/1000ms, used
 * for chorded shortcuts such as the screenshot combo.
 *
 * <p>Built from source rather than shipped prebuilt. Everything hidden goes through
 * {@link InputInjector}, {@link ActivityManagerBridge} and {@link TelephonyBridge},
 * so this compiles against the public SDK and one source tree covers every API level
 * — see {@link Reflect} for why that matters.
 */
public final class InputContext {

    private static final String TAG = "LMT::InputContext";
    private static final String VERSION = "v4.0";

    private static boolean DEBUG = false;
    private static boolean quit = false;
    private static InputContext instance;

    private final Instrumentation mInstrumentation = new Instrumentation();
    private List<ActivityManager.RecentTaskInfo> mRecentTasks;

    private InputContext() {}

    private static InputContext getInstance() {
        if (instance == null) {
            instance = new InputContext();
        }
        return instance;
    }

    public static void main(String[] args) {
        Log.d(TAG, "InputContext started " + VERSION);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        do {
            String command;
            try {
                command = in.readLine();
            } catch (Exception e) {
                logFailure("Failed to read line", e);
                break;
            }
            if (command == null) {
                break;
            }
            try {
                getInstance().runCommand(command);
            } catch (Exception e) {
                logFailure("Failed to process command: " + command, e);
            }
        } while (!quit);
        Log.d(TAG, "quit");
        System.out.println("quit");
    }

    private static void logFailure(String message, Throwable t) {
        StringWriter trace = new StringWriter();
        t.printStackTrace(new PrintWriter(trace));
        Log.e(TAG, message);
        Log.e(TAG, trace.toString());
    }

    private static void unknown(String command) {
        if (DEBUG) {
            Log.d(TAG, "Unknown command! " + command);
        }
    }

    private void runCommand(String command) throws Exception {
        if (DEBUG) {
            Log.d(TAG, "runCommand(" + command + ")");
        }
        String[] argv = command.split(" ");
        if (argv.length < 1) {
            return;
        }
        switch (argv[0]) {
            case "input":
                runInput(argv);
                break;
            case "am":
                runActivityManager(argv);
                break;
            case "telephone":
                TelephonyBridge.toggleData();
                break;
            case "debug":
                if (argv.length >= 2) {
                    DEBUG = argv[1].equals("1");
                } else {
                    unknown(command);
                }
                break;
            case "quit":
                quit = true;
                break;
            case "version":
                Log.d(TAG, "InputContext version " + VERSION);
                System.out.println(VERSION);
                break;
            case "ping":
                System.out.println("pong");
                break;
            default:
                unknown(command);
                break;
        }
    }

    // ------------------------------------------------------------------ input

    private void runInput(String[] argv) throws Exception {
        if (DEBUG) {
            Log.d(TAG, "runInput()");
        }
        if (argv.length < 3) {
            return;
        }
        switch (argv[1]) {
            case "keyevent":
                for (int i = 2; i < argv.length; i++) {
                    sendEncodedKeyEvent(argv[i]);
                }
                break;
            case "text":
                sendText(argv[2]);
                break;
            case "tap":
                if (argv.length == 4) {
                    sendTap(Integer.parseInt(argv[2]), Integer.parseInt(argv[3]));
                }
                break;
            default:
                unknown(argv[1]);
                break;
        }
    }

    /**
     * One "code" or "code,code2" token, where the leading digit of the first code
     * encodes a wait before the up-stroke: >2000 waits 1000ms, >1000 waits 600ms,
     * >500 waits 100ms. LMT uses this for chords such as screenshot (1000 120).
     */
    private void sendEncodedKeyEvent(String token) throws Exception {
        String[] codes = token.split(",");
        int keycode1 = codes.length > 0 ? Integer.parseInt(codes[0]) : 0;
        int keycode2 = codes.length > 1 ? Integer.parseInt(codes[1]) : 0;

        if (keycode1 > 2000) {
            sendKeyEvent(keycode1 - 2000, keycode2, 1000);
        } else if (keycode1 > 1000) {
            sendKeyEvent(keycode1 - 1000, keycode2, 600);
        } else if (keycode1 > 500) {
            sendKeyEvent(keycode1 - 500, keycode2, 100);
        } else {
            sendKeyEvent(keycode1, keycode2, 0);
        }
    }

    private void sendKeyEvent(int keycode1, int keycode2, int wait) throws Exception {
        if (DEBUG) {
            Log.d(TAG, "sendKeyEvent(" + keycode1 + ", " + keycode2 + ", " + wait + ")");
        }
        if (Build.VERSION.SDK_INT < 18) {
            mInstrumentation.sendKeySync(new KeyEvent(KeyEvent.ACTION_DOWN, keycode1));
            if (keycode2 > 0) {
                mInstrumentation.sendKeySync(new KeyEvent(KeyEvent.ACTION_DOWN, keycode2));
            }
            if (wait > 0) {
                Thread.sleep(wait);
            }
            mInstrumentation.sendKeySync(new KeyEvent(KeyEvent.ACTION_UP, keycode1));
            if (keycode2 > 0) {
                mInstrumentation.sendKeySync(new KeyEvent(KeyEvent.ACTION_UP, keycode2));
            }
            return;
        }

        long down = SystemClock.uptimeMillis();
        injectKey(down, KeyEvent.ACTION_DOWN, keycode1);
        if (keycode2 > 0) {
            injectKey(down, KeyEvent.ACTION_DOWN, keycode2);
        }
        if (wait > 0) {
            Thread.sleep(wait);
        }
        long up = SystemClock.uptimeMillis();
        injectKey(up, KeyEvent.ACTION_UP, keycode1);
        if (keycode2 > 0) {
            injectKey(up, KeyEvent.ACTION_UP, keycode2);
        }
    }

    /** FLAG_FROM_SYSTEM | FLAG_KEEP_TOUCH_MODE (257), as the original sent. */
    private void injectKey(long time, int action, int keycode) throws Exception {
        InputInjector.inject(
                new KeyEvent(time, time, action, keycode, 0, 0, -1, 0, 257,
                        android.view.InputDevice.SOURCE_KEYBOARD));
    }

    private void sendText(String text) throws Exception {
        if (DEBUG) {
            Log.d(TAG, "sendText()");
        }
        if (Build.VERSION.SDK_INT < 18) {
            mInstrumentation.sendStringSync(text);
            return;
        }
        // %s is the escape the original accepted for a literal space
        String decoded = text.replace("%s", " ");
        KeyCharacterMap map = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        KeyEvent[] events = map.getEvents(decoded.toCharArray());
        if (events == null) {
            Log.e(TAG, "no key events for text");
            return;
        }
        for (KeyEvent event : events) {
            InputInjector.inject(event);
        }
    }

    private void sendTap(float x, float y) throws Exception {
        if (DEBUG) {
            Log.d(TAG, "sendTap(" + x + ", " + y + ")");
        }
        long now = SystemClock.uptimeMillis();
        if (Build.VERSION.SDK_INT < 18) {
            mInstrumentation.sendPointerSync(
                    MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0));
            mInstrumentation.sendPointerSync(
                    MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, x, y, 0));
            return;
        }
        injectPointer(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0));
        injectPointer(MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, x, y, 0));
    }

    private void injectPointer(MotionEvent event) throws Exception {
        event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        InputInjector.inject(event);
    }

    // ----------------------------------------------------------- activity manager

    private void runActivityManager(String[] argv) throws Exception {
        if (DEBUG) {
            Log.d(TAG, "runActivityManager()");
        }
        if (argv.length < 3) {
            return;
        }
        switch (argv[1]) {
            case "start":
                if (argv.length >= 6) {
                    startComponent(argv[5]);
                }
                break;
            case "move-to-front":
                ActivityManagerBridge.moveTaskToFront(Integer.parseInt(argv[2]));
                break;
            case "move-to-front-or-start":
                moveToFrontOrStart(argv[2]);
                break;
            case "last-app":
                lastApp();
                break;
            case "prev-app":
                prevApp();
                break;
            case "next-app":
                nextApp();
                break;
            case "kill-app":
                killApps(true);
                break;
            case "kill-all-apps":
                killApps(false);
                break;
            case "get-recent-app":
                printRecentApps(argv.length == 3 ? Integer.parseInt(argv[2]) : 1);
                break;
            default:
                unknown(argv[1]);
                break;
        }
    }

    private void refreshRecentTasks() throws Exception {
        mRecentTasks = ActivityManagerBridge.getRecentTasks(20);
    }

    private static boolean isHome(ActivityManager.RecentTaskInfo task) {
        return task.baseIntent != null
                && task.baseIntent.hasCategory(Intent.CATEGORY_HOME);
    }

    private static ComponentName baseActivityOf(ActivityManager.RecentTaskInfo task) {
        return task.baseActivity;
    }

    private void startComponent(String flattened) throws Exception {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.setComponent(ComponentName.unflattenFromString(flattened));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityManagerBridge.startActivity(intent);
        if (DEBUG) {
            Log.d(TAG, "startActivity(" + flattened + ")");
        }
    }

    private void moveToFrontOrStart(String flattened) throws Exception {
        refreshRecentTasks();
        ComponentName component = ComponentName.unflattenFromString(flattened);
        String packageName = component.getPackageName();

        if (mRecentTasks != null && mRecentTasks.size() > 1) {
            // The original only ever examined the first non-current task.
            ActivityManager.RecentTaskInfo task = mRecentTasks.get(1);
            ComponentName base = baseActivityOf(task);
            if (base != null && base.getPackageName().equals(packageName)) {
                ActivityManagerBridge.moveTaskToFront(task.id);
                if (DEBUG) {
                    Log.d(TAG, "moveTaskToFront(" + task.id + ")");
                }
            }
        }
        startComponent(flattened);
    }

    private void lastApp() throws Exception {
        refreshRecentTasks();
        int choose = 0;
        if (mRecentTasks.size() > 1 && !isHome(mRecentTasks.get(1))) {
            choose = 1;
        } else if (mRecentTasks.size() > 2 && !isHome(mRecentTasks.get(2))) {
            choose = 2;
        }
        ActivityManagerBridge.moveTaskToFront(mRecentTasks.get(choose).id);
        if (DEBUG) {
            Log.d(TAG, "lastApp -> position " + choose);
        }
    }

    /** The task before the current one by id, wrapping to the highest id. */
    private void prevApp() throws Exception {
        refreshRecentTasks();
        int currentId = mRecentTasks.get(0).id;
        int prevId = 0;
        int prevPos = 0;
        int highestId = 0;
        for (int i = 1; i < mRecentTasks.size(); i++) {
            ActivityManager.RecentTaskInfo task = mRecentTasks.get(i);
            if (isHome(task)) {
                continue;
            }
            if (task.id > prevId && task.id < currentId) {
                prevId = task.id;
                prevPos = i;
            }
            if (task.id > highestId && task.id > -1) {
                highestId = task.id;
            }
        }
        int target = prevPos > 0 ? prevId : highestId;
        ActivityManagerBridge.moveTaskToFront(target);
        if (DEBUG) {
            Log.d(TAG, "prevApp -> " + target);
        }
    }

    /** The task after the current one by id, wrapping to the lowest id. */
    private void nextApp() throws Exception {
        refreshRecentTasks();
        int currentId = mRecentTasks.get(0).id;
        int nextId = Integer.MAX_VALUE;
        int nextPos = 0;
        int lowestId = Integer.MAX_VALUE;
        for (int i = 1; i < mRecentTasks.size(); i++) {
            ActivityManager.RecentTaskInfo task = mRecentTasks.get(i);
            if (isHome(task)) {
                continue;
            }
            if (task.id < nextId && task.id > currentId) {
                nextId = task.id;
                nextPos = i;
            }
            if (task.id < lowestId && task.id > -1) {
                lowestId = task.id;
            }
        }
        int target = nextPos > 0 ? nextId : lowestId;
        ActivityManagerBridge.moveTaskToFront(target);
        if (DEBUG) {
            Log.d(TAG, "nextApp -> " + target);
        }
    }

    /** @param onlyFirst stop at the first killable task ("kill-app") or keep going. */
    private void killApps(boolean onlyFirst) throws Exception {
        refreshRecentTasks();
        for (ActivityManager.RecentTaskInfo task : mRecentTasks) {
            ComponentName base = baseActivityOf(task);
            if (base == null) {
                continue;
            }
            String packageName = base.getPackageName();
            if (DEBUG) {
                Log.d(TAG, "Evaluate to kill " + packageName);
            }
            if (isHome(task)
                    || packageName.contains("com.noname81.lmt")
                    || packageName.contains("com.android.systemui")) {
                continue;
            }
            if (DEBUG) {
                Log.d(TAG, "forceStopPackage(" + packageName + ")");
            }
            ActivityManagerBridge.forceStopPackage(packageName);
            if (onlyFirst) {
                return;
            }
        }
    }

    private void printRecentApps(int requested) throws Exception {
        refreshRecentTasks();
        StringBuilder out = new StringBuilder();
        int returned = 0;
        for (ActivityManager.RecentTaskInfo task : mRecentTasks) {
            ComponentName base = baseActivityOf(task);
            if (DEBUG) {
                Log.d(TAG, "baseActivity:" + base);
            }
            if (isHome(task) || base == null) {
                continue;
            }
            if (returned > 0) {
                out.append(" ");
            }
            out.append(base.getPackageName());
            returned++;
            if (returned >= requested) {
                break;
            }
        }
        if (DEBUG) {
            Log.d(TAG, "Return recent apps " + out);
        }
        System.out.println(out.toString());
    }
}
