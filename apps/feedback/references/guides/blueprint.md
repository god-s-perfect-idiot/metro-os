# Feedback — blueprint

**Authoritative spec for this app.** Read this before `images/` or `web-resources.md`.

Agents implement pages, layout, and interactions exactly as described here. Screenshots in `images/` are visual aids only — they do not override this file.

## Pages

### Page 1 — Feedback hub (pivot)

Single-activity pivot shell. Opens immediately on launch.

- **Background:** theme background (`#000000` dark / `#FFFFFF` light)
- **Chrome:** `MetroAppTitle` overline `feedback` (ALL CAPS via toolkit). No large page title under it — pivot headers carry the section names.
- **Control model:** `MetroPivot` with two headers: **feedback** | **metrics**. Horizontal swipe between panes.
- **Insets:** `statusBarsPadding()` + `navigationBarsPadding()` + `metroNavBarPadding()` on the shell root.
- **No Material** chrome (no FAB, snackbars, cards, drawer).

### Page 2 — feedback pivot pane

Bug / suggestion report surface for the metro-os GitHub repo.

- **Layout (top → bottom, 12dp horizontal margins on fields):**
  1. Section copy (secondary text): short help — title the report, optionally collect metro logcat, then submit to open a GitHub issue.
  2. `MetroTextBox` — placeholder `title` (single line). Required for submit.
  3. `MetroBorderButton` — label `collect metro logcat`. Flush left, hugs label (never `fillMaxWidth`).
  4. Status line (secondary text) under the collect button: idle / collecting / ready with filename + size / error.
  5. `MetroBorderButton` — label `submit`. Enabled only when title is non-blank. Flush left.
  6. Result line (secondary text): submitting / opened issue draft / error.
- **Collect logcat:**
  - Runs `logcat -d` (dump, no follow) on a background dispatcher.
  - Keeps only lines that belong to metro suite processes (`com.metro.*` package / process names, plus common `Metro` tags).
  - Writes `metro-logcat-<timestamp>.txt` under the app cache (FileProvider-shareable).
  - Does not upload yet — only attaches locally for the next submit.
- **Submit:**
  - Builds a markdown issue body: title echo, device/build meta, and the **full** collected logcat (fenced code).
  - Opens the system browser to `issues/new?title=&body=` with the logcat in `body` (also copies the full markdown to the clipboard as backup if the URL must truncate).
- **Interactions:** no app bar icons in v1. Soft Back exits the app via `MetroAppPivotShell`.

### Page 3 — metrics pivot pane

Live RAM + cache usage for installed metro suite apps (Storage Sense / Battery Sense language).

- **Layout:**
  1. Section header `memory` (accent) + determinate usage bar (accent fill = used, muted track = free) + used / free / total rows.
  2. Section header `cache` (accent) + total metro-app cache size.
  3. Section header `apps` (accent) + scrollable list of metro packages.
- **App rows (`MetroListItem`):**
  - Leading = **64dp** accent square + suite glyph (`MetroAppGlyphs.forPackage`, fallback Feedback).
  - Title = app label (fallback package suffix).
  - Subtitle = two lines: `RAM <size> · <pct>% of RAM` then `cache <size>`.
  - Sorted by **RAM** descending, then cache, then label.
- **Data:**
  - System RAM from `ActivityManager.MemoryInfo`.
  - Per-app RAM from suite `MetroMemoryProvider` PSS probes (`content://{pkg}.metro.memory`). Stock Android blocks third-party `dumpsys meminfo`.
  - Per-app cache/data/code from `StorageStatsManager.queryStatsForPackage` when usage-stats access is granted; otherwise show cache as unavailable and a bordered `grant usage access` button that opens the system usage-access settings for this package.
- **Refresh:** reload when the metrics pane becomes visible and on an explicit pull via re-entering the pane (v1 — no swipe-to-refresh Material control).

## Images

| Image | Page | Notes |
|-------|------|-------|
| `sdk_leak_collage_storage_battery_dark.jpg` | metrics | WP8.1 SDK leak collage including Storage Sense / Battery Sense language |
| _(feedback pane)_ | feedback | See `known-gaps.md` — no 1:1 WP8.1 Feedback-report capture; use Settings Feedback + blueprint |

## Research notes

### Feedback

WP8.1 shipped a **Settings → Feedback** toggle (send device data to Microsoft), not a first-party GitHub bug reporter. This app is a **metro-os suite diagnostic** surface that reuses Metro form language (`MetroTextBox`, `MetroBorderButton`, pivot) to open issues on `god-s-perfect-idiot/metro-os`.

### Metrics

Visual language follows WP8.1 **Storage Sense** (determinate accent usage bar + used/free/total) and per-app breakdown ideas from Storage Sense / Battery Saver Sense lists. Scope is **metro suite packages only**, and includes **RAM** in addition to cache.

## Out of scope (v1)

- Multi-line description field / screenshots attach
- Authenticated GitHub API issue create (no shipped token)
- Clearing cache / force-stop from the metrics list
- Non-metro third-party process metrics
- Landscape-primary layout
