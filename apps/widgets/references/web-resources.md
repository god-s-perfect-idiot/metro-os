# Widgets — web resources

WP8.1 / Windows Phone sources used for the widget catalog faces.

## Widget catalog (app chrome)

- Start grid density and 4-column footprints: `apps/launcher/references/guides/blueprint.md`
- Suite app title overline: `toolkits/metro-ui-android/METRO-UX-LANGUAGE.md` (`MetroAppTitle`)

## Time

- No stock WP8.1 system Time live tile — tray clock only (`apps/statusbar/references/guides/blueprint.md`)
- Third-party precedent (large digital Start clocks):
  - https://www.windowscentral.com/timeme-tile-windows-phone-81 — TimeMe Tile (time / date / weather / battery on Start)
  - Clock Hub coverage (same era) — giant Live tile clock on Start

## Battery

- Charge-level live face precedent (WP8.1 Battery Saver app percentage tile):
  - https://www.windowscentral.com/eight-tips-make-most-out-windows-phone-81 — pin Battery Saver; live tile reflects charge
  - https://www.windowscentral.com/windows-phone-811-update-build-14203-due-soon — real-time Battery Saver Live Tile (8.1.14203+)
  - https://nokiapoweruser.com/new-windows-phone-8-1-update-8-1-14203-hinted-by-battery-saver-changelog-big-update/ — changelog (real-time tile)

## Battery Saver

- Action Center / Quick Settings shield toggle (enable saver), distinct from the percentage Start tile:
  - https://www.thurrott.com/mobile/windows-phone/3623/windows-phone-tip-extend-your-range-with-battery-saver — shield when saver engaged
  - https://www.windowscentral.com/windows-phone-811-update-build-14203-due-soon — Battery Saver in Quick Settings (8.1.14203+)
- Android control: `Settings.Global` `low_power` (+ `low_power_sticky`) via `WRITE_SECURE_SETTINGS`; listen `PowerManager.ACTION_POWER_SAVE_MODE_CHANGED`; fallback `Settings.ACTION_BATTERY_SAVER_SETTINGS`

## Analog clock

- Suite accent live-tile language (flat face, no Material chrome): `toolkits/metro-ui-android/METRO-UX-LANGUAGE.md`
- Visual reference capture: `images/analog_clock_1x1_dark_crimson.png` (user-provided 1×1 dial)

## Torch

- No stock WP8.1 system Torch Start tile — Action Center flashlight quick action only
- Third-party lamp / flashlight apps with Start tiles:
  - https://www.windowscentral.com/ultimate-torch-windows-phone-flashlight-app-from-myappfree — Ultimate Torch (live tile / LED or white screen)
  - https://gadgetstouse.com/blog/2014/05/07/three-useful-flashlight-apps-windows-phone-devices/ — Flashlight-X / Quick Settings flashlight tile era
- Android control: `CameraManager.setTorchMode` (requires `CAMERA`; flash feature optional)

## Reference images

High-fidelity Start captures of some live faces are scarce in reusable form. Gaps and workarounds: [`known-gaps.md`](known-gaps.md).
