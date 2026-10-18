package com.metro.statusbar

import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.graphics.Color
import com.metro.system.MetroPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TrayThemeResolverTest {
    private lateinit var preferences: MetroPreferences

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        preferences = MetroPreferences(context)
        preferences.themeMode = com.metro.system.MetroThemeMode.Dark
        preferences.accentColorHex = MetroPreferences.DEFAULT_ACCENT_HEX
    }

    @Test
    fun resolve_defaultsToBlackBackgroundAndWhiteForeground() {
        val snapshot = TrayThemeResolver.resolve(preferences)
        assertEquals(Color.Black, snapshot.backgroundColor)
        assertEquals(Color.White, snapshot.foregroundColor)
    }

    @Test
    fun resolve_matchAppBackground_usesAppFillAndInvertsLight() {
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            backgroundMode = StatusBarBackgroundMode.MatchAppBackground,
            appBackgroundColor = Color(0xFFF2F2F2), // light gray
        )
        assertEquals(Color(0xFFF2F2F2), snapshot.backgroundColor)
        assertEquals(Color.Black, snapshot.foregroundColor)
    }

    @Test
    fun resolve_matchAppBackground_keepsWhiteOnDarkFill() {
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            backgroundMode = StatusBarBackgroundMode.MatchAppBackground,
            appBackgroundColor = Color(0xFF0050EF), // cobalt
        )
        assertEquals(Color(0xFF0050EF), snapshot.backgroundColor)
        assertEquals(Color.White, snapshot.foregroundColor)
    }

    @Test
    fun resolve_showAccent_ignoresAppBackground() {
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            backgroundMode = StatusBarBackgroundMode.ShowAccentColor,
            appBackgroundColor = Color(0xFFF0A30A),
        )
        assertEquals(preferences.accentColor, snapshot.backgroundColor)
        assertEquals(Color.White, snapshot.foregroundColor)
    }

    @Test
    fun resolve_showAccent_lightAccent_usesBlackGlyphs() {
        preferences.accentColorHex = "#A4C400" // lime — light fill
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            backgroundMode = StatusBarBackgroundMode.ShowAccentColor,
        )
        assertEquals(preferences.accentColor, snapshot.backgroundColor)
        assertEquals(Color.Black, snapshot.foregroundColor)
    }

    @Test
    fun resolve_showAccent_amber_usesBlackGlyphs() {
        preferences.accentColorHex = "#F0A30A"
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            backgroundMode = StatusBarBackgroundMode.ShowAccentColor,
        )
        assertEquals(Color.Black, snapshot.foregroundColor)
    }

    @Test
    fun resolve_defaultBlack_ignoresAppBackground() {
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            backgroundMode = StatusBarBackgroundMode.DefaultBlackBackground,
            appBackgroundColor = Color(0xFFF0A30A),
        )
        assertEquals(Color.Black, snapshot.backgroundColor)
        assertEquals(Color.White, snapshot.foregroundColor)
    }

    @Test
    fun resolve_transparent_keepsClearTrayWithThemeContrastGlyphs() {
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            backgroundMode = StatusBarBackgroundMode.Transparent,
            appBackgroundColor = Color(0xFFF0A30A),
        )
        assertEquals(Color.Transparent, snapshot.backgroundColor)
        assertEquals(Color.Black, snapshot.backdropColor)
        assertEquals(Color.White, snapshot.foregroundColor)
    }

    @Test
    fun resolve_transparent_lightTheme_usesDarkGlyphs() {
        preferences.themeMode = com.metro.system.MetroThemeMode.Light
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            backgroundMode = StatusBarBackgroundMode.Transparent,
        )
        assertEquals(Color.Transparent, snapshot.backgroundColor)
        assertEquals(Color.White, snapshot.backdropColor)
        assertEquals(Color.Black, snapshot.foregroundColor)
    }

    @Test
    fun resolve_transparent_contentBackdrop_overridesSystemTheme() {
        // Dark Metro theme, but light content under the tray (e.g. Chrome).
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            backgroundMode = StatusBarBackgroundMode.Transparent,
            contentBackdropColor = Color(0xFFF5F5F5),
        )
        assertEquals(Color.Transparent, snapshot.backgroundColor)
        assertEquals(Color(0xFFF5F5F5), snapshot.backdropColor)
        assertEquals(Color.Black, snapshot.foregroundColor)
    }

    @Test
    fun resolve_transparent_darkContentBackdrop_keepsWhiteGlyphs() {
        preferences.themeMode = com.metro.system.MetroThemeMode.Light
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            backgroundMode = StatusBarBackgroundMode.Transparent,
            contentBackdropColor = Color(0xFF121212),
        )
        assertEquals(Color.White, snapshot.foregroundColor)
    }

    @Test
    fun resolve_matchAppBackground_fallsBackToBlackWhenAppColorMissing() {
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            backgroundMode = StatusBarBackgroundMode.MatchAppBackground,
            appBackgroundColor = null,
        )
        assertEquals(Color.Black, snapshot.backgroundColor)
        assertEquals(Color.White, snapshot.foregroundColor)
    }

    @Test
    fun resolve_shellFill_overridesThemeAndMatchApp() {
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            backgroundMode = StatusBarBackgroundMode.MatchAppBackground,
            appBackgroundColor = Color(0xFFF2F2F2),
            shellFillColor = Color(0xFF1BA1E2),
        )
        assertEquals(Color(0xFF1BA1E2), snapshot.backgroundColor)
        assertEquals(Color.White, snapshot.foregroundColor)
    }

    @Test
    fun resolve_shellFillUnderlay_keepsTransparentTrayWithContrastGlyphs() {
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            shellFillColor = Color(0xFF252525),
            shellFillUnderlay = true,
        )
        assertEquals(Color.Transparent, snapshot.backgroundColor)
        assertEquals(Color(0xFF252525), snapshot.backdropColor)
        assertEquals(Color.White, snapshot.foregroundColor)
    }

    @Test
    fun resolve_shellFill_lightAccent_usesDarkGlyphs() {
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            shellFillColor = Color(0xFFF2F2F2),
        )
        assertEquals(Color(0xFFF2F2F2), snapshot.backgroundColor)
        assertEquals(Color.Black, snapshot.foregroundColor)
    }

    @Test
    fun resolve_shellFill_hidden_staysTransparent() {
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            visibilityMode = TrayVisibilityMode.Hidden,
            shellFillColor = Color(0xFF252525),
        )
        assertEquals(Color.Transparent, snapshot.backgroundColor)
    }

    @Test
    fun foregroundForBackground_threshold() {
        assertEquals(Color.Black, TrayThemeResolver.foregroundForBackground(Color.White))
        assertEquals(Color.White, TrayThemeResolver.foregroundForBackground(Color.Black))
        assertEquals(Color.Black, TrayThemeResolver.foregroundForBackground(Color(0xFFA4C400)))
        assertEquals(Color.White, TrayThemeResolver.foregroundForBackground(Color(0xFF1BA1E2)))
        assertTrue(TrayThemeResolver.isLightBackground(Color(0xFFF0A30A)))
        assertFalse(TrayThemeResolver.isLightBackground(Color(0xFF0050EF)))
    }
}

