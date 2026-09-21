package com.metro.settings.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.settings.R
import com.metro.settings.data.InstalledAppEntry
import com.metro.settings.data.SettingsLogic
import com.metro.system.MetroFontScale
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroListPivotController
import com.metro.ui.MetroPivot
import com.metro.ui.MetroStaggeredPivotEnter
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.ui.rememberMetroListPivotController
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** WP8.1 system settings rows stay at single-line height even with a value subtitle. */
private val SettingsRowHeight = 76.dp

/** Stagger index 0 is pivot chrome; list rows start at 1. */
private const val ListStaggerStart = 1

private data class SystemSettingsRow(
    val key: String,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsRootScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = state.rootPivot,
        pageCount = { 2 },
    )
    val scope = rememberCoroutineScope()
    val pivotTitles = listOf(
        stringResource(R.string.settings_system_title),
        stringResource(R.string.settings_applications_title),
    )
    val systemRowCount = 12
    val appSlotCount = remember(state.applicationEntries) {
        val user = state.applicationEntries.count { !it.isSystemApp }
        val system = state.applicationEntries.count { it.isSystemApp }
        when {
            state.applicationEntries.isEmpty() -> 1
            else -> {
                (if (user > 0) 1 + user else 0) + (if (system > 0) 1 + system else 0)
            }
        }
    }
    val listPivot = rememberMetroListPivotController(
        lastStaggerIndex = maxOf(systemRowCount, appSlotCount),
    )

    LaunchedEffect(state.rootPivot) {
        if (pagerState.currentPage != state.rootPivot && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(state.rootPivot)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .filter { (_, scrolling) -> !scrolling }
            .distinctUntilChanged()
            .collect { (page, _) -> state.selectRootPivot(page) }
    }

    MetroPivot(
        titles = pivotTitles,
        pagerState = pagerState,
        modifier = modifier.fillMaxSize(),
        chromeStaggerLoadKey = listPivot.loadKey,
        chromeStaggerExiting = listPivot.exiting,
        chromeStaggerSkipEnter = listPivot.skipEnterFor(staggerIndex = 0),
        userScrollEnabled = !listPivot.exiting,
        header = {
            MetroAppTitle(title = stringResource(R.string.settings_app_title))
        },
        onTitleClick = { index ->
            if (listPivot.exiting) return@MetroPivot
            scope.launch {
                pagerState.animateScrollToPage(
                    page = index,
                    animationSpec = MetroTransitions.pivotTween(),
                )
            }
        },
    ) { page ->
        when (page) {
            SettingsState.PIVOT_SYSTEM -> SystemSettingsList(
                state = state,
                listPivot = listPivot,
            )
            else -> ApplicationsSettingsList(
                entries = state.applicationEntries,
                listPivot = listPivot,
                onOpen = state::openApplicationSettings,
            )
        }
    }
}

@Composable
private fun SystemSettingsList(
    state: SettingsState,
    listPivot: MetroListPivotController,
    modifier: Modifier = Modifier,
) {
    val storage = state.system.storageSnapshot()
    val storageSubtitle = storage?.let {
        SettingsLogic.formatBytes(it.freeBytes) + " free"
    } ?: stringResource(R.string.settings_status_unavailable)
    val brightnessPct = (state.brightness * 100).toInt()

    val rows = listOf(
        SystemSettingsRow(
            key = "start_theme",
            title = stringResource(R.string.settings_start_theme),
            subtitle = state.accentDisplayName,
            onClick = { state.open(SettingsRoute.StartTheme) },
        ),
        SystemSettingsRow(
            key = "storage_sense",
            title = stringResource(R.string.settings_storage_sense),
            subtitle = storageSubtitle,
            onClick = { state.open(SettingsRoute.StorageSense) },
        ),
        SystemSettingsRow(
            key = "brightness",
            title = stringResource(R.string.settings_brightness),
            subtitle = "$brightnessPct%",
            onClick = { state.open(SettingsRoute.Brightness) },
        ),
        SystemSettingsRow(
            key = "ease_of_access",
            title = stringResource(R.string.settings_ease_of_access),
            subtitle = "text ${state.fontScaleIndex + 1}/${MetroFontScale.STEP_COUNT}",
            onClick = { state.open(SettingsRoute.EaseOfAccess) },
        ),
        SystemSettingsRow(
            key = "connected_apps",
            title = stringResource(R.string.settings_connected_apps),
            subtitle = stringResource(R.string.settings_connected_apps_subtitle),
            onClick = { state.open(SettingsRoute.ConnectedApps) },
        ),
        SystemSettingsRow(
            key = "navigation_bar",
            title = stringResource(R.string.settings_navigation_bar),
            subtitle = stringResource(R.string.settings_navigation_bar_subtitle),
            onClick = { state.openNavbarSettings() },
        ),
        SystemSettingsRow(
            key = "status_bar",
            title = stringResource(R.string.settings_status_bar),
            subtitle = stringResource(R.string.settings_status_bar_subtitle),
            onClick = { state.openStatusbarSettings() },
        ),
        SystemSettingsRow(
            key = "notifications",
            title = stringResource(R.string.settings_notifications),
            subtitle = stringResource(R.string.settings_notifications_subtitle),
            onClick = { state.openNotificationsSettings() },
        ),
        SystemSettingsRow(
            key = "volume",
            title = stringResource(R.string.settings_volume),
            subtitle = stringResource(R.string.settings_volume_subtitle),
            onClick = { state.openVolumeSettings() },
        ),
        SystemSettingsRow(
            key = "lock_screen",
            title = stringResource(R.string.settings_lock_screen),
            subtitle = stringResource(R.string.settings_lock_screen_subtitle),
            onClick = { state.openLockscreenSettings() },
        ),
        SystemSettingsRow(
            key = "keyboard",
            title = stringResource(R.string.settings_keyboard),
            subtitle = stringResource(R.string.settings_keyboard_subtitle),
            onClick = { state.openKeyboardSettings() },
        ),
        SystemSettingsRow(
            key = "about",
            title = stringResource(R.string.settings_about),
            subtitle = stringResource(R.string.settings_about_suite),
            onClick = { state.open(SettingsRoute.About) },
        ),
    )

    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
            StaggeredSettingsRow(
                staggerIndex = ListStaggerStart + index,
                listPivot = listPivot,
            ) {
                MetroListItem(
                    title = row.title,
                    subtitle = row.subtitle,
                    modifier = Modifier.height(SettingsRowHeight),
                    onClick = {
                        if (!listPivot.exiting) listPivot.requestExit(row.onClick)
                    },
                )
            }
        }
    }
}

