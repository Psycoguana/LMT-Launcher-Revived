package com.noname81.lmt;

import java.lang.reflect.Method;

/**
 * Minimal reflection helpers.
 *
 * <p>Everything this helper process needs from the framework is hidden API:
 * {@code IActivityManager}, {@code InputManagerGlobal}, {@code TelephonyManager
 * .getDefault()}. Those are not on the public {@code android.jar}, so calling them
 * directly would require a framework stub jar — a binary dependency of uncertain
 * provenance, which is exactly what building this helper from source is meant to
 * remove.
 *
 * <p>Going through reflection has a second, larger payoff. The five prebuilt
 * {@code InputContext<sdk>.jar} variants this replaces existed only because hidden
 * API <em>signatures</em> changed between releases, which a runtime {@code SDK_INT}
 * branch cannot express at compile time. Reflection defers signature resolution to
 * runtime, so all five collapse into this one source tree.
 *
 * <p>The cost is losing compile-time checking on these calls, so the reflective
 * surface is deliberately confined to {@link InputInjector},
 * {@link ActivityManagerBridge} and {@link TelephonyBridge} rather than scattered
 * through the command server.
 */
final class Reflect {

    private Reflect() {}

    /** The first method on {@code owner} with this name and arity, or null. */
    static Method findMethod(Class<?> owner, String name, int arity) {
        for (Method m : owner.getMethods()) {
            if (m.getName().equals(name) && m.getParameterTypes().length == arity) {
                m.setAccessible(true);
                return m;
            }
        }
        for (Method m : owner.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterTypes().length == arity) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    /** The class, or null when this Android version does not have it. */
    static Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Build the argument array for {@code method}, filling every parameter with the
     * type's default (null / 0) and then overwriting the positions given.
     *
     * <p>This is what absorbs the signature drift that used to require a separate
     * jar per SDK level: {@code moveTaskToFront} gained two leading parameters in
     * API 29 and {@code startActivity} swapped a parameter type in API 21, but in
     * both cases the values LMT passes are nulls and zeros. Placing the few real
     * arguments by index, relative to the end of the parameter list, survives both.
     */
    static Object[] defaults(Method method) {
        Class<?>[] types = method.getParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            args[i] = defaultValue(types[i]);
        }
        return args;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return Boolean.FALSE;
        if (type == char.class) return (char) 0;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        return 0d;
    }
}
