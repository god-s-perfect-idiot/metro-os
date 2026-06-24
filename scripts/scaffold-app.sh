#!/usr/bin/env bash
# Scaffold a new app from _template. See scope.md app inventory.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NAME="${1:-}"
TIER="${2:-1}"
SUFFIX="${3:-$NAME}"

if [[ -z "$NAME" ]]; then
  echo "Usage: $0 <app-name> [tier] [package-suffix]" >&2
  echo "Example: $0 browser 1 browser" >&2
  exit 2
fi

if [[ "$NAME" == "_template" ]]; then
  echo "ERROR: cannot scaffold _template" >&2
  exit 1
fi

DEST="$ROOT/apps/$NAME"
TEMPLATE="$ROOT/apps/_template"

if [[ -d "$DEST" ]]; then
  echo "ERROR: apps/$NAME already exists" >&2
  exit 1
fi

if [[ ! -d "$TEMPLATE" ]]; then
  echo "ERROR: apps/_template missing" >&2
  exit 1
fi

echo "==> scaffold-app: $NAME (tier $TIER, com.metro.$SUFFIX)"

cp -R "$TEMPLATE" "$DEST"
rm -f "$DEST/AGENTS.md.bak" 2>/dev/null || true

# Replace placeholders
PACKAGE="com.metro.$SUFFIX"
DISPLAY_NAME=$(python3 -c "print('${NAME}'.replace('-', ' ').title())")

if [[ "$(uname)" == "Darwin" ]]; then
  sed -i '' "s/{{APP_NAME}}/$NAME/g" "$DEST/AGENTS.md" "$DEST/README.md"
  sed -i '' "s/{{DISPLAY_NAME}}/$DISPLAY_NAME/g" "$DEST/AGENTS.md" "$DEST/README.md"
  sed -i '' "s/{{PACKAGE}}/$PACKAGE/g" "$DEST/AGENTS.md" "$DEST/README.md"
  sed -i '' "s/{{TIER}}/$TIER/g" "$DEST/AGENTS.md" "$DEST/README.md"
else
  sed -i "s/{{APP_NAME}}/$NAME/g" "$DEST/AGENTS.md" "$DEST/README.md"
  sed -i "s/{{DISPLAY_NAME}}/$DISPLAY_NAME/g" "$DEST/AGENTS.md" "$DEST/README.md"
  sed -i "s/{{PACKAGE}}/$PACKAGE/g" "$DEST/AGENTS.md" "$DEST/README.md"
  sed -i "s/{{TIER}}/$TIER/g" "$DEST/AGENTS.md" "$DEST/README.md"
fi

mkdir -p "$DEST/screenshots/golden" "$DEST/deploy"
touch "$DEST/screenshots/golden/.gitkeep"

"$ROOT/scripts/bootstrap-references.sh" "$NAME"

echo "Created apps/$NAME"
echo "Next: customize apps/$NAME/AGENTS.md, references/README.md, and references/web-resources.md"
echo "      add Gradle project under apps/$NAME/app/"
