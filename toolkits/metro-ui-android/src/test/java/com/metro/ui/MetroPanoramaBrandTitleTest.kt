package com.metro.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetroPanoramaBrandTitleTest {

    @Test
    fun travel_singlePage_isZero() {
        assertEquals(
            0f,
            metroPanoramaBrandTravelPx(
                brandWidthPx = 500f,
                availableWidthPx = 300f,
                pageCount = 1,
            ),
        )
    }

    @Test
    fun travel_scalesWithTitleLength() {
        val hub = metroPanoramaBrandTravelPx(
            brandWidthPx = 160f,
            availableWidthPx = 336f,
            pageCount = 4,
        )
        val metron = metroPanoramaBrandTravelPx(
            brandWidthPx = 280f,
            availableWidthPx = 336f,
            pageCount = 5,
        )
        val music = metroPanoramaBrandTravelPx(
            brandWidthPx = 520f,
            availableWidthPx = 336f,
            pageCount = 4,
        )
        assertTrue("hub ($hub) < metron ($metron)", hub < metron)
        assertTrue("metron ($metron) < music ($music)", metron < music)
    }

    @Test
    fun travel_shortTitle_isFractionOfWidth() {
        assertEquals(
            160f * MetroPanoramaBrandParallaxWidthFraction,
            metroPanoramaBrandTravelPx(
                brandWidthPx = 160f,
                availableWidthPx = 336f,
                pageCount = 4,
            ),
        )
    }

    @Test
    fun travel_overflowingTitle_usesHiddenWidthWhenLarger() {
        // 800 − 336 = 464 overflow > 800 × fraction length travel
        val brandWidth = 800f
        val available = 336f
        val hidden = brandWidth - available
        assertTrue(hidden > brandWidth * MetroPanoramaBrandParallaxWidthFraction)
        assertEquals(
            hidden,
            metroPanoramaBrandTravelPx(
                brandWidthPx = brandWidth,
                availableWidthPx = available,
                pageCount = 4,
            ),
        )
    }

    @Test
    fun travel_trailingSlack_increasesTravel() {
        val withoutSlack = metroPanoramaBrandTravelPx(
            brandWidthPx = 336f,
            availableWidthPx = 336f,
            pageCount = 4,
        )
        val withSlack = metroPanoramaBrandTravelPx(
            brandWidthPx = 336f + 128f,
            availableWidthPx = 336f,
            pageCount = 4,
        )
        assertTrue(
            "trailing slack should increase travel ($withoutSlack → $withSlack)",
            withSlack > withoutSlack,
        )
    }
}
