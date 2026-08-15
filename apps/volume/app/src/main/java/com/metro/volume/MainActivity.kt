package com.metro.volume

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.system.MetroPreferences
import com.metro.ui.MetroActivities
import com.metro.ui.MetroSplash
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
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { MetroPreferences(context) }
            val hudPrefs = remember { VolumeHudPreferences(context) }
            var permissionTick by remember { mutableIntStateOf(0) }
            var hudEnabled by remember { mutableStateOf(hudPrefs.enabled) }

            DisposableEffect(this@MainActivity) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionTick++
                        hudEnabled = hudPrefs.enabled
                        // Keep overlay aligned with the master toggle after returning from Settings.
                        if (hudPrefs.enabled &&
                            Settings.canDrawOverlays(context) &&
                            VolumeAccessibilityService.isEnabled() &&
                            !VolumeOverlayService.isRunning()
                        ) {
                            VolumeOverlayService.start(context)
                        }
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            val overlayGranted = remember(permissionTick) { Settings.canDrawOverlays(context) }
            val accessibilityEnabled = remember(permissionTick) {
                VolumeAccessibilityService.isEnabled()
            }
            val accent = remember(permissionTick) { prefs.accentColor }
            val canToggleHud = overlayGranted && accessibilityEnabled

            MetroTheme(darkTheme = prefs.isDark, accent = accent) {
                MetroAppPivotShell(
                    modifier = Modifier.fillMaxSize(),
                    onExit = { MetroActivities.finishWithExitTransition(this@MainActivity) },
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
                        style = MetroTextStyle.PivotTab,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    )
                    MetroText(
                        text = stringResource(R.string.setup_body),
                        style = MetroTextStyle.DialogBody,
                        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 16.dp),
                    )

                    MetroBorderButton(
                        text = stringResource(R.string.grant_overlay),
                        enabled = !overlayGranted,
                        onClick = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName"),
                                ),
                            )
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MetroBorderButton(
                        text = stringResource(R.string.enable_accessibility),
                        enabled = !accessibilityEnabled,
                        onClick = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    MetroToggleSwitch(
                        checked = hudEnabled,
                        onCheckedChange = { enabled ->
                            VolumeOverlayService.applyMasterToggle(context, enabled)
                            hudEnabled = hudPrefs.enabled
                        },
                        enabled = canToggleHud || hudEnabled,
                        label = stringResource(R.string.show_volume_hud),
                        labelStyle = MetroTextStyle.DialogBody,
                        statusStyle = MetroTextStyle.Body,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    )
                    if (!canToggleHud && !hudEnabled) {
                        MetroText(
                            text = stringResource(R.string.show_volume_hud_hint),
                            style = MetroTextStyle.DialogBody,
                            color = MetroTheme.colors.secondaryText,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(top = 8.dp),
                        )
                    }
                    if (hudEnabled && overlayGranted && accessibilityEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        MetroText(
                            text = stringResource(R.string.setup_ready),
                            style = MetroTextStyle.DialogBody,
                            color = MetroTheme.colors.secondaryText,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
                }
            }
        }
    }
}
