package com.noname81.lmt;

import android.app.PendingIntent;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
class NotificationDataHelper {
    private static NotificationDataHelper instance;
    private ArrayList<NotificationData> mNotificationList = new ArrayList<>();

    public class NotificationData {
        PendingIntent mDeleteIntent;
        Drawable mIcon;
        String mName;
        PendingIntent mStartIntent;
        String mText;
        long mTime;

        NotificationData(
                String str,
                long j,
                String str2,
                PendingIntent pendingIntent,
                PendingIntent pendingIntent2,
                Drawable drawable) {
            this.mName = str;
            this.mTime = j;
            this.mText = str2;
            this.mStartIntent = pendingIntent;
            this.mDeleteIntent = pendingIntent2;
            this.mIcon = drawable;
        }
    }

    private NotificationDataHelper() {}

    public static NotificationDataHelper getInstance() {
        if (instance == null) {
            instance = new NotificationDataHelper();
        }
        return instance;
    }

    void addNotificationData(
            String str,
            long j,
            String str2,
            PendingIntent pendingIntent,
            PendingIntent pendingIntent2,
            Drawable drawable) {
        int i = 0;
        while (i < this.mNotificationList.size()) {
            if (this.mNotificationList.get(i) != null
                    && this.mNotificationList.get(i).mName != null) {
                if (this.mNotificationList.get(i).mName.equals(str)) {
                    this.mNotificationList.remove(i);
                    if (i > 0) {
                        i--;
                    }
                }
            }
            i++;
        }
        this.mNotificationList.add(
                0, new NotificationData(str, j, str2, pendingIntent, pendingIntent2, drawable));
        if (this.mNotificationList.size() > 5) {
            ArrayList<NotificationData> arrayList = this.mNotificationList;
            arrayList.remove(arrayList.size() - 1);
        }
    }

    int getNotificationDataSize() {
        return this.mNotificationList.size();
    }

    String getNotificationDataName(int i) {
        return (i < 0 || i >= this.mNotificationList.size())
                ? ""
                : this.mNotificationList.get(i).mName;
    }

    long getNotificationDataTime(int i) {
        if (i < 0 || i >= this.mNotificationList.size()) {
            return 0L;
        }
        return this.mNotificationList.get(i).mTime;
    }

    PendingIntent getNotificationDataStartIntent(int i) {
        if (i < 0 || i >= this.mNotificationList.size()) {
            return null;
        }
        return this.mNotificationList.get(i).mStartIntent;
    }

    String getNotificationDataText(int i) {
        return (i < 0 || i >= this.mNotificationList.size())
                ? ""
                : this.mNotificationList.get(i).mText;
    }

    void removeNotificationData(int i) {
        if (i < 0 || i >= this.mNotificationList.size()) {
            return;
        }
        try {
            if (this.mNotificationList.get(i).mDeleteIntent != null) {
                this.mNotificationList.get(i).mDeleteIntent.send();
            }
        } catch (Exception unused) {
        }
        this.mNotificationList.remove(i);
    }
}
