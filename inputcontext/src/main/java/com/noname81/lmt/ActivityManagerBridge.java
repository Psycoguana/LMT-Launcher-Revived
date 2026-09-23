package com.noname81.lmt;

import android.app.ActivityManager;
import android.content.Intent;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * The task-management half of the helper, reached through {@code IActivityManager}.
 *
 * <p>These calls all work on current Android — unlike input injection — so this is a
 * faithful port, not a repair. What reflection buys here is collapsing the five
 * per-SDK jars into one: every difference between them lived in these signatures.
 *
 * <ul>
 *   <li>API 21 swapped a {@code startActivity} parameter type
 *       ({@code IThumbnailReceiver}/{@code ParcelFileDescriptor} → {@code ProfilerInfo})
 *   <li>API 24 made {@code getRecentTasks} return a sliced list needing {@code getList()}
 *   <li>API 28 replaced {@code ActivityManagerNative.getDefault()} with
 *       {@code ActivityManager.getService()}
 *   <li>API 29 gave {@code moveTaskToFront} two extra leading parameters
 * </ul>
 *
 * <p>Every one of those extra or changed parameters is passed as null or zero, so
 * filling the argument array with type defaults and placing the few real values
 * relative to the end of the list absorbs all four differences at runtime.
 */
final class ActivityManagerBridge {

    private static Object service;

    private ActivityManagerBridge() {}

    /** {@code IActivityManager}, however this release exposes it. */
    private static Object service() throws Exception {
        if (service != null) return service;

        // API 26+ : ActivityManager.getService()
        Method getService = Reflect.findMethod(ActivityManager.class, "getService", 0);
        if (getService != null) {
            service = getService.invoke(null);
            if (service != null) return service;
        }
        // older : ActivityManagerNative.getDefault()
        Class<?> amn = Reflect.findClass("android.app.ActivityManagerNative");
        if (amn != null) {
            Method getDefault = Reflect.findMethod(amn, "getDefault", 0);
            if (getDefault != null) {
                service = getDefault.invoke(null);
                if (service != null) return service;
            }
        }
        throw new IllegalStateException("cannot reach IActivityManager on API "
                + android.os.Build.VERSION.SDK_INT);
    }

    static void forceStopPackage(String packageName) throws Exception {
        Object am = service();
        Method m = Reflect.findMethod(am.getClass(), "forceStopPackage", 2);
        if (m == null) throw new NoSuchMethodException("forceStopPackage");
        Object[] args = Reflect.defaults(m);
        args[0] = packageName;   // userId stays 0
        m.invoke(am, args);
    }

    static void moveTaskToFront(int task) throws Exception {
        Object am = service();
        // API 29 prepended (IApplicationThread, String); the task id is third from
        // the end in both shapes, followed by flags and an options Bundle.
        Method m = pickByTrailingIntPair(am, "moveTaskToFront");
        Object[] args = Reflect.defaults(m);
        args[args.length - 3] = task;
        m.invoke(am, args);
    }

    static void startActivity(Intent intent) throws Exception {
        Object am = service();
        Method m = Reflect.findMethod(am.getClass(), "startActivity", 10);
        if (m == null) m = Reflect.findMethod(am.getClass(), "startActivity", 11);
        if (m == null) throw new NoSuchMethodException("startActivity");
        Object[] args = Reflect.defaults(m);
        // (caller, callingPackage, intent, resolvedType, ...) in every version seen
        args[2] = intent;
        args[3] = intent.getType();
        m.invoke(am, args);
    }

    @SuppressWarnings("unchecked")
    static List<ActivityManager.RecentTaskInfo> getRecentTasks(int max) throws Exception {
        Object am = service();
        Method m = Reflect.findMethod(am.getClass(), "getRecentTasks", 3);
        if (m == null) throw new NoSuchMethodException("getRecentTasks");
        Object[] args = Reflect.defaults(m);
        args[0] = max;   // flags and userId stay 0, as in the original
        Object result = m.invoke(am, args);
        return (List<ActivityManager.RecentTaskInfo>) unwrapList(result);
    }

    @SuppressWarnings("unchecked")
    static List<ActivityManager.RunningTaskInfo> getTasks(int max) throws Exception {
        Object am = service();
        Method m = Reflect.findMethod(am.getClass(), "getTasks", 1);
        if (m == null) throw new NoSuchMethodException("getTasks");
        return (List<ActivityManager.RunningTaskInfo>) unwrapList(m.invoke(am, max));
    }

    /** API 24+ wraps the result in a ParceledListSlice; earlier returns a plain List. */
    private static Object unwrapList(Object result) throws Exception {
        if (result == null) return Collections.emptyList();
        if (result instanceof List) return result;
        Method getList = Reflect.findMethod(result.getClass(), "getList", 0);
        if (getList == null) {
            throw new IllegalStateException(
                    "unexpected task list type " + result.getClass().getName());
        }
        Object list = getList.invoke(result);
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * Choose the overload whose parameter list ends in (int task, int flags, Bundle),
     * which is stable across the signature change in API 29.
     */
    private static Method pickByTrailingIntPair(Object am, String name) throws Exception {
        Method best = null;
        for (Method m : am.getClass().getMethods()) {
            if (!m.getName().equals(name)) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length >= 3
                    && p[p.length - 3] == int.class
                    && p[p.length - 2] == int.class) {
                if (best == null || p.length > best.getParameterTypes().length) {
                    m.setAccessible(true);
                    best = m;
                }
            }
        }
        if (best == null) throw new NoSuchMethodException(name);
        return best;
    }
}
