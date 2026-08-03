package com.metro.settings.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.metro.ui.MetroPivot
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** WP8.1 system settings rows stay at single-line height even with a value subtitle. */
private val SettingsRowHeight = 76.dp

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
        header = {
            MetroAppTitle(title = stringResource(R.string.settings_app_title))
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
            SettingsState.PIVOT_SYSTEM -> SystemSettingsList(state = state)
            else -> ApplicationsSettingsList(
                entries = state.applicationEntries,
                onOpen = state::openApplicationSettings,
            )
        }
    }
}

@Composable
private fun SystemSettingsList(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    val storage = state.system.storageSnapshot()
    val storageSubtitle = storage?.let {
        SettingsLogic.formatBytes(it.freeBytes) + " free"
    } ?: stringResource(R.string.settings_status_unavailable)
    val brightnessPct = (state.brightness * 100).toInt()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            MetroListItem(
                title = stringResource(R.string.settings_start_theme),
                subtitle = state.accentDisplayName,
                modifier = Modifier.height(SettingsRowHeight),
                onClick = { state.open(SettingsRoute.StartTheme) },
            )
        }
        item {
            MetroListItem(
                title = stringResource(R.string.settings_storage_sense),
                subtitle = storageSubtitle,
                modifier = Modifier.height(SettingsRowHeight),
                onClick = { state.open(SettingsRoute.StorageSense) },
            )
        }
        item {
            MetroListItem(
                title = stringResource(R.string.settings_brightness),
                subtitle = "$brightnessPct%",
                modifier = Modifier.height(SettingsRowHeight),
                onClick = { state.open(SettingsRoute.Brightness) },
            )
        }
        item {
            MetroListItem(
                title = stringResource(R.string.settings_ease_of_access),
                subtitle = "text ${state.fontScaleIndex + 1}/${MetroFontScale.STEP_COUNT}",
                modifier = Modifier.height(SettingsRowHeight),
                onClick = { state.open(SettingsRoute.EaseOfAccess) },
            )
        }
        item {
            MetroListItem(
                title = stringResource(R.string.settings_navigation_bar),
                subtitle = stringResource(R.string.settings_navigation_bar_subtitle),
                modifier = Modifier.height(SettingsRowHeight),
                onClick = { state.openNavbarSettings() },
            )
        }
        item {
            MetroListItem(
                title = stringResource(R.string.settings_status_bar),
                subtitle = stringResource(R.string.settings_status_bar_subtitle),
                modifier = Modifier.height(SettingsRowHeight),
                onClick = { state.openStatusbarSettings() },
            )
        }
        item {
            MetroListItem(
                title = stringResource(R.string.settings_volume),
                subtitle = stringResource(R.string.settings_volume_subtitle),
                modifier = Modifier.height(SettingsRowHeight),
                onClick = { state.openVolumeSettings() },
            )
        }
        item {
            MetroListItem(
                title = stringResource(R.string.settings_keyboard),
                subtitle = stringResource(R.string.settings_keyboard_subtitle),
                modifier = Modifier.height(SettingsRowHeight),
                onClick = { state.openKeyboardSettings() },
            )
        }
        item {
            MetroListItem(
                title = stringResource(R.string.settings_about),
                subtitle = stringResource(R.string.settings_about_suite),
                modifier = Modifier.height(SettingsRowHeight),
                onClick = { state.open(SettingsRoute.About) },
            )
        }
    }
}

@Composable
private fun ApplicationsSettingsList(
    entries: List<InstalledAppEntry>,
    onOpen: (InstalledAppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val userApps = entries.filterNot { it.isSystemApp }
    val systemApps = entries.filter { it.isSystemApp }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (entries.isEmpty()) {
            item {
                MetroText(
                    text = stringResource(R.string.settings_applications_empty),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(
                        horizontal = MetroDimens.ScreenHorizontalMargin,
                        vertical = 12.dp,
                    ),
                )
            }
        } else {
            if (userApps.isNotEmpty()) {
                item {
                    ApplicationsSectionHeader(
                        text = stringResource(R.string.settings_applications_section_apps),
                    )
                }
                items(userApps, key = { "user:${it.packageName}" }) { entry ->
                    MetroListItem(
                        title = entry.title,
                        subtitle = entry.listSubtitle,
                        modifier = Modifier.height(SettingsRowHeight),
                        onClick = { onOpen(entry) },
                    )
                }
            }
            if (systemApps.isNotEmpty()) {
                item {
                    ApplicationsSectionHeader(
                        text = stringResource(R.string.settings_applications_section_system),
                    )
                }
                items(systemApps, key = { "system:${it.packageName}" }) { entry ->
                    MetroListItem(
                        title = entry.title,
                        subtitle = entry.listSubtitle,
                        modifier = Modifier.height(SettingsRowHeight),
                        onClick = { onOpen(entry) },
                    )
                }
            }
        }
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
