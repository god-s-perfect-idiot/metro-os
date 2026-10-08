package com.metro.metron.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.category.visualName
import eu.kanade.tachiyomi.ui.library.LibrarySettingsViewModel
import eu.kanade.tachiyomi.ui.updates.UpdatesSettingsViewModel
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.sort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.updates.service.UpdatesPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroListItem
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch

/** Options panels shown in [MetronOptionsCard] over the bottom app bar. */
enum class MetronLibraryOptionsPanel {
    Filter,
    Sort,
    Display,
}

enum class MetronUpdatesOptionsPanel {
    Filter,
    Categories,
}

@Composable
fun ColumnScope.MetronLibraryOptionsBody(
    panel: MetronLibraryOptionsPanel,
    viewModel: LibrarySettingsViewModel,
    category: Category?,
) {
    when (panel) {
        MetronLibraryOptionsPanel.Filter -> LibraryFilterOptions(viewModel)
        MetronLibraryOptionsPanel.Sort -> LibrarySortOptions(category, viewModel)
        MetronLibraryOptionsPanel.Display -> LibraryDisplayOptions(viewModel)
    }
}

@Composable
fun ColumnScope.MetronUpdatesOptionsBody(
    panel: MetronUpdatesOptionsPanel,
    viewModel: UpdatesSettingsViewModel,
) {
    when (panel) {
        MetronUpdatesOptionsPanel.Filter -> UpdatesFilterOptions(viewModel)
        MetronUpdatesOptionsPanel.Categories -> UpdatesCategoryOptions(viewModel)
    }
}

