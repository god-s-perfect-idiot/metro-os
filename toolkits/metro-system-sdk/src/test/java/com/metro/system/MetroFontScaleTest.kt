package com.metro.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetroFontScaleTest {
    @Test
    fun steps_areTen() {
        assertEquals(10, MetroFontScale.STEP_COUNT)
        assertEquals(10, MetroFontScale.STEPS.size)
    }

    @Test
    fun steps_ascendFromSmallestToLargest() {
        assertEquals(0.625f, MetroFontScale.STEPS.first(), 0.001f)
        assertEquals(1.6f, MetroFontScale.STEPS.last(), 0.001f)
        MetroFontScale.STEPS.toList().zipWithNext { a, b -> assertTrue(b > a) }
    }

    @Test
    fun defaultRemainsUnscaled() {
        assertEquals(MetroFontScale.DEFAULT, MetroFontScale.coerceToStep(1.0f), 0.001f)
    }

    @Test
    fun coerceToStep_snapsToNearest() {
        assertEquals(1.0f, MetroFontScale.coerceToStep(1.02f), 0.001f)
        assertEquals(1.15f, MetroFontScale.coerceToStep(1.14f), 0.001f)
        assertEquals(0.625f, MetroFontScale.coerceToStep(0.5f), 0.001f)
        assertEquals(0.7f, MetroFontScale.coerceToStep(0.71f), 0.001f)
    }

    @Test
    fun indexRoundTrip() {
        MetroFontScale.STEPS.forEachIndexed { index, value ->
            assertEquals(index, MetroFontScale.indexOf(value))
            assertEquals(value, MetroFontScale.fromIndex(index), 0.001f)
        }
    }
}

class MetroAccentPaletteTest {
    @Test
    fun palette_hasTwentyColors() {
        assertEquals(20, MetroAccentPalette.all.size)
    }

    @Test
    fun findByHex_normalizesCaseAndHash() {
        assertEquals("cyan", MetroAccentPalette.findByHex("1ba1e2")?.name)
        assertEquals("cyan", MetroAccentPalette.findByHex("#1BA1E2")?.name)
    }

    @Test
    fun customHex_displayName() {
        assertEquals("custom", MetroAccentPalette.displayName("#123456"))
    }
}
