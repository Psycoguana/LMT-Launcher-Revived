# Shared toolchain environment for the LMT source reconstruction.
# Source this before any build/verify step:  . tools/env.sh
# Set LMT_JAVA_HOME (a JDK 21) and LMT_ANDROID_HOME for your machine; the defaults are the
# macOS Homebrew paths. A JAVA_HOME already in the shell is not used: it is often another JDK.
export JAVA_HOME="${LMT_JAVA_HOME:-/usr/local/opt/openjdk@21}"
export ANDROID_HOME="${LMT_ANDROID_HOME:-/usr/local/share/android-commandlinetools}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export BUILD_TOOLS="$ANDROID_HOME/build-tools/36.0.0"
export PATH="$JAVA_HOME/bin:$BUILD_TOOLS:$ANDROID_HOME/platform-tools:$PATH"

# Target devices for the scripts in tools/: set LMT_DEVICE (rooted, primary) and
# LMT_REF_DEVICE (a second phone) to adb serials before sourcing, or leave them unset
# to use the first connected device.
export LMT_DEVICE="${LMT_DEVICE:-$(adb devices 2>/dev/null | awk 'NR>1 && $2=="device" { print $1; exit }')}"
export LMT_REF_DEVICE="${LMT_REF_DEVICE:-}"
