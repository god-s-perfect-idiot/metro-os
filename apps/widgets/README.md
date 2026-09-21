# Widgets

**Package:** `com.metro.widgets`  
**Tier:** 2

## Status

Implemented — Start-style 4-column widget catalog (Time, Battery, Storage Sense). Pin-to-Start deferred.

## App role

Catalog of custom homescreen widgets rendered as live tiles. Opens to a black Start-like grid (4 columns only). Footprints: 1×1, 2×2, 2×4 (wide).

## Screen inventory

1. **Widget catalog** — `MetroAppTitle` `widgets` + packed tile grid  
   - Time (2×4 digital clock — AM/PM + square colon; no weather)  
   - Battery (1×1 vertical glyph + digits)  
   - Storage Sense (2×4 free/used)

See [`references/guides/blueprint.md`](references/guides/blueprint.md).

## System functions and contracts

- Theme via `MetroSystemTheme` / `MetroPreferences`
- Battery: `ACTION_BATTERY_CHANGED`
- Storage: `StatFs` on primary / secondary volumes
- No launcher pin contract in v1

## UI guardrails

- Toolkit-first (`MetroAppTitle`, `MetroTheme`, `MetroText`)
- No Material components
- Match Start gutters (12dp) and gap (8dp)
- `Modifier.metroNavBarPadding()` on root

## Data and state model

- `WidgetTileSize`: OneByOne / TwoByTwo / TwoByFour
- `WidgetCatalog`: fixed placements for the three faces
- `BatterySnapshot`, `StorageSnapshot` formatters (unit-tested)

## Commands

```bash
cd apps/widgets

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test

../../scripts/verify-app.sh widgets
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Stock Time Start tile | None shipped | Digital clock face from third-party WP8.1 precedent |
| Battery Saver mode shield | No OS Battery Saver API | Shield / low styling at ≤20% |

## Agent postmortem

_None._
