#!/usr/bin/env bash
# Metro lint stub — scans for banned Material imports.
set -euo pipefail

APP_DIR="${1:-}"
if [[ -z "$APP_DIR" || ! -d "$APP_DIR" ]]; then
  echo "Usage: $0 <app-dir>" >&2
  exit 2
fi

SRC="$APP_DIR/app/src"
if [[ ! -d "$SRC" ]]; then
  echo "WARN  no app/src — skipping lint"
  exit 0
fi

BANNED='com\.google\.android\.material|androidx\.compose\.material3\.(?!icons)'
if grep -rE "$BANNED" "$SRC" --include='*.kt' 2>/dev/null; then
  echo "FAIL  banned Material import found" >&2
  echo "  See toolkits/metro-ui-android/METRO-UX-LANGUAGE.md §12" >&2
  exit 1
fi

echo "PASS  lint-metro"
exit 0
