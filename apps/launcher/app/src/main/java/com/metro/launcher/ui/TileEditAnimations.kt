package com.metro.launcher.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.util.lerp
import com.metro.launcher.data.PinnedTileSize
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Disco Launcher hold threshold before edit mode arms (pointerdown timer). */
internal const val TILE_EDIT_HOLD_MS = 500L

/** Intro phase — page recess + selected tile pop (Disco `home-menu-back-intro`, 500ms). */
internal const val TILE_EDIT_INTRO_MS = 500
/** Steady phase — dim/shake settle (Disco `home-menu-back` transition, 500ms). */
internal const val TILE_EDIT_STEADY_MS = 500
/** Exit outro (Disco `home-menu-back-outro`, 500ms). */
internal const val TILE_EDIT_EXIT_MS = 500
internal const val TILE_EDIT_ENTER_MS = TILE_EDIT_INTRO_MS + TILE_EDIT_STEADY_MS

/** UI chrome (arrow row) tracks intro timing. */
internal const val TILE_EDIT_VISUAL_MS = TILE_EDIT_INTRO_MS

/** Disco `perspective: calc(var(--flow-perspective) * 2)` → 2000px. */
internal const val TILE_EDIT_PERSPECTIVE_DP = 2000f
/** Steady page recess — `translateZ(-50px)`. */
internal const val TILE_EDIT_PAGE_Z_STEADY_DP = -50f
/** Intro page recess — `translateZ(-5px)`. */
internal const val TILE_EDIT_PAGE_Z_INTRO_DP = -5f
/** Selected tile lift — `translateZ(10px)`. */
internal const val TILE_EDIT_ACTIVE_Z_DP = 10f

internal const val TILE_EDIT_INACTIVE_SCALE = 0.925f
internal const val TILE_EDIT_INACTIVE_ALPHA = 0.6f
/** Disco CSS `scale: 1.05` on the selected tile. */
internal const val TILE_EDIT_ACTIVE_SCALE = 1.05f
/** Intro pop — `scale(1.12)` on selected tile. */
internal const val TILE_EDIT_INTRO_ACTIVE_SCALE = 1.12f
/** Focus-change overshoot peak relative to [TILE_EDIT_ACTIVE_SCALE] (same 1.12 ratio as intro). */
internal const val TILE_EDIT_ACTIVE_FOCUS_BOUNCE_PEAK =
    TILE_EDIT_INTRO_ACTIVE_SCALE / TILE_EDIT_ACTIVE_SCALE
internal const val TILE_EDIT_ACTIVE_FOCUS_BOUNCE_MS = 450
internal const val TILE_EDIT_SCRIM_ALPHA = 0.55f

internal const val TILE_EDIT_RESIZE_MS = 250
internal const val TILE_EDIT_UNPIN_MS = 200
/** Brief pause before Perlin shake so enter scale/alpha settle first. */
internal const val TILE_EDIT_FLOAT_DELAY_MS = 120L

private val EditSteadyEasing = CubicBezierEasing(0.075f, 0.82f, 0.165f, 1f)
private val EditExitEasing = CubicBezierEasing(0.25f, 1f, 0.25f, 1f)

/** Maps [editProgress] 0..0.5 → intro phase 0..1. */
internal fun tileEditIntroPhase(editProgress: Float): Float =
    (editProgress * 2f).coerceIn(0f, 1f)

/** Maps [editProgress] 0.5..1 → steady phase 0..1. */
internal fun tileEditSteadyPhase(editProgress: Float): Float =
    ((editProgress - 0.5f) * 2f).coerceIn(0f, 1f)

/** Page recess depth in dp (Disco `translateZ` on `div.tile-list-page`). */
internal fun tileEditPageTranslationZDp(editProgress: Float): Float {
    val intro = tileEditIntroPhase(editProgress)
    val steady = tileEditSteadyPhase(editProgress)
    val withIntro = lerp(0f, TILE_EDIT_PAGE_Z_INTRO_DP, intro)
    return lerp(withIntro, TILE_EDIT_PAGE_Z_STEADY_DP, steady)
}

/** Maps Disco `translateZ` + `perspective` to an apparent uniform scale. */
internal fun tileEditPerspectiveScale(zDp: Float): Float =
    TILE_EDIT_PERSPECTIVE_DP / (TILE_EDIT_PERSPECTIVE_DP - zDp)

internal fun tileEditPageScale(editProgress: Float): Float =
    tileEditPerspectiveScale(tileEditPageTranslationZDp(editProgress))

/** Selected tile Z lift in dp (Disco `translateZ(10px)` on `.home-menu-selected`). */
internal fun tileEditActiveTranslationZDp(
    editProgress: Float,
    activeBlend: Float,
): Float {
    if (activeBlend <= 0f) return 0f
    val intro = tileEditIntroPhase(editProgress)
    val steady = tileEditSteadyPhase(editProgress)
    val lift = lerp(0f, TILE_EDIT_ACTIVE_Z_DP, intro.coerceAtLeast(steady))
    return lift * activeBlend
}

internal fun tileEditActiveFocusBounceSpec(): SpringSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    // Slightly softer than StiffnessMedium (~1500) for a slower settle.
    stiffness = 850f,
)

internal fun tileEditFocusScale(
    editProgress: Float,
    activeBlend: Float,
): Float {
    val intro = tileEditIntroPhase(editProgress)
    val steady = tileEditSteadyPhase(editProgress)
    val inactiveScale = lerp(1f, TILE_EDIT_INACTIVE_SCALE, steady)
    val activeScale = lerp(
        lerp(1f, TILE_EDIT_INTRO_ACTIVE_SCALE, intro),
        TILE_EDIT_ACTIVE_SCALE,
        steady,
    )
    return lerp(inactiveScale, activeScale, activeBlend)
}

