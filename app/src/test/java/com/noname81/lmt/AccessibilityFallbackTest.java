package com.noname81.lmt;

import static org.junit.Assert.assertEquals;

import android.accessibilityservice.AccessibilityService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * ROADMAP 2.1: which root-only actions get an accessibility global action, from which Android
 * version. Launcher itself can't be built here (its RootContext execs {@code su}), so this pins
 * the version gate that {@code Launcher.performAccessibilityFallback} relies on.
 *
 * <p>Only SDK levels the rest of the suite already loads (30, 33): every extra level loads
 * another android-all jar into the one test JVM, and a handful more ran it out of memory.
 */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityFallbackTest {

    @Test
    @Config(sdk = 30)
    public void android11HasScreenshotButNoDrawerOrMenu() {
        assertEquals(
                AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT,
                Launcher.accessibilityFallback(Action.Screenshot));
        assertEquals(0, Launcher.accessibilityFallback(Action.Nova));
        assertEquals(0, Launcher.accessibilityFallback(Action.Menu));
    }

    @Test
    @Config(sdk = 33)
    public void everyLauncherDrawerOpensAllAppsFromAndroid12() {
        for (int drawer : new int[] {Action.Nova, Action.Apex, Action.Holo}) {
            assertEquals(
                    AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS,
                    Launcher.accessibilityFallback(drawer));
        }
    }

    @Test
    @Config(sdk = 33)
    public void menuStillNeedsRootBeforeAndroid16() {
        // GLOBAL_ACTION_MENU is API 36, newer than this Robolectric can run.
        assertEquals(0, Launcher.accessibilityFallback(Action.Menu));
    }

    @Test
    @Config(sdk = 33)
    public void actionsWithoutAFallbackReturnZero() {
        assertEquals(0, Launcher.accessibilityFallback(Action.MenuLongpress));
        assertEquals(0, Launcher.accessibilityFallback(Action.KillApp));
    }
}
