# Music third-party

## zemer-cipher

Vendored from [ZemerTeam/zemer-cipher](https://github.com/ZemerTeam/zemer-cipher) (GPL-3.0).

Metrolist and zemer-app use this library to:

1. Deobfuscate WEB_REMIX `signatureCipher` URLs
2. Transform the googlevideo `n` throttle parameter
3. Self-heal YouTube `player_ias` rotations via remote `player_configs.json`

`apps/music` includes it as a Gradle composite build (`includeBuild("third_party/zemer-cipher")`).

Plugin versions in the vendored root `build.gradle.kts` are pinned to the music host AGP/Kotlin so the composite resolves.
