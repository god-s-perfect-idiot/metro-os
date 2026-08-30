#!/usr/bin/env bash
# Build and install every complete metro-os suite app onto a connected adb device.
#
# "Complete" means the Android project is present and buildable:
#   apps/<name>/app/ exists and apps/<name>/gradlew is executable.
# Docs-only apps (no Gradle project) are skipped.
#
# Usage:
#   ./scripts/deploy-suite.sh                    # build + install all complete apps
#   ./scripts/deploy-suite.sh launcher photos    # named apps only
#   ./scripts/deploy-suite.sh --list             # print complete apps, no build/install
#   ./scripts/deploy-suite.sh --no-build         # install existing APKs only
#   ./scripts/deploy-suite.sh --release          # assembleRelease + install release APKs
#   ./scripts/deploy-suite.sh --configure-shell  # set home + overlay grants after install
#   ./scripts/deploy-suite.sh --continue-on-error
#   ./scripts/deploy-suite.sh -s SERIAL          # target a specific device
#
# Outputs:
#   apps/<name>/deploy/app-debug.apk (via build-apks.sh)
#   deploy/apks/<name>-debug.apk
#   deploy/deploy-suite-report.json
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=lib/metro-common.sh
source "$ROOT/scripts/lib/metro-common.sh"

# Same tier order as build-apks.sh / verify-all.sh
APP_ORDER=(
  launcher statusbar notifications navbar volume lockscreen
  browser notes music
  photos calendar mail messaging people dialer store settings calculator clock files discord
)

SHELL_APPS=(launcher statusbar notifications navbar volume lockscreen)

DO_BUILD=1
BUILD_VARIANT="debug"
LIST_ONLY=0
CONTINUE_ON_ERROR=0
CONFIGURE_SHELL=0
SERIAL=""
REQUESTED=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --list)
      LIST_ONLY=1
      ;;
    --no-build)
      DO_BUILD=0
      ;;
    --release)
      BUILD_VARIANT="release"
      ;;
    --configure-shell)
      CONFIGURE_SHELL=1
      ;;
    --continue-on-error)
      CONTINUE_ON_ERROR=1
      ;;
    -s|--serial)
      shift
      if [[ $# -eq 0 || "$1" == -* ]]; then
        echo "ERROR: -s/--serial requires a device serial" >&2
        exit 2
      fi
      SERIAL="$1"
      ;;
    -h|--help)
      sed -n '2,21p' "$0"
      exit 0
      ;;
    -*)
      echo "Unknown option: $1" >&2
      echo "Usage: $0 [--list] [--no-build] [--release] [--configure-shell] [--continue-on-error] [-s SERIAL] [app-name...]" >&2
      exit 2
      ;;
    *)
      REQUESTED+=("$1")
      ;;
  esac
  shift
done

is_complete_app() {
  local name="$1"
  local dir="$ROOT/apps/$name"
  [[ -d "$dir/app" && -x "$dir/gradlew" ]]
}

