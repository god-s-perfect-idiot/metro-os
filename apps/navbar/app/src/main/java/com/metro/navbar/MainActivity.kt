package com.metro.navbar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.metroNavBarPadding

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    applyBlackSystemNavigationBar()
    setContent {
      val context = LocalContext.current
      val state = remember { NavbarState(context) }
      var permissionTick by remember { mutableIntStateOf(0) }
      var overlayEnabled by remember { mutableStateOf(NavbarOverlayController.isActive) }

      DisposableEffect(this@MainActivity) {
        val observer = LifecycleEventObserver { _, event ->
          if (event == Lifecycle.Event.ON_RESUME) {
            permissionTick++
            overlayEnabled = NavbarOverlayController.isActive
          }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
      }

      DisposableEffect(state) {
        state.registerReceivers(context)
        state.refreshTheme()
        onDispose { state.unregisterReceivers(context) }
      }

      val threeButtonNav = remember(permissionTick) {
        SystemNavigationMode.isThreeButton(context)
      }
      val overlayGranted = remember(permissionTick) { Settings.canDrawOverlays(context) }
      val accessibilityEnabled = remember(permissionTick) { NavbarAccessibilityService.isEnabled() }
      val canEnableOverlay = threeButtonNav && overlayGranted && accessibilityEnabled
      val toggleInteractive = overlayEnabled || canEnableOverlay

      MetroTheme(
        darkTheme = state.theme.darkTheme,
        accent = state.theme.barColor,
      ) {
        MetroAppPivotShell(
          modifier = Modifier.fillMaxSize(),
          onExit = { finish() },
        ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding(),
          verticalArrangement = Arrangement.Top,
        ) {
          MetroAppTitle(title = stringResource(R.string.app_name))
          MetroText(
            text = stringResource(R.string.setup_title),
            style = MetroTextStyle.HubTitle,
            modifier = Modifier
              .padding(horizontal = 12.dp)
              .padding(bottom = 12.dp),
          )

          MetroToggleSwitch(
            checked = overlayEnabled && threeButtonNav,
            onCheckedChange = { enabled ->
              if (enabled) {
                overlayEnabled = NavbarOverlayService.start(context)
              } else {
                NavbarOverlayService.stop(context)
                overlayEnabled = false
              }
            },
            enabled = toggleInteractive,
            label = stringResource(R.string.master_toggle),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp)
              .padding(bottom = 24.dp),
          )

          if (!threeButtonNav) {
            MetroText(
              text = stringResource(R.string.three_button_required_title),
              style = MetroTextStyle.SectionHeader,
              modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp),
            )
            MetroText(
              text = stringResource(R.string.three_button_required_body),
              style = MetroTextStyle.DialogBody,
              modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 16.dp),
            )
            MetroBorderButton(
              text = stringResource(R.string.open_system_navigation),
              onClick = { SystemNavigationMode.openSystemNavigationSettings(context) },
              modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
          }

          MetroText(
            text = stringResource(R.string.permission_overlay_body),
            style = MetroTextStyle.DialogBody,
            modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 16.dp),
          )
          MetroBorderButton(
            text = stringResource(R.string.grant_overlay),
            enabled = threeButtonNav && !overlayGranted,
            onClick = {
              startActivity(
                Intent(
                  Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                  Uri.parse("package:$packageName"),
                ),
              )
            },
            modifier = Modifier.padding(horizontal = 12.dp),
          )
          Spacer(modifier = Modifier.height(12.dp))
          MetroBorderButton(
            text = stringResource(R.string.grant_accessibility),
            enabled = threeButtonNav && !accessibilityEnabled,
            onClick = { NavbarActions.openAccessibilitySettings(context) },
            modifier = Modifier.padding(horizontal = 12.dp),
          )
          Spacer(modifier = Modifier.height(32.dp))
          MetroText(
            text = "Preview",
            style = MetroTextStyle.SectionHeader,
            modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp),
          )
          NavbarWithSystemChrome(
            theme = state.theme,
            onBack = { NavbarActions.dispatchBack(context) },
            onBackLongPress = { NavbarActions.dispatchRecents(context) },
            onStart = { NavbarActions.launchStart(context) },
            onSearch = { NavbarActions.launchGoogleSearch(context) },
            onSearchLongPress = { NavbarActions.launchGemini(context) },
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp),
          )
        }
        }
      }
    }
  }
}
