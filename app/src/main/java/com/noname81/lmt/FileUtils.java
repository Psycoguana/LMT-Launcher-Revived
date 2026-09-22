package com.noname81.lmt;

import java.io.File;

/* JADX INFO: loaded from: classes.dex */
class FileUtils {
    FileUtils() {}

    static boolean isFileAvailable(String str, String str2) {
        return new File(str, str2).exists();
    }

    static void deleteFile(String str, String str2) {
        new File(str, str2).delete();
    }

    static void createFolder(String str) {
        new File(str).mkdirs();
    }
}
