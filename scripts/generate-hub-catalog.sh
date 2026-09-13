#!/usr/bin/env bash
# Generate apps/hub catalog sidecar for a GitHub release.
# Prefers aapt badging of built APKs for per-app versionName / versionCode.
#
# Usage:
#   ./scripts/generate-hub-catalog.sh
#   ./scripts/generate-hub-catalog.sh --upload alpha-8
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${ROOT}/deploy/hub-catalog.json"
APK_DIR="${ROOT}/deploy/apks"
UPLOAD_TAG=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --upload)
      UPLOAD_TAG="${2:-}"
      shift 2
      ;;
    -h|--help)
      sed -n '2,12p' "$0"
      exit 0
      ;;
    *)
      echo "Unknown arg: $1" >&2
      exit 2
      ;;
  esac
done

AAPT="$(ls "${ANDROID_HOME:-$HOME/Library/Android/sdk}"/build-tools/*/aapt 2>/dev/null | tail -1 || true)"
if [[ -z "$AAPT" ]]; then
  echo "ERROR: aapt not found (set ANDROID_HOME)" >&2
  exit 1
fi

mkdir -p "$(dirname "$OUT")"
TAG_HINT="$(cd "$ROOT" && git describe --tags --abbrev=0 2>/dev/null || echo "")"

python3 - <<PY
import json, os, re, subprocess, glob

root = r"""$ROOT"""
apk_dir = r"""$APK_DIR"""
aapt = r"""$AAPT"""
out = r"""$OUT"""
tag_hint = r"""$TAG_HINT"""

apps = []
for apk in sorted(glob.glob(os.path.join(apk_dir, "*-debug.apk"))):
    name = os.path.basename(apk)
    badging = subprocess.check_output([aapt, "dump", "badging", apk], text=True, stderr=subprocess.DEVNULL)
    pkg = re.search(r"package: name='([^']+)'", badging)
    ver_name = re.search(r"versionName='([^']*)'", badging)
    ver_code = re.search(r"versionCode='(\d+)'", badging)
    label = re.search(r"application-label:'([^']*)'", badging)
    app_id = name.removesuffix("-debug.apk").removesuffix("-release.apk")
    apps.append({
        "apk": name,
        "id": app_id,
        "packageName": pkg.group(1) if pkg else f"com.metro.{app_id}",
        "versionName": ver_name.group(1) if ver_name else None,
        "versionCode": int(ver_code.group(1)) if ver_code else None,
        "label": label.group(1) if label else app_id,
        # Optional: set after uploading <id>-icon.png to the same release.
        "iconUrl": None,
    })

catalog = {"tag": tag_hint or None, "apps": apps}
with open(out, "w", encoding="utf-8") as f:
    json.dump(catalog, f, indent=2)
    f.write("\n")
print(f"Wrote {out} ({len(apps)} apps)")
for app in apps:
    print(f"  {app['apk']}: {app.get('versionName')} ({app.get('versionCode')})")
PY

if [[ -n "$UPLOAD_TAG" ]]; then
  echo "==> uploading $OUT to release $UPLOAD_TAG"
  gh release upload "$UPLOAD_TAG" "$OUT" --clobber --repo god-s-perfect-idiot/metro-os
  echo "OK  hub-catalog.json attached to $UPLOAD_TAG"
fi
