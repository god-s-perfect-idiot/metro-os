package com.metro.launcher

import com.metro.launcher.data.CustomTileBranding
import com.metro.ui.MetroAppGlyphs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomTileBrandingTest {

    @Test
    fun googleSearch_whiteGOnAccent() {
        val entry = CustomTileBranding.entry("com.google.android.googlequicksearchbox")
        assertNotNull(entry)
        assertEquals(MetroAppGlyphs.tileOverride("com.google.android.googlequicksearchbox"), entry!!.glyphResId)
        assertNull(entry.backgroundHex)
        assertTrue(CustomTileBranding.hasCustomTile("com.google.android.googlequicksearchbox"))
    }

    @Test
    fun gmail_whiteMOnAccent() {
        val entry = CustomTileBranding.entry("com.google.android.gm")
        assertNotNull(entry)
        assertEquals(MetroAppGlyphs.tileOverride("com.google.android.gm"), entry!!.glyphResId)
        assertNull(entry.backgroundHex)
        assertEquals(
            MetroAppGlyphs.tileOverride("com.google.android.gm.lite"),
            CustomTileBranding.glyphResId("com.google.android.gm.lite"),
        )
        assertEquals(
            MetroAppGlyphs.tileOverride("com.google.android.apps.gmail"),
            CustomTileBranding.glyphResId("com.google.android.apps.gmail"),
        )
    }

    @Test
    fun youtubeMusic_brandRed() {
        val entry = CustomTileBranding.entry("com.google.android.apps.youtube.music")
        assertNotNull(entry)
        assertEquals(
            MetroAppGlyphs.tileOverride("com.google.android.apps.youtube.music"),
            entry!!.glyphResId,
        )
        assertEquals("#FF0000", entry.backgroundHex)
    }

    @Test
    fun whatsapp_brandGreen() {
        assertEquals(
            MetroAppGlyphs.tileOverride("com.whatsapp"),
            CustomTileBranding.glyphResId("com.whatsapp"),
        )
        assertEquals("#25D366", CustomTileBranding.entry("com.whatsapp")!!.backgroundHex)
    }

    @Test
    fun camera_usesAccent() {
        val entry = CustomTileBranding.entry("com.google.android.GoogleCamera")
        assertNotNull(entry)
        assertNull(entry!!.backgroundHex)
        assertEquals(
            MetroAppGlyphs.tileOverride("com.google.android.GoogleCamera"),
            entry.glyphResId,
        )
    }

    @Test
    fun unknownPackage_hasNoOverride() {
        assertNull(CustomTileBranding.entry("com.example.unknown"))
        assertNull(CustomTileBranding.glyphResId("com.example.unknown"))
    }
}
