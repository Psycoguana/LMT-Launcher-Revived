package com.noname81.lmt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * ROADMAP 1.13: which actions the picker greys out when root is missing or denied. Derived from
 * the caption tags, with version-gated accessibility routes checked against the running SDK
 * (only 30 and 33 here, see AccessibilityFallbackTest for why).
 */
@RunWith(RobolectricTestRunner.class)
public class ActionNeedsRootTest {

    @Test
    @Config(sdk = 33)
    public void rootOnlyTagsNeedRoot() {
        assertTrue(Action.needsRoot(Action.HomeLongpress)); // [Root]
        assertTrue(Action.needsRoot(Action.ImmersiveModeToggle)); // [Root]
    }

    @Test
    @Config(sdk = 33)
    public void anyOtherWayMeansNoRootNeeded() {
        assertFalse(Action.needsRoot(Action.None)); // no tag
        assertFalse(Action.needsRoot(Action.App)); // [Root|None]
        assertFalse(Action.needsRoot(Action.Back)); // [Accessibility|Root]
        assertFalse(Action.needsRoot(Action.OpenKeyboard)); // [None]
    }

    @Test
    @Config(sdk = 33)
    public void versionGatedAccessibilityCountsFromItsVersion() {
        assertFalse(Action.needsRoot(Action.Screenshot)); // Accessibility (Android 9+)
        assertFalse(Action.needsRoot(Action.Nova)); // Accessibility (Android 12+)
        assertTrue(Action.needsRoot(Action.Menu)); // Accessibility (Android 16+)
    }

    @Test
    @Config(sdk = 30)
    public void drawerFallbackNeedsAndroid12() {
        assertTrue(Action.needsRoot(Action.Nova));
        assertFalse(Action.needsRoot(Action.Screenshot));
    }

    @Test
    @Config(sdk = 33)
    public void everyActionHasAnAnswer() {
        for (int type = 0; type < Action.captions.length; type++) {
            Action.needsRoot(type);
        }
    }
}
