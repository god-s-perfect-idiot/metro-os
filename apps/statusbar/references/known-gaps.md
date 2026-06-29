# Statusbar — known reference gaps

Blueprint pages that do not yet resolve to a high-fidelity WP8.1 capture in `images/`,
with the workaround used during implementation. See root `AGENTS.md` § Reference research.

| Blueprint page | Missing file | What it should show | Workaround |
|----------------|--------------|---------------------|------------|
| Progress tray state | `images/progress_dark.png` | Indeterminate accent progress affordance (sweeping dots) in the tray during a long operation | No public-domain capture of the live `StatusBarProgressIndicator` was found. Implement from the textual spec: Microsoft "Essential graphics / Progress" ([hh202884](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202884(v=vs.105))) + `blueprint.md` § Page 3 (accent indeterminate indicator left of the clock row). Reuse the layout/colors from `images/expanded_dark.png`. |

## Notes

- `images/collapsed_dark.png` is a real WP8.1 dark page (Settings). The status tray region (clock-only
  resting state) is at the very top; the capture is framed on the page content, so use it for the
  dark-theme tray context and rely on `blueprint.md` for exact collapsed-tray geometry.
