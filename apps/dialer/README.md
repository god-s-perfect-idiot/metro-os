# Phone

**Package:** `com.metro.dialer`  
**Tier:** 2

## Status

Android project implements WP 8.1 Phone v1 per `references/guides/blueprint.md`.

## App role

Metro-native replacement for the WP 8.1 **Phone** app: grouped call history, speed dial, numeric keypad with T9 suggestions, and full-screen in-call UI.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Call log + phone permissions strategy approved

## Screen inventory

Authoritative spec: [`references/guides/blueprint.md`](references/guides/blueprint.md)

### 1. History (default pivot pane)

- Grouped recent calls with `(n)` count and one-tap callback
- Reference: `references/images/history_dark_blue.png`

### 2. Number view (call detail)

- Per-number chronological call list with duration
- Supplementary: `references/guides/phone-hub.md`

### 3. Dial pad

- Key grid, call/save tiles, T9 contact suggestions
- Reference: `references/images/dialpad_dark_blue.jpg`

### 4. In-call / dialing

- Full-screen outgoing call with end-call control
- Reference: `references/images/in_call_dark_blue.png`

### 5. Speed dial (pivot pane 1)

- Pinned contacts for quick dial
- Reference: `references/images/speed_dial_dark_blue.png`

## System functions and contracts

- `CallLog.Calls` for history; `ContactsContract.PhoneLookup` for names
- `Intent.ACTION_CALL` / `ACTION_DIAL` for placing calls
- `ACTION_DIAL` + `tel:` intent filters on MainActivity
- Launch People via `com.metro.people` package

## UI and interaction guardrails

- `MetroPivot` for history | speed dial
- WP 8.1: call/save on dial-pad grid, not app bar
- Grouped history with red missed-call coloring
- No Material FAB, chips, or rounded keypad tiles

## Data and state model

- `CallGroup`, `CallEntry`, `CallDirection`, `SpeedDialEntry`
- Track pivot index, dial string, active call, permissions, speed-dial list

## Primary implementation order

1. Call log repository + grouping logic
2. History + number view
3. Dial pad + T9 lookup
4. In-call screen
5. Speed dial persistence

## Test-critical user flows

1. Grant/deny call log permission
2. Browse grouped history and open number view
3. Enter number on dial pad and place call
4. End in-call and return to previous screen
5. Add/remove speed dial entry

## Reference and golden expectations

- `references/guides/blueprint.md` — read first
- `references/images/history_dark_blue.png`
- `references/images/dialpad_dark_blue.jpg`
- `references/images/in_call_dark_blue.png`
- `references/images/speed_dial_dark_blue.png`

## Commands

```bash
cd apps/dialer

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test

# From repo root
../../scripts/verify-app.sh dialer
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| System telephony stack integration | Emulator may lack SIM | Use `ACTION_DIAL` fallback; in-call timer simulated when `CALL_PHONE` unavailable |
| Green return-to-call banner under status tray | Requires notification-listener access in Status Bar | Phone posts an ongoing call notification; Status Bar draws the Metro green banner when notification access is granted |
| Incoming call UI over lock screen | Background activity starts blocked when locked/screen-off | High-priority FSI notification only when locked/screen-off; unlocked devices open Metro incoming UI directly (no Android heads-up) |
| Proximity screen-off on ear | Emulator / devices without proximity sensor | `PROXIMITY_SCREEN_OFF_WAKE_LOCK` while in-call UI is resumed on earpiece; no-ops when unsupported or on speaker/Bluetooth |
| Visual voicemail | Carrier-specific | Omitted in v1 |
| Dual-SIM smart switcher | Multi-SIM APIs vary | Omitted in v1 |

## Agent postmortem

_None._
