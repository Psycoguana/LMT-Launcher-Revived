package com.noname81.lmt;

import java.io.File;

/** Small helpers for the custom-icon files under the user's resource search path. */
class FileUtils {

    FileUtils() {}

    static boolean isFileAvailable(String directory, String fileName) {
        return new File(directory, fileName).exists();
    }

    static void deleteFile(String directory, String fileName) {
        new File(directory, fileName).delete();
    }

    static void createFolder(String path) {
        new File(path).mkdirs();
    }
}
