# Agent instructions — News (`com.metro.news`)

**Tier 2** | Bing/MSN News–style panorama. Requires Tier 0 shell gate per `scope.md`.

**Data:** Public RSS (not a Google News fork). Do not pull in Material news clients.

Reference: `references/images/`. Patterns: `MetroPanorama` + hero overlay; `MetroSubpageHost` for article / topics.

Flows: hub hero → headlines/categories → article summary → open in browser; app-bar refresh + topics.

Verify: `../../scripts/verify-app.sh news`
