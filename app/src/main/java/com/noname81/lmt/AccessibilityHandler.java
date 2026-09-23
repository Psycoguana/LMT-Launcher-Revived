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

/**
 * Accessibility service, used for two unrelated jobs.
 *
 * <p>It performs the global actions an app cannot otherwise reach — back, home, recents,
 * notification shade, quick settings, power dialog, split screen — and it dispatches synthetic taps
 * for {@link PiePointer}. Separately, it listens for notification events and feeds {@link
 * NotificationDataHelper}, which drives the badge in the pie's centre.
 *
 * <p>Since root key injection broke on modern Android (see docs/ROADMAP.md 3.1), this is the only
 * working path for home, back and recents.
 */
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
            String appLabel =
                    (String)
                            (applicationInfo != null
                                    ? packageManager.getApplicationLabel(applicationInfo)
                                    : "");
            long postedAt =
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
                        appLabel,
                        postedAt,
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

    static void setDebug(int enabled) {
        DEBUG = enabled == 1;
    }

    static boolean isAccessibilityAvailable(Context context, boolean log) {
        boolean available = Build.VERSION.SDK_INT >= 16 && mInitialized && instance != null;
        if (log && !available) {
            Toast.makeText(context, R.string.accessibility_service_activate, 0).show();
            Log.e(TAG, String.valueOf(R.string.accessibility_service_activate));
        }
        return available;
    }

    static void performAction(int globalAction) {
        AccessibilityHandler accessibilityHandler;
        if (DEBUG) {
            Log.d(TAG, "performAction(" + globalAction + ")");
        }
        if (Build.VERSION.SDK_INT < 16
                || !mInitialized
                || (accessibilityHandler = instance) == null) {
            return;
        }
        boolean zPerformGlobalAction = accessibilityHandler.performGlobalAction(globalAction);
        if (DEBUG) {
            Log.d(TAG, "result: " + zPerformGlobalAction);
        }
    }

    private static GestureDescription createClick(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription strokeDescription =
                new GestureDescription.StrokeDescription(path, 0L, 1L);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(strokeDescription);
        return builder.build();
    }

    static void performClick(float x, float y) {
        AccessibilityHandler accessibilityHandler;
        if (DEBUG) {
            Log.d(TAG, "performClick(x " + x + " y " + y + ")");
        }
        if (Build.VERSION.SDK_INT < 24
                || !mInitialized
                || (accessibilityHandler = instance) == null) {
            return;
        }
        boolean zDispatchGesture =
                accessibilityHandler.dispatchGesture(
                        createClick(x, y),
                        new AccessibilityService.GestureResultCallback() { // from class:
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
