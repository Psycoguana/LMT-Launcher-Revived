package com.noname81.lmt;

import android.content.Context;

/* JADX INFO: loaded from: classes.dex */
class DebugHelper {
    private static DebugHelper instance;
    private boolean mDebugLauncherEnabled = false;
    private boolean mDebugTouchServiceEnabled = false;
    private boolean mDebugPieEnabled = false;
    private boolean mNoRootEnabled = true;

    private DebugHelper() {}

    public static DebugHelper getInstance() {
        if (instance == null) {
            instance = new DebugHelper();
        }
        return instance;
    }

    void showDebugMenu(final Context context) {
        new InputDialog(
                context,
                context.getString(R.string.dialog_set_debug_mode),
                new String[] {
                    context.getString(R.string.dialog_none),
                    context.getString(R.string.dialog_launcher_debug),
                    context.getString(R.string.dialog_touch_service_debug),
                    context.getString(R.string.dialog_pie_service_debug),
                    context.getString(R.string.dialog_no_root)
                },
                Integer.toString(0)) { // from class: com.noname81.lmt.DebugHelper.1
            @Override // com.noname81.lmt.InputDialog
            public boolean onOkClicked(String str) {
                try {
                    int i = Integer.parseInt(str);
                    if (i == 1) {
                        DebugHelper debugHelper = DebugHelper.this;
                        debugHelper.mDebugLauncherEnabled =
                                debugHelper.mDebugLauncherEnabled ? false : true;
                        DebugHelper debugHelper2 = DebugHelper.this;
                        debugHelper2.setDebugLauncher(context, debugHelper2.mDebugLauncherEnabled);
                    } else if (i == 2) {
                        DebugHelper debugHelper3 = DebugHelper.this;
                        debugHelper3.mDebugTouchServiceEnabled =
                                debugHelper3.mDebugTouchServiceEnabled ? false : true;
                        DebugHelper debugHelper4 = DebugHelper.this;
                        debugHelper4.setDebugTouchService(
                                context, debugHelper4.mDebugTouchServiceEnabled);
                    } else if (i == 3) {
                        DebugHelper debugHelper5 = DebugHelper.this;
                        debugHelper5.mDebugPieEnabled =
                                debugHelper5.mDebugPieEnabled ? false : true;
                        DebugHelper debugHelper6 = DebugHelper.this;
                        debugHelper6.setPieDebug(debugHelper6.mDebugPieEnabled);
                    } else if (i == 4) {
                        DebugHelper debugHelper7 = DebugHelper.this;
                        debugHelper7.mNoRootEnabled = debugHelper7.mNoRootEnabled ? false : true;
                        RootContext.getInstance(context)
                                .setInitialized(DebugHelper.this.mNoRootEnabled);
                    }
                } catch (NumberFormatException unused) {
                }
                return true;
            }
        }.show();
    }

    private void setDebugTouchService(Context context, boolean z) {
        TouchServiceNative.getInstance(context).setDebug(z ? 1 : 0);
    }

    private void setDebugLauncher(Context context, boolean z) {
        if (z) {
            Launcher.setDebug(1);
            AccessibilityHandler.setDebug(1);
            RootContext.getInstance(context).runCommandRemote("debug 1", false);
            RootContext.getInstance(context).setDebug(1);
            return;
        }
        Launcher.setDebug(0);
        AccessibilityHandler.setDebug(0);
        RootContext.getInstance(context).runCommandRemote("debug 0", false);
        RootContext.getInstance(context).setDebug(0);
    }

    private void setPieDebug(boolean z) {
        PieContainer.setDebug(z);
        PieContainer.setDebug(z);
        PieMenu.setDebug(z);
    }
}
