package com.noname81.lmt;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * The Flashlight action (ROADMAP 2.6): turns the torch of the back camera on or off. Needs no
 * permission; the torch state is followed with a callback, so a flashlight turned on from quick
 * settings is turned off by the next toggle.
 */
final class Torch {
    private static final String TAG = "LMT::Torch";
    private static String sCameraId;
    private static boolean sOn;
    private static boolean sWatching;

    private Torch() {}

    static void toggle(Context context) {
        CameraManager cameras = context.getSystemService(CameraManager.class);
        try {
            String id = cameraId(cameras);
            if (id == null) {
                Toast.makeText(context, "This device has no flashlight", Toast.LENGTH_SHORT).show();
                return;
            }
            watch(cameras);
            cameras.setTorchMode(id, !sOn);
        } catch (Exception e) {
            // The camera is in use by an app, or the torch is unavailable.
            Log.w(TAG, "Torch toggle failed", e);
            Toast.makeText(context, "The flashlight is in use", Toast.LENGTH_SHORT).show();
        }
    }

    /** Starts following the torch state; called early too, so the first toggle knows it. */
    static void watch(Context context) {
        try {
            watch(context.getSystemService(CameraManager.class));
        } catch (RuntimeException e) {
            Log.w(TAG, "No torch callback", e);
        }
    }

    private static void watch(CameraManager cameras) {
        if (sWatching) {
            return;
        }
        sWatching = true;
        cameras.registerTorchCallback(
                new CameraManager.TorchCallback() {
                    @Override
                    public void onTorchModeChanged(String cameraId, boolean enabled) {
                        if (cameraId.equals(sCameraId)) {
                            sOn = enabled;
                        }
                    }
                },
                new Handler(Looper.getMainLooper()));
    }

    /** The first back camera with a flash unit, or null. */
    private static String cameraId(CameraManager cameras) throws Exception {
        if (sCameraId == null) {
            for (String id : cameras.getCameraIdList()) {
                CameraCharacteristics info = cameras.getCameraCharacteristics(id);
                Boolean flash = info.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = info.get(CameraCharacteristics.LENS_FACING);
                if (Boolean.TRUE.equals(flash)
                        && (facing == null || facing == CameraCharacteristics.LENS_FACING_BACK)) {
                    sCameraId = id;
                    break;
                }
            }
        }
        return sCameraId;
    }
}
