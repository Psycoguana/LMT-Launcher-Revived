package com.noname81.lmt;

import android.telephony.TelephonyManager;

import java.lang.reflect.Method;

/**
 * Toggles mobile data, which has no public API at all — hence root, and hence
 * {@code TelephonyManager.getDefault()}, which is hidden.
 */
final class TelephonyBridge {

    private TelephonyBridge() {}

    static void toggleData() throws Exception {
        Method getDefault = Reflect.findMethod(TelephonyManager.class, "getDefault", 0);
        if (getDefault == null) throw new NoSuchMethodException("TelephonyManager.getDefault");
        Object tm = getDefault.invoke(null);

        Method get = Reflect.findMethod(tm.getClass(), "getDataEnabled", 0);
        Method set = Reflect.findMethod(tm.getClass(), "setDataEnabled", 1);
        if (get == null || set == null) {
            throw new NoSuchMethodException("get/setDataEnabled");
        }
        boolean enabled = (Boolean) get.invoke(tm);
        set.invoke(tm, !enabled);
    }
}
