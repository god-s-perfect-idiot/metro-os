# Keyboard

**Package:** `com.metro.keyboard`  
**Tier:** 0 (system IME)

WP8.1 touch keyboard (SIP) and keyboard settings for metro-os.

## Upstream

Vendored from **[FlorisBoard](https://github.com/florisboard/florisboard)** (Apache-2.0) — chosen as the best-maintained open-source Android keyboard (Kotlin, active releases, Apache license). See [`THIRD_PARTY.md`](THIRD_PARTY.md).

Metro-os changes:

- Application id `com.metro.keyboard`
- Default **Windows Phone 8.1** Snygg themes (`wp81_dark` / `wp81_light`)
- Suggestions-only prediction bar; suggestions enabled by default
- Metro Compose settings launcher (`com.metro.keyboard.MainActivity`)

## Status

IME + Metro settings scaffolded. Visual chrome via Snygg WP8.1 themes.

## Commands

```bash
cd apps/keyboard

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test

# From repo root
../../scripts/verify-app.sh keyboard
```

### Native build requirements

FlorisBoard’s `lib:native` needs:

- Android NDK (see `gradle/tools.versions.toml`)
- CMake + Ninja
- Rust (`rustup`) with Android targets

## Enable on device

1. Install the APK
2. Open **Keyboard** from the launcher
3. Tap **enable** → turn on Keyboard in system input settings
4. Tap **select** → choose Keyboard as the current IME
5. Focus any text field — SIP should match WP8.1 dark/light theme

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Keyboard always available as system SIP | User must enable/select IME | Metro setup banners deep-link to Android IME enabler/picker |
| Settings → keyboard inside system Settings hub | Separate APK | Dedicated Keyboard settings app with WP8.1 page hierarchy |
| FlorisBoard Material3 settings | Metro bans Material in app UI | Metro launcher settings only; engine Material retained behind `lint-engine-exception` |

## Agent postmortem

_None._
