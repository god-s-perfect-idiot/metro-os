# Statusbar — web resources

Curated links for how this app should look and behave on Windows Phone 8.1.

One `##` section per screen or feature. Use archive.org / Wikimedia mirrors when originals disappear.

## General

| Resource | URL | Notes |
|----------|-----|-------|
| WP8.1 UI Design and Interaction Guide | https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202915(v=win.10) | System tray and status indicators |
| First look at Windows Phone — Status bar | https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202905(v=vs.105) | Authoritative status-bar behavior + indicator order |
| Essential graphics, visual indicators, and notifications | https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202884(v=vs.105) | Progress indicators + status bar |
| scope.md — this app | [`scope.md`](../../../scope.md) | Repo source of truth |

## Collapsed tray (resting state)

| Resource | URL | Notes |
|----------|-----|-------|
| First look at Windows Phone | https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202905(v=vs.105) | "By default, only the system clock is always visible." Clock is right-aligned. |
| WP8.1 Settings (Einstellungen) capture | https://commons.wikimedia.org/wiki/File:Windows_Phone_8.1_Update_2_Einstellungen.png | Dark theme page with clock-only resting tray (public domain, Armin2208) |

## Expanded tray (tap to reveal indicators)

| Resource | URL | Notes |
|----------|-----|-------|
| First look at Windows Phone | https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202905(v=vs.105) | "If a user taps in the Status Bar area, all other relevant indicators slide into view for approximately eight seconds before sliding out of view." |
| WP8.1 Action Center (Benachrichtigungszentrale) capture | https://commons.wikimedia.org/wiki/File:Windows_Phone_8.1_Benachrichtigungszentrale.png | Full expanded tray: L→R signal/data/Wi-Fi, R battery glyph + clock with battery % below (public domain, Armin2208) |

### Authoritative WP8.1 indicator order (left → right)

Per Microsoft (`hh202905`): signal strength, data connection, call forwarding, roaming,
wireless-network (Wi-Fi) signal, Bluetooth, ringer mode, input status, battery power level,
system clock. This project's `blueprint.md` uses the simplified v1 subset
(cellular, Wi-Fi, Bluetooth, alarm, location, battery, clock) — blueprint is authoritative here.

## Progress tray state

| Resource | URL | Notes |
|----------|-----|-------|
| Essential graphics & indicators (Progress) | https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202884(v=vs.105) | Indeterminate `StatusBarProgressIndicator` (sweeping accent dots) sits in the tray during long ops |
| StatusBar / ProgressIndicator API | https://learn.microsoft.com/en-us/uwp/api/windows.ui.viewmanagement.statusbar | `ProgressIndicator.ShowAsync()` — indeterminate accent affordance |

## Background / opacity behavior

| Resource | URL | Notes |
|----------|-----|-------|
| Differences between StatusBar and SystemTray | https://visuallylocated.azurewebsites.net/post/2014/04/07/Differences-between-the-new-StatusBar-in-Windows-Phone-XAML-Apps-and-the-SystemTray.aspx | Opaque vs translucent; `BackgroundOpacity`; foreground/background color control |

## Reference images

| File | Source | License |
|------|--------|---------|
| `images/expanded_dark.png` | Wikimedia Commons — Windows Phone 8.1 Benachrichtigungszentrale.png (Armin2208) | Public domain |
| `images/collapsed_dark.png` | Wikimedia Commons — Windows Phone 8.1 Update 2 Einstellungen.png (Armin2208) | Public domain |
