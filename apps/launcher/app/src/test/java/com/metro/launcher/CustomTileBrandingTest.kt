package com.metro.launcher

import com.metro.launcher.data.CustomTileBranding
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
        assertEquals(R.drawable.ic_tile_google, entry!!.glyphResId)
        assertNull(entry.backgroundHex)
        assertTrue(CustomTileBranding.hasCustomTile("com.google.android.googlequicksearchbox"))
    }

    @Test
    fun youtubeMusic_brandRed() {
        val entry = CustomTileBranding.entry("com.google.android.apps.youtube.music")
        assertNotNull(entry)
        assertEquals(R.drawable.ic_tile_yt_music, entry!!.glyphResId)
        assertEquals("#FF0000", entry.backgroundHex)
    }

    @Test
    fun whatsapp_brandGreen() {
        assertEquals(R.drawable.ic_tile_whatsapp, CustomTileBranding.glyphResId("com.whatsapp"))
        assertEquals("#25D366", CustomTileBranding.entry("com.whatsapp")!!.backgroundHex)
    }

    @Test
    fun camera_usesAccent() {
        val entry = CustomTileBranding.entry("com.google.android.GoogleCamera")
        assertNotNull(entry)
        assertNull(entry!!.backgroundHex)
        assertEquals(R.drawable.ic_tile_camera, entry.glyphResId)
    }

    @Test
    fun unknownPackage_hasNoOverride() {
        assertNull(CustomTileBranding.entry("com.example.unknown"))
        assertNull(CustomTileBranding.glyphResId("com.example.unknown"))
    }
}
