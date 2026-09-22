#!/usr/bin/env bash
# Rebuild the upstream patched smali tree into a signed APK.
# This is the behavioural baseline every reconstruction step is compared against.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
. "$ROOT/tools/env.sh"
OUT="$ROOT/build-out/baseline.apk"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT

apktool b "$ROOT/reference/apktool" -o "$TMP/unsigned.apk" -f >/dev/null
zipalign -p -f 4 "$TMP/unsigned.apk" "$TMP/aligned.apk"
apksigner sign --ks "$ROOT/keystore/lmt-release.jks" \
  --ks-pass pass:***REMOVED*** --ks-key-alias lmt --key-pass pass:***REMOVED*** \
  --out "$OUT" "$TMP/aligned.apk"
echo "built: $OUT"
