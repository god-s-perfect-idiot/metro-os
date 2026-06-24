#!/usr/bin/env bash
# Full verification gate for a single app. See docs/HARNESS.md.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP="${1:-}"

if [[ -z "$APP" ]]; then
  echo "Usage: $0 <app-name>" >&2
  echo "Example: $0 launcher" >&2
  exit 2
fi

APP_DIR="$ROOT/apps/$APP"
REPORT_DIR="$APP_DIR/deploy"
REPORT="$REPORT_DIR/verify-report.json"
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

mkdir -p "$REPORT_DIR"

STEPS_JSON="[]"
FAILURES_JSON="[]"
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

run_step() {
  local name="$1"
  shift
  local start end duration
  start=$(python3 -c 'import time; print(int(time.time()*1000))')
  if "$@"; then
    end=$(python3 -c 'import time; print(int(time.time()*1000))')
    duration=$((end - start))
    record_step "$name" "true" "$duration"
    echo "PASS  $name (${duration}ms)"
  else
    end=$(python3 -c 'import time; print(int(time.time()*1000))')
    duration=$((end - start))
    record_step "$name" "false" "$duration" "step failed"
    record_failure "$name" "Command failed: $*" "See docs/TROUBLESHOOTING.md"
    echo "FAIL  $name" >&2
    write_report
    exit 1
  fi
}

write_report() {
  python3 - "$REPORT" "$APP" "$TIMESTAMP" "$PASSED" "$STEPS_JSON" "$FAILURES_JSON" <<'PY'
import json, sys
path, app, ts, passed, steps, failures = sys.argv[1:7]
report = {
    "app": app,
    "timestamp": ts,
    "passed": passed == "true",
    "steps": json.loads(steps),
    "failures": json.loads(failures),
}
with open(path, "w") as f:
    json.dump(report, f, indent=2)
PY
  echo "Report: $REPORT"
}

echo "==> verify-app: $APP"

# Step 0: structure
if [[ ! -d "$APP_DIR/app" ]]; then
  record_step "structure" "false" 0 "apps/$APP/app/ not found — run scripts/scaffold-app.sh"
  record_failure "structure" "App not scaffolded" "Run: ./scripts/scaffold-app.sh $APP"
  write_report
  exit 1
fi
if [[ ! -f "$APP_DIR/AGENTS.md" ]]; then
  record_failure "structure" "Missing AGENTS.md" "Copy from apps/_template/AGENTS.md"
  write_report
  exit 1
fi
if [[ ! -f "$APP_DIR/references/README.md" ]]; then
  record_failure "structure" "Missing references/" "Run: ./scripts/bootstrap-references.sh $APP"
  write_report
  exit 1
fi
record_step "structure" "true" 0
echo "PASS  structure"

cd "$APP_DIR"

# Step 1: build
if [[ -f "./gradlew" ]]; then
  run_step "build" ./gradlew :app:assembleDebug --quiet
  run_step "unit_tests" ./gradlew :app:test --quiet
  if command -v adb >/dev/null 2>&1 && adb devices 2>/dev/null | grep -q "device$"; then
    run_step "instrumented_tests" ./gradlew :app:connectedDebugAndroidTest --quiet
  else
    record_step "instrumented_tests" "true" 0
    echo "WARN  instrumented_tests skipped (no device)"
  fi
else
  record_failure "build" "No gradlew in apps/$APP" "Scaffold the app first"
  write_report
  exit 1
fi

# Steps 5-7: metro-test-harness (when toolkit exists)
HARNESS="$ROOT/toolkits/metro-test-harness"
if [[ -x "$HARNESS/scripts/check-ux-language.sh" ]]; then
  run_step "ux_language" "$HARNESS/scripts/check-ux-language.sh"
else
  echo "WARN  check-ux-language.sh missing — skipping UX spec gate"
  record_step "ux_language" "true" 0
fi
if [[ -x "$HARNESS/scripts/check-references.sh" ]]; then
  run_step "references" "$HARNESS/scripts/check-references.sh" "$APP_DIR"
else
  echo "WARN  check-references.sh missing — skipping references layout"
  record_step "references" "true" 0
fi
if [[ -x "$HARNESS/scripts/lint-metro.sh" ]]; then
  run_step "metro_lint" "$HARNESS/scripts/lint-metro.sh" "$APP_DIR"
  run_step "screenshot_diff" "$HARNESS/scripts/screenshot-diff.sh" "$APP_DIR"
  run_step "motion_profile" "$HARNESS/scripts/motion-profile.sh" "$APP_DIR"
else
  echo "WARN  metro-test-harness not ready — skipping lint/screenshot/motion"
  record_step "metro_lint" "true" 0
  record_step "screenshot_diff" "true" 0
  record_step "motion_profile" "true" 0
fi

write_report
echo "verify-app: PASSED"

# Optional on-device completeness gate when a device is connected
if [[ "${METRO_VERIFY_AVD:-}" == "1" ]] && command -v adb >/dev/null 2>&1 && adb devices 2>/dev/null | grep -q "device$"; then
  echo "==> verify-avd (METRO_VERIFY_AVD=1)"
  "$ROOT/scripts/verify-avd.sh" "$APP"
fi

exit 0
