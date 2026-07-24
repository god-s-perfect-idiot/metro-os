package com.metro.launcher

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
}
