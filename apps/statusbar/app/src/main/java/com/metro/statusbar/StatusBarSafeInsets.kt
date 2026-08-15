package com.metro.statusbar

import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Geometry helpers so the Metro tray clock/icons clear physical display edges: cutouts,
 * waterfall bezels, and top rounded corners (API 31+). Pure math is kept separate from
 * [android.view.WindowInsets] so unit tests can cover the chord inset without Robolectric.
 */
internal object StatusBarSafeInsets {

    /**
     * Horizontal inset (px from [windowLeft] or [windowRight]) so a content band from
     * [contentTopY]…[contentBottomY] stays inside a top rounded corner quarter-circle.
     *
     * Samples the worst-case Y in that band (closest to the corner → largest inset).
     * Only the upper half of the corner circle matters — below [centerY] the screen is square.
     */
    fun topRoundedCornerInsetPx(
        radius: Int,
        centerX: Int,
        centerY: Int,
        contentTopY: Float,
        contentBottomY: Float,
        windowLeft: Int,
        windowRight: Int,
        isLeftCorner: Boolean,
    ): Float {
        if (radius <= 0) return 0f
        val r = radius.toFloat()
        val cy = centerY.toFloat()
        val top = minOf(contentTopY, contentBottomY)
        val bottom = maxOf(contentTopY, contentBottomY)
        // Upper half of the corner circle only (y from cy−r … cy).
        val sampleY = top.coerceIn(cy - r, cy)
        val sampleY2 = bottom.coerceIn(cy - r, cy)
        return maxOf(
            insetAtY(r, centerX, cy, sampleY, windowLeft, windowRight, isLeftCorner),
            insetAtY(r, centerX, cy, sampleY2, windowLeft, windowRight, isLeftCorner),
        )
    }

    private fun insetAtY(
        radius: Float,
        centerX: Int,
        centerY: Float,
        y: Float,
        windowLeft: Int,
        windowRight: Int,
        isLeftCorner: Boolean,
    ): Float {
        val dy = kotlin.math.abs(centerY - y)
        // Outside the circle → no chord. Exactly at the pole (dy == radius) halfChord is 0 and
        // the safe edge sits at centerX, which for a corner centered at `radius` is a full inset.
        if (dy > radius) return 0f
        val halfChord = sqrt(radius * radius - dy * dy)
        return if (isLeftCorner) {
            val safeLeft = centerX - halfChord
            maxOf(0f, safeLeft - windowLeft)
        } else {
            val safeRight = centerX + halfChord
            maxOf(0f, windowRight - safeRight)
        }
    }

    fun pxToDpCeil(px: Float, density: Float): Int =
        ceil(px / density).toInt().coerceAtLeast(0)
}
