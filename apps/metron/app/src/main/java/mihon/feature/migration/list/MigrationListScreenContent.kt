package mihon.feature.migration.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metro.metron.ui.MetronOptionsCard
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.presentation.util.rememberResourceBitmapPainter
import eu.kanade.tachiyomi.R
import mihon.feature.migration.list.models.MigratingManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private const val CoverAspect = 3f / 4f

@Composable
fun MigrationListScreenContent(
    items: List<MigratingManga>,
    migrationComplete: Boolean,
    finishedCount: Int,
    onItemClick: (Manga) -> Unit,
    onSearchManually: (MigratingManga) -> Unit,
    onSkip: (Long) -> Unit,
    onMigrate: (Long) -> Unit,
    onCopy: (Long) -> Unit,
    openMigrationDialog: (Boolean) -> Unit,
) {
    val title = if (items.isNotEmpty()) {
        stringResource(MR.strings.migrationListScreenTitleWithProgress, finishedCount, items.size)
    } else {
        stringResource(MR.strings.migrationListScreenTitle)
    }
    var actionItemId by rememberSaveable { mutableStateOf<Long?>(null) }
    var actionCanMigrate by rememberSaveable { mutableStateOf(false) }
    val actionItem = items.find { it.manga.id == actionItemId }

    MetroSystemTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .metroNavBarPadding()
                .background(MetroTheme.colors.background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = MetroAppBarDefaults.BarHeight),
            ) {
                MetroSettingsHeader(
                    pageTitle = title.lowercase(),
                    appTitle = "metron",
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    items(items, key = { it.manga.id }) { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MigrationListItem(
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.Top)
                                    .fillMaxHeight(),
                                manga = item.manga,
                                source = item.source,
                                chapterCount = item.chapterCount,
                                latestChapter = item.latestChapter,
                                onClick = { onItemClick(item.manga) },
                            )

                            MetroSystemIcon(
                                type = MetroSystemIconType.Forward,
                                iconSize = 28.dp,
                                color = MetroTheme.colors.primaryText,
                                showCircle = false,
                            )

                            val result by item.searchResult.collectAsState()
                            MigrationListItemResult(
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.Top)
                                    .fillMaxHeight(),
                                result = result,
                                onItemClick = onItemClick,
                            )

                            when (result) {
                                MigratingManga.SearchResult.Searching -> {
                                    MetroCircleIconButton(
                                        type = MetroSystemIconType.Close,
                                        contentDescription = stringResource(
                                            MR.strings.migrationListScreen_skipActionLabel,
                                        ).lowercase(),
                                        onClick = { onSkip(item.manga.id) },
                                        glyphSize = 28.dp,
                                    )
                                }
                                MigratingManga.SearchResult.NotFound,
                                is MigratingManga.SearchResult.Success,
                                -> {
                                    MetroCircleIconButton(
                                        type = MetroSystemIconType.More,
                                        contentDescription = "more",
                                        onClick = {
                                            actionItemId = item.manga.id
                                            actionCanMigrate =
                                                result is MigratingManga.SearchResult.Success
                                        },
                                        glyphSize = 28.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            MetroAppBar(
                icons = listOf(
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Copy,
                        label = stringResource(MR.strings.migrationListScreen_copyActionLabel)
                            .lowercase(),
                        onClick = { openMigrationDialog(true) },
                        enabled = migrationComplete,
                    ),
                    MetroAppBarIcon(
                        type = MetroSystemIconType.Check,
                        label = stringResource(MR.strings.migrationListScreen_migrateActionLabel)
                            .lowercase(),
                        onClick = { openMigrationDialog(false) },
                        enabled = migrationComplete,
                    ),
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            MetronOptionsCard(
                visible = actionItem != null,
                onDismiss = {
                    actionItemId = null
                    actionCanMigrate = false
                },
            ) {
                OptionsRow(
                    text = stringResource(MR.strings.migrationListScreen_searchManuallyActionLabel)
                        .lowercase(),
                    onClick = {
                        val item = actionItem ?: return@OptionsRow
                        actionItemId = null
                        actionCanMigrate = false
                        onSearchManually(item)
                    },
                )
                OptionsRow(
                    text = stringResource(MR.strings.migrationListScreen_skipActionLabel)
                        .lowercase(),
                    onClick = {
                        val id = actionItemId ?: return@OptionsRow
                        actionItemId = null
                        actionCanMigrate = false
                        onSkip(id)
                    },
                )
                if (actionCanMigrate) {
                    OptionsRow(
                        text = stringResource(MR.strings.migrationListScreen_migrateNowActionLabel)
                            .lowercase(),
                        onClick = {
                            val id = actionItemId ?: return@OptionsRow
                            actionItemId = null
                            actionCanMigrate = false
                            onMigrate(id)
                        },
                    )
                    OptionsRow(
                        text = stringResource(MR.strings.migrationListScreen_copyNowActionLabel)
                            .lowercase(),
                        onClick = {
                            val id = actionItemId ?: return@OptionsRow
                            actionItemId = null
                            actionCanMigrate = false
                            onCopy(id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionsRow(
    text: String,
    onClick: () -> Unit,
) {
    MetroText(
        text = text,
        style = MetroTextStyle.ListItemTitle,
        color = MetroTheme.colors.primaryText,
        modifier = Modifier
            .fillMaxWidth()
            .metroClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

@Composable
fun MigrationListItem(
    modifier: Modifier,
    manga: Manga,
    source: String,
    chapterCount: Int,
    latestChapter: Double?,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .widthIn(max = 150.dp)
            .fillMaxWidth()
            .metroClickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CoverAspect)
                .background(MetroTheme.colors.secondarySurface, RectangleShape),
        ) {
            AsyncImage(
                model = manga.asMangaCover(),
                contentDescription = null,
                placeholder = ColorPainter(Color(0x1F888888)),
                error = ColorPainter(Color(0x1F888888)),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(MetroTheme.colors.accent, RectangleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                MetroText(
                    text = "$chapterCount",
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.primaryText,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0xCC000000))
                    .padding(6.dp),
            ) {
                MetroText(
                    text = manga.title,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.primaryText,
                    maxLines = 2,
                )
            }
        }

        Column(
            modifier = Modifier.padding(top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MetroText(
                text = source,
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.primaryText,
                maxLines = 1,
            )
            val formattedLatestChapters = remember(latestChapter) {
                latestChapter?.let(::formatChapterNumber)
            }
            MetroText(
                text = stringResource(
                    MR.strings.migrationListScreen_latestChapterLabel,
                    formattedLatestChapters
                        ?: stringResource(MR.strings.migrationListScreen_unknownLatestChapter),
                ),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun MigrationListItemResult(
    modifier: Modifier,
    result: MigratingManga.SearchResult,
    onItemClick: (Manga) -> Unit,
) {
    Box(modifier.height(IntrinsicSize.Min)) {
        when (result) {
            MigratingManga.SearchResult.Searching -> {
                Box(
                    modifier = Modifier
                        .widthIn(max = 150.dp)
                        .fillMaxSize()
                        .aspectRatio(CoverAspect),
                    contentAlignment = Alignment.Center,
                ) {
                    MetroLoadingDots()
                }
            }
            MigratingManga.SearchResult.NotFound -> {
                Column(
                    Modifier
                        .widthIn(max = 150.dp)
                        .fillMaxSize()
                        .padding(4.dp),
                ) {
                    Image(
                        painter = rememberResourceBitmapPainter(id = R.drawable.cover_error),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(CoverAspect)
                            .background(MetroTheme.colors.secondarySurface, RectangleShape),
                        contentScale = ContentScale.Crop,
                    )
                    MetroText(
                        text = stringResource(MR.strings.migrationListScreen_noMatchFoundText),
                        modifier = Modifier.padding(top = 6.dp),
                        style = MetroTextStyle.ListItemTitle,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
            }
            is MigratingManga.SearchResult.Success -> {
                MigrationListItem(
                    modifier = Modifier.fillMaxSize(),
                    manga = result.manga,
                    source = result.source,
                    chapterCount = result.chapterCount,
                    latestChapter = result.latestChapter,
                    onClick = { onItemClick(result.manga) },
                )
            }
        }
    }
}
