package com.metro.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
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
    /** Soft-key-style overshoot on app bar icon buttons after the bar creeps in. */
    const val AppBarButtonOvershootMs = 500
    private const val AppBarButtonOvershootPeakFraction = 0.7f
    /** Enter start position for app bar icons (fraction of button height, downward). */
    const val AppBarButtonStartOffsetFraction = 1.2f
    /** Overshoot peak above rest (fraction of button height, upward). */
    const val AppBarButtonOvershootPeakOffsetFraction = -0.2f
    const val StatusTrayExpandMs = 200
    const val StatusTrayCollapseMs = 200
    /** Per-icon delay when indicators drop in / exit upward (right → left). */
    const val StatusTrayIconStaggerMs = 90
    /** Hold after staggered enter before staggered exit. */
    const val StatusTrayAutoCollapseMs = 5000
    /** Action Center shade open / close. */
    const val ActionCenterOpenMs = 280
    const val ActionCenterCloseMs = 240
    const val TileFlipMs = 600
    /** First half of live-tile flip (0° → edge-on). */
    const val TileFlipHalfMs = TileFlipMs / 2
    /**
     * Spring settle on the second half (−90° → 0°): overshoot past flat, then correct.
     * Lower dampingRatio = larger, more readable bounce at landing.
     */
    const val TileFlipSettleDampingRatio = 0.45f
    const val TileFlipSettleStiffness = Spring.StiffnessLow
    /** Jump-list letter tile entrance flip (PlaneProjection RotationX). */
    const val JumpListFlipMs = 300
    /** Delay between successive diagonals when the jump grid enters. */
    const val JumpListFlipStaggerMs = 40
    /** Page pivot load — rotateY door-close from the left edge (PlaneProjection). */
    const val PagePivotLoadMs = 200
    /** Enter start angle — quarter swing; page is already mostly flat (not edge-on). */
    const val PagePivotLoadStartDegrees = 22.5f
    /** Hinge at the viewport left edge (fraction of width). */
    const val PagePivotLoadOriginX = 0f
    /** Enter slide start — +15% into the viewport; settles to rest (not off-screen). */
    const val PagePivotLoadStartTranslationXFraction = 0.15f
    /**
     * [MetroPagePivotSwing] enter start angle — deeper than [PagePivotLoadStartDegrees]
     * so a shared page hinge (Start tiles) reads clear foreshortening without an X slide.
     */
    const val PagePivotSwingStartDegrees = 50f
    /**
     * Closer camera than page-load (0.9× width) for [MetroPagePivotSwing] —
     * stronger perspective on the shared hinge.
     */
    const val PagePivotSwingCameraWidthFactor = 0.55f
    /** Exit hinge inset from the left — +15% into the viewport. */
    const val PagePivotExitOriginX = 0.15f
    /** Exit tilt — page recedes into the screen (`rotateY` 0° → −28°), no vertical stretch. */
    const val PagePivotExitEndDegrees = -28f
    /** Softer perspective on exit so the page does not shoot upward off-screen. */
    const val PagePivotExitCameraWidthFactor = 1.45f
    /** Exit slide — fraction of viewport width; negative pushes the left hinge off-screen. */
    const val PagePivotExitTranslationXFraction = -0.15f

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

    fun <T> tileFlipHalfTween(): FiniteAnimationSpec<T> = tween(
        durationMillis = TileFlipHalfMs,
        easing = PivotEasing,
    )

    /** Second flip half: spring landing with overshoot past 0° before settling flat. */
    fun <T> tileFlipSettleSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = TileFlipSettleDampingRatio,
        stiffness = TileFlipSettleStiffness,
    )

    fun <T> jumpListFlipTween(): FiniteAnimationSpec<T> = tween(
        durationMillis = JumpListFlipMs,
        easing = PageEasing,
    )

    fun <T> pagePivotLoadTween(): FiniteAnimationSpec<T> = tween(
        durationMillis = PagePivotLoadMs,
        easing = PageEasing,
    )

    fun <T> appBarCreepTween(): FiniteAnimationSpec<T> = tween(
        durationMillis = AppBarSlideMs,
        easing = PageEasing,
    )

    fun appBarButtonOvershootKeyframes(): FiniteAnimationSpec<Float> = keyframes {
        durationMillis = AppBarButtonOvershootMs
        AppBarButtonStartOffsetFraction at 0
        AppBarButtonOvershootPeakOffsetFraction at
            (AppBarButtonOvershootMs * AppBarButtonOvershootPeakFraction).toInt()
        0f at AppBarButtonOvershootMs using EaseOutCubic
    }

    const val ListTiltDegrees = 3f
}

/** Horizontal page slide offset helper for 300ms transitions. */
fun pageEnterOffset(fullWidth: Int): IntOffset = IntOffset(fullWidth, 0)

fun pageExitOffset(fullWidth: Int): IntOffset = IntOffset(fullWidth, 0)