@Composable
private fun ApplicationsSettingsList(
    entries: List<InstalledAppEntry>,
    listPivot: MetroListPivotController,
    onOpen: (InstalledAppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val userApps = entries.filterNot { it.isSystemApp }
    val systemApps = entries.filter { it.isSystemApp }

    val slotKeys = remember(userApps, systemApps, entries.isEmpty()) {
        buildList {
            if (entries.isEmpty()) {
                add("empty")
            } else {
                if (userApps.isNotEmpty()) {
                    add("header:apps")
                    userApps.forEach { add("user:${it.packageName}") }
                }
                if (systemApps.isNotEmpty()) {
                    add("header:system")
                    systemApps.forEach { add("system:${it.packageName}") }
                }
            }
        }
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(slotKeys, key = { _, key -> key }) { index, key ->
            StaggeredSettingsRow(
                staggerIndex = ListStaggerStart + index,
                listPivot = listPivot,
            ) {
                when {
                    key == "empty" -> MetroText(
                        text = stringResource(R.string.settings_applications_empty),
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.padding(
                            horizontal = MetroDimens.ScreenHorizontalMargin,
                            vertical = 12.dp,
                        ),
                    )
                    key == "header:apps" -> ApplicationsSectionHeader(
                        text = stringResource(R.string.settings_applications_section_apps),
                    )
                    key == "header:system" -> ApplicationsSectionHeader(
                        text = stringResource(R.string.settings_applications_section_system),
                    )
                    key.startsWith("user:") -> {
                        val pkg = key.removePrefix("user:")
                        val entry = userApps.first { it.packageName == pkg }
                        MetroListItem(
                            title = entry.title,
                            subtitle = entry.listSubtitle,
                            modifier = Modifier.height(SettingsRowHeight),
                            onClick = {
                                if (!listPivot.exiting) {
                                    listPivot.requestExit { onOpen(entry) }
                                }
                            },
                        )
                    }
                    else -> {
                        val pkg = key.removePrefix("system:")
                        val entry = systemApps.first { it.packageName == pkg }
                        MetroListItem(
                            title = entry.title,
                            subtitle = entry.listSubtitle,
                            modifier = Modifier.height(SettingsRowHeight),
                            onClick = {
                                if (!listPivot.exiting) {
                                    listPivot.requestExit { onOpen(entry) }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaggeredSettingsRow(
    staggerIndex: Int,
    listPivot: MetroListPivotController,
    content: @Composable () -> Unit,
) {
    MetroStaggeredPivotEnter(
        staggerIndex = staggerIndex,
        loadKey = listPivot.loadKey,
        exiting = listPivot.exiting,
        skipEnter = listPivot.skipEnterFor(staggerIndex),
        enterDelayMs = listPivot.enterDelayMsFor(staggerIndex),
    ) {
        content()
    }
}

@Composable
private fun ApplicationsSectionHeader(text: String) {
    MetroText(
        text = text,
        style = MetroTextStyle.SectionHeader,
        color = MetroTheme.colors.accent,
        modifier = Modifier.padding(
            start = MetroDimens.ScreenHorizontalMargin,
            end = MetroDimens.ScreenHorizontalMargin,
            top = 16.dp,
            bottom = 4.dp,
        ),
    )
}
