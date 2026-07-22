# Agent instructions — Settings (`com.metro.settings`)

**Tier 2** | System settings — **writes** `MetroPreferences` and hosts `content://com.metro.system`. Reference: `references/guides/blueprint.md`.

Mirror WP8.1 settings: start+theme (dark/light + accent), accent grid (20 WP8 colours), ease of access text size (7-step).

**Critical**: Broadcast `THEME_CHANGED` on every change. Consumer apps use `MetroSystemTheme`.

Verify: `../../scripts/verify-app.sh settings`
