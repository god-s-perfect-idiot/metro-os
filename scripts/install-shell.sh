#!/usr/bin/env bash
# Install Tier 0 Metro Shell APKs on connected device/emulator.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if ! command -v adb >/dev/null 2>&1; then
  echo "ERROR: adb not found" >&2
  exit 1
fi

if ! adb devices 2>/dev/null | grep -q "device$"; then
  echo "ERROR: no device connected" >&2
  exit 1
fi

install_app() {
  local name="$1"
  local apk="$ROOT/apps/$name/deploy/app-debug.apk"
  if [[ -f "$apk" ]]; then
    echo "Installing $name..."
    adb install -r "$apk"
  elif [[ -f "$ROOT/apps/$name/app/build/outputs/apk/debug/app-debug.apk" ]]; then
    echo "Installing $name (from build output)..."
    adb install -r "$ROOT/apps/$name/app/build/outputs/apk/debug/app-debug.apk"
  else
    echo "WARN  $name APK not found — build with: cd apps/$name && ./gradlew :app:assembleDebug"
  fi
}

echo "==> install-shell: Tier 0 Metro Shell"

# Order matters: launcher sets home, overlays depend on it
for app in launcher statusbar navbar; do
  install_app "$app"
done

echo ""
echo "Set default launcher:"
echo "  adb shell cmd package set-home-activity com.metro.launcher/.MainActivity"
echo ""
echo "Grant overlay permission (status bar):"
echo "  adb shell appops set com.metro.statusbar SYSTEM_ALERT_WINDOW allow"
echo ""
echo "install-shell: done"
