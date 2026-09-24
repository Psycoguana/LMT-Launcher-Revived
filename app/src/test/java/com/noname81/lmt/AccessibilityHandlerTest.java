package com.noname81.lmt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * ROADMAP 3.5: the accessibility service "silently unbinds while Android still shows it enabled".
 *
 * <p>The cause was {@code onInterrupt()} tearing down LMT's availability state. The system calls
 * onInterrupt() on every bound service whenever anything calls {@code
 * AccessibilityManager.interrupt()} - it means "stop your current feedback", not "you are
 * disconnected". The service stayed bound (so Settings showed it on), but LMT treated it as gone
 * until a toggle off/on re-ran onServiceConnected - exactly the reported workaround.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class AccessibilityHandlerTest {

    private Context context;
    private AccessibilityHandler service;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        service = Robolectric.setupService(AccessibilityHandler.class);
    }

    @After
    public void tearDown() {
        // Availability lives in statics; leave nothing behind for the next test.
        service.onUnbind(new Intent());
    }

    @Test
    public void aConnectedServiceIsAvailable() {
        service.onServiceConnected();
        assertTrue(AccessibilityHandler.isAccessibilityAvailable(context, false));
    }

    @Test
    public void anInterruptDoesNotMakeABoundServiceUnavailable() {
        service.onServiceConnected();
        service.onInterrupt();
        assertTrue(AccessibilityHandler.isAccessibilityAvailable(context, false));
    }

    @Test
    public void unbindingMakesTheServiceUnavailable() {
        service.onServiceConnected();
        service.onUnbind(new Intent());
        assertFalse(AccessibilityHandler.isAccessibilityAvailable(context, false));
    }

    @Test
    public void reconnectingAfterAnUnbindMakesItAvailableAgain() {
        service.onServiceConnected();
        service.onUnbind(new Intent());
        service.onServiceConnected();
        assertTrue(AccessibilityHandler.isAccessibilityAvailable(context, false));
    }
}
