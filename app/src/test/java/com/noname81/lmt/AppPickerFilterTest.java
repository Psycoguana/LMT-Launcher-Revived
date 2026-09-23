package com.noname81.lmt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.pm.ApplicationInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * The two predicates behind the app picker's search box and system-app toggle.
 *
 * <p>Worth pinning because everything downstream depends on them agreeing with the
 * model rebuild: {@code onItemClick} resolves a tap as {@code mPackageInfos.get(
 * position - 1)}, so if a row is displayed the corresponding model entry must exist,
 * and vice versa.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class AppPickerFilterTest {

    private static ApplicationInfo appWithFlags(int flags) {
        ApplicationInfo info = new ApplicationInfo();
        info.flags = flags;
        return info;
    }

    @Test
    public void anEmptyQueryMatchesEverything() {
        assertTrue(MultiSelectActivity.matches("", "Chrome", "com.android.chrome"));
        assertTrue(MultiSelectActivity.matches(null, "Chrome", "com.android.chrome"));
    }

    @Test
    public void matchingIsCaseInsensitive() {
        assertTrue(MultiSelectActivity.matches("CHR", "Chrome", "com.android.chrome"));
        assertTrue(MultiSelectActivity.matches("chr", "CHROME", "com.android.chrome"));
    }

    @Test
    public void thePackageNameIsSearchableToo() {
        // Users look for "com.whatsapp" as readily as for the display name.
        assertTrue(MultiSelectActivity.matches("whatsapp", "WhatsApp Messenger", "com.whatsapp"));
        assertTrue(MultiSelectActivity.matches("com.and", "Chrome", "com.android.chrome"));
    }

    @Test
    public void nonMatchesAreRejected() {
        assertFalse(MultiSelectActivity.matches("firefox", "Chrome", "com.android.chrome"));
    }

    @Test
    public void aNullFieldDoesNotThrow() {
        // ApplicationInfo.name can be null for a package with no loadable label.
        assertFalse(MultiSelectActivity.matches("chrome", null, null));
        assertTrue(MultiSelectActivity.matches("chrome", null, "com.android.chrome"));
    }

    @Test
    public void systemAppsAreRecognised() {
        assertTrue(MultiSelectActivity.isSystem(appWithFlags(ApplicationInfo.FLAG_SYSTEM)));
        assertFalse(MultiSelectActivity.isSystem(appWithFlags(0)));
    }

    @Test
    public void anUpdatedSystemAppCountsAsAUserApp() {
        // Chrome and the like ship preinstalled but are updated from the store; they
        // behave like user apps and should not disappear when system apps are hidden.
        assertFalse(
                MultiSelectActivity.isSystem(
                        appWithFlags(
                                ApplicationInfo.FLAG_SYSTEM
                                        | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)));
    }
}
