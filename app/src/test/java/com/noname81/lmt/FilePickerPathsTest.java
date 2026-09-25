package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.net.Uri;
import android.provider.DocumentsContract;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** ROADMAP 1.13: turning what the system file picker returns into what LMT needs. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class FilePickerPathsTest {
    private static final String STORAGE = "com.android.externalstorage.documents";

    private static Uri document(String authority, String id) {
        return DocumentsContract.buildDocumentUri(authority, id);
    }

    @Test
    public void scriptOnInternalStorageBecomesAPath() {
        assertEquals(
                "/storage/emulated/0/Download/lmt.sh",
                CommandSelectActivity.filePathOf(document(STORAGE, "primary:Download/lmt.sh")));
    }

    @Test
    public void scriptOnAnSdCardBecomesAPath() {
        assertEquals(
                "/storage/1A2B-3C4D/scripts/lmt.sh",
                CommandSelectActivity.filePathOf(document(STORAGE, "1A2B-3C4D:scripts/lmt.sh")));
    }

    @Test
    public void downloadsRawIdsCarryThePath() {
        assertEquals(
                "/storage/emulated/0/Download/lmt.sh",
                CommandSelectActivity.filePathOf(
                        document(
                                "com.android.providers.downloads.documents",
                                "raw:/storage/emulated/0/Download/lmt.sh")));
    }

    @Test
    public void cloudDocumentsHaveNoPath() {
        assertNull(
                CommandSelectActivity.filePathOf(
                        document("com.google.android.apps.docs.storage", "doc=abc123")));
        assertNull(CommandSelectActivity.filePathOf(null));
    }

    @Test
    public void pickedFoldersAreDescribedReadably() {
        String folder =
                DocumentsContract.buildTreeDocumentUri(STORAGE, "primary:Pictures/LMT").toString();
        assertTrue(IconFolder.isPicked(folder));
        assertEquals("Internal storage/Pictures/LMT", IconFolder.describe(folder));
        assertFalse(
                IconFolder.isPicked("/storage/emulated/0/Android/data/com.noname81.lmt/files/"));
    }
}
