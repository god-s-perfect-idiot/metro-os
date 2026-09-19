package com.metro.navbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NavbarPreferencesTest {
    @Test
    fun backgroundMode_defaultsToDefaultBlackBackground() {
        val prefs = NavbarPreferences(RuntimeEnvironment.getApplication())
        assertEquals(NavbarBackgroundMode.DefaultBlackBackground, prefs.backgroundMode)
    }

    @Test
    fun backgroundMode_persistsAcrossInstances() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = NavbarPreferences(context)
        prefs.backgroundMode = NavbarBackgroundMode.MatchAppBackground
        assertEquals(
            NavbarBackgroundMode.MatchAppBackground,
            NavbarPreferences(context).backgroundMode,
        )
        prefs.backgroundMode = NavbarBackgroundMode.ShowAccentColor
        assertEquals(
            NavbarBackgroundMode.ShowAccentColor,
            NavbarPreferences(context).backgroundMode,
        )
        prefs.backgroundMode = NavbarBackgroundMode.DefaultBlackBackground
        assertEquals(
            NavbarBackgroundMode.DefaultBlackBackground,
            NavbarPreferences(context).backgroundMode,
        )
    }

    @Test
    fun backgroundMode_unknownStorageFallsBackToDefaultBlack() {
        assertEquals(
            NavbarBackgroundMode.DefaultBlackBackground,
            NavbarBackgroundMode.fromStorage(null),
        )
        assertEquals(
            NavbarBackgroundMode.DefaultBlackBackground,
            NavbarBackgroundMode.fromStorage("theme"),
        )
        assertEquals(
            NavbarBackgroundMode.MatchAppBackground,
            NavbarBackgroundMode.fromStorage(NavbarBackgroundMode.STORAGE_MATCH_APP),
        )
        assertEquals(
            NavbarBackgroundMode.ShowAccentColor,
            NavbarBackgroundMode.fromStorage(NavbarBackgroundMode.STORAGE_SHOW_ACCENT),
        )
    }
}

class SystemNavigationBarsDetectorTest {
    @Test
    fun areHiddenFromVisible_nullOrTrue_isNotHidden() {
        assertFalse(SystemNavigationBarsDetector.areHiddenFromVisible(null))
        assertFalse(SystemNavigationBarsDetector.areHiddenFromVisible(true))
    }

    @Test
    fun areHiddenFromVisible_false_isHidden() {
        assertTrue(SystemNavigationBarsDetector.areHiddenFromVisible(false))
    }
}

class NavbarVisibilityModeTest {
    @Test
    fun fromContract_mapsHiddenAndOpaque() {
        assertEquals(
            NavbarVisibilityMode.Hidden,
            NavbarVisibilityMode.fromContract(com.metro.system.MetroNavBar.MODE_HIDDEN),
        )
        assertEquals(
            NavbarVisibilityMode.Opaque,
            NavbarVisibilityMode.fromContract(com.metro.system.MetroNavBar.MODE_OPAQUE),
        )
        assertEquals(
            NavbarVisibilityMode.Opaque,
            NavbarVisibilityMode.fromContract(null),
        )
    }
}
