#!/usr/bin/env bash
# Verify JDK, Android SDK, and adb are available.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ERRORS=0

fail() { echo "ERROR: $*" >&2; ERRORS=$((ERRORS + 1)); }
warn() { echo "WARN: $*" >&2; }

echo "==> verify-env: metro-os environment"

# Java
if command -v java >/dev/null 2>&1; then
  JAVA_VER=$(java -version 2>&1 | head -1)
  echo "OK  java: $JAVA_VER"
else
  fail "java not found — install JDK 17+"
fi

# Android SDK
if [[ -n "${ANDROID_HOME:-}" && -d "$ANDROID_HOME" ]]; then
  echo "OK  ANDROID_HOME=$ANDROID_HOME"
elif [[ -d "$HOME/Library/Android/sdk" ]]; then
  warn "ANDROID_HOME unset; found $HOME/Library/Android/sdk — set ANDROID_HOME"
elif [[ -d "$HOME/Android/Sdk" ]]; then
  warn "ANDROID_HOME unset; found $HOME/Android/Sdk — set ANDROID_HOME"
else
  fail "Android SDK not found — install Android Studio or command-line tools"
fi

# adb
if command -v adb >/dev/null 2>&1; then
  DEVICES=$(adb devices 2>/dev/null | grep -v "List" | grep "device$" | wc -l | tr -d ' ')
  echo "OK  adb: $DEVICES device(s) connected"
else
  warn "adb not in PATH — instrumented tests will be skipped"
fi

# Repo structure
for f in scope.md AGENTS.md docs/HARNESS.md; do
  if [[ -f "$ROOT/$f" ]]; then
    echo "OK  $f"
  else
    fail "missing $f"
  fi
done

if [[ $ERRORS -gt 0 ]]; then
  echo "verify-env: FAILED ($ERRORS error(s))" >&2
  exit 1
fi

echo "verify-env: PASSED"
exit 0
