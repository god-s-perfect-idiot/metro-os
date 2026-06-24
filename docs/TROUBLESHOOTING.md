# Troubleshooting — agent playbook

Common verification failures and how to fix them. Work top-to-bottom; do not skip diagnosis.

## Environment

### `verify-env.sh` fails: ANDROID_HOME not set

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"   # macOS default
export PATH="$PATH:$ANDROID_HOME/platform-tools"
```

Copy `local.properties.example` to `local.properties` in each app after scaffolding.

### No device for instrumented tests

Verify prints `WARN: no device; skipping instrumented tests`. This is acceptable for logic-only changes but **not** for UI work. Start your existing AVD:

```bash
./scripts/setup-emulators.sh    # prints METRO_AVD if you have one
./scripts/run-app.sh <app>      # auto-starts AVD if none connected
```

Or manually:

```bash
emulator -avd "${METRO_AVD:-Pixel_9}" &
adb wait-for-device
```

### `agent-overseer.sh` fails: not logged in

```bash
curl https://cursor.com/install -fsS | bash
agent login
# or: export CURSOR_API_KEY=...
./scripts/agent-overseer.sh launcher --verify-only
```

---

## Build failures

### `Could not find metro-ui-android`

Toolkit not published to local maven. From repo root:

```bash
./scripts/verify-toolkit.sh metro-ui-android
```

Apps must declare toolkit deps in `settings.gradle.kts` includeBuild or project dependency.

### Material dependency pulled transitively

Run `./gradlew :app:dependencies` and exclude Material at the dependency that pulls it. Never add Material to fix a build.

---

## Metro lint failures

### `BANNED_IMPORT: com.google.android.material`

Remove the import. Find the Metro equivalent in `toolkits/metro-ui-android`:

| Material | Metro replacement |
|----------|-------------------|
| `Button` | `MetroButton` |
| `TopAppBar` | `MetroPageHeader` |
| `FloatingActionButton` | App bar icon (WP8.1 has no FAB) |
| `Card` | Flat `Surface` with `#1F1F1F` / `#F2F2F2` secondary |
| `Snackbar` | `MetroMessageDialog` or tray progress |
| `ModalBottomSheet` | `MetroMessageDialog` or full-screen page |

### `CONTRAST_FAIL`

Check foreground against background per `scope.md` §2. Secondary text must use `#999999` (dark) or `#666666` (light).

### `TOUCH_TARGET_UNDER_44DP`

Increase clickable area with `Modifier.minimumInteractiveComponentSize()` or padding — visual glyph can stay 26dp inside 48dp touch area.

---

## Screenshot diff failures

### Diff > 2% entire screen

1. Open diff image in `apps/<name>/screenshots/captured/diff/`
2. Compare to `apps/<name>/references/images/` or `screenshots/golden/`
3. Common causes: wrong font, wrong margin (24dp), Material ripple, wrong accent

### Diff localized to status/nav bar

Shell apps may not be installed. Run `./scripts/install-shell.sh` and re-capture.

### Font rendering mismatch

Ensure Noto Sans is bundled in `metro-ui-android` assets. Emulator must have font accessible.

**Do not** update golden screenshots to match a wrong implementation.

---

## Motion profile failures

### Page transition wrong duration

Check `MetroTransitions.PageEnter` — must be `300.milliseconds` with `EaseOutCubic`.

### Missing tilt on list item

Wrap row in `MetroListItem` from toolkit; do not use raw `clickable` without tilt modifier.

---

## Test failures

### Theme not applied in test

Use `MetroThemeProvider(darkTheme = true, accent = MetroAccent.Blue)` in `@Preview` and test rule.

### `ThemeChangeReceiver` not firing

Register receiver in test with `IntentFilter(MetroBroadcasts.THEME_CHANGED)`.

---

## Escalation template

After 5 failed verify iterations, append to app `README.md`:

```markdown
## Agent postmortem — YYYY-MM-DD

**Task:** <what was attempted>
**Failures:** <list verify steps that never passed>
**Root cause hypothesis:** <best guess>
**Blocked on:** <human decision needed>
**Attempts:** 5
```

Then stop. Do not continue guessing.
