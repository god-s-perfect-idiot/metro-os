# metro-os

A suite of Android applications that recreate the **Windows Phone 8.1** user experience — launcher, system chrome, browser, notes, music, and more — using the Metro design language at 1:1 fidelity.

## Quick start

```bash
# Verify environment
./scripts/verify-env.sh

# Build, install, and launch on AVD (single command)
./scripts/run-app.sh launcher
./scripts/run-app.sh launcher --verify   # + on-device screenshots & UI checks

# Full verification gate (build, tests, lint)
./scripts/verify-app.sh launcher
METRO_VERIFY_AVD=1 ./scripts/verify-app.sh launcher   # include AVD checks

# Autonomous agent loop (coding agent → verify → AVD → verifier agent)
agent login   # once
./scripts/agent-overseer.sh launcher "implement blueprint gaps"

# Install Metro Shell on device/emulator
./scripts/install-shell.sh
```

## Documentation

| Document | Audience | Contents |
|----------|----------|----------|
| [`scope.md`](scope.md) | Everyone | Full project spec, design system, app inventory |
| [`AGENTS.md`](AGENTS.md) | AI agents | Operating rules, workflow, build order |
| [`docs/QUICK-START.md`](docs/QUICK-START.md) | Agents | One-page harness index |
| [`docs/HARNESS.md`](docs/HARNESS.md) | Agents + devs | Harness engineering, verify loop, reports |
| [`docs/DESIGN-CHECKLIST.md`](docs/DESIGN-CHECKLIST.md) | UI work | Pre-merge WP8.1 compliance checklist |
| [`docs/TROUBLESHOOTING.md`](docs/TROUBLESHOOTING.md) | Agents | Failure recovery playbook |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Humans | Review gates, golden screenshot policy |

## Repository layout

```
metro-os/
├── scope.md              # Source of truth
├── AGENTS.md               # Agent entrypoint
├── toolkits/               # Shared libraries
├── apps/                   # One folder per Android app (each has references/)
├── references/             # Global WP8.1 assets (design PDFs, device profiles)
├── scripts/                # Verification and install scripts
└── docs/                   # Harness and design docs
```

## Apps

| Tier | Apps |
|------|------|
| 0 — Shell | launcher, statusbar, navbar |
| 1 — Core | browser, notes, music |
| 2 — Extended | photos, calendar, mail, messaging, people, store, settings, calculator, clock, files |

Package naming: `com.metro.<app>`

## License

Typography uses **Noto Sans** (SIL Open Font License), bundled in `metro-ui-android`.
