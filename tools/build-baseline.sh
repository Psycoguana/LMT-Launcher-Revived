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
# Signing reads keystore/key.properties (git-ignored), like app/build.gradle.
prop() { grep "^$1=" "$ROOT/keystore/key.properties" | cut -d= -f2-; }
apksigner sign --ks "$ROOT/keystore/$(prop storeFile)" \
  --ks-pass "pass:$(prop storePassword)" --ks-key-alias "$(prop keyAlias)" \
  --key-pass "pass:$(prop keyPassword)" \
  --out "$OUT" "$TMP/aligned.apk"
echo "built: $OUT"
