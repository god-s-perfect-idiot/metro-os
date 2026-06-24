# Notes

**Package:** `com.metro.notes`  
**Tier:** 1

## Status

Harness docs only — Android project not scaffolded yet. This README is the implementation brief for the WP8.1 notes experience.

## App role

This app recreates **OneNote Mobile**-style note taking for metro-os: notebooks, sections, and pages with a Metro navigation model and a lightweight editor.

The emphasis is on WP8.1 structure and rhythm, not on building a modern feature-rich markdown or rich-text editor. In v1, notes are local-first and plain-text-first unless a richer behavior is explicitly approved.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Notes data model approved before editing UI deeply

## Screen inventory

### 1. Notebooks hub

- Landing surface showing notebooks and entry points
- Must feel like a Metro content hub rather than a generic documents list
- Expected reference: `references/images/notebooks_dark_blue.png`

### 2. Sections pivot

- Within a notebook, sections are navigated through a pivot
- Horizontal section switching should feel native to WP8.1
- Expected reference: `references/images/sections_dark_blue.png`

### 3. Page editor

- Edit a note page with page title and body content
- v1 prioritizes plain text with reliable autosave
- Expected reference: `references/images/editor_dark_blue.png`

## System functions and contracts

### Domain model

- `Notebook`
- `Section`
- `Page`

The hierarchy is strict: notebooks contain sections, sections contain pages. Avoid collapsing this into a flat generic documents model because navigation and back behavior depend on the hierarchy.

### Storage model

- Local storage only in v1
- Autosave is required
- Sync may exist only as a visible stub action until backend scope exists

### Navigation semantics

- Notebook list -> notebook -> section pivot -> page editor
- Back must unwind that hierarchy cleanly
- Preserve current section context when entering and leaving a page

### Editor contract

- Use Metro text controls, not Material inputs
- Support title and body editing
- If formatting is deferred, make that explicit in code and docs instead of implying rich text

## UI and interaction guardrails

- Keep typography large and left-aligned
- Use pivot only for section switching, not for unrelated top-level surfaces
- Avoid floating compose buttons; use bottom app bar actions instead
- Autosave should be quiet and non-disruptive
- Empty states should encourage creating the first notebook/section/page without introducing non-WP patterns

## Data and state model

- `Notebook`: id, title, order, created/updated timestamps
- `Section`: id, notebookId, title, order
- `Page`: id, sectionId, title, body, created/updated timestamps
- Keep editor draft state separate from persisted state so autosave can be debounced safely

## Primary implementation order

1. Define storage schema and repositories
2. Build notebooks landing surface
3. Build section pivot inside a notebook
4. Build page list and page editor
5. Add autosave and state restoration
6. Add sync stub affordance if still out of scope

## Test-critical user flows

1. Create notebook, section, and page
2. Edit title and body with autosave
3. Switch sections via pivot without losing state
4. Navigate back through page -> section -> notebook hierarchy
5. Relaunch and restore saved notes

## Reference and golden expectations

- `references/images/notebooks_dark_blue.png`
- `references/images/sections_dark_blue.png`
- `references/images/editor_dark_blue.png`
- `screenshots/golden/notebooks_dark_blue.png`
- `screenshots/golden/editor_dark_blue.png`

## Commands

```bash
cd apps/notes

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh notes
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Rich note formatting and cloud sync | Out of current v1 scope | Ship plain-text local notebooks first and surface sync as stub if shown in chrome |

## Agent postmortem

_None._