list_complete_apps() {
  local name
  if [[ ${#REQUESTED[@]} -gt 0 ]]; then
    for name in "${REQUESTED[@]}"; do
      if ! is_complete_app "$name"; then
        echo "ERROR: apps/$name is not a complete (buildable) app" >&2
        echo "       Need apps/$name/app/ and apps/$name/gradlew" >&2
        return 1
      fi
      echo "$name"
    done
    return 0
  fi

  for name in "${APP_ORDER[@]}"; do
    if is_complete_app "$name"; then
      echo "$name"
    fi
  done

  local dir
  for dir in "$ROOT"/apps/*/; do
    name="$(basename "$dir")"
    [[ "$name" == "_template" ]] && continue
    local known=0
    local ordered
    for ordered in "${APP_ORDER[@]}"; do
      if [[ "$ordered" == "$name" ]]; then
        known=1
        break
      fi
    done
    if [[ "$known" -eq 0 ]] && is_complete_app "$name"; then
      echo "$name"
    fi
  done
}

apk_candidates() {
  local name="$1"
  local app_dir="$ROOT/apps/$name"
  if [[ "$BUILD_VARIANT" == "debug" ]]; then
    echo "$app_dir/deploy/app-debug.apk"
    echo "$ROOT/deploy/apks/${name}-debug.apk"
    echo "$app_dir/app/build/outputs/apk/debug/app-debug.apk"
  else
    echo "$app_dir/deploy/app-release.apk"
    echo "$ROOT/deploy/apks/${name}-release.apk"
    echo "$app_dir/app/build/outputs/apk/release/app-release.apk"
    echo "$app_dir/app/build/outputs/apk/release/app-release-unsigned.apk"
  fi
}

resolve_apk() {
  local name="$1"
  local candidate
  while IFS= read -r candidate; do
    if [[ -f "$candidate" ]]; then
      echo "$candidate"
      return 0
    fi
  done < <(apk_candidates "$name")
  return 1
}

adb_cmd() {
  if [[ -n "$SERIAL" ]]; then
    adb -s "$SERIAL" "$@"
  else
    adb "$@"
  fi
}

configure_shell() {
  echo ""
  echo "==> configure-shell"
  adb_cmd shell cmd package set-home-activity com.metro.launcher/.MainActivity >/dev/null 2>&1 \
    && echo "OK  default launcher → com.metro.launcher" \
    || echo "WARN  could not set default launcher (may need manual picker)"

  adb_cmd shell appops set com.metro.statusbar SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1 \
    && echo "OK  overlay: statusbar" \
    || echo "WARN  overlay grant failed: statusbar"

  adb_cmd shell appops set com.metro.volume SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1 \
    && echo "OK  overlay: volume" \
    || echo "WARN  overlay grant failed: volume"

  adb_cmd shell appops set com.metro.notifications SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1 \
    && echo "OK  overlay: notifications" \
    || echo "WARN  overlay grant failed: notifications"

  adb_cmd shell pm grant com.metro.notifications android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1 \
    && echo "OK  WRITE_SECURE_SETTINGS: notifications" \
    || echo "WARN  WRITE_SECURE_SETTINGS grant failed (device may require adb root / manual grant)"

  echo "Note: enable Volume + Notifications accessibility / notification access from each app's setup screen."
}

APPS=()
while IFS= read -r _app; do
  [[ -n "$_app" ]] && APPS+=("$_app")
done < <(list_complete_apps)

if [[ ${#APPS[@]} -eq 0 ]]; then
  echo "ERROR: no complete apps found under apps/" >&2
  exit 1
fi

echo "==> deploy-suite: ${#APPS[@]} complete app(s) ($BUILD_VARIANT)"
for app in "${APPS[@]}"; do
  echo "  - $app"
done

if [[ "$LIST_ONLY" -eq 1 ]]; then
  exit 0
fi

metro_path_android_tools

if ! command -v adb >/dev/null 2>&1; then
  echo "ERROR: adb not found — install platform-tools or set ANDROID_HOME" >&2
  exit 1
fi

DEVICES=()
while IFS= read -r _dev; do
  [[ -n "$_dev" ]] && DEVICES+=("$_dev")
done < <(metro_adb_devices)

if [[ ${#DEVICES[@]} -eq 0 ]]; then
  echo "ERROR: no adb device connected (adb devices must show 'device')" >&2
  echo "       Connect a phone/emulator, then re-run." >&2
  exit 1
fi

if [[ -n "$SERIAL" ]]; then
  local_found=0
  for d in "${DEVICES[@]}"; do
    if [[ "$d" == "$SERIAL" ]]; then
      local_found=1
      break
    fi
  done
  if [[ "$local_found" -eq 0 ]]; then
    echo "ERROR: serial '$SERIAL' not among connected devices: ${DEVICES[*]}" >&2
    exit 1
  fi
elif [[ ${#DEVICES[@]} -gt 1 ]]; then
  echo "ERROR: multiple devices connected: ${DEVICES[*]}" >&2
  echo "       Pass -s SERIAL (or set ANDROID_SERIAL) to pick one." >&2
  exit 1
else
  SERIAL="${DEVICES[0]}"
fi

echo "OK  target device: $SERIAL"
export ANDROID_SERIAL="$SERIAL"

adb_cmd wait-for-device
metro_ensure_user_unlocked

if [[ "$DO_BUILD" -eq 1 ]]; then
  echo ""
  echo "==> build"
  build_args=()
  [[ "$BUILD_VARIANT" == "release" ]] && build_args+=(--release)
  [[ "$CONTINUE_ON_ERROR" -eq 1 ]] && build_args+=(--continue-on-error)
  if [[ ${#REQUESTED[@]} -gt 0 ]]; then
    build_args+=("${REQUESTED[@]}")
  fi
  if ! "$ROOT/scripts/build-apks.sh" "${build_args[@]+"${build_args[@]}"}"; then
    if [[ "$CONTINUE_ON_ERROR" -eq 0 ]]; then
      echo "ERROR: build-apks failed — aborting deploy" >&2
      exit 1
    fi
    echo "WARN  build-apks reported failures — installing whatever APKs exist"
  fi
fi

INSTALLED=()
FAILED=()
SKIPPED=()
RESULTS="[]"

echo ""
echo "==> install (${#APPS[@]} apps)"

for app in "${APPS[@]}"; do
  echo ""
  echo "--- $app"
  apk=""
  if apk="$(resolve_apk "$app")"; then
    echo "APK  $apk"
    if adb_cmd install -r "$apk"; then
      echo "OK  installed $app"
      INSTALLED+=("$app")
      RESULTS=$(echo "$RESULTS" | python3 -c "import json,sys; a=json.load(sys.stdin); a.append({'name':'$app','passed':True,'apk':'$apk'}); print(json.dumps(a))")
    else
      echo "ERROR: adb install failed for $app" >&2
      FAILED+=("$app")
      RESULTS=$(echo "$RESULTS" | python3 -c "import json,sys; a=json.load(sys.stdin); a.append({'name':'$app','passed':False,'error':'install_failed','apk':'$apk'}); print(json.dumps(a))")
      if [[ "$CONTINUE_ON_ERROR" -eq 0 ]]; then
        exit 1
      fi
    fi
  else
    echo "WARN  APK not found for $app — skip (build with: ./scripts/build-apks.sh $app)" >&2
    SKIPPED+=("$app")
    FAILED+=("$app")
    RESULTS=$(echo "$RESULTS" | python3 -c "import json,sys; a=json.load(sys.stdin); a.append({'name':'$app','passed':False,'error':'apk_missing'}); print(json.dumps(a))")
    if [[ "$CONTINUE_ON_ERROR" -eq 0 ]]; then
      exit 1
    fi
  fi
done

# Offer shell tips when any shell app was in the deploy set
deployed_shell=0
for app in "${APPS[@]}"; do
  for shell in "${SHELL_APPS[@]}"; do
    if [[ "$app" == "$shell" ]]; then
      deployed_shell=1
      break 2
    fi
  done
done

if [[ "$CONFIGURE_SHELL" -eq 1 ]]; then
  configure_shell
elif [[ "$deployed_shell" -eq 1 ]]; then
  echo ""
  echo "Shell tips (or re-run with --configure-shell):"
  echo "  adb -s $SERIAL shell cmd package set-home-activity com.metro.launcher/.MainActivity"
  echo "  adb -s $SERIAL shell appops set com.metro.statusbar SYSTEM_ALERT_WINDOW allow"
  echo "  adb -s $SERIAL shell appops set com.metro.volume SYSTEM_ALERT_WINDOW allow"
  echo "  adb -s $SERIAL shell appops set com.metro.notifications SYSTEM_ALERT_WINDOW allow"
  echo "  adb -s $SERIAL shell pm grant com.metro.notifications android.permission.WRITE_SECURE_SETTINGS"
fi

REPORT="$ROOT/deploy/deploy-suite-report.json"
mkdir -p "$ROOT/deploy"
python3 - "$REPORT" "$RESULTS" "$BUILD_VARIANT" "$SERIAL" <<'PY'
import json, sys
from datetime import datetime, timezone
path, results, variant, serial = sys.argv[1], json.loads(sys.argv[2]), sys.argv[3], sys.argv[4]
failed = [r for r in results if not r.get("passed")]
report = {
    "timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    "variant": variant,
    "serial": serial,
    "passed": len(failed) == 0,
    "installed_count": len(results) - len(failed),
    "failed_count": len(failed),
    "results": results,
}
with open(path, "w") as f:
    json.dump(report, f, indent=2)
print(f"Report: {path}")
PY

echo ""
echo "deploy-suite: installed ${#INSTALLED[@]}/${#APPS[@]} on $SERIAL"
if [[ ${#SKIPPED[@]} -gt 0 ]]; then
  echo "deploy-suite: skipped (no APK): ${SKIPPED[*]}" >&2
fi
if [[ ${#FAILED[@]} -gt 0 ]]; then
  echo "deploy-suite: FAILED (${#FAILED[@]}): ${FAILED[*]}" >&2
  exit 1
fi
echo "deploy-suite: PASSED"
exit 0
