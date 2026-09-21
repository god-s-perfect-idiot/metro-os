# Hub — known gaps

| Missing / low-fidelity | Should show | Workaround |
|------------------------|-------------|------------|
| Dedicated Hub panorama capture (dark + accent) | Brand `hub`, panes home / apps / featured / local, HubLinks + quick-link tiles + featured list + local icon tiles | Use `images/panorama_dark_teal.png` (Music hub layout stand-in) + [`guides/blueprint.md`](guides/blueprint.md) |
| Local pane capture | Section header `local` + updater/device icon tiles | Music get-music tile language + blueprint § Page 8 |
| Device apps list capture | Installed app rows with optional bordered update | Blueprint § Page 10 |
| Suite apps list capture | Dense `MetroListItem` rows of release APKs with install affordance | Blueprint § Page 4 + live GitHub latest release |
| Search page capture | Music-style TextBox + filtered catalog rows | Blueprint § Page 7 + Music explore reference |
| Featured apps pane capture | Section header + 4 Store-style rows | Blueprint § Page 3 + suite list row language |
| Category-empty / 3rd-party captures | Empty-state tiles for second/third party | Blueprint stubs; tiles present, lists may be empty in v1 |
| Hub extras+info with Metro Ruby (crimson) | Same layout as Lumia extras+info but ruby accent + metro-os copy | Use `images/extras_info_dark_cyan.png` for structure; paint **Metro Ruby** with `AccentCrimson` |
