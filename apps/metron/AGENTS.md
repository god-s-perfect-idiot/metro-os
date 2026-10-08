# Agent instructions — Metron (`com.metro.metron`)

**Tier 2** | Full [Mihon](https://github.com/mihonapp/mihon) fork. Authoritative UI chrome: `references/guides/blueprint.md`.

**Engine:** Entire Mihon tree (Apache-2.0). See `MIHON-FORK.txt`. Do not reintroduce Material
`NavigationSuiteScaffold` on home — use `com.metro.metron.ui.MetronHomeScaffold`.

Home: **4-pane `MetroPanorama`** over real `LibraryTab` / `UpdatesTab` / `HistoryTab` / `BrowseTab`.
More via app-bar menu. `lint-engine-exception` → lint scans `com.metro.*` only.

Verify: `../../scripts/verify-app.sh metron`

Build flags for local FOSS: `-Pdist=foss -Pinclude-telemetry=false`

App APK OTA is permanently disabled (no Mihon GitHub release listening). `-Penable-updater`
is ignored.
