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
  fun alwaysUsesBlackBarWithWhiteIcons() {
    val context = RuntimeEnvironment.getApplication()
    val prefs = MetroPreferences(context)
    prefs.themeMode = MetroThemeMode.Dark
    prefs.navBarColorHex = "#1BA1E2"

    val snapshot = NavbarThemeResolver.resolve(prefs)

    assertEquals(Color.Black, snapshot.barColor)
    assertEquals(Color.White, snapshot.iconColor)
    assertTrue(snapshot.darkTheme)
  }

  @Test
  fun ignoresLightNavBarPreference() {
    val context = RuntimeEnvironment.getApplication()
    val prefs = MetroPreferences(context)
    prefs.themeMode = MetroThemeMode.Light
    prefs.navBarColorHex = "#F2F2F2"

    val snapshot = NavbarThemeResolver.resolve(prefs)

    assertEquals(Color.Black, snapshot.barColor)
    assertEquals(Color.White, snapshot.iconColor)
  }
}
