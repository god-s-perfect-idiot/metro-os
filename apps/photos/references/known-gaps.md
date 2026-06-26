# Photos — known reference gaps

Items the blueprint requires but we do not yet have a high-fidelity capture for in `images/`.

## Missing / low-fidelity screenshots

| File | What we need | Workaround |
|------|--------------|------------|
| `viewer_dark_blue.jpg` | Full-screen WP8.1 photo viewer — edge-to-edge image, minimal chrome, horizontal flick between photos | Blueprint Page 3 + WP8.1 full-screen auto-mode note in `web-resources.md`. Implement black background, `ContentScale.Fit`, tap-to-toggle chrome. |
| `hub_all_pictures_dark_blue.jpg` | WP8.1-specific **all pictures** pivot header (not WP8 panorama) | Use `hub_dark_blue.jpg` grid layout + `pivot_albums_dark_blue.jpg` pivot chrome. Build 2014 article confirms thumbnail-first landing. |
| `permission_gate_dark_blue.jpg` | Android permission gate | No WP source; implement per blueprint Page 4 with Metro page header + accent links. |

## Caveats on shipped images

- **Era mix:** `hub_dark_blue.jpg` and date images are WP8 / early WP8.1 camera-roll captures; `pivot_albums_dark_blue.jpg` is WP8.1 SDK leak. Pivot chrome from albums image; grid layout from hub/date images.
- **Resolution:** Sources are 432×720–480×800, below the **768×1280** Lumia 925 profile. Use for layout/structure, not pixel diff.
- **Accent:** References use device accent (purple in date headers). App uses system accent from `MetroPreferences`.

## How to close gaps

1. Capture from WP8.1 GDR2+ device at **768×1280**.
2. Save as `<screen>_dark_<accent>.<ext>` per [`README.md`](README.md).
3. Update this file, blueprint image table, and `references/README.md` catalog.
