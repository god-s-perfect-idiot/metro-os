# Agent instructions — metro-os

You are building a **1:1 Windows Phone 8.1 experience on Android**. Read this file at the start of every session before writing code.

## Required reading order

1. [`scope.md`](scope.md) — design system, app inventory, quality gates (source of truth)
2. [`toolkits/metro-ui-android/METRO-UX-LANGUAGE.md`](toolkits/metro-ui-android/METRO-UX-LANGUAGE.md) — per-control WP8.1 shape, button, and interaction language
3. This file — operating rules and workflow
4. [`docs/HARNESS.md`](docs/HARNESS.md) — verification loop and self-correction protocol
5. [`docs/QUICK-START.md`](docs/QUICK-START.md) — one-page harness index
6. Target path's `AGENTS.md` — app- or toolkit-specific rules (e.g. `apps/launcher/AGENTS.md`)
7. Target app's `README.md` — detailed app build brief, page inventory, system contracts, and implementation guardrails
8. Target app's `references/guides/blueprint.md` — authoritative page and interaction spec
9. Target app's `references/README.md` — image catalog and reading order
10. [`docs/DESIGN-CHECKLIST.md`](docs/DESIGN-CHECKLIST.md) — before marking any UI task done

## Project map

| Path | Purpose |
|------|---------|
| `scope.md` | What to build and how it must look |
| `toolkits/` | Shared UI, system SDK, test harness — **use before duplicating code** |
| `apps/<name>/` | One independent Android app per folder |
| `apps/<name>/references/` | Per-app WP8.1 screenshots (`images/`) and web guides (`web-resources.md`) |
| `references/` | Global shared assets (design PDFs, device profiles, fonts) |
| `scripts/` | Build, verify, install automation |

## Hard rules

1. **No Material Design** in app UI. Banned: `com.google.android.material`, Material 3 components, FAB, snackbars, bottom sheets, navigation drawer/rail, elevation cards.
2. **Toolkit first**. If a control exists in `toolkits/metro-ui-android`, import it. Add to toolkit only when reused by ≥ 2 apps.
3. **No cross-app imports**. Apps talk via `metro-system-sdk` intents and preferences only.
4. **Reference-driven UI**. Every screen implements `apps/<name>/references/guides/blueprint.md` first; use `references/images/` for visual polish only.
5. **Verify before done**. Run `scripts/verify-app.sh <name>` (or toolkit equivalent). For UI work, also run `scripts/run-app.sh <name> --verify`. Fix failures up to 5 times before escalating.
6. **No scope drift**. Do not add features, dependencies, or patterns absent from `scope.md` without human approval.
7. **Portrait phone only** (v1). No tablet-first or landscape-primary layouts.
8. **Commit format**: `<app-or-toolkit>: <imperative summary>` (e.g. `launcher: add tile flip animation`).

## Standard workflow

```
READ scope + AGENTS → PLAN (cite references) → IMPLEMENT → TEST → VERIFY → REPORT
```

### Before editing

- Confirm which **app** or **toolkit** you are working in.
- Read the target app `README.md` in full before making app changes; treat it as the app-specific implementation spec unless it conflicts with `scope.md`.
- Check **build phase** in `scope.md` — do not build Tier 1 apps before Tier 0 shell passes verify.
- List reference screenshots you will match.

### While implementing

- Use Kotlin + Jetpack Compose.
- Theme via `MetroTheme` from `metro-ui-android`.
- System settings via `MetroPreferences` from `metro-system-sdk`.
- Log platform exceptions in the app's `README.md` § Platform exceptions.

### Before marking complete

```bash
# From repo root
./scripts/verify-app.sh <app-name>     # apps
./scripts/run-app.sh <app-name> --verify   # build + AVD completeness
./scripts/agent-overseer.sh <app-name> "<task>"   # autonomous loop (requires agent login)
./scripts/verify-toolkit.sh <name>     # toolkits (ui-android | system-sdk | test-harness)
```

All checks must pass. Read `apps/<name>/deploy/verify-report.json` on failure.

## Self-correction protocol

On verify failure:

1. Read stderr and `verify-report.json` `failures[]`.
2. Fix the **first** failure only; re-run verify.
3. Repeat up to **5** attempts.
4. After 5 failures: append a postmortem to the target `README.md` § Agent postmortem and stop.

See [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md) for common failures.

## Build order (do not skip)

```
Phase 1: toolkits (metro-ui-android → metro-system-sdk → metro-test-harness)
Phase 2: launcher → statusbar → navbar
Phase 3: browser, notes, music (parallel OK)
Phase 4: Tier 2 apps (parallel OK)
```

## Scaffolding a new app

Only when `scope.md` already lists the app:

```bash
./scripts/scaffold-app.sh <name> <tier> <package-suffix>
# Example: ./scripts/scaffold-app.sh browser 1 browser
```

Then customize `apps/<name>/references/guides/blueprint.md`, add visual refs to `references/images/`, and add golden screenshots.

## What not to do

- Do not commit APKs/AABs (they go in `deploy/`, gitignored).
- Do not update golden screenshots without human approval.
- Use **Noto Sans** for Metro chrome typography (not Roboto, not system default).
- Do not install Material theme or AppCompat Material overlays.
- Do not create git commits unless the user asks.

## Escalation

Escalate to the human when:

- Verify fails 5 times on the same task
- WP8.1 behavior is impossible on Android without a documented platform exception
- A new dependency is required not listed in scope
- Golden screenshots need updating
