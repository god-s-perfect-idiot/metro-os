package com.metro.system

import android.content.Context
import android.content.Intent

/**
 * Shared spec for the Metro navigation bar overlay (`com.metro.navbar`).
 *
 * The navbar is drawn in a separate overlay window on top of the foreground app, so apps do not
 * receive it as a system window inset. Every app must instead reserve [HEIGHT_DP] of bottom space
 * whenever the navbar is enabled so its content is never occluded by the soft keys. Use
 * `Modifier.metroNavBarPadding()` from `metro-ui-android` rather than hard-coding this value.
 *
 * The navbar app only sets enabled when Android system navigation is in 3-button mode. Gesture /
 * edge-to-edge layouts keep the overlay off so apps are not cut off by conflicting insets.
 *
 * Fullscreen / immersive surfaces hide the bar through this contract (mirrors [MetroStatusBar]) —
 * never via a classpath dependency on the navbar app.
 */
object MetroNavBar {
    /** Package that owns and renders the navigation bar overlay. */
    const val PACKAGE = "com.metro.navbar"

    /** Height of the visible three-key bar, in dp (scope.md §Navigation bar). */
    const val HEIGHT_DP = 48

    /** Height of the slim strip shown when the bar has been swiped away, in dp. */
    const val REVEAL_STRIP_HEIGHT_DP = 6

    /** Re-read theme / background mode from preferences and redraw. */
    const val ACTION_REFRESH = "com.metro.navbar.action.REFRESH"

    /** Set the per-app navbar visibility mode (see MODE_OPAQUE / MODE_HIDDEN). */
    const val ACTION_SET_VISIBILITY = "com.metro.navbar.action.SET_VISIBILITY"

    /** String extra (one of the MODE_* values) for [ACTION_SET_VISIBILITY]. */
    const val EXTRA_VISIBILITY_MODE = "visibility_mode"

    /** Opaque theme-colored bar (WP8.1 default). */
    const val MODE_OPAQUE = "Opaque"

    /**
     * Fully hidden bar — use for fullscreen surfaces (photo viewer, in-call, immersive video).
     * Prefer [requestFullscreen] so exit restores [MODE_OPAQUE].
     */
    const val MODE_HIDDEN = "Hidden"

    /** Ask the bar to re-read preferences and redraw. */
    fun requestRefresh(context: Context) {
        context.sendBroadcast(request(ACTION_REFRESH))
    }

    /** Ask the bar to switch visibility mode. [mode] must be one of the MODE_* constants. */
    fun requestVisibility(context: Context, mode: String) {
        context.sendBroadcast(request(ACTION_SET_VISIBILITY).putExtra(EXTRA_VISIBILITY_MODE, mode))
    }

    /**
     * Hide or show the bar for a fullscreen surface.
     * [fullscreen] true → [MODE_HIDDEN]; false → [MODE_OPAQUE].
     * Pair with hiding Android navigation bars so nothing peeks through under the overlay.
     */
    fun requestFullscreen(context: Context, fullscreen: Boolean) {
        requestVisibility(context, if (fullscreen) MODE_HIDDEN else MODE_OPAQUE)
    }

    private fun request(action: String): Intent =
        Intent(action).setPackage(PACKAGE)
}
