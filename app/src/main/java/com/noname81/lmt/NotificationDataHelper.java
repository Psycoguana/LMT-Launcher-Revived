package com.noname81.lmt;

import android.app.PendingIntent;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;

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
                String name,
                long postedAt,
                String text,
                PendingIntent startIntent,
                PendingIntent deleteIntent,
                Drawable icon) {
            this.mName = name;
            this.mTime = postedAt;
            this.mText = text;
            this.mStartIntent = startIntent;
            this.mDeleteIntent = deleteIntent;
            this.mIcon = icon;
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
            String name,
            long postedAt,
            String text,
            PendingIntent startIntent,
            PendingIntent deleteIntent,
            Drawable icon) {
        int index = 0;
        while (index < this.mNotificationList.size()) {
            if (this.mNotificationList.get(index) != null
                    && this.mNotificationList.get(index).mName != null) {
                if (this.mNotificationList.get(index).mName.equals(name)) {
                    this.mNotificationList.remove(index);
                    if (index > 0) {
                        index--;
                    }
                }
            }
            index++;
        }
        this.mNotificationList.add(
                0, new NotificationData(name, postedAt, text, startIntent, deleteIntent, icon));
        if (this.mNotificationList.size() > 5) {
            ArrayList<NotificationData> arrayList = this.mNotificationList;
            arrayList.remove(arrayList.size() - 1);
        }
    }

    int getNotificationDataSize() {
        return this.mNotificationList.size();
    }

    String getNotificationDataName(int index) {
        return (index < 0 || index >= this.mNotificationList.size())
                ? ""
                : this.mNotificationList.get(index).mName;
    }

    long getNotificationDataTime(int index) {
        if (index < 0 || index >= this.mNotificationList.size()) {
            return 0L;
        }
        return this.mNotificationList.get(index).mTime;
    }

    PendingIntent getNotificationDataStartIntent(int index) {
        if (index < 0 || index >= this.mNotificationList.size()) {
            return null;
        }
        return this.mNotificationList.get(index).mStartIntent;
    }

    String getNotificationDataText(int index) {
        return (index < 0 || index >= this.mNotificationList.size())
                ? ""
                : this.mNotificationList.get(index).mText;
    }

    void removeNotificationData(int index) {
        if (index < 0 || index >= this.mNotificationList.size()) {
            return;
        }
        try {
            if (this.mNotificationList.get(index).mDeleteIntent != null) {
                this.mNotificationList.get(index).mDeleteIntent.send();
            }
        } catch (Exception unused) {
        }
        this.mNotificationList.remove(index);
    }
}
