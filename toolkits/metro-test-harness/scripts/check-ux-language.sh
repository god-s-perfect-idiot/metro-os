#!/usr/bin/env bash
# Verify Metro UX language spec is present and wired into agent docs.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
UX_SPEC="$ROOT/toolkits/metro-ui-android/METRO-UX-LANGUAGE.md"
FAIL=0

if [[ ! -f "$UX_SPEC" ]]; then
  echo "FAIL  missing toolkits/metro-ui-android/METRO-UX-LANGUAGE.md" >&2
  exit 1
fi

require_reference() {
  local file="$1"
  local label="$2"
  if [[ ! -f "$file" ]]; then
    echo "WARN  $label not found — skipping link check"
    return 0
  fi
  if ! grep -q 'METRO-UX-LANGUAGE' "$file" 2>/dev/null; then
    echo "FAIL  $label does not reference METRO-UX-LANGUAGE.md" >&2
    FAIL=1
  fi
}

require_reference "$ROOT/AGENTS.md" "AGENTS.md"
require_reference "$ROOT/docs/HARNESS.md" "docs/HARNESS.md"
require_reference "$ROOT/docs/DESIGN-CHECKLIST.md" "docs/DESIGN-CHECKLIST.md"
require_reference "$ROOT/toolkits/metro-ui-android/AGENTS.md" "toolkits/metro-ui-android/AGENTS.md"

if [[ "$FAIL" -ne 0 ]]; then
  exit 1
fi

echo "PASS  check-ux-language"
exit 0
