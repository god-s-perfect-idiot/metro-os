# Files — known reference gaps

High-fidelity WP8.1 Files captures could not be downloaded (Ars/Vox CDN 403, Windows Central/GadgetsToUse 404, Wayback misses). Development proceeds from the blueprint and official Microsoft / Ars write-ups in `web-resources.md`.

| Missing / low-fidelity file | Should show | Workaround |
|-----------------------------|-------------|------------|
| `pivots_dark_blue.png` | Files hub with pivot-style headers and folder/file list | Blueprint Page 1 + Ars Technica description of path + list chrome; use `MetroPivot` + `MetroListItem` per toolkit |
| `list_dark_blue.png` | Folder path line + rows with name, date, size, folder count badges | Blueprint Page 1 subtitle rules; Ars: “timestamp and file size”; Microsoft Devices: phone / SD roots |
| `permission_dark_blue.png` | First-run storage access gate | Blueprint Page 2; photos app `PermissionScreen` pattern adapted to all-files access copy |

When a licensed or device capture becomes available, drop it into `images/` using the filenames above and remove the matching row.
