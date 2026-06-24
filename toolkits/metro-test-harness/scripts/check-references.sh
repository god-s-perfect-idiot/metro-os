#!/usr/bin/env bash
# Validate per-app references/ layout. See apps/_template/references/README.md.
set -euo pipefail

APP_DIR="${1:-}"
if [[ -z "$APP_DIR" || ! -d "$APP_DIR" ]]; then
  echo "Usage: $0 <app-dir>" >&2
  exit 2
fi

APP_NAME=$(basename "$APP_DIR")
REF="$APP_DIR/references"
MISSING=()

require_path() {
  local path="$1"
  local label="$2"
  if [[ ! -e "$path" ]]; then
    MISSING+=("$label")
  fi
}

require_path "$REF/README.md" "references/README.md"
require_path "$REF/web-resources.md" "references/web-resources.md"
require_path "$REF/images" "references/images/"
require_path "$REF/guides" "references/guides/"
require_path "$REF/guides/blueprint.md" "references/guides/blueprint.md"

if [[ ${#MISSING[@]} -gt 0 ]]; then
  echo "FAIL  references layout incomplete for apps/$APP_NAME" >&2
  printf '  missing: %s\n' "${MISSING[@]}" >&2
  echo "  Run: ./scripts/bootstrap-references.sh $APP_NAME" >&2
  exit 1
fi

# Warn when blueprint still has template placeholders
if grep -q '_(List every page' "$REF/guides/blueprint.md" 2>/dev/null || \
   grep -q '_(add rows)' "$REF/guides/blueprint.md" 2>/dev/null; then
  echo "WARN  references/guides/blueprint.md still has placeholders for $APP_NAME"
fi

# Warn when README still has template placeholder rows
if grep -q '_(add rows' "$REF/README.md" 2>/dev/null; then
  echo "WARN  references/README.md still has placeholder screen table — add rows for $APP_NAME"
fi

# Warn when no reference images yet (common during bootstrap)
shopt -s nullglob
images=("$REF/images"/*)
shopt -u nullglob
if [[ ${#images[@]} -eq 0 ]] || [[ ${#images[@]} -eq 1 && "$(basename "${images[0]}")" == ".gitkeep" ]]; then
  echo "WARN  references/images/ empty for $APP_NAME — add WP8.1 screenshots before UI work"
fi

# Warn when web-resources has no http links
if ! grep -qE 'https?://' "$REF/web-resources.md" 2>/dev/null; then
  echo "WARN  references/web-resources.md has no URLs for $APP_NAME"
fi

echo "PASS  check-references ($APP_NAME)"
exit 0
