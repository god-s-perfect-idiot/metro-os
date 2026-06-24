# Navbar — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Pages

### Page 1 — Default navigation bar

- Layout: full-width bottom strip, **48dp** tall, theme-colored background
- Keys left → center → right: **Back** (chevron), **Start** (four-pane Windows logo), **Search** (magnifier)
- Icons monochrome: white on dark bar, black on light bar; no circular outlines on soft keys
- Background from `MetroPreferences.nav_bar_color` or accent fallback
- Navigation: overlay service draws above all apps; setup activity grants overlay + accessibility permissions

### Page 2 — Hidden bar (swipe reveal)

- Layout: **6dp** reveal strip at bottom edge when bar is hidden
- Interactions: swipe up on strip or tap strip to restore full 48dp bar; swipe up on visible bar hides it
- Background: same theme color at ~85% opacity on reveal strip

## System behavior

| Key | Action |
|-----|--------|
| Back (tap) | `AccessibilityService.performGlobalAction(BACK)` when enabled; never backspace in text fields |
| Back (long press) | `AccessibilityService.performGlobalAction(RECENTS)` — recent apps |
| Start | Launch `com.metro.launcher` Start/home activity |
| Search (tap) | Open Google Search (`android.search.action.GLOBAL_SEARCH` on Google app, then web-search fallbacks) |
| Search (long press) | Launch Google Gemini (`com.google.android.apps.bard`) |
| Theme | Observe `com.metro.system.THEME_CHANGED`; redraw within one frame |

## Images

| Image | Page | Notes |
|-------|------|-------|
| `navbar.png` | Default navigation bar | Three-key black bar reference (Back / Start / Search) |
| `hidden_dark.png` | Hidden bar | Swipe-reveal strip reference |

## Out of scope (v1)

- Per-app custom key layouts
- Haptic profiles beyond default Android feedback
