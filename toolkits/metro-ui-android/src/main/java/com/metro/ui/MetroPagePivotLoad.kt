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
import kotlinx.coroutines.delay
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
fun metroPagePivotCameraDistance(
    widthPx: Float,
    widthFactor: Float = PagePivotCameraWidthFactor,
): Float {
    if (widthPx <= 0f) return PagePivotCameraDefaultInches
    return (widthPx / SkiaPointsPerInch) * widthFactor
}

/**
 * Full-screen app wrapper — page pivot load on launch; Back runs the flip-out
 * then invokes [onExit] (typically [MetroActivities.finishWithExitTransition]).
 *
 * Place inside [MetroTheme]. Call [MetroActivities.applyLaunchTransition] from
 * the activity `onCreate` so the platform open animation does not fight this
 * Compose pivot. Nested `BackHandler`s take priority, so in-app navigation can
 * intercept Back before this shell exits the activity.
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
 * WP8.1 page pivot load — enter finishes a short left-hinge swing
 * (`rotateY` [MetroTransitions.PagePivotLoadStartDegrees]° → 0°) and slide from
 * [MetroTransitions.PagePivotLoadStartTranslationXFraction]× width to rest. Exit tilts back into
 * the screen: `rotateY` 0° → [MetroTransitions.PagePivotExitEndDegrees]°, hinge at
 * [MetroTransitions.PagePivotExitOriginX], slide
 * [MetroTransitions.PagePivotExitTranslationXFraction]× width left, softer exit camera, and fade.
 *
 * Pass a distinct [loadKey] when replacing page content so the enter animation runs again.
 * Set [exiting] for the flip-out; [onExitComplete] runs once that outro finishes.
 *
 * For a hinge-only swing with no X slide (e.g. Start tiles), use [MetroPagePivotSwing].
 */
@Composable
fun MetroPagePivotLoad(
    modifier: Modifier = Modifier,
    loadKey: Any? = Unit,
    exiting: Boolean = false,
    onExitComplete: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    MetroPagePivotMotion(
        modifier = modifier,
        loadKey = loadKey,
        exiting = exiting,
        onExitComplete = onExitComplete,
        translateX = true,
        content = content,
    )
}

/**
 * Page pivot **swing** — left-hinge `rotateY` + fade like [MetroPagePivotLoad], but
 * **no X translation**, with a deeper start angle
 * ([MetroTransitions.PagePivotSwingStartDegrees]) and closer camera
 * ([MetroTransitions.PagePivotSwingCameraWidthFactor]) so a shared page hinge
 * (Start tiles) reads clear foreshortening.
 *
 * [cameraWidthPx] sizes perspective (defaults to this layer's width). [hingeInsetPx] is the
 * distance from the page/camera left edge to this layer's left edge so several children can
 * share one page hinge (local origin may be &lt; 0). [delayMs] waits before enter starts.
 * [onEnterComplete] runs after the enter swing finishes (not on exit).
 */
@Composable
fun MetroPagePivotSwing(
    modifier: Modifier = Modifier,
    loadKey: Any? = Unit,
    delayMs: Long = 0L,
    exiting: Boolean = false,
    onExitComplete: (() -> Unit)? = null,
    onEnterComplete: (() -> Unit)? = null,
    cameraWidthPx: Float? = null,
    hingeInsetPx: Float = 0f,
    content: @Composable () -> Unit,
) {
    MetroPagePivotMotion(
        modifier = modifier,
        loadKey = loadKey,
        delayMs = delayMs,
        exiting = exiting,
        onExitComplete = onExitComplete,
        onEnterComplete = onEnterComplete,
        translateX = false,
        cameraWidthPx = cameraWidthPx,
        hingeInsetPx = hingeInsetPx,
        content = content,
    )
}

