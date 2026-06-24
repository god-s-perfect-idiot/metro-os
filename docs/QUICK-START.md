# Harness quick reference

One-page index for agents. Full detail in linked docs.

## Start every session

```bash
./scripts/verify-env.sh
```

Read: `AGENTS.md` → `toolkits/metro-ui-android/METRO-UX-LANGUAGE.md` (UI work) → `apps/<name>/AGENTS.md` or `toolkits/<name>/AGENTS.md` → `scope.md` (relevant section)

## Verify commands

| Command | Purpose |
|---------|---------|
| `./scripts/verify-env.sh` | JDK, SDK, adb |
| `./scripts/verify-toolkit.sh metro-ui-android` | UI toolkit |
| `./scripts/verify-toolkit.sh metro-system-sdk` | System SDK |
| `./scripts/verify-toolkit.sh metro-test-harness` | Test harness |
| `./scripts/verify-app.sh <name>` | Single app gate |
| `./scripts/run-app.sh <name>` | Build, install, launch on AVD |
| `./scripts/run-app.sh <name> --verify` | Above + on-device completeness |
| `./scripts/verify-avd.sh <name>` | AVD screenshots + UI probes |
| `./scripts/agent-overseer.sh <name> "<task>"` | Cursor agent orchestration |
| `./scripts/verify-all.sh` | Full suite |
| `./scripts/install-shell.sh` | Install Tier 0 APKs |

## Self-correction

1. Implement smallest change
2. Run verify
3. Read `apps/<name>/deploy/verify-report.json` → `failures[0]`
4. Fix one issue; repeat (max 5×)
5. Postmortem in app README if still failing

## Doc map

| File | Use when |
|------|----------|
| [`scope.md`](../scope.md) | What to build, design specs |
| [`METRO-UX-LANGUAGE.md`](../toolkits/metro-ui-android/METRO-UX-LANGUAGE.md) | Per-control WP8.1 UX language |
| [`AGENTS.md`](../AGENTS.md) | Global agent rules |
| [`HARNESS.md`](HARNESS.md) | Verify pipeline, golden policy, per-app references |
| [`DESIGN-CHECKLIST.md`](DESIGN-CHECKLIST.md) | Before UI task done |
| [`TROUBLESHOOTING.md`](TROUBLESHOOTING.md) | Verify failed |
| [`CONTRIBUTING.md`](../CONTRIBUTING.md) | Human review gates |
| [`.cursor/rules/`](../.cursor/rules/) | Cursor auto-loaded rules |

## Build order

```
toolkits → launcher → statusbar → navbar → browser/notes/music → Tier 2
```

## Scaffold

```bash
./scripts/scaffold-app.sh <name> <tier> <suffix>
```
