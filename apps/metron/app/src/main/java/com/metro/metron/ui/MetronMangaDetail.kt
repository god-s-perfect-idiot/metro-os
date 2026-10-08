package com.metro.metron.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import coil3.compose.AsyncImage
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroListItem
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.getNameForMangaInfo
import eu.kanade.tachiyomi.ui.manga.ChapterList
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.missingChaptersCount
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

private val ContentBottomClearance = MetroAppBarDefaults.BarHeight + 32.dp

/**
 * Metro comic detail — square cover, chapter list, resume + actions in the bottom app bar
 * (no Material FAB / top toolbar; system Back navigates).
 */
@Composable
fun MetronMangaDetail(
    state: MangaViewModel.State.Success,
    snackbarHostState: SnackbarHostState,
    onChapterClicked: (Chapter) -> Unit,
    onDownloadChapter: ((List<ChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onFilterClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: () -> Unit,
    onCoverClicked: () -> Unit,
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onEditNotesClicked: () -> Unit,
    onMultiMarkAsReadClicked: (List<Chapter>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsReadClicked: (Chapter) -> Unit,
    onMultiDeleteClicked: (List<Chapter>) -> Unit,
    onChapterSelected: (ChapterList.Item, Boolean, Boolean) -> Unit,
    onAllChapterSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
) {
    val chapters = state.processedChapters
    val listItems = state.chapterListItems
    val isAnySelected = state.isAnySelected
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val isReading = remember(state.chapters) { state.chapters.fastAny { it.chapter.read } }
    val canResume = remember(chapters) { chapters.fastAny { !it.chapter.read } }
    val missingCount = remember(chapters) {
        chapters.map { it.chapter.chapterNumber }.missingChaptersCount()
    }
    val authorLine = listOfNotNull(
        state.manga.author?.takeIf { it.isNotBlank() },
        state.manga.artist?.takeIf { it.isNotBlank() && it != state.manga.author },
    ).joinToString(" · ").ifBlank { null }
    val sourceName = remember(state.source) { state.source.getNameForMangaInfo() }

    BackHandler(enabled = isAnySelected) {
        onAllChapterSelected(false)
    }

    MetroSystemTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .metroNavBarPadding()
                .background(MetroTheme.colors.background),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MetroSettingsHeader(
                    pageTitle = state.manga.title,
                    appTitle = "metron",
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = ContentBottomClearance),
                ) {
                    item(key = "cover") {
                        AsyncImage(
                            model = state.manga.asMangaCover(),
                            contentDescription = state.manga.title,
                            placeholder = ColorPainter(Color(0x1F888888)),
                            error = ColorPainter(Color(0x1F888888)),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .width(160.dp)
                                .aspectRatio(1f)
                                .background(MetroTheme.colors.secondarySurface, RectangleShape)
                                .metroClickable(onClick = onCoverClicked),
                        )
                    }
                    if (authorLine != null) {
                        item(key = "author") {
                            MetroText(
                                text = authorLine,
                                style = MetroTextStyle.Body,
                                color = MetroTheme.colors.secondaryText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                    item(key = "source") {
                        MetroText(
                            text = sourceName,
                            style = MetroTextStyle.ListItemSubtitle,
                            color = MetroTheme.colors.secondaryText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        )
                    }
                    if (!state.manga.description.isNullOrBlank()) {
                        item(key = "description") {
                            MetroText(
                                text = state.manga.description.orEmpty(),
                                style = MetroTextStyle.Body,
                                color = MetroTheme.colors.primaryText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            )
                        }
                    }
                    item(key = "chapters-header") {
                        val header = buildString {
                            append(stringResource(MR.strings.chapters))
                            if (chapters.isNotEmpty()) append(" (${chapters.size})")
                            if (missingCount > 0) {
                                append(" · ")
                                append(
                                    pluralStringResource(
                                        MR.plurals.missing_chapters,
                                        count = missingCount.toInt(),
                                        missingCount.toInt(),
                                    ),
                                )
                            }
                        }
                        MetroText(
                            text = header.lowercase(),
                            style = MetroTextStyle.SectionHeader,
                            color = MetroTheme.colors.accent,
                            modifier = Modifier.padding(
                                start = 12.dp,
                                end = 12.dp,
                                top = 16.dp,
                                bottom = 4.dp,
                            ),
                        )
                    }
                    items(
                        items = listItems,
                        key = { item ->
                            when (item) {
                                is ChapterList.MissingCount -> "missing-${item.id}"
                                is ChapterList.Item -> "chapter-${item.id}"
                            }
                        },
                    ) { item ->
                        when (item) {
                            is ChapterList.MissingCount -> {
                                MetroText(
                                    text = pluralStringResource(
                                        MR.plurals.missing_chapters,
                                        count = item.count,
                                        item.count,
                                    ),
                                    style = MetroTextStyle.ListItemSubtitle,
                                    color = MetroTheme.colors.secondaryText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                            is ChapterList.Item -> {
                                val title = if (state.manga.displayMode == Manga.CHAPTER_DISPLAY_NUMBER) {
                                    stringResource(
                                        MR.strings.display_mode_chapter,
                                        formatChapterNumber(item.chapter.chapterNumber),
                                    )
                                } else {
                                    item.chapter.name
                                }
                                val subtitle = buildList {
                                    relativeDateText(item.chapter.dateUpload)
                                        .takeIf { it.isNotBlank() }
                                        ?.let { add(it) }
                                    item.chapter.scanlator?.takeIf { it.isNotBlank() }?.let { add(it) }
                                    item.chapter.lastPageRead
                                        .takeIf { !item.chapter.read && it > 0L }
                                        ?.let {
                                            add(
                                                stringResource(MR.strings.chapter_progress, it + 1),
                                            )
                                        }
                                }.joinToString(" · ").ifBlank { null }
                                MetroListItem(
                                    title = title,
                                    subtitle = subtitle,
                                    titleStyle = MetroTextStyle.Body,
                                    titleColor = when {
                                        item.selected -> MetroTheme.colors.accent
                                        item.chapter.read -> MetroTheme.colors.secondaryText
                                        else -> null
                                    },
                                    oneLineMinHeight = 52.dp,
                                    twoLineMinHeight = 60.dp,
                                    verticalPadding = 6.dp,
                                    onClick = null,
                                    modifier = Modifier.metroClickable(
                                        onLongClick = {
                                            onChapterSelected(item, !item.selected, true)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onClick = {
                                            if (isAnySelected) {
                                                onChapterSelected(item, !item.selected, false)
                                            } else {
                                                onChapterClicked(item.chapter)
                                            }
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            MetronSnackbarHost(hostState = snackbarHostState)

            if (isAnySelected) {
                val selected = chapters.filter { it.selected }
                MetroAppBar(
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Clear,
                            label = "cancel",
                            onClick = { onAllChapterSelected(false) },
                        ),
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Check,
                            label = stringResource(MR.strings.action_select_all),
                            onClick = { onAllChapterSelected(true) },
                        ),
                    ),
                    menuItems = buildList {
                        if (selected.fastAny { !it.chapter.read }) {
                            add(
                                MetroAppBarMenuItem(
                                    text = stringResource(MR.strings.action_mark_as_read).lowercase(),
                                    onClick = {
                                        onMultiMarkAsReadClicked(selected.fastMap { it.chapter }, true)
                                    },
                                ),
                            )
                        }
                        if (selected.fastAny { it.chapter.read || it.chapter.lastPageRead > 0L }) {
                            add(
                                MetroAppBarMenuItem(
                                    text = stringResource(MR.strings.action_mark_as_unread).lowercase(),
                                    onClick = {
                                        onMultiMarkAsReadClicked(selected.fastMap { it.chapter }, false)
                                    },
                                ),
                            )
                        }
                        if (selected.size == 1) {
                            add(
                                MetroAppBarMenuItem(
                                    text = stringResource(MR.strings.action_mark_previous_as_read)
                                        .lowercase(),
                                    onClick = { onMarkPreviousAsReadClicked(selected[0].chapter) },
                                ),
                            )
                        }
                        add(
                            MetroAppBarMenuItem(
                                text = stringResource(MR.strings.action_select_inverse).lowercase(),
                                onClick = onInvertSelection,
                            ),
                        )
                        if (onDownloadChapter != null &&
                            selected.fastAny { it.downloadState != Download.State.DOWNLOADED }
                        ) {
                            add(
                                MetroAppBarMenuItem(
                                    text = stringResource(MR.strings.manga_download).lowercase(),
                                    onClick = {
                                        onDownloadChapter(selected, ChapterDownloadAction.START)
                                    },
                                ),
                            )
                        }
                        if (selected.fastAny { it.downloadState == Download.State.DOWNLOADED }) {
                            add(
                                MetroAppBarMenuItem(
                                    text = stringResource(MR.strings.action_delete).lowercase(),
                                    onClick = {
                                        onMultiDeleteClicked(selected.fastMap { it.chapter })
                                    },
                                ),
                            )
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            } else {
                MetroAppBar(
                    icons = buildList {
                        if (canResume) {
                            add(
                                MetroAppBarIcon(
                                    type = MetroSystemIconType.Play,
                                    label = stringResource(
                                        if (isReading) {
                                            MR.strings.action_resume
                                        } else {
                                            MR.strings.action_start
                                        },
                                    ).lowercase(),
                                    onClick = onContinueReading,
                                ),
                            )
                        }
                        add(
                            MetroAppBarIcon(
                                type = if (state.manga.favorite) {
                                    MetroSystemIconType.Heart
                                } else {
                                    MetroSystemIconType.HeartSlash
                                },
                                label = stringResource(
                                    if (state.manga.favorite) {
                                        MR.strings.in_library
                                    } else {
                                        MR.strings.add_to_library
                                    },
                                ).lowercase(),
                                onClick = onAddToLibraryClicked,
                            ),
                        )
                        add(
                            MetroAppBarIcon(
                                type = MetroSystemIconType.Filter,
                                label = stringResource(MR.strings.action_filter).lowercase(),
                                onClick = onFilterClicked,
                            ),
                        )
                    },
                    menuItems = buildList {
                        if (onDownloadActionClicked != null) {
                            add(
                                MetroAppBarMenuItem(
                                    text = stringResource(MR.strings.manga_download).lowercase(),
                                    onClick = {
                                        onDownloadActionClicked(DownloadAction.NEXT_5_CHAPTERS)
                                    },
                                ),
                            )
                        }
                        add(
                            MetroAppBarMenuItem(
                                text = stringResource(MR.strings.action_webview_refresh).lowercase(),
                                onClick = onRefresh,
                            ),
                        )
                        if (onEditCategoryClicked != null) {
                            add(
                                MetroAppBarMenuItem(
                                    text = stringResource(MR.strings.action_edit_categories).lowercase(),
                                    onClick = onEditCategoryClicked,
                                ),
                            )
                        }
                        add(
                            MetroAppBarMenuItem(
                                text = stringResource(MR.strings.manga_tracking_tab).lowercase(),
                                onClick = onTrackingClicked,
                            ),
                        )
                        if (onWebViewClicked != null) {
                            add(
                                MetroAppBarMenuItem(
                                    text = stringResource(MR.strings.action_web_view).lowercase(),
                                    onClick = onWebViewClicked,
                                ),
                            )
                        }
                        if (onMigrateClicked != null) {
                            add(
                                MetroAppBarMenuItem(
                                    text = stringResource(MR.strings.action_migrate).lowercase(),
                                    onClick = onMigrateClicked,
                                ),
                            )
                        }
                        if (onShareClicked != null) {
                            add(
                                MetroAppBarMenuItem(
                                    text = stringResource(MR.strings.action_share).lowercase(),
                                    onClick = onShareClicked,
                                ),
                            )
                        }
                        add(
                            MetroAppBarMenuItem(
                                text = stringResource(MR.strings.action_notes).lowercase(),
                                onClick = onEditNotesClicked,
                            ),
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
