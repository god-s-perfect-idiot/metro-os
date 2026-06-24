package com.metro.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * WP8.1 motion constants from scope.md §9.
 */
object MetroTransitions {
    const val PageTransitionMs = 300
    const val PivotSwitchMs = 250
    const val ListTiltMs = 150
    const val AppBarSlideMs = 200
    const val StatusTrayExpandMs = 200
    const val StatusTrayCollapseMs = 200
    const val StatusTrayAutoCollapseMs = 8000
    const val TileFlipMs = 600

    /** WP NavigationThemeTransition ease-out cubic approximation. */
    val PageEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)

    val PivotEasing: Easing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)

    fun <T> pageTween(): FiniteAnimationSpec<T> = tween(
        durationMillis = PageTransitionMs,
        easing = PageEasing,
    )

    fun <T> pivotTween(): FiniteAnimationSpec<T> = tween(
        durationMillis = PivotSwitchMs,
        easing = PivotEasing,
    )

    fun <T> tileFlipTween(): FiniteAnimationSpec<T> = tween(
        durationMillis = TileFlipMs,
        easing = PivotEasing,
    )

    const val ListTiltDegrees = 3f
}

/** Horizontal page slide offset helper for 300ms transitions. */
fun pageEnterOffset(fullWidth: Int): IntOffset = IntOffset(fullWidth, 0)

fun pageExitOffset(fullWidth: Int): IntOffset = IntOffset(fullWidth, 0)
