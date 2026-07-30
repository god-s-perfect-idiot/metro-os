# Agent instructions — Keyboard (`com.metro.keyboard`)

**Tier 0** | Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

WP8.1 SIP / Word Flow–style touch keyboard and keyboard settings. Engine is FlorisBoard-derived; UI chrome and settings must match WP8.1, not Material.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| `metro-ui-android` verified | Yes |
| `metro-system-sdk` verified | Yes |
| Tier 0 shell (launcher/statusbar/navbar) | Recommended before ship |
| Tier 1 apps | No |

## Screens to implement

| Screen | Navigation pattern | Reference |
|--------|-------------------|-----------|
| Alpha SIP | IME window | `references/images/sip_alpha_dark_black.png` |
| Symbols / emoji | IME pages | `references/guides/blueprint.md` + `known-gaps.md` |
| Keyboard settings | Page stack | blueprint Surface 4 |

## WP8.1 rules specific to this app

- SIP themes: `wp81_dark` / `wp81_light` (0dp keys, black/white prediction bar)
- Settings: `MetroTheme` + `MetroPreferences` accent; `Modifier.metroNavBarPadding()`
- No Material in `com.metro.keyboard.**`
- Do not change golden screenshots without human approval

## Primary user flows (instrumented tests required)

1. Launch Keyboard settings from launcher
2. Enable + select IME via setup banners
3. Type in an editor; prediction bar updates
4. Toggle Suggest text; predictions follow
5. Back returns through settings stack

## Golden screenshots required

```
screenshots/golden/sip_alpha_dark_blue.png
screenshots/golden/settings_keyboard_dark_blue.png
```

## Verify

```bash
cd apps/keyboard
./gradlew :app:assembleDebug
cd ../.. && ./scripts/verify-app.sh keyboard
```

## Platform exceptions

See [`README.md`](README.md) § Platform exceptions.

## Agent postmortem

_(Agents append here after 5 failed verify iterations — see docs/TROUBLESHOOTING.md)_
