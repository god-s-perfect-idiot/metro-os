package com.metro.system

import org.junit.Assert.assertEquals
import org.junit.Test

class MetroFontScaleTest {
    @Test
    fun steps_areSeven() {
        assertEquals(7, MetroFontScale.STEP_COUNT)
        assertEquals(7, MetroFontScale.STEPS.size)
    }

    @Test
    fun coerceToStep_snapsToNearest() {
        assertEquals(1.0f, MetroFontScale.coerceToStep(1.02f), 0.001f)
        assertEquals(1.15f, MetroFontScale.coerceToStep(1.14f), 0.001f)
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
