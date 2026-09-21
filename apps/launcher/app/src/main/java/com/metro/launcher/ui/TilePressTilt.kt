package com.metro.launcher.ui

import kotlin.math.abs
import kotlin.math.hypot

/**
 * WP8.1 Start tile PointerDown tilt — the pressed side pushes into the screen as if the
 * opposite edge were anchored (WinJS / Splash phone tilt).
 *
 * Normalized touch (0–1, top-left origin) drives a rotation whose axis is perpendicular to
 * the center→touch vector. Center presses shrink more and rotate less; edge/corner presses
 * rotate more and shrink less.
 */
internal data class TilePressTiltPose(
    val rotationXDegrees: Float,
    val rotationYDegrees: Float,
    val scale: Float,
) {
    companion object {
        val Rest = TilePressTiltPose(0f, 0f, 1f)
    }
}

/** Peak rotation at a corner press (magnitude = 1). Edge-center is half of this. */
internal const val TILE_PRESS_TILT_MAX_DEGREES = 10f

/** Extra center depress — scale = 1 − (1 − magnitude) × this (center → 0.975). */
internal const val TILE_PRESS_TILT_MAX_SHRINK = 0.025f

/** PointerDown settle — matches prior tap-bounce dip timing. */
internal const val TILE_PRESS_TILT_DOWN_MS = 100

/** PointerUp / cancel spring-back. */
internal const val TILE_PRESS_TILT_UP_MS = 120

/** Camera multiplier so small rotationX/Y read as perspective, not a flat squash. */
internal const val TILE_PRESS_TILT_CAMERA_DISTANCE = 12f

/**
 * @param localX touch X in the tile's local pixels (0 = left)
 * @param localY touch Y in the tile's local pixels (0 = top)
 * @param widthPx tile width in pixels
 * @param heightPx tile height in pixels
 */
internal fun tilePressTiltAt(
    localX: Float,
    localY: Float,
    widthPx: Float,
    heightPx: Float,
): TilePressTiltPose {
    if (widthPx <= 0f || heightPx <= 0f) return TilePressTiltPose.Rest
    val x = (localX / widthPx).coerceIn(0f, 1f)
    val y = (localY / heightPx).coerceIn(0f, 1f)
    val nx = x - 0.5f
    val ny = y - 0.5f
    val magnitude = (abs(nx) + abs(ny)).coerceIn(0f, 1f)
    val angle = magnitude * TILE_PRESS_TILT_MAX_DEGREES
    val len = hypot(nx.toDouble(), ny.toDouble()).toFloat().coerceAtLeast(1e-4f)
    // graphicsLayer: +rotationY from +nx pushes the pressed horizontal edge away; vertical
    // needs the opposite sign so top/bottom press matches the finger, not the far edge.
    val rotationX = -ny / len * angle
    val rotationY = nx / len * angle
    val scale = 1f - (1f - magnitude) * TILE_PRESS_TILT_MAX_SHRINK
    return TilePressTiltPose(rotationX, rotationY, scale)
}
