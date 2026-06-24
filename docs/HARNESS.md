# Harness engineering

This document defines how AI agents (and humans) use automation to keep the metro-os suite merge-ready with minimal manual QA.

## Philosophy

1. **Specifications are executable** — `scope.md` rules map to lint checks and screenshot diffs, not just prose.
2. **Fail fast, fail loud** — scripts exit non-zero with actionable messages; agents fix and retry.
3. **Isolation** — each app verifies independently; toolkits verify before any app depends on them.
4. **Self-correction** — agents loop implement → verify → fix without human intervention until green or 5 failures.

## Verification entrypoints

| Script | When to run | Output |
|--------|-------------|--------|
| `scripts/verify-env.sh` | Start of session / new machine | Checks JDK, Android SDK, adb |
| `scripts/verify-toolkit.sh <name>` | After toolkit changes | `toolkits/<name>/build/verify-report.json` |
| `scripts/verify-app.sh <name>` | After any app change | `apps/<name>/deploy/verify-report.json` |
| `scripts/run-app.sh <name>` | Dev loop / after build | Installs APK, launches on AVD |
| `scripts/verify-avd.sh <name>` | UI completeness on device | `apps/<name>/deploy/avd-report.json` |
| `scripts/agent-overseer.sh <name> "<task>"` | Autonomous implement+verify | `apps/<name>/deploy/overseer.log` |
| `scripts/verify-all.sh` | Pre-release / nightly | Aggregated report at `deploy/verify-suite.json` |
| `scripts/install-shell.sh` | Device testing | Installs Tier 0 APKs in order |

## verify-app.sh pipeline

Executed from repo root. Steps run **in order**; first failure stops the pipeline.

| Step | Command | Pass criteria |
|------|---------|---------------|
| 1. Structure | Internal checks | `apps/<name>/app/` exists, `AGENTS.md` and `references/` present |
| 2. UX language | `metro-test-harness check-ux-language` | `toolkits/metro-ui-android/METRO-UX-LANGUAGE.md` present and linked from agent docs |
| 3. References | `metro-test-harness check-references` | `references/README.md`, `web-resources.md`, `images/`, `guides/` |
| 4. Build | `./gradlew :app:assembleDebug` | Exit 0 |
| 5. Unit tests | `./gradlew :app:test` | Exit 0, 0 failures |
| 6. Instrumented | `./gradlew :app:connectedDebugAndroidTest` | Exit 0 if device present; WARN if skipped |
| 7. Metro lint | `metro-test-harness lint-metro` | No banned imports, contrast/touch-target violations |
| 8. Screenshots | `metro-test-harness screenshot-diff` | ≤ 2% pixel diff per golden |
| 9. Motion | `metro-test-harness motion-profile` | Transitions within ±50ms of spec |

### verify-report.json schema

```json
{
  "app": "launcher",
  "timestamp": "2026-06-24T12:00:00Z",
  "passed": false,
  "steps": [
    { "name": "build", "passed": true, "duration_ms": 12000 },
    { "name": "unit_tests", "passed": false, "duration_ms": 3000, "error": "..." }
  ],
  "failures": [
  {
      "step": "unit_tests",
      "message": "MetroTileTest.flipAnimation_duration_matchesSpec",
      "hint": "Expected 600ms, got 400ms. See scope.md §9 Motion."
    }
  ]
}
```

Agents must parse `failures[0]` first.

## Self-correction loop

```
┌──────────────────────────────────────────────────────────┐
│ 1. Read AGENTS.md + app AGENTS.md + scope.md section     │
│ 2. Implement smallest change that satisfies the task       │
│ 3. Run targeted test (unit) if logic-only change           │
│ 4. Run verify-app.sh <name>                              │
│ 5. If fail → read report → fix ONE issue → goto 4        │
│ 6. If 5 failures → write postmortem → STOP               │
│ 7. If pass → summarize what changed + verify output      │
└──────────────────────────────────────────────────────────┘
```

### Iteration discipline

- Fix **one** failure category per iteration (do not batch unrelated fixes).
- Re-run **full** verify after each fix (no partial skips).
- Do not weaken tests or raise diff thresholds to pass.
- Do not delete golden screenshots to pass diff.

## Per-app references policy

Each app maintains reference material under `apps/<name>/references/`:

