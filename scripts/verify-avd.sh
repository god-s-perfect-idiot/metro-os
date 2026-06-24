#!/usr/bin/env bash
# On-device completeness checks — screenshots, UI probes, blueprint page coverage.
#
# Usage:
#   ./scripts/verify-avd.sh <app-name>
#
# Output:
#   apps/<name>/deploy/avd-report.json
#   apps/<name>/deploy/avd/*.png
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=lib/metro-common.sh
source "$ROOT/scripts/lib/metro-common.sh"

APP="${1:-}"
if [[ -z "$APP" ]]; then
  echo "Usage: $0 <app-name>" >&2
  exit 2
fi

APP_DIR="$(metro_app_dir "$APP")"
REPORT_DIR="$APP_DIR/deploy"
AVD_DIR="$REPORT_DIR/avd"
REPORT="$REPORT_DIR/avd-report.json"
TIMESTAMP="$(metro_timestamp_utc)"

mkdir -p "$AVD_DIR"

PKG="$(metro_app_package "$APP_DIR")"
COMPONENT="$(metro_app_launch_activity "$APP_DIR")"
BLUEPRINT="$APP_DIR/references/guides/blueprint.md"

STEPS_JSON="[]"
FAILURES_JSON="[]"
CHECKS_JSON="[]"
PASSED=true

record_step() {
  local name="$1" ok="$2" duration="$3" error="${4:-}"
  local entry
  if [[ -n "$error" ]]; then
    entry=$(printf '{"name":"%s","passed":%s,"duration_ms":%s,"error":%s}' \
      "$name" "$ok" "$duration" "$(echo "$error" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')")
  else
    entry=$(printf '{"name":"%s","passed":%s,"duration_ms":%s}' "$name" "$ok" "$duration")
  fi
  STEPS_JSON=$(echo "$STEPS_JSON" | python3 -c "import json,sys; a=json.load(sys.stdin); a.append(json.loads(sys.argv[1])); print(json.dumps(a))" "$entry")
}

record_failure() {
  local step="$1" message="$2" hint="${3:-}"
  PASSED=false
  local entry
  entry=$(printf '{"step":"%s","message":%s,"hint":%s}' \
    "$step" \
    "$(echo "$message" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')" \
    "$(echo "$hint" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')")
  FAILURES_JSON=$(echo "$FAILURES_JSON" | python3 -c "import json,sys; a=json.load(sys.stdin); a.append(json.loads(sys.argv[1])); print(json.dumps(a))" "$entry")
}

record_check() {
  local id="$1" ok="$2" detail="${3:-}"
  id="${id//$'\n'/}"
  detail="${detail//$'\n'/}"
  local entry
  entry=$(printf '{"id":%s,"passed":%s,"detail":%s}' \
    "$(echo "$id" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')" \
    "$ok" \
    "$(echo "$detail" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')")
  CHECKS_JSON=$(echo "$CHECKS_JSON" | python3 -c "import json,sys; a=json.load(sys.stdin); a.append(json.loads(sys.argv[1])); print(json.dumps(a))" "$entry")
}

write_report() {
  python3 - "$REPORT" "$APP" "$TIMESTAMP" "$PASSED" "$STEPS_JSON" "$FAILURES_JSON" "$CHECKS_JSON" "$AVD_DIR" <<'PY'
import json, sys, glob, os
path, app, ts, passed, steps, failures, checks, avd_dir = sys.argv[1:9]
shots = sorted(glob.glob(os.path.join(avd_dir, "*.png")))
report = {
    "app": app,
    "timestamp": ts,
    "passed": passed == "true",
    "device": os.environ.get("METRO_ADB_SERIAL", ""),
    "screenshots": [os.path.relpath(s, os.path.dirname(path)) for s in shots],
    "steps": json.loads(steps),
    "checks": json.loads(checks),
    "failures": json.loads(failures),
}
with open(path, "w") as f:
    json.dump(report, f, indent=2)
PY
  echo "Report: $REPORT"
}

capture_screenshot() {
  local name="$1"
  local out="$AVD_DIR/$name.png"
  adb exec-out screencap -p >"$out"
  echo "$out"
}

ui_dump_contains() {
  local pattern="$1"
  local remote="/sdcard/metro-ui-dump.xml"
  adb shell uiautomator dump "$remote" >/dev/null 2>&1 || true
  adb pull "$remote" /tmp/metro-ui-dump.xml >/dev/null 2>&1 || true
  grep -q "$pattern" /tmp/metro-ui-dump.xml 2>/dev/null
}

foreground_package() {
  adb shell dumpsys window 2>/dev/null | tr -d '\r' | grep -E 'mCurrentFocus|mFocusedApp' | head -1 || true
}

echo "==> verify-avd: $APP"
metro_ensure_avd

start_ms=$(python3 -c 'import time; print(int(time.time()*1000))')

