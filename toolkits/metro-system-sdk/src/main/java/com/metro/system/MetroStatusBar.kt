package com.metro.system

import android.content.Context
import android.content.Intent

/**
 * Shared spec for the Metro status bar (system tray) overlay (`com.metro.statusbar`).
 *
 * The status bar is drawn in a separate overlay window pinned to the top edge, on top of the
 * foreground app, so apps do not receive it as a system window inset. Any app that wants to change
 * the tray (request progress, opacity, hide it, or temporary shell fill) must talk to the overlay
 * through this contract — never via a direct classpath dependency on the status bar app
 * (scope.md § Inter-app communication).
 *
 * Requests are delivered as broadcasts targeted at [PACKAGE] and handled by an exported receiver in
 * the status bar app, mirroring the navbar's [MetroBroadcasts.ACTION_NAVBAR_QUERY] pattern.
 */
object MetroStatusBar {
    /** Package that owns and renders the status tray overlay. */
    const val PACKAGE = "com.metro.statusbar"

    /** Height of the WP8.1 system tray strip, in dp (scope.md § Status bar). */
    const val HEIGHT_DP = 32

    /**
     * Default how long indicators stay fully visible after the staggered enter finishes, in ms.
     * Tap or going home reveals icons; they then exit upward after this hold. The status bar
     * setup screen can override this with 3s / 5s / 10s.
     */
    const val AUTO_COLLAPSE_MS = 5000L

    /** Background opacity used when an app requests the translucent tray (scope.md § Status bar). */
    const val TRANSLUCENT_OPACITY = 0.5f

    /** Re-read theme/accent from [MetroPreferences] and redraw. */
    const val ACTION_REFRESH = "com.metro.statusbar.action.REFRESH"

    /** Show/hide the indeterminate accent progress affordance in the tray. */
    const val ACTION_SET_PROGRESS = "com.metro.statusbar.action.SET_PROGRESS"

    /** Set the per-app tray visibility mode (see MODE_OPAQUE / MODE_TRANSLUCENT / MODE_HIDDEN). */
    const val ACTION_SET_VISIBILITY = "com.metro.statusbar.action.SET_VISIBILITY"

    /**
     * Reveal the indicator row (same as tapping the tray). Fired when going home / Start so the
     * tray briefly shows status icons on the Start screen.
     */
    const val ACTION_EXPAND = "com.metro.statusbar.action.EXPAND"

    /**
     * Temporarily tint the tray fill to match a top shell overlay (toast / volume) so the strip
     * and banner read as one continuous band. Cleared when the overlay dismisses.
     */
    const val ACTION_SET_SHELL_FILL = "com.metro.statusbar.action.SET_SHELL_FILL"

    /** Boolean extra for [ACTION_SET_PROGRESS]. */
    const val EXTRA_PROGRESS = "progress"

    /** String extra (one of the MODE_* values) for [ACTION_SET_VISIBILITY]. */
    const val EXTRA_VISIBILITY_MODE = "visibility_mode"

    /**
     * Hex color (`#RRGGBB` or `#AARRGGBB`) for [ACTION_SET_SHELL_FILL]. Omit or pass null via
     * [requestShellFill] to clear that owner's temporary fill.
     */
    const val EXTRA_SHELL_FILL_COLOR = "shell_fill_color"

    /**
     * Owner id for [ACTION_SET_SHELL_FILL] — each overlay clears only its own request.
     * See [OWNER_NOTIFICATIONS] / [OWNER_VOLUME].
     */
    const val EXTRA_SHELL_FILL_OWNER = "shell_fill_owner"

    /** [EXTRA_SHELL_FILL_OWNER] value used by `com.metro.notifications` toasts. */
    const val OWNER_NOTIFICATIONS = "notifications"

    /** [EXTRA_SHELL_FILL_OWNER] value used by `com.metro.volume` HUD. Outranks notifications. */
    const val OWNER_VOLUME = "volume"

    /** Opaque theme-colored tray (WP8.1 default). */
    const val MODE_OPAQUE = "Opaque"

    /** Translucent tray at [TRANSLUCENT_OPACITY] over content. */
    const val MODE_TRANSLUCENT = "Translucent"

    /**
     * Fully hidden tray — use for fullscreen surfaces (photo viewer, in-call, immersive video).
     * Prefer [requestFullscreen] so exit restores [MODE_OPAQUE].
     */
    const val MODE_HIDDEN = "Hidden"

    /** Ask the tray to re-read preferences and redraw. */
    fun requestRefresh(context: Context) {
        context.sendBroadcast(request(ACTION_REFRESH))
    }

    /** Ask the tray to show or hide the indeterminate progress affordance. */
    fun requestProgress(context: Context, visible: Boolean) {
        context.sendBroadcast(request(ACTION_SET_PROGRESS).putExtra(EXTRA_PROGRESS, visible))
    }

    /** Ask the tray to switch visibility mode. [mode] must be one of the MODE_* constants. */
    fun requestVisibility(context: Context, mode: String) {
        context.sendBroadcast(request(ACTION_SET_VISIBILITY).putExtra(EXTRA_VISIBILITY_MODE, mode))
    }

    /**
     * Hide or show the tray for a fullscreen surface.
     * [fullscreen] true → [MODE_HIDDEN]; false → [MODE_OPAQUE].
     * Pair with hiding Android status bars so nothing peeks through under the overlay.
     */
    fun requestFullscreen(context: Context, fullscreen: Boolean) {
        requestVisibility(context, if (fullscreen) MODE_HIDDEN else MODE_OPAQUE)
    }

    /** Ask the tray to reveal indicators (tap-equivalent; used when returning home). */
    fun requestExpand(context: Context) {
        context.sendBroadcast(request(ACTION_EXPAND))
    }

    /**
     * Tint the tray fill to match a top shell overlay (toast accent, volume charcoal) so the
     * strip reads as one continuous band. Pass [colorHex] null to clear that [owner]'s request.
     * Volume outranks notifications when both are active.
     */
    fun requestShellFill(context: Context, owner: String, colorHex: String?) {
        val intent = request(ACTION_SET_SHELL_FILL).putExtra(EXTRA_SHELL_FILL_OWNER, owner)
        if (colorHex != null) {
            intent.putExtra(EXTRA_SHELL_FILL_COLOR, colorHex)
        }
        context.sendBroadcast(intent)
    }

    private fun request(action: String): Intent =
        Intent(action).setPackage(PACKAGE)
}
