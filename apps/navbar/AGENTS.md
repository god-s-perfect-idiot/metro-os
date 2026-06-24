# Agent instructions — Navigation Bar (`com.metro.navbar`)

**Tier 0 — Metro Shell** | Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

WP8.1 **soft key bar** — Back, Start, Search. Theme-colored 48dp bar; swipe-to-hide.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| Toolkits verified | Yes |
| Launcher installed | Yes (Start key target) |

## Surfaces

| Surface | Reference |
|---------|-----------|
| Standard bar (3 keys) | `references/images/navbar.png` |
| Hidden bar (swipe reveal) | `references/images/hidden_dark.png` |

## WP8.1 rules

- Keys: Back (chevron), Start (Windows logo), Search (magnifier)
- Height **48dp**; icons white on dark bar, black on light
- Background from `MetroPreferences.nav_bar_color` or theme
- **Back**: page stack → exit app; long-press → recent apps (never backspace in `TextField`)
- **Start**: `MetroIntents` → launcher Start screen
- **Search**: tap → Google Search; long-press → Gemini
- Swipe up from bottom: hide bar; swipe up again: show
- No Material navigation bar

## Primary flows

1. Back dispatches to foreground app's back handler via `metro-system-sdk`
2. Start launches `com.metro.launcher`
3. Theme change updates bar color
4. Bar hides/shows with swipe gesture

## Golden screenshots

```
screenshots/golden/bar_dark_blue.png
screenshots/golden/bar_light_blue.png
```

## Implementation options

Prefer `AccessibilityService` or `TYPE_NAVIGATION_BAR` overlay per `scope.md`. Document chosen approach in README.

## Verify

```bash
../../scripts/verify-app.sh navbar
```

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Hide nav bar on all devices | OEM variance | Document supported API levels |
