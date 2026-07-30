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

# If lint-engine-exception exists (vendored IME engines), only enforce on com.metro packages.
if [[ -f "$APP_DIR/lint-engine-exception" ]]; then
  SCAN_ROOTS=()
  [[ -d "$SRC/main/kotlin/com/metro" ]] && SCAN_ROOTS+=("$SRC/main/kotlin/com/metro")
  [[ -d "$SRC/main/java/com/metro" ]] && SCAN_ROOTS+=("$SRC/main/java/com/metro")
  [[ -d "$SRC/test/kotlin/com/metro" ]] && SCAN_ROOTS+=("$SRC/test/kotlin/com/metro")
  [[ -d "$SRC/test/java/com/metro" ]] && SCAN_ROOTS+=("$SRC/test/java/com/metro")
  echo "INFO  lint-engine-exception present — scanning com.metro only"
  FOUND=false
  for root in "${SCAN_ROOTS[@]}"; do
    if grep -rE "$BANNED" "$root" --include='*.kt' 2>/dev/null; then
      FOUND=true
    fi
  done
  if [[ "$FOUND" == "true" ]]; then
    echo "FAIL  banned Material import found in com.metro UI" >&2
    echo "  See toolkits/metro-ui-android/METRO-UX-LANGUAGE.md §12" >&2
    exit 1
  fi
  echo "PASS  lint-metro"
  exit 0
fi

if grep -rE "$BANNED" "$SRC" --include='*.kt' 2>/dev/null; then
  echo "FAIL  banned Material import found" >&2
  echo "  See toolkits/metro-ui-android/METRO-UX-LANGUAGE.md §12" >&2
  exit 1
fi

echo "PASS  lint-metro"
exit 0
