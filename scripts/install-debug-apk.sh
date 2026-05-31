#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="$ROOT_DIR/build/soundbar-keepalive-debug.apk"

if [[ ! -f "$APK" ]]; then
  "$ROOT_DIR/scripts/build-debug-apk.sh" >/dev/null
fi

if [[ "${1:-}" != "" ]]; then
  adb -s "$1" install -r "$APK"
else
  adb install -r "$APK"
fi

