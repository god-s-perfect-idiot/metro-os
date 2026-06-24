# Agent instructions — metro-test-harness

**Phase 1 — build after `metro-system-sdk`.**

## Purpose

Automated verification: Metro lint, screenshot diff, motion timing. Consumed by `scripts/verify-app.sh`.

## Package

`com.metro.test`

## Required scripts (called by verify-app.sh)

| Script | Path | Purpose |
|--------|------|---------|
| `check-ux-language.sh` | `scripts/check-ux-language.sh` | UX spec present and linked from agent docs |
| `lint-metro.sh` | `scripts/lint-metro.sh` | Banned imports, contrast, touch targets |
| `screenshot-diff.sh` | `scripts/screenshot-diff.sh` | Golden comparison ≤ 2% |
| `motion-profile.sh` | `scripts/motion-profile.sh` | Transition duration ±50ms |

## lint-metro rules

See [`metro-ui-android/METRO-UX-LANGUAGE.md`](../metro-ui-android/METRO-UX-LANGUAGE.md) §11–§12 for control choice and anti-patterns.

1. Fail on `com.google.android.material` in `apps/*/app/src/**`
2. Fail on `androidx.compose.material3` in app UI (not toolkit internals)
3. Warn on text size < 15sp in composable literals
4. Fail on touch targets < 44dp where detectable via lint API

## screenshot-diff

- Golden dir: `apps/<name>/screenshots/golden/`
- Capture dir: `apps/<name>/screenshots/captured/` (gitignored)
- Diff dir: `apps/<name>/screenshots/captured/diff/`
- Threshold: 2% per image (RMSE or pixel count)
- Profile: lumia-925 unless app README overrides

## motion-profile

- Instrumentation test tag: `@MetroMotionTest`
- Assert durations from `scope.md` §9

## Gesture replay (JSON format)

```json
{
  "profile": "lumia-925",
  "steps": [
    { "action": "tap", "x": 400, "y": 600 },
    { "action": "swipe", "x1": 700, "y1": 640, "x2": 100, "y2": 640, "duration_ms": 300 }
  ]
}
```

Store scripts in `apps/<name>/test/gestures/`.

## Rules

- Harness must run headless in CI.
- Exit 0 = pass, non-zero = fail with stderr message.
- Never auto-update golden images.

## Verify

```bash
./scripts/verify-toolkit.sh metro-test-harness
```
