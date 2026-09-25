package com.metro.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import com.metro.ui.MetroTransitions
import com.metro.ui.metroPagePivotCameraDistance
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * App-list exit peel only (list is static on enter / resume).
 *
 * Disco continuum: `rotateY` 0°→−40°, slide −25% page width, opacity holds then drops.
 * Shared page-left hinge. Stagger owned by the caller (bottom→top; selected last).
 *
 * Vertically clipped to the row so peels do not bleed into letter markers above/below;
 * horizontal bleed is allowed so the swing reaches the screen edge (LazyColumn is
 * full-bleed with content padding).
 */
@Composable
internal fun AppListRowPivot(
    exitDelayMs: Long,
    exitSelected: Boolean,
    exiting: Boolean,
    pageWidthPx: Float,
    /** Distance from page left to this item's left — Disco `-offsetLeft`. */
    itemLeftInPagePx: Float,
    /** Bump to snap out of a leftover peel pose (resume / unlock). */
    restKey: Int = 0,
    modifier: Modifier = Modifier,
    onExitComplete: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val onExitCompleteState = rememberUpdatedState(onExitComplete)
    val exitSlideEndPx = pageWidthPx.coerceAtLeast(1f) * MetroTransitions.TilePivotExitSlideFraction
    val exitDurationMs =
        if (exitSelected) AppListPivotLogic.ExitSelectedMs else AppListPivotLogic.ExitDefaultMs
    val exitTween = MetroTransitions.tilePivotExitTween<Float>(exitDurationMs)
    // Enough for −25% slide + −40° perspective without clipping at the screen edge.
    val horizontalBleedPx = pageWidthPx.coerceAtLeast(1f)

    val outerRotationY = remember { Animatable(0f) }
    val slideTranslationX = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    var exitPose by remember { mutableStateOf(false) }

    val rotY = outerRotationY.value
    val slideX = slideTranslationX.value
    val layerAlpha = alpha.value

    LaunchedEffect(restKey) {
        if (restKey <= 0) return@LaunchedEffect
        outerRotationY.snapTo(0f)
        slideTranslationX.snapTo(0f)
        alpha.snapTo(1f)
        exitPose = false
    }

    LaunchedEffect(exiting, exitDelayMs, exitSelected, exitDurationMs, exitSlideEndPx) {
        if (!exiting) {
            outerRotationY.snapTo(0f)
            slideTranslationX.snapTo(0f)
            alpha.snapTo(1f)
            exitPose = false
            return@LaunchedEffect
        }
        outerRotationY.snapTo(0f)
        slideTranslationX.snapTo(0f)
        alpha.snapTo(1f)
        exitPose = true
        if (exitDelayMs > 0L) delay(exitDelayMs)
        coroutineScope {
            launch {
                outerRotationY.animateTo(
                    MetroTransitions.TilePivotExitEndDegrees,
                    exitTween,
                )
            }
            launch {
                slideTranslationX.animateTo(exitSlideEndPx, exitTween)
            }
        }
        alpha.snapTo(0f)
        onExitCompleteState.value?.invoke()
    }

    Box(
        modifier = modifier
            // Outer clip after transform: keep peels inside this row's vertical band so
            // apps do not slide out of the letter marker above, but allow horizontal bleed
            // to the screen edge.
            .drawWithContent {
                clipRect(
                    left = -horizontalBleedPx,
                    top = 0f,
                    right = size.width + horizontalBleedPx,
                    bottom = size.height,
                ) {
                    this@drawWithContent.drawContent()
                }
            }
            .graphicsLayer {
                this.rotationY = rotY
                this.alpha = layerAlpha
                val layerWidth = size.width.coerceAtLeast(1f)
                transformOrigin = TransformOrigin(
                    pivotFractionX = -itemLeftInPagePx / layerWidth,
                    pivotFractionY = 0.5f,
                )
                if (exitPose) {
                    translationX = slideX
                }
                cameraDistance = metroPagePivotCameraDistance(
                    widthPx = pageWidthPx.coerceAtLeast(layerWidth),
                    widthFactor = if (exitPose) {
                        MetroTransitions.PagePivotExitCameraWidthFactor
                    } else {
                        MetroTransitions.PagePivotSwingCameraWidthFactor
                    },
                )
                clip = false
            },
    ) {
        content()
    }
}
