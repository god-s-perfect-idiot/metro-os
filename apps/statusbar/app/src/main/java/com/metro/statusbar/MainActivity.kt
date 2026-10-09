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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.metro.statusbar.ui.TrayIconsPreview
import com.metro.statusbar.ui.TrayLayoutEditor
import com.metro.statusbar.ui.TrayPreview
import com.metro.ui.MetroActivities
import com.metro.ui.MetroSplash
import com.metro.ui.MetroAppPivotShell
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption
import com.metro.ui.MetroPivot
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSubpageHost
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.MetroTransitions
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.launch

private sealed interface SetupRoute {
    data object Main : SetupRoute
    data object Configure : SetupRoute
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        MetroSplash.install(this)
        super.onCreate(savedInstanceState)
        MetroActivities.applyLaunchTransition(this)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val state = remember { TrayState(context) }
            val trayPrefs = remember { StatusTrayPreferences(context) }
            var permissionTick by remember { mutableIntStateOf(0) }
            var trayEnabled by remember { mutableStateOf(trayPrefs.enabled) }
            var iconHideTimeoutMs by remember { mutableLongStateOf(trayPrefs.iconHideTimeoutMs) }
            var notchPosition by remember { mutableStateOf(trayPrefs.notchPosition) }
            var backgroundMode by remember { mutableStateOf(trayPrefs.backgroundMode) }
            var iconFlags by remember { mutableStateOf(trayPrefs.iconFlags) }
            var trayLayout by remember { mutableStateOf(trayPrefs.layout) }
            var route by remember { mutableStateOf<SetupRoute>(SetupRoute.Main) }
            val pagerState = rememberPagerState(pageCount = { 2 })
            val scope = rememberCoroutineScope()
            val tabEdit = stringResource(R.string.tab_edit)
            val tabIcons = stringResource(R.string.tab_icons)
            val pivotTitles = remember(tabEdit, tabIcons) {
                listOf(tabEdit, tabIcons)
            }

