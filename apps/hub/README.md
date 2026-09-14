# Hub

**Package:** `com.metro.hub`  
**Tier:** 2

## Status

Panorama hub + Firestore catalog cache + GitHub APK downloads.

## App role

About / suite catalog for metro-os: panoramic overview, category quick links, and download/install of APKs from the latest GitHub release, with metadata cached in Firebase Firestore (`metro-os-a961a`).

## Screen inventory

1. **Hub panorama** — brand `hub`; panes `home` (HubLinks) and `apps` (quick-link tiles)
2. **Suite apps list** — Firestore / release assets as Store-style rows (title, description, By); tap opens detail
3. **App detail** — version, size, category, full description; bottom app bar download icon installs the APK

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
- Stub destinations for most home links until wired

## Data and state model

- Firestore collections: `first-party`, `second-party`, `third-party`, `explore`
- App docs include `backgroundColor` (`#RRGGBB`) from `ic_launcher_background` / brand fallback
- GitHub latest release remains the APK download source; Firestore caches metadata
- `HubAppCategory`, `ReleaseApkAsset`, `FirestoreHubApp`

## Commands

```bash
cd apps/hub

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test

# From repo root — sync first-party docs after a release
./scripts/sync-hub-firestore.sh --tag alpha-8
../../scripts/verify-app.sh hub
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| No WP8.1 Hub inbox app | N/A | Music-style panorama for metro-os catalog |
| Silent sideload | Package installer confirmation required | Request unknown-sources install permission + user prompt |
| Store CDN icons | Logos cached in Firestore as vector XML / PNG | Render via `HubLogoDecoder` |

## Agent postmortem

_None._
