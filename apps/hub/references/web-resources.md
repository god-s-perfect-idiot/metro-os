# Hub — web resources

Hub is a **metro-os original** about / suite-catalog app. There is no 1:1 WP8.1 “Hub” inbox app. Visual and interaction language is taken from Xbox Music panorama and general WP8.1 hub patterns.

## Hub panorama (home + apps)

| Source | URL | What it informs |
|--------|-----|-----------------|
| METRO-UX-LANGUAGE §6.8 Panorama | [`METRO-UX-LANGUAGE.md`](../../../toolkits/metro-ui-android/METRO-UX-LANGUAGE.md) | Peek, HubTitle, minimized app bar |
| Music hub blueprint | [`apps/music/references/guides/blueprint.md`](../../music/references/guides/blueprint.md) | Panoramic brand + hub link / tile panes |
| Music hub capture | `apps/music/references/images/hub_fullpage.png` | Layout reference copied into `images/panorama_dark_teal.png` |
| WP8.1 UI Design and Interaction Guide | (global `references/` PDFs when present) | Panorama vs pivot |

## Suite apps list / download

| Source | URL | What it informs |
|--------|-----|-----------------|
| GitHub Releases API | https://docs.github.com/en/rest/releases/releases#get-the-latest-release | Latest release + asset download URLs |
| metro-os releases | https://github.com/god-s-perfect-idiot/metro-os/releases | Live APK catalog for this suite |

## extras+info

| Source | URL | What it informs |
|--------|-----|-----------------|
| Lumia extras+info capture | `images/extras_info_dark_cyan.png` | Page title, Software release accent name + circular `i`, underlined policies link, component list, more info border button |
| Settings extras+info | [`apps/settings`](../../settings/README.md) | Suite already uses the `extras+info` title string for device about |

## Updater (phone update)

| Source | URL | What it informs |
|--------|-----|-----------------|
| WP8.1 Settings → phone update capture | `images/phone_update_dark_red.png` | SETTINGS overline, large title, Update status, Learn more, bordered action (Hub omits toggles / install time) |
| How to check for OS updates on your Windows Phone | https://www.windowscentral.com/how-to-update-your-windows-phone | Phone update entry path and install flow |

## Device

| Source | URL | What it informs |
|--------|-----|-----------------|
| Settings applications list | [`apps/settings`](../../settings/README.md) | Launchable installed-app discovery via `MetroAppDiscovery` |

## Reference images

| File | Source | Notes |
|------|--------|-------|
| `images/panorama_dark_teal.png` | Copied from Music `hub_fullpage.png` | Stand-in for panorama chrome until Hub-specific captures exist |
| `images/extras_info_dark_cyan.png` | Lumia extras+info screenshot (user-provided) | Layout reference; Hub paints **Metro Ruby** in crimson instead of cyan |
| `images/phone_update_dark_red.png` | WP8.1 phone update screenshot (user-provided) | Layout reference for Hub updater; omit checkboxes and preferred install time |
