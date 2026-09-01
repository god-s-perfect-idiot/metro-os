package com.metro.lockscreen

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.system.MetroAppDiscovery
import com.metro.ui.MetroActivities
import com.metro.ui.MetroAppPickerEntry
import com.metro.ui.MetroAppPickerScreen
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption
import com.metro.ui.MetroSplash
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private sealed interface SetupRoute {
    data object Main : SetupRoute
    data class Crop(val uri: Uri) : SetupRoute
    data class PickQuickStatusApp(val slotIndex: Int) : SetupRoute
}

class MainActivity : ComponentActivity() {

    private var permissionRefresh: (() -> Unit)? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        permissionRefresh?.invoke()
    }

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        permissionRefresh?.invoke()
    }

    private val phoneStatePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        permissionRefresh?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { LockscreenPreferences(context) }
            val scope = rememberCoroutineScope()
            var permissionTick by remember { mutableIntStateOf(0) }
            var enabled by remember { mutableStateOf(prefs.enabled) }
            var backgroundMode by remember { mutableStateOf(prefs.backgroundMode) }
            var customEnabled by remember { mutableStateOf(prefs.customBackgroundEnabled) }
            var customEpoch by remember { mutableIntStateOf(0) }
            var quickStatusEpoch by remember { mutableIntStateOf(0) }
            var route by remember { mutableStateOf<SetupRoute>(SetupRoute.Main) }

            DisposableEffect(this@MainActivity) {
                permissionRefresh = { permissionTick++ }
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionTick++
                        enabled = prefs.enabled
                        backgroundMode = prefs.backgroundMode
                        customEnabled = prefs.customBackgroundEnabled
                        if (prefs.enabled &&
                            LockscreenAccessibilityService.isEnabled() &&
                            !LockscreenHostService.isRunning()
                        ) {
                            LockscreenHostService.start(context)
                        }
                    }
                }
                lifecycle.addObserver(observer)
                onDispose {
                    permissionRefresh = null
                    lifecycle.removeObserver(observer)
                }
            }

            val accessibilityEnabled = remember(permissionTick) {
                LockscreenAccessibilityService.isEnabled()
            }
            val notificationsGranted = remember(permissionTick) {
                areNotificationsEnabled(context)
            }
            val calendarGranted = remember(permissionTick) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CALENDAR,
                ) == PackageManager.PERMISSION_GRANTED
            }
            val phoneStateGranted = remember(permissionTick) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE,
                ) == PackageManager.PERMISSION_GRANTED
            }
            val fullscreenGranted = remember(permissionTick) {
                canUseFullScreenIntent(context)
            }
            val notificationAccessGranted = remember(permissionTick) {
                LockscreenNotificationAccess.isEnabled(context)
            }
            val quickStatusSlots = remember(quickStatusEpoch) { prefs.quickStatusSlots() }
            val launchableApps = remember(permissionTick) {
                MetroAppDiscovery.discoverInstalledApps(context).map {
                    MetroAppPickerEntry(it.packageName, it.label)
                }
            }
            val canToggle = accessibilityEnabled

            MetroSystemTheme {
                when (val current = route) {
                    is SetupRoute.Crop -> {
                        BackHandler { route = SetupRoute.Main }
                        LockscreenBackgroundCropScreen(
                            sourceUri = current.uri,
                            onSaved = {
                                customEnabled = prefs.customBackgroundEnabled
                                customEpoch++
                                LockscreenHostService.requestRehost()
                                route = SetupRoute.Main
                            },
                            onCancel = { route = SetupRoute.Main },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    is SetupRoute.PickQuickStatusApp -> {
                        BackHandler { route = SetupRoute.Main }
                        MetroAppPivotShell(
                            modifier = Modifier.fillMaxSize(),
                            onExit = { route = SetupRoute.Main },
                            skipEnter = false,
                        ) {
                            MetroAppPickerScreen(
                                apps = launchableApps,
                                selectedPackageName = quickStatusSlots[current.slotIndex],
                                headerTitle = stringResource(R.string.choose_app_header),
                                onSelected = { packageName ->
                                    prefs.setQuickStatusSlot(current.slotIndex, packageName)
                                    quickStatusEpoch++
                                    LockscreenHostService.requestRehost()
                                    route = SetupRoute.Main
                                },
                                onBack = { route = SetupRoute.Main },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .metroNavBarPadding(),
                            )
                        }
                    }
                    SetupRoute.Main -> {
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
                                    style = MetroTextStyle.HubTitle,
                                    modifier = Modifier
                                        .padding(start = MetroDimens.ScreenHorizontalMargin)
                                        .padding(bottom = 16.dp),
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.Top,
                                ) {
                                MetroToggleSwitch(
                                    checked = enabled,
                                    onCheckedChange = { value ->
                                        LockscreenHostService.applyMasterToggle(context, value)
                                        enabled = prefs.enabled
                                    },
                                    enabled = canToggle || enabled,
                                    label = stringResource(R.string.show_lockscreen),
                                    labelStyle = MetroTextStyle.Body,
                                    statusStyle = MetroTextStyle.ListItemTitle,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                )

                                if (!canToggle && !enabled) {
                                    MetroText(
                                        text = stringResource(R.string.show_lockscreen_hint),
                                        style = MetroTextStyle.Body,
                                        color = MetroTheme.colors.secondaryText,
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp)
                                            .padding(top = 8.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                MetroListPicker(
                                    selected = backgroundMode,
                                    options = listOf(
                                        MetroListPickerOption(
                                            LockscreenBackgroundMode.Accent,
                                            stringResource(R.string.background_mode_accent),
                                        ),
                                        MetroListPickerOption(
                                            LockscreenBackgroundMode.Custom,
                                            stringResource(R.string.background_mode_custom),
                                        ),
                                        MetroListPickerOption(
                                            LockscreenBackgroundMode.Bing,
                                            stringResource(R.string.background_mode_bing),
                                        ),
                                    ),
                                    onSelectedChange = { mode ->
                                        prefs.backgroundMode = mode
                                        backgroundMode = prefs.backgroundMode
                                        LockscreenHostService.requestRehost()
                                        if (mode == LockscreenBackgroundMode.Bing) {
                                            scope.launch(Dispatchers.IO) {
                                                BingWallpaperCache.ensureFresh(context)
                                                LockscreenHostService.requestRehost()
                                            }
                                        }
                                    },
                                    label = stringResource(R.string.background_mode_label),
                                    labelStyle = MetroTextStyle.Body,
                                    optionStyle = MetroTextStyle.Body,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                )

                                if (backgroundMode == LockscreenBackgroundMode.Custom) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    LockscreenCustomBackgroundPicker(
                                        customEnabled = customEnabled,
                                        reloadEpoch = customEpoch,
                                        onBeginCrop = { uri -> route = SetupRoute.Crop(uri) },
                                        onCleared = {
                                            customEnabled = false
                                            customEpoch++
                                            LockscreenHostService.requestRehost()
                                        },
                                    )
                                }

                                Spacer(modifier = Modifier.height(28.dp))
                                LockscreenQuickStatusSetup(
                                    slots = quickStatusSlots,
                                    onSlotClick = { index ->
                                        route = SetupRoute.PickQuickStatusApp(index)
                                    },
                                )

                                if (enabled && accessibilityEnabled) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    MetroText(
                                        text = stringResource(R.string.setup_ready),
                                        style = MetroTextStyle.Body,
                                        color = MetroTheme.colors.secondaryText,
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.height(28.dp))
                                MetroText(
                                    text = stringResource(R.string.permissions_section),
                                    style = MetroTextStyle.SectionHeader,
                                    color = MetroTheme.colors.accent,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .padding(bottom = 8.dp),
                                )
                                MetroText(
                                    text = stringResource(R.string.setup_body),
                                    style = MetroTextStyle.Body,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .padding(bottom = 16.dp),
                                )

                                MetroBorderButton(
                                    text = stringResource(R.string.enable_accessibility),
                                    enabled = !accessibilityEnabled,
                                    onClick = {
                                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    fontSize = 18.sp,
                                )

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    MetroBorderButton(
                                        text = stringResource(R.string.grant_notifications),
                                        enabled = !notificationsGranted,
                                        onClick = {
                                            when {
                                                ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.POST_NOTIFICATIONS,
                                                ) != PackageManager.PERMISSION_GRANTED -> {
                                                    notificationPermissionLauncher.launch(
                                                        Manifest.permission.POST_NOTIFICATIONS,
                                                    )
                                                }
                                                else -> {
                                                    startActivity(
                                                        Intent(
                                                            Settings.ACTION_APP_NOTIFICATION_SETTINGS,
                                                        ).apply {
                                                            putExtra(
                                                                Settings.EXTRA_APP_PACKAGE,
                                                                packageName,
                                                            )
                                                        },
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        fontSize = 18.sp,
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                MetroBorderButton(
                                    text = stringResource(R.string.grant_notification_access),
                                    enabled = !notificationAccessGranted,
                                    onClick = {
                                        LockscreenNotificationAccess.openSettings(context)
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    fontSize = 18.sp,
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                MetroBorderButton(
                                    text = stringResource(R.string.grant_calendar),
                                    enabled = !calendarGranted,
                                    onClick = {
                                        calendarPermissionLauncher.launch(
                                            Manifest.permission.READ_CALENDAR,
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    fontSize = 18.sp,
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                MetroBorderButton(
                                    text = stringResource(R.string.grant_phone_state),
                                    enabled = !phoneStateGranted,
                                    onClick = {
                                        phoneStatePermissionLauncher.launch(
                                            Manifest.permission.READ_PHONE_STATE,
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    fontSize = 18.sp,
                                )

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    MetroBorderButton(
                                        text = stringResource(R.string.grant_fullscreen_intent),
                                        enabled = true,
                                        onClick = {
                                            startActivity(
                                                Intent(
                                                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                                    Uri.parse("package:$packageName"),
                                                ),
                                            )
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        fontSize = 18.sp,
                                    )
                                    if (fullscreenGranted) {
                                        MetroText(
                                            text = stringResource(R.string.fullscreen_intent_granted),
                                            style = MetroTextStyle.Body,
                                            color = MetroTheme.colors.secondaryText,
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp)
                                                .padding(top = 6.dp),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun canUseFullScreenIntent(context: android.content.Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.canUseFullScreenIntent()
    }

    private fun areNotificationsEnabled(context: android.content.Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.areNotificationsEnabled()
    }
}
