package com.noname81.lmt;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The folder holding the user's own pie images (ROADMAP 1.13: picked with the system file picker).
 * The "resource search path" setting is either a plain path - the default app folder, or one typed
 * in before - or a {@code content://} folder from the picker, which LMT keeps a persistent
 * permission for. Plain paths outside the app folder mostly can't be written since Android 11,
 * which is why the picker exists.
 */
final class IconFolder {
    private static final String TAG = "LMT::IconFolder";

    /** How long a picked folder's listing is reused: building the pie asks for ~40 names. */
    private static final long LISTING_TTL_MS = 3000;

    private static String sCachedFolder;
    private static Map<String, String> sCachedListing;
    private static long sCachedAt;

    private final ContentResolver mResolver;
    private final String mPath;
    private final Uri mTree;

    private IconFolder(ContentResolver resolver, String folder) {
        this.mResolver = resolver;
        if (isPicked(folder)) {
            this.mTree = Uri.parse(folder);
            this.mPath = null;
        } else {
            this.mTree = null;
            this.mPath = folder;
        }
    }

    /** The folder the settings point at. */
    static IconFolder of(Context context) {
        return new IconFolder(
                context.getContentResolver(),
                SettingsValues.getInstance(context).loadResourceSearchPath());
    }

    /** A plain directory, for tests and backup code that has no Context. */
    static IconFolder forPath(String path) {
        return new IconFolder(null, path);
    }

    static boolean isPicked(String folder) {
        return folder != null && folder.startsWith("content://");
    }

    boolean exists(String name) {
        if (this.mTree == null) {
            return new File(this.mPath, name).exists();
        }
        return listing().containsKey(name);
    }

    InputStream open(String name) throws IOException {
        if (this.mTree == null) {
            return new FileInputStream(new File(this.mPath, name));
        }
        String id = listing().get(name);
        if (id == null) {
            throw new IOException(name + " not found");
        }
        return this.mResolver.openInputStream(
                DocumentsContract.buildDocumentUriUsingTree(this.mTree, id));
    }

    /** Creates or overwrites {@code name}. */
    OutputStream create(String name) throws IOException {
        if (this.mTree == null) {
            new File(this.mPath).mkdirs();
            return new FileOutputStream(new File(this.mPath, name));
        }
        String id = listing().get(name);
        Uri document;
        if (id != null) {
            document = DocumentsContract.buildDocumentUriUsingTree(this.mTree, id);
        } else {
            document =
                    DocumentsContract.createDocument(
                            this.mResolver, folderDocument(), "image/png", name);
            if (document == null) {
                throw new IOException("Can't create " + name);
            }
        }
        invalidate();
        return this.mResolver.openOutputStream(document, "wt");
    }

    boolean delete(String name) {
        if (this.mTree == null) {
            return new File(this.mPath, name).delete();
        }
        String id = listing().get(name);
        invalidate();
        try {
            return id != null
                    && DocumentsContract.deleteDocument(
                            this.mResolver,
                            DocumentsContract.buildDocumentUriUsingTree(this.mTree, id));
        } catch (Exception e) {
            Log.w(TAG, "Can't delete " + name + ": " + e);
            return false;
        }
    }

    /** Number of files, or -1 when the folder can't be read. */
    int count() {
        if (this.mTree == null) {
            String[] names = new File(this.mPath).list();
            return names != null ? names.length : -1;
        }
        return listing().size();
    }

    /** Drops the cached listing, after a write or when the setting changes. */
    static synchronized void invalidate() {
        sCachedFolder = null;
        sCachedListing = null;
    }

    private Uri folderDocument() {
        return DocumentsContract.buildDocumentUriUsingTree(
                this.mTree, DocumentsContract.getTreeDocumentId(this.mTree));
    }

    /** Display name -> document id of the picked folder's children. */
    private Map<String, String> listing() {
        String key = this.mTree.toString();
        synchronized (IconFolder.class) {
            if (key.equals(sCachedFolder)
                    && SystemClock.uptimeMillis() - sCachedAt < LISTING_TTL_MS) {
                return sCachedListing;
            }
        }
        Map<String, String> listing = new HashMap<>();
        Uri children =
                DocumentsContract.buildChildDocumentsUriUsingTree(
                        this.mTree, DocumentsContract.getTreeDocumentId(this.mTree));
        try (Cursor cursor =
                this.mResolver.query(
                        children,
                        new String[] {
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME
                        },
                        null,
                        null,
                        null)) {
            while (cursor != null && cursor.moveToNext()) {
                listing.put(cursor.getString(1), cursor.getString(0));
            }
        } catch (Exception e) {
            // Permission revoked or folder gone: behave like an empty folder.
            Log.w(TAG, "Can't list " + key + ": " + e);
        }
        synchronized (IconFolder.class) {
            sCachedFolder = key;
            sCachedListing = listing;
            sCachedAt = SystemClock.uptimeMillis();
        }
        return listing;
    }

    /**
     * A readable name for the settings row: the part of the picker's document id after the volume
     * ("primary:Pictures/LMT" becomes "Pictures/LMT"), or the plain path.
     */
    static String describe(String folder) {
        if (!isPicked(folder)) {
            return folder;
        }
        try {
            String id = DocumentsContract.getTreeDocumentId(Uri.parse(folder));
            int colon = id.indexOf(':');
            String volume = colon > 0 ? id.substring(0, colon) : "";
            String path = colon >= 0 ? id.substring(colon + 1) : id;
            String root = volume.equals("primary") ? "Internal storage" : volume;
            return path.isEmpty() ? root : root + "/" + path;
        } catch (Exception e) {
            return folder;
        }
    }
}
