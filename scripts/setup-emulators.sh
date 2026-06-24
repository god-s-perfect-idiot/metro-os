#!/usr/bin/env bash
# Use an existing system AVD for metro-os verify/screenshots (no extra AVDs required).
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
EMULATOR_BIN="$ANDROID_HOME/emulator/emulator"

list_avds() {
  if [[ -x "$EMULATOR_BIN" ]]; then
    "$EMULATOR_BIN" -list-avds 2>/dev/null || true
  elif command -v emulator >/dev/null 2>&1; then
    emulator -list-avds 2>/dev/null || true
  fi
}

AVDS=()
while IFS= read -r line; do
  [[ -n "$line" ]] && AVDS+=("$line")
done < <(list_avds)

echo "==> setup-emulators: use existing system AVD"
echo ""

if [[ ${#AVDS[@]} -eq 0 ]]; then
  echo "No AVDs found. Create one in Android Studio → Device Manager:"
  echo "  API 26+, portrait phone (e.g. Pixel_9)"
  echo ""
  echo "One AVD is enough — avoid creating multiple profiles to save disk space."
  exit 0
fi

echo "Available AVDs:"
for avd in "${AVDS[@]}"; do
  echo "  $avd"
done
echo ""

if [[ -n "${METRO_AVD:-}" ]]; then
  SELECTED="$METRO_AVD"
else
  SELECTED="${AVDS[0]}"
  if [[ ${#AVDS[@]} -gt 1 ]]; then
    echo "Multiple AVDs found; using: $SELECTED"
    echo "Override with: export METRO_AVD=<name>"
    echo ""
  fi
fi

found=0
for avd in "${AVDS[@]}"; do
  if [[ "$avd" == "$SELECTED" ]]; then
    found=1
    break
  fi
done
if [[ $found -eq 0 ]]; then
  echo "ERROR: METRO_AVD=$SELECTED is not in the list above" >&2
  exit 1
fi

echo "Use this AVD for metro-os (one profile keeps disk usage low):"
echo "  export METRO_AVD=$SELECTED"
echo ""
echo "Start emulator:"
echo "  emulator -avd $SELECTED &"
echo "  adb wait-for-device"
echo ""
echo "Screenshot reference size: 768×1280 (lumia-925). Any phone AVD API 26+ is fine for dev."