# Package installed
if adb shell pm path "$PKG" 2>/dev/null | grep -q "$PKG"; then
  record_check "package_installed" "true" "$PKG"
  echo "PASS  package_installed"
else
  record_check "package_installed" "false" "$PKG not installed"
  record_failure "package_installed" "Package $PKG not on device" "Run: ./scripts/run-app.sh $APP"
  echo "FAIL  package_installed" >&2
  end_ms=$(python3 -c 'import time; print(int(time.time()*1000))')
  record_step "avd_verify" "false" "$((end_ms - start_ms))" "package missing"
  write_report
  exit 1
fi

# Launch and confirm focus
adb shell am force-stop "$PKG" >/dev/null 2>&1 || true
adb shell am start -n "$COMPONENT" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER >/dev/null
sleep 2

focus="$(foreground_package)"
if echo "$focus" | grep -q "$PKG"; then
  record_check "foreground_activity" "true" "$focus"
  echo "PASS  foreground_activity"
else
  record_check "foreground_activity" "false" "$focus"
  record_failure "foreground_activity" "App not in foreground after launch" "$focus"
  echo "FAIL  foreground_activity" >&2
fi

# Crash probe (recent fatal)
if adb logcat -d -t 80 2>/dev/null | tr -d '\r' | grep -E "FATAL EXCEPTION.*$PKG|AndroidRuntime.*$PKG" >/dev/null; then
  record_check "no_fatal_crash" "false" "FATAL in recent logcat"
  record_failure "crash" "Recent FATAL EXCEPTION for $PKG" "adb logcat -d | tail -100"
  echo "FAIL  no_fatal_crash" >&2
else
  record_check "no_fatal_crash" "true" "no recent fatal for package"
  echo "PASS  no_fatal_crash"
fi

main_shot="$(capture_screenshot "main")"
record_check "screenshot_main" "true" "$(basename "$main_shot")"
echo "PASS  screenshot_main → deploy/avd/$(basename "$main_shot")"

# Blueprint pages (informational checklist)
if [[ -f "$BLUEPRINT" ]]; then
  while IFS= read -r page; do
    [[ -z "$page" ]] && continue
    record_check "blueprint_page" "true" "$page (manual visual review)"
    echo "INFO  blueprint page: $page"
  done < <(python3 - "$BLUEPRINT" <<'PY'
import re, sys
text = open(sys.argv[1]).read()
# Bullets like "- Tiles in start menu:" under a Pages section
in_pages = False
for line in text.splitlines():
    if re.match(r'^#+\s*Pages', line, re.I):
        in_pages = True
        continue
    if in_pages and re.match(r'^#+\s', line) and not re.match(r'^#+\s*Pages', line, re.I):
        break
    if in_pages:
        m = re.match(r'^-\s+(.+?)\s*:?\s*$', line)
        if m:
            print(m.group(1).strip())
PY
)
else
  record_check "blueprint_present" "false" "missing references/guides/blueprint.md"
  record_failure "blueprint" "Missing blueprint.md" "Add apps/$APP/references/guides/blueprint.md"
  echo "WARN  blueprint missing"
fi

# App-specific UI flows (testTag probes when present)
case "$APP" in
  launcher)
    if ui_dump_contains 'metro_page_start'; then
      record_check "ui_start_screen" "true" "testTag metro_page_start"
      echo "PASS  ui_start_screen"
    else
      record_check "ui_start_screen" "false" "testTag metro_page_start not in UI dump"
      record_failure "ui_start_screen" "Start screen testTag not found" "Add Modifier.testTag(\"metro_page_start\") or fix layout"
      echo "FAIL  ui_start_screen" >&2
    fi

    adb shell input swipe 900 640 100 640 350 >/dev/null 2>&1 || true
    sleep 1
    list_shot="$(capture_screenshot "app_list")"
    record_check "screenshot_app_list" "true" "$(basename "$list_shot")"
    echo "PASS  screenshot_app_list → deploy/avd/$(basename "$list_shot")"

    if ui_dump_contains 'metro_page_app_list'; then
      record_check "ui_app_list" "true" "testTag metro_page_app_list"
      echo "PASS  ui_app_list"
    else
      record_check "ui_app_list" "false" "testTag metro_page_app_list not in UI dump"
      record_failure "ui_app_list" "App list testTag not found after swipe" "Check HorizontalPager navigation"
      echo "FAIL  ui_app_list" >&2
    fi
    ;;
esac

end_ms=$(python3 -c 'import time; print(int(time.time()*1000))')
if [[ "$PASSED" == "true" ]]; then
  record_step "avd_verify" "true" "$((end_ms - start_ms))"
  write_report
  echo "verify-avd: PASSED"
  exit 0
fi

record_step "avd_verify" "false" "$((end_ms - start_ms))" "one or more checks failed"
write_report
echo "verify-avd: FAILED — see $REPORT and deploy/avd/*.png" >&2
exit 1
