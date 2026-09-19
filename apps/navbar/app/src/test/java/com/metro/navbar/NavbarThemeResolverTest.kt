package com.metro.navbar

import androidx.compose.ui.graphics.Color
import com.metro.system.MetroPreferences
import com.metro.system.MetroThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NavbarThemeResolverTest {
    @Test
    fun defaultsToBlackBarWithWhiteIcons() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = MetroPreferences(context)
        prefs.themeMode = MetroThemeMode.Dark
        prefs.accentColorHex = MetroPreferences.DEFAULT_ACCENT_HEX

        val snapshot = NavbarThemeResolver.resolve(prefs)

        assertEquals(Color.Black, snapshot.barColor)
        assertEquals(Color.White, snapshot.iconColor)
        assertEquals(prefs.accentColor, snapshot.accentColor)
        assertTrue(snapshot.darkTheme)
    }

    @Test
    fun usesDarkIconsOnLightBar() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = MetroPreferences(context)
        prefs.themeMode = MetroThemeMode.Light

        val snapshot = NavbarThemeResolver.resolve(
            preferences = prefs,
            backgroundMode = NavbarBackgroundMode.MatchAppBackground,
            appBackgroundColor = Color(0xFFF2F2F2),
        )

        assertEquals(Color(0xFFF2F2F2), snapshot.barColor)
        assertEquals(Color.Black, snapshot.iconColor)
    }

    @Test
    fun resolve_matchAppBackground_usesAppFillAndInvertsLight() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = MetroPreferences(context)
        prefs.themeMode = MetroThemeMode.Dark

        val snapshot = NavbarThemeResolver.resolve(
            preferences = prefs,
            backgroundMode = NavbarBackgroundMode.MatchAppBackground,
            appBackgroundColor = Color(0xFFF2F2F2),
        )
        assertEquals(Color(0xFFF2F2F2), snapshot.barColor)
        assertEquals(Color.Black, snapshot.iconColor)
    }

    @Test
    fun resolve_showAccent_usesSystemAccent() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = MetroPreferences(context)
        prefs.themeMode = MetroThemeMode.Dark
        prefs.accentColorHex = "#00ABA9"

        val snapshot = NavbarThemeResolver.resolve(
            preferences = prefs,
            backgroundMode = NavbarBackgroundMode.ShowAccentColor,
            appBackgroundColor = Color(0xFFF0A30A),
        )
        assertEquals(prefs.accentColor, snapshot.barColor)
        assertEquals(prefs.accentColor, snapshot.accentColor)
    }

    @Test
    fun resolve_defaultBlack_ignoresAppBackground() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = MetroPreferences(context)

        val snapshot = NavbarThemeResolver.resolve(
            preferences = prefs,
            backgroundMode = NavbarBackgroundMode.DefaultBlackBackground,
            appBackgroundColor = Color(0xFFF0A30A),
        )
        assertEquals(Color.Black, snapshot.barColor)
    }

    @Test
    fun resolve_ignoresLegacyNavBarColorOverride() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = MetroPreferences(context)
        prefs.navBarColorHex = "#D80073"
        prefs.accentColorHex = "#00ABA9"

        val snapshot = NavbarThemeResolver.resolve(
            preferences = prefs,
            backgroundMode = NavbarBackgroundMode.DefaultBlackBackground,
        )
        assertEquals(Color.Black, snapshot.barColor)
        assertEquals(prefs.accentColor, snapshot.accentColor)
    }
}
