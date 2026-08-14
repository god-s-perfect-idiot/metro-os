package com.metro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val SkiaPointsPerInch = 72f
/** Camera Z ≈ 0.9× viewport width — matches CSS `perspective: 1000px` on a phone viewport. */
private const val PagePivotCameraWidthFactor = 0.9f
private const val PagePivotCameraDefaultInches = 8f

/**
 * Skia/RenderNode camera distance (inches) for a full-width [MetroPagePivotLoad].
 *
 * Small-tile flips use ~16×density, but that camera is so far from a full-screen page that
 * `rotationY` reads as a flat fade. Size from [widthPx] so the left-edge hinge stays visible.
 */
fun metroPagePivotCameraDistance(widthPx: Float): Float {
    if (widthPx <= 0f) return PagePivotCameraDefaultInches
    return (widthPx / SkiaPointsPerInch) * PagePivotCameraWidthFactor
}

/**
 * Full-screen app wrapper — page pivot load on launch; Back runs the flip-out
 * then invokes [onExit] (typically `activity.finish()`).
 *
 * Place inside [MetroTheme]. Nested `BackHandler`s take priority, so in-app
 * navigation can intercept Back before this shell exits the activity.
 */
@Composable
fun MetroAppPivotShell(
    modifier: Modifier = Modifier,
    onExit: () -> Unit,
    content: @Composable () -> Unit,
) {
    var exiting by remember { mutableStateOf(false) }

    BackHandler(enabled = !exiting) {
        exiting = true
    }

    BackHandler(enabled = exiting) {
        // Hold the activity until the flip-out finishes.
    }

    MetroPagePivotLoad(
        modifier = modifier.background(MetroTheme.colors.background),
        exiting = exiting,
        onExitComplete = onExit,
    ) {
        content()
    }
}

/**
 * WP8.1 page pivot load — enter swings inward from off-screen left
 * (`rotateY` [MetroTransitions.PagePivotLoadStartDegrees]° → 0°). Exit is a separate flip-out:
 * `rotateY` 0° → [MetroTransitions.PagePivotExitEndDegrees]° with
 * `scaleY` → [MetroTransitions.PagePivotExitScaleY] and fade.
 *
 * Pass a distinct [loadKey] when replacing page content so the enter animation runs again.
 * Set [exiting] for the flip-out; [onExitComplete] runs once that outro finishes.
 */
@Composable
fun MetroPagePivotLoad(
    modifier: Modifier = Modifier,
    loadKey: Any? = Unit,
    exiting: Boolean = false,
    onExitComplete: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val rotationY = remember {
        Animatable(if (exiting) 0f else MetroTransitions.PagePivotLoadStartDegrees)
    }
    val alpha = remember { Animatable(if (exiting) 1f else 0f) }
    val scaleY = remember { Animatable(1f) }
    LaunchedEffect(loadKey, exiting) {
        if (exiting) {
            rotationY.snapTo(0f)
            alpha.snapTo(1f)
            scaleY.snapTo(1f)
            coroutineScope {
                val fade = launch { alpha.animateTo(0f, MetroTransitions.pagePivotLoadTween()) }
                val pivot = launch {
                    rotationY.animateTo(
                        MetroTransitions.PagePivotExitEndDegrees,
                        MetroTransitions.pagePivotLoadTween(),
                    )
                }
                val stretch = launch {
                    scaleY.animateTo(
                        MetroTransitions.PagePivotExitScaleY,
                        MetroTransitions.pagePivotLoadTween(),
                    )
                }
                fade.join()
                pivot.join()
                stretch.join()
            }
            onExitComplete?.invoke()
        } else {
            rotationY.snapTo(MetroTransitions.PagePivotLoadStartDegrees)
            alpha.snapTo(0f)
            scaleY.snapTo(1f)
            coroutineScope {
                launch { alpha.animateTo(1f, MetroTransitions.pagePivotLoadTween()) }
                launch { rotationY.animateTo(0f, MetroTransitions.pagePivotLoadTween()) }
            }
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            this.rotationY = rotationY.value
            this.alpha = alpha.value
            this.scaleY = scaleY.value
            transformOrigin = if (exiting) {
                TransformOrigin(0f, 0.5f)
            } else {
                TransformOrigin(MetroTransitions.PagePivotLoadOriginX, 0.5f)
            }
            clip = false
            cameraDistance = metroPagePivotCameraDistance(size.width)
        },
    ) {
        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 640)
@Composable
private fun MetroPagePivotLoadPreview() {
    MetroTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MetroTheme.colors.background),
        ) {
            MetroPagePivotLoad(modifier = Modifier.fillMaxSize()) {
                Column(Modifier.padding(start = 12.dp, top = 48.dp)) {
                    MetroPageHeader(title = "settings")
                }
            }
        }
    }
}
