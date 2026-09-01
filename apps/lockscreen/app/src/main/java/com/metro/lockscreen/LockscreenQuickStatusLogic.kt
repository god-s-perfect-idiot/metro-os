package com.metro.lockscreen

import com.metro.ui.MetroAppGlyphs

/**
 * WP8.1 lock quick-status rules — five slots, counts capped at 99+.
 */
object LockscreenQuickStatusLogic {
    const val SLOT_COUNT = 5

    /** Display label for a notification count (WP8.1 caps at 99+). */
    fun formatCount(count: Int): String =
        when {
            count <= 0 -> ""
            count > 99 -> "99+"
            else -> count.toString()
        }

    /** Lock chrome only shows a quick-status glyph when there is at least one notification. */
    fun shouldShowQuickStatus(count: Int): Boolean = count > 0

    /** Monochrome notification glyph for a package (suite → tile override → notification art). */
    fun notificationIconRes(packageName: String): Int? =
        MetroAppGlyphs.notificationForPackage(packageName)
            ?: MetroAppGlyphs.forPackage(packageName)
            ?: MetroAppGlyphs.tileOverride(packageName)
}
