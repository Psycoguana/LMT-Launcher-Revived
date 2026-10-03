package com.noname81.lmt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** ROADMAP 1.13: finding the touchscreen in getevent's device list. */
public class TouchscreenDetectorTest {
    // test phone (1080x2408), as LMT's grep returns it: event4 is a proximity sensor.
    private static final String TEST_PHONE =
            "add device 1: /dev/input/event5\n"
                + "  name:     \"grip_sensor\"\n"
                + "add device 3: /dev/input/event4\n"
                + "  name:     \"sec_touchproximity\"\n"
                + "add device 6: /dev/input/event3\n"
                + "  name:     \"sec_touchscreen\"\n"
                + "                0035  : value 0, min 0, max 1080, fuzz 0, flat 0, resolution 0\n"
                + "                0036  : value 0, min 0, max 2408, fuzz 0, flat 0, resolution 0\n"
                + "add device 7: /dev/input/event0\n"
                + "  name:     \"gpio_keys\"\n";

    @Test
    public void findsTheTouchscreenAndItsFactors() {
        assertArrayEquals(new int[] {3, 100, 100}, TouchscreenDetector.detect(TEST_PHONE, 1080, 2408));
    }

    @Test
    public void factorsFollowATouchscreenWithMoreResolution() {
        String hires = TEST_PHONE.replace("max 1080", "max 2160").replace("max 2408", "max 4816");
        assertArrayEquals(new int[] {3, 200, 200}, TouchscreenDetector.detect(hires, 1080, 2408));
    }

    @Test
    public void twoDigitDeviceNumbers() {
        // The dialog used to take only the last character, turning event10 into 0.
        assertArrayEquals(
                new int[] {10, 100, 100},
                TouchscreenDetector.detect(TEST_PHONE.replace("event3", "event10"), 1080, 2408));
    }

    @Test
    public void onlyTheTouchscreenReportsTouch() {
        assertTrue(TouchscreenDetector.reportsTouch(TEST_PHONE, 3));
        assertFalse(TouchscreenDetector.reportsTouch(TEST_PHONE, 4));
        assertFalse(TouchscreenDetector.reportsTouch(TEST_PHONE, 9));
    }

    @Test
    public void nothingToFind() {
        assertNull(TouchscreenDetector.detect("add device 1: /dev/input/event0\n", 1080, 2408));
        assertNull(TouchscreenDetector.detect(null, 1080, 2408));
        assertNull(TouchscreenDetector.detect(TEST_PHONE, 0, 0));
    }
}
