# Clock — web resources

Curated links for how this app should look and behave on Windows Phone 8.1.

Add one `##` section per screen or feature. Keep URLs stable; use archive.org mirrors when originals disappear.

## General

| Resource | URL | Notes |
|----------|-----|-------|
| Lumia WP8.1 Update User Guide — Alarms | https://download-support.webapps.microsoft.com/ncss/PUBLIC/en_HK/smartwebpdf/GUID-B54A29DF-229A-4CE9-AAF0-8F374D014186/Lumia_Windows_Phone_8-1_Update_UG_en_GB.pdf | Official “Tap Alarms” / set time / snooze guidance |
| scope.md — Clock | [`scope.md`](../../../scope.md) | Suite inventory: alarms, world clock, timer, stopwatch |
| METRO-UX-LANGUAGE | [`toolkits/metro-ui-android/METRO-UX-LANGUAGE.md`](../../../toolkits/metro-ui-android/METRO-UX-LANGUAGE.md) | Toggle, app bar, multi-select, list picker, flip motion |

## Alarms list

| Resource | URL | Notes |
|----------|-----|-------|
| Device capture (this repo) | [`images/alarms_list_dark_amber.png`](images/alarms_list_dark_amber.png) | ALARMS / all, toggle rows, Add app bar |
| Lumia UG — Set an alarm | PDF § Clock and calendar | Add alarm, enable for wake |

## New / edit alarm

| Resource | URL | Notes |
|----------|-----|-------|
| Device capture (this repo) | [`images/alarms_new_dark_amber.png`](images/alarms_new_dark_amber.png) | Time / Repeats / Name fields + Save |
| Lumia UG tip — custom tone | PDF tip under Set an alarm | v1 skips picker; plays bundled generic tone |

## Time picker

| Resource | URL | Notes |
|----------|-----|-------|
| Device capture — tiles | [`images/alarms_time_picker_dark_amber.png`](images/alarms_time_picker_dark_amber.png) | Three accent tiles hour / min / AM·PM |
| Device capture — slider | [`images/alarms_time_picker_slider_dark_amber.png`](images/alarms_time_picker_slider_dark_amber.png) | Outlined neighbor column on active field |
| Jump-list flip primitive | `MetroDiagonalFlip` / `MetroTransitions.JumpListFlipMs` | timing reference for tile flip |

## Alarm repeats on

| Resource | URL | Notes |
|----------|-----|-------|
| Device capture (this repo) | [`images/alarms_repeats_dark_amber.png`](images/alarms_repeats_dark_amber.png) | Weekday checkboxes + Check/Close |
| Multi-select list | `MetroMultiSelectList` | Shared Apps Corner–style chrome |

## World clock

| Resource | URL | Notes |
|----------|-----|-------|
| W10 Mobile Alarms preview | https://www.wpxbox.com/whats-new-alarm-clock-windows-10-preview-phone/ | City list + compare (compare out of scope v1) |
| Microsoft Store — Alarms & Clock | https://web.archive.org/web/20171003140949/https:/www.microsoft.com/en-us/store/p/windows-alarms-clock/9wzdncrfj3pr | Feature list: world clock locations |

## Timer

| Resource | URL | Notes |
|----------|-----|-------|
| W10 Mobile Alarms preview | https://www.wpxbox.com/whats-new-alarm-clock-windows-10-preview-phone/ | Start / pause / reset on one surface |
| TimeSpanPicker (WP7 community) | https://learn.microsoft.com/en-us/archive/blogs/amit_chatterjee/extending-the-chronometer-windows-phone-7-app-introducing-nuget-pivot-and-the-timespanpicker-control | HH:MM:SS scrolling fields — metro-os uses accent tile columns instead |
| Device capture — alarm tiles | [`images/alarms_time_picker_dark_amber.png`](images/alarms_time_picker_dark_amber.png) | Visual stand-in for duration tile picker |

## Stopwatch

| Resource | URL | Notes |
|----------|-----|-------|
| W10 Mobile Alarms preview | https://www.wpxbox.com/whats-new-alarm-clock-windows-10-preview-phone/ | Laps / splits; share out of scope v1 |
| Win8.1 Alarms timer/stopwatch guide | https://www.digitalcitizen.life/how-set-timers-use-stopwatch-windows-81s-alarms-app/ | Desktop Alarms app behavior notes |

## Reference images

| File | Theme / accent | Source |
|------|----------------|--------|
| `alarms_list_dark_amber.png` | dark / amber | User WP8.1-style capture, 2026-11-06 |
| `alarms_new_dark_amber.png` | dark / amber | User WP8.1-style capture, 2026-11-06 |
| `alarms_time_picker_dark_amber.png` | dark / amber | User WP8.1-style capture, 2026-11-06 |
| `alarms_time_picker_slider_dark_amber.png` | dark / amber | User WP8.1-style capture, 2026-11-06 |
| `alarms_repeats_dark_amber.png` | dark / amber | User WP8.1-style capture, 2026-11-06 |
