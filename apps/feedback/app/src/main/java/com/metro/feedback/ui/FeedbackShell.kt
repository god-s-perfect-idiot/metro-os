package com.metro.feedback.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.feedback.R
import com.metro.feedback.data.AppMetric
import com.metro.feedback.data.GitHubIssueLauncher
import com.metro.feedback.data.MetricsRepository
import com.metro.feedback.data.MetricsSnapshot
import com.metro.ui.MetroAppGlyphs
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListItem
import com.metro.ui.MetroPivot
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedbackShell(
    state: FeedbackState,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = state.pivot.index,
        pageCount = { 2 },
    )
    val scope = rememberCoroutineScope()
    val titles = listOf(
        stringResource(R.string.pivot_feedback),
        stringResource(R.string.pivot_metrics),
    )

    LaunchedEffect(state.pivot) {
        if (pagerState.currentPage != state.pivot.index && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(state.pivot.index)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .filter { (_, scrolling) -> !scrolling }
            .distinctUntilChanged()
            .collect { (page, _) -> state.setPivot(page) }
    }

    MetroPivot(
        titles = titles,
        pagerState = pagerState,
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .metroNavBarPadding()
            .testTag("metro_page_hub"),
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
        when (FeedbackPivot.fromIndex(page)) {
            FeedbackPivot.Feedback -> FeedbackPane(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("metro_page_feedback"),
            )
            FeedbackPivot.Metrics -> MetricsPane(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("metro_page_metrics"),
            )
        }
    }
}

