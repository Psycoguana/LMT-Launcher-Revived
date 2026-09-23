package com.noname81.lmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The gesture catalogue, which is parallel to {@link Action}'s but drives the native
 * recogniser instead of the picker.
 *
 * <p>{@code SettingsValuesBase.loadActions()} iterates {@code names.length} to build
 * one stored slot per gesture, and {@code Toaster} looks up a drawable by the
 * lower-cased name, so the array's length and contents are a storage and resource
 * contract, not just a label list.
 */
public class TouchServiceResultTest {

    @Test
    public void namesAndCaptionsStayInStep() {
        assertEquals(TouchServiceResult.names.length, TouchServiceResult.captions.length);
    }

    @Test
    public void thereAreFourteenGestures() {
        // The native library recognises exactly these; adding one means rebuilding
        // libTouchServiceNative.so, not just extending the array (ROADMAP 1.8).
        assertEquals(14, TouchServiceResult.names.length);
    }

    @Test
    public void everyNameIsUsableAsADrawableIdentifier() {
        // Toaster resolves getIdentifier(name.toLowerCase(), "drawable", ...), so a
        // name with a space or punctuation would silently resolve to nothing.
        for (String name : TouchServiceResult.names) {
            assertTrue("not a usable resource name: " + name,
                    name.matches("[A-Za-z][A-Za-z0-9_]*"));
        }
    }

    @Test
    public void namesAreUnique() {
        // They are used as preference key prefixes ("<name> Type"/" String"/" Icon"),
        // so a duplicate would make two gestures share stored state.
        for (int i = 0; i < TouchServiceResult.names.length; i++) {
            for (int j = i + 1; j < TouchServiceResult.names.length; j++) {
                assertTrue("duplicate gesture name: " + TouchServiceResult.names[i],
                        !TouchServiceResult.names[i].equals(TouchServiceResult.names[j]));
            }
        }
    }
}
