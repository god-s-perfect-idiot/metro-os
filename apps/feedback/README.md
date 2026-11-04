# Feedback

**Package:** `com.metro.feedback`  
**Tier:** 2

## Status

Pivot hub with feedback submit (GitHub issue + metro logcat) and metrics (RAM / cache per suite app).

## App role

Diagnostics companion for metro-os: report issues with optional metro-only logcat, and inspect live memory / cache usage across installed `com.metro.*` packages.

## Screen inventory

1. **feedback** — title text box, collect metro logcat, submit → GitHub `issues/new` (+ share full log txt)
2. **metrics** — system RAM bar, total cache, per-app RAM/cache list

See [`references/guides/blueprint.md`](references/guides/blueprint.md).

## System functions and contracts

- Network: open GitHub issue draft in the browser (`INTERNET` not required for VIEW intent, but kept for future API)
- Logcat: `Runtime` / `ProcessBuilder` dump filtered to metro packages
- Metrics: `ActivityManager` + `StorageStatsManager` (usage-stats special access)
- Theme via `MetroSystemTheme` / `MetroPreferences`
- Share logcat via `FileProvider` (`com.metro.feedback.files`)

## UI guardrails

- Toolkit-first: `MetroPivot`, `MetroTextBox`, `MetroBorderButton`, `MetroListItem`, `MetroAppTitle`
- No Material components
- Border buttons hug label, flush left

## Data and state model

- `FeedbackState` — title, logcat file path/status, submit status, pivot index
- `MetricsRepository` — memory snapshot + per-app rows
- Submit target: `https://github.com/god-s-perfect-idiot/metro-os/issues/new`

## Commands

```bash
cd apps/feedback

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test

# From repo root
../../scripts/verify-app.sh feedback
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Full cross-app logcat | `READ_LOGS` privileged | Filter best-effort metro dump; optional `pm grant` on debug builds |
| Attach file to GitHub via deep link | Not supported | Prefill `issues/new` URL with full logcat in `body` + clipboard backup |
| Cross-app RAM | App UID cannot `dumpsys meminfo` / read other `/proc` | Suite [MetroMemoryProvider] (`{pkg}.metro.memory`) PSS probe; requires rebuilt suite apps |
| Always-on per-app cache | Usage access required | Grant button → system usage-access settings |

## Agent postmortem

_None._
