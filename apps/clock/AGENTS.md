# Agent instructions — Clock (`com.metro.clock`)

**Tier 2** | Alarms, world clock, timer, stopwatch. Reference: `references/guides/blueprint.md`.

- Alarm rows use `MetroToggleSwitch(showStatus = false)`.
- New/edit alarm **slides up** from the Add app bar — do **not** use `MetroSubpageHost` for that surface.
- World-clock city picker **does** use `MetroSubpageHost`.
- Time / duration picker: center-first tile flip on enter; slider columns cross-fade on field change.
- Timer duration = hours | minutes | seconds tiles (same chrome as alarm time picker).
- No sound picker — play `R.raw.alarm_generic`.

Verify: `../../scripts/verify-app.sh clock`
