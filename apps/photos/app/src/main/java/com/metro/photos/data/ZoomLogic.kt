package com.metro.photos.data

/**
 * Pure math for WP8.1-style photo viewer pinch-zoom / pan.
 * Scale is applied around the container center; translation is in screen pixels.
 */
object ZoomLogic {
    const val MinScale = 1f
    const val MaxScale = 5f
    const val DoubleTapScale = 2.5f

    fun coerceScale(scale: Float, min: Float = MinScale, max: Float = MaxScale): Float =
        scale.coerceIn(min, max)

    fun maxPan(containerSize: Float, scale: Float): Float =
        if (scale <= MinScale) 0f else (containerSize * (scale - 1f)) / 2f

    fun coerceOffset(
        offsetX: Float,
        offsetY: Float,
        containerWidth: Float,
        containerHeight: Float,
        scale: Float,
    ): Pair<Float, Float> {
        val maxX = maxPan(containerWidth, scale)
        val maxY = maxPan(containerHeight, scale)
        return offsetX.coerceIn(-maxX, maxX) to offsetY.coerceIn(-maxY, maxY)
    }

    /**
     * Apply a pinch zoom centered on [centroid] (container coords) plus a pan delta.
     * Returns the new scale and offset.
     */
    fun applyTransform(
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        containerWidth: Float,
        containerHeight: Float,
        centroidX: Float,
        centroidY: Float,
        zoom: Float,
        panX: Float,
        panY: Float,
        minScale: Float = MinScale,
        maxScale: Float = MaxScale,
    ): Triple<Float, Float, Float> {
        val newScale = coerceScale(scale * zoom, minScale, maxScale)
        if (newScale <= minScale) {
            return Triple(minScale, 0f, 0f)
        }
        val scaleRatio = newScale / scale
        val centerX = containerWidth / 2f
        val centerY = containerHeight / 2f
        val newOffsetX = offsetX * scaleRatio + (centroidX - centerX) * (1f - scaleRatio) + panX
        val newOffsetY = offsetY * scaleRatio + (centroidY - centerY) * (1f - scaleRatio) + panY
        val (clampedX, clampedY) = coerceOffset(
            offsetX = newOffsetX,
            offsetY = newOffsetY,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            scale = newScale,
        )
        return Triple(newScale, clampedX, clampedY)
    }

    /**
     * Double-tap toggle: zoom toward [tapX]/[tapY], or reset to fit.
     */
    fun applyDoubleTap(
        scale: Float,
        containerWidth: Float,
        containerHeight: Float,
        tapX: Float,
        tapY: Float,
        zoomedScale: Float = DoubleTapScale,
    ): Triple<Float, Float, Float> {
        if (scale > MinScale + 0.01f) {
            return Triple(MinScale, 0f, 0f)
        }
        val centerX = containerWidth / 2f
        val centerY = containerHeight / 2f
        val offsetX = (centerX - tapX) * (zoomedScale - 1f)
        val offsetY = (centerY - tapY) * (zoomedScale - 1f)
        val (clampedX, clampedY) = coerceOffset(
            offsetX = offsetX,
            offsetY = offsetY,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            scale = zoomedScale,
        )
        return Triple(zoomedScale, clampedX, clampedY)
    }

    fun isZoomed(scale: Float): Boolean = scale > MinScale + 0.01f
}
