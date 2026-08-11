# Agent instructions — metro-ui-android

**Phase 1 — build this toolkit before any app.**

## Purpose

Single source of WP8.1 UI components for all metro-os apps. Every control in `scope.md` §6 must live here.

## UX language (required reading)

Before implementing or changing any composable, read [`METRO-UX-LANGUAGE.md`](METRO-UX-LANGUAGE.md). It defines:

- **Square family** — tiles, border text buttons, lists, dialogs (0dp corners)
- **Round family** — app bar icon press circles, radio (exceptions only); toggle is square-family
- Per-control anatomy, states, and anti-patterns

`scope.md` holds numeric tokens; `METRO-UX-LANGUAGE.md` holds shape and interaction decisions.

## Package

`com.metro.ui`

## Required components (implement in order)

| Priority | Component | WP8.1 mapping |
|----------|-----------|---------------|
| P0 | `MetroTheme` | Theme provider (dark/light + accent) |
| P0 | `MetroText` / `MetroTextStyle` | Typography §1 |
| P0 | `MetroColors` | Exact hex from scope §2 |
| P0 | `MetroPageHeader` | 64sp title area |
| P1 | `MetroAppBar` ✅ | ApplicationBar — bottom, standard/minimized; `…` reveals icon labels + text overflow menu. Apps needing bottom-bar options must use this, not a bespoke row. |
| P1 | `MetroBorderButton` ✅ | Outlined square text button (§6.3) |
| P1 | `MetroIconButton` | In-page circular-press icon button (§6.4) |
| P1 | `MetroPivot` | Pivot control |
| P1 | `MetroPanorama` | Panorama control |
| P1 | `MetroListItem` | LongListSelector + tilt animation |
| P1 | `MetroToggleSwitch` | ToggleSwitch |
| P2 | `MetroSlider` / `MetroStepSlider` / `MetroBarStepSlider` | Slider (ticks / continuous bar) |
| P2 | `MetroProgressBar` | ProgressBar |
| P2 | `MetroTextBox` ✅ | TextBox — light fill, 3dp accent border when focused |
| P2 | `MetroMessageDialog` | MessageDialog |
| P2 | `MetroListPicker` ✅ | ListPicker — bordered field, inline inverted options |
| P2 | `MetroHub` | Hub control |
| P3 | `MetroDatePicker` / `MetroTimePicker` | Looping selectors |

## Motion (`MetroTransitions`)

Constants from `scope.md` §9 — export as `Duration` and easing objects:

- `PageEnter` / `PageExit`: 300ms ease-out
- `PivotSwitch`: 250ms ease-in-out
- `ListTilt`: 150ms, 3° Z-rotation
- `AppBarSlide`: 200ms from bottom
- `TileFlip`: 600ms turnstile
- `JumpListFlip`: 300ms `rotationX` with 40ms diagonal stagger

## Fonts

Bundle **Noto Sans** static faces in `src/main/res/font/` (DiscoLauncher [NotoCustom](https://github.com/cherryhoax/DiscoLauncher/tree/main/www/assets/fonts/NotoCustom), OFL):

- `noto_sans_thin.ttf`
- `noto_sans_extralight.ttf`
- `noto_sans_light.ttf`
- `noto_sans_regular.ttf`
- `noto_sans_medium.ttf`
- `noto_sans_semibold.ttf`
- `noto_sans_bold.ttf`
- `noto_sans_extrabold.ttf`
- `noto_sans_black.ttf`

Mapped via `MetroFontFamily` in `MetroTextStyle.kt`. Weight roles: Light → page titles; Medium → app titles; Regular → body/list titles; SemiBold → section headers; Bold/Black sparingly.

## Rules

- **No Material** in this module's public API surface.
- Every composable must have `@Preview` in dark + light theme.
- Export `Modifier.metroTiltOnPress()` for list items.
- Document each public composable in `README.md` and the matching section of `METRO-UX-LANGUAGE.md` §13.

## Verify

```bash
./scripts/verify-toolkit.sh metro-ui-android
```

## Tests required

- Unit: color contrast ratios, typography scale values
- Screenshot: each component in dark/light + blue accent on lumia-925

## Do not

- Add app-specific screens (e.g. launcher tiles belong in launcher app, tile *view* composable belongs here as `MetroTile`)
