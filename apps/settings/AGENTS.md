# Agent instructions — Settings (`com.metro.settings`)

**Tier 2** | System settings — **writes** `MetroPreferences` and hosts `content://com.metro.system`. Reference: `references/guides/blueprint.md`.

Mirror WP8.1 settings: root `system` | `applications` pivot (all launchable apps + in-Settings app detail with toggles/uninstall), start+theme accent colour picker (20 WP8 colours), ease of access text size (10-step: WP8.1's 7 plus three smaller steps), brightness, storage sense, keyboard (launch `com.metro.keyboard`), and about (more info device details). Never deep-link into the Android Settings app.

**Critical**: Broadcast `THEME_CHANGED` on every theme/accent/font change. Consumer apps use `MetroSystemTheme`.

Verify: `../../scripts/verify-app.sh settings`
