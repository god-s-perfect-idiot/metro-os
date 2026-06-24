package com.metro.navbar

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.metro.navbar.ui.HiddenNavbarRevealStrip
import com.metro.navbar.ui.NavigationBar

fun Activity.applyBlackSystemNavigationBar() {
  window.navigationBarColor = AndroidColor.BLACK
  WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = false
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    window.isNavigationBarContrastEnforced = false
  }
}

@Composable
fun NavbarWithSystemChrome(
  theme: NavbarThemeSnapshot,
  onBack: () -> Unit,
  onBackLongPress: () -> Unit,
  onStart: () -> Unit,
  onSearch: () -> Unit,
  onSearchLongPress: () -> Unit,
  modifier: Modifier = Modifier,
) {
  NavigationBar(
    theme = theme,
    onBack = onBack,
    onBackLongPress = onBackLongPress,
    onStart = onStart,
    onSearch = onSearch,
    onSearchLongPress = onSearchLongPress,
    modifier = modifier
      .fillMaxWidth()
      .background(theme.barColor),
  )
}

@Composable
fun HiddenNavbarWithSystemChrome(
  theme: NavbarThemeSnapshot,
  onReveal: () -> Unit,
  modifier: Modifier = Modifier,
) {
  HiddenNavbarRevealStrip(
    theme = theme,
    onReveal = onReveal,
    modifier = modifier
      .fillMaxWidth()
      .background(theme.barColor),
  )
}
