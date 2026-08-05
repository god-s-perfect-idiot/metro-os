package com.metro.music

import com.metro.music.data.AlbumTintLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumTintLogicTest {
    private fun red(argb: Int) = argb ushr 16 and 0xFF
    private fun green(argb: Int) = argb ushr 8 and 0xFF
    private fun blue(argb: Int) = argb and 0xFF
    private fun brightest(argb: Int) = maxOf(red(argb), green(argb), blue(argb))

    @Test
    fun tintArgb_keepsDominantHueButStaysDark() {
        val tint = AlbumTintLogic.tintArgb(IntArray(64) { 0xFFCC2233.toInt() })

        requireNotNull(tint)
        assertTrue("red should dominate", red(tint) > green(tint) && red(tint) > blue(tint))
        assertTrue("backdrop must stay near-black", brightest(tint) <= 64)
        assertEquals(0xFF, tint ushr 24 and 0xFF)
    }

    @Test
    fun tintArgb_ignoresBlackBordersWhenChoosingHue() {
        val bordered = IntArray(64) { index ->
            if (index < 48) 0xFF000000.toInt() else 0xFF2244EE.toInt()
        }
        val tint = AlbumTintLogic.tintArgb(bordered)

        requireNotNull(tint)
        assertTrue("blue should dominate", blue(tint) > red(tint) && blue(tint) > green(tint))
        assertTrue(brightest(tint) <= 64)
    }

    @Test
    fun tintArgb_greyscaleArtFallsBackToNeutralDark() {
        val tint = AlbumTintLogic.tintArgb(IntArray(64) { 0xFF808080.toInt() })

        requireNotNull(tint)
        assertEquals(red(tint), green(tint))
        assertEquals(green(tint), blue(tint))
        assertTrue(brightest(tint) <= 48)
    }

    @Test
    fun tintArgb_returnsNullWithoutOpaquePixels() {
        assertNull(AlbumTintLogic.tintArgb(IntArray(0)))
        assertNull(AlbumTintLogic.tintArgb(IntArray(16) { 0x00FFFFFF }))
    }
}
