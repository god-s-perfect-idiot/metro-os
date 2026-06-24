#!/usr/bin/env bash
# Screenshot diff stub — passes if no golden dir or harness not fully built.
set -euo pipefail

APP_DIR="${1:-}"
GOLDEN="$APP_DIR/screenshots/golden"

if [[ ! -d "$GOLDEN" ]] || [[ -z "$(ls -A "$GOLDEN" 2>/dev/null | grep -v gitkeep)" ]]; then
  echo "WARN  no golden screenshots — skipping diff"
  exit 0
fi

# Full implementation: capture emulator frame, compare with ImageMagick or harness lib
echo "WARN  screenshot-diff not fully implemented — add golden images + harness module"
exit 0
