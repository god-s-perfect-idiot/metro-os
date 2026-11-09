# Clock

**Package:** `com.metro.clock`  
**Tier:** 2

## Status

Alarms, world clock, timer, and stopwatch pivots implemented.

## App role

WP8.1-style **Clock / Alarms** experience with four pivots: alarms list (primary), world clock, timer, and stopwatch. Alarms keep the slide-up editor + tile-flip time picker; timer reuses the same tile language for duration (hours | minutes | seconds).

## Build gate

- Toolkits verified
- Tier 0 shell passes verify

## Screen inventory

| Screen | Reference |
|--------|-----------|
| Alarms list | `references/images/alarms_list_dark_amber.png` |
| New / edit alarm | `references/images/alarms_new_dark_amber.png` |
| Time picker | `references/images/alarms_time_picker_dark_amber.png` |
| Time picker + slider | `references/images/alarms_time_picker_slider_dark_amber.png` |
| Alarm repeats on | `references/images/alarms_repeats_dark_amber.png` |
| World clock | see `references/known-gaps.md` — list + city picker |
| Timer | see `references/known-gaps.md` — countdown + duration tiles |
| Stopwatch | see `references/known-gaps.md` — elapsed + laps |

## System functions and contracts

- Exact alarms via `AlarmManager.setAlarmClock` + `RECEIVE_BOOT_COMPLETED` reschedule
- Timer completion via `TimerScheduler` / `TimerReceiver` → ring activity
- Bundled generic alarm tone in `res/raw/alarm_generic.wav` (no sound picker in v1)
- Theme/accent via `MetroSystemTheme` / `MetroPreferences`
- World cities persisted in `WorldClockStore` (SharedPreferences)

## UI and interaction guardrails

- `MetroToggleSwitch(showStatus = false)` on alarm rows
- New/edit editor **slides up** from the bottom with the app bar — **not** `MetroSubpageHost`
- World-clock city picker uses `MetroSubpageHost` page-pivot
- Time / duration picker tiles flip from center on launch; field sliders cross-fade
- Repeats uses `MetroMultiSelectList` patterns
- No Material time pickers / chips / FABs

## Data and state model

- `AlarmItem` — id, hour, minute, enabled, name, repeat weekdays
- `AlarmStore` / `AlarmScheduler` — persist + schedule / cancel / boot restore
- `WorldClockEntry` / `WorldClockStore` — curated IANA city ids
- `TimerLogic` — duration / stopwatch formatting
- `TimerScheduler` — exact fire when countdown ends in background

## Primary implementation order

1. Pivot shell (alarms + siblings)
2. Alarms list + scheduling
3. Slide-up new/edit
4. Time picker (flip + slider fade)
5. Repeats multi-select
6. World clock list + city picker
7. Timer + duration tile picker + ring
8. Stopwatch + laps

## Test-critical user flows

1. Create / toggle / edit / delete alarm
2. Time picker tile flip + slider cross-fade
3. Repeats → summary label
4. Alarm fires with generic tone
5. Boot reschedule
6. Add / select-delete world cities; live local times
7. Timer: set duration via tiles → start / pause / reset → ring at zero
8. Stopwatch: start / lap / pause / reset

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
| Exact background alarm reliability on every OEM | OEM Doze / alarm restrictions | Use `setAlarmClock` + document device quirks |
| Nokia / custom alarm tone picker | Out of scope v1 | Bundled generic WAV only |
| New-alarm page pivot enter | Reference uses bottom-bar slide-up | Slide-up overlay instead of `MetroSubpageHost` |
| Built-in world clock / timer / stopwatch | Stock WP8.1 Alarms was alarms-only; W10M added the rest | Metro pivot siblings matching suite `scope.md` |

## Agent postmortem

_None._
