# WP8.1 design checklist

Use before marking any UI task complete. Every item must pass or be documented as a platform exception in the app README.

**Per-control UX rules:** [`toolkits/metro-ui-android/METRO-UX-LANGUAGE.md`](../toolkits/metro-ui-android/METRO-UX-LANGUAGE.md) — squares, border buttons, circular icon press, tiles, pivots, anti-patterns.

## Shape language

- [ ] Tiles and border text buttons use **0dp** corner radius (sharp squares)
- [ ] App bar icon buttons use **circular** press affordance — circle not baked into asset
- [ ] No rounded-corner cards, FABs, or Material elevation
- [ ] ToggleSwitch is the only pill-shaped control in chrome (besides slider thumb / radio)

## Typography

- [ ] Noto Sans used for all Metro chrome (not Roboto)
- [ ] No text below 15sp in interactive UI
- [ ] Max 2 font weights per screen
- [ ] Page title 64sp, flush left
- [ ] List titles 24sp, subtitles 16sp at 60% opacity

## Color and theme

- [ ] Screen tested in **dark** theme
- [ ] Screen tested in **light** theme
- [ ] Accent color from official palette only (`scope.md` §2)
- [ ] Body text contrast ≥ 4.5:1
- [ ] No pure black/white tile backgrounds
- [ ] App bar at 80% opacity over content

## Layout

- [ ] 12dp horizontal margins on list content
- [ ] Touch targets ≥ 44×44dp
- [ ] 12dp grid alignment
- [ ] Pivot header 48dp with 3dp accent underline on selected

## System chrome

- [ ] App bar at **bottom** (never top)
- [ ] Panorama pages use **minimized** app bar (ellipsis only)
- [ ] No FAB
- [ ] Status bar 32dp — no Material status styling
- [ ] Screen root applies `Modifier.metroNavBarPadding()` so content clears the navigation bar overlay when enabled (no hard-coded 48dp)
- [ ] Page transitions 300ms horizontal slide

## Controls

- [ ] All controls from `metro-ui-android` toolkit (not reimplemented)
- [ ] Border text buttons: 3dp stroke, square corners, transparent rest (see `METRO-UX-LANGUAGE.md` §6.3)
- [ ] App bar: use `MetroAppBar` — icon-only row (max 4), `…` reveals labels + text menu list below the icon row (max 5), bottom only
- [ ] List items use tilt-on-press (3°, 150ms)
- [ ] ToggleSwitch pill shape with accent fill when on
- [ ] No Material buttons, chips, cards, bottom sheets, snackbars

## Navigation

- [ ] Pivot for filtering (max 7 items)
- [ ] Panorama for hub/overview only
- [ ] Back navigates page stack (not backspace in text fields)
- [ ] No drawer navigation

## Motion

- [ ] Page transition 300ms ease-out
- [ ] Pivot switch 250ms
- [ ] Progress shown for operations > 500ms
- [ ] No Material shared-element transitions

## Reference

- [ ] Reference screenshot path noted in PR/commit description
- [ ] Screenshot diff ≤ 2% on lumia-925 profile

## Banned imports (auto-fail lint)

```
com.google.android.material.*
androidx.compose.material3.*   # except where explicitly allowed in toolkit internals
com.google.android.material.floatingactionbutton.*
```

Allowed: `androidx.compose.material` icons only if wrapped by metro-ui-android icon set.
