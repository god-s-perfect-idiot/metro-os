# Agent instructions — Settings (`com.metro.settings`)

**Tier 2** | System settings — **writes** `MetroPreferences`. Reference: `references/images/`.

Mirror WP8.1 settings hierarchy: theme (dark/light), accent color picker (official palette only), nav bar color, font size.

**Critical**: This app owns theme writes; broadcast `THEME_CHANGED` on every change.

Verify: `../../scripts/verify-app.sh settings`
