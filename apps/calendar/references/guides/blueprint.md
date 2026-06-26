# Calendar — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Navigation

Top-level navigation is a **pivot** with three items (left → right):

1. **agenda** — chronological list of upcoming events
2. **day** — focused hourly schedule for the selected date
3. **month** — month grid with event indicators

Pivot headers use `MetroPivot` / `MetroHubTitleRow` in Pivot mode. Selected pivot title is primary foreground; unselected titles are secondary. Horizontal flick or tap switches pivots.

The **selected date** is shared across all three pivots. Swiping between pivots preserves the selected date.

## App bar (bottom)

Standard `MetroAppBar` anchored to the bottom:

| Icon | Label | Action |
|------|-------|--------|
| Today | `today` | Jump selected date to today; scroll agenda/day to current time |
| Add | `new` | Stub in v1 — toast "Event creation not available in v1" |
| (ellipsis) | — | Expand bar; menu items below |

Max 4 icons. No FAB.

## Pages

### Page 1 — Agenda pivot

- **Layout:** Full-bleed black background. Below pivot header strip, a vertically scrolling list grouped by date.
- **Date group header:** Small caps label, e.g. `SUNDAY, 06 JULY 2014` — `MetroTextStyle.SectionHeader`, secondary text color.
- **Event row:** 76dp min height. Left column: time (`All day` or `HH:mm`) in `ListItemTitle`. Center: event title in accent/calendar color, duration subtitle in secondary text. Right: thin vertical accent bar (4dp wide, event height).
- **All-day events:** Time column shows `All day`; subtitle shows duration (e.g. `1 day`).
- **Empty state:** When no events in range, show centered secondary text: `No upcoming events`.
- **Navigation:** Tap event → event detail overlay (stub in v1: toast with title).
- **Interactions:** Scroll vertically. Today action scrolls to today's first event or top of today section.

### Page 2 — Day pivot

- **Layout:** Black background. Header area shows date label (small caps), large day name (lowercase, `HubTitle` style), peek of adjacent day names in secondary text to the right.
- **All-day / holiday row:** Below header, all-day events listed with accent-colored title.
- **Hourly grid:** Time labels on left (8 AM – 8 PM default window, scrollable). Thin horizontal divider lines. Events shown as accent-colored blocks or inline title + time within their hour slot.
- **Weather:** Out of scope v1 (platform stub acceptable).
- **Navigation:** Swipe left/right on day content changes selected date by ±1 day.
- **Interactions:** Tap empty hour slot → stub toast. Tap event → detail stub.

### Page 3 — Month pivot

- **Layout:** Black background. Year label small at top-left. Current month name large (`HubTitle`), adjacent months peek in secondary text.
- **Weekday header row:** Mon–Sun abbreviated, 7 equal columns, thin vertical dividers.
- **Grid:** 6 rows × 7 columns. Thin white grid lines. Date number top-left of cell. Small colored horizontal bars (2–3 max) in top-right for days with events. Selected day: accent triangle marker top-right. Today: accent underline on date number.
- **Below grid:** Optional event list for selected day (when a day is tapped, show that day's events below the grid).
- **Navigation:** Tap a day → select it and show events below grid. Swipe left/right changes month.
- **Interactions:** Today action returns to current month and selects today.

## Event detail (v1 stub)

Overlay page with back affordance. Shows title, date/time, location, calendar name. Out of full create/edit scope in v1 — read-only stub acceptable.

## Live tile

WP8.1 Calendar agenda tile. Accent background from `MetroAppRegistry.brandHex`. The tile exports a
structured agenda payload (`MetroTileData.agenda` → `MetroTileAgenda`) that the launcher renders for
medium (2×2) and wide (4×2) sizes; the small (1×1) tile falls back to the app icon.

Content (next upcoming appointment, today's date badge bottom-right):

- **Lines** (priority order `[title, location?, time]`): event title, then location, then the time
  range (`3:00 PM - 4:00 PM` or `All day`). If the event is not today its time line is prefixed with
  the weekday, e.g. `Mon: All day`.
- **Date badge** (bottom-right): short weekday + day-of-month for **today**, e.g. `Thu 15`.
- **Footer** (bottom-left, wide only space permitting): app name `Calendar`.

Size behaviour:

- **2×2 (medium):** title + trailing time line + date badge. See `images/live_tile_medium_dark_blue.png`.
- **4×2 (wide):** title + location + time + footer + date badge. See `images/live_tile_wide_dark_blue.png`.

When there are no upcoming events the tile shows just the date badge and footer.

## Data

- v1 reads local Android `CalendarContract` events via `READ_CALENDAR` permission.
- When permission denied, show permission gate with demo data fallback (same as People/Messaging pattern).
- Events normalized to `CalendarEvent` model regardless of provider source.
- Timezone: device default (`ZoneId.systemDefault()`).
- Recurrence: display expanded instances only (Android provider returns expanded events); no local recurrence engine in v1.

## Images

| Image | Page | Notes |
|-------|------|-------|
| `agenda_dark_blue.png` | Agenda pivot | WP8.0 agenda list — layout reference (restored in WP8.1 Update 2) |
| `day_dark_blue.png` | Day pivot | Hourly grid + header |
| `month_dark_blue.jpg` | Month pivot | Month grid + year header (official Microsoft) |
| `live_tile_medium_dark_blue.png` | Live tile (2×2) | Agenda tile — title, time, date badge |
| `live_tile_wide_dark_blue.png` | Live tile (4×2) | Agenda tile — title, location, time, footer, date badge |
| `week_dark_blue.jpg` | (ref only) | Week view — out of v1 scope |
| `week_expanded_dark_blue.png` | (ref only) | Expanded week day — out of v1 scope |
| `hero_dark_blue.jpg` | (ref only) | Marketing hero |

## Out of scope (v1)

- Week view and year view
- Weather integration in day/week headers
- Event create/edit flows (stub only)
- Account-specific sync (Exchange/Google quirks)
- Cortana scheduling
- Landscape layout
