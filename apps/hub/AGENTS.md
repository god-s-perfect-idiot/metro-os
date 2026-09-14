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
| Suite apps list | Full page list (title / description / By) | Blueprint § Page 4 / `known-gaps.md` |
| App detail | Full page + download/share text app-bar buttons | Blueprint § Page 5 |
| Search | Full page + `MetroTextBox` catalog filter | Blueprint § Page 7 / Music explore |
| extras+info | Full page (Lumia extras+info language) | Blueprint § Page 6 / `extras_info_dark_cyan.png` |

## WP8.1 rules

- Hub = panorama only (3 panes: home / apps / featured); drill-ins = full page
- Minimized app bar on panorama
- HubLinks for home links; accent squares for quick-link tiles
- No Material cards / snackbars / FAB
- `Modifier.metroNavBarPadding()` on shell roots

## Primary flows

1. Launch → panorama brand `hub`, swipe `home` ↔ `apps` ↔ `featured`
2. Tap `metro os apps` → first-party catalog only (`first-party` / GitHub suite fallback)
3. Tap list row → app detail → download / share text buttons → install or share GitHub link
4. Tap `related apps` or second-party tile → `second-party` Firestore catalog
5. Tap `unofficial metro apps` or third-party tile → `third-party` Firestore catalog (may be empty)
6. Swipe to featured → 4 random apps from combined catalogs → tap opens detail
7. App-bar search → filter suite catalog → open app detail
8. Tap `extras+info` → Metro Ruby about page (latest release + buy me a coffee)

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
