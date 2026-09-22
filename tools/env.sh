# Shared toolchain environment for the LMT source reconstruction.
# Source this before any build/verify step:  . tools/env.sh
export JAVA_HOME=/usr/local/opt/openjdk@21
export ANDROID_HOME=/usr/local/share/android-commandlinetools
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export BUILD_TOOLS="$ANDROID_HOME/build-tools/36.0.0"
export PATH="$JAVA_HOME/bin:$BUILD_TOOLS:$ANDROID_HOME/platform-tools:$PATH"

# Test devices are resolved by model, not by serial: the test phone's serial changes
# when it moves between USB and wireless adb.
#   test phone - rooted, Android 15 - primary target
#   flagship - unrooted, SDK 36  - final compatibility gate
lmt_device_by_model() {
  adb devices -l 2>/dev/null | awk -v m="model:$1" '$2=="device" { for(i=3;i<=NF;i++) if($i==m) { print $1; exit } }'
}
export LMT_DEVICE="$(lmt_device_by_model TEST_MODEL)"
export LMT_REF_DEVICE="$(lmt_device_by_model FLAGSHIP_MODEL)"
