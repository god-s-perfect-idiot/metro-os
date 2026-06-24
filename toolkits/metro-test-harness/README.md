# metro-test-harness

Verification toolkit for metro-os apps.

## Status

**Not scaffolded** — Phase 1. Shell scripts below are stubs until Gradle module exists.

## Scripts

```bash
./scripts/check-ux-language.sh
./scripts/check-references.sh <app-dir>
./scripts/lint-metro.sh <app-dir>
./scripts/screenshot-diff.sh <app-dir>
./scripts/motion-profile.sh <app-dir>
```

`check-ux-language.sh` verifies [`metro-ui-android/METRO-UX-LANGUAGE.md`](../metro-ui-android/METRO-UX-LANGUAGE.md) exists and is linked from agent docs. It runs in `verify-app.sh` and `verify-toolkit.sh`.

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)
