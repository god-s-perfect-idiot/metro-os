#!/usr/bin/env bash
# Build, install, and launch a metro-os app on the AVD (single dev command).
#
# Usage:
#   ./scripts/run-app.sh <app-name>              # ensure AVD, build, install, launch
#   ./scripts/run-app.sh <app-name> --verify     # also run verify-avd.sh
#   ./scripts/run-app.sh <app-name> --no-build   # skip Gradle (reuse last APK)
#   ./scripts/run-app.sh <app-name> --no-launch  # install only
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=lib/metro-common.sh
source "$ROOT/scripts/lib/metro-common.sh"

APP="${1:-}"
shift || true

DO_BUILD=1
DO_LAUNCH=1
DO_VERIFY=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --verify) DO_VERIFY=1 ;;
    --no-build) DO_BUILD=0 ;;
    --no-launch) DO_LAUNCH=0 ;;
    -h|--help)
      sed -n '2,8p' "$0"
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 2
      ;;
  esac
  shift
done

if [[ -z "$APP" ]]; then
  echo "Usage: $0 <app-name> [--verify] [--no-build] [--no-launch]" >&2
  exit 2
fi

APP_DIR="$(metro_app_dir "$APP")"
if [[ ! -d "$APP_DIR/app" ]]; then
  echo "ERROR: apps/$APP/app/ not found — scaffold first" >&2
  exit 1
fi

PKG="$(metro_app_package "$APP_DIR")"
COMPONENT="$(metro_app_launch_activity "$APP_DIR")"
APK_DIR="$APP_DIR/app/build/outputs/apk/debug"
APK_DEPLOY="$APP_DIR/deploy/app-debug.apk"

# Resolve debug APK path. Suite apps emit app-debug.apk; Mihon/Metron uses ABI splits
# (+ universal). Prefer universal, then device ABI, then any debug apk.
resolve_debug_apk() {
  local dir="$1"
  if [[ -f "$dir/app-debug.apk" ]]; then
    echo "$dir/app-debug.apk"
    return 0
  fi
  if [[ -f "$dir/app-universal-debug.apk" ]]; then
    echo "$dir/app-universal-debug.apk"
    return 0
  fi
  local abi
  abi="$(adb shell getprop ro.product.cpu.abi 2>/dev/null | tr -d '\r' || true)"
  if [[ -n "$abi" && -f "$dir/app-${abi}-debug.apk" ]]; then
    echo "$dir/app-${abi}-debug.apk"
    return 0
  fi
  local found
  found="$(ls -1 "$dir"/app-*-debug.apk "$dir"/app-debug.apk 2>/dev/null | head -1 || true)"
  if [[ -n "$found" && -f "$found" ]]; then
    echo "$found"
    return 0
  fi
  return 1
}

echo "==> run-app: $APP ($PKG)"
metro_ensure_avd
metro_ensure_user_unlocked

if [[ "$DO_BUILD" -eq 1 ]]; then
  echo "==> build"
  # AGP 9 apps (keyboard, metron) need toolkits in mavenLocal.
  if [[ "$APP" == "keyboard" || "$APP" == "metron" ]]; then
    echo "==> publish toolkits → mavenLocal (required by $APP)"
    (cd "$ROOT/toolkits/metro-system-sdk" && ./gradlew publishToMavenLocal --quiet)
    (cd "$ROOT/toolkits/metro-ui-android" && ./gradlew publishToMavenLocal --quiet)
  fi
  if [[ "$APP" == "metron" ]]; then
    (cd "$APP_DIR" && ./gradlew :app:assembleDebug \
      -Pdist=foss -Pinclude-telemetry=false -Penable-updater=false --quiet)
  else
    (cd "$APP_DIR" && ./gradlew :app:assembleDebug --quiet)
  fi
fi

APK_BUILD=""
if APK_BUILD="$(resolve_debug_apk "$APK_DIR")"; then
  :
else
  echo "ERROR: APK missing under $APK_DIR — build failed or --no-build without prior build" >&2
  exit 1
fi

mkdir -p "$APP_DIR/deploy"
cp -f "$APK_BUILD" "$APK_DEPLOY"
echo "OK  apk: $(basename "$APK_BUILD")"

echo "==> install"
adb install -r "$APK_DEPLOY"

# Notifications replaces AOSP heads-up; grant overlay + secure-settings when installing that app.
if [[ "$APP" == "notifications" ]]; then
  adb shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1 \
    && echo "OK  overlay: notifications" \
    || echo "WARN  overlay grant failed: notifications"
  adb shell pm grant "$PKG" android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1 \
    && echo "OK  WRITE_SECURE_SETTINGS: notifications" \
    || echo "WARN  WRITE_SECURE_SETTINGS grant failed"
fi

# Widgets Battery Saver tile toggles Settings.Global low_power.
if [[ "$APP" == "widgets" ]]; then
  adb shell pm grant "$PKG" android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1 \
    && echo "OK  WRITE_SECURE_SETTINGS: widgets (battery saver)" \
    || echo "WARN  WRITE_SECURE_SETTINGS grant failed: widgets"
fi

if [[ "$DO_LAUNCH" -eq 1 ]]; then
  echo "==> launch $COMPONENT"
  adb shell am force-stop "$PKG" >/dev/null 2>&1 || true
  adb shell am start -n "$COMPONENT" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
  sleep 2
  echo "OK  launched on $(metro_adb_devices | head -1)"
fi

if [[ "$DO_VERIFY" -eq 1 ]]; then
  "$ROOT/scripts/verify-avd.sh" "$APP"
fi

echo "run-app: done ($APP)"
echo "  APK: $APK_DEPLOY"
echo "  Tip: attach a screenshot of the emulator to your agent for visual review"
