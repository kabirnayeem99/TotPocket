#!/usr/bin/env bash
# Builds TotPocket's R8-shrunk release APK and checks it before it goes on a phone.
#
#   tools/build_release.sh             build, verify, copy to dist/
#   tools/build_release.sh --install   …and install on the one phone connected over adb
#
# Steps: rebuild the photo pack only if asked (PACK=1), assembleRelease (R8 + resource shrinking),
# verify the signature with apksigner, check the pack and baseline profile made it into the APK,
# then copy the APK, R8 mapping (for reading crash traces) and a SHA-256 checksum to dist/.
# See docs/install.md for the whole process.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ "${PACK:-0}" == "1" ]]; then
  echo "==> Rebuilding the photo pack"
  PYTHONDONTWRITEBYTECODE=1 python3 tools/build_media_pack.py
fi

if [[ ! -f keystore.properties ]]; then
  echo "!! keystore.properties not found — this APK will be signed with the DEBUG key."
  echo "   Fine for testing; for a phone you'll keep updating, create a release key (docs/install.md)."
fi

echo "==> Building the release APK (R8 + resource shrinking)"
./gradlew --quiet :androidApp:assembleRelease

APK="androidApp/build/outputs/apk/release/androidApp-release.apk"
MAPPING="androidApp/build/outputs/mapping/release/mapping.txt"
[[ -f "$APK" ]] || { echo "!! No APK at $APK"; exit 1; }

SDK="${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' local.properties 2>/dev/null)}"
APKSIGNER="$(ls -d "$SDK"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1 || true)"
if [[ -n "$APKSIGNER" ]]; then
  echo "==> Verifying the signature"
  "$APKSIGNER" verify --print-certs "$APK" | grep -E "certificate (DN|SHA-256)" | head -2
else
  echo "!! apksigner not found under \$ANDROID_HOME/build-tools; skipping signature check"
fi

echo "==> Checking the APK's contents"
CONTENTS="$(unzip -l "$APK")"
grep -q "files/media/pack.zip" <<<"$CONTENTS" || { echo "!! The photo pack is missing from the APK"; exit 1; }
grep -q "assets/dexopt/baseline.prof" <<<"$CONTENTS" || { echo "!! The baseline profile is missing from the APK"; exit 1; }
echo "   photo pack ✓  baseline profile ✓"

VERSION="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' androidApp/build.gradle.kts)"
STAMP="$(date +%Y%m%d-%H%M)"
OUT="dist/totpocket-$VERSION-$STAMP"
mkdir -p dist
cp "$APK" "$OUT.apk"
[[ -f "$MAPPING" ]] && cp "$MAPPING" "$OUT-mapping.txt"
(cd dist && shasum -a 256 "$(basename "$OUT.apk")" > "$(basename "$OUT.apk").sha256")
echo "==> $OUT.apk ($(du -h "$OUT.apk" | cut -f1))"
echo "    keep $OUT-mapping.txt: it turns obfuscated crash traces back into real names"

if [[ "${1:-}" == "--install" ]]; then
  echo "==> Installing on the connected phone"
  adb install -r "$OUT.apk"
fi
