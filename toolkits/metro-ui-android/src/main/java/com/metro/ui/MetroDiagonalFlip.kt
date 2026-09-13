package com.metro.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay

/** Start edge-on (WP PlaneProjection RotationX = 90) before flipping flat. */
private const val DiagonalFlipStartDegrees = 90f

/** Extra camera distance so rotationX reads as a 3D flip, not a squash. */
private const val DiagonalFlipCameraDistance = 16f

/**
 * WP8.1 diagonal flip used by the find-by-letter grid and the accents colour picker.
 *
 * Enter: [rotationX] 90° → 0° around the horizontal center, staggered by
 * [MetroJumpListLogic.diagonalIndex] × [MetroTransitions.JumpListFlipStaggerMs].
 * Exit: reverse flip 0° → 90° with the same stagger wave.
 *
 * When [hapticOnEnter] is true, each cell fires a light tick as its enter flip starts
 * (find-by-letter jump list).
 */
@Composable
fun MetroDiagonalFlip(
    cellIndex: Int,
    columns: Int,
    modifier: Modifier = Modifier,
    exiting: Boolean = false,
    hapticOnEnter: Boolean = false,
    content: @Composable () -> Unit,
) {
    val rotationX = remember { Animatable(DiagonalFlipStartDegrees) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(exiting, cellIndex, columns, hapticOnEnter) {
        val stagger =
            MetroJumpListLogic.diagonalIndex(cellIndex, columns) *
                MetroTransitions.JumpListFlipStaggerMs.toLong()
        if (exiting) {
            delay(stagger)
            rotationX.animateTo(
                targetValue = DiagonalFlipStartDegrees,
                animationSpec = MetroTransitions.jumpListFlipTween(),
            )
        } else {
            rotationX.snapTo(DiagonalFlipStartDegrees)
            delay(stagger)
            if (hapticOnEnter) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            rotationX.animateTo(
                targetValue = 0f,
                animationSpec = MetroTransitions.jumpListFlipTween(),
            )
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            this.rotationX = rotationX.value
            transformOrigin = TransformOrigin(0.5f, 0.5f)
            cameraDistance = DiagonalFlipCameraDistance * density
        },
    ) {
        content()
    }
}

/**
 * Total time for a full enter or exit wave across [cellCount] cells in a [columns]-wide grid
 * (last diagonal's stagger + flip duration).
 */
fun metroDiagonalFlipWaveDurationMs(cellCount: Int, columns: Int): Long {
    require(cellCount > 0) { "cellCount must be > 0" }
    require(columns > 0) { "columns must be > 0" }
    val maxDiagonal = (0 until cellCount).maxOf { MetroJumpListLogic.diagonalIndex(it, columns) }
    return maxDiagonal.toLong() * MetroTransitions.JumpListFlipStaggerMs +
        MetroTransitions.JumpListFlipMs
}
