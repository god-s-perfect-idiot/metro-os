package com.metro.photos.ui

import android.net.Uri
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.metro.photos.data.ZoomLogic
import kotlin.math.abs

/**
 * Full-bleed photo with WP8.1-style pinch-zoom and pan.
 *
 * Owns all touch handling for the viewer page (parent pager has
 * `userScrollEnabled = false`) so pinches are never stolen by page swipes.
 * At fit scale, a horizontal fling/drag requests the previous/next photo via
 * [onSwipeToAdjacent]. Zoom resets when [isActive] becomes false.
 */
@Composable
fun ZoomablePhoto(
    uri: Uri,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    onTap: () -> Unit = {},
    onZoomChanged: (zoomed: Boolean) -> Unit = {},
    onSwipeToAdjacent: (direction: Int) -> Unit = {},
) {
    var scale by remember(uri) { mutableFloatStateOf(ZoomLogic.MinScale) }
    var offset by remember(uri) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }
    val swipeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }

    LaunchedEffect(uri) {
        scale = ZoomLogic.MinScale
        offset = Offset.Zero
    }

    LaunchedEffect(isActive) {
        if (!isActive) {
            scale = ZoomLogic.MinScale
            offset = Offset.Zero
            onZoomChanged(false)
        }
    }

    LaunchedEffect(isActive, scale) {
        if (isActive) {
            onZoomChanged(ZoomLogic.isZoomed(scale))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it.toSize() }
            .pointerInput(uri, isActive, swipeThresholdPx) {
                if (!isActive) return@pointerInput
                val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
                val touchSlop = viewConfiguration.touchSlop
                var lastTapUpTime = 0L
                var lastTapUpPosition = Offset.Unspecified

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val pointerId = down.id
                    val downPosition = down.position
                    val downTime = down.uptimeMillis

                    var pinching = false
                    var panning = false
                    var draggedPastSlop = false
                    var totalPan = Offset.Zero
                    var pressed = true

                    while (pressed) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        pressed = event.changes.fastAny { it.pressed }
                        if (!pressed) {
                            event.changes.fastForEach { it.consume() }
                            break
                        }

                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = true)
                        val pointerCount = event.changes.count { it.pressed }

                        if (pointerCount >= 2) {
                            pinching = true
                            draggedPastSlop = true
                            val (newScale, x, y) = ZoomLogic.applyTransform(
                                scale = scale,
                                offsetX = offset.x,
                                offsetY = offset.y,
                                containerWidth = size.width.toFloat(),
                                containerHeight = size.height.toFloat(),
                                centroidX = centroid.x,
                                centroidY = centroid.y,
                                zoom = zoomChange,
                                panX = panChange.x,
                                panY = panChange.y,
                            )
                            scale = newScale
                            offset = Offset(x, y)
                        } else if (ZoomLogic.isZoomed(scale)) {
                            totalPan += panChange
                            if (!draggedPastSlop && totalPan.getDistance() > touchSlop) {
                                draggedPastSlop = true
                                panning = true
                            }
                            if (panning) {
                                val (_, x, y) = ZoomLogic.applyTransform(
                                    scale = scale,
                                    offsetX = offset.x,
                                    offsetY = offset.y,
                                    containerWidth = size.width.toFloat(),
                                    containerHeight = size.height.toFloat(),
                                    centroidX = centroid.x,
                                    centroidY = centroid.y,
                                    zoom = 1f,
                                    panX = panChange.x,
                                    panY = panChange.y,
                                )
                                offset = Offset(x, y)
                            }
                        } else {
                            totalPan += panChange
                            if (!draggedPastSlop && totalPan.getDistance() > touchSlop) {
                                draggedPastSlop = true
                            }
                        }

                        event.changes.fastForEach {
                            if (it.positionChanged()) it.consume()
                        }
                    }

                    val up = currentEvent.changes.firstOrNull { it.id == pointerId }
                        ?: currentEvent.changes.firstOrNull()
                    val upTime = up?.uptimeMillis ?: downTime
                    val upPosition = up?.position ?: downPosition
                    val wasTap = !pinching && !panning && !draggedPastSlop

                    if (wasTap) {
                        val isDoubleTap = lastTapUpTime > 0L &&
                            (upTime - lastTapUpTime) <= doubleTapTimeout &&
                            lastTapUpPosition != Offset.Unspecified &&
                            (upPosition - lastTapUpPosition).getDistance() < touchSlop * 2f
                        if (isDoubleTap) {
                            val (newScale, x, y) = ZoomLogic.applyDoubleTap(
                                scale = scale,
                                containerWidth = size.width.toFloat(),
                                containerHeight = size.height.toFloat(),
                                tapX = upPosition.x,
                                tapY = upPosition.y,
                            )
                            scale = newScale
                            offset = Offset(x, y)
                            lastTapUpTime = 0L
                            lastTapUpPosition = Offset.Unspecified
                        } else {
                            lastTapUpTime = upTime
                            lastTapUpPosition = upPosition
                            onTap()
                        }
                    } else if (
                        !pinching &&
                        !panning &&
                        !ZoomLogic.isZoomed(scale) &&
                        abs(totalPan.x) > swipeThresholdPx &&
                        abs(totalPan.x) > abs(totalPan.y)
                    ) {
                        // Negative pan = finger moved left → next photo.
                        onSwipeToAdjacent(if (totalPan.x < 0f) 1 else -1)
                    }
                }
            },
    ) {
        PhotoFullImage(
            uri = uri,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}
