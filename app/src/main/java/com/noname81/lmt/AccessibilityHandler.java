package com.noname81.lmt;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.Objects;

/* JADX INFO: loaded from: classes.dex */
public class AccessibilityHandler extends AccessibilityService {
    private static boolean DEBUG = false;
    private static final String TAG = "LMT::Accessibility";
    private static AccessibilityHandler instance;
    private static boolean mInitialized;
    private NotificationDataHelper mNotificationDataHelper;

    @Override // android.accessibilityservice.AccessibilityService
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        Notification notification;
        ApplicationInfo applicationInfo;
        Drawable drawable;
        if (accessibilityEvent.getEventType() == 64
                && Objects.equals(accessibilityEvent.getClassName(), "android.app.Notification")
                && (notification = (Notification) accessibilityEvent.getParcelableData()) != null) {
            String strValueOf = String.valueOf(accessibilityEvent.getPackageName());
            PackageManager packageManager = getPackageManager();
            try {
                applicationInfo = packageManager.getApplicationInfo(strValueOf, 0);
                drawable =
                        packageManager
                                .getResourcesForApplication(strValueOf)
                                .getDrawable(notification.icon);
            } catch (Exception unused) {
                applicationInfo = null;
                drawable = null;
            }
            String str =
                    (String)
                            (applicationInfo != null
                                    ? packageManager.getApplicationLabel(applicationInfo)
                                    : "");
            long jCurrentTimeMillis =
                    (System.currentTimeMillis() - SystemClock.uptimeMillis())
                            + accessibilityEvent.getEventTime();
            String strValueOf2 =
                    accessibilityEvent.getText().size() > 0
                            ? String.valueOf(accessibilityEvent.getText().get(0))
                            : getString(R.string.pie_status_info_no_additional_info);
            if (this.mNotificationDataHelper == null) {
                this.mNotificationDataHelper = NotificationDataHelper.getInstance();
            }
            NotificationDataHelper notificationDataHelper = this.mNotificationDataHelper;
            if (notificationDataHelper != null) {
                notificationDataHelper.addNotificationData(
                        str,
                        jCurrentTimeMillis,
                        strValueOf2,
                        notification.contentIntent,
                        notification.deleteIntent,
                        drawable);
            }
        }
    }

    @Override // android.accessibilityservice.AccessibilityService
    protected void onServiceConnected() {
        if (mInitialized) {
            instance = this;
            return;
        }
        AccessibilityServiceInfo accessibilityServiceInfo = new AccessibilityServiceInfo();
        accessibilityServiceInfo.eventTypes = 64;
        accessibilityServiceInfo.feedbackType = 16;
        setServiceInfo(accessibilityServiceInfo);
        this.mNotificationDataHelper = NotificationDataHelper.getInstance();
        instance = this;
        mInitialized = true;
    }

    @Override // android.accessibilityservice.AccessibilityService
    public void onInterrupt() {
        instance = null;
        mInitialized = false;
    }

    static void setDebug(int i) {
        DEBUG = i == 1;
    }

    static boolean isAccessibilityAvailable(Context context, boolean z) {
        boolean z2 = Build.VERSION.SDK_INT >= 16 && mInitialized && instance != null;
        if (z && !z2) {
            Toast.makeText(context, R.string.accessibility_service_activate, 0).show();
            Log.e(TAG, String.valueOf(R.string.accessibility_service_activate));
        }
        return z2;
    }

    static void performAction(int i) {
        AccessibilityHandler accessibilityHandler;
        if (DEBUG) {
            Log.d(TAG, "performAction(" + i + ")");
        }
        if (Build.VERSION.SDK_INT < 16
                || !mInitialized
                || (accessibilityHandler = instance) == null) {
            return;
        }
        boolean zPerformGlobalAction = accessibilityHandler.performGlobalAction(i);
        if (DEBUG) {
            Log.d(TAG, "result: " + zPerformGlobalAction);
        }
    }

    private static GestureDescription createClick(float f, float f2) {
        Path path = new Path();
        path.moveTo(f, f2);
        GestureDescription.StrokeDescription strokeDescription =
                new GestureDescription.StrokeDescription(path, 0L, 1L);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(strokeDescription);
        return builder.build();
    }

    static void performClick(float f, float f2) {
        AccessibilityHandler accessibilityHandler;
        if (DEBUG) {
            Log.d(TAG, "performClick(x " + f + " y " + f2 + ")");
        }
        if (Build.VERSION.SDK_INT < 24
                || !mInitialized
                || (accessibilityHandler = instance) == null) {
            return;
        }
        boolean zDispatchGesture =
                accessibilityHandler.dispatchGesture(
                        createClick(f, f2),
                        new AccessibilityService
                                .GestureResultCallback() { // from class:
                                                           // com.noname81.lmt.AccessibilityHandler.1
                            @Override // android.accessibilityservice.AccessibilityService.GestureResultCallback
                            public void onCompleted(GestureDescription gestureDescription) {
                                super.onCompleted(gestureDescription);
                                if (AccessibilityHandler.DEBUG) {
                                    Log.d(AccessibilityHandler.TAG, "gesture completed");
                                }
                            }

                            @Override // android.accessibilityservice.AccessibilityService.GestureResultCallback
                            public void onCancelled(GestureDescription gestureDescription) {
                                super.onCancelled(gestureDescription);
                                if (AccessibilityHandler.DEBUG) {
                                    Log.d(AccessibilityHandler.TAG, "gesture cancelled");
                                }
                            }
                        },
                        null);
        if (DEBUG) {
            Log.d(TAG, "gesture result: " + zDispatchGesture);
        }
    }
}
