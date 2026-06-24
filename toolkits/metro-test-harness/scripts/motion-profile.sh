#!/usr/bin/env bash
# Motion profile stub — passes until instrumentation tests exist.
set -euo pipefail

APP_DIR="${1:-}"
if [[ ! -d "$APP_DIR/app/src" ]]; then
  echo "WARN  no app source — skipping motion profile"
  exit 0
fi

echo "WARN  motion-profile not fully implemented — add @MetroMotionTest instrumentation"
exit 0
