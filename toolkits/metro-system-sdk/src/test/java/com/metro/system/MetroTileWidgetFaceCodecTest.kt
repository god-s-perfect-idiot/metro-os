package com.metro.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetroTileWidgetFaceCodecTest {
    @Test
    fun roundTrip_glyphToggle() {
        val face = MetroTileWidgetFace(
            kind = MetroTileWidgetFaceKind.GLYPH_TOGGLE,
            glyph = MetroTileWidgetGlyph.TORCH,
            toggleOn = true,
            dimmed = false,
        )
        val encoded = MetroTileWidgetFaceCodec.encode(face)
        val decoded = MetroTileWidgetFaceCodec.decode(encoded)
        assertEquals(face, decoded)
        assertTrue(decoded!!.hasContent)
    }

    @Test
    fun roundTrip_battery() {
        val face = MetroTileWidgetFace(
            kind = MetroTileWidgetFaceKind.BATTERY,
            batteryPercent = 42,
        )
        assertEquals(face, MetroTileWidgetFaceCodec.decode(MetroTileWidgetFaceCodec.encode(face)))
    }

    @Test
    fun decode_blank_returnsNull() {
        assertNull(MetroTileWidgetFaceCodec.decode(null))
        assertNull(MetroTileWidgetFaceCodec.decode(""))
        assertNull(MetroTileWidgetFaceCodec.encode(null))
    }

    @Test
    fun clockParts_twelveHour() {
        val afternoon = java.time.LocalDateTime.of(2024, 6, 15, 14, 5)
        val parts = MetroClockFace.parts(afternoon)
        assertEquals("2", parts.hour)
        assertEquals("05", parts.minute)
        assertEquals("PM", parts.period)
    }

    @Test
    fun peeks_roundTrip() {
        val peeks = listOf(
            MetroTilePeek(title = "Hello", body = "World", footer = "Mail", packageName = "com.example.mail"),
            MetroTilePeek(title = "Only title", footer = "Chat", packageName = "com.example.chat"),
        )
        val encoded = MetroTilePeekCodec.encode(peeks)
        assertEquals(peeks, MetroTilePeekCodec.decode(encoded))
    }
}
