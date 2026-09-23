package com.noname81.lmt;

import android.util.Log;
import android.view.InputEvent;

import java.lang.reflect.Method;

/**
 * Injects key and motion events into the system, from a root {@code app_process}.
 *
 * <p>This is the fix for the bug that silently disabled every root key action on
 * modern Android. The original helper had one line:
 *
 * <pre>InputManager.getInstance().injectInputEvent(event, 2);</pre>
 *
 * <p>{@code InputManager.getInstance()} throws {@link NullPointerException} inside a
 * bare {@code app_process} on recent releases — the singleton it returns is only set
 * up for a real application context. The failure is silent from the user's side: the
 * command server logs a stack trace and carries on, so the pie slice simply does
 * nothing.
 *
 * <p>Routes are tried in order and the first that works is remembered. Measured on a
 * test phone (Android 15 / SDK 35) under root, verified by the system volume actually
 * changing rather than by the absence of an exception:
 *
 * <ul>
 *   <li>{@code InputManagerGlobal.getInstance()} — works (the API 34+ home of this)
 *   <li>{@code IInputManager} via {@code ServiceManager} — works, and is far older
 *       API, so it covers releases predating {@code InputManagerGlobal}
 *   <li>{@code InputManager.getInstance()} — the original; kept last so old devices
 *       where it still works are unaffected
 * </ul>
 */
final class InputInjector {

    private static final String TAG = "LMT::InputContext";

    /** INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT, as the original used. */
    private static final int MODE = 2;

    private static Object target;
    private static Method inject;
    private static boolean resolved;

    private InputInjector() {}

    static void inject(InputEvent event) throws Exception {
        if (!resolved) {
            resolve();
            resolved = true;
        }
        if (inject == null) {
            throw new IllegalStateException(
                    "no usable injectInputEvent route on API " + android.os.Build.VERSION.SDK_INT);
        }
        inject.invoke(target, event, MODE);
    }

    private static void resolve() {
        if (tryGlobal() || tryServiceManager() || tryLegacyInputManager()) {
            Log.d(TAG, "injectInputEvent via " + target.getClass().getName());
        } else {
            Log.e(TAG, "no usable injectInputEvent route found");
        }
    }

    /** Android 14+ moved the implementation out of InputManager into this class. */
    private static boolean tryGlobal() {
        return bind(Reflect.findClass("android.hardware.input.InputManagerGlobal"), "getInstance");
    }

    /** The original route. Still correct on releases old enough to have it working. */
    private static boolean tryLegacyInputManager() {
        return bind(Reflect.findClass("android.hardware.input.InputManager"), "getInstance");
    }

    /**
     * Straight at the binder the other two routes eventually reach. Oldest API of the
     * three, so it is the fallback that covers the widest range of releases.
     */
    private static boolean tryServiceManager() {
        try {
            Class<?> serviceManager = Reflect.findClass("android.os.ServiceManager");
            Class<?> stub = Reflect.findClass("android.hardware.input.IInputManager$Stub");
            if (serviceManager == null || stub == null) return false;

            Object binder = serviceManager.getMethod("getService", String.class)
                    .invoke(null, "input");
            if (binder == null) return false;

            Object service = stub.getMethod("asInterface", Class.forName("android.os.IBinder"))
                    .invoke(null, binder);
            Method m = Reflect.findMethod(service.getClass(), "injectInputEvent", 2);
            if (m == null) return false;

            target = service;
            inject = m;
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Resolve {@code owner.factory()} and its two-arg injectInputEvent. */
    private static boolean bind(Class<?> owner, String factory) {
        if (owner == null) return false;
        try {
            Object instance = owner.getMethod(factory).invoke(null);
            if (instance == null) return false;
            Method m = Reflect.findMethod(owner, "injectInputEvent", 2);
            if (m == null) return false;
            target = instance;
            inject = m;
            return true;
        } catch (Throwable t) {
            // getInstance() itself throws here on modern Android - that is the bug.
            return false;
        }
    }
}
