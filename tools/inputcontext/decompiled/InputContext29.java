package com.noname81.lmt;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.Instrumentation;
import android.app.ProfilerInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class InputContext {
    private static final String TAG = "LMT::InputContext";
    private Instrumentation mInstrumentation = new Instrumentation();
    private List<ActivityManager.RecentTaskInfo> mRecentTaskInfo;
    private List<ActivityManager.RunningTaskInfo> mRunningTaskInfo;
    private static boolean DEBUG = false;
    private static boolean QUIT = false;
    private static InputContext instance = null;

    public static void main(String[] args) {
        Log.d(TAG, "InputContext started");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        do {
            try {
                String command = bufferedReader.readLine();
                if (command == null) {
                    break;
                }
                try {
                    getInstance().runCommand(command);
                } catch (Exception e) {
                    Writer result = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(result);
                    e.printStackTrace(printWriter);
                    Log.e(TAG, "Failed to process command: " + command);
                    Log.e(TAG, result.toString());
                }
            } catch (Exception e2) {
                Writer result2 = new StringWriter();
                PrintWriter printWriter2 = new PrintWriter(result2);
                e2.printStackTrace(printWriter2);
                Log.e(TAG, "Failed to read line");
                Log.e(TAG, result2.toString());
            }
        } while (!QUIT);
        Log.d(TAG, "quit");
        System.out.println("quit");
    }

    private InputContext() {
    }

    private static InputContext getInstance() {
        if (instance == null) {
            instance = new InputContext();
        }
        return instance;
    }

    private void runCommand(String command) throws Exception {
        if (DEBUG) {
            Log.d(TAG, "runCommand(" + command + ")");
        }
        String[] stringArray = command.split(" ");
        if (stringArray.length >= 1) {
            switch (stringArray[0]) {
                case "input":
                    runInput(stringArray);
                    break;
                case "am":
                    runActivityManager(stringArray);
                    break;
                case "telephone":
                    runTelephone();
                    break;
                case "debug":
                    runDebug(stringArray);
                    break;
                case "quit":
                    QUIT = true;
                    break;
                case "version":
                    Log.d(TAG, "InputContext version v3.3");
                    break;
                case "ping":
                    System.out.println("pong");
                    break;
                default:
                    if (DEBUG) {
                        Log.d(TAG, "Unknown command!");
                        break;
                    }
                    break;
            }
        }
    }

    private void runDebug(String[] stringArray) {
        if (DEBUG) {
            Log.d(TAG, "runDebug()");
        }
        if (stringArray.length >= 2) {
            DEBUG = stringArray[1].equals("1");
        } else if (DEBUG) {
            Log.d(TAG, "Unknown command!");
        }
    }

    private void runInput(String[] stringArray) throws Exception {
        if (DEBUG) {
            Log.d(TAG, "runInput()");
        }
        if (stringArray.length >= 3) {
            switch (stringArray[1]) {
                case "keyevent":
                    for (int i = 2; i < stringArray.length; i++) {
                        String[] commandArray = stringArray[i].split(",");
                        int keycode1 = commandArray.length > 0 ? Integer.parseInt(commandArray[0]) : 0;
                        int keycode2 = commandArray.length > 1 ? Integer.parseInt(commandArray[1]) : 0;
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
                    break;
                case "text":
                    sendText(stringArray[2]);
                    break;
                case "tap":
                    if (stringArray.length == 4) {
                        int x = Integer.parseInt(stringArray[2]);
                        int y = Integer.parseInt(stringArray[3]);
                        sendTap(x, y);
                        break;
                    }
                    break;
                default:
                    if (DEBUG) {
                        Log.d(TAG, "Unknown command!");
                        break;
                    }
                    break;
            }
        }
    }

    /* JADX WARN: Code duplicated, block: B:191:0x076d  */
    @SuppressLint({"NewApi"})
    private void runActivityManager(String[] stringArray) throws Exception {
        if (DEBUG) {
            Log.d(TAG, "runActivityManager()");
        }
        if (stringArray.length >= 3) {
            switch (stringArray[1]) {
                case "start":
                    if (stringArray.length >= 6) {
                        Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
                        intent.setComponent(ComponentName.unflattenFromString(stringArray[5]));
                        intent.setFlags(268435456);
                        startActivity(intent);
                        if (DEBUG) {
                            Log.d(TAG, "startActivityAndWait(" + stringArray[5] + ")");
                        }
                        break;
                    }
                    break;
                case "move-to-front":
                    moveTaskToFront(Integer.parseInt(stringArray[2]));
                    if (DEBUG) {
                        Log.d(TAG, "moveTaskToFront(" + stringArray[2] + ")");
                        break;
                    }
                    break;
                case "move-to-front-or-start":
                    getRecentTasks();
                    ComponentName componentName = ComponentName.unflattenFromString(stringArray[2]);
                    String packageName = componentName.getPackageName();
                    if (this.mRecentTaskInfo != null) {
                        for (int i = 1; i < this.mRecentTaskInfo.size(); i++) {
                            if (DEBUG) {
                                Log.d(TAG, i + " baseActivity:" + this.mRecentTaskInfo.get(i).baseActivity);
                            }
                            if (this.mRecentTaskInfo.get(i).baseActivity != null && this.mRecentTaskInfo.get(i).baseActivity.getPackageName().equals(packageName)) {
                                moveTaskToFront(this.mRecentTaskInfo.get(i).id);
                                if (DEBUG) {
                                    Log.d(TAG, "moveTaskToFront(" + this.mRecentTaskInfo.get(i).id + ")");
                                }
                            }
                            break;
                        }
                    }
                    Intent intent2 = new Intent("android.intent.action.MAIN", (Uri) null);
                    intent2.setComponent(componentName);
                    intent2.setFlags(268435456);
                    startActivity(intent2);
                    if (DEBUG) {
                        Log.d(TAG, "startActivityAndWait(" + stringArray[2] + ")");
                        break;
                    }
                    break;
                case "last-app":
                    getRecentTasks();
                    int taskToChoose = 0;
                    if (this.mRecentTaskInfo.size() > 1 && !this.mRecentTaskInfo.get(1).baseIntent.hasCategory("android.intent.category.HOME")) {
                        taskToChoose = 1;
                    } else if (this.mRecentTaskInfo.size() > 2 && !this.mRecentTaskInfo.get(2).baseIntent.hasCategory("android.intent.category.HOME")) {
                        taskToChoose = 2;
                    }
                    moveTaskToFront(this.mRecentTaskInfo.get(taskToChoose).id);
                    if (DEBUG) {
                        Log.d(TAG, "moveTaskToFront(" + taskToChoose + ")");
                        break;
                    }
                    break;
                case "prev-app":
                    getRecentTasks();
                    int prevID = 0;
                    int prevPos = 0;
                    int lastID = 0;
                    for (int i2 = 1; i2 < this.mRecentTaskInfo.size(); i2++) {
                        if (this.mRecentTaskInfo.get(i2).id > prevID && this.mRecentTaskInfo.get(i2).id < this.mRecentTaskInfo.get(0).id && !this.mRecentTaskInfo.get(i2).baseIntent.hasCategory("android.intent.category.HOME")) {
                            prevID = this.mRecentTaskInfo.get(i2).id;
                            prevPos = i2;
                        }
                        if (this.mRecentTaskInfo.get(i2).id > lastID && !this.mRecentTaskInfo.get(i2).baseIntent.hasCategory("android.intent.category.HOME") && this.mRecentTaskInfo.get(i2).id > -1) {
                            lastID = this.mRecentTaskInfo.get(i2).id;
                        }
                    }
                    moveTaskToFront(prevPos > 0 ? prevID : lastID);
                    if (DEBUG) {
                        StringBuilder sbAppend = new StringBuilder().append("moveTaskToFront(");
                        if (prevPos <= 0) {
                            prevID = lastID;
                        }
                        Log.d(TAG, sbAppend.append(prevID).append(")").toString());
                        break;
                    }
                    break;
                case "next-app":
                    getRecentTasks();
                    int nextID = Integer.MAX_VALUE;
                    int nextPos = 0;
                    int firstID = Integer.MAX_VALUE;
                    for (int i3 = 1; i3 < this.mRecentTaskInfo.size(); i3++) {
                        if (this.mRecentTaskInfo.get(i3).id < nextID && this.mRecentTaskInfo.get(i3).id > this.mRecentTaskInfo.get(0).id && !this.mRecentTaskInfo.get(i3).baseIntent.hasCategory("android.intent.category.HOME")) {
                            nextID = this.mRecentTaskInfo.get(i3).id;
                            nextPos = i3;
                        }
                        if (this.mRecentTaskInfo.get(i3).id < firstID && !this.mRecentTaskInfo.get(i3).baseIntent.hasCategory("android.intent.category.HOME") && this.mRecentTaskInfo.get(i3).id > -1) {
                            firstID = this.mRecentTaskInfo.get(i3).id;
                        }
                    }
                    moveTaskToFront(nextPos > 0 ? nextID : firstID);
                    if (DEBUG) {
                        StringBuilder sbAppend2 = new StringBuilder().append("moveTaskToFront(");
                        if (nextPos <= 0) {
                            nextID = firstID;
                        }
                        Log.d(TAG, sbAppend2.append(nextID).append(")").toString());
                        break;
                    }
                    break;
                case "kill-app":
                    getRecentTasks();
                    for (int i4 = 0; i4 < this.mRecentTaskInfo.size(); i4++) {
                        ComponentName baseActivity = getBaseActivityFromRecentTaskInfo(this.mRecentTaskInfo.get(i4));
                        if (baseActivity != null) {
                            String packageName2 = baseActivity.getPackageName();
                            if (DEBUG) {
                                Log.d(TAG, "Evaluate to kill " + packageName2);
                            }
                            if (!this.mRecentTaskInfo.get(i4).baseIntent.hasCategory("android.intent.category.HOME") && !packageName2.contains("com.noname81.lmt") && !packageName2.contains("com.android.systemui")) {
                                if (DEBUG) {
                                    Log.d(TAG, "killApplicationProcess(" + packageName2 + ")");
                                }
                                forceStopPackage(packageName2);
                                break;
                            }
                        }
                    }
                    break;
                case "kill-all-apps":
                    getRecentTasks();
                    for (int i5 = 0; i5 < this.mRecentTaskInfo.size(); i5++) {
                        ComponentName baseActivity2 = getBaseActivityFromRecentTaskInfo(this.mRecentTaskInfo.get(i5));
                        if (baseActivity2 != null) {
                            String packageName3 = baseActivity2.getPackageName();
                            if (DEBUG) {
                                Log.d(TAG, "Evaluate to kill " + packageName3);
                            }
                            if (!this.mRecentTaskInfo.get(i5).baseIntent.hasCategory("android.intent.category.HOME") && !packageName3.contains("com.noname81.lmt") && !packageName3.contains("com.android.systemui")) {
                                if (DEBUG) {
                                    Log.d(TAG, "killApplicationProcess(" + packageName3 + ")");
                                }
                                forceStopPackage(packageName3);
                            }
                        }
                    }
                    break;
                case "get-recent-app":
                    getRecentTasks();
                    int numberOfRecentAppsRequested = 1;
                    if (stringArray.length == 3) {
                        numberOfRecentAppsRequested = Integer.parseInt(stringArray[2]);
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    int numberOfRecentAppsReturned = 0;
                    for (int i6 = 0; i6 < this.mRecentTaskInfo.size(); i6++) {
                        ComponentName baseActivity3 = getBaseActivityFromRecentTaskInfo(this.mRecentTaskInfo.get(i6));
                        if (DEBUG) {
                            Log.d(TAG, i6 + " baseActivity:" + baseActivity3);
                        }
                        if (!this.mRecentTaskInfo.get(i6).baseIntent.hasCategory("android.intent.category.HOME") && baseActivity3 != null) {
                            if (numberOfRecentAppsReturned != 0) {
                                stringBuilder.append(" ");
                            }
                            stringBuilder.append(baseActivity3.getPackageName());
                            numberOfRecentAppsReturned++;
                        }
                        if (numberOfRecentAppsReturned >= numberOfRecentAppsRequested) {
                            if (DEBUG) {
                                Log.d(TAG, "Return recent apps " + stringBuilder.toString());
                            }
                            System.out.println(stringBuilder.toString());
                        }
                        break;
                    }
                    if (DEBUG) {
                        Log.d(TAG, "Return recent apps " + stringBuilder.toString());
                    }
                    System.out.println(stringBuilder.toString());
                    break;
                default:
                    if (DEBUG) {
                        Log.d(TAG, "Unknown command!");
                        break;
                    }
                    break;
            }
        }
    }

    @SuppressLint({"InlinedApi"})
    @TargetApi(11)
    private void sendKeyEvent(int keycode1, int keycode2, int wait) throws Exception {
        if (Build.VERSION.SDK_INT >= 18) {
            if (DEBUG) {
                Log.d(TAG, "sendKeyEvent(" + keycode1 + ", " + keycode2 + ", " + wait + ") >=18");
            }
            long now = SystemClock.uptimeMillis();
            injectKeyEvent(new KeyEvent(now, now, 0, keycode1, 0, 0, -1, 0, 0, 257));
            if (keycode2 > 0) {
                injectKeyEvent(new KeyEvent(now, now, 0, keycode2, 0, 0, -1, 0, 0, 257));
            }
            if (wait > 0) {
                Thread.sleep(wait);
            }
            long now2 = SystemClock.uptimeMillis();
            injectKeyEvent(new KeyEvent(now2, now2, 1, keycode1, 0, 0, -1, 0, 0, 257));
            if (keycode2 > 0) {
                injectKeyEvent(new KeyEvent(now2, now2, 1, keycode2, 0, 0, -1, 0, 0, 257));
                return;
            }
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "sendKeyEvent(" + keycode1 + ", " + keycode2 + ", " + wait + ") <18");
        }
        this.mInstrumentation.sendKeySync(new KeyEvent(0, keycode1));
        if (keycode2 > 0) {
            this.mInstrumentation.sendKeySync(new KeyEvent(0, keycode2));
        }
        if (wait > 0) {
            Thread.sleep(wait);
        }
        this.mInstrumentation.sendKeySync(new KeyEvent(1, keycode1));
        if (keycode2 > 0) {
            this.mInstrumentation.sendKeySync(new KeyEvent(1, keycode2));
        }
    }

    private void injectKeyEvent(KeyEvent event) {
        InputManager.getInstance().injectInputEvent(event, 2);
    }

    @SuppressLint({"InlinedApi"})
    private void sendText(String text) {
        if (Build.VERSION.SDK_INT >= 18) {
            if (DEBUG) {
                Log.d(TAG, "sendText() >=18");
            }
            StringBuilder buff = new StringBuilder(text);
            boolean escapeFlag = false;
            int i = 0;
            while (i < buff.length()) {
                if (escapeFlag) {
                    escapeFlag = false;
                    if (buff.charAt(i) == 's') {
                        buff.setCharAt(i, ' ');
                        i--;
                        buff.deleteCharAt(i);
                    }
                }
                if (buff.charAt(i) == '%') {
                    escapeFlag = true;
                }
                i++;
            }
            char[] chars = buff.toString().toCharArray();
            KeyCharacterMap kcm = KeyCharacterMap.load(-1);
            KeyEvent[] events = kcm.getEvents(chars);
            for (KeyEvent event : events) {
                injectKeyEvent(event);
            }
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "sendText() <18");
        }
        this.mInstrumentation.sendStringSync(text);
    }

    @SuppressLint({"Recycle"})
    private void sendTap(float x, float y) {
        long now = SystemClock.uptimeMillis();
        if (Build.VERSION.SDK_INT >= 18) {
            if (DEBUG) {
                Log.d(TAG, "sendTap() >=18");
            }
            injectPointerEvent(MotionEvent.obtain(now, now, 0, x, y, 0));
            injectPointerEvent(MotionEvent.obtain(now, now, 1, x, y, 0));
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "sendTap() <18");
        }
        this.mInstrumentation.sendPointerSync(MotionEvent.obtain(now, now, 0, x, y, 0));
        this.mInstrumentation.sendPointerSync(MotionEvent.obtain(now, now, 1, x, y, 0));
    }

    @SuppressLint({"NewApi"})
    private void injectPointerEvent(MotionEvent event) {
        event.setSource(4098);
        InputManager.getInstance().injectInputEvent(event, 2);
    }

    private void forceStopPackage(String packageName) throws Exception {
        IActivityManager am = ActivityManager.getService();
        am.forceStopPackage(packageName, 0);
    }

    private void moveTaskToFront(int task) throws Exception {
        IActivityManager am = ActivityManager.getService();
        am.moveTaskToFront((IApplicationThread) null, (String) null, task, 0, (Bundle) null);
    }

    private void startActivity(Intent intent) throws Exception {
        IActivityManager am = ActivityManager.getService();
        am.startActivity((IApplicationThread) null, (String) null, intent, intent.getType(), (IBinder) null, (String) null, 0, 0, (ProfilerInfo) null, (Bundle) null);
        if (DEBUG) {
            Log.d(TAG, "startActivityAndWait() >=28");
        }
    }

    private void getTasks() throws Exception {
        IActivityManager am = ActivityManager.getService();
        this.mRunningTaskInfo = am.getTasks(20);
        if (DEBUG) {
            Log.d(TAG, "getTasks() >=v28");
        }
    }

    private void getRecentTasks() throws Exception {
        IActivityManager am = ActivityManager.getService();
        this.mRecentTaskInfo = am.getRecentTasks(20, 0, 0).getList();
        if (DEBUG) {
            Log.d(TAG, "getRecentTasks() >=v28");
        }
    }

    @SuppressLint({"NewApi"})
    private ComponentName getBaseActivityFromRecentTaskInfo(ActivityManager.RecentTaskInfo recentTaskInfo) {
        return recentTaskInfo.baseActivity;
    }

    @SuppressLint({"NewApi"})
    private void runTelephone() {
        TelephonyManager.getDefault().setDataEnabled(!TelephonyManager.getDefault().getDataEnabled());
    }
}
