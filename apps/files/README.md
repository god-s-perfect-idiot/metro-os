# Files

**Package:** `com.metro.files`  
**Tier:** 2

## Status

Harness docs only — Android project not scaffolded yet. This README is the implementation brief for the WP8.1 files app.

## App role

This app recreates the WP8.1 **Files** experience: a file explorer with pivot-based filtering for documents, music, pictures, and videos.

It should feel structured and lightweight, not like a modern Android file manager with drawers, cards, or highly nested chrome.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Storage Access Framework strategy understood before path-heavy work begins

## Screen inventory

### 1. Filter pivots

- Top-level pivots for documents, music, pictures, and videos
- Expected reference: `references/images/pivots_dark_blue.png`

### 2. Folder / file list surface

- Main listing experience
- Expected reference: `references/images/list_dark_blue.png`

### 3. Detail / action surface

- Optional in v1 if a file actions screen is necessary
- Expected reference: `references/images/detail_dark_blue.png`

## System functions and contracts

- Use Storage Access Framework and approved local file APIs
- Define what “browse” means in v1: filtered top-level collections, folder traversal, or both
- Keep file-type detection and icon mapping deterministic
- If edit/share/delete flows are deferred, document that explicitly

## UI and interaction guardrails

- Use `MetroListItem` for file rows
- Pivot controls the active file class filter
- No Material drawers, bottom sheets, or card tiles
- Breadcrumbs and navigation should stay text-forward and simple

## Data and state model

- `FileEntry`, `FileFilter`, `FolderPathState`, `PermissionState`
- Track active pivot, current folder/path, and SAF grant state independently

## Primary implementation order

1. Define SAF permission flow
2. Build filtered collection pivots
3. Build file/folder list rendering
4. Add navigation and breadcrumbs
5. Add only approved actions such as open/share if needed

## Test-critical user flows

1. Grant file access
2. Switch among content pivots
3. Browse folders/files
4. Preserve current path when moving in and out of child views
5. Handle empty/protected/error states clearly

## Reference and golden expectations

- `references/images/pivots_dark_blue.png`
- `references/images/list_dark_blue.png`
- `references/images/detail_dark_blue.png`

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
| Native broad filesystem browsing without friction | Android SAF and storage restrictions add permission friction | Preserve Metro information architecture while using SAF-compatible entry and browse flows |

## Agent postmortem

_None._
