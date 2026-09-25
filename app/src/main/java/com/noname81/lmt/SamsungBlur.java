package com.noname81.lmt;

import android.util.Log;
import android.view.View;

import java.lang.reflect.Method;

/**
 * Samsung's live window blur, {@code View.semSetBlurInfo(SemBlurInfo)} with {@code
 * BLUR_MODE_WINDOW}, reached by reflection (ROADMAP 1.11 upgrade). One UI only turns it on for
 * phones with {@code SEC_FLOATING_FEATURE_GRAPHICS_SUPPORT_3D_SURFACE_TRANSITION_FLAG} (the Samsung flagship has
 * it, the test phone logs "This model doesn't support window blur!"), so that flag decides.
 */
final class SamsungBlur {
    private static final String TAG = "LMT::SamsungBlur";
    private static final String FEATURE =
            "SEC_FLOATING_FEATURE_GRAPHICS_SUPPORT_3D_SURFACE_TRANSITION_FLAG";
    private static final int BLUR_MODE_WINDOW = 0;
    private static Boolean sAvailable;

    private SamsungBlur() {}

    static synchronized boolean isAvailable() {
        if (sAvailable == null) {
            sAvailable = false;
            try {
                Class<?> feature = Class.forName("com.samsung.android.feature.SemFloatingFeature");
                Object instance = feature.getMethod("getInstance").invoke(null);
                boolean supported =
                        (Boolean)
                                feature.getMethod("getBoolean", String.class)
                                        .invoke(instance, FEATURE);
                Class.forName("android.view.SemBlurInfo");
                sAvailable = supported;
            } catch (Throwable t) {
                // Not a Samsung phone, or an older One UI.
            }
            Log.d(TAG, "Live window blur " + (sAvailable ? "available" : "not available"));
        }
        return sAvailable;
    }

    /** Blurs what is behind {@code view}; false if the call failed. */
    static boolean apply(View view, int radius, float cornerRadius) {
        try {
            Class<?> info = Class.forName("android.view.SemBlurInfo");
            Class<?> builderClass = Class.forName("android.view.SemBlurInfo$Builder");
            Object builder = builderClass.getConstructor(int.class).newInstance(BLUR_MODE_WINDOW);
            builderClass.getMethod("setRadius", int.class).invoke(builder, radius);
            builderClass.getMethod("setBackgroundColor", int.class).invoke(builder, 0);
            if (cornerRadius > 0) {
                builderClass
                        .getMethod("setBackgroundCornerRadius", float.class)
                        .invoke(builder, cornerRadius);
            }
            Object built = builderClass.getMethod("build").invoke(builder);
            Method set = View.class.getMethod("semSetBlurInfo", info);
            set.invoke(view, built);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "semSetBlurInfo failed", t);
            return false;
        }
    }
}
