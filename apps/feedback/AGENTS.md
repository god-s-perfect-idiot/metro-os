# Agent instructions — Feedback (`com.metro.feedback`)

**Tier 2** | Package: `com.metro.feedback`

Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first. Authoritative UI: [`references/guides/blueprint.md`](references/guides/blueprint.md).

## App role

Suite **feedback + diagnostics** — pivot `feedback` (title, collect metro logcat, open GitHub issue) and `metrics` (RAM / cache per `com.metro.*` app). Not a WP8.1 inbox clone; Settings Feedback naming + Storage Sense language.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| `metro-ui-android` verified | Yes |
| `metro-system-sdk` verified | Yes |
| Tier 0 shell | Recommended for visual parity |
| Tier 1 apps | No |

## Screens

| Screen | Pattern | Reference |
|--------|---------|-----------|
| Hub pivot | `MetroPivot` `feedback` \| `metrics` | `references/guides/blueprint.md` |
| feedback pane | `MetroTextBox` + `MetroBorderButton` | Blueprint § Page 2 |
| metrics pane | Usage bar + `MetroListItem` rows | Blueprint § Page 3 / SDK collage |

## WP8.1 rules

- Pivot only (2 headers). No panorama, no Material chrome
- Border buttons flush left (never full-width)
- `Modifier.metroNavBarPadding()` on shell root
- Icons via `MetroAppGlyphs.Feedback` / shared suite set

## Primary flows

1. Launch → pivot `feedback`
2. Enter title → collect metro logcat → submit → GitHub issue draft (+ share full logcat)
3. Swipe to `metrics` → view RAM / cache per metro app

## Verify

```bash
../../scripts/verify-app.sh feedback
```

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Full system logcat | `READ_LOGS` is privileged; third-party apps only see own UID without grant | Best-effort dump filtered to `com.metro.*`; document adb grant when available |
| GitHub file attach via URL | Issue create URLs cannot attach binaries | Prefill issue `body` query with logcat; clipboard backup if truncated |
| Cross-app RAM | App UID blocked from dumpsys/meminfo and other `/proc` | `MetroMemoryProvider` per suite app; sort by RAM desc |
| Per-app cache sizes | Needs `PACKAGE_USAGE_STATS` | Prompt → usage-access settings; show RAM without grant |
