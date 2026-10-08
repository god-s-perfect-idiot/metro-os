# Camera

**Package:** `com.metro.camera`  
**Tier:** 2

## Status

Android project implemented — Open Camera engine fork (`CameraController` + `Preview`) with WP8.1 Microsoft Camera Compose chrome (viewfinder, More panel, photo/video settings, burst review).

## App role

Recreates the WP8.1 **Microsoft Camera** experience: full-bleed viewfinder, left quick-settings column (≤5), right photo / burst / video mode stack, More… customization panel, and burst review with retention caption.

Capture pipeline is forked from [Open Camera](http://opencamera.org.uk/) (GPL-3.0-or-later). Metro owns the UI.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Camera / mic permissions granted on device for capture flows

## Screen inventory

### 1. Viewfinder

- Left quick settings + right mode buttons + live preview
- Expected reference: `references/images/viewfinder_shortcuts_dark.png`

### 2. More / shortcut customization

- Trailing black panel over live preview
- Expected reference: `references/images/photo_settings_dark.jpg`

### 3. Photo / video settings lists

- Metro list pages for still and video options

### 4. Burst review

- Thumbnail strip + retention caption
- Expected reference: `references/images/burst_mode_dark.png`

## System functions and contracts

- `CAMERA`, `RECORD_AUDIO`, and media storage permissions as required by API level
- Save JPEG / MP4 via MediaStore (Camera Roll)
- Optional deep link into `com.metro.photos` for last capture
- No cross-app classpath imports — Photos via package/intent only

## UI and interaction guardrails

- Toolkit / Compose chrome only — no Open Camera `MainUI` / Material controls
- Circular outline icons; selected mode uses larger ring
- Status feedback is centered lowercase text, not Snackbar
- More panel is an edge pane, not a bottom sheet

## Data and state model

- `CaptureMode`: Photo | Burst | Video
- `QuickSetting` slots (≤5): Flash, SwitchCamera, Lenses, Iso, WhiteBalance, …
- `CameraUiState` held on Activity across config changes

## Primary implementation order

1. Vendor Open Camera Preview + CameraController
2. `MetroApplicationInterface` + permission / save hooks
3. Viewfinder Compose overlay
4. More + settings lists
5. Burst review

## Test-critical user flows

1. Grant camera permission → preview starts
2. Capture photo → thumbnail updates
3. Switch burst / video modes
4. Open More → assign viewfinder shortcuts
5. Flash / camera switch cycle

## Reference and golden expectations

- `references/images/viewfinder_shortcuts_dark.png`
- `references/images/viewfinder_engadget_dark.jpg`
- `references/images/photo_settings_dark.jpg`
- `references/images/burst_mode_dark.png`

## Commands

```bash
cd apps/camera

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh camera
```

## Licence

Engine sources under `app/src/main/java/net/sourceforge/opencamera/` are GPL-3.0-or-later (Open Camera). See `OPENCAMERA-GPL-3.0.txt`.

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|--------------------|------------|
| Hardware camera button half-press focus | Most Android devices lack dedicated shutter | Tap-to-focus + on-screen shutter |
| Lenses marketplace | No WP Store lenses | Stub empty lenses list |
| Unsaved burst auto-delete in OS Photos | Android has no burst container | App-local burst review; save selected frames only |
| Open Camera histogram / zebra / peaking | RenderScript-generated ScriptC not shipped | Features disabled; stub ScriptC for compile |

## Agent postmortem

_None._
