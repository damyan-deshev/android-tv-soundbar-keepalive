#!/usr/bin/env bash
set -euo pipefail

DEVICE="${1:-}"
ADB=(adb)
if [[ -n "$DEVICE" ]]; then
  ADB=(adb -s "$DEVICE")
fi

PACKAGE="io.github.damyandeshev.soundbarkeepalive"

"${ADB[@]}" shell am start-foreground-service \
  -n "$PACKAGE/.KeepAliveService" \
  -a "$PACKAGE.START" \
  --ei frequency_hz "${FREQUENCY_HZ:-25000}" \
  --ei sample_rate "${SAMPLE_RATE:-96000}" \
  --ei amplitude "${AMPLITUDE:-900}" \
  --ei duration_ms "${DURATION_MS:-6000}" \
  --ei interval_sec "${INTERVAL_SEC:-120}"
