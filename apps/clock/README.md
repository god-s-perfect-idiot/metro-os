# Clock

**Package:** `com.metro.clock`  
**Tier:** 2

## Status

Harness docs only — Android project not scaffolded yet. This README is the implementation guide for the WP8.1 clock app.

## App role

This app recreates the WP8.1 **Clock** experience with four pivots: alarms, world clock, timer, and stopwatch.

It should feel utility-focused, rhythmically simple, and Metro-native. Avoid Android OEM clock patterns that drift away from WP8.1 layout and interaction.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Alarm/timer background behavior strategy reviewed before coding

## Screen inventory

### 1. Alarms pivot

- List and toggle alarms
- Expected reference: `references/images/alarms_dark_blue.png`

### 2. World clock pivot

- City/timezone list
- Expected reference: `references/images/worldclock_dark_blue.png`

### 3. Timer pivot

- Countdown timer controls
- Expected reference: `references/images/timer_dark_blue.png`

### 4. Stopwatch pivot

- Stopwatch controls and elapsed-time display
- Expected reference: `references/images/stopwatch_dark_blue.png`

## System functions and contracts

- Alarm scheduling must be explicit and testable
- Timer and stopwatch state handling should survive app backgrounding where feasible
- World clock requires a stable city/timezone data source
- Respect the max-four-pivots pattern already implied by the product

## UI and interaction guardrails

- Use `MetroToggleSwitch` for alarm enable/disable
- Pivot is the top-level organizer
- Avoid Material time pickers and chips
- Keep the screen text-first and flat

## Data and state model

- `AlarmItem`, `WorldClockCity`, `TimerState`, `StopwatchState`
- Track currently selected pivot, active alarms, and running timer/stopwatch lifecycle state

## Primary implementation order

1. Build pivot shell
2. Implement alarms list and scheduling
3. Implement timer
4. Implement stopwatch
5. Implement world clock city list and timezone formatting

## Test-critical user flows

1. Create/toggle alarm
2. Start/pause/reset timer
3. Start/pause/reset stopwatch
4. Browse world clock cities
5. Preserve running state across lifecycle events where supported

## Reference and golden expectations

- `references/images/alarms_dark_blue.png`
- `references/images/worldclock_dark_blue.png`
- `references/images/timer_dark_blue.png`
- `references/images/stopwatch_dark_blue.png`

## Commands

```bash
cd apps/clock

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh clock
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Exact background alarm/timer reliability across every Android build | OEM alarm/background restrictions can vary | Preserve Metro UX and document any device-specific scheduling limitations if encountered |

## Agent postmortem

_None._