internal fun tileEditFocusAlpha(
    editProgress: Float,
    activeBlend: Float,
): Float {
    val steady = tileEditSteadyPhase(editProgress)
    val inactiveAlpha = lerp(1f, TILE_EDIT_INACTIVE_ALPHA, steady)
    return lerp(inactiveAlpha, 1f, activeBlend)
}

/**
 * Two-phase edit visual timeline (Disco `home-menu-back-intro` → `home-menu-back`).
 * Returns 0 (normal) .. 1 (full steady edit).
 */
@Composable
internal fun rememberTileEditProgress(editMode: Boolean): Float {
    val haptic = LocalHapticFeedback.current
    val progress by animateFloatAsState(
        targetValue = if (editMode) 1f else 0f,
        animationSpec = if (editMode) {
            keyframes {
                durationMillis = TILE_EDIT_ENTER_MS
                0f at 0
                0.5f at TILE_EDIT_INTRO_MS using FastOutSlowInEasing
                1f at TILE_EDIT_ENTER_MS using EditSteadyEasing
            }
        } else {
            tween(TILE_EDIT_EXIT_MS, easing = EditExitEasing)
        },
        label = "tileEditProgress",
    )
    LaunchedEffect(editMode) {
        if (editMode) {
            delay(TILE_EDIT_INTRO_MS.toLong())
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    return progress
}

/** Deterministic Perlin noise (Disco `perlin.js`) for edit-mode tile jiggle. */
internal object TileEditPerlin {
    private val gradients = mutableMapOf<Long, Pair<Float, Float>>()

    fun reseed() {
        gradients.clear()
    }

    fun get(x: Float, y: Float): Float {
        val xf = floor(x).toInt()
        val yf = floor(y).toInt()
        val xDiff = x - xf
        val yDiff = y - yf

        val tl = dotProdGrid(x, y, xf, yf)
        val tr = dotProdGrid(x, y, xf + 1, yf)
        val bl = dotProdGrid(x, y, xf, yf + 1)
        val br = dotProdGrid(x, y, xf + 1, yf + 1)
        val xt = interp(xDiff, tl, tr)
        val xb = interp(xDiff, bl, br)
        return interp(yDiff, xt, xb)
    }

    private fun dotProdGrid(x: Float, y: Float, vx: Int, vy: Int): Float {
        val (gx, gy) = gradient(vx, vy)
        return (x - vx) * gx + (y - vy) * gy
    }

    private fun gradient(vx: Int, vy: Int): Pair<Float, Float> {
        val key = vx.toLong() shl 32 or (vy.toLong() and 0xFFFFFFFFL)
        return gradients.getOrPut(key) {
            val hash = ((vx * 374761393) xor (vy * 668265263)).toUInt()
            val angle = (hash % 6283u).toFloat() / 1000f
            cos(angle) to sin(angle)
        }
    }

    private fun smootherstep(x: Float): Float =
        x * x * x * (x * (x * 6f - 15f) + 10f)

    private fun interp(t: Float, a: Float, b: Float): Float =
        a + smootherstep(t) * (b - a)
}

internal data class TileEditShakeOffset(
    val offsetXDp: Float,
    val offsetYDp: Float,
) {
    companion object {
        val Still = TileEditShakeOffset(0f, 0f)
    }
}

/** Per-tile Perlin jiggle (Disco `homeTileEditShake` interval). */
internal fun tileEditShakeAt(seed: Int, timeSec: Float): TileEditShakeOffset {
    if (timeSec <= 0f) return TileEditShakeOffset.Still
    val hash = abs(seed) % 500
    val timeScale = 2000f + (abs(seed) % 1000)
    val distance = timeSec * 1000f / timeScale
    val amp = 1.5f * 10f
    return TileEditShakeOffset(
        offsetXDp = (TileEditPerlin.get(distance, hash.toFloat()) * amp * 10f).roundToInt() / 10f,
        offsetYDp = (TileEditPerlin.get(distance, (hash + 1000).toFloat()) * amp * 10f).roundToInt() / 10f,
    )
}

/** Directional resize overshoot (Disco `tile-size-change-anim-*`, 250ms). */
internal data class TileResizeOvershoot(
    val translationFractionX: Float,
    val translationFractionY: Float,
    val scaleMultiplier: Float,
)

internal fun tileResizeOvershoot(from: PinnedTileSize, to: PinnedTileSize): TileResizeOvershoot =
    when (to) {
        PinnedTileSize.TwoByTwo -> TileResizeOvershoot(
            translationFractionX = 0.10f,
            translationFractionY = 0f,
            scaleMultiplier = 1f,
        )
        PinnedTileSize.FourByTwo -> TileResizeOvershoot(
            translationFractionX = 0f,
            translationFractionY = 0.10f,
            scaleMultiplier = 1f,
        )
        PinnedTileSize.OneByOne -> when (from) {
            PinnedTileSize.FourByTwo -> TileResizeOvershoot(
                translationFractionX = -0.10f,
                translationFractionY = 0f,
                scaleMultiplier = 0.6f,
            )
            else -> TileResizeOvershoot(
                translationFractionX = 0f,
                translationFractionY = 0f,
                scaleMultiplier = 0.63f,
            )
        }
    }