| Path | Purpose |
|------|---------|
| `guides/blueprint.md` | **Authoritative** — pages, layout, interactions |
| `README.md` | Reading order; maps images to pages |
| `web-resources.md` | Supplementary external URLs |
| `images/` | Visual reference only — does not override blueprint |
| `guides/*.md` | Additional offline guides (e.g. edit-mode detail) |

Bootstrap: `./scripts/bootstrap-references.sh <name>`

Verify runs `check-references.sh` (requires `blueprint.md`; warns on empty placeholders or missing images).

Global shared assets (design PDFs, device profiles) remain in repo-root `references/`.

## Golden screenshot policy

| Action | Who | Process |
|--------|-----|---------|
| Add golden | Agent proposes | Capture on `lumia-925` profile; human approves PR |
| Update golden | Human only | Visual diff review; document reason in PR |
| Delete golden | Human only | Must reduce coverage — forbidden during agent self-correction |

Golden path: `apps/<name>/screenshots/golden/<screen>_<theme>_<accent>.png`

Example: `start_dark_blue.png`

## Device profiles

Screenshot reference sizes (logical profiles — **one system AVD is enough**):

| Profile | Resolution | Density | Use |
|---------|------------|---------|-----|
| `lumia-520` | 480×800 | hdpi | Minimum layout (optional) |
| `lumia-925` | 768×1280 | xhdpi | Primary screenshot reference |
| `lumia-1520` | 1080×1920 | xxhdpi | Large phone (optional) |

Primary screenshot profile: **lumia-925**. Reuse an existing Android Studio AVD (API 26+, portrait phone) instead of creating separate `metro_lumia_*` images.

```bash
./scripts/setup-emulators.sh   # lists AVDs, sets METRO_AVD
export METRO_AVD=Pixel_9       # optional override
emulator -avd "$METRO_AVD" &
```

## Dev loop (build + AVD)

One command to build, install, and launch on the emulator:

```bash
./scripts/run-app.sh <app-name>           # ensure AVD, build, install, launch
./scripts/run-app.sh <app-name> --verify  # + screenshots & UI probes
```

On-device verification writes:

- `apps/<name>/deploy/avd/*.png` — emulator screenshots
- `apps/<name>/deploy/avd-report.json` — install, focus, crash, testTag checks

Apps should expose `Modifier.testTag("metro_page_<id>")` on each blueprint page for automated UI probes.

## Agent overseer (Cursor CLI)

Orchestrates coding agent → `verify-app.sh` → `run-app.sh --verify` → read-only verifier agent:

```bash
curl https://cursor.com/install -fsS | bash   # if agent not installed
agent login
./scripts/agent-overseer.sh launcher "complete start menu per blueprint"
./scripts/agent-overseer.sh launcher --verify-only   # re-run harness + verifier only
```

Logs: `apps/<name>/deploy/overseer.log`  
Verifier output: `apps/<name>/deploy/verifier-verdict.json`

Prompt templates: `scripts/prompts/coding-agent.md`, `scripts/prompts/verifier-agent.md`

## Toolkit dependency order

Apps must not add a dependency on a toolkit that has not passed `verify-toolkit.sh`.

```
metro-ui-android  (no internal deps)
       ↓
metro-system-sdk  (depends on ui-android for theme types)
       ↓
metro-test-harness (depends on both)
       ↓
apps/*            (depend on all three)
```

## CI integration (future)

When CI is added, `verify-all.sh` becomes the required check. Until then, local verify is the gate.

## Metro UX language

Per-control WP8.1 shape and interaction rules live in [`toolkits/metro-ui-android/METRO-UX-LANGUAGE.md`](../toolkits/metro-ui-android/METRO-UX-LANGUAGE.md). Agents must read it before any UI work (after `scope.md`). It defines square vs round geometry, border text buttons, circular app bar icon press, tiles, pivots, and anti-patterns.

`check-ux-language.sh` runs in `verify-app.sh` and `verify-toolkit.sh` to ensure the spec file exists and stays linked from agent docs.

## Agent session checklist

- [ ] `verify-env.sh` passes
- [ ] Read `METRO-UX-LANGUAGE.md` for UI tasks
- [ ] Read target `AGENTS.md`
- [ ] Confirmed build phase allows this work
- [ ] Reference screenshots identified
- [ ] Changes confined to one app/toolkit
- [ ] `verify-app.sh` or `verify-toolkit.sh` passes
- [ ] No Material imports introduced
- [ ] README/AGENTS updated if behavior or commands changed
