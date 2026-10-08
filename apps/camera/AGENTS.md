# Agent instructions — Camera (`com.metro.camera`)

**Tier 2** | WP8.1 Microsoft Camera viewfinder. Requires Tier 0 shell gate per `scope.md`.

**Engine:** Open Camera `CameraController` + `Preview` (GPL-3.0-or-later). See `OPENCAMERA-FORK.txt` and `OPENCAMERA-GPL-3.0.txt`. Do not reintroduce Open Camera's Material-ish XML UI.

Reference: `references/images/`. Patterns: full-bleed preview + edge circular chrome; More panel (not bottom sheet).

Flows: viewfinder → capture / burst / video → More → photo|video settings → burst review. Verify: `../../scripts/verify-app.sh camera`
