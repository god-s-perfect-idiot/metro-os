# Hub

**Package:** `com.metro.hub`  
**Tier:** 2

## Status

Panorama hub + Firestore catalog cache + GitHub APK downloads.

## App role

About / suite catalog for metro-os: panoramic overview, category quick links, and download/install of APKs from the latest GitHub release, with metadata cached in Firebase Firestore (`metro-os-a961a`).

## Screen inventory

1. **Hub panorama** — brand `hub`; panes `home` (HubLinks), `apps` (quick-link tiles), and `featured` (4 random Store rows from combined catalogs); app-bar search opens catalog search
2. **Suite apps list** — Firestore / release assets as Store-style rows (title, description, By); tap opens detail
3. **App detail** — version, size, category, full description; bottom app bar download text button installs the APK
4. **extras+info** — project about page (Metro Ruby alphas, latest release, buy me a coffee)
5. **Search** — Music explore–style `MetroTextBox` filtering the suite catalog; tap opens detail

See [`references/guides/blueprint.md`](references/guides/blueprint.md).

## System functions and contracts

- Firestore: `first-party`, `second-party`, `third-party`, `explore`
- Network: GitHub Releases API (`releases/latest`) for APK bytes / fallback catalog
- Install: `REQUEST_INSTALL_PACKAGES` + `FileProvider` + `ACTION_VIEW` package archive
- Theme via `MetroSystemTheme` / `MetroPreferences`
- Secrets (gitignored): `app/google-services.json`, `../../firebase/service-account.json`

## UI guardrails

- Toolkit-first: `MetroPanorama`, `MetroListItem`, `MetroAppBar`, `MetroLoadingScreen`, `MetroLoadingDots`
- No Material components
- Home catalog links: `metro os apps` → first-party, `related apps` → second-party, `unofficial metro apps` → third-party
- `get started with os` remains disabled in v1

## Data and state model

- Firestore collections: `first-party`, `second-party`, `third-party`, `explore`
- App docs include `backgroundColor` (`#RRGGBB`) from `ic_launcher_background` / brand fallback
- Logos: `logoXml` (vector XML **or** `https://…` PNG URL), `iconUrl` (https PNG), or `logoPngBase64`
- GitHub latest release remains the APK download source; Firestore caches metadata
- `HubAppCategory`, `ReleaseApkAsset`, `FirestoreHubApp`

## Commands

```bash
cd apps/hub

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test

# From repo root — sync Hub Firestore catalogs
./scripts/sync-hub-firestore.sh --tag alpha-8          # first-party (default)
./scripts/sync-hub-firestore.sh --party second         # curated second-party
./scripts/sync-hub-firestore.sh --party all --tag alpha-8
../../scripts/verify-app.sh hub
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| No WP8.1 Hub inbox app | N/A | Music-style panorama for metro-os catalog |
| Silent sideload | Package installer confirmation required | Request unknown-sources install permission + user prompt |
| Store CDN icons | Logos as vector XML, base64 PNG, or https PNG URL (`iconUrl` / `logoXml`); modern Android rejects string-backed `Drawable.createFromXml` | Rasterize `<vector>`/`<group>`/`<path>` (incl. evenOdd + fillAlpha) via `HubLogoDecoder` + Coil for remote PNGs |

## Agent postmortem

_None._
