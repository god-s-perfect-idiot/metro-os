package com.metro.launcher

import com.metro.launcher.data.PinnedTileEntry
import com.metro.launcher.data.usesChromeLegacyFace
import com.metro.launcher.ui.isChromeTilePackage
import org.junit.Assert.assertFalse
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
    fun chromeLegacyFace_matchesSentinel() {
        assertTrue(
            PinnedTileEntry(
                packageName = "com.android.chrome",
                iconPackPackage = PinnedTileEntry.CHROME_LEGACY_FACE,
            ).usesChromeLegacyFace(),
        )
        assertFalse(
            PinnedTileEntry(packageName = "com.android.chrome").usesChromeLegacyFace(),
        )
        assertFalse(
            PinnedTileEntry(
                packageName = "com.android.chrome",
                iconPackPackage = "",
            ).usesChromeLegacyFace(),
        )
    }
}
