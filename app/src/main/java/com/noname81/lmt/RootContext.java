package com.noname81.lmt;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class RootContext {
    private static boolean DEBUG = false;
    private static final String PREFIX = "";
    private static final String TAG = "LMT::RootContext";
    private static RootContext instance = null;
    private static byte[] returnBuffer = new byte[512];
    private static int returnBufferSize = 512;
    private String mAbsolutePath;
    private Context mContext;
    private boolean mInitialized;
    private DataInputStream mRemoteInputStream;
    private DataOutputStream mRemoteOutputStream;
    private Process mRemoteProcess;
    private DataInputStream mRootInputStream;
    private DataOutputStream mRootOutputStream;
    private Process mRootProcess;
    private String mShell;

    private RootContext(Context context) {
        this.mInitialized = false;
        try {
            this.mContext = context;
            this.mShell = "su";
            this.mAbsolutePath = context.getFilesDir().getAbsolutePath();
            initRemote();
            initRoot();
            this.mInitialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.mInitialized) {
            return;
        }
        Toast.makeText(context, "Failed to get root permissions!", 0).show();
        Log.e(TAG, "Failed to get root permissions!");
    }

    public static RootContext getInstance(Context context) {
        if (instance == null) {
            if (DEBUG) {
                Log.d(TAG, "getInstance()");
            }
            instance = new RootContext(context);
        }
        return instance;
    }

    boolean isRootAvailable(boolean log) {
        if (log && !this.mInitialized) {
            Toast.makeText(this.mContext, "This feature needs root permissions!", 0).show();
            Log.e(TAG, "This feature needs root permissions!");
        }
        return this.mInitialized;
    }

    void setDebug(int mode) {
        DEBUG = mode == 1;
    }

    void setInitialized(boolean initialized) {
        this.mInitialized = initialized;
    }

    private void initRemote() throws Exception {
        if (DEBUG) {
            Log.d(TAG, "initRemote()");
        }
        initRemoteStreams();
        if (Build.VERSION.SDK_INT >= 23) {
            initRemoteProcess23();
        } else if (Build.VERSION.SDK_INT >= 20) {
            initRemoteProcess21();
        } else {
            initRemoteProcess20();
        }
    }

    private void copyRemote() throws Exception {
        if (DEBUG) {
            Log.d(TAG, "copyRemote()");
        }
        // One helper now covers every API level. The five prebuilt
        // InputContext<sdk>.jar variants existed only because hidden-API signatures
        // changed between releases; the helper is built from source (see
        // :inputcontext) and reaches those APIs reflectively, which defers signature
        // resolution to runtime.
        InputStream inputStreamOpen = this.mContext.getAssets().open("InputContext.jar");
        FileOutputStream fileOutputStream =
                new FileOutputStream(new File(this.mAbsolutePath + "/InputContext.jar"));
        byte[] bArr = new byte[4096];
        while (true) {
            int read = inputStreamOpen.read(bArr);
            if (read != -1) {
                fileOutputStream.write(bArr, 0, read);
            } else {
                fileOutputStream.close();
                inputStreamOpen.close();
                return;
            }
        }
    }

    private void initRemoteStreams() throws Exception {
        if (DEBUG) {
            Log.d(TAG, "initRemoteStreams()");
        }
        try {
            DataOutputStream dataOutputStream = this.mRemoteOutputStream;
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
            DataInputStream dataInputStream = this.mRemoteInputStream;
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            Process process = this.mRemoteProcess;
            if (process != null) {
                process.destroy();
            }
        } catch (Exception unused) {
            this.mRemoteOutputStream = null;
            this.mRemoteInputStream = null;
            this.mRemoteProcess = null;
        }
        copyRemote();
        this.mRemoteProcess = new ProcessBuilder(this.mShell).redirectErrorStream(true).start();
        this.mRemoteOutputStream = new DataOutputStream(this.mRemoteProcess.getOutputStream());
        this.mRemoteInputStream = new DataInputStream(this.mRemoteProcess.getInputStream());
    }

    private void initRemoteProcess20() throws Exception {
        if (DEBUG) {
            Log.d(TAG, "initRemoteProcess20()");
        }
        if (Build.VERSION.SDK_INT >= 19) {
            this.mRemoteOutputStream.write("sh\n".getBytes("ASCII"));
        }
        this.mRemoteOutputStream.write(
                ("export CLASSPATH=" + this.mAbsolutePath + "/InputContext.jar\n")
                        .getBytes("ASCII"));
        this.mRemoteOutputStream.write(
                ("exec app_process "
                                + this.mAbsolutePath
                                + " com.noname81.lmt.InputContext \"$@\"\n")
                        .getBytes("ASCII"));
    }

    private void initRemoteProcess21() throws Exception {
        if (DEBUG) {
            Log.d(TAG, "initRemoteProcess21()");
        }
        this.mRemoteOutputStream.write(
                ("export CLASSPATH=" + this.mAbsolutePath + "/InputContext.jar\n")
                        .getBytes("ASCII"));
        this.mRemoteOutputStream.write(
                ("exec app_process " + this.mAbsolutePath + " com.noname81.lmt.InputContext\n")
                        .getBytes("ASCII"));
        this.mRemoteOutputStream.flush();
    }

    private void initRemoteProcess23() throws Exception {
        if (DEBUG) {
            Log.d(TAG, "initRemoteProcess23()");
        }
        this.mRemoteOutputStream.write(
                "supolicy --live 'allow qti_init_shell zygote_exec file execute'\n"
                        .getBytes("ASCII"));
        this.mRemoteOutputStream.write(
                ("export CLASSPATH=" + this.mAbsolutePath + "/InputContext.jar\n")
                        .getBytes("ASCII"));
        this.mRemoteOutputStream.write(
                ("exec app_process " + this.mAbsolutePath + " com.noname81.lmt.InputContext\n")
                        .getBytes("ASCII"));
        this.mRemoteOutputStream.flush();
    }

    private void initRoot() throws Exception {
        if (DEBUG) {
            Log.d(TAG, "initRoot()");
        }
        DataOutputStream dataOutputStream = this.mRootOutputStream;
        if (dataOutputStream != null) {
            dataOutputStream.close();
        }
        DataInputStream dataInputStream = this.mRootInputStream;
        if (dataInputStream != null) {
            dataInputStream.close();
        }
        Process process = this.mRootProcess;
        if (process != null) {
            process.destroy();
        }
        this.mRootProcess = new ProcessBuilder(this.mShell).redirectErrorStream(true).start();
        this.mRootOutputStream = new DataOutputStream(this.mRootProcess.getOutputStream());
        this.mRootInputStream = new DataInputStream(this.mRootProcess.getInputStream());
    }

    boolean runCommandRemote(String command, boolean log) {
        if (DEBUG) {
            Log.d(TAG, "runCommandRemote(" + command + ")");
        }
        if (!isRootAvailable(log) || this.mRemoteOutputStream == null) {
            return false;
        }
        if (getReturnString(this.mRemoteInputStream).contains("quit")) {
            try {
                initRemote();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            try {
                this.mRemoteOutputStream.write((command + "\n").getBytes("ASCII"));
                this.mRemoteOutputStream.flush();
                return true;
            } catch (Exception e2) {
                e2.printStackTrace();
                Log.e(TAG, "Failed to write remote command!");
                return true;
            }
        } catch (Exception unused) {
            try {
                initRemote();
            } catch (Exception ignored) {
            }
            return true;
        }
    }

    String runCommandRemoteResult(String command, boolean log) {
        if (!runCommandRemote(command, log)) {
            return null;
        }
        try {
            String line = this.mRemoteInputStream.readLine();
            if (DEBUG) {
                Log.d(TAG, "Command returned: " + line);
            }
            return line;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to read  from remote!");
            return null;
        }
    }

    private String getReturnString(DataInputStream dataInputStream) {
        StringBuilder sb = new StringBuilder();
        try {
            int available = dataInputStream.available();
            while (available > 0) {
                int read =
                        dataInputStream.read(
                                returnBuffer, 0, Math.min(available, returnBufferSize));
                if (read == -1) {
                    break;
                }
                sb.append(new String(returnBuffer, 0, read));
                available -= read;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String string = sb.toString();
        if (DEBUG) {
            Log.d(TAG, "Command returned: " + string);
        }
        return string;
    }

    void runCommandRoot(String command, boolean log) {
        DataOutputStream dataOutputStream;
        if (DEBUG) {
            Log.d(TAG, "runCommandRoot(" + command + ")");
        }
        if (isRootAvailable(log) && (dataOutputStream = this.mRootOutputStream) != null) {
            try {
                try {
                    dataOutputStream.write((PREFIX + command + "\n").getBytes("ASCII"));
                    this.mRootOutputStream.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to write root command!");
                }
            } catch (Exception unused) {
                try {
                    initRoot();
                } catch (Exception ignored) {
                }
            }
        }
    }

    String runCommandResult(String command, boolean log) {
        if (DEBUG) {
            Log.d(TAG, "runCommandResult(" + command + ", " + log + ")");
        }
        try {
            String[] strArr = new String[1];
            strArr[0] = log ? this.mShell : "sh";
            Process processStart = new ProcessBuilder(strArr).redirectErrorStream(true).start();
            DataOutputStream dataOutputStream =
                    new DataOutputStream(processStart.getOutputStream());
            DataInputStream dataInputStream = new DataInputStream(processStart.getInputStream());
            dataOutputStream.write((command + "\n").getBytes("ASCII"));
            dataOutputStream.close();
            processStart.waitFor();
            String returnString = getReturnString(dataInputStream);
            if (DEBUG) {
                Log.d(TAG, "Command returned: " + returnString);
            }
            dataInputStream.close();
            processStart.destroy();
            return returnString;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to write command!");
            return null;
        }
    }
}
