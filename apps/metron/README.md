# Metron

**Package:** `com.metro.metron`  
**Tier:** 2  
**Upstream:** [mihonapp/mihon](https://github.com/mihonapp/mihon) (full tree fork)

## Status

Full Mihon multi-module app vendored under this folder. Home uses Metro panorama chrome;
remaining screens are upstream Mihon UI pending Metro overhaul. No demo library — real
Mihon database, sources, downloads, reader, and extensions.

## App role

Manga / comic reader forked from Mihon with WP8.1 Metro home (panorama panes for
library / updates / history / browse; More via app-bar menu).

## Build gate

- Toolkits verified (`metro-ui-android`, `metro-system-sdk` via includeBuild)
- Tier 0 shell passes verify

## Screen inventory

1. **Panorama home** — brand `metron`; panes library / updates / history / browse (real Mihon tabs)
2. **More** — full-page Metro settings chrome (`MetroSettingsHeader` / list / toggles)
3. **Settings hub + categories** — Metro `PreferenceScaffold` over Mihon preference graphs
4. **All other Mihon screens** — manga detail, reader, browse source, extensions, trackers, …

## Commands

```bash
cd apps/metron

./gradlew :app:assembleDebug -Pdist=foss -Pinclude-telemetry=false
./gradlew :app:installDebug -Pdist=foss -Pinclude-telemetry=false
./gradlew :app:testDebugUnitTest -Pdist=foss -Pinclude-telemetry=false

# App OTA / Mihon release polling is permanently off (Hub ships Metron APKs).

# From repo root (uses assembleDebug)
../../scripts/verify-app.sh metron
```

## Licence

Apache-2.0 — see `LICENSE` / `MIHON-APACHE-2.0.txt`. Fork notes: `MIHON-FORK.txt`.

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|--------------------|------------|
| No WP manga reader | N/A | Music panorama + full Mihon engine |
| Full Metro on every Mihon screen | Large surface area | Home Metro first; drill-ins Metroized incrementally |

## Agent postmortem

_None._
