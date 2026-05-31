#!/usr/bin/env bash
set -euo pipefail

DEVICE="${1:-}"
ADB=(adb)
if [[ -n "$DEVICE" ]]; then
  ADB=(adb -s "$DEVICE")
fi

PACKAGE="io.github.damyandeshev.soundbarkeepalive"

"${ADB[@]}" shell am startservice \
  -n "$PACKAGE/.KeepAliveService" \
  -a "$PACKAGE.STOP"

