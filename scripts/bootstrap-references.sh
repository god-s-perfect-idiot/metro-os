#!/usr/bin/env bash
# Create or refresh apps/<name>/references/ from _template.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEMPLATE="$ROOT/apps/_template/references"

if [[ ! -d "$TEMPLATE" ]]; then
  echo "ERROR: apps/_template/references missing" >&2
  exit 1
fi

bootstrap_app() {
  local name="$1"
  local app_dir="$ROOT/apps/$name"
  local dest="$app_dir/references"

  if [[ ! -d "$app_dir" ]]; then
    echo "SKIP  apps/$name (no app dir)"
    return 0
  fi

  local display_name
  display_name=$(python3 -c "print('${name}'.replace('-', ' ').title())")

  mkdir -p "$dest/images" "$dest/guides"
  touch "$dest/images/.gitkeep" "$dest/guides/.gitkeep"

  for file in README.md web-resources.md; do
    if [[ ! -f "$dest/$file" ]]; then
      sed \
        -e "s/{{APP_NAME}}/$name/g" \
        -e "s/{{DISPLAY_NAME}}/$display_name/g" \
        "$TEMPLATE/$file" > "$dest/$file"
      echo "CREATE apps/$name/references/$file"
    else
      echo "KEEP  apps/$name/references/$file"
    fi
  done

  if [[ ! -f "$dest/guides/blueprint.md" ]]; then
    sed \
      -e "s/{{APP_NAME}}/$name/g" \
      -e "s/{{DISPLAY_NAME}}/$display_name/g" \
      "$TEMPLATE/guides/blueprint.md" > "$dest/guides/blueprint.md"
    echo "CREATE apps/$name/references/guides/blueprint.md"
  else
    echo "KEEP  apps/$name/references/guides/blueprint.md"
  fi
}

if [[ $# -gt 0 ]]; then
  for name in "$@"; do
    bootstrap_app "$name"
  done
else
  for app_dir in "$ROOT/apps"/*/; do
    name=$(basename "$app_dir")
    [[ "$name" == "_template" ]] && continue
    bootstrap_app "$name"
  done
fi

echo "Done. Fill references/guides/blueprint.md for each app; add images as visual aids."
