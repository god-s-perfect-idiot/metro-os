# Clock — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Pages

### Page 1 — Alarms list (`all`)

- Layout: Dark/light theme background. `MetroAppTitle("ALARMS")` overline + large page title `all` (`MetroPageHeader`). Vertical alarm list. Each row: large time (e.g. `8:00 AM`), status line `On: {name}` / `Off: {name}`, schedule subtitle (`Mon, Wed, Fri` or `only once`), trailing `MetroToggleSwitch(showStatus = false)`.
- Navigation: Root of the Clock app (alarms-first). Bottom `MetroAppBar` with centered Add (`+`) and ellipsis.
- Interactions: Toggle enables/disables and reschedules. Tap row (time/body) opens edit editor via **slide-up** (same chrome as new). Add opens new-alarm editor via **slide-up from the bottom** — **not** `MetroSubpageHost` / page-pivot. Delete via app-bar overflow when editing.
- Background: Theme background.

### Page 2 — New / edit alarm

- Layout: `MetroAppTitle("ALARMS")` + `MetroPageHeader("new")` (or the alarm name lowercased when editing). Fields stacked with 12dp horizontal inset:
  - **Time** — labelled bordered field (`MetroListPicker` chrome / `onOpen`) showing formatted time
  - **Repeats** — same bordered field showing `only once` or weekday summary
  - **Name** — `MetroTextBox` (light fill), default `Alarm`
  - **No Sound picker** — v1 always plays the bundled generic alarm tone
- Navigation: Full-screen overlay that **slides up** over the list (with its app bar). Back / cancel slides down. Save (floppy) commits and slides down.
- Interactions: Tap Time → Time picker. Tap Repeats → Repeats multi-select. Save persists + schedules.
- Background: Theme background.

### Page 3 — Time picker

- Layout: `MetroAppTitle("TIME")` only (no large page title). Centered horizontal row of **three accent-filled square tiles**: hour | minutes | AM/PM. Bottom `MetroAppBar`: Check (confirm) + Close (cancel).
- Navigation: Stacked above the editor (covers it). Confirm writes time back; cancel discards.
- Interactions / motion:
  1. **Entrance:** the three tiles **flip in from the center** (`rotationX` 90° → 0°). Center (minutes) starts first; hour and AM/PM follow with a short stagger.
  2. **Tap a tile:** fade in that field’s vertical value “slider” (outlined square cells above/below the accent tile showing adjacent looping values). Flick / tap neighbors to change the value; the accent tile always shows the selected value.
  3. **Tap another tile:** fade out the current slider, fade in the new field’s slider (cross-fade).
- Background: Theme background.

### Page 4 — Alarm repeats on

- Layout: `MetroMultiSelectList` style — overline `ALARM REPEATS ON`, seven weekday rows with square checkboxes, bottom Check / Close app bar.
- Navigation: Stacked above the editor. Confirm writes selection; empty selection = `only once`.
- Interactions: Toggle days; confirm/cancel.
- Background: Theme background.

### Page 5 — World clock

- Layout: Pivot sibling. `MetroAppTitle("ALARMS")` stays in the shell header; pivot title is `world clock`. Vertical city list. Each row: large local time (`8:05 AM`), city name as primary status line, offset subtitle (`+5:30 hrs` / `-8 hrs` / `same as local`). Empty → `MetroEmptyState("no cities")`.
- Navigation: Root pivot. Add opens **city picker** via `MetroSubpageHost` (page-pivot). Select mode (overflow) enables multi-select delete.
- Interactions: Add → pick a city (curated IANA list; already-added cities disabled). Live clock ticks once per second. Select → check rows → Delete removes. Compare / pin-to-Start out of scope v1.
- Background: Theme background.

### Page 6 — Timer

- Layout: Pivot sibling (`timer`). **In-page** hours | minutes | seconds accent tiles (same flip/slider language as Page 3) — **no nested app title / overlay**. Running updates the tile values and disables interaction.
- Navigation: Root pivot. Shared shell `MetroAppBar` (does not scroll with the pager); icons/text bounce via `enterKey`.
- Interactions: App bar **text buttons** `start` / `pause` and `reset`. When remaining hits zero → in-app `MetroMessageDialog` (“timer” / “time's up” / **close** only) with tone; auto-dismisses after 5s. Background finish posts a notification that reopens the app to the same dialog.
- Background: Theme background.

### Page 7 — Stopwatch

- Layout: Pivot sibling (`stopwatch`). Large elapsed `M:SS.hh` centered horizontally and vertically in the page. Lap list docks above the app bar (newest on top) without un-centering the time.
- Navigation: Root pivot. Shared shell app bar.
- Interactions: App bar Play / Pause; while running, Add records a lap; Refresh resets elapsed + clears laps.
- Background: Theme background.

### Page 8 — Add city

- Layout: `MetroSettingsHeader(appTitle = "world clock", pageTitle = "add city")` (tight overline → title spacing). Search field + dense city list. Bottom app bar with Close (`×`).
- Navigation: `MetroSubpageHost` from world clock.
- Interactions: Tap city adds and exits; Close cancels.
- Background: Theme background.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `alarms_list_dark_amber.png` | Alarms list | Dark theme, amber accent; two sample alarms + Add app bar |
| `alarms_new_dark_amber.png` | New / edit alarm | Time / Repeats / Sound / Name fields — **omit Sound in v1** |
| `alarms_time_picker_dark_amber.png` | Time picker | Three accent tiles, no slider yet |
| `alarms_time_picker_slider_dark_amber.png` | Time picker | Minutes slider faded in (outlined neighbors) |
| `alarms_repeats_dark_amber.png` | Alarm repeats on | Weekday multi-select |

World clock / timer / stopwatch captures: see `known-gaps.md` (reuse time-picker tiles for duration UX).

## Motion exceptions (platform)

| Surface | Spec | Why not `MetroSubpageHost` |
|---------|------|----------------------------|
| New / edit alarm | Slide up / down 300ms ease-out with the bottom app bar | User/WP Alarms reference: bottom-bar Add lifts the editor from the bottom |
| Time / duration picker tiles | Center-first `rotationX` flip | Matches WP tile entrance; reuse flip timing from `MetroTransitions.JumpListFlipMs` |
| Time / duration picker sliders | 200ms fade in/out | Cross-fade when switching hour/minute/(meridiem\|second) |

## Out of scope (v1)

- Custom / Nokia alarm sound picker (always play bundled generic tone)
- World-clock compare / pin-to-Start live tiles
- Multiple simultaneous timers
- Live tile / lock-screen alarm glyph