@RunWith(RobolectricTestRunner::class)
class ForegroundAppDetectorTest {
    @Test
    fun foregroundPackage_prefersActiveApplicationWindow() {
        val pkg = ForegroundAppDetector.foregroundPackageFromProbes(
            listOf(
                ForegroundAppDetector.PackageProbe(
                    type = AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY,
                    layer = 100,
                    packageName = "com.metro.statusbar",
                    isActive = true,
                ),
                ForegroundAppDetector.PackageProbe(
                    type = AccessibilityWindowInfo.TYPE_APPLICATION,
                    layer = 40,
                    packageName = "com.metro.settings",
                    isActive = true,
                ),
                ForegroundAppDetector.PackageProbe(
                    type = AccessibilityWindowInfo.TYPE_APPLICATION,
                    layer = 30,
                    packageName = "com.android.chrome",
                    isActive = false,
                ),
            ),
        )
        assertEquals("com.metro.settings", pkg)
    }

    @Test
    fun isIgnored_shellPackages() {
        assertTrue(ForegroundAppDetector.isIgnored("com.android.systemui"))
        assertTrue(ForegroundAppDetector.isIgnored("com.metro.navbar"))
        assertTrue(ForegroundAppDetector.isIgnored("com.metro.volume"))
        assertTrue(ForegroundAppDetector.isIgnored("com.metro.lockscreen"))
        // Notifications setup is a normal activity — tray must stay visible over it.
        assertFalse(ForegroundAppDetector.isIgnored("com.metro.notifications"))
        assertFalse(ForegroundAppDetector.isIgnored("com.metro.statusbar"))
        assertFalse(ForegroundAppDetector.isIgnored("com.metro.settings"))
        assertFalse(ForegroundAppDetector.isIgnored("com.android.chrome"))
    }

    @Test
    fun foregroundPackage_detectsNotificationsSetup() {
        val pkg = ForegroundAppDetector.foregroundPackageFromProbes(
            listOf(
                ForegroundAppDetector.PackageProbe(
                    type = AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY,
                    layer = 100,
                    packageName = "com.metro.statusbar",
                    isActive = true,
                ),
                ForegroundAppDetector.PackageProbe(
                    type = AccessibilityWindowInfo.TYPE_APPLICATION,
                    layer = 50,
                    packageName = "com.metro.notifications",
                    isActive = true,
                    isFocused = true,
                ),
                ForegroundAppDetector.PackageProbe(
                    type = AccessibilityWindowInfo.TYPE_APPLICATION,
                    layer = 30,
                    packageName = "com.metro.settings",
                    isActive = false,
                ),
            ),
        )
        assertEquals("com.metro.notifications", pkg)
    }
}
