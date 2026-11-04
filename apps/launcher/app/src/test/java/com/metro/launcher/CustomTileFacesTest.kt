package com.metro.launcher

import com.metro.launcher.data.PinnedTileEntry
import com.metro.launcher.data.resolvedIconPackOverride
import com.metro.launcher.data.usesAmazonCustomFace
import com.metro.launcher.data.usesChromeLegacyFace
import com.metro.launcher.data.usesFacebookCustomFace
import com.metro.launcher.data.usesYouTubeCustomFace
import com.metro.launcher.data.usesYouTubeMusicCustomFace
import com.metro.launcher.ui.isAmazonTilePackage
import com.metro.launcher.ui.isChromeTilePackage
import com.metro.launcher.ui.isFacebookTilePackage
import com.metro.launcher.ui.isYouTubeMusicTilePackage
import com.metro.launcher.ui.isYouTubeTilePackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomTileFacesTest {

    @Test
    fun chromePackages_matchStableAndChannels() {
        assertTrue(isChromeTilePackage("com.android.chrome"))
        assertTrue(isChromeTilePackage("com.chrome.beta"))
        assertTrue(isChromeTilePackage("com.chrome.dev"))
        assertTrue(isChromeTilePackage("com.chrome.canary"))
        assertTrue(isChromeTilePackage("com.google.android.apps.chrome"))
    }

    @Test
    fun chromePackages_ignoreOtherBrowsers() {
        assertFalse(isChromeTilePackage("com.metro.browser"))
        assertFalse(isChromeTilePackage("org.mozilla.firefox"))
        assertFalse(isChromeTilePackage("com.android.chrome.fake"))
    }

    @Test
    fun facebookPackages_matchMainAndLite() {
        assertTrue(isFacebookTilePackage("com.facebook.katana"))
        assertTrue(isFacebookTilePackage("com.facebook.lite"))
        assertTrue(isFacebookTilePackage("com.facebook.mlite"))
    }

    @Test
    fun facebookPackages_ignoreMessengerAndOthers() {
        assertFalse(isFacebookTilePackage("com.facebook.orca"))
        assertFalse(isFacebookTilePackage("com.facebook.katana.fake"))
        assertFalse(isFacebookTilePackage("com.android.chrome"))
    }

    @Test
    fun youtubePackages_matchMainAndGo() {
        assertTrue(isYouTubeTilePackage("com.google.android.youtube"))
        assertTrue(isYouTubeTilePackage("com.google.android.apps.youtube.mango"))
    }

    @Test
    fun youtubePackages_ignoreMusicAndOthers() {
        assertFalse(isYouTubeTilePackage("com.google.android.apps.youtube.music"))
        assertFalse(isYouTubeTilePackage("com.google.android.youtube.tv"))
        assertFalse(isYouTubeTilePackage("com.android.chrome"))
    }

    @Test
    fun youtubeMusicPackages_matchAndIgnoreSiblings() {
        assertTrue(isYouTubeMusicTilePackage("com.google.android.apps.youtube.music"))
        assertFalse(isYouTubeMusicTilePackage("com.google.android.youtube"))
        assertFalse(isYouTubeMusicTilePackage("com.google.android.apps.youtube.mango"))
        assertFalse(isYouTubeMusicTilePackage("com.spotify.music"))
    }

    @Test
    fun amazonPackages_matchShoppingAndIgnoreMusic() {
        assertTrue(isAmazonTilePackage("com.amazon.mShop.android.shopping"))
        assertTrue(isAmazonTilePackage("com.amazon.mShop.android"))
        assertTrue(isAmazonTilePackage("in.amazon.mShop.android.shopping"))
        assertFalse(isAmazonTilePackage("com.amazon.mp3"))
        assertFalse(isAmazonTilePackage("com.android.chrome"))
    }

    @Test
    fun chromeCustomFace_defaultsWhenNeverCustomized() {
        assertTrue(
            PinnedTileEntry(packageName = "com.android.chrome").usesChromeLegacyFace(),
        )
        assertTrue(
            PinnedTileEntry(
                packageName = "com.android.chrome",
                iconPackPackage = PinnedTileEntry.CHROME_LEGACY_FACE,
            ).usesChromeLegacyFace(),
        )
        assertFalse(
            PinnedTileEntry(
                packageName = "com.android.chrome",
                iconPackPackage = "",
            ).usesChromeLegacyFace(),
        )
        assertFalse(
            PinnedTileEntry(
                packageName = "com.android.chrome",
                iconPackPackage = "com.example.iconpack",
            ).usesChromeLegacyFace(),
        )
    }

    @Test
    fun facebookCustomFace_defaultsWhenNeverCustomized() {
        assertTrue(
            PinnedTileEntry(packageName = "com.facebook.katana").usesFacebookCustomFace(),
        )
        assertTrue(
            PinnedTileEntry(
                packageName = "com.facebook.lite",
                iconPackPackage = PinnedTileEntry.FACEBOOK_CUSTOM_FACE,
            ).usesFacebookCustomFace(),
        )
        assertFalse(
            PinnedTileEntry(
                packageName = "com.facebook.katana",
                iconPackPackage = "",
            ).usesFacebookCustomFace(),
        )
        assertFalse(
            PinnedTileEntry(
                packageName = "com.facebook.katana",
                iconPackPackage = "com.example.iconpack",
            ).usesFacebookCustomFace(),
        )
    }

    @Test
    fun youtubeCustomFace_defaultsWhenNeverCustomized() {
        assertTrue(
            PinnedTileEntry(packageName = "com.google.android.youtube").usesYouTubeCustomFace(),
        )
        assertTrue(
            PinnedTileEntry(
                packageName = "com.google.android.apps.youtube.mango",
                iconPackPackage = PinnedTileEntry.YOUTUBE_CUSTOM_FACE,
            ).usesYouTubeCustomFace(),
        )
        assertFalse(
            PinnedTileEntry(
                packageName = "com.google.android.youtube",
                iconPackPackage = "",
            ).usesYouTubeCustomFace(),
        )
        assertFalse(
            PinnedTileEntry(
                packageName = "com.google.android.youtube",
                iconPackPackage = "com.example.iconpack",
            ).usesYouTubeCustomFace(),
        )
    }

    @Test
    fun youtubeMusicCustomFace_defaultsWhenNeverCustomized() {
        assertTrue(
            PinnedTileEntry(
                packageName = "com.google.android.apps.youtube.music",
            ).usesYouTubeMusicCustomFace(),
        )
        assertTrue(
            PinnedTileEntry(
                packageName = "com.google.android.apps.youtube.music",
                iconPackPackage = PinnedTileEntry.YOUTUBE_MUSIC_CUSTOM_FACE,
            ).usesYouTubeMusicCustomFace(),
        )
        assertFalse(
            PinnedTileEntry(
                packageName = "com.google.android.apps.youtube.music",
                iconPackPackage = "",
            ).usesYouTubeMusicCustomFace(),
        )
        assertFalse(
            PinnedTileEntry(
                packageName = "com.google.android.apps.youtube.music",
                iconPackPackage = "com.example.iconpack",
            ).usesYouTubeMusicCustomFace(),
        )
    }

    @Test
    fun amazonCustomFace_defaultsWhenNeverCustomized() {
        assertTrue(
            PinnedTileEntry(
                packageName = "com.amazon.mShop.android.shopping",
            ).usesAmazonCustomFace(),
        )
        assertTrue(
            PinnedTileEntry(
                packageName = "in.amazon.mShop.android.shopping",
                iconPackPackage = PinnedTileEntry.AMAZON_CUSTOM_FACE,
            ).usesAmazonCustomFace(),
        )
        assertFalse(
            PinnedTileEntry(
                packageName = "com.amazon.mShop.android.shopping",
                iconPackPackage = "",
            ).usesAmazonCustomFace(),
        )
        assertFalse(
            PinnedTileEntry(
                packageName = "com.amazon.mShop.android.shopping",
                iconPackPackage = "com.example.iconpack",
            ).usesAmazonCustomFace(),
        )
    }

    @Test
    fun composedFaces_areNotIconPackOverrides() {
        assertNull(
            PinnedTileEntry(
                packageName = "com.android.chrome",
                iconPackPackage = PinnedTileEntry.CHROME_LEGACY_FACE,
            ).resolvedIconPackOverride(),
        )
        assertNull(
            PinnedTileEntry(
                packageName = "com.facebook.katana",
                iconPackPackage = PinnedTileEntry.FACEBOOK_CUSTOM_FACE,
            ).resolvedIconPackOverride(),
        )
        assertNull(
            PinnedTileEntry(
                packageName = "com.google.android.youtube",
                iconPackPackage = PinnedTileEntry.YOUTUBE_CUSTOM_FACE,
            ).resolvedIconPackOverride(),
        )
        assertNull(
            PinnedTileEntry(
                packageName = "com.google.android.apps.youtube.music",
                iconPackPackage = PinnedTileEntry.YOUTUBE_MUSIC_CUSTOM_FACE,
            ).resolvedIconPackOverride(),
        )
        assertNull(
            PinnedTileEntry(
                packageName = "com.amazon.mShop.android.shopping",
                iconPackPackage = PinnedTileEntry.AMAZON_CUSTOM_FACE,
            ).resolvedIconPackOverride(),
        )
        assertEquals(
            "com.example.iconpack",
            PinnedTileEntry(
                packageName = "com.android.chrome",
                iconPackPackage = "com.example.iconpack",
            ).resolvedIconPackOverride(),
        )
        assertTrue(PinnedTileEntry.isComposedTileFace(PinnedTileEntry.CHROME_LEGACY_FACE))
        assertTrue(PinnedTileEntry.isComposedTileFace(PinnedTileEntry.FACEBOOK_CUSTOM_FACE))
        assertTrue(PinnedTileEntry.isComposedTileFace(PinnedTileEntry.YOUTUBE_CUSTOM_FACE))
        assertTrue(PinnedTileEntry.isComposedTileFace(PinnedTileEntry.YOUTUBE_MUSIC_CUSTOM_FACE))
        assertTrue(PinnedTileEntry.isComposedTileFace(PinnedTileEntry.AMAZON_CUSTOM_FACE))
        assertFalse(PinnedTileEntry.isComposedTileFace(""))
        assertFalse(PinnedTileEntry.isComposedTileFace(null))
    }
}
