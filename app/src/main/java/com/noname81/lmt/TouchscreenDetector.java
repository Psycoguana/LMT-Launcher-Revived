package com.noname81.lmt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the touchscreen among the input devices, from the output of {@code getevent -p | grep -E
 * '(device|name|0035|0036)'}: the device whose axes include ABS_MT_POSITION_X (0035) followed by
 * ABS_MT_POSITION_Y (0036). Used by Auto configuration and, since ROADMAP 1.13, by the service when
 * the saved device can't be the touchscreen (the default, event4, is a proximity sensor on the
 * test phone, so gestures silently did nothing).
 */
final class TouchscreenDetector {
    private static final Pattern DEVICE = Pattern.compile("/dev/input/event(\\d+)");
    private static final Pattern MAX = Pattern.compile("max (\\d+)");

    private TouchscreenDetector() {}

    /**
     * The touchscreen's device number and its touchscreen-to-screen factors in percent, or null
     * when no device reports touch positions. The screen size must be in natural orientation, which
     * is what the touchscreen's axes follow. With several, the last one wins, as before.
     */
    static int[] detect(String getevent, int naturalWidth, int naturalHeight) {
        if (getevent == null || naturalWidth <= 0 || naturalHeight <= 0) {
            return null;
        }
        String[] lines = getevent.split("\n");
        int[] found = null;
        int device = -1;
        for (int line = 0; line < lines.length; line++) {
            Matcher deviceMatcher = DEVICE.matcher(lines[line]);
            if (deviceMatcher.find()) {
                device = Integer.parseInt(deviceMatcher.group(1));
            } else if (device >= 0
                    && lines[line].contains("0035")
                    && line + 1 < lines.length
                    && lines[line + 1].contains("0036")) {
                int maxX = max(lines[line]);
                int maxY = max(lines[line + 1]);
                if (maxX > 0 && maxY > 0) {
                    found =
                            new int[] {
                                device,
                                Math.round(maxX / (float) naturalWidth * 100.0f),
                                Math.round(maxY / (float) naturalHeight * 100.0f)
                            };
                }
            }
        }
        return found;
    }

    /** Whether {@code device} is listed with touch positions (ABS_MT_POSITION_X). */
    static boolean reportsTouch(String getevent, int device) {
        boolean inDevice = false;
        for (String line : getevent.split("\n")) {
            Matcher deviceMatcher = DEVICE.matcher(line);
            if (deviceMatcher.find()) {
                inDevice = Integer.parseInt(deviceMatcher.group(1)) == device;
            } else if (inDevice && line.contains("0035")) {
                return true;
            }
        }
        return false;
    }

    private static int max(String axisLine) {
        Matcher matcher = MAX.matcher(axisLine);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }
}