            DisposableEffect(this@MainActivity) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionTick++
                        trayEnabled = trayPrefs.enabled
                        iconHideTimeoutMs = trayPrefs.iconHideTimeoutMs
                        notchPosition = trayPrefs.notchPosition
                        backgroundMode = trayPrefs.backgroundMode
                        iconFlags = trayPrefs.iconFlags
                        trayLayout = trayPrefs.layout
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
                    state.refreshSignalBars()
                }
            }
            val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) {
                permissionTick++
                state.refreshBluetoothAudio()
                StatusBarOverlayService.requestIconFlagsRefresh()
            }
            val notificationAccessEnabled = remember(permissionTick) {
                StatusBarNotificationAccess.isEnabled(context)
            }
            val bluetoothConnectGranted = remember(permissionTick) {
                BluetoothAudioSource.hasConnectPermission(context)
            }
            val canToggleTray = overlayGranted && accessibilityEnabled

            MetroTheme(
                darkTheme = state.theme.darkTheme,
                accent = state.theme.accentColor,
            ) {
                MetroAppPivotShell(
                    modifier = Modifier.fillMaxSize(),
                    onExit = { MetroActivities.finishWithExitTransition(this@MainActivity) },
                ) {
                    MetroSubpageHost(
                        route = route,
                        isRoot = { it is SetupRoute.Main },
                        parentOf = { SetupRoute.Main },
                        loadKeyOf = { current ->
                            when (current) {
                                SetupRoute.Configure -> "Configure"
                                SetupRoute.Main -> "Main"
                            }
                        },
                        onGoBack = { route = SetupRoute.Main },
                        modifier = Modifier.fillMaxSize(),
                        rootContent = {
                            MetroPivot(
                                titles = pivotTitles,
                                pagerState = pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                                    .metroNavBarPadding(),
                                header = {
                                    MetroAppTitle(title = stringResource(R.string.app_name))
                                },
                                onTitleClick = { index ->
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                            page = index,
                                            animationSpec = MetroTransitions.pivotTween(),
                                        )
                                    }
                                },
                            ) { page ->
                                when (page) {
                                    0 -> EditTab(
                                        state = state,
                                        trayEnabled = trayEnabled,
                                        canToggleTray = canToggleTray,
                                        backgroundMode = backgroundMode,
                                        iconHideTimeoutMs = iconHideTimeoutMs,
                                        notchPosition = notchPosition,
                                        overlayGranted = overlayGranted,
                                        accessibilityEnabled = accessibilityEnabled,
                                        phoneStateGranted = phoneStateGranted,
                                        notificationAccessEnabled = notificationAccessEnabled,
                                        bluetoothConnectGranted = bluetoothConnectGranted,
                                        onTrayEnabledChange = { enabled ->
                                            StatusBarOverlayService.applyMasterToggle(context, enabled)
                                            trayEnabled = trayPrefs.enabled
                                        },
                                        onBackgroundModeChange = { mode ->
                                            trayPrefs.backgroundMode = mode
                                            backgroundMode = trayPrefs.backgroundMode
                                            state.applyBackgroundModePreference()
                                            StatusBarOverlayService.requestBackgroundModeRefresh()
                                        },
                                        onIconHideTimeoutChange = { timeoutMs ->
                                            trayPrefs.iconHideTimeoutMs = timeoutMs
                                            iconHideTimeoutMs = trayPrefs.iconHideTimeoutMs
                                            StatusBarOverlayService.requestIconHideTimeoutRefresh()
                                        },
                                        onConfigureClick = { route = SetupRoute.Configure },
                                        onGrantOverlay = {
                                            startActivity(
                                                Intent(
                                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    Uri.parse("package:$packageName"),
                                                ),
                                            )
                                        },
                                        onGrantAccessibility = {
                                            startActivity(
                                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                },
                                            )
                                        },
                                        onGrantPhoneState = {
                                            phoneStatePermissionLauncher.launch(
                                                Manifest.permission.READ_PHONE_STATE,
                                            )
                                        },
                                        onGrantNotificationAccess = {
                                            StatusBarNotificationAccess.openSettings(context)
                                        },
                                        onGrantBluetooth = {
                                            if (android.os.Build.VERSION.SDK_INT >=
                                                android.os.Build.VERSION_CODES.S
                                            ) {
                                                bluetoothPermissionLauncher.launch(
                                                    Manifest.permission.BLUETOOTH_CONNECT,
                                                )
                                            }
                                        },
                                    )
                                    else -> IconsTab(
                                        flags = iconFlags,
                                        layout = trayLayout,
                                        clockText = state.clockText,
                                        onFlagsChange = { next ->
                                            trayPrefs.iconFlags = next
                                            iconFlags = trayPrefs.iconFlags
                                            val trimmed = TrayLayout.trimSpacersToMax(
                                                trayPrefs.layout,
                                                next,
                                            )
                                            if (trimmed != trayPrefs.layout) {
                                                trayPrefs.layout = trimmed
                                                trayLayout = trayPrefs.layout
                                                state.refreshLayout()
                                                StatusBarOverlayService.requestLayoutRefresh()
                                            }
                                            state.refreshIconFlags()
                                            StatusBarOverlayService.requestIconFlagsRefresh()
                                        },
                                    )
                                }
                            }
                        },
                        subpageContent = { current ->
                            when (current) {
                                SetupRoute.Configure -> ConfigurePage(
                                    layout = trayLayout,
                                    iconFlags = iconFlags,
                                    clockText = state.clockText,
                                    onLayoutChange = { next ->
                                        trayPrefs.layout = next
                                        trayLayout = trayPrefs.layout
                                        state.refreshLayout()
                                        StatusBarOverlayService.requestLayoutRefresh()
                                    },
                                )
                                SetupRoute.Main -> Unit
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigurePage(
    layout: List<TrayLayoutSlot>,
    iconFlags: TrayIconFlags,
    clockText: String,
    onLayoutChange: (List<TrayLayoutSlot>) -> Unit,
) {
    val visibleSlots = remember(layout, iconFlags) {
        TrayLayout.visible(layout, iconFlags)
    }
    val canAddSpacer = TrayLayout.canAddSpacer(
        slots = layout,
        flags = iconFlags,
        widthDp = TrayLayout.DEFAULT_SPACER_WIDTH_DP,
    )
    val canAddTinySpacer = TrayLayout.canAddSpacer(
        slots = layout,
        flags = iconFlags,
        widthDp = TrayLayout.TINY_SPACER_WIDTH_DP,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .metroNavBarPadding(),
    ) {
        MetroSettingsHeader(
            pageTitle = stringResource(R.string.configure_page_title),
            appTitle = stringResource(R.string.app_name),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            TrayLayoutEditor(
                slots = visibleSlots,
                iconFlags = iconFlags,
                clockText = clockText,
                onSlotsChange = { nextVisible ->
                    onLayoutChange(
                        mergeVisibleLayoutEdit(
                            full = layout,
                            flags = iconFlags,
                            newVisible = nextVisible,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        MetroBorderButton(
            text = stringResource(R.string.configure_add_spacer),
            enabled = canAddSpacer,
            onClick = {
                onLayoutChange(
                    TrayLayout.addSpacer(
                        slots = layout,
                        flags = iconFlags,
                        widthDp = TrayLayout.DEFAULT_SPACER_WIDTH_DP,
                    ),
                )
            },
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 15.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetroBorderButton(
            text = stringResource(R.string.configure_add_tiny_spacer),
            enabled = canAddTinySpacer,
            onClick = {
                onLayoutChange(
                    TrayLayout.addSpacer(
                        slots = layout,
                        flags = iconFlags,
                        widthDp = TrayLayout.TINY_SPACER_WIDTH_DP,
                    ),
                )
            },
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 15.sp,
        )
        MetroText(
            text = stringResource(
                if (canAddSpacer || canAddTinySpacer) {
                    R.string.configure_add_spacer_hint
                } else {
                    R.string.configure_add_spacer_max_hint
                },
            ),
            style = MetroTextStyle.DialogBody,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp, bottom = 24.dp),
        )
    }
}

/**
 * Applies a visible-slot edit (enabled icons + spacers) back onto the full layout,
 * keeping disabled icons parked just before the clock so they return when toggled on.
 */
private fun mergeVisibleLayoutEdit(
    full: List<TrayLayoutSlot>,
    flags: TrayIconFlags,
    newVisible: List<TrayLayoutSlot>,
): List<TrayLayoutSlot> {
    val disabled = full.filterIsInstance<TrayLayoutSlot.Icon>()
        .filter { !it.kind.isEnabled(flags) }
    if (disabled.isEmpty()) return newVisible
    val result = newVisible.toMutableList()
    val clockIdx = result.indexOfFirst {
        it is TrayLayoutSlot.Icon && it.kind == TrayLayoutIcon.Clock
    }
    val insertAt = if (clockIdx >= 0) clockIdx else result.size
    disabled.forEachIndexed { i, icon -> result.add(insertAt + i, icon) }
    return result
}

@Composable
private fun EditTab(
    state: TrayState,
    trayEnabled: Boolean,
    canToggleTray: Boolean,
    backgroundMode: StatusBarBackgroundMode,
    iconHideTimeoutMs: Long,
    notchPosition: NotchPosition,
    overlayGranted: Boolean,
    accessibilityEnabled: Boolean,
    phoneStateGranted: Boolean,
    notificationAccessEnabled: Boolean,
    bluetoothConnectGranted: Boolean,
    onTrayEnabledChange: (Boolean) -> Unit,
    onBackgroundModeChange: (StatusBarBackgroundMode) -> Unit,
    onIconHideTimeoutChange: (Long) -> Unit,
    onConfigureClick: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onGrantPhoneState: () -> Unit,
    onGrantNotificationAccess: () -> Unit,
    onGrantBluetooth: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
    ) {
        MetroToggleSwitch(
            checked = trayEnabled,
            onCheckedChange = onTrayEnabledChange,
            enabled = canToggleTray || trayEnabled,
            label = stringResource(R.string.show_status_tray),
            labelStyle = MetroTextStyle.DialogBody,
            statusStyle = MetroTextStyle.Body,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )
        if (!canToggleTray && !trayEnabled) {
            MetroText(
                text = stringResource(R.string.show_status_tray_hint),
                style = MetroTextStyle.DialogBody,
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
                    StatusBarBackgroundMode.DefaultBlackBackground,
                    stringResource(R.string.statusbar_background_default_black),
                ),
                MetroListPickerOption(
                    StatusBarBackgroundMode.MatchAppBackground,
                    stringResource(R.string.statusbar_background_match_app),
                ),
                MetroListPickerOption(
                    StatusBarBackgroundMode.ShowAccentColor,
                    stringResource(R.string.statusbar_background_show_accent),
                ),
            ),
            onSelectedChange = onBackgroundModeChange,
            label = stringResource(R.string.statusbar_background_label),
            labelStyle = MetroTextStyle.DialogBody,
            optionStyle = MetroTextStyle.DialogBody,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))
        MetroListPicker(
            selected = iconHideTimeoutMs,
            options = listOf(
                MetroListPickerOption(
                    StatusTrayPreferences.TIMEOUT_3S_MS,
                    stringResource(R.string.icon_hide_timeout_3s),
                ),
                MetroListPickerOption(
                    StatusTrayPreferences.TIMEOUT_5S_MS,
                    stringResource(R.string.icon_hide_timeout_5s),
                ),
                MetroListPickerOption(
                    StatusTrayPreferences.TIMEOUT_10S_MS,
                    stringResource(R.string.icon_hide_timeout_10s),
                ),
                MetroListPickerOption(
                    StatusTrayPreferences.TIMEOUT_NEVER_MS,
                    stringResource(R.string.icon_hide_timeout_never),
                ),
            ),
            onSelectedChange = onIconHideTimeoutChange,
            label = stringResource(R.string.icon_hide_timeout_label),
            labelStyle = MetroTextStyle.DialogBody,
            optionStyle = MetroTextStyle.DialogBody,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))
        MetroBorderButton(
            text = stringResource(R.string.configure_statusbar),
            onClick = onConfigureClick,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 15.sp,
        )
        MetroText(
            text = stringResource(R.string.configure_statusbar_subtitle),
            style = MetroTextStyle.DialogBody,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp),
        )

        Spacer(modifier = Modifier.height(28.dp))
        MetroText(
            text = stringResource(R.string.preview_section),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
        )
        val previewPadding = TraySpec.horizontalPaddingDp(notchPosition)
        TrayPreview(
            flags = state.iconFlags,
            clockText = state.clockText,
            layout = state.layout,
            leftPaddingDp = previewPadding.left,
            rightPaddingDp = previewPadding.right,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )

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
            style = MetroTextStyle.DialogBody,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 16.dp),
        )
        MetroBorderButton(
            text = stringResource(R.string.grant_overlay),
            enabled = !overlayGranted,
            onClick = onGrantOverlay,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 15.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetroBorderButton(
            text = stringResource(R.string.grant_accessibility),
            enabled = !accessibilityEnabled,
            onClick = onGrantAccessibility,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 15.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetroBorderButton(
            text = stringResource(R.string.grant_phone_state),
            enabled = !phoneStateGranted,
            onClick = onGrantPhoneState,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 15.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetroBorderButton(
            text = stringResource(R.string.grant_notification_access),
            enabled = !notificationAccessEnabled,
            onClick = onGrantNotificationAccess,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 15.sp,
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            Spacer(modifier = Modifier.height(12.dp))
            MetroBorderButton(
                text = stringResource(R.string.grant_bluetooth_connect),
                enabled = !bluetoothConnectGranted,
                onClick = onGrantBluetooth,
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 15.sp,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun IconsTab(
    flags: TrayIconFlags,
    layout: List<TrayLayoutSlot>,
    clockText: String,
    onFlagsChange: (TrayIconFlags) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
    ) {
        IconToggle(
            checked = flags.network,
            label = stringResource(R.string.icon_network),
            onCheckedChange = { onFlagsChange(flags.copy(network = it)) },
        )
        IconToggle(
            checked = flags.wifi,
            label = stringResource(R.string.icon_wifi),
            onCheckedChange = { onFlagsChange(flags.copy(wifi = it)) },
        )
        IconToggle(
            checked = flags.mute,
            label = stringResource(R.string.icon_mute),
            onCheckedChange = { onFlagsChange(flags.copy(mute = it)) },
        )
        IconToggle(
            checked = flags.notifications,
            label = stringResource(R.string.icon_notifications),
            onCheckedChange = { onFlagsChange(flags.copy(notifications = it)) },
        )
        IconToggle(
            checked = flags.hotspot,
            label = stringResource(R.string.icon_hotspot),
            onCheckedChange = { onFlagsChange(flags.copy(hotspot = it)) },
        )
        IconToggle(
            checked = flags.bluetoothAudio,
            label = stringResource(R.string.icon_bluetooth_audio),
            onCheckedChange = { onFlagsChange(flags.copy(bluetoothAudio = it)) },
        )
        IconToggle(
            checked = flags.battery,
            label = stringResource(R.string.icon_battery),
            onCheckedChange = { onFlagsChange(flags.copy(battery = it)) },
        )

        Spacer(modifier = Modifier.height(28.dp))
        MetroText(
            text = stringResource(R.string.icons_preview_section),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
        )
        TrayIconsPreview(
            flags = flags,
            layout = layout,
            clockText = clockText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun IconToggle(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    MetroToggleSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = label,
        labelStyle = MetroTextStyle.DialogBody,
        statusStyle = MetroTextStyle.Body,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp),
    )
}
