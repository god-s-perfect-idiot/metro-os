#!/usr/bin/env bash
# Sync Hub catalog collections into Firestore.
#   first-party  — metro-os suite APKs from a GitHub release (local aapt optional)
#   second-party — curated external Metro apps; metadata from GitHub Releases API only
# Requires: firebase/service-account.json, network. gh optional for first-party.
#
# Hard rule (first-party): every release APK must resolve a Hub logo —
#   logoXml (vector under toolkits/metro-ui-android/.../drawable + glyphFiles map)
#   or logoPngBase64 (legacy People PNG only). Sync refuses to upsert when missing.
#
# Usage:
#   ./scripts/sync-hub-firestore.sh
#   ./scripts/sync-hub-firestore.sh --tag alpha-8
#   ./scripts/sync-hub-firestore.sh --party second
#   ./scripts/sync-hub-firestore.sh --party all --tag alpha-8
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SA="${FIREBASE_SERVICE_ACCOUNT:-$ROOT/firebase/service-account.json}"
TAG=""
RELEASE_REPO="god-s-perfect-idiot/metro-os"
PARTY="first"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tag)
      TAG="${2:-}"
      shift 2
      ;;
    --party)
      PARTY="${2:-}"
      shift 2
      ;;
    -h|--help)
      sed -n '2,14p' "$0"
      exit 0
      ;;
    *)
      echo "Unknown arg: $1" >&2
      exit 2
      ;;
  esac
done

case "$PARTY" in
  first|second|all) ;;
  *)
    echo "ERROR: --party must be first|second|all (got: $PARTY)" >&2
    exit 2
    ;;
esac

if [[ ! -f "$SA" ]]; then
  echo "ERROR: missing service account at $SA" >&2
  exit 1
fi

export ROOT SA TAG RELEASE_REPO PARTY
export APK_DIR="$ROOT/deploy/apks"
export AAPT="$(ls "${ANDROID_HOME:-$HOME/Library/Android/sdk}"/build-tools/*/aapt 2>/dev/null | tail -1 || true)"

