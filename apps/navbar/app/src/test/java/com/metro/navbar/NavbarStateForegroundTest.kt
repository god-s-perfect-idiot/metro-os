package com.metro.navbar

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowSystemClock
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class NavbarStateForegroundTest {
    @Test
    fun matchApp_startAuthoritative_setsBlackAndIgnoresStaleProbe() {
        val context = RuntimeEnvironment.getApplication()
        NavbarPreferences(context).backgroundMode = NavbarBackgroundMode.MatchAppBackground
        val state = NavbarState(context)

        state.applyForegroundPackage("com.example.photos", authoritative = true)
        // Branding is async for third-party; force the retained color.
        state.applyForegroundPackage("com.example.photos", authoritative = true)

        state.applyForegroundPackage(NavbarSpec.LAUNCHER_PACKAGE, authoritative = true)
        assertEquals(NavbarSpec.LAUNCHER_PACKAGE, state.foregroundPackage)
        assertEquals(Color.Black, state.theme.barColor)

        val epoch = state.foregroundEpochPeek()
        state.applyForegroundPackage(
            "com.example.photos",
            epoch = epoch,
            fromProbe = true,
        )
        assertEquals(NavbarSpec.LAUNCHER_PACKAGE, state.foregroundPackage)
        assertEquals(Color.Black, state.theme.barColor)
    }

    @Test
    fun matchApp_probeAppliesAfterQuietWindow() {
        val context = RuntimeEnvironment.getApplication()
        NavbarPreferences(context).backgroundMode = NavbarBackgroundMode.MatchAppBackground
        val state = NavbarState(context)

        state.applyForegroundPackage(NavbarSpec.LAUNCHER_PACKAGE, authoritative = true)
        ShadowSystemClock.advanceBy(500, TimeUnit.MILLISECONDS)

        state.applyForegroundPackage(
            "com.example.notes",
            epoch = state.foregroundEpochPeek(),
            fromProbe = true,
        )
        assertEquals("com.example.notes", state.foregroundPackage)
    }
}
