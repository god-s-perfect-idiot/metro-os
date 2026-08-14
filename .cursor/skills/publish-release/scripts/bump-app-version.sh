#!/usr/bin/env bash
# Bump versionName / versionCode for a metro-os suite app.
#
# Usage:
#   bump-app-version.sh <app> minor|major|patch [--dry-run]
#   bump-app-version.sh --read <app>
#   bump-app-version.sh --list-suite
#
# Standard apps: apps/<name>/app/build.gradle.kts
#   versionCode = N
#   versionName = "MAJOR.MINOR.PATCH"
#
# keyboard is skipped (FlorisBoard fork; versions live in gradle.properties).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# .cursor/skills/publish-release/scripts → repo root
ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
if [[ ! -d "$ROOT/apps" ]]; then
  # Fallback: git toplevel when invoked from elsewhere
  ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || true)"
fi
if [[ -z "${ROOT:-}" || ! -d "$ROOT/apps" ]]; then
  echo "ERROR: could not locate metro-os repo root (expected apps/)" >&2
  exit 1
fi

usage() {
  sed -n '2,14p' "$0"
  exit 2
}

is_complete_app() {
  local name="$1"
  [[ -d "$ROOT/apps/$name/app" && -x "$ROOT/apps/$name/gradlew" ]]
}

gradle_file() {
  echo "$ROOT/apps/$1/app/build.gradle.kts"
}

read_version() {
  local app="$1"
  local file
  file="$(gradle_file "$app")"
  if [[ "$app" == "keyboard" ]]; then
    echo "keyboard uses apps/keyboard/gradle.properties (not suite-bumped)" >&2
    return 1
  fi
  if [[ ! -f "$file" ]]; then
    echo "ERROR: missing $file" >&2
    return 1
  fi
  local code name
  code="$(rg -oN --pcre2 '^\s*versionCode\s*=\s*\K[0-9]+' "$file" | head -1)"
  name="$(rg -oN --pcre2 '^\s*versionName\s*=\s*"\K[0-9]+\.[0-9]+\.[0-9]+' "$file" | head -1)"
  if [[ -z "$code" || -z "$name" ]]; then
    echo "ERROR: could not parse versionCode/versionName in $file" >&2
    return 1
  fi
  printf '%s %s\n' "$code" "$name"
}

bump_semver() {
  local name="$1"
  local kind="$2"
  local major minor patch
  IFS=. read -r major minor patch <<<"$name"
  case "$kind" in
    major)
      major=$((major + 1))
      minor=0
      patch=0
      ;;
    minor)
      minor=$((minor + 1))
      patch=0
      ;;
    patch)
      patch=$((patch + 1))
      ;;
    *)
      echo "ERROR: kind must be major|minor|patch (got: $kind)" >&2
      return 1
      ;;
  esac
  printf '%s.%s.%s\n' "$major" "$minor" "$patch"
}

list_suite() {
  local name file code ver
  for dir in "$ROOT"/apps/*/; do
    name="$(basename "$dir")"
    [[ "$name" == "_template" ]] && continue
    is_complete_app "$name" || continue
    if [[ "$name" == "keyboard" ]]; then
      printf '%-14s  (skipped — keyboard gradle.properties)\n' "$name"
      continue
    fi
    file="$(gradle_file "$name")"
    if [[ ! -f "$file" ]]; then
      printf '%-14s  (no build.gradle.kts)\n' "$name"
      continue
    fi
    if code_ver="$(read_version "$name" 2>/dev/null)"; then
      read -r code ver <<<"$code_ver"
      printf '%-14s  versionCode=%s  versionName=%s\n' "$name" "$code" "$ver"
    else
      printf '%-14s  (unparseable)\n' "$name"
    fi
  done
}

apply_bump() {
  local app="$1"
  local kind="$2"
  local dry_run="${3:-0}"
  local file code name new_name new_code

  if [[ "$app" == "keyboard" ]]; then
    echo "ERROR: keyboard is not suite-bumped; edit apps/keyboard/gradle.properties manually if needed" >&2
    return 1
  fi
  if ! is_complete_app "$app"; then
    echo "ERROR: apps/$app is not a complete (buildable) app" >&2
    return 1
  fi

  read -r code name <<<"$(read_version "$app")"
  new_name="$(bump_semver "$name" "$kind")"
  new_code=$((code + 1))
  file="$(gradle_file "$app")"

  echo "$app: $name ($code) → $new_name ($new_code) [$kind]"

  if [[ "$dry_run" == "1" ]]; then
    return 0
  fi

  # Replace only the defaultConfig version lines (first matches).
  perl -i -0pe \
    's/(versionCode\s*=\s*)\d+/${1}'"$new_code"'/; s/(versionName\s*=\s*")[^"]+/${1}'"$new_name"'/' \
    "$file"

  # Verify
  read -r got_code got_name <<<"$(read_version "$app")"
  if [[ "$got_code" != "$new_code" || "$got_name" != "$new_name" ]]; then
    echo "ERROR: bump verification failed for $app (got $got_name/$got_code)" >&2
    return 1
  fi
}

# --- main ---
if [[ $# -lt 1 ]]; then
  usage
fi

case "$1" in
  --list-suite)
    list_suite
    ;;
  --read)
    [[ $# -eq 2 ]] || usage
    read_version "$2"
    ;;
  -h|--help)
    usage
    ;;
  *)
    [[ $# -ge 2 ]] || usage
    APP="$1"
    KIND="$2"
    DRY=0
    if [[ "${3:-}" == "--dry-run" ]]; then
      DRY=1
    elif [[ -n "${3:-}" ]]; then
      usage
    fi
    apply_bump "$APP" "$KIND" "$DRY"
    ;;
esac
