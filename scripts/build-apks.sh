#!/usr/bin/env bash
# Build debug (or release) APKs for every complete metro-os app.
#
# "Complete" means the Android project is present and buildable:
#   apps/<name>/app/ exists and apps/<name>/gradlew is executable.
# Docs-only apps (no Gradle project) are skipped.
#
# Usage:
#   ./scripts/build-apks.sh                 # build all complete apps
#   ./scripts/build-apks.sh launcher photos # build named apps only
#   ./scripts/build-apks.sh --list          # print complete apps, no build
#   ./scripts/build-apks.sh --release       # assembleRelease instead of debug
#   ./scripts/build-apks.sh --continue-on-error
#
# Outputs (gitignored under deploy/):
#   apps/<name>/deploy/app-debug.apk   (or app-release-unsigned.apk)
#   deploy/apks/<name>-debug.apk       (suite collection copy)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=lib/metro-common.sh
source "$ROOT/scripts/lib/metro-common.sh"

# Same tier order as verify-all.sh
APP_ORDER=(
  launcher statusbar navbar
  browser notes music
  photos calendar mail messaging people dialer store settings calculator clock files
)

BUILD_VARIANT="debug"
GRADLE_TASK=":app:assembleDebug"
LIST_ONLY=0
CONTINUE_ON_ERROR=0
REQUESTED=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --list)
      LIST_ONLY=1
      ;;
    --release)
      BUILD_VARIANT="release"
      GRADLE_TASK=":app:assembleRelease"
      ;;
    --continue-on-error)
      CONTINUE_ON_ERROR=1
      ;;
    -h|--help)
      sed -n '2,18p' "$0"
      exit 0
      ;;
    -*)
      echo "Unknown option: $1" >&2
      echo "Usage: $0 [--list] [--release] [--continue-on-error] [app-name...]" >&2
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

  # Any extra scaffolded apps not in APP_ORDER
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

apk_build_path() {
  local app_dir="$1"
  if [[ "$BUILD_VARIANT" == "debug" ]]; then
    echo "$app_dir/app/build/outputs/apk/debug/app-debug.apk"
  else
    # Prefer unsigned release naming from AGP; fall back to signed if present
    local unsigned="$app_dir/app/build/outputs/apk/release/app-release-unsigned.apk"
    local signed="$app_dir/app/build/outputs/apk/release/app-release.apk"
    if [[ -f "$unsigned" ]]; then
      echo "$unsigned"
    else
      echo "$signed"
    fi
  fi
}

apk_deploy_name() {
  if [[ "$BUILD_VARIANT" == "debug" ]]; then
    echo "app-debug.apk"
  else
    echo "app-release.apk"
  fi
}

apk_suite_name() {
  local name="$1"
  if [[ "$BUILD_VARIANT" == "debug" ]]; then
    echo "${name}-debug.apk"
  else
    echo "${name}-release.apk"
  fi
}

APPS=()
while IFS= read -r _app; do
  [[ -n "$_app" ]] && APPS+=("$_app")
done < <(list_complete_apps)

if [[ ${#APPS[@]} -eq 0 ]]; then
  echo "ERROR: no complete apps found under apps/" >&2
  exit 1
fi

echo "==> build-apks: ${#APPS[@]} complete app(s) ($BUILD_VARIANT)"
for app in "${APPS[@]}"; do
  echo "  - $app"
done

if [[ "$LIST_ONLY" -eq 1 ]]; then
  exit 0
fi

SUITE_DIR="$ROOT/deploy/apks"
mkdir -p "$SUITE_DIR"

BUILT=()
FAILED=()
RESULTS="[]"

for app in "${APPS[@]}"; do
  app_dir="$ROOT/apps/$app"
  echo ""
  echo "--- $app"
  echo "==> $GRADLE_TASK"

  if (cd "$app_dir" && ./gradlew "$GRADLE_TASK" --quiet); then
    build_apk="$(apk_build_path "$app_dir")"
    # Resolve path after build (release may be unsigned or signed)
    if [[ "$BUILD_VARIANT" == "release" ]]; then
      if [[ -f "$app_dir/app/build/outputs/apk/release/app-release-unsigned.apk" ]]; then
        build_apk="$app_dir/app/build/outputs/apk/release/app-release-unsigned.apk"
      elif [[ -f "$app_dir/app/build/outputs/apk/release/app-release.apk" ]]; then
        build_apk="$app_dir/app/build/outputs/apk/release/app-release.apk"
      else
        build_apk=""
      fi
    fi

    if [[ -z "${build_apk:-}" || ! -f "$build_apk" ]]; then
      echo "ERROR: APK missing after build for $app" >&2
      FAILED+=("$app")
      RESULTS=$(echo "$RESULTS" | python3 -c "import json,sys; a=json.load(sys.stdin); a.append({'name':'$app','passed':False,'error':'apk_missing'}); print(json.dumps(a))")
      if [[ "$CONTINUE_ON_ERROR" -eq 0 ]]; then
        exit 1
      fi
      continue
    fi

    mkdir -p "$app_dir/deploy"
    deploy_apk="$app_dir/deploy/$(apk_deploy_name)"
    suite_apk="$SUITE_DIR/$(apk_suite_name "$app")"
    cp -f "$build_apk" "$deploy_apk"
    cp -f "$build_apk" "$suite_apk"

    size="$(wc -c < "$suite_apk" | tr -d ' ')"
    echo "OK  $app → $suite_apk ($size bytes)"
    BUILT+=("$app")
    RESULTS=$(echo "$RESULTS" | python3 -c "import json,sys; a=json.load(sys.stdin); a.append({'name':'$app','passed':True,'apk':'$suite_apk','bytes':$size}); print(json.dumps(a))")
  else
    echo "ERROR: build failed for $app" >&2
    FAILED+=("$app")
    RESULTS=$(echo "$RESULTS" | python3 -c "import json,sys; a=json.load(sys.stdin); a.append({'name':'$app','passed':False,'error':'build_failed'}); print(json.dumps(a))")
    if [[ "$CONTINUE_ON_ERROR" -eq 0 ]]; then
      exit 1
    fi
  fi
done

REPORT="$ROOT/deploy/build-apks-report.json"
python3 - "$REPORT" "$RESULTS" "$BUILD_VARIANT" <<'PY'
import json, sys
from datetime import datetime, timezone
path, results, variant = sys.argv[1], json.loads(sys.argv[2]), sys.argv[3]
failed = [r for r in results if not r.get("passed")]
report = {
    "timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    "variant": variant,
    "passed": len(failed) == 0,
    "built_count": len(results) - len(failed),
    "failed_count": len(failed),
    "results": results,
}
with open(path, "w") as f:
    json.dump(report, f, indent=2)
print(f"Report: {path}")
PY

echo ""
echo "build-apks: built ${#BUILT[@]}/${#APPS[@]} → $SUITE_DIR"
if [[ ${#FAILED[@]} -gt 0 ]]; then
  echo "build-apks: FAILED (${#FAILED[@]}): ${FAILED[*]}" >&2
  exit 1
fi
echo "build-apks: PASSED"
exit 0
