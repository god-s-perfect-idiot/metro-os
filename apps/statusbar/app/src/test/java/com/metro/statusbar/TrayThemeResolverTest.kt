package com.metro.statusbar

import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.graphics.Color
import com.metro.system.MetroPreferences
import com.metro.ui.MetroColors
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
    fun resolve_defaultsToThemeBackgroundAndForeground() {
        val snapshot = TrayThemeResolver.resolve(preferences)
        assertEquals(MetroColors.DarkBackground, snapshot.backgroundColor)
        assertEquals(MetroColors.DarkPrimaryText, snapshot.foregroundColor)
    }

    @Test
    fun resolve_matchAppBackground_usesAppFillAndInvertsLight() {
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            matchAppBackground = true,
            appBackgroundColor = Color(0xFFF2F2F2), // light gray
        )
        assertEquals(Color(0xFFF2F2F2), snapshot.backgroundColor)
        assertEquals(Color.Black, snapshot.foregroundColor)
    }

    @Test
    fun resolve_matchAppBackground_keepsWhiteOnDarkFill() {
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            matchAppBackground = true,
            appBackgroundColor = Color(0xFF0050EF), // cobalt
        )
        assertEquals(Color(0xFF0050EF), snapshot.backgroundColor)
        assertEquals(Color.White, snapshot.foregroundColor)
    }

    @Test
    fun resolve_matchDisabled_ignoresAppBackground() {
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            matchAppBackground = false,
            appBackgroundColor = Color(0xFFF0A30A),
        )
        assertEquals(MetroColors.DarkBackground, snapshot.backgroundColor)
        assertEquals(MetroColors.DarkPrimaryText, snapshot.foregroundColor)
    }

    @Test
    fun resolve_metroSuite_usesMetroPageFillNotMatchedColor() {
        preferences.themeMode = com.metro.system.MetroThemeMode.Light
        val snapshot = TrayThemeResolver.resolve(
            preferences = preferences,
            matchAppBackground = true,
            appBackgroundColor = Color(0xFFF0A30A),
            metroSuiteForeground = true,
        )
        assertEquals(MetroColors.LightBackground, snapshot.backgroundColor)
        assertEquals(MetroColors.LightPrimaryText, snapshot.foregroundColor)
    }

    @Test
    fun foregroundForBackground_threshold() {
        assertEquals(Color.Black, TrayThemeResolver.foregroundForBackground(Color.White))
        assertEquals(Color.White, TrayThemeResolver.foregroundForBackground(Color.Black))
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
        assertFalse(ForegroundAppDetector.isIgnored("com.metro.statusbar"))
        assertFalse(ForegroundAppDetector.isIgnored("com.metro.settings"))
        assertFalse(ForegroundAppDetector.isIgnored("com.android.chrome"))
    }
}
