#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"

if [[ -z "$SDK_DIR" ]]; then
  echo "Set ANDROID_HOME or ANDROID_SDK_ROOT to an Android SDK directory." >&2
  exit 1
fi

if [[ ! -d "$SDK_DIR/platforms" || ! -d "$SDK_DIR/build-tools" ]]; then
  echo "Android SDK is missing platforms/ or build-tools/: $SDK_DIR" >&2
  exit 1
fi

PLATFORM_DIR="${ANDROID_PLATFORM_DIR:-}"
if [[ -z "$PLATFORM_DIR" ]]; then
  PLATFORM_DIR="$(find "$SDK_DIR/platforms" -mindepth 1 -maxdepth 1 -type d | sort | tail -1)"
fi

BUILD_TOOLS_DIR="${ANDROID_BUILD_TOOLS_DIR:-}"
if [[ -z "$BUILD_TOOLS_DIR" ]]; then
  BUILD_TOOLS_DIR="$(find "$SDK_DIR/build-tools" -mindepth 1 -maxdepth 1 -type d | sort | tail -1)"
fi

ANDROID_JAR="$PLATFORM_DIR/android.jar"
if [[ ! -f "$ANDROID_JAR" ]]; then
  echo "Missing android.jar under $PLATFORM_DIR" >&2
  exit 1
fi

BUILD_DIR="$ROOT_DIR/build"
APP_DIR="$ROOT_DIR/app"
KEYSTORE="$BUILD_DIR/debug.keystore"

rm -rf "$BUILD_DIR/compiled" "$BUILD_DIR/generated" "$BUILD_DIR/classes" "$BUILD_DIR/dex"
mkdir -p "$BUILD_DIR/compiled" "$BUILD_DIR/generated" "$BUILD_DIR/classes" "$BUILD_DIR/dex"

"$BUILD_TOOLS_DIR/aapt2" compile --dir "$APP_DIR/src/main/res" -o "$BUILD_DIR/compiled/res.zip"
"$BUILD_TOOLS_DIR/aapt2" link \
  -o "$BUILD_DIR/resources.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$APP_DIR/src/main/AndroidManifest.xml" \
  --java "$BUILD_DIR/generated" \
  --min-sdk-version 23 \
  --target-sdk-version 31 \
  "$BUILD_DIR/compiled/res.zip"

find "$APP_DIR/src/main/java" "$BUILD_DIR/generated" -name '*.java' > "$BUILD_DIR/sources.list"
javac -source 8 -target 8 -classpath "$ANDROID_JAR" -d "$BUILD_DIR/classes" @"$BUILD_DIR/sources.list"

find "$BUILD_DIR/classes" -name '*.class' > "$BUILD_DIR/classes.list"
"$BUILD_TOOLS_DIR/d8" --lib "$ANDROID_JAR" --min-api 23 --output "$BUILD_DIR/dex" @"$BUILD_DIR/classes.list"

cp "$BUILD_DIR/resources.apk" "$BUILD_DIR/unsigned.apk"
(cd "$BUILD_DIR/dex" && zip -q -r "$BUILD_DIR/unsigned.apk" classes.dex)

"$BUILD_TOOLS_DIR/zipalign" -f -p 4 "$BUILD_DIR/unsigned.apk" "$BUILD_DIR/aligned-unsigned.apk"

if [[ ! -f "$KEYSTORE" ]]; then
  keytool -genkeypair \
    -keystore "$KEYSTORE" \
    -storepass android \
    -alias androiddebugkey \
    -keypass android \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Android Debug,O=Soundbar Keepalive,C=US" >/dev/null
fi

"$BUILD_TOOLS_DIR/apksigner" sign \
  --ks "$KEYSTORE" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$BUILD_DIR/soundbar-keepalive-debug.apk" \
  "$BUILD_DIR/aligned-unsigned.apk"

"$BUILD_TOOLS_DIR/apksigner" verify "$BUILD_DIR/soundbar-keepalive-debug.apk"
echo "$BUILD_DIR/soundbar-keepalive-debug.apk"

