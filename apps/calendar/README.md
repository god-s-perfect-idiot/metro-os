# Calendar

**Package:** `com.metro.calendar`  
**Tier:** 2

## Status

Implemented — agenda / day / month pivot views with local calendar provider data, demo fallback, and live tile provider.

## App role

This app recreates the WP8.1 **Calendar** experience with agenda, day, and month views organized via pivot navigation and backed by local calendar-provider data in v1.

The emphasis is information clarity, fast pivot switching, and Metro layout discipline rather than dense Android calendar chrome.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Local calendar-provider access strategy approved

## Screen inventory

### 1. Agenda pivot

- Chronological agenda list
- Expected reference: `references/images/agenda_dark_blue.png`

### 2. Day pivot

- Focused day schedule
- Expected reference: `references/images/day_dark_blue.png`

### 3. Month pivot

- Month overview surface
- Expected reference: `references/images/month_dark_blue.png`

## System functions and contracts

- Use local calendar provider data in v1
- Provide a Today action in the app bar
- Normalize event models so agenda/day/month render from the same source of truth
- Define recurrence and timezone handling explicitly before edge-case bug fixing begins

## UI and interaction guardrails

- Pivot is the top-level navigation pattern here
- Keep headers and typography consistent with WP8.1 hierarchy
- Avoid dense Material calendars, chips, or floating create actions
- Use app bar actions for Today and any add/edit flow

## Data and state model

- `CalendarEvent`, `DayBucket`, `MonthGridCell`
- Track selected date, current pivot, timezone context, and provider sync/load state

## Primary implementation order

1. Build provider repository and event normalization
2. Implement selected-date state and Today action
3. Implement agenda view
4. Implement day view
5. Implement month view
6. Add event detail/create flows if in scope

## Test-critical user flows

1. Load local calendar events
2. Switch among agenda/day/month pivots
3. Jump back to Today
4. Preserve selected date when navigating in and out of detail screens

## Reference and golden expectations

- `references/images/agenda_dark_blue.png`
- `references/images/day_dark_blue.png`
- `references/images/month_dark_blue.png`

## Commands

```bash
cd apps/calendar

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh calendar
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Exact WP calendar account integrations | Android provider/account combinations differ by device | Build the Metro surfaces over local provider data first and document unsupported account-specific quirks |

## Agent postmortem

_None._