@Composable
private fun MetroPagePivotMotion(
    modifier: Modifier,
    loadKey: Any?,
    exiting: Boolean,
    onExitComplete: (() -> Unit)?,
    translateX: Boolean,
    content: @Composable () -> Unit,
    delayMs: Long = 0L,
    onEnterComplete: (() -> Unit)? = null,
    cameraWidthPx: Float? = null,
    hingeInsetPx: Float = 0f,
) {
    // Swing (no X slide) uses a deeper enter angle + closer camera than full page load.
    val enterStartDegrees = if (translateX) {
        MetroTransitions.PagePivotLoadStartDegrees
    } else {
        MetroTransitions.PagePivotSwingStartDegrees
    }
    val enterCameraWidthFactor = if (translateX) {
        PagePivotCameraWidthFactor
    } else {
        MetroTransitions.PagePivotSwingCameraWidthFactor
    }
    val rotationY = remember {
        Animatable(if (exiting) 0f else enterStartDegrees)
    }
    val alpha = remember { Animatable(if (exiting) 1f else 0f) }
    val translationXFraction = remember {
        Animatable(
            when {
                !translateX -> 0f
                exiting -> 0f
                else -> MetroTransitions.PagePivotLoadStartTranslationXFraction
            },
        )
    }
    LaunchedEffect(loadKey, exiting, delayMs, translateX) {
        if (exiting) {
            rotationY.snapTo(0f)
            alpha.snapTo(1f)
            if (translateX) translationXFraction.snapTo(0f)
            coroutineScope {
                val fade = launch { alpha.animateTo(0f, MetroTransitions.pagePivotLoadTween()) }
                val pivot = launch {
                    rotationY.animateTo(
                        MetroTransitions.PagePivotExitEndDegrees,
                        MetroTransitions.pagePivotLoadTween(),
                    )
                }
                val slide = if (translateX) {
                    launch {
                        translationXFraction.animateTo(
                            MetroTransitions.PagePivotExitTranslationXFraction,
                            MetroTransitions.pagePivotLoadTween(),
                        )
                    }
                } else {
                    null
                }
                fade.join()
                pivot.join()
                slide?.join()
            }
            onExitComplete?.invoke()
        } else {
            rotationY.snapTo(enterStartDegrees)
            // Stay fully hidden until the (optional) stagger delay ends — otherwise waiting
            // Start tiles sit in a half-visible pre-swing pose.
            alpha.snapTo(0f)
            if (translateX) {
                translationXFraction.snapTo(
                    MetroTransitions.PagePivotLoadStartTranslationXFraction,
                )
            } else {
                translationXFraction.snapTo(0f)
            }
            if (delayMs > 0L) delay(delayMs)
            coroutineScope {
                launch { alpha.animateTo(1f, MetroTransitions.pagePivotLoadTween()) }
                launch { rotationY.animateTo(0f, MetroTransitions.pagePivotLoadTween()) }
                if (translateX) {
                    launch {
                        translationXFraction.animateTo(
                            0f,
                            MetroTransitions.pagePivotLoadTween(),
                        )
                    }
                }
            }
            onEnterComplete?.invoke()
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            this.rotationY = rotationY.value
            this.alpha = alpha.value
            val layerWidth = size.width.coerceAtLeast(1f)
            if (translateX) {
                translationX = translationXFraction.value * layerWidth
            }
            transformOrigin = if (exiting) {
                TransformOrigin(MetroTransitions.PagePivotExitOriginX, 0.5f)
            } else {
                // Shared page hinge: inset maps page-left into this layer's local origin.
                TransformOrigin(
                    pivotFractionX = MetroTransitions.PagePivotLoadOriginX -
                        (hingeInsetPx / layerWidth),
                    pivotFractionY = 0.5f,
                )
            }
            clip = false
            val cameraWidth = cameraWidthPx?.takeIf { it > 0f } ?: size.width
            cameraDistance = metroPagePivotCameraDistance(
                widthPx = cameraWidth,
                widthFactor = if (exiting) {
                    MetroTransitions.PagePivotExitCameraWidthFactor
                } else {
                    enterCameraWidthFactor
                },
            )
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
