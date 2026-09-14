#!/usr/bin/env bash
# Sync Hub catalog collections into Firestore.
#   first-party  — metro-os suite APKs from a GitHub release (local aapt optional)
#   second-party — curated external Metro apps; metadata from GitHub Releases API only
# Requires: firebase/service-account.json, network. gh optional for first-party.
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
  "photos", "calendar", "mail", "messaging", "people", "dialer",
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

for (const apkName of releaseApks.sort()) {
  const id = assetId(apkName);
  const apkPath = join(apkDir, apkName);
  const fromApk = badging(apkPath);
  const fromGradle = readVersionFromGradle(id);
  const logos = readLogoXml(id);
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
  console.log(`  upsert first-party/${id}  ${doc.versionName || "?"}  ${doc.type}  ${backgroundColor}`);
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
    backgroundColor: "#1BA1E2",
    githubRepo: "god-s-perfect-idiot/Metro-Weather",
    preferApk: (name) => name.toLowerCase().endsWith(".apk"),
    versionNameFromTag: (tag) => tag,
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
      "second-party Hub catalog. Same fields as first-party (incl. backgroundColor); creator + githubRepo vary per app.",
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
