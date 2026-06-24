# People — reference materials

**Start with [`guides/blueprint.md`](guides/blueprint.md)** — authoritative page spec. Images are visual aids only.

## Pages (from blueprint)

| Page | Blueprint | Image | Supplementary |
|------|-----------|-------|---------------|
| Hub · all (contacts) | [blueprint § Page 1](guides/blueprint.md) | _missing_ — [`known-gaps.md`](known-gaps.md) | [`guides/people-hub.md`](guides/people-hub.md) |
| Hub · what's new | [blueprint § Page 2](guides/blueprint.md) | _missing_ — [`known-gaps.md`](known-gaps.md) | [`guides/people-hub.md`](guides/people-hub.md) |
| Filter contacts | [blueprint § Page 3](guides/blueprint.md) | `images/pivot_dark_blue.jpg` | [`web-resources.md#filter-contacts`](web-resources.md) |
| Jump list | [blueprint § Page 4](guides/blueprint.md) | — | [`guides/people-hub.md`](guides/people-hub.md) |
| Contact detail · profile | [blueprint § Page 6](guides/blueprint.md) | `images/detail_dark_blue.jpg` | [`guides/contact-detail.md`](guides/contact-detail.md) |
| Contact detail · connect | [blueprint § Page 7](guides/blueprint.md) | `images/detail_connect_dark_blue.jpg` | [`guides/contact-detail.md`](guides/contact-detail.md) |
| Contact detail · what's new | [blueprint § Page 8](guides/blueprint.md) | `images/detail_whatsnew_dark_blue.jpg` | [`guides/contact-detail.md`](guides/contact-detail.md) |
| Contact detail · history | [blueprint § Page 9](guides/blueprint.md) | — | [`guides/contact-detail.md`](guides/contact-detail.md) |
| Add account | [blueprint § Page 10](guides/blueprint.md) | `images/accounts_dark_blue.jpg` | [`web-resources.md#accounts`](web-resources.md) |

## Folder layout

```
references/
├── README.md              # This file
├── known-gaps.md          # Missing reference screenshots
├── web-resources.md       # Curated links
├── guides/
│   ├── blueprint.md       # Authoritative — read first
│   ├── people-hub.md      # Panorama panes (all, what's new)
│   └── contact-detail.md  # Profile / connect / feeds / history
└── images/                # WP 8.1 screenshots (visual aid only)
```

## Image catalog

Screenshots from [All About Windows Phone](https://allaboutwindowsphone.com/features/item/20748_Proving_theres_still_full_Face.php) (Lumia 640, WP 8.1, 720×1280). Blueprint wins on conflict.

| File | Illustrates |
|------|-------------|
| `pivot_dark_blue.jpg` | Filter contacts — toggle + account checkboxes |
| `detail_dark_blue.jpg` | Contact profile pivot — photo, actions, app bar |
| `detail_connect_dark_blue.jpg` | Connect pivot — linked app tiles |
| `detail_whatsnew_dark_blue.jpg` | Per-contact what's new feed |
| `accounts_dark_blue.jpg` | Add an account provider list |
| `hub_dark_blue.jpg` | _Not yet added_ — all-pane contact list |

## Image naming

- Pattern: `<screen>_dark_blue.jpg` (or `.png`)
- Primary device profile: **768×1280** (Lumia 925 / xhdpi) — see `scope.md`
- Reference captures may be 720×1280 from community sources; scale proportionally

## Agent workflow

1. Read `guides/blueprint.md`
2. Read supplementary guides for the page you are building
3. Use `images/` for visual polish only
4. Check `known-gaps.md` before assuming a screenshot exists
5. Cite: `apps/people/references/guides/blueprint.md`

Golden emulator captures: `screenshots/golden/` (not yet scaffolded).