sync_first_party() {
  if [[ -z "$TAG" ]]; then
    TAG="$(gh release view --repo "$RELEASE_REPO" --json tagName -q .tagName 2>/dev/null || true)"
  fi
  export TAG

  # APK asset names attached to the release — source of truth for first-party.
  # Prefer live GitHub assets; fall back to local deploy/apks when gh is unavailable.
  RELEASE_APKS="$(gh release view "${TAG}" --repo "$RELEASE_REPO" --json assets \
    --jq '[.assets[].name | select(endswith(".apk"))] | join(" ")' 2>/dev/null || true)"
  if [[ -z "${RELEASE_APKS// /}" && -d "$ROOT/deploy/apks" ]]; then
    RELEASE_APKS="$(find "$ROOT/deploy/apks" -maxdepth 1 -name '*.apk' -exec basename {} \; | tr '\n' ' ')"
    echo "WARN  gh release assets unavailable — using local deploy/apks/"
  fi
  if [[ -z "${RELEASE_APKS// /}" ]]; then
    echo "ERROR: no .apk assets found on release ${TAG:-latest} (and no deploy/apks/*.apk)" >&2
    exit 1
  fi
  export RELEASE_APKS

  cd "$ROOT"
  node --input-type=module <<'NODE'
import { readFileSync, existsSync, statSync } from "node:fs";
import { join } from "node:path";
import { execFileSync } from "node:child_process";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
let admin;
try {
  admin = require("firebase-admin");
} catch {
  console.error("Installing firebase-admin locally under firebase/ …");
  execFileSync("npm", ["install", "--prefix", "firebase", "firebase-admin@13"], {
    stdio: "inherit",
  });
  admin = require(join(process.env.ROOT, "firebase/node_modules/firebase-admin"));
}

const root = process.env.ROOT;
const saPath = process.env.SA;
const tag = process.env.TAG || "";
const apkDir = process.env.APK_DIR;
const aapt = process.env.AAPT || "";
const releaseRepo = process.env.RELEASE_REPO;
const releaseApks = (process.env.RELEASE_APKS || "")
  .split(/\s+/)
  .map((s) => s.trim())
  .filter((s) => s.endsWith(".apk"));
const releaseUrl = tag
  ? `https://github.com/${releaseRepo}/releases/tag/${tag}`
  : `https://github.com/${releaseRepo}/releases/latest`;
const githubRepo = `https://github.com/${releaseRepo}`;

if (releaseApks.length === 0) {
  console.error("ERROR: RELEASE_APKS empty — refusing to sync");
  process.exit(1);
}

const shell = new Set([
  "launcher", "statusbar", "notifications", "navbar", "volume", "lockscreen", "keyboard",
]);
const core = new Set([
  "browser", "notes", "music", "calculator", "clock", "files", "settings", "store", "hub",
  "photos", "calendar", "mail", "messaging", "people", "dialer", "widgets", "conversations",
]);

const descriptions = {
  launcher: "Start screen, live tiles, and app list for metro-os.",
  statusbar: "System tray overlay with clock, signal, and battery.",
  notifications: "WP8.1-style toast banners and notification surface.",
  navbar: "Soft keys: Back, Start, and Search.",
  volume: "Hardware rocker volume HUD for ringer and media.",
  lockscreen: "WP8.1 lock screen overlay above the system keyguard.",
  keyboard: "Metro SIP / Word Flow–style touch keyboard.",
  browser: "IE Mobile–style browser with tabs and favorites.",
  notes: "OneNote-style notebooks, sections, and pages.",
  music: "Xbox Music–style player with local and streaming library.",
  photos: "Photo hub with date and album pivots.",
  calendar: "Agenda, day, and month calendar views.",
  mail: "Linked inboxes and conversation mail.",
  messaging: "SMS and MMS message threads.",
  people: "Contacts hub and people directory.",
  dialer: "Phone dialer, call history, and in-call UI.",
  store: "App discovery shell for the metro-os suite.",
  settings: "System settings in WP8.1 hierarchy.",
  calculator: "Portrait calculator with scientific landscape mode.",
  clock: "Alarms, world clock, timer, and stopwatch.",
  files: "File explorer with pivot filters.",
  hub: "About metro-os and suite app downloads.",
  widgets: "Homescreen widget catalog with Start-style live tiles.",
  conversations: "Reply inbox for third-party shade chats (RemoteInput).",
};

const glyphFiles = {
  browser: "metro_app_browser.xml",
  notes: "metro_app_notes.xml",
  music: "metro_app_music.xml",
  settings: "metro_app_settings.xml",
  store: "metro_app_store.xml",
  photos: "metro_app_photos.xml",
  calendar: "metro_app_calendar.xml",
  mail: "metro_app_mail.xml",
  messaging: "metro_app_messaging.xml",
  people: null,
  dialer: "metro_app_phone.xml",
  calculator: "metro_app_calculator.xml",
  clock: "metro_app_clock.xml",
  files: "metro_app_files.xml",
  lockscreen: "metro_app_lockscreen.xml",
  volume: "metro_app_volume.xml",
  statusbar: "metro_app_statusbar.xml",
  navbar: "metro_app_navbar.xml",
  notifications: "metro_app_notifications.xml",
  launcher: "metro_app_launcher.xml",
  keyboard: "metro_app_keyboard.xml",
  hub: "metro_app_hub.xml",
  widgets: "metro_app_widgets.xml",
  conversations: "metro_app_conversations.xml",
};

/** Catalog brand fills (MetroAppRegistry.brandHex) when launcher bg is missing. */
const brandHexFallback = {
  browser: "#1BA1E2",
  notes: "#A200FF",
  music: "#E3008C",
  settings: "#F09609",
  store: "#7CB342",
  photos: "#EB3C00",
  calendar: "#0078D7",
  mail: "#0078D7",
  messaging: "#0078D7",
  people: "#D34829",
  dialer: "#0078D7",
  calculator: "#007500",
  clock: "#0078D7",
  files: "#0078D7",
  hub: "#1BA1E2",
  widgets: "#1BA1E2",
  conversations: "#00ABA9",
};

const DEFAULT_BACKGROUND_COLOR = "#1BA1E2";

function assetId(apkName) {
  return apkName
    .replace(/\.apk$/i, "")
    .replace(/-debug$/i, "")
    .replace(/-release$/i, "")
    .toLowerCase();
}

function titleCase(id) {
  return id.split(/[-_]/).filter(Boolean).map((w) => w[0].toUpperCase() + w.slice(1)).join(" ");
}

function appType(id) {
  if (shell.has(id)) return "shell";
  if (core.has(id)) return "core";
  return "core";
}

function readLogoXml(id) {
  const file = glyphFiles[id];
  if (!file) {
    const png = join(root, "toolkits/metro-ui-android/src/main/res/drawable-nodpi/metro_app_people.png");
    if (id === "people" && existsSync(png)) {
      return { logoXml: null, logoPngBase64: readFileSync(png).toString("base64") };
    }
    return { logoXml: null, logoPngBase64: null };
  }
  const path = join(root, "toolkits/metro-ui-android/src/main/res/drawable", file);
  if (!existsSync(path)) return { logoXml: null, logoPngBase64: null };
  return { logoXml: readFileSync(path, "utf8"), logoPngBase64: null };
}

/** New suite apps must ship a Hub logo — vector logoXml (preferred) or People PNG. */
function assertHubLogo(id, logos) {
  if (logos.logoXml || logos.logoPngBase64) return;
  const hint = [
    `ERROR: first-party/${id} has no Hub logo (logoXml / logoPngBase64).`,
    `  New apps require logoXml before sync:`,
    `    1. Add toolkits/metro-ui-android/src/main/res/drawable/metro_app_${id}.xml`,
    `    2. Register it in glyphFiles + MetroAppGlyphs (package → drawable)`,
    `    3. Add descriptions / brandHexFallback / core|shell for ${id}`,
    `  Then re-run: ./scripts/sync-hub-firestore.sh --tag <tag>`,
  ].join("\n");
  throw new Error(hint);
}

/** Prefer each app's `ic_launcher_background`, then registry brandHex, then default accent. */
function readBackgroundColor(id) {
  const launcherBg = join(root, `apps/${id}/app/src/main/res/values/ic_launcher_background.xml`);
  if (existsSync(launcherBg)) {
    const text = readFileSync(launcherBg, "utf8");
    const m = text.match(/#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})\b/);
    if (m) {
      const hex = `#${m[1].slice(-6).toUpperCase()}`;
      // Adaptive icon fills are often pure white — unusable as a Hub catalog tile.
      if (!isNearWhite(hex)) return hex;
    }
  }
  return brandHexFallback[id] || DEFAULT_BACKGROUND_COLOR;
}

function isNearWhite(hex) {
  const n = parseInt(hex.slice(1), 16);
  const r = (n >> 16) & 0xff;
  const g = (n >> 8) & 0xff;
  const b = n & 0xff;
  return r >= 0xf0 && g >= 0xf0 && b >= 0xf0;
}

function badging(apkPath) {
  if (!aapt || !existsSync(apkPath)) return {};
  try {
    const out = execFileSync(aapt, ["dump", "badging", apkPath], { encoding: "utf8" });
    const pkg = out.match(/package: name='([^']+)'/);
    const vn = out.match(/versionName='([^']*)'/);
    const vc = out.match(/versionCode='(\d+)'/);
    return {
      packageName: pkg?.[1],
      versionName: vn?.[1] || null,
      versionCode: vc ? Number(vc[1]) : null,
    };
  } catch {
    return {};
  }
}

function readVersionFromGradle(id) {
  if (id === "keyboard") {
    const p = join(root, "apps/keyboard/gradle.properties");
    if (!existsSync(p)) return {};
    const text = readFileSync(p, "utf8");
    const m = text.match(/^(?:projectVersionName|VERSION_NAME|versionName)\s*=\s*(\S+)/m);
    return { versionName: m?.[1]?.split("-")[0] || null };
  }
  const p = join(root, `apps/${id}/app/build.gradle.kts`);
  if (!existsSync(p)) return {};
  const text = readFileSync(p, "utf8");
  const vn = text.match(/versionName\s*=\s*"([^"]+)"/);
  const vc = text.match(/versionCode\s*=\s*(\d+)/);
  return {
    versionName: vn?.[1] || null,
    versionCode: vc ? Number(vc[1]) : null,
  };
}

