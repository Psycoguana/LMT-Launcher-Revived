#!/usr/bin/env bash
# The equivalence oracle.
#
# Builds the Gradle project, disassembles its dex, and compares every
# com.noname81.lmt class against the upstream patched smali that the reference
# APK is built from. Reports any behavioural difference.
#
# Usage: tools/verify-equivalence.sh [--skip-build]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
. "$ROOT/tools/env.sh"

APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
WORK="$ROOT/build-out/verify"

if [[ "${1:-}" != "--skip-build" ]]; then
  echo "==> building"
  (cd "$ROOT" && ./gradlew :app:assembleRelease --no-daemon -q)
fi

rm -rf "$WORK"; mkdir -p "$WORK"
echo "==> disassembling our APK"
apktool d -r -f "$APK" -o "$WORK/ours" >/dev/null

echo "==> resolving resource ids"
python3 "$ROOT/tools/resource_ids.py" "$ROOT/reference/apktool/res/values/public.xml" "$WORK/ref_ids.json"
R_TXT=$(find "$ROOT/app/build/intermediates" -name "R.txt" -path "*release*" | head -1)
python3 "$ROOT/tools/resource_ids.py" "$R_TXT" "$WORK/our_ids.json"

echo "==> fingerprinting"
OURS_SMALI=$(find "$WORK/ours" -type d -path "*/com/noname81/lmt" | head -1)
python3 "$ROOT/tools/smali_fingerprint.py" "$OURS_SMALI" "$WORK/ours.json" "$WORK/our_ids.json" >/dev/null
python3 "$ROOT/tools/smali_fingerprint.py" \
    "$ROOT/reference/apktool/smali/com/noname81/lmt" "$WORK/ref.json" "$WORK/ref_ids.json" >/dev/null

echo "==> comparing"
python3 "$ROOT/tools/compare_fingerprints.py" "$WORK/ref.json" "$WORK/ours.json"
