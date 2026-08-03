package com.metro.statusbar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.statusbar.ui.StatusTray
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
        setContent {
            val context = LocalContext.current
            val state = remember { TrayState(context) }
            val trayPrefs = remember { StatusTrayPreferences(context) }
            var permissionTick by remember { mutableIntStateOf(0) }
            var trayEnabled by remember { mutableStateOf(trayPrefs.enabled) }

            DisposableEffect(this@MainActivity) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionTick++
                        trayEnabled = trayPrefs.enabled
                        // Keep overlay aligned with the master toggle after returning from Settings.
                        if (trayPrefs.enabled &&
                            Settings.canDrawOverlays(context) &&
                            StatusBarAccessibilityService.isEnabled() &&
                            !StatusBarOverlayService.isRunning()
                        ) {
                            StatusBarOverlayService.start(context)
                        }
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            DisposableEffect(state) {
                state.registerReceivers(context)
                state.refreshTheme()
                state.refreshClock()
                onDispose { state.unregisterReceivers(context) }
            }

            val overlayGranted = remember(permissionTick) { Settings.canDrawOverlays(context) }
            val accessibilityEnabled = remember(permissionTick) { StatusBarAccessibilityService.isEnabled() }
            val notificationAccess = remember(permissionTick) {
                ActionNotificationListenerService.isEnabled(context)
            }
            val phoneStateGranted = remember(permissionTick) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                    PackageManager.PERMISSION_GRANTED
            }
            val phoneStatePermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { granted ->
                permissionTick++
                if (granted) {
                    state.refreshDataConnectionLabel()
                }
            }
            val canToggleTray = overlayGranted && accessibilityEnabled

            MetroTheme(
                darkTheme = state.theme.darkTheme,
                accent = state.theme.accentColor,
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
                    MetroText(
                        text = stringResource(R.string.permission_overlay_body),
                        style = MetroTextStyle.Body,
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
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MetroBorderButton(
                        text = stringResource(R.string.grant_phone_state),
                        enabled = !phoneStateGranted,
                        onClick = {
                            phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MetroBorderButton(
                        text = stringResource(R.string.grant_accessibility),
                        enabled = !accessibilityEnabled,
                        onClick = {
                            startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MetroBorderButton(
                        text = stringResource(R.string.grant_notifications),
                        enabled = !notificationAccess,
                        onClick = {
                            startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    MetroToggleSwitch(
                        checked = trayEnabled,
                        onCheckedChange = { enabled ->
                            StatusBarOverlayService.applyMasterToggle(context, enabled)
                            trayEnabled = trayPrefs.enabled
                        },
                        enabled = canToggleTray || trayEnabled,
                        label = stringResource(R.string.show_status_tray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    )
                    if (!canToggleTray && !trayEnabled) {
                        MetroText(
                            text = stringResource(R.string.show_status_tray_hint),
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(top = 8.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    MetroText(
                        text = "Preview",
                        style = MetroTextStyle.SectionHeader,
                        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp),
                    )
                    StatusTray(
                        snapshot = state.snapshot,
                        onTrayTap = { state.toggleExpanded() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    )
                }
            }
        }
    }
}
