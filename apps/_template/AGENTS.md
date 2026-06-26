# Agent instructions — {{DISPLAY_NAME}} (`{{PACKAGE}}`)

**Tier {{TIER}}** | Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

<!-- CUSTOMIZE: Describe this app's WP8.1 equivalent and primary user flows -->

{{DISPLAY_NAME}} for metro-os. Package: `{{PACKAGE}}`.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| `metro-ui-android` verified | Yes |
| `metro-system-sdk` verified | Yes |
| Tier 0 shell (if Tier ≥ 1) | <!-- yes/no --> |
| Tier 1 apps (if Tier 2) | No |

## Screens to implement

<!-- CUSTOMIZE: List each screen with reference screenshot path -->

| Screen | Navigation pattern | Reference |
|--------|-------------------|-----------|
| Main | Page / Pivot / Panorama | `references/guides/blueprint.md` |

## WP8.1 rules specific to this app

<!-- CUSTOMIZE: App-specific Metro rules beyond scope.md -->

- Use `MetroTheme` and system accent from `MetroPreferences`
- App bar at bottom; no FAB
- Register `ThemeChangeReceiver` for live theme updates
- Apply `Modifier.metroNavBarPadding()` to every screen's root container so content clears the
  navigation bar overlay when it is enabled (see root `AGENTS.md` / metro-android-ui rule)

## Primary user flows (instrumented tests required)

<!-- CUSTOMIZE -->

1. Launch app from launcher tile
2. Navigate primary content
3. Back button returns through page stack correctly

## Golden screenshots required

Capture on **lumia-925** profile:

```
screenshots/golden/main_dark_blue.png
screenshots/golden/main_light_blue.png
```

## Verify

```bash
cd apps/{{APP_NAME}}
./gradlew :app:assembleDebug
cd ../.. && ./scripts/verify-app.sh {{APP_NAME}}
```

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| _(none yet)_ | | |

## Agent postmortem

_(Agents append here after 5 failed verify iterations — see docs/TROUBLESHOOTING.md)_
