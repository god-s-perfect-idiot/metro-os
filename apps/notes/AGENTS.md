# Agent instructions — Notes (`com.metro.notes`)

**Tier 1** | Package: `com.metro.notes`

Read [`scope.md`](../../scope.md) and root [`AGENTS.md`](../../AGENTS.md) first.

## App role

**OneNote Mobile** — notebooks → sections → pages hierarchy with pivot navigation.

## Build phase gate

| Prerequisite | Required |
|--------------|----------|
| Toolkits verified | Yes |
| Tier 0 shell passes verify | **Yes** |

## Screens

| Screen | Pattern | Reference |
|--------|---------|-----------|
| Notebooks hub | Panorama or list | `references/images/notebooks_dark_blue.png` |
| Sections | Pivot within notebook | `references/images/sections_dark_blue.png` |
| Page editor | Full page + app bar | `references/images/editor_dark_blue.png` |

## WP8.1 rules

- Hub/panorama for notebook list (sparse content, not dense lists on panorama)
- Pivot for sections within a notebook
- `MetroTextBox` for page title; rich text simplified to plain text v1
- App bar: sync (stub), add, delete via menu items
- Local storage only v1 (no Microsoft account)

## Primary flows

1. Create notebook → section → page
2. Edit page text; auto-save
3. Pivot between sections
4. Back navigates hierarchy

## Golden screenshots

```
screenshots/golden/notebooks_dark_blue.png
screenshots/golden/editor_dark_blue.png
```

## Verify

```bash
../../scripts/verify-app.sh notes
```
