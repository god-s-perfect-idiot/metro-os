package com.metro.metron.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.presentation.history.components.HistoryDeleteAllDialog
import eu.kanade.presentation.history.components.HistoryDeleteDialog
import eu.kanade.presentation.manga.DuplicateMangaDialog
import eu.kanade.presentation.updates.UpdatesDeleteConfirmationDialog
import eu.kanade.presentation.updates.UpdatesFilterDialog
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.history.HistoryViewModel
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.updates.UpdatesSettingsViewModel
import eu.kanade.tachiyomi.ui.updates.UpdatesViewModel
import kotlinx.coroutines.flow.collectLatest
import mihon.feature.migration.dialog.MigrateMangaDialog
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MetronUpdatesPane(
    viewModel: UpdatesViewModel,
    settingsViewModel: UpdatesSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.resetNewUpdatesCount()
    }

    when {
        state.isLoading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MetroLoadingDots()
            }
        }
        state.items.isEmpty() -> {
            MetroEmptyState(
                message = stringResource(MR.strings.information_no_recent),
                modifier = modifier,
            )
        }
        else -> {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                items(state.items, key = { it.update.chapterId }) { item ->
                    val update = item.update
                    MetroListItem(
                        title = update.mangaTitle,
                        subtitle = update.chapterName,
                        singleLine = true,
                        leading = {
                            MetronCoverThumb(cover = update.coverData)
                        },
                        onClick = {
                            context.startActivity(
                                ReaderActivity.newIntent(
                                    context,
                                    update.mangaId,
                                    update.chapterId,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }

    when (val dialog = state.dialog) {
        is UpdatesViewModel.Dialog.DeleteConfirmation -> {
            UpdatesDeleteConfirmationDialog(
                onDismissRequest = { viewModel.setDialog(null) },
                onConfirm = { viewModel.deleteChapters(dialog.toDelete) },
            )
        }
        is UpdatesViewModel.Dialog.FilterSheet -> {
            UpdatesFilterDialog(
                onDismissRequest = { viewModel.setDialog(null) },
                viewModel = settingsViewModel,
            )
        }
        null -> Unit
    }
}

@Composable
fun MetronHistoryPane(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        if (state.searchQuery != null) {
            MetroTextBox(
                value = state.searchQuery.orEmpty(),
                onValueChange = viewModel::updateSearchQuery,
                placeholder = stringResource(MR.strings.action_search).lowercase(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 8.dp),
            )
        }

        val list = state.list
        when {
            list == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MetroLoadingDots()
                }
            }
            list.isEmpty() -> {
                val msg = if (!state.searchQuery.isNullOrEmpty()) {
                    stringResource(MR.strings.no_results_found)
                } else {
                    stringResource(MR.strings.information_no_recent_manga)
                }
                MetroEmptyState(message = msg)
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = list,
                        key = {
                            when (it) {
                                is HistoryUiModel.Header -> "h-${it.date}"
                                is HistoryUiModel.Item -> "i-${it.item.id}"
                            }
                        },
                    ) { model ->
                        when (model) {
                            is HistoryUiModel.Header -> {
                                MetroText(
                                    text = relativeDateText(model.date).lowercase(),
                                    style = MetroTextStyle.SectionHeader,
                                    color = MetroTheme.colors.accent,
                                    modifier = Modifier.padding(
                                        horizontal = MetroDimens.ScreenHorizontalMargin,
                                        vertical = 12.dp,
                                    ),
                                )
                            }
                            is HistoryUiModel.Item -> {
                                val history = model.item
                                MetroListItem(
                                    title = history.title,
                                    subtitle = history.chapterNumber
                                        .takeIf { it >= 0 }
                                        ?.let { "ch. $it" },
                                    singleLine = true,
                                    leading = {
                                        MetronCoverThumb(cover = history.coverData)
                                    },
                                    onClick = {
                                        viewModel.getNextChapterForManga(
                                            history.mangaId,
                                            history.chapterId,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    when (val dialog = state.dialog) {
        is HistoryViewModel.Dialog.Delete -> {
            HistoryDeleteDialog(
                onDismissRequest = { viewModel.setDialog(null) },
                onDelete = { all ->
                    if (all) {
                        viewModel.removeAllFromHistory(dialog.history.mangaId)
                    } else {
                        viewModel.removeFromHistory(dialog.history)
                    }
                },
            )
        }
        is HistoryViewModel.Dialog.DeleteAll -> {
            HistoryDeleteAllDialog(
                onDismissRequest = { viewModel.setDialog(null) },
                onDelete = viewModel::removeAllHistory,
            )
        }
        is HistoryViewModel.Dialog.DuplicateManga -> {
            DuplicateMangaDialog(
                duplicates = dialog.duplicates,
                onDismissRequest = { viewModel.setDialog(null) },
                onConfirm = { viewModel.addFavorite(dialog.manga) },
                onOpenManga = { navigator.push(MangaScreen(it.id)) },
                onMigrate = { viewModel.showMigrateDialog(dialog.manga, it) },
            )
        }
        is HistoryViewModel.Dialog.ChangeCategory -> {
            ChangeCategoryDialog(
                initialSelection = dialog.initialSelection,
                onDismissRequest = { viewModel.setDialog(null) },
                onEditCategories = { navigator.push(CategoryScreen()) },
                onConfirm = { include, _ ->
                    viewModel.moveMangaToCategoriesAndAddToLibrary(dialog.manga, include)
                },
            )
        }
        is HistoryViewModel.Dialog.Migrate -> {
            with(HistoryTab) {
                MigrateMangaDialog(
                    current = dialog.current,
                    target = dialog.target,
                    onClickTitle = { navigator.push(MangaScreen(dialog.current.id)) },
                    onDismissRequest = { viewModel.setDialog(null) },
                )
            }
        }
        null -> Unit
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HistoryViewModel.Event.OpenChapter -> {
                    val chapter = event.chapter ?: return@collectLatest
                    context.startActivity(
                        ReaderActivity.newIntent(context, chapter.mangaId, chapter.id),
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun MetronCoverThumb(
    cover: MangaCover,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = cover,
        contentDescription = null,
        placeholder = ColorPainter(Color(0x1F888888)),
        error = ColorPainter(Color(0x1F888888)),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(width = 48.dp, height = 72.dp)
            .background(MetroTheme.colors.secondarySurface, RectangleShape),
    )
}
