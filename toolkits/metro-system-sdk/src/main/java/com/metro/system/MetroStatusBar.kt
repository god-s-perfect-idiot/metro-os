package com.metro.system

import android.content.Context
import android.content.Intent

/**
 * Shared spec for the Metro status bar (system tray) overlay (`com.metro.statusbar`).
 *
 * The status bar is drawn in a separate overlay window pinned to the top edge, on top of the
 * foreground app, so apps do not receive it as a system window inset. Any app that wants to change
 * the tray (request progress, opacity, or hide it) must talk to the overlay through this contract —
 * never via a direct classpath dependency on the status bar app (scope.md § Inter-app communication).
 *
 * Requests are delivered as broadcasts targeted at [PACKAGE] and handled by an exported receiver in
 * the status bar app, mirroring the navbar's [MetroBroadcasts.ACTION_NAVBAR_QUERY] pattern.
 */
object MetroStatusBar {
    /** Package that owns and renders the status tray overlay. */
    const val PACKAGE = "com.metro.statusbar"

    /** Height of the WP8.1 system tray strip, in dp (scope.md § Status bar). */
    const val HEIGHT_DP = 32

    /** Auto-collapse timeout after the expanded indicators are revealed, in ms. */
    const val AUTO_COLLAPSE_MS = 8000L

    /** Background opacity used when an app requests the translucent tray (scope.md § Status bar). */
    const val TRANSLUCENT_OPACITY = 0.5f

    /** Re-read theme/accent from [MetroPreferences] and redraw. */
    const val ACTION_REFRESH = "com.metro.statusbar.action.REFRESH"

    /** Show/hide the indeterminate accent progress affordance in the tray. */
    const val ACTION_SET_PROGRESS = "com.metro.statusbar.action.SET_PROGRESS"

    /** Set the per-app tray visibility mode (see [MODE_OPAQUE]/[MODE_TRANSLUCENT]/[MODE_HIDDEN]). */
    const val ACTION_SET_VISIBILITY = "com.metro.statusbar.action.SET_VISIBILITY"

    /** Boolean extra for [ACTION_SET_PROGRESS]. */
    const val EXTRA_PROGRESS = "progress"

    /** String extra (one of the `MODE_*` values) for [ACTION_SET_VISIBILITY]. */
    const val EXTRA_VISIBILITY_MODE = "visibility_mode"

    /** Opaque theme-colored tray (WP8.1 default). */
    const val MODE_OPAQUE = "Opaque"

    /** Translucent tray at [TRANSLUCENT_OPACITY] over content. */
    const val MODE_TRANSLUCENT = "Translucent"

    /** Fully hidden tray (clock still considered essential — use sparingly). */
    const val MODE_HIDDEN = "Hidden"

    /** Ask the tray to re-read preferences and redraw. */
    fun requestRefresh(context: Context) {
        context.sendBroadcast(request(ACTION_REFRESH))
    }

    /** Ask the tray to show or hide the indeterminate progress affordance. */
    fun requestProgress(context: Context, visible: Boolean) {
        context.sendBroadcast(request(ACTION_SET_PROGRESS).putExtra(EXTRA_PROGRESS, visible))
    }

    /** Ask the tray to switch visibility mode. [mode] must be one of the `MODE_*` constants. */
    fun requestVisibility(context: Context, mode: String) {
        context.sendBroadcast(request(ACTION_SET_VISIBILITY).putExtra(EXTRA_VISIBILITY_MODE, mode))
    }

    private fun request(action: String): Intent =
        Intent(action).setPackage(PACKAGE)
}