const credential = admin.credential.cert(JSON.parse(readFileSync(saPath, "utf8")));
if (!admin.apps.length) {
  admin.initializeApp({ credential });
}
const db = admin.firestore();
const col = db.collection("first-party");

/** Only apps that ship an APK on the GitHub release. */
const releaseIds = new Set(releaseApks.map(assetId));
console.log(`Release ${tag}: ${releaseApks.length} APK(s) → ${[...releaseIds].join(", ")}`);

const batch = db.batch();
let upserted = 0;
const logoFailures = [];

for (const apkName of releaseApks.sort()) {
  const id = assetId(apkName);
  const logos = readLogoXml(id);
  if (!logos.logoXml && !logos.logoPngBase64) {
    logoFailures.push(id);
  }
}

if (logoFailures.length > 0) {
  console.error(
    `ERROR: ${logoFailures.length} first-party app(s) missing Hub logoXml (or logoPngBase64): ${logoFailures.join(", ")}`,
  );
  for (const id of logoFailures) {
    try {
      assertHubLogo(id, { logoXml: null, logoPngBase64: null });
    } catch (e) {
      console.error(e.message);
    }
  }
  process.exit(1);
}

for (const apkName of releaseApks.sort()) {
  const id = assetId(apkName);
  const apkPath = join(apkDir, apkName);
  const fromApk = badging(apkPath);
  const fromGradle = readVersionFromGradle(id);
  const logos = readLogoXml(id);
  assertHubLogo(id, logos);
  const backgroundColor = readBackgroundColor(id);
  const sizeBytes = existsSync(apkPath) ? statSync(apkPath).size : null;
  const apkUrl = tag
    ? `https://github.com/${releaseRepo}/releases/download/${tag}/${apkName}`
    : null;

  const doc = {
    id,
    name: titleCase(id),
    packageName: fromApk.packageName || `com.metro.${id}`,
    description: descriptions[id] || "metro-os suite app.",
    versionName: fromApk.versionName || fromGradle.versionName || null,
    versionCode: fromApk.versionCode || fromGradle.versionCode || null,
    type: appType(id),
    creator: "Entropy",
    logoXml: logos.logoXml,
    logoPngBase64: logos.logoPngBase64,
    backgroundColor,
    apkName,
    apkUrl,
    releaseUrl,
    githubRepo,
    sizeBytes,
    party: "first",
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  // Omit nulls so a tag-less / APK-less run cannot wipe apkUrl and friends.
  const patch = Object.fromEntries(
    Object.entries(doc).filter(([, v]) => v !== null && v !== undefined),
  );

  batch.set(col.doc(id), patch, { merge: true });
  upserted += 1;
  const logoKind = logos.logoXml ? "logoXml" : "logoPngBase64";
  console.log(`  upsert first-party/${id}  ${doc.versionName || "?"}  ${doc.type}  ${backgroundColor}  ${logoKind}`);
}

await batch.commit();

// Remove docs that are not on the latest release (stubs / never-shipped apps).
const existing = await col.get();
let deleted = 0;
const deleteBatch = db.batch();
for (const doc of existing.docs) {
  if (doc.id.startsWith("_")) continue;
  if (releaseIds.has(doc.id)) continue;
  deleteBatch.delete(doc.ref);
  deleted += 1;
  console.log(`  delete first-party/${doc.id}`);
}
if (deleted > 0) {
  await deleteBatch.commit();
}

console.log(`OK  first-party: upserted ${upserted}, deleted ${deleted} (tag=${tag})`);

for (const name of ["second-party", "third-party", "explore"]) {
  const meta = db.collection(name).doc("_meta");
  await meta.set(
    {
      description:
        name === "explore"
          ? "Featured Hub entries (future). Docs reference first/second/third-party apps."
          : `${name} Hub catalog. Same fields as first-party (incl. backgroundColor); creator + githubRepo vary per app.`,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true },
  );
}
console.log("OK  ensured second-party / third-party / explore collections");
NODE
}

