package com.noname81.lmt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;

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

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class TouchServiceThread extends HandlerThread {
        boolean stopped;

        TouchServiceThread(String name) {
            super(name);
            this.stopped = false;
        }

        @Override // android.os.HandlerThread, java.lang.Thread, java.lang.Runnable
        public void run() {
            while (!this.stopped) {
                TouchService.this.mMessageHandler.sendMessage(
                        Message.obtain(
                                TouchService.this.mMessageHandler,
                                0,
                                TouchService.this.mTouchServiceNative.run()));
            }
        }

        @Override // android.os.HandlerThread
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

        @Override // android.os.Handler
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

    @Override // android.app.Service
    public void onCreate() {
        Log.d(TAG, "TouchService created");
        this.mSettings = SettingsValues.getInstance(this);
        this.mToaster = Toaster.getInstance(this);
        this.mLauncher = Launcher.getInstance(this);
        Log.d(TAG, "App version " + this.mSettings.getVersion());
        this.mSettings.rotate();
        if (this.mSettings.loadTouchServiceMode() < 2) {
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
            }
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
        File file = new File(this.mSettings.loadResourceSearchPath());
        String[] list = file.list();
        int length = list != null ? list.length : 0;
        Log.d(TAG, "Resource path " + this.mSettings.loadResourceSearchPath());
        StringBuilder sb = new StringBuilder("Resource path is ");
        sb.append(file.isDirectory() ? "available" : "not available");
        sb.append(" containing ");
        sb.append(length);
        sb.append(" file(s)");
        Log.d(TAG, sb.toString());
        this.mSettings.setServiceState(1);
        startForeground(777, createNotification());
    }

    Notification createNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            ((NotificationManager) getSystemService("notification"))
                    .createNotificationChannel(
                            new NotificationChannel(
                                    this.NOTIFICATION_CHANNEL_ID,
                                    this.NOTIFICATION_CHANNEL_NAME,
                                    0));
        }
        if (Build.VERSION.SDK_INT >= 18) {
            PendingIntent activity =
                    PendingIntent.getActivity(
                            this, 777, new Intent(this, (Class<?>) LMT.class), 67108864);
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
        return new Notification();
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "LMT started", 0).show();
        Log.d(TAG, "TouchService started");
        return 1;
    }

    @Override // android.app.Service
    public void onDestroy() {
        Toast.makeText(this, "LMT stopped", 0).show();
        Log.d(TAG, "TouchService stopped");
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

    @Override // android.app.Service, android.content.ComponentCallbacks
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
