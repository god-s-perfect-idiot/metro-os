package eu.kanade.presentation.more.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroListItem
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import eu.kanade.presentation.more.stats.data.StatsData
import eu.kanade.presentation.util.toDurationString
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun StatsScreenContent(
    state: StatsScreenState.Success,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { OverviewSection(state.overview) }
        item { TitlesStats(state.titles) }
        item { ChapterStats(state.chapters) }
        item { TrackerStats(state.trackers) }
    }
}

@Composable
private fun LazyItemScope.OverviewSection(
    data: StatsData.Overview,
) {
    val none = stringResource(MR.strings.none)
    val context = LocalContext.current
    val readDurationString = remember(data.totalReadDuration) {
        data.totalReadDuration
            .toDuration(DurationUnit.MILLISECONDS)
            .toDurationString(context, fallback = none)
    }
    StatsSection(title = stringResource(MR.strings.label_overview_section)) {
        MetroListItem(
            title = data.libraryMangaCount.toString(),
            subtitle = stringResource(MR.strings.in_library),
            singleLine = true,
        )
        MetroListItem(
            title = readDurationString,
            subtitle = stringResource(MR.strings.label_read_duration),
            singleLine = true,
        )
        MetroListItem(
            title = data.completedMangaCount.toString(),
            subtitle = stringResource(MR.strings.label_completed_titles),
            singleLine = true,
        )
    }
}

@Composable
private fun LazyItemScope.TitlesStats(
    data: StatsData.Titles,
) {
    StatsSection(title = stringResource(MR.strings.label_titles_section)) {
        MetroListItem(
            title = data.globalUpdateItemCount.toString(),
            subtitle = stringResource(MR.strings.label_titles_in_global_update),
            singleLine = true,
        )
        MetroListItem(
            title = data.startedMangaCount.toString(),
            subtitle = stringResource(MR.strings.label_started),
            singleLine = true,
        )
        MetroListItem(
            title = data.localMangaCount.toString(),
            subtitle = stringResource(MR.strings.label_local),
            singleLine = true,
        )
    }
}

@Composable
private fun LazyItemScope.ChapterStats(
    data: StatsData.Chapters,
) {
    StatsSection(title = stringResource(MR.strings.chapters)) {
        MetroListItem(
            title = data.totalChapterCount.toString(),
            subtitle = stringResource(MR.strings.label_total_chapters),
            singleLine = true,
        )
        MetroListItem(
            title = data.readChapterCount.toString(),
            subtitle = stringResource(MR.strings.label_read_chapters),
            singleLine = true,
        )
        MetroListItem(
            title = data.downloadCount.toString(),
            subtitle = stringResource(MR.strings.label_downloaded),
            singleLine = true,
        )
    }
}

@Composable
private fun LazyItemScope.TrackerStats(
    data: StatsData.Trackers,
) {
    val notApplicable = stringResource(MR.strings.not_applicable)
    val meanScoreStr = remember(data.trackedTitleCount, data.meanScore) {
        if (data.trackedTitleCount > 0 && !data.meanScore.isNaN()) {
            "%.2f ★".format(Locale.ENGLISH, data.meanScore)
        } else {
            notApplicable
        }
    }
    StatsSection(title = stringResource(MR.strings.label_tracker_section)) {
        MetroListItem(
            title = data.trackedTitleCount.toString(),
            subtitle = stringResource(MR.strings.label_tracked_titles),
            singleLine = true,
        )
        MetroListItem(
            title = meanScoreStr,
            subtitle = stringResource(MR.strings.label_mean_score),
            singleLine = true,
        )
        MetroListItem(
            title = data.trackerCount.toString(),
            subtitle = stringResource(MR.strings.label_used),
            singleLine = true,
        )
    }
}

@Composable
private fun StatsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MetroText(
            text = title.lowercase(),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        )
        content()
    }
}
