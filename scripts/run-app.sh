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
APK_BUILD="$APP_DIR/app/build/outputs/apk/debug/app-debug.apk"
APK_DEPLOY="$APP_DIR/deploy/app-debug.apk"

echo "==> run-app: $APP ($PKG)"
metro_ensure_avd
metro_ensure_user_unlocked

if [[ "$DO_BUILD" -eq 1 ]]; then
  echo "==> build"
  (cd "$APP_DIR" && ./gradlew :app:assembleDebug --quiet)
fi

if [[ ! -f "$APK_BUILD" ]]; then
  echo "ERROR: APK missing at $APK_BUILD — build failed or --no-build without prior build" >&2
  exit 1
fi

mkdir -p "$APP_DIR/deploy"
cp -f "$APK_BUILD" "$APK_DEPLOY"

echo "==> install"
adb install -r "$APK_DEPLOY"

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
