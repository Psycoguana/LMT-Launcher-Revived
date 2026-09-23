#!/usr/bin/env bash
# Build the injection probe and run it on the test device under root.
#
# Answers: which route can inject input events from app_process on this Android
# version? Compiles against the PUBLIC SDK only (everything hidden goes through
# reflection), so it needs no framework stub jar.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
. "$HERE/../../env.sh"

rm -rf "$HERE/out"; mkdir -p "$HERE/out/classes" "$HERE/out/jar"
javac -source 8 -target 8 \
      -bootclasspath "$ANDROID_HOME/platforms/android-36/android.jar" \
      -d "$HERE/out/classes" "$HERE/src/probe/Probe.java" 2>/dev/null
"$BUILD_TOOLS/d8" --min-api 21 --output "$HERE/out" $(find "$HERE/out/classes" -name '*.class')
cp "$HERE/out/classes.dex" "$HERE/out/jar/"
(cd "$HERE/out/jar" && zip -q ../probe.jar classes.dex)

adb -s "$LMT_DEVICE" push "$HERE/out/probe.jar" /data/local/tmp/probe.jar >/dev/null
echo "volume before: $(adb -s "$LMT_DEVICE" shell "cmd media_session volume --stream 3 --get" | tr -d '\r' | grep -oE '[0-9]+$')"
adb -s "$LMT_DEVICE" shell "su -c 'CLASSPATH=/data/local/tmp/probe.jar app_process /data/local/tmp probe.Probe'"
sleep 1
echo "volume after : $(adb -s "$LMT_DEVICE" shell "cmd media_session volume --stream 3 --get" | tr -d '\r' | grep -oE '[0-9]+$')"
echo "(a route that really injected moves the volume; 'OK' alone only means it did not throw)"
