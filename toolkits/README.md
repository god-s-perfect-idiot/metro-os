# Toolkits

Shared libraries used by every app in metro-os. **Build and verify toolkits before any app.**

## Modules

| Module | Purpose | Verify |
|--------|---------|--------|
| [`metro-ui-android`](metro-ui-android/) | Compose components, theme, motion, typography | `./scripts/verify-toolkit.sh metro-ui-android` |
| [`metro-system-sdk`](metro-system-sdk/) | Preferences, intents, broadcasts, content URIs | `./scripts/verify-toolkit.sh metro-system-sdk` |
| [`metro-test-harness`](metro-test-harness/) | Lint, screenshot diff, motion profiler | `./scripts/verify-toolkit.sh metro-test-harness` |

## Dependency graph

```
metro-ui-android
       ↓
metro-system-sdk
       ↓
metro-test-harness
       ↓
apps/*
```

## Agent rules

1. Read [`metro-ui-android/METRO-UX-LANGUAGE.md`](metro-ui-android/METRO-UX-LANGUAGE.md) before any UI work.
2. Read the target toolkit's `AGENTS.md` before editing.
2. Public API changes require updating all consuming app `AGENTS.md` files if behavior changes.
3. No app-specific logic in toolkits — only reusable Metro/WP8.1 primitives.
4. Components map 1:1 to `scope.md` §6 Controls table and `METRO-UX-LANGUAGE.md` §13.

## Adding a new shared component

1. Confirm ≥ 2 apps need it (or is a core WP8.1 control).
2. Implement in `metro-ui-android` with preview + unit test.
3. Document in `metro-ui-android/README.md` API table.
4. Pass `verify-toolkit.sh metro-ui-android`.
