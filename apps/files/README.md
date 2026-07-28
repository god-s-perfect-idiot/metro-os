# Files

**Package:** `com.metro.files`  
**Tier:** 2

## Status

Implemented (v1) — browse all readable storage, filter by content pivot, open files in their respective apps. Copy/move/rename/delete/share deferred.

## App role

This app recreates the WP8.1 **Files** experience: a file explorer over phone / SD storage, with metro-os **pivot filters** for documents, music, pictures, and videos (plus **all**).

It should feel structured and lightweight, not like a modern Android file manager with drawers, cards, or highly nested chrome.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- All-files / storage access strategy understood before path-heavy work begins

## Screen inventory

### 1. Filter pivots + browse list

- Pivots: `all`, `documents`, `music`, `pictures`, `videos`
- Volume root (`phone`, `sd card`) then folder traversal with path breadcrumb
- Expected reference: `references/images/pivots_dark_blue.png` / `list_dark_blue.png` (see `known-gaps.md`)

### 2. Permission gate

- All-files access (API 30+) or legacy read storage
- Expected reference: `references/images/permission_dark_blue.png` (gap)

## System functions and contracts

- **v1 browse:** full shared storage via `MANAGE_EXTERNAL_STORAGE` (legacy `READ_EXTERNAL_STORAGE` below API 30)
- Tap file → `ACTION_VIEW` through `FileProvider` (`com.metro.files.files`) so the system opens the matching app / chooser
- File-type detection via extension → MIME map in `FilesLogic` (deterministic)
- Deferred: copy / move / rename / delete / new folder / multi-select share / search

## UI and interaction guardrails

- Use `MetroListItem` for file rows
- Leading accent / type tiles before name (phone / SD glyphs at volume root; folder count badge; Office-style file badges)
- `MetroPivot` controls the active file class filter
- No Material drawers, bottom sheets, or card tiles
- Breadcrumbs stay text-forward (`phone > Pictures`)

## Data and state model

- `FileEntry`, `FileFilter`, `FolderPathState` helpers, `FilesState`
- Track active pivot, current folder path, and storage-access grant independently

## Primary implementation order

1. Storage permission / all-files flow
2. Filtered collection pivots
3. File/folder list rendering
4. Navigation and breadcrumbs
5. Open in respective apps

## Test-critical user flows

1. Grant file access
2. Switch among content pivots
3. Browse folders/files
4. Preserve current path when switching pivots
5. Open a file in another app; handle empty / no-handler states

## Reference and golden expectations

- `references/images/pivots_dark_blue.png`
- `references/images/list_dark_blue.png`
- `references/images/permission_dark_blue.png`

See `references/known-gaps.md` until captures are checked in.

## Commands

```bash
cd apps/files

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh files
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Native broad filesystem browsing without friction | Scoped storage / all-files permission | Preserve Metro IA; gate once with all-files access, then browse with `java.io.File` |
| Open file in associated app | Package visibility + URI grants | `FileProvider` + `ACTION_VIEW` chooser; `queries` for VIEW intents |
| Content-type hubs as first-class pivots | Stock WP8.1 Files was volume + folders only | metro-os adds `all` / type pivots as filters over the same tree (`scope.md`) |

## Agent postmortem

_None._
