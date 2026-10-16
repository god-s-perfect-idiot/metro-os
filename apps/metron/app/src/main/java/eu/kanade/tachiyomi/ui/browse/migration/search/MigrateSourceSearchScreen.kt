package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroDimens
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.browse.BrowseSourceContent
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceViewModel
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterDialog
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.launch
import mihon.feature.migration.dialog.MigrateMangaDialog
import mihon.feature.migration.list.MigrationListScreen
import mihon.presentation.core.util.collectAsLazyPagingItems
import tachiyomi.core.common.Constants
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.LocalSource

data class MigrateSourceSearchScreen(
    private val currentManga: Manga,
    private val sourceId: Long,
    private val query: String?,
) : Screen() {

    @Composable
    override fun Content() {
        val uriHandler = LocalUriHandler.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val searchFocus = remember { FocusRequester() }

        val viewModel =
            assistedMetroViewModel<BrowseSourceViewModel, BrowseSourceViewModel.Factory> {
                create(sourceId = sourceId, listingQuery = query)
            }
        val state by viewModel.state.collectAsState()

        val source = state.source
        if (source == null) {
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

        val snackbarHostState = remember { SnackbarHostState() }
        val contentPadding = PaddingValues(bottom = MetroAppBarDefaults.BarHeight + 32.dp)

        MetroSystemTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .metroNavBarPadding()
                    .background(MetroTheme.colors.background),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MetroAppTitle(title = source.name)
                    MetroTextBox(
                        value = state.toolbarQuery.orEmpty(),
                        onValueChange = viewModel::setToolbarQuery,
                        placeholder = stringResource(MR.strings.action_search_hint).lowercase(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.search(state.toolbarQuery.orEmpty())
                                focusManager.clearFocus(force = true)
                                keyboard?.hide()
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MetroDimens.ScreenHorizontalMargin,
                                vertical = 4.dp,
                            )
                            .focusRequester(searchFocus),
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        val openMigrateDialog: (Manga) -> Unit = {
                            val migrateListScreen = navigator.items
                                .filterIsInstance<MigrationListScreen>()
                                .lastOrNull()

                            if (migrateListScreen == null) {
                                viewModel.setDialog(
                                    BrowseSourceViewModel.Dialog.Migrate(
                                        target = it,
                                        current = currentManga,
                                    ),
                                )
                            } else {
                                migrateListScreen.addMatchOverride(
                                    current = currentManga.id,
                                    target = it.id,
                                )
                                navigator.popUntil { screen -> screen is MigrationListScreen }
                            }
                        }
                        BrowseSourceContent(
                            source = source,
                            mangaList = viewModel.mangaPagerFlowFlow.collectAsLazyPagingItems(),
                            columns = viewModel.getColumnsPreference(
                                LocalConfiguration.current.orientation,
                            ),
                            displayMode = viewModel.displayMode,
                            snackbarHostState = snackbarHostState,
                            contentPadding = contentPadding,
                            onWebViewClick = {
                                val httpSource = source as? HttpSource ?: return@BrowseSourceContent
                                navigator.push(
                                    WebViewScreen(
                                        url = httpSource.getHomeUrl(),
                                        initialTitle = httpSource.name,
                                        sourceId = httpSource.id,
                                    ),
                                )
                            },
                            onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
                            onLocalSourceHelpClick = { uriHandler.openUri(LocalSource.HELP_URL) },
                            onMangaClick = openMigrateDialog,
                            onMangaLongClick = { navigator.push(MangaScreen(it.id, true)) },
                        )
                        com.metro.metron.ui.MetronSnackbarHost(hostState = snackbarHostState)
                    }
                }

                MetroAppBar(
                    icons = buildList {
                        add(
                            MetroAppBarIcon(
                                type = MetroSystemIconType.Search,
                                label = stringResource(MR.strings.action_search).lowercase(),
                                onClick = {
                                    viewModel.search(state.toolbarQuery.orEmpty())
                                    focusManager.clearFocus(force = true)
                                    keyboard?.hide()
                                },
                            ),
                        )
                        if (state.filters.isNotEmpty()) {
                            add(
                                MetroAppBarIcon(
                                    type = MetroSystemIconType.Filter,
                                    label = stringResource(MR.strings.action_filter).lowercase(),
                                    onClick = viewModel::openFilterSheet,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        val onDismissRequest = { viewModel.setDialog(null) }
        when (val dialog = state.dialog) {
            is BrowseSourceViewModel.Dialog.Filter -> {
                SourceFilterDialog(
                    onDismissRequest = onDismissRequest,
                    filters = state.filters,
                    onReset = viewModel::resetFilters,
                    onFilter = { viewModel.search(filters = state.filters) },
                    onUpdate = viewModel::setFilters,
                )
            }
            is BrowseSourceViewModel.Dialog.Migrate -> {
                MigrateMangaDialog(
                    current = currentManga,
                    target = dialog.target,
                    onClickTitle = { navigator.push(MangaScreen(dialog.target.id)) },
                    onDismissRequest = onDismissRequest,
                    onComplete = {
                        scope.launch {
                            navigator.popUntilRoot()
                            HomeScreen.openTab(HomeScreen.Tab.Browse())
                            navigator.push(MangaScreen(dialog.target.id))
                        }
                    },
                )
            }
            else -> {}
        }
    }
}