@Composable
private fun FeedbackPane(
    state: FeedbackState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MetroText(
            text = stringResource(R.string.feedback_help),
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        MetroTextBox(
            value = state.title,
            onValueChange = state::updateTitle,
            placeholder = stringResource(R.string.feedback_title_placeholder),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        MetroBorderButton(
            text = stringResource(R.string.feedback_collect_logcat),
            onClick = state::collectLogcat,
            enabled = state.logcatStatus !is LogcatStatus.Collecting,
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        MetroText(
            text = logcatStatusText(state.logcatStatus),
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        MetroBorderButton(
            text = stringResource(R.string.feedback_submit),
            onClick = state::submit,
            enabled = state.title.isNotBlank() && state.submitStatus !is SubmitStatus.Opening,
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        val submitText = submitStatusText(state.submitStatus)
        if (submitText != null) {
            MetroText(
                text = submitText,
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
            )
        }
    }
}

@Composable
private fun logcatStatusText(status: LogcatStatus): String = when (status) {
    LogcatStatus.Idle -> stringResource(R.string.feedback_logcat_idle)
    LogcatStatus.Collecting -> stringResource(R.string.feedback_logcat_collecting)
    is LogcatStatus.Ready -> stringResource(
        R.string.feedback_logcat_ready,
        status.capture.file.name,
        GitHubIssueLauncher.formatBytes(status.capture.byteCount),
    )
    is LogcatStatus.Empty -> stringResource(R.string.feedback_logcat_empty)
    is LogcatStatus.Error -> stringResource(R.string.feedback_logcat_error, status.message)
}

@Composable
private fun submitStatusText(status: SubmitStatus): String? = when (status) {
    SubmitStatus.Idle -> null
    SubmitStatus.Opening -> stringResource(R.string.feedback_submit_opening)
    SubmitStatus.Opened -> stringResource(R.string.feedback_submit_opened)
    is SubmitStatus.Error -> {
        if (status.message == "title required") {
            stringResource(R.string.feedback_submit_need_title)
        } else {
            stringResource(R.string.feedback_submit_error, status.message)
        }
    }
}

@Composable
private fun MetricsPane(
    state: FeedbackState,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        state.refreshMetrics()
    }

    val snapshot = state.metrics
    LazyColumn(
        modifier = modifier.padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            MetricsHeader(
                snapshot = snapshot,
                loading = state.metricsLoading,
                onGrantUsage = state::openUsageAccess,
            )
        }
        if (snapshot == null) {
            item {
                MetroText(
                    text = stringResource(R.string.metrics_refreshing),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(
                        horizontal = MetroDimens.ScreenHorizontalMargin,
                        vertical = 12.dp,
                    ),
                )
            }
        } else if (snapshot.apps.isEmpty()) {
            item {
                MetroText(
                    text = stringResource(R.string.metrics_empty),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(
                        horizontal = MetroDimens.ScreenHorizontalMargin,
                        vertical = 12.dp,
                    ),
                )
            }
        } else {
            items(snapshot.apps, key = { it.packageName }) { app ->
                AppMetricRow(
                    app = app,
                    totalRam = snapshot.memory.totalBytes,
                )
            }
        }
    }
}

@Composable
private fun MetricsHeader(
    snapshot: MetricsSnapshot?,
    loading: Boolean,
    onGrantUsage: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        MetroText(
            text = stringResource(R.string.metrics_memory),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent,
            modifier = Modifier.padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 8.dp,
            ),
        )

        if (snapshot != null) {
            UsageBar(
                usedFraction = snapshot.memory.usedFraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                    .padding(bottom = 12.dp),
            )
            MetricStatusRow(
                label = stringResource(R.string.metrics_used),
                value = MetricsRepository.formatBytes(snapshot.memory.usedBytes),
            )
            MetricStatusRow(
                label = stringResource(R.string.metrics_free),
                value = MetricsRepository.formatBytes(snapshot.memory.availBytes),
            )
            MetricStatusRow(
                label = stringResource(R.string.metrics_total),
                value = MetricsRepository.formatBytes(snapshot.memory.totalBytes),
            )
        } else if (loading) {
            MetroText(
                text = stringResource(R.string.metrics_refreshing),
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        MetroText(
            text = stringResource(R.string.metrics_cache),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent,
            modifier = Modifier.padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 8.dp,
            ),
        )

        if (snapshot != null) {
            if (snapshot.hasUsageAccess) {
                MetroText(
                    text = stringResource(
                        R.string.metrics_cache_total,
                        MetricsRepository.formatBytes(snapshot.totalCacheBytes),
                    ),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.primaryText,
                    modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                )
            } else {
                MetroText(
                    text = stringResource(R.string.metrics_cache_unavailable),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                )
                Spacer(modifier = Modifier.height(12.dp))
                MetroBorderButton(
                    text = stringResource(R.string.metrics_grant_usage),
                    onClick = onGrantUsage,
                    modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        MetroText(
            text = stringResource(R.string.metrics_apps),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent,
            modifier = Modifier.padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 8.dp,
            ),
        )
    }
}

@Composable
private fun AppMetricRow(
    app: AppMetric,
    totalRam: Long,
) {
    val cacheLabel = app.cacheBytes?.let { MetricsRepository.formatBytes(it) } ?: "—"
    val ramLabel = MetricsRepository.formatBytes(app.ramBytes)
    val pctLabel = stringResource(
        R.string.metrics_ram_pct,
        (app.ramFractionOf(totalRam) * 100f).toInt().coerceIn(0, 100),
    )
    val glyphRes = MetroAppGlyphs.forPackage(app.packageName) ?: MetroAppGlyphs.Feedback
    val subtitle = stringResource(R.string.metrics_app_ram, ramLabel, pctLabel) +
        "\n" +
        stringResource(R.string.metrics_app_cache, cacheLabel)
    MetroListItem(
        title = app.label,
        subtitle = subtitle,
        twoLineMinHeight = 112.dp,
        leading = {
            Box(
                modifier = Modifier
                    .size(AppMetricTileSize)
                    .background(MetroTheme.colors.accent, RectangleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(glyphRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(
                        MetroColors.tileContentColor(MetroTheme.colors.accent),
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

private val AppMetricTileSize = 64.dp

@Composable
private fun MetricStatusRow(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 6.dp),
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
        )
        MetroText(
            text = value,
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.primaryText,
        )
    }
}

@Composable
private fun UsageBar(
    usedFraction: Float,
    modifier: Modifier = Modifier,
) {
    val fraction = usedFraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(8.dp)
            .background(
                color = MetroTheme.colors.secondaryText.copy(alpha = 0.35f),
                shape = RectangleShape,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(MetroTheme.colors.accent, RectangleShape),
        )
    }
}
