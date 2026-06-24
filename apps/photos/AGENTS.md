# Agent instructions — Photos (`com.metro.photos`)

**Tier 2** | Hub + date/album pivots. Requires Tier 0 + Tier 1 gate per `scope.md`.

Reference: `references/images/`. Patterns: `MetroHub`, `MetroPivot`. Local `MediaStore` images only v1.

Flows: hub → albums pivot → photo viewer (full bleed, no rounded cards). Verify: `../../scripts/verify-app.sh photos`
