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
for app in launcher statusbar notifications navbar volume lockscreen; do
  install_app "$app"
done

echo ""
echo "==> shell grants"
adb shell cmd package set-home-activity com.metro.launcher/.MainActivity >/dev/null 2>&1 \
  && echo "OK  home: com.metro.launcher" \
  || echo "WARN  set-home-activity failed"

adb shell appops set com.metro.statusbar SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1 \
  && echo "OK  overlay: statusbar" \
  || echo "WARN  overlay grant failed: statusbar"
adb shell appops set com.metro.volume SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1 \
  && echo "OK  overlay: volume" \
  || echo "WARN  overlay grant failed: volume"
adb shell appops set com.metro.notifications SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1 \
  && echo "OK  overlay: notifications" \
  || echo "WARN  overlay grant failed: notifications"
adb shell pm grant com.metro.notifications android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1 \
  && echo "OK  WRITE_SECURE_SETTINGS: notifications (heads-up suppress)" \
  || echo "WARN  WRITE_SECURE_SETTINGS grant failed (device may require adb root / manual grant)"

echo ""
echo "Enable Volume accessibility (volume keys + overlay layer) from the Volume app setup screen."
echo "Enable Notifications accessibility + notification access from the Notifications app setup screen."
echo "Enable Lock screen accessibility + Show lock screen toggle from the Lock screen app setup."
echo "With Show notifications on, Metro sets heads_up_notifications_enabled=0 so stock peeks do not compete."
echo ""
echo "install-shell: done"
