# Shared toolchain environment for the LMT source reconstruction.
# Source this before any build/verify step:  . tools/env.sh
export JAVA_HOME=/usr/local/opt/openjdk@21
export ANDROID_HOME=/usr/local/share/android-commandlinetools
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export BUILD_TOOLS="$ANDROID_HOME/build-tools/36.0.0"
export PATH="$JAVA_HOME/bin:$BUILD_TOOLS:$ANDROID_HOME/platform-tools:$PATH"

# Primary test device: rooted test phone (Android 15 / SDK 35).
export LMT_DEVICE=<serial>
# Reference device: unrooted Samsung flagship running release v3.11.
export LMT_REF_DEVICE=<phone-ip>:5555
