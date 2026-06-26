package com.metro.system

/**
 * Shared spec for the Metro navigation bar overlay (`com.metro.navbar`).
 *
 * The navbar is drawn in a separate overlay window on top of the foreground app, so apps do not
 * receive it as a system window inset. Every app must instead reserve [HEIGHT_DP] of bottom space
 * whenever the navbar is enabled so its content is never occluded by the soft keys. Use
 * `Modifier.metroNavBarPadding()` from `metro-ui-android` rather than hard-coding this value.
 */
object MetroNavBar {
    /** Package that owns and renders the navigation bar overlay. */
    const val PACKAGE = "com.metro.navbar"

    /** Height of the visible three-key bar, in dp (scope.md §Navigation bar). */
    const val HEIGHT_DP = 48

    /** Height of the slim strip shown when the bar has been swiped away, in dp. */
    const val REVEAL_STRIP_HEIGHT_DP = 6
}
