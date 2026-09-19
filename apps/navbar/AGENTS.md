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
- Background from setup **Navbar background** ListPicker (default black / match app / accent)
- **Back**: page stack → exit app; long-press → recent apps (never backspace in `TextField`)
- **Start**: `MetroIntents` → launcher Start screen
- **Search**: tap → Google Search; long-press → Gemini
- Swipe up from bottom: hide bar; swipe up again: show
- Per-app: apps request opaque / hidden via `metro-system-sdk` `MetroNavBar`; fullscreen surfaces use `MetroStatusBarFullscreenEffect`
- Immersive: when Android navigation bars are hidden, the Metro bar creeps away (same motion as `MODE_HIDDEN`)
- Overlay activates only when Android system navigation is **3-button** (gesture / edge-to-edge stays disabled)
- On device rotate the bar slides out and back in from the new bottom
- No Material navigation bar

## Primary flows

1. Master **Navigation bar** toggle starts/stops the overlay (setup UI). **Navbar background** ListPicker chooses **Default black background** (solid black; default), **Match app background** (Metro suite → black; other apps → icon tile brand / adaptive background), or **Show accent color** (always system accent). On device rotate the bar slides out and back in from the new bottom.
2. Back dispatches to foreground app's back handler via `metro-system-sdk`
3. Start launches `com.metro.launcher`
4. Theme change updates bar color
5. Bar hides/shows with swipe gesture
6. Immersive / fullscreen apps hide the bar (contract `MODE_HIDDEN` or system navigation-bar hide)

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
| Soft keys always present | Gesture / edge-to-edge nav conflicts with overlay insets | Overlay usable only on Android **3-button** navigation |
| Hide nav bar on all devices | OEM variance | Document supported API levels |
| Fullscreen apps hide soft keys | Accessibility overlay would stay above immersive content | Apps call `MetroStatusBarFullscreenEffect` / `MetroNavBar.requestFullscreen`; shell also creeps away when `WindowInsets` reports navigation bars hidden (API 30+) |
