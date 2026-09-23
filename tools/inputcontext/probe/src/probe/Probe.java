package probe;

import android.os.SystemClock;
import android.view.KeyEvent;

import java.lang.reflect.Method;

/**
 * Standalone probe: which route can inject a key event from app_process under root
 * on this Android version? Pure reflection so it compiles against the public SDK.
 *
 * Run: CLASSPATH=probe.jar app_process /data/local/tmp probe.Probe
 */
public final class Probe {

    private static final int ASYNC = 0;   // INJECT_INPUT_EVENT_MODE_ASYNC

    public static void main(String[] args) {
        System.out.println("sdk=" + android.os.Build.VERSION.SDK_INT);
        report("A InputManager.getInstance()", new Route() {
            public void run() throws Exception { viaGetInstance(); }
        });
        report("B InputManagerGlobal.getInstance()", new Route() {
            public void run() throws Exception { viaGlobal(); }
        });
        report("C IInputManager via ServiceManager", new Route() {
            public void run() throws Exception { viaServiceManager(); }
        });
    }

    interface Route { void run() throws Exception; }

    private static void report(String name, Route r) {
        try {
            r.run();
            System.out.println("  OK      " + name);
        } catch (Throwable t) {
            Throwable c = t.getCause() != null ? t.getCause() : t;
            System.out.println("  FAIL    " + name + "  ->  " + c.getClass().getName()
                    + (c.getMessage() == null ? "" : ": " + c.getMessage()));
        }
    }

    private static KeyEvent[] volumeUp() {
        long now = SystemClock.uptimeMillis();
        return new KeyEvent[] {
            new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP,
                    0, 0, -1, 0, 0, 257),
            new KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP,
                    0, 0, -1, 0, 0, 257),
        };
    }

    /** What LMT does today. */
    private static void viaGetInstance() throws Exception {
        Class<?> im = Class.forName("android.hardware.input.InputManager");
        Object inst = im.getMethod("getInstance").invoke(null);
        Method inject = im.getMethod("injectInputEvent",
                Class.forName("android.view.InputEvent"), int.class);
        for (KeyEvent e : volumeUp()) { inject.invoke(inst, e, ASYNC); }
    }

    /** Android 14+ moved the implementation here. */
    private static void viaGlobal() throws Exception {
        Class<?> g = Class.forName("android.hardware.input.InputManagerGlobal");
        Object inst = g.getMethod("getInstance").invoke(null);
        Method inject = g.getMethod("injectInputEvent",
                Class.forName("android.view.InputEvent"), int.class);
        for (KeyEvent e : volumeUp()) { inject.invoke(inst, e, ASYNC); }
    }

    /** Straight at the binder, the way the shell `input` command ultimately lands. */
    private static void viaServiceManager() throws Exception {
        Class<?> sm = Class.forName("android.os.ServiceManager");
        Object binder = sm.getMethod("getService", String.class).invoke(null, "input");
        if (binder == null) throw new IllegalStateException("no 'input' service binder");
        Class<?> stub = Class.forName("android.hardware.input.IInputManager$Stub");
        Object iim = stub.getMethod("asInterface", Class.forName("android.os.IBinder"))
                .invoke(null, binder);
        Method inject = iim.getClass().getMethod("injectInputEvent",
                Class.forName("android.view.InputEvent"), int.class);
        for (KeyEvent e : volumeUp()) { inject.invoke(iim, e, ASYNC); }
    }
}