sync_second_party() {
  cd "$ROOT"
  node --input-type=module <<'NODE'
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { execFileSync } from "node:child_process";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
let admin;
try {
  admin = require("firebase-admin");
} catch {
  console.error("Installing firebase-admin locally under firebase/ …");
  execFileSync("npm", ["install", "--prefix", "firebase", "firebase-admin@13"], {
    stdio: "inherit",
  });
  admin = require(join(process.env.ROOT, "firebase/node_modules/firebase-admin"));
}

const saPath = process.env.SA;

/**
 * Curated second-party Hub catalog.
 * Release APK + size come from GitHub Releases API (no local APK download).
 * packageName / creator / description are pinned from each project's source.
 */
const CATALOG = [
  {
    id: "metro-wordle",
    name: "Metro Wordle",
    packageName: "com.metrowordle.app",
    description: "WP8.1-style Wordle daily puzzle with Metro keyboard and tile flips.",
    creator: "Entropy",
    type: "core",
    backgroundColor: "#000000",
    githubRepo: "god-s-perfect-idiot/metro-wordle",
    // Android vector XML (4×4 window grid) — Hub tints to tile content color.
    logoXml: `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="200dp"
    android:height="200dp"
    android:viewportWidth="512"
    android:viewportHeight="512">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M365.7 102.4v146.3H512V102.4H365.7zm109.7 109.7h-73.1V139h73.1v73.1zM0 431.6h146.3V285.3H0v146.3zm36.6-109.7h73.1V395H36.6v-73.1zM0 248.7h146.3V102.4H0v146.3zM36.6 139h73.1v73.1H36.6V139zm146.3 109.7h146.3V102.4H182.9v146.3zM219.4 139h73.1v73.1h-73.1V139zm-36.5 292.6h146.3V285.3H182.9v146.3zm36.5-109.7h73.1V395h-73.1v-73.1zm146.3 109.7H512V285.3H365.7v146.3zm36.6-109.7h73.1V395h-73.1v-73.1z"/>
</vector>`,
    preferApk: (name) => name.toLowerCase().endsWith(".apk"),
    versionNameFromTag: (tag) => tag.replace(/^v/i, ""),
  },
  {
    id: "metro-notes",
    name: "Metro Notes",
    packageName: "com.metronotes.app",
    description: "Capacitor Metro notes with notebooks, sketches, and settings shell.",
    creator: "Entropy",
    type: "core",
    backgroundColor: "#5D5D5D",
    githubRepo: "god-s-perfect-idiot/metro-notes",
    // Android vector XML (document / folded-corner Metro SVG) — Hub tints to tile content color.
    logoXml: `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="200dp"
    android:height="200dp"
    android:viewportWidth="512"
    android:viewportHeight="512">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M320 0v128h128L320 0zm-21.3 0H64v512h384V149.3H298.7V0zm-192 42.7H256v106.7H106.7V42.7zM256 405.3H106.7v-42.7H256v42.7zM405.3 320H106.7v-42.7h298.7V320zm0-128v42.7H106.7V192h298.6z"/>
</vector>`,
    preferApk: (name) => name.toLowerCase().endsWith(".apk"),
    versionNameFromTag: (tag) => tag,
  },
  {
    id: "metro-browser-native",
    name: "Metro Browser",
    packageName: "com.metro.browser",
    description: "IE Mobile–style browser with tabs, address bar, and Metro animations.",
    creator: "Entropy",
    type: "core",
    backgroundColor: "#046AB8",
    githubRepo: "god-s-perfect-idiot/metro-browser-native",
    // Android vector XML (from IE/compass Metro SVG) — Hub tints to tile content color.
    logoXml: `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="200dp"
    android:height="200dp"
    android:viewportWidth="512"
    android:viewportHeight="512">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M256 0C114.6 0 0 114.6 0 256s114.6 256 256 256s256-114.6 256-256S397.4 0 256 0zm0 472.6c-119.6 0-216.6-97-216.6-216.6S136.4 39.4 256 39.4s216.6 97 216.6 216.6s-97 216.6-216.6 216.6zm-137.8-78.8l187.1-88.6l88.6-187.1l-187.1 88.6l-88.6 187.1zm167.3-108.3l-118.2 59.1l59.1-118.2l59.1 59.1z"/>
</vector>`,
    preferApk: (name) => name.toLowerCase().endsWith(".apk"),
    versionNameFromTag: (tag) => (tag === "beta-2" ? "0.2" : tag),
    versionCode: null,
  },
  {
    id: "metro-weather",
    name: "Metro Weather",
    packageName: "com.metro.weather",
    description: "Metro weather hub with live location and animated backgrounds.",
    creator: "Entropy",
    type: "core",
    // Match weather-win8preview.svg tile fill (#00b1de).
    backgroundColor: "#00b1de",
    githubRepo: "god-s-perfect-idiot/Metro-Weather",
    // Android vector XML (sun + cloud from weather-win8preview.svg) — Hub tints to tile content color.
    logoXml: `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="200dp"
    android:height="148dp"
    android:viewportWidth="135"
    android:viewportHeight="100">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 68.26695,36.338831 c -0.561041,-1.112397 -1.876406,-2.969159 -2.883032,-4.069668 -0.49849,-0.545002 -0.90639,-1.020382 -0.90639,-1.056402 0,-0.166733 6.782399,-5.734288 7.322526,-6.01093 0.455319,-0.233199 0.844153,-0.312436 1.522091,-0.310172 2.12026,0.0071 3.608798,1.489751 3.608798,3.59461 0,1.67416 -0.179915,1.895856 -4.430444,5.460179 -2.045615,1.715359 -3.752563,3.118834 -3.793228,3.118834 -0.04094,0 -0.23882,-0.326904 -0.440289,-0.726449 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 54.445626,25.889265 c -0.989869,-0.231428 -2.004039,-0.346694 -3.563071,-0.404962 l -2.179336,-0.08144 0.0017,-0.360896 c 0.0012,-0.19849 0.123703,-2.150311 0.272162,-4.33738 0.308358,-4.542656 0.416441,-4.973773 1.482188,-5.911873 1.429138,-1.257977 3.308086,-1.273784 4.749543,-0.03998 0.810205,0.693512 1.23905,1.660144 1.243196,2.802201 0.0051,1.362992 -0.424715,8.203189 -0.530495,8.445455 -0.08173,0.18713 -0.25722,0.173916 -1.476435,-0.111127 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 31.259985,29.243012 c -3.721362,-3.557452 -4.004303,-3.942531 -4.004303,-5.449997 0,-2.113455 1.4873,-3.60294 3.597661,-3.60294 1.61183,0 1.45496,-0.120554 7.452251,5.726747 1.094922,1.067549 1.90902,1.962762 1.81378,1.99451 -0.574882,0.191624 -3.255389,2.175213 -4.337738,3.209936 l -1.278378,1.222146 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 24.142338,48.399997 c -2.535415,-0.234948 -4.51654,-0.48312 -4.844713,-0.606891 -2.324293,-0.876614 -3.046621,-3.889198 -1.400133,-5.839485 0.57732,-0.683853 1.645946,-1.151693 2.639775,-1.155682 0.746339,-0.003 9.083423,0.691795 9.177883,0.764863 0.02078,0.01606 -0.06904,0.614196 -0.199953,1.328983 -0.160765,0.87819 -0.240361,2.051168 -0.245375,3.617327 l -0.0068,2.317717 -0.415286,-0.01471 c -0.228391,-0.0081 -2.345554,-0.193553 -4.704774,-0.412174 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 25.764017,69.917677 c -1.285111,-0.640408 -1.959567,-1.755603 -1.964575,-3.248394 -0.0051,-1.508999 0.217502,-1.811636 4.416191,-6.005475 2.053756,-2.051377 3.757288,-3.701453 3.785649,-3.666835 0.02846,0.03461 0.451027,0.652712 0.939289,1.373544 0.488256,0.720832 1.510415,1.95043 2.271461,2.73244 l 1.3837,1.42184 -3.528464,3.598155 c -3.921265,3.99872 -4.119036,4.143137 -5.680641,4.148368 -0.701412,0.0024 -1.068963,-0.07778 -1.62261,-0.353624 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 82.187644,35.751334 c -7.374562,0 -13.352732,5.978181 -13.352732,13.352738 0,0.690227 0.01983,1.367345 0.05948,2.029351 -1.377404,-0.167967 -2.861676,-0.231357 -4.41565,-0.231357 -4.710012,0 -8.85042,2.440889 -11.227544,6.124411 -0.669243,-0.113277 -1.354308,-0.175169 -2.055786,-0.175169 -6.763194,0 -12.248822,5.482325 -12.248822,12.245522 0,6.763192 5.241051,12.523149 12.004244,12.523149 0.133519,0 0.267269,-0.0022 0.399905,-0.0064 v 0.01663 h 61.839051 l -0.003,-0.01024 a 11.484806,11.484806 0 0 0 11.34313,-11.485343 11.484806,11.484806 0 0 0 -7.37044,-10.718548 c 0.25002,-1.021191 0.38337,-2.087889 0.38337,-3.18615 0,-7.374555 -5.97819,-13.352737 -13.35274,-13.352737 -3.72634,0 -6.883278,0.925412 -9.095734,2.792838 -1.515228,-5.710633 -6.719081,-9.918705 -12.906514,-9.918705 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M 37.42352,58.074744 C 35.508848,56.036193 33.914042,52.93195 33.327177,50.101356 32.490912,46.06771 32.986363,42.184041 34.79867,38.566815 c 0.927849,-1.851912 1.904249,-3.160026 3.534929,-4.735866 6.643705,-6.420254 17.181126,-6.276698 23.731301,0.323305 1.502007,1.513426 2.808459,3.446181 3.604937,5.333149 l 0.450266,1.06677 -0.443999,0.99476 c -0.518509,1.16162 -0.946191,2.436282 -1.186827,3.537128 -0.09408,0.430509 -0.183816,0.83104 -0.199422,0.890068 -0.01663,0.06364 -0.44451,0.134229 -1.05113,0.173448 -4.298762,0.277924 -8.245342,2.051145 -11.469815,5.153447 l -0.782741,0.753091 -1.02735,0.05494 c -3.107389,0.166164 -6.358616,1.38886 -9.050493,3.403665 -0.821949,0.615202 -2.495225,2.208876 -2.841996,2.706796 -0.08427,0.121079 -0.185927,0.22015 -0.22576,0.22015 -0.03979,0 -0.227518,-0.165108 -0.417052,-0.36691 z"/>
</vector>`,
    preferApk: (name) => name.toLowerCase().endsWith(".apk"),
    versionNameFromTag: (tag) => tag,
  },
  {
    id: "metro-weather-alexthew1",
    name: "Metro Weather",
    packageName: "com.metroweather.app",
    description: "WP8.1 Bing Weather–style panorama with today, daily, hourly, and radar maps.",
    creator: "alexthew1",
    type: "core",
    // Match weather-win8.svg tile fill (#2671ec).
    backgroundColor: "#2671ec",
    githubRepo: "alexthew1/Metro-Weather",
    // Android vector XML (sun from weather-win8.svg; scale baked into path coords) — Hub tints to tile content color.
    logoXml: `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="200dp"
    android:height="196dp"
    android:viewportWidth="102"
    android:viewportHeight="100">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M33.269,50.362a18.7934,18.7934 0 1,0 37.5869,0a18.7934,18.7934 0 1,0 -37.5869,0"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 53.3203,28.1956 c -0.5402,-0.0397 -1.4979,-0.048 -2.1281,-0.0184 l -1.1459,0.0538 0.0041,-5.2755 c 0.0031,-4.0481 0.032,-5.3537 0.1242,-5.6114 0.1955,-0.547 0.6054,-0.9996 1.1438,-1.263 1.033,-0.5054 2.2782,-0.1409 2.8882,0.8455 l 0.2831,0.4578 0.0263,5.4564 c 0.0211,4.3924 0.0029,5.4535 -0.0935,5.4417 -0.0659,-0.0081 -0.5618,-0.0472 -1.102,-0.0869 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 41.084,27.3263 c -1.5517,-3.776 -1.4925,-3.5862 -1.2866,-4.1255 0.4248,-1.1123 1.9443,-1.2081 2.4758,-0.1561 0.0949,0.1878 0.6664,1.5411 1.27,3.0074 0.6037,1.4663 1.1519,2.7978 1.2184,2.9589 l 0.1208,0.2929 -1.1833,0.4843 c -0.6508,0.2663 -1.2254,0.5029 -1.2768,0.5257 -0.0514,0.0228 -0.6537,-1.3216 -1.3383,-2.9876 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 31.1775,32.3854 c -2.4456,-2.4515 -3.8199,-3.8979 -3.9475,-4.1547 -0.2821,-0.5677 -0.2652,-1.3477 0.0427,-1.9656 0.5659,-1.1357 1.9922,-1.5651 3.0686,-0.9239 0.2134,0.1271 2.0607,1.9071 4.1051,3.9555 l 3.7171,3.7244 -0.5709,0.4677 c -0.314,0.2572 -1.0417,0.9604 -1.6171,1.5625 l -1.0462,1.0948 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 27.4553,42.0476 c -1.8522,-0.7661 -3.5054,-1.4654 -3.6738,-1.554 -1.1639,-0.6121 -0.6929,-2.5596 0.6194,-2.561 0.1905,-0.0002 1.7105,0.5733 3.8821,1.4648 1.9629,0.8058 3.5838,1.4766 3.6018,1.4906 0.0181,0.014 -0.1348,0.4387 -0.3396,0.9437 -0.2049,0.505 -0.4229,1.076 -0.4845,1.269 -0.0616,0.1929 -0.1403,0.3484 -0.1749,0.3454 -0.0346,-0.003 -1.5783,-0.6322 -3.4305,-1.3983 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 19.1502,52.7972 c -1.7656,-0.4158 -2.3807,-2.5702 -1.0845,-3.7986 0.6919,-0.6557 0.4728,-0.6332 6.4542,-0.6631 l 5.39,-0.0269 -0.0428,1.4769 c -0.0235,0.8123 -0.0055,1.8452 0.0401,2.2954 l 0.0829,0.8185 -5.243,-0.0094 c -2.8837,-0.0052 -5.4022,-0.0469 -5.5968,-0.0927 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 23.8742,63.3143 c -0.8655,-0.4266 -1.0126,-1.5783 -0.2865,-2.2438 0.1905,-0.1746 7.4573,-3.2587 7.5204,-3.1917 0.1186,0.1258 1.0487,2.4686 0.9978,2.5134 -0.2024,0.1785 -7.4086,3.0784 -7.6441,3.0761 -0.1543,-0.0016 -0.4188,-0.0708 -0.5876,-0.1541 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 28.4705,75.2969 c -1.2025,-0.4438 -1.7956,-1.9685 -1.2088,-3.1072 0.1591,-0.3087 1.3324,-1.5434 3.9055,-4.1098 l 3.6781,-3.6686 0.5504,0.6485 c 0.5681,0.6694 1.4762,1.5669 2.2059,2.1804 l 0.4177,0.3511 -3.645,3.6514 c -2.0047,2.0083 -3.8239,3.758 -4.0425,3.8881 -0.5002,0.2979 -1.3086,0.3699 -1.8613,0.1659 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 40.1305,79.4546 c -0.6254,-0.2487 -1.0158,-0.9727 -0.8451,-1.5676 0.0829,-0.2891 2.988,-7.4056 3.0559,-7.4859 0.0176,-0.0208 0.3166,0.0932 0.6646,0.2533 0.3479,0.1601 0.9062,0.3952 1.2407,0.5225 0.3789,0.1442 0.5968,0.2779 0.5781,0.3548 -0.0634,0.2611 -2.9593,7.2441 -3.089,7.4486 -0.2898,0.4568 -1.0696,0.6873 -1.6051,0.4744 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 51.7294,84.9031 c -0.7119,-0.2331 -1.1868,-0.6546 -1.4885,-1.3212 -0.1845,-0.4075 -0.1906,-0.5909 -0.1924,-5.753 l -0.0019,-5.332 h 2.2449 2.2449 l -0.0021,5.332 c -0.0021,5.1618 -0.0082,5.3455 -0.1926,5.753 -0.2359,0.5211 -0.5834,0.8957 -1.0567,1.1391 -0.4033,0.2074 -1.1952,0.3001 -1.5555,0.1821 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 62.414,78.3304 c -0.1691,-0.1083 -0.3737,-0.3249 -0.4546,-0.4814 -0.1918,-0.3709 -2.6522,-6.3352 -2.6522,-6.4292 0,-0.0396 0.1052,-0.0984 0.2338,-0.1307 0.1286,-0.0323 0.6816,-0.25 1.2289,-0.4837 0.5473,-0.2338 1.0263,-0.3908 1.0644,-0.349 0.0958,0.105 2.5894,6.1881 2.6691,6.5112 0.0999,0.4047 -0.1922,1.0569 -0.5963,1.3315 -0.4371,0.2971 -1.0578,0.31 -1.4932,0.0313 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 74.4505,75.3144 c -0.186,-0.0665 -0.4596,-0.2092 -0.608,-0.3172 -0.1484,-0.108 -1.9093,-1.8388 -3.9131,-3.8461 l -3.6432,-3.6497 1.1751,-1.1505 c 0.6463,-0.6328 1.3512,-1.3587 1.5664,-1.6132 l 0.3913,-0.4627 3.7437,3.7368 c 2.6197,2.6149 3.8119,3.8692 3.9711,4.178 0.8945,1.7358 -0.8583,3.7769 -2.6832,3.1247 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 75.6115,61.8978 c -1.9656,-0.8114 -3.5996,-1.501 -3.6311,-1.5325 -0.0315,-0.0315 0.0502,-0.2695 0.1816,-0.5288 0.1314,-0.2593 0.3694,-0.8002 0.5289,-1.202 0.1595,-0.4018 0.3023,-0.743 0.3173,-0.7582 0.0427,-0.0432 7.0637,2.8687 7.277,3.0181 0.774,0.5421 0.6981,1.7842 -0.1402,2.2953 -0.1677,0.1022 -0.4522,0.1853 -0.6323,0.1846 -0.207,-0.0008 -1.6419,-0.5439 -3.9012,-1.4765 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 74.2253,50.6076 0.0021,-2.2988 5.3788,0.0268 c 5.2604,0.0262 5.3881,0.0312 5.7997,0.2255 1.775,0.8378 1.775,3.2549 0,4.0928 -0.4117,0.1943 -0.5395,0.1993 -5.8018,0.2256 l -5.3809,0.0269 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 72.9113,42.667 c -0.1359,-0.3661 -0.3697,-0.935 -0.5195,-1.2642 -0.1498,-0.3292 -0.2588,-0.6122 -0.2422,-0.6288 0.1191,-0.1191 6.92,-2.8726 7.1863,-2.9096 0.7014,-0.0974 1.3769,0.4708 1.4624,1.2301 0.0495,0.4394 -0.1023,0.8178 -0.4583,1.1423 -0.1206,0.11 -1.7859,0.8515 -3.7005,1.6478 l -3.4812,1.4479 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 67.7169,34.6556 -1.5526,-1.5724 3.8445,-3.8331 c 2.1145,-2.1082 3.9835,-3.9029 4.1535,-3.9883 0.7364,-0.3698 1.6941,-0.2591 2.397,0.2769 0.7406,0.5648 1.0283,1.8426 0.6061,2.6919 -0.1277,0.257 -1.517,1.7182 -3.9986,4.2058 -2.0916,2.0966 -3.8241,3.8073 -3.8501,3.8017 -0.026,-0.0056 -0.7459,-0.7178 -1.5998,-1.5826 z"/>
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m 60.5443,29.8428 c -0.6315,-0.2695 -1.1709,-0.5128 -1.1988,-0.5406 -0.0553,-0.0553 2.6264,-6.6231 2.8475,-6.9739 0.0748,-0.1187 0.3053,-0.3022 0.5121,-0.4077 0.6313,-0.3221 1.3031,-0.13 1.7801,0.5088 0.3685,0.4936 0.2827,0.8053 -1.2283,4.4622 -0.7754,1.8766 -1.4446,3.4186 -1.4872,3.4266 -0.0425,0.008 -0.594,-0.2059 -1.2255,-0.4754 z"/>
</vector>``,
    preferApk: (name) => name.toLowerCase().endsWith(".apk"),
    versionNameFromTag: (tag) => tag.replace(/^v/i, ""),
  },
  {
    id: "disco-launcher",
    name: "Disco Launcher",
    packageName: "io.github.cherryhoax.discolauncher2",
    description: "Metro-inspired Android launcher (DiscoUI nightly builds).",
    creator: "DiscoUI",
    type: "shell",
    backgroundColor: "#E51400",
    githubRepo: "discoui-org/discolauncher",
    // Remote PNG (disco-ball globe) — Hub draws on catalog tile color.
    iconUrl: "https://raw.githubusercontent.com/cherryhoax/DiscoLauncher/main/www/assets/navbar/b2.png",
    includePrerelease: true,
    preferApk: (name) => {
      const n = name.toLowerCase();
      return n.endsWith(".apk") && n.includes("webview") && n.includes("arm64");
    },
    versionNameFromTag: (tag) => tag,
    versionName: "0.7.0-beta",
    versionCode: 70,
  },
  {
    id: "metro-store",
    name: "Metro Store",
    packageName: "com.aurora.store",
    description: "Metro-skinned Aurora Store client for Play downloads without Google account.",
    creator: "Cyanexani",
    type: "core",
    backgroundColor: "#7CB342",
    githubRepo: "Cyanexani/metrostore",
    // Android vector XML (Windows Store bag SVG) — Hub tints to tile content color.
    logoXml: `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="200dp"
    android:height="200dp"
    android:viewportWidth="381"
    android:viewportHeight="429">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="m152.26758,1.8574219c-1.4636,-0.00115 -2.92835,0.042489 -4.39258,0.1308593 -44.34164,2.6768839 -79.586932,44.9763108 -78.722656,94.4785158 0.07767,4.418903 0.451523,8.813243 1.111328,13.148433L0.78320312,123.68945 0,367.1543 305.31055,428.2168 380.46289,400.81836 378.11523,90.027344 338.97266,82.199219 306.09375,61.845703 266.98047,69.767578C257.35115,38.379406 233.1705,13.203479 201.875,13.160156c7.61889,5.915934 11.97926,11.125119 14.73828,15.847656 22.47568,6.217238 30.99245,24.134175 37.73438,43.320313L118.0625,99.933594c5.27657,-42.023904 31.17477,-69.938946 57.78516,-76.09961l-17.16993,-5.027343c-24.19741,9.921052 -51.97129,42.517965 -50.97656,83.224609l-28.892576,5.85352c-0.619839,-3.97522 -0.97017,-8.007984 -1.041016,-12.064458 -0.76508,-43.838916 30.176032,-81.298968 69.109372,-83.669921 1.36858,-0.08332 2.73765,-0.121749 4.10547,-0.115235 34.70621,0.165183 63.26769,28.821129 67.31055,67.533203l10.82226,-2.214843C224.19951,33.869392 191.6455,1.8879245 152.26758,1.8574219ZM225.85156,169.48633v78.08984h-87.67968v-65.56445zm-90.41992,12.91797v65.17187H66.933594V192.18945ZM66.933594,250.31445H135.43164V316.2168L66.933594,306.09375Zm71.238286,0h87.67968v79.26367l-87.67968,-12.95703z"/>
</vector>`,
    preferApk: (name) => {
      const n = name.toLowerCase();
      return n.endsWith(".apk") && n.includes("universal");
    },
    versionName: "0.8.5-beta",
    versionCode: 77,
  },
  {
    id: "metro-maps",
    name: "Metro Maps",
    packageName: "com.metromap.ultimate",
    description: "HERE Maps–style WP8.1 maps clone with pivots, favorites, and navigation.",
    creator: "alexthew1",
    type: "core",
    backgroundColor: "#0078D7",
    githubRepo: "alexthew1/Metro-Maps",
    // Android vector XML (HERE/maps Metro SVG) — Hub tints to tile content color.
    logoXml: `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="200dp"
    android:height="200dp"
    android:viewportWidth="512"
    android:viewportHeight="512">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M362.7 42.7v74.9c3.2.9 6.3 1.8 9.5 2.7c41 11.7 83.5 23.8 104.3 42.4c24.2 21.7 8.1 50.4-6.2 75.7c-11 19.4-22.3 39.5-22.3 60.2h-21.3c0-26.3 13.3-49.9 25-70.7c15.6-27.7 21-40 10.5-49.3c-17.1-15.4-59-27.3-95.9-37.8c-1.3-0.4-2.4-0.7-3.7-1.1V512L512 469.3V0L362.7 42.7zm128 362.6l-21.3 21.3l-32-32l-32 32l-21.4-21.3l32-32l-32-32l21.3-21.3l32 32l32-32l21.3 21.3l-32 32l32.1 32zm-320-58.7c15.8-8.6 34.5-12.1 56.7-4.7c14.6 4.9 19-0.8 20.8-3.2c8.1-10.7 10.1-18.7 7.8-18.7c7.2 0 14.9 2.2 21.3 0c0 10.7-4 20.9-12.2 31.6c-5.7 7.6-19.2 18.9-44.5 10.5c-19.6-6.5-35.4-1.2-50 10.3v97L341.3 512V133.5c-18.2-5.4-34.8-11-47.4-17.3c-21-10.5-51-6.7-64.1 8.1c-11.3 12.7-4.6 29 3.1 40.4c3.5 5.3 9.1 20.8 12.5 27.2c-7.2 1.4-15.2-3.6-21.3 0c-2.6-4.9-6-11.1-8.9-15.4c-16.4-24.6-16.9-48.8-1.3-66.4c19.6-22.1 59.8-28 89.6-13.1c9.9 4.9 23.2 9.5 37.9 14.1V42.7L170.6 0l0.1 346.6zM256 213.3c23.5 0 42.7 19.1 42.7 42.7s-19.1 42.7-42.7 42.7c-23.5 0-42.7-19.1-42.7-42.7c0-23.5 19.2-42.7 42.7-42.7zM0 512l149.3-42.7v-74.7c-4.1 5-8.3 9.3-12.5 14.8c-9.5 12.6-17 22.5-25.4 26.7c-4.9 2.5-10 3.6-15.1 3.6c-23.7 0-47.4-23.7-61.2-37.6c-17.6-17.6-3.4-39.4 9.2-58.5c9.2-14.1 19.7-30.1 19.7-45v-85.3h21.3v85.3c0 21.3-12.3 40.1-23.2 56.7C50 374 45.6 382.5 50.2 387.1c17.3 17.3 38.8 36.5 51.7 30c3.9-2 11.4-11.8 18-20.5c8.1-10.7 17.8-23.1 29.5-34V0L0 42.7V512zm74.7-384c17.7 0 32 14.3 32 32s-14.3 32-32 32s-32-14.3-32-32s14.3-32 32-32z"/>
</vector>`,
    preferApk: (name) => name.toLowerCase().endsWith(".apk"),
    versionNameFromTag: (tag) => tag.replace(/^v/i, ""),
  },
  {
    id: "metro-keyboard",
    name: "Metro Keyboard",
    packageName: "dev.patrickgold.metroboard",
    description: "Metroboard — WP8.1-style soft keyboard fork (Cyanexani builds).",
    creator: "Cyanexani",
    type: "shell",
    backgroundColor: "#1BA1E2",
    githubRepo: "Cyanexani/metrokeyboard",
    // Android vector XML (keyboard) — Hub tints to tile content color.
    logoXml: `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="200dp"
    android:height="200dp"
    android:viewportWidth="16"
    android:viewportHeight="16">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M0 4v9h16V4H0zm10 2h1v1h-1V6zM8 6h1v1H8V6zm2 2v1H9V8h1zM6 6h1v1H6V6zm2 2v1H7V8h1zM4 6h1v1H4V6zm2 2v1H5V8h1zM2 6h1v1H2V6zm1 5H2v-1h1v1zm0-3h1v1H3V8zm9 3H4v-1h8v1zm0-2h-1V8h1v1zm2 2h-1v-1h1v1zm0-2h-1V7h-1V6h2v3z"/>
</vector>`,
    preferApk: (name) => {
      const n = name.toLowerCase();
      return n.endsWith(".apk") && n.includes("universal");
    },
    versionName: "0.6.0-alpha02",
    versionCode: 119,
  },
];

async function fetchJson(url) {
  const res = await fetch(url, {
    headers: {
      Accept: "application/vnd.github+json",
      "User-Agent": "metro-os-sync-hub-firestore",
    },
  });
  if (!res.ok) {
    throw new Error(`GET ${url} → ${res.status} ${res.statusText}`);
  }
  return res.json();
}

async function latestRelease(repo, includePrerelease) {
  if (includePrerelease) {
    const list = await fetchJson(
      `https://api.github.com/repos/${repo}/releases?per_page=5`,
    );
    if (!Array.isArray(list) || list.length === 0) {
      throw new Error(`No releases for ${repo}`);
    }
    return list[0];
  }
  try {
    return await fetchJson(`https://api.github.com/repos/${repo}/releases/latest`);
  } catch (err) {
    // Some repos only ship pre-releases (GitHub /latest 404s).
    const list = await fetchJson(
      `https://api.github.com/repos/${repo}/releases?per_page=5`,
    );
    if (!Array.isArray(list) || list.length === 0) throw err;
    return list[0];
  }
}

