package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** ROADMAP 1.13: the watchdog that stops a gesture thread spinning on a dead input source. */
public class TouchServiceSpinTest {
    // Real line from the test phone while spinning; the thread name holds a space-free prefix only.
    private static final String STAT =
            "15790 (TouchServiceThr) R 1076 1076 0 0 -1 4194368 227 0 0 0 1297 4130 0 0 20 0 38 0";

    @Test
    public void readsUserPlusSystemTicks() {
        assertEquals(1297 + 4130, TouchService.parseCpuTicks(STAT));
    }

    @Test
    public void threadNameMayContainSpacesAndParentheses() {
        assertEquals(7, TouchService.parseCpuTicks(
                "1 (a (b) c) S 1 1 0 0 -1 0 0 0 0 0 3 4 0 0 20 0 1 0"));
    }

    @Test
    public void malformedStatIsUnreadable() {
        assertEquals(-1, TouchService.parseCpuTicks(""));
        assertEquals(-1, TouchService.parseCpuTicks("1 (x) S 1 2"));
    }

    @Test
    public void spinningMeansOverHalfACore() {
        // 100 ticks/s: 10 s window
        assertTrue(TouchService.isSpinning(960, 10000, 100)); // 96%, as measured
        assertTrue(TouchService.isSpinning(501, 10000, 100));
        assertFalse(TouchService.isSpinning(500, 10000, 100));
        assertFalse(TouchService.isSpinning(0, 10000, 100)); // blocked in read()
        assertFalse(TouchService.isSpinning(5, 0, 100));
    }
}
