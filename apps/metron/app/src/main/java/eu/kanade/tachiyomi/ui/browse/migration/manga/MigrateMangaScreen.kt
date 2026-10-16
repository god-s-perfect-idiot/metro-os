package eu.kanade.tachiyomi.ui.browse.migration.manga

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroMultiSelectRow
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import mihon.feature.migration.config.MigrationConfigScreen
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data class MigrateMangaScreen(
    private val sourceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel =
            assistedMetroViewModel<MigrateMangaViewModel, MigrateMangaViewModel.Factory> { create(sourceId = sourceId) }

        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state.isLoading) {
            MetroSystemTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .metroNavBarPadding()
                        .background(MetroTheme.colors.background),
                    contentAlignment = Alignment.Center,
                ) {
                    MetroLoadingDots()
                }
            }
            return
        }

        BackHandler(enabled = state.selectionMode) {
            viewModel.clearSelection()
        }

        val lazyListState = rememberLazyListState()
        val sourceName = state.source?.name.orEmpty()

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
                    MetroPageHeader(title = sourceName.lowercase())
                    if (state.isEmpty) {
                        MetroEmptyState(
                            message = stringResource(MR.strings.empty_screen).lowercase(),
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp),
                        ) {
                            items(state.titles, key = { it.id }) { manga ->
                                val selected = manga.id in state.selection
                                MetroMultiSelectRow(
                                    title = manga.title,
                                    checked = selected,
                                    onClick = { viewModel.toggleSelection(manga) },
                                    leading = {
                                        AsyncImage(
                                            model = manga.asMangaCover(),
                                            contentDescription = null,
                                            placeholder = ColorPainter(Color(0x1F888888)),
                                            error = ColorPainter(Color(0x1F888888)),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    MetroTheme.colors.secondarySurface,
                                                    RectangleShape,
                                                ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                MetroAppBar(
                    icons = buildList {
                        if (state.selectionMode) {
                            add(
                                MetroAppBarIcon(
                                    type = MetroSystemIconType.Forward,
                                    label = stringResource(
                                        MR.strings.migrationConfigScreen_continueButtonText,
                                    ).lowercase(),
                                    onClick = {
                                        val selection = state.selection
                                        viewModel.clearSelection()
                                        navigator.push(MigrationConfigScreen(selection))
                                    },
                                ),
                            )
                            add(
                                MetroAppBarIcon(
                                    type = MetroSystemIconType.Close,
                                    label = stringResource(MR.strings.action_cancel).lowercase(),
                                    onClick = viewModel::clearSelection,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        LaunchedEffect(Unit) {
            viewModel.events.collectLatest { event ->
                when (event) {
                    MigrationMangaEvent.FailedFetchingFavorites -> {
                        context.toast(MR.strings.internal_error)
                    }
                }
            }
        }
    }
}