function pickApk(assets, preferApk) {
  const apks = (assets || []).filter((a) => /\.apk$/i.test(a.name || ""));
  if (apks.length === 0) return null;
  const preferred = apks.find((a) => preferApk(a.name));
  if (preferred) return preferred;
  // Fallback: universal → arm64 → first
  const score = (name) => {
    const n = name.toLowerCase();
    if (n.includes("universal")) return 0;
    if (n.includes("arm64") && n.includes("webview")) return 1;
    if (n.includes("arm64")) return 2;
    return 9;
  };
  return [...apks].sort((a, b) => score(a.name) - score(b.name))[0];
}

const credential = admin.credential.cert(JSON.parse(readFileSync(saPath, "utf8")));
if (!admin.apps.length) {
  admin.initializeApp({ credential });
}
const db = admin.firestore();
const col = db.collection("second-party");

const batch = db.batch();
const keepIds = new Set(["_meta"]);
let upserted = 0;

console.log(`Second-party: syncing ${CATALOG.length} curated app(s) from GitHub Releases…`);

for (const app of CATALOG) {
  const release = await latestRelease(app.githubRepo, !!app.includePrerelease);
  const tag = release.tag_name;
  const apk = pickApk(release.assets, app.preferApk);
  if (!apk) {
    console.error(`  SKIP ${app.id}: no .apk on ${app.githubRepo}@${tag}`);
    continue;
  }

  const versionName =
    app.versionName ||
    (app.versionNameFromTag ? app.versionNameFromTag(tag) : tag) ||
    null;

  const doc = {
    id: app.id,
    name: app.name,
    packageName: app.packageName,
    description: app.description,
    versionName,
    versionCode: app.versionCode ?? null,
    type: app.type,
    creator: app.creator,
    backgroundColor: app.backgroundColor,
    // Logos when set on the catalog entry; omit so console-edited
    // iconUrl / logoXml values are preserved by merge.
    iconUrl: app.iconUrl || null,
    logoXml: app.logoXml || null,
    apkName: apk.name,
    apkUrl: apk.browser_download_url,
    releaseUrl: release.html_url,
    githubRepo: `https://github.com/${app.githubRepo}`,
    sizeBytes: apk.size ?? null,
    party: "second",
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  const patch = Object.fromEntries(
    Object.entries(doc).filter(([, v]) => v !== null && v !== undefined),
  );

  batch.set(col.doc(app.id), patch, { merge: true });
  keepIds.add(app.id);
  upserted += 1;
  console.log(
    `  upsert second-party/${app.id}  ${versionName || "?"}  ${apk.name}  (${apk.size} bytes)`,
  );
}

await batch.commit();

const existing = await col.get();
let deleted = 0;
const deleteBatch = db.batch();
for (const doc of existing.docs) {
  if (keepIds.has(doc.id)) continue;
  deleteBatch.delete(doc.ref);
  deleted += 1;
  console.log(`  delete second-party/${doc.id}`);
}
if (deleted > 0) {
  await deleteBatch.commit();
}

await col.doc("_meta").set(
  {
    description:
      "second-party Hub catalog. Same fields as first-party (incl. backgroundColor). Logos: logoXml (vector XML or https PNG URL), iconUrl (https PNG), or logoPngBase64. Sync merge preserves console-edited logo fields when iconUrl is omitted from the catalog entry.",
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  },
  { merge: true },
);

console.log(`OK  second-party: upserted ${upserted}, deleted ${deleted}`);
NODE
}

case "$PARTY" in
  first)
    sync_first_party
    ;;
  second)
    sync_second_party
    ;;
  all)
    sync_first_party
    sync_second_party
    ;;
esac
