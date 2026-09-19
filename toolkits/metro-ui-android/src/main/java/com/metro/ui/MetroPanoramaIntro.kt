package com.metro.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Panorama hub brand intro — slides in from the right with fade
 * ([MetroTransitions.PanoramaBrandStartTranslationXFraction]× width → 0).
 *
 * Pair with [MetroPanoramaBodyEnter] on the panorama body. Re-runs when [loadKey] changes
 * unless [skipEnter] is true (keep content at rest — e.g. returning to an in-memory hub).
 * [onEnterComplete] runs after the enter finishes (not when [skipEnter]).
 */
@Composable
fun MetroPanoramaBrandEnter(
    modifier: Modifier = Modifier,
    loadKey: Any? = Unit,
    skipEnter: Boolean = false,
    onEnterComplete: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val translationXFraction = remember(loadKey) {
        Animatable(
            if (skipEnter) 0f else MetroTransitions.PanoramaBrandStartTranslationXFraction,
        )
    }
    val alpha = remember(loadKey) { Animatable(if (skipEnter) 1f else 0f) }
    LaunchedEffect(loadKey, skipEnter) {
        if (skipEnter) {
            translationXFraction.snapTo(0f)
            alpha.snapTo(1f)
            return@LaunchedEffect
        }
        translationXFraction.snapTo(MetroTransitions.PanoramaBrandStartTranslationXFraction)
        alpha.snapTo(0f)
        delay(MetroTransitions.PanoramaBrandEnterDelayMs)
        launch { alpha.animateTo(1f, MetroTransitions.panoramaBrandEnterTween()) }
        translationXFraction.animateTo(0f, MetroTransitions.panoramaBrandEnterTween())
        onEnterComplete?.invoke()
    }
    Box(
        modifier = modifier.graphicsLayer {
            translationX = translationXFraction.value * size.width.coerceAtLeast(1f)
            this.alpha = alpha.value
        },
    ) {
        content()
    }
}

/**
 * Panorama hub body intro — left-hinge `rotateY`
 * ([MetroTransitions.PanoramaBodyStartDegrees]° → 0°) plus slide from
 * [MetroTransitions.PanoramaBodyStartTranslationXFraction]× width, with fade.
 *
 * Matches the Metro Spotify carousel enter. Pair with [MetroPanoramaBrandEnter] for the
 * giant panoramic brand. Re-runs when [loadKey] changes unless [skipEnter] is true.
 * [onEnterComplete] runs after the enter finishes (not when [skipEnter]).
 */
@Composable
fun MetroPanoramaBodyEnter(
    modifier: Modifier = Modifier,
    loadKey: Any? = Unit,
    skipEnter: Boolean = false,
    onEnterComplete: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val rotationY = remember(loadKey) {
        Animatable(if (skipEnter) 0f else MetroTransitions.PanoramaBodyStartDegrees)
    }
    val translationXFraction = remember(loadKey) {
        Animatable(
            if (skipEnter) 0f else MetroTransitions.PanoramaBodyStartTranslationXFraction,
        )
    }
    val alpha = remember(loadKey) { Animatable(if (skipEnter) 1f else 0f) }
    LaunchedEffect(loadKey, skipEnter) {
        if (skipEnter) {
            rotationY.snapTo(0f)
            translationXFraction.snapTo(0f)
            alpha.snapTo(1f)
            return@LaunchedEffect
        }
        rotationY.snapTo(MetroTransitions.PanoramaBodyStartDegrees)
        translationXFraction.snapTo(MetroTransitions.PanoramaBodyStartTranslationXFraction)
        alpha.snapTo(0f)
        delay(MetroTransitions.PanoramaBodyEnterDelayMs)
        launch { alpha.animateTo(1f, MetroTransitions.panoramaBodyEnterTween()) }
        launch { rotationY.animateTo(0f, MetroTransitions.panoramaBodyEnterTween()) }
        translationXFraction.animateTo(0f, MetroTransitions.panoramaBodyEnterTween())
        onEnterComplete?.invoke()
    }
    Box(
        modifier = modifier.graphicsLayer {
            this.rotationY = rotationY.value
            this.alpha = alpha.value
            translationX = translationXFraction.value * size.width.coerceAtLeast(1f)
            transformOrigin = TransformOrigin(
                pivotFractionX = 0f,
                pivotFractionY = 0.5f,
            )
            clip = false
            cameraDistance = metroPagePivotCameraDistance(widthPx = size.width)
        },
    ) {
        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 640)
@Composable
private fun MetroPanoramaIntroPreview() {
    MetroTheme {
        MetroPanoramaBrandEnter {
            MetroText(text = "metro music", style = MetroTextStyle.PageTitle)
        }
    }
}
