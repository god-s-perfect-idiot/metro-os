# News — reference materials

Visual and behavioral source material for implementing Bing/MSN News–style UI to WP8.1 fidelity.

Agents must read this folder **before** changing UI in `apps/news/`.

## Folder layout

```
references/
├── README.md           # This file — screen index and usage rules
├── web-resources.md    # Curated web guides and capture sources
├── images/             # WP8 Bing News screenshots
│   └── <screen>_<theme>.<ext>
└── guides/
    └── blueprint.md    # Authoritative page spec
```

## Screens

| Screen | Image | Notes |
|--------|-------|-------|
| Top story hero | `images/top_stories_dark.png` | Full-bleed photo + headline overlay |
| Headlines list | `images/headlines_dark.png` | Category bands + thumbnail rows |
| Article | `images/article_dark.png` | Hero, headline, byline, body |
| Topics / sources | `images/topics_dark.png` | Dual-pane sources + topics |
| Multi-screen overview | `images/bing_news_screens_dark.png` | Windows Central collage |

## Image catalog

| File | Source | License / attribution |
|------|--------|----------------------|
| `top_stories_dark.png` | [CNET Bing News review](https://www.cnet.com/reviews/bing-news-windows-phone-review/) | Editorial reference capture |
| `headlines_dark.png` | CNET Bing News review | Editorial reference capture |
| `article_dark.png` | CNET Bing News review | Editorial reference capture |
| `topics_dark.png` | [CNET Bing News gallery](https://www.cnet.com/pictures/bing-news-windows-phone/) | Editorial reference capture |
| `bing_news_screens_dark.png` | [Windows Central Bing Apps (archive)](https://web.archive.org/web/20190829170540/https://www.windowscentral.com/microsoft-announces-bing-apps-windows-phone-8) | Editorial reference capture |

## Agent workflow

1. Identify the screen (see `AGENTS.md` and app `README.md`).
2. Read `guides/blueprint.md` for the authoritative spec.
3. Open matching row in **Screens** above.
4. Compare implementation against `images/`.
5. Cite paths in commits/PRs:

```
Reference: apps/news/references/images/top_stories_dark.png
Guide: apps/news/references/web-resources.md#top-story--panorama-hub
```

Golden screenshots for verify live in `screenshots/golden/` (captured from emulator, not WP8.1 source).
