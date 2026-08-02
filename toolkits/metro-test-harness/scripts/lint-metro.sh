#!/usr/bin/env bash
# Metro lint — banned Material imports + WP8.1 alignment rules.
# See toolkits/metro-ui-android/METRO-UX-LANGUAGE.md §6.3 and §12.
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
  SCAN_PATHS=("${SCAN_ROOTS[@]}")
else
  if grep -rE "$BANNED" "$SRC" --include='*.kt' 2>/dev/null; then
    echo "FAIL  banned Material import found" >&2
    echo "  See toolkits/metro-ui-android/METRO-UX-LANGUAGE.md §12" >&2
    exit 1
  fi
  SCAN_PATHS=("$SRC")
fi

# Alignment rules (flush-left setup chrome). Failures exit 1.
python3 - "$APP_DIR" "${SCAN_PATHS[@]}" <<'PY'
import re
import sys
from pathlib import Path

app_dir = Path(sys.argv[1])
roots = [Path(p) for p in sys.argv[2:]]
failures = []

def kt_files(roots):
    for root in roots:
        if not root.exists():
            continue
        yield from root.rglob("*.kt")

# Match a MetroBorderButton(… ) call whose modifier arg includes fillMaxWidth(
# across nested parentheses.
border_call_re = re.compile(r"MetroBorderButton\s*\(", re.MULTILINE)
fill_max_re = re.compile(r"fillMaxWidth\s*\(")

def extract_call(src, start):
    """Return text from start through matching close-paren, or None."""
    i = src.find("(", start)
    if i < 0:
        return None
    depth = 0
    for j in range(i, len(src)):
        c = src[j]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return src[start : j + 1]
    return None

def line_no(src: str, index: int) -> int:
    return src.count("\n", 0, index) + 1

for path in kt_files(roots):
    text = path.read_text(encoding="utf-8")
    rel = path.relative_to(app_dir) if path.is_relative_to(app_dir) else path

    for m in border_call_re.finditer(text):
        call = extract_call(text, m.start())
        if call is None:
            continue
        # Only the modifier= argument matters; fillMaxWidth inside onClick lambdas is rare
        # but still wrong if applied to the button itself. Scan the whole call — WP buttons
        # never stretch.
        if "modifier" in call and fill_max_re.search(call):
            # Ignore fillMaxWidth that is clearly not on the button modifier: if it appears
            # only inside a nested lambda after onClick={, skip when modifier= has no fill.
            mod = re.search(r"modifier\s*=\s*([^,\n]+(?:,)?)", call, re.DOTALL)
            # Broader: take from modifier= to next top-level comma/paren at depth of call args
            mod_m = re.search(r"modifier\s*=", call)
            if mod_m:
                # slice from modifier= to end of call; if fillMaxWidth appears before a
                # sibling named arg at the same nesting as modifier's value start…
                after = call[mod_m.end() :]
                # Walk until we leave the modifier expression (comma/paren at depth 0 of after
                # starting after '='). Simpler heuristic: fillMaxWidth before next `\n    <ident>=`
                # or end, ignoring nested parens.
                depth = 0
                end = len(after)
                for idx, ch in enumerate(after):
                    if ch == "(":
                        depth += 1
                    elif ch == ")":
                        if depth == 0:
                            end = idx
                            break
                        depth -= 1
                    elif ch == "," and depth == 0:
                        end = idx
                        break
                mod_expr = after[:end]
                if fill_max_re.search(mod_expr):
                    failures.append(
                        f"{rel}:{line_no(text, m.start())}: "
                        "MetroBorderButton must be flush-left and hug its label — "
                        "do not use fillMaxWidth() on its modifier "
                        "(METRO-UX-LANGUAGE.md §6.3)"
                    )

    # MetroAppTitle must not sit in a Column that applies horizontal padding on the Column itself.
    if "MetroAppTitle" not in text:
        continue
    # Find Column( ... ) blocks that include padding(horizontal and MetroAppTitle before the
    # Column's trailing content closes. Heuristic: between Column( and MetroAppTitle, if we
    # see padding(horizontal on a modifier= line and no intervening composable call that
    # closes the Column opener inappropriately.
    for col in re.finditer(r"\bColumn\s*\(", text):
        call = extract_call(text, col.start())
        if call is None:
            continue
        # Column(modifier=..., content=) or Column(modifier) { } trailing lambda is OUTSIDE
        # the paren call in Kotlin — so extract_call only gets the paren args.
        # Look ahead from Column( for a trailing lambda { ... } that contains MetroAppTitle.
        after_parens = text[col.start() + len(call) :]
        # Skip whitespace / comments to '{'
        rest = after_parens.lstrip()
        if not rest.startswith("{"):
            continue
        # Extract balanced braces for the trailing lambda
        depth = 0
        body = None
        for idx, ch in enumerate(rest):
            if ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
                if depth == 0:
                    body = rest[: idx + 1]
                    break
        if body is None or "MetroAppTitle" not in body:
            continue
        # Horizontal padding on the Column modifier (paren args), not on children.
        if re.search(r"padding\s*\(\s*horizontal\s*=", call):
            failures.append(
                f"{rel}:{line_no(text, col.start())}: "
                "Column with padding(horizontal) must not host MetroAppTitle — "
                "MetroAppTitle already owns the 12dp start inset; pad siblings per-child "
                "(METRO-UX-LANGUAGE.md §6.3 / §12)"
            )

if failures:
    print("FAIL  metro alignment lint", file=sys.stderr)
    for f in failures:
        print(f"  {f}", file=sys.stderr)
    sys.exit(1)

print("PASS  metro alignment lint")
sys.exit(0)
PY

echo "PASS  lint-metro"
exit 0
