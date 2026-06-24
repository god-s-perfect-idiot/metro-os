#!/usr/bin/env bash
# Verify all scaffolded apps and toolkits. See docs/HARNESS.md.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPORT="$ROOT/deploy/verify-suite.json"
mkdir -p "$ROOT/deploy"

RESULTS="[]"
FAILED=0

echo "==> verify-all: metro-os suite"

# Toolkits first (dependency order)
for tk in metro-ui-android metro-system-sdk metro-test-harness; do
  if [[ -d "$ROOT/toolkits/$tk" ]]; then
  echo "--- toolkit: $tk"
    if "$ROOT/scripts/verify-toolkit.sh" "$tk"; then
      RESULTS=$(echo "$RESULTS" | python3 -c "import json,sys; a=json.load(sys.stdin); a.append({'name':'$tk','type':'toolkit','passed':True}); print(json.dumps(a))")
    else
      RESULTS=$(echo "$RESULTS" | python3 -c "import json,sys; a=json.load(sys.stdin); a.append({'name':'$tk','type':'toolkit','passed':False}); print(json.dumps(a))")
      FAILED=$((FAILED + 1))
    fi
  fi
done

# Apps in tier order
for app in launcher statusbar navbar browser notes music \
           photos calendar mail messaging people store settings calculator clock files; do
  if [[ -d "$ROOT/apps/$app/app" ]]; then
    echo "--- app: $app"
    if "$ROOT/scripts/verify-app.sh" "$app"; then
      RESULTS=$(echo "$RESULTS" | python3 -c "import json,sys; a=json.load(sys.stdin); a.append({'name':'$app','type':'app','passed':True}); print(json.dumps(a))")
    else
      RESULTS=$(echo "$RESULTS" | python3 -c "import json,sys; a=json.load(sys.stdin); a.append({'name':'$app','type':'app','passed':False}); print(json.dumps(a))")
      FAILED=$((FAILED + 1))
    fi
  fi
done

python3 - "$REPORT" "$RESULTS" "$FAILED" <<'PY'
import json, sys
from datetime import datetime, timezone
path, results, failed = sys.argv[1], json.loads(sys.argv[2]), int(sys.argv[3])
report = {
    "timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    "passed": failed == 0,
    "failed_count": failed,
    "results": results,
}
with open(path, "w") as f:
    json.dump(report, f, indent=2)
print(f"Report: {path}")
PY

if [[ $FAILED -gt 0 ]]; then
  echo "verify-all: FAILED ($FAILED module(s))" >&2
  exit 1
fi

echo "verify-all: PASSED"
exit 0
