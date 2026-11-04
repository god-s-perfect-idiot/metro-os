package com.metro.launcher

import androidx.compose.ui.graphics.Color
import com.metro.launcher.data.BrandTileFace
import com.metro.launcher.data.PinnedTileEntry
import com.metro.launcher.data.brandFaceBackground
import com.metro.launcher.data.brandTileFaceFor
import com.metro.launcher.data.pinForPackage
import com.metro.launcher.data.usesWhiteGoogleChrome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppGlyphAppearanceTest {

    @Test
    fun brandFace_defaultsWhenUnpinned() {
        assertEquals(
            BrandTileFace.Chrome,
            brandTileFaceFor("com.android.chrome", pinned = null),
        )
        assertEquals(
            BrandTileFace.Facebook,
            brandTileFaceFor("com.facebook.katana", pinned = null),
        )
        assertEquals(
            BrandTileFace.YouTube,
            brandTileFaceFor("com.google.android.youtube", pinned = null),
        )
        assertEquals(
            BrandTileFace.YouTubeMusic,
            brandTileFaceFor("com.google.android.apps.youtube.music", pinned = null),
        )
        assertEquals(
            BrandTileFace.Amazon,
            brandTileFaceFor("com.amazon.mShop.android.shopping", pinned = null),
        )
        assertNull(brandTileFaceFor("com.example.other", pinned = null))
    }

    @Test
    fun brandFace_respectsPinOptOut() {
        val none = PinnedTileEntry(
            packageName = "com.android.chrome",
            iconPackPackage = "",
        )
        assertNull(brandTileFaceFor("com.android.chrome", pinned = none))

        val pack = PinnedTileEntry(
            packageName = "com.android.chrome",
            iconPackPackage = "com.example.iconpack",
        )
        assertNull(brandTileFaceFor("com.android.chrome", pinned = pack))
    }

    @Test
    fun brandFace_suppressedByCustomIcon() {
        val custom = PinnedTileEntry(
            packageName = "com.android.chrome",
            useCustomIcon = true,
        )
        assertNull(brandTileFaceFor("com.android.chrome", pinned = custom))
    }

    @Test
    fun brandFaceBackground_matchesTileFills() {
        assertEquals(Color(0xFF3B5998), brandFaceBackground(BrandTileFace.Facebook))
        assertEquals(Color(0xFFE5E5E5), brandFaceBackground(BrandTileFace.YouTube))
        assertEquals(Color(0xFFFF0000), brandFaceBackground(BrandTileFace.YouTubeMusic))
        assertEquals(Color.White, brandFaceBackground(BrandTileFace.Chrome))
        assertEquals(Color.White, brandFaceBackground(BrandTileFace.Amazon))
    }

    @Test
    fun googleApps_useWhiteListSplashChrome() {
        assertTrue(usesWhiteGoogleChrome("com.google.android.gm"))
        assertTrue(usesWhiteGoogleChrome("com.google.android.googlequicksearchbox"))
        assertTrue(usesWhiteGoogleChrome("com.google.android.apps.maps"))
        assertTrue(usesWhiteGoogleChrome("com.android.chrome"))
        assertFalse(usesWhiteGoogleChrome("com.google.android.apps.youtube.music"))
        assertFalse(usesWhiteGoogleChrome("com.whatsapp"))
        assertFalse(usesWhiteGoogleChrome("com.facebook.katana"))
    }

    @Test
    fun pinForPackage_prefersPrimary() {
        val pins = listOf(
            PinnedTileEntry(packageName = "com.example.app", tileId = "secondary"),
            PinnedTileEntry(packageName = "com.example.app", tileId = "primary"),
            PinnedTileEntry(packageName = "com.other", tileId = "primary"),
        )
        assertEquals("primary", pins.pinForPackage("com.example.app")!!.tileId)
        assertNull(pins.pinForPackage("com.missing"))
    }
}
