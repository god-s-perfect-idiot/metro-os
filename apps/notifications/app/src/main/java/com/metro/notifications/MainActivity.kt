package com.metro.notifications

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.metro.ui.MetroActivities
import com.metro.ui.MetroSplash
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroSystemTheme
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
            val notifPrefs = remember { NotificationsPreferences(context) }
            var permissionTick by remember { mutableIntStateOf(0) }
            var enabled by remember { mutableStateOf(notifPrefs.enabled) }
            var toastDurationMs by remember { mutableLongStateOf(notifPrefs.toastDurationMs) }

            DisposableEffect(this@MainActivity) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionTick++
                        enabled = notifPrefs.enabled
                        if (notifPrefs.enabled) {
                            HeadsUpController.disableStockHeadsUp(context)
                            ActionNotificationListenerService.requestHeadsUpSuppression()
                        }
                        if (notifPrefs.enabled &&
                            Settings.canDrawOverlays(context) &&
                            NotificationsAccessibilityService.isEnabled() &&
                            !NotificationsOverlayService.isRunning()
                        ) {
                            NotificationsOverlayService.start(context)
                        }
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            val overlayGranted = remember(permissionTick) { Settings.canDrawOverlays(context) }
            val accessibilityEnabled = remember(permissionTick) {
                NotificationsAccessibilityService.isEnabled()
            }
            val notificationAccess = remember(permissionTick) {
                ActionNotificationListenerService.isEnabled(context)
            }
            val canToggle = overlayGranted && accessibilityEnabled

            MetroSystemTheme {
                MetroAppPivotShell(
                    modifier = Modifier.fillMaxSize(),
                    onExit = { MetroActivities.finishWithExitTransition(this@MainActivity) },
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .metroNavBarPadding()
                        .verticalScroll(rememberScrollState()),
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
                        text = stringResource(R.string.grant_accessibility),
                        enabled = !accessibilityEnabled,
                        onClick = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MetroBorderButton(
                        text = stringResource(R.string.grant_notifications),
                        enabled = !notificationAccess,
                        onClick = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    MetroToggleSwitch(
                        checked = enabled,
                        onCheckedChange = { value ->
                            NotificationsOverlayService.applyMasterToggle(context, value)
                            enabled = notifPrefs.enabled
                        },
                        enabled = canToggle || enabled,
                        label = stringResource(R.string.show_notifications),
                        labelStyle = MetroTextStyle.DialogBody,
                        statusStyle = MetroTextStyle.Body,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    )
                    if (!canToggle && !enabled) {
                        MetroText(
                            text = stringResource(R.string.show_notifications_hint),
                            style = MetroTextStyle.DialogBody,
                            color = MetroTheme.colors.secondaryText,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(top = 8.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    MetroListPicker(
                        selected = toastDurationMs,
                        options = listOf(
                            MetroListPickerOption(
                                ToastSpec.DURATION_3S_MS,
                                stringResource(R.string.toast_timeout_3s),
                            ),
                            MetroListPickerOption(
                                ToastSpec.DURATION_5S_MS,
                                stringResource(R.string.toast_timeout_5s),
                            ),
                            MetroListPickerOption(
                                ToastSpec.DURATION_10S_MS,
                                stringResource(R.string.toast_timeout_10s),
                            ),
                        ),
                        onSelectedChange = { ms ->
                            notifPrefs.toastDurationMs = ms
                            toastDurationMs = notifPrefs.toastDurationMs
                        },
                        label = stringResource(R.string.toast_timeout),
                        labelStyle = MetroTextStyle.DialogBody,
                        optionStyle = MetroTextStyle.DialogBody,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    MetroBorderButton(
                        text = stringResource(R.string.show_test_toast),
                        enabled = enabled && canToggle,
                        onClick = { NotificationsOverlayService.showTestToast(context) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontSize = 15.sp,
                    )
                }
                }
            }
        }
    }
}
