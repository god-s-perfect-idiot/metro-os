package com.metro.photos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomLogicTest {
    @Test
    fun coerceScale_clampsToRange() {
        assertEquals(1f, ZoomLogic.coerceScale(0.5f), 0.001f)
        assertEquals(5f, ZoomLogic.coerceScale(9f), 0.001f)
        assertEquals(2.5f, ZoomLogic.coerceScale(2.5f), 0.001f)
    }

    @Test
    fun maxPan_isZeroWhenFit() {
        assertEquals(0f, ZoomLogic.maxPan(800f, 1f), 0.001f)
    }

    @Test
    fun maxPan_growsWithScale() {
        assertEquals(400f, ZoomLogic.maxPan(800f, 2f), 0.001f)
    }

    @Test
    fun applyTransform_resetsWhenPinchedBackToFit() {
        val (scale, x, y) = ZoomLogic.applyTransform(
            scale = 2f,
            offsetX = 100f,
            offsetY = 50f,
            containerWidth = 800f,
            containerHeight = 1280f,
            centroidX = 400f,
            centroidY = 640f,
            zoom = 0.4f,
            panX = 0f,
            panY = 0f,
        )
        assertEquals(1f, scale, 0.001f)
        assertEquals(0f, x, 0.001f)
        assertEquals(0f, y, 0.001f)
    }

    @Test
    fun applyTransform_clampsPanToEdges() {
        val (scale, x, y) = ZoomLogic.applyTransform(
            scale = 2f,
            offsetX = 0f,
            offsetY = 0f,
            containerWidth = 800f,
            containerHeight = 1280f,
            centroidX = 400f,
            centroidY = 640f,
            zoom = 1f,
            panX = 10_000f,
            panY = -10_000f,
        )
        assertEquals(2f, scale, 0.001f)
        assertEquals(400f, x, 0.001f)
        assertEquals(-640f, y, 0.001f)
    }

    @Test
    fun applyDoubleTap_zoomsTowardTapThenResets() {
        val zoomed = ZoomLogic.applyDoubleTap(
            scale = 1f,
            containerWidth = 800f,
            containerHeight = 1280f,
            tapX = 200f,
            tapY = 320f,
        )
        assertEquals(ZoomLogic.DoubleTapScale, zoomed.first, 0.001f)
        assertTrue(zoomed.second != 0f || zoomed.third != 0f)

        val reset = ZoomLogic.applyDoubleTap(
            scale = zoomed.first,
            containerWidth = 800f,
            containerHeight = 1280f,
            tapX = 200f,
            tapY = 320f,
        )
        assertEquals(1f, reset.first, 0.001f)
        assertEquals(0f, reset.second, 0.001f)
        assertEquals(0f, reset.third, 0.001f)
    }

    @Test
    fun isZoomed_threshold() {
        assertFalse(ZoomLogic.isZoomed(1f))
        assertFalse(ZoomLogic.isZoomed(1.005f))
        assertTrue(ZoomLogic.isZoomed(1.02f))
    }
}