@Composable
private fun ColumnScope.LibraryFilterOptions(viewModel: LibrarySettingsViewModel) {
    val filterDownloaded by viewModel.libraryPreferences.filterDownloaded.collectAsState()
    val downloadedOnly by viewModel.preferences.downloadedOnly.collectAsState()
    val autoUpdateMangaRestrictions by viewModel.libraryPreferences.autoUpdateMangaRestrictions.collectAsState()

    TriStateOptionRow(
        label = stringResource(MR.strings.label_downloaded),
        state = if (downloadedOnly) TriState.ENABLED_IS else filterDownloaded,
        enabled = !downloadedOnly,
        onClick = { viewModel.toggleFilter(LibraryPreferences::filterDownloaded) },
    )
    val filterUnread by viewModel.libraryPreferences.filterUnread.collectAsState()
    TriStateOptionRow(
        label = stringResource(MR.strings.action_filter_unread),
        state = filterUnread,
        onClick = { viewModel.toggleFilter(LibraryPreferences::filterUnread) },
    )
    val filterStarted by viewModel.libraryPreferences.filterStarted.collectAsState()
    TriStateOptionRow(
        label = stringResource(MR.strings.label_started),
        state = filterStarted,
        onClick = { viewModel.toggleFilter(LibraryPreferences::filterStarted) },
    )
    val filterBookmarked by viewModel.libraryPreferences.filterBookmarked.collectAsState()
    TriStateOptionRow(
        label = stringResource(MR.strings.action_filter_bookmarked),
        state = filterBookmarked,
        onClick = { viewModel.toggleFilter(LibraryPreferences::filterBookmarked) },
    )
    val filterCompleted by viewModel.libraryPreferences.filterCompleted.collectAsState()
    TriStateOptionRow(
        label = stringResource(MR.strings.completed),
        state = filterCompleted,
        onClick = { viewModel.toggleFilter(LibraryPreferences::filterCompleted) },
    )
    if ((!isReleaseBuildType) &&
        LibraryPreferences.MANGA_OUTSIDE_RELEASE_PERIOD in autoUpdateMangaRestrictions
    ) {
        val filterIntervalCustom by viewModel.libraryPreferences.filterIntervalCustom.collectAsState()
        TriStateOptionRow(
            label = stringResource(MR.strings.action_filter_interval_custom),
            state = filterIntervalCustom,
            onClick = { viewModel.toggleFilter(LibraryPreferences::filterIntervalCustom) },
        )
    }

    val trackers by viewModel.trackersFlow.collectAsState()
    when (trackers.size) {
        0 -> Unit
        1 -> {
            val service = trackers[0]
            val filterTracker by viewModel.libraryPreferences.filterTracking(service.id.toInt())
                .collectAsState()
            TriStateOptionRow(
                label = stringResource(MR.strings.action_filter_tracked),
                state = filterTracker,
                onClick = { viewModel.toggleTracker(service.id.toInt()) },
            )
        }
        else -> {
            MetroText(
                text = stringResource(MR.strings.action_filter_tracked).lowercase(),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
            trackers.forEach { service ->
                val filterTracker by viewModel.libraryPreferences.filterTracking(service.id.toInt())
                    .collectAsState()
                TriStateOptionRow(
                    label = service.name,
                    state = filterTracker,
                    onClick = { viewModel.toggleTracker(service.id.toInt()) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.LibrarySortOptions(
    category: Category?,
    viewModel: LibrarySettingsViewModel,
) {
    val trackers by viewModel.trackersFlow.collectAsState()
    val sortingMode = category.sort.type
    val sortDescending = !category.sort.isAscending

    val options = remember(trackers.isEmpty()) {
        val trackerMeanPair = if (trackers.isNotEmpty()) {
            MR.strings.action_sort_tracker_score to LibrarySort.Type.TrackerMean
        } else {
            null
        }
        listOfNotNull(
            MR.strings.action_sort_alpha to LibrarySort.Type.Alphabetical,
            MR.strings.action_sort_total to LibrarySort.Type.TotalChapters,
            MR.strings.action_sort_last_read to LibrarySort.Type.LastRead,
            MR.strings.action_sort_last_manga_update to LibrarySort.Type.LastUpdate,
            MR.strings.action_sort_unread_count to LibrarySort.Type.UnreadCount,
            MR.strings.action_sort_latest_chapter to LibrarySort.Type.LatestChapter,
            MR.strings.action_sort_chapter_fetch_date to LibrarySort.Type.ChapterFetchDate,
            MR.strings.action_sort_date_added to LibrarySort.Type.DateAdded,
            trackerMeanPair,
            MR.strings.action_sort_random to LibrarySort.Type.Random,
        )
    }

    options.forEach { (titleRes, mode) ->
        val selected = sortingMode == mode
        val directionIcon = when {
            !selected -> null
            mode == LibrarySort.Type.Random -> MetroSystemIconType.Refresh
            sortDescending -> MetroSystemIconType.ChevronDown
            else -> MetroSystemIconType.ChevronUp
        }
        MetroListItem(
            title = stringResource(titleRes).lowercase(),
            singleLine = true,
            titleColor = if (selected) MetroTheme.colors.accent else null,
            trailing = directionIcon?.let { type ->
                {
                    MetroSystemIcon(
                        type = type,
                        iconSize = 20.dp,
                        color = MetroTheme.colors.accent,
                        showCircle = false,
                    )
                }
            },
            oneLineMinHeight = 48.dp,
            verticalPadding = 8.dp,
            onClick = {
                if (mode == LibrarySort.Type.Random) {
                    viewModel.setSort(category, mode, LibrarySort.Direction.Ascending)
                    return@MetroListItem
                }
                val isTogglingDirection = sortingMode == mode
                val direction = when {
                    isTogglingDirection -> if (sortDescending) {
                        LibrarySort.Direction.Ascending
                    } else {
                        LibrarySort.Direction.Descending
                    }
                    else -> if (sortDescending) {
                        LibrarySort.Direction.Descending
                    } else {
                        LibrarySort.Direction.Ascending
                    }
                }
                viewModel.setSort(category, mode, direction)
            },
        )
    }
}

@Composable
private fun ColumnScope.LibraryDisplayOptions(viewModel: LibrarySettingsViewModel) {
    // Metro library uses a fixed cover tile grid — no compact/comfortable/list modes or columns.
    BadgeToggle(
        label = stringResource(MR.strings.action_display_download_badge),
        prefChecked = viewModel.libraryPreferences.downloadBadge,
    )
    BadgeToggle(
        label = stringResource(MR.strings.action_display_unread_badge),
        prefChecked = viewModel.libraryPreferences.unreadBadge,
    )
    BadgeToggle(
        label = stringResource(MR.strings.action_display_local_badge),
        prefChecked = viewModel.libraryPreferences.localBadge,
    )
    BadgeToggle(
        label = stringResource(MR.strings.action_display_language_badge),
        prefChecked = viewModel.libraryPreferences.languageBadge,
    )
}

@Composable
private fun BadgeToggle(
    label: String,
    prefChecked: tachiyomi.core.common.preference.Preference<Boolean>,
) {
    val checked by prefChecked.collectAsState()
    MetroToggleSwitch(
        checked = checked,
        onCheckedChange = { prefChecked.set(it) },
        label = label.lowercase(),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun ColumnScope.UpdatesFilterOptions(viewModel: UpdatesSettingsViewModel) {
    val filterDownloaded by viewModel.updatesPreferences.filterDownloaded.collectAsState()
    TriStateOptionRow(
        label = stringResource(MR.strings.label_downloaded),
        state = filterDownloaded,
        onClick = { viewModel.toggleFilter(UpdatesPreferences::filterDownloaded) },
    )
    val filterUnread by viewModel.updatesPreferences.filterUnread.collectAsState()
    TriStateOptionRow(
        label = stringResource(MR.strings.action_filter_unread),
        state = filterUnread,
        onClick = { viewModel.toggleFilter(UpdatesPreferences::filterUnread) },
    )
    val filterStarted by viewModel.updatesPreferences.filterStarted.collectAsState()
    TriStateOptionRow(
        label = stringResource(MR.strings.label_started),
        state = filterStarted,
        onClick = { viewModel.toggleFilter(UpdatesPreferences::filterStarted) },
    )
    val filterBookmarked by viewModel.updatesPreferences.filterBookmarked.collectAsState()
    TriStateOptionRow(
        label = stringResource(MR.strings.action_filter_bookmarked),
        state = filterBookmarked,
        onClick = { viewModel.toggleFilter(UpdatesPreferences::filterBookmarked) },
    )

    val filterExcludedScanlators by viewModel.updatesPreferences.filterExcludedScanlators.collectAsState()
    MetroToggleSwitch(
        checked = filterExcludedScanlators,
        onCheckedChange = {
            viewModel.updatesPreferences.filterExcludedScanlators.getAndSet { !it }
        },
        label = stringResource(MR.strings.action_filter_excluded_scanlators).lowercase(),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun ColumnScope.UpdatesCategoryOptions(viewModel: UpdatesSettingsViewModel) {
    MetroText(
        text = stringResource(MR.strings.pref_filter_update_categories_details).lowercase(),
        style = MetroTextStyle.Body,
        color = MetroTheme.colors.secondaryText,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )

    val allCategories by viewModel.getCategories.subscribe().collectAsState(initial = emptyList())
    if (allCategories.isEmpty()) {
        MetroLoadingDots(modifier = Modifier.padding(16.dp))
        return
    }

    val excluded by viewModel.updatesPreferences.filterExcludedCategories.collectAsState()
    val included by viewModel.updatesPreferences.filterIncludedCategories.collectAsState()
    val selected = remember(allCategories, included, excluded) {
        allCategories.map { category ->
            when (category.id) {
                in included -> TriState.ENABLED_IS
                in excluded -> TriState.ENABLED_NOT
                else -> TriState.DISABLED
            }
        }.toMutableStateList()
    }

    allCategories.forEachIndexed { idx, category ->
        val state = selected.getOrElse(idx) { TriState.DISABLED }
        TriStateOptionRow(
            label = category.visualName,
            state = state,
            onClick = {
                selected[idx] = state.next()
                viewModel.cycleCategory(category)
            },
        )
    }
}

@Composable
private fun TriStateOptionRow(
    label: String,
    state: TriState,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val status = when (state) {
        TriState.DISABLED -> "any"
        TriState.ENABLED_IS -> "include"
        TriState.ENABLED_NOT -> "exclude"
    }
    MetroListItem(
        title = label.lowercase(),
        subtitle = status,
        singleLine = true,
        enabled = enabled,
        leading = {
            when (state) {
                TriState.DISABLED -> MetroCheckBox(checked = false, onCheckedChange = null, enabled = enabled)
                TriState.ENABLED_IS -> MetroCheckBox(checked = true, onCheckedChange = null, enabled = enabled)
                TriState.ENABLED_NOT -> MetroText(
                    text = "✕",
                    style = MetroTextStyle.ListItemTitle,
                    color = if (enabled) {
                        MetroTheme.colors.accent
                    } else {
                        MetroTheme.colors.secondaryText
                    },
                )
            }
        },
        oneLineMinHeight = 48.dp,
        twoLineMinHeight = 56.dp,
        verticalPadding = 6.dp,
        onClick = if (enabled) onClick else null,
    )
}
