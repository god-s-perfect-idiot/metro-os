# Agent instructions — Hub (`com.metro.hub`)

**Tier 2** | Package: `com.metro.hub`

Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first. Authoritative UI: [`references/guides/blueprint.md`](references/guides/blueprint.md).

## App role

**metro-os about / suite catalog** — panoramic hub (`home` + `apps`), GitHub latest-release APK list, download + install. Not a WP8.1 inbox clone; Music panorama language.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| Toolkits verified | Yes |
| Tier 0 shell passes verify | Yes |

## Screens

| Screen | Pattern | Reference |
|--------|---------|-----------|
| Hub | `MetroPanorama` + panoramic `hub` brand | `references/images/panorama_dark_teal.png` |
| Suite apps list | Full page list (title / description / By) | Blueprint § Page 3 / `known-gaps.md` |
| App detail | Full page + download app-bar icon | Blueprint § Page 4 |

## WP8.1 rules

- Hub = panorama only (2 panes); drill-ins = full page
- Minimized app bar on panorama
- HubLinks for home links; accent squares for quick-link tiles
- No Material cards / snackbars / FAB
- `Modifier.metroNavBarPadding()` on shell roots

## Primary flows

1. Launch → panorama brand `hub`, swipe `home` ↔ `apps`
2. Tap `all metro apps` → fetch latest GitHub release → list APKs
3. Tap list row → app detail → download icon → system install prompt
4. Tap category tile → filtered list (may be empty for 3rd/second party)

## Golden screenshots

```
screenshots/golden/hub_dark_blue.png
```

## Verify

```bash
../../scripts/verify-app.sh hub
```

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| No WP8.1 Hub inbox equivalent | N/A | Music panorama + metro-os catalog |
| Silent APK install | User must confirm package installer | `REQUEST_INSTALL_PACKAGES` + install intent |
