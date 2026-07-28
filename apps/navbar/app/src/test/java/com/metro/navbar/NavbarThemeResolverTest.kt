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
    prefs.navBarColorHex = null

    val snapshot = NavbarThemeResolver.resolve(prefs)

    assertEquals(Color.Black, snapshot.barColor)
    assertEquals(Color.White, snapshot.iconColor)
    assertTrue(snapshot.darkTheme)
  }

  @Test
  fun usesAccentOverrideWhenSet() {
    val context = RuntimeEnvironment.getApplication()
    val prefs = MetroPreferences(context)
    prefs.themeMode = MetroThemeMode.Dark
    prefs.navBarColorHex = "#1BA1E2"

    val snapshot = NavbarThemeResolver.resolve(prefs)

    assertEquals(MetroPreferences.parseAccentHex("#1BA1E2"), snapshot.barColor)
    assertEquals(Color.White, snapshot.iconColor)
  }

  @Test
  fun usesDarkIconsOnLightBar() {
    val context = RuntimeEnvironment.getApplication()
    val prefs = MetroPreferences(context)
    prefs.themeMode = MetroThemeMode.Light
    prefs.navBarColorHex = "#F2F2F2"

    val snapshot = NavbarThemeResolver.resolve(prefs)

    assertEquals(MetroPreferences.parseAccentHex("#F2F2F2"), snapshot.barColor)
    assertEquals(Color.Black, snapshot.iconColor)
  }
}
