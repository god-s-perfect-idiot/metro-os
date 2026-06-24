#!/usr/bin/env bash
# Verify a toolkit module. See toolkits/<name>/AGENTS.md.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TK="${1:-}"

if [[ -z "$TK" ]]; then
  echo "Usage: $0 <toolkit-name>" >&2
  echo "Names: metro-ui-android | metro-system-sdk | metro-test-harness" >&2
  exit 2
fi

TK_DIR="$ROOT/toolkits/$TK"
REPORT_DIR="$TK_DIR/build"
REPORT="$REPORT_DIR/verify-report.json"
mkdir -p "$REPORT_DIR"

echo "==> verify-toolkit: $TK"

if [[ ! -d "$TK_DIR" ]]; then
  echo "ERROR: toolkits/$TK not found" >&2
  exit 1
fi

if [[ ! -f "$TK_DIR/AGENTS.md" ]]; then
  echo "ERROR: toolkits/$TK/AGENTS.md missing" >&2
  exit 1
fi

HARNESS="$ROOT/toolkits/metro-test-harness"
if [[ -x "$HARNESS/scripts/check-ux-language.sh" ]]; then
  "$HARNESS/scripts/check-ux-language.sh" || exit 1
fi

if [[ "$TK" == "metro-ui-android" && ! -f "$TK_DIR/METRO-UX-LANGUAGE.md" ]]; then
  echo "ERROR: toolkits/metro-ui-android/METRO-UX-LANGUAGE.md missing" >&2
  exit 1
fi

cd "$TK_DIR"
PASSED=true
STEPS="[]"

if [[ -f "./gradlew" ]]; then
  ./gradlew build --quiet || PASSED=false
  ./gradlew test --quiet || PASSED=false
else
  echo "WARN  no gradlew — toolkit not scaffolded yet"
fi

python3 - "$REPORT" "$TK" "$PASSED" <<'PY'
import json, sys
from datetime import datetime, timezone
path, name, passed = sys.argv[1], sys.argv[2], sys.argv[3] == "true"
report = {
    "toolkit": name,
    "timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    "passed": passed,
}
with open(path, "w") as f:
    json.dump(report, f, indent=2)
PY

if [[ "$PASSED" != true ]]; then
  echo "verify-toolkit: FAILED" >&2
  exit 1
fi

echo "verify-toolkit: PASSED"
exit 0
