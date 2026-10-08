package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroDimens
import com.metro.ui.MetroPivot
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import com.metro.ui.metroNavBarPadding
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.browse.BrowseSourceContent
import eu.kanade.presentation.browse.MissingSourceScreen
import eu.kanade.presentation.browse.components.RemoveMangaDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.manga.DuplicateMangaDialog
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.extension.details.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceViewModel.Listing
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.feature.migration.dialog.MigrateMangaDialog
import mihon.presentation.core.util.collectAsLazyPagingItems
import tachiyomi.core.common.Constants
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.source.local.LocalSource

data class BrowseSourceScreen(
    val sourceId: Long,
    private val listingQuery: String?,
) : Screen(), AssistContentScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val viewModel =
            assistedMetroViewModel<BrowseSourceViewModel, BrowseSourceViewModel.Factory> {
                create(sourceId = sourceId, listingQuery = listingQuery)
            }
        val state by viewModel.state.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val searchFocus = remember { FocusRequester() }
        var searchVisible by remember { mutableStateOf(false) }
        var imeWasVisibleWhileSearching by remember { mutableStateOf(false) }
        var everFocused by remember { mutableStateOf(false) }
        val imeVisible = WindowInsets.isImeVisible

        val dismissSearchUi: () -> Unit = {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
            searchVisible = false
            imeWasVisibleWhileSearching = false
            everFocused = false
            if (!state.isUserQuery) {
                viewModel.setToolbarQuery(null)
            }
        }

        val leaveSearchListing: () -> Unit = {
            dismissSearchUi()
            viewModel.resetFilters()
            viewModel.setListing(Listing.Popular)
        }

        val navigateUp: () -> Unit = {
            when {
                searchVisible && !state.isUserQuery -> dismissSearchUi()
                state.listing is Listing.Search -> leaveSearchListing()
                else -> navigator.pop()
            }
        }

        val source = state.source
        if (source == null) {
            LoadingScreen()
            return
        }

        if (source is StubSource) {
            MissingSourceScreen(
                source = source,
                navigateUp = navigateUp,
            )
            return
        }

        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val uriHandler = LocalUriHandler.current
        val snackbarHostState = remember { SnackbarHostState() }

        val onHelpClick = { uriHandler.openUri(LocalSource.HELP_URL) }
        val onWebViewClick = f@{
            val httpSource = source as? HttpSource ?: return@f
            navigator.push(
                WebViewScreen(
                    url = httpSource.getHomeUrl(),
                    initialTitle = httpSource.name,
                    sourceId = httpSource.id,
                ),
            )
        }

        LaunchedEffect(source) {
            assistUrl = (source as? HttpSource)?.getHomeUrl()
        }

        // Keep search chrome open when arriving already in a text-search listing.
        LaunchedEffect(Unit) {
            if (state.isUserQuery) {
                searchVisible = true
            }
        }

        LaunchedEffect(searchVisible, imeVisible) {
            if (!searchVisible) {
                imeWasVisibleWhileSearching = false
                everFocused = false
                return@LaunchedEffect
            }
            if (imeVisible) {
                imeWasVisibleWhileSearching = true
            } else if (imeWasVisibleWhileSearching && !state.isUserQuery) {
                dismissSearchUi()
            }
        }

        BackHandler(onBack = navigateUp)

        val showingBrowsePivots =
            !searchVisible && state.listing !is Listing.Search
        val supportsLatest = source.supportsLatest
        val pivotTitles = buildList {
            add(stringResource(MR.strings.popular).lowercase())
            if (supportsLatest) {
                add(stringResource(MR.strings.latest).lowercase())
            }
        }
        val pagerState = rememberPagerState(
            initialPage = if (state.listing == Listing.Latest && supportsLatest) 1 else 0,
            pageCount = { pivotTitles.size.coerceAtLeast(1) },
        )

        LaunchedEffect(state.listing, supportsLatest) {
            if (state.listing is Listing.Search) return@LaunchedEffect
            val target = when {
                state.listing == Listing.Latest && supportsLatest -> 1
                else -> 0
            }
            if (pagerState.currentPage != target && !pagerState.isScrollInProgress) {
                pagerState.scrollToPage(target)
            }
        }

        LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress, showingBrowsePivots) {
            if (!showingBrowsePivots || pagerState.isScrollInProgress) return@LaunchedEffect
            val target = when {
                supportsLatest && pagerState.currentPage == 1 -> Listing.Latest
                else -> Listing.Popular
            }
            if (state.listing != target) {
                viewModel.resetFilters()
                viewModel.setListing(target)
            }
        }

        val contentPadding = PaddingValues(bottom = MetroAppBarDefaults.BarHeight + 32.dp)
        val mangaList = viewModel.mangaPagerFlowFlow.collectAsLazyPagingItems()

        @Composable
        fun SourceBrowseList() {
            BrowseSourceContent(
                source = source,
                mangaList = mangaList,
                columns = viewModel.getColumnsPreference(
                    LocalConfiguration.current.orientation,
                ),
                displayMode = viewModel.displayMode,
                snackbarHostState = snackbarHostState,
                contentPadding = contentPadding,
                onWebViewClick = onWebViewClick,
                onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
                onLocalSourceHelpClick = onHelpClick,
                onMangaClick = { navigator.push(MangaScreen(it.id, true)) },
                onMangaLongClick = { manga ->
                    scope.launchIO {
                        val duplicates = viewModel.getDuplicateLibraryManga(manga)
                        when {
                            manga.favorite -> viewModel.setDialog(
                                BrowseSourceViewModel.Dialog.RemoveManga(manga),
                            )
                            duplicates.isNotEmpty() -> viewModel.setDialog(
                                BrowseSourceViewModel.Dialog.AddDuplicateManga(
                                    manga,
                                    duplicates,
                                ),
                            )
                            else -> viewModel.addFavorite(manga)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
            )
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
                    val showBrowsePivots =
                        pivotTitles.size > 1 &&
                            !searchVisible &&
                            state.listing !is Listing.Search

                    if (!showBrowsePivots) {
                        MetroAppTitle(title = source.name)
                    }

                    AnimatedVisibility(
                        visible = searchVisible,
                        enter = expandVertically(
                            animationSpec = tween(
                                MetroTransitions.PageTransitionMs,
                                easing = MetroTransitions.PageEasing,
                            ),
                        ) + fadeIn(
                            animationSpec = tween(
                                MetroTransitions.PageTransitionMs,
                                easing = MetroTransitions.PageEasing,
                            ),
                        ),
                        exit = shrinkVertically(
                            animationSpec = tween(
                                MetroTransitions.PageTransitionMs,
                                easing = MetroTransitions.PageEasing,
                            ),
                        ) + fadeOut(
                            animationSpec = tween(
                                MetroTransitions.PageTransitionMs,
                                easing = MetroTransitions.PageEasing,
                            ),
                        ),
                    ) {
                        LaunchedEffect(Unit) {
                            everFocused = false
                            searchFocus.requestFocus()
                            keyboard?.show()
                        }
                        MetroTextBox(
                            value = state.toolbarQuery.orEmpty(),
                            onValueChange = { viewModel.setToolbarQuery(it) },
                            placeholder = stringResource(MR.strings.action_search_hint).lowercase(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    viewModel.search(state.toolbarQuery.orEmpty())
                                    focusManager.clearFocus(force = true)
                                    keyboard?.hide()
                                },
                            ),
                            onFocusChange = { focused ->
                                if (focused) {
                                    everFocused = true
                                } else if (everFocused && !state.isUserQuery) {
                                    dismissSearchUi()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = MetroDimens.ScreenHorizontalMargin,
                                    vertical = 4.dp,
                                )
                                .focusRequester(searchFocus),
                        )
                    }

                    when {
                        searchVisible || state.listing is Listing.Search -> {
                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                SourceBrowseList()
                                com.metro.metron.ui.MetronSnackbarHost(hostState = snackbarHostState)
                            }
                        }
                        pivotTitles.size > 1 -> {
                            MetroPivot(
                                titles = pivotTitles,
                                pagerState = pagerState,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                header = { MetroAppTitle(title = source.name) },
                                onTitleClick = { index ->
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                            ) { page ->
                                if (page == pagerState.currentPage) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        SourceBrowseList()
                                        com.metro.metron.ui.MetronSnackbarHost(
                                            hostState = snackbarHostState,
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            // Single listing (popular only) — no pivot chrome.
                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                SourceBrowseList()
                                com.metro.metron.ui.MetronSnackbarHost(
                                    hostState = snackbarHostState,
                                )
                            }
                        }
                    }
                }

                MetroAppBar(
                    icons = buildList {
                        add(
                            MetroAppBarIcon(
                                type = MetroSystemIconType.Search,
                                label = stringResource(MR.strings.action_search).lowercase(),
                                onClick = {
                                    if (searchVisible) {
                                        viewModel.search(state.toolbarQuery.orEmpty())
                                        focusManager.clearFocus(force = true)
                                        keyboard?.hide()
                                    } else {
                                        searchVisible = true
                                        if (state.toolbarQuery == null) {
                                            viewModel.setToolbarQuery("")
                                        }
                                    }
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
                    menuItems = buildList {
                        add(
                            MetroAppBarMenuItem(
                                text = stringResource(MR.strings.action_display_comfortable_grid)
                                    .lowercase(),
                                onClick = {
                                    viewModel.displayMode = LibraryDisplayMode.ComfortableGrid
                                },
                            ),
                        )
                        add(
                            MetroAppBarMenuItem(
                                text = stringResource(MR.strings.action_display_grid)
                                    .lowercase(),
                                onClick = {
                                    viewModel.displayMode = LibraryDisplayMode.CompactGrid
                                },
                            ),
                        )
                        add(
                            MetroAppBarMenuItem(
                                text = stringResource(MR.strings.action_display_list).lowercase(),
                                onClick = {
                                    viewModel.displayMode = LibraryDisplayMode.List
                                },
                            ),
                        )
                        if (source is LocalSource) {
                            add(
                                MetroAppBarMenuItem(
                                    text = stringResource(MR.strings.label_help).lowercase(),
                                    onClick = onHelpClick,
                                ),
                            )
                        } else {
                            add(
                                MetroAppBarMenuItem(
                                    text = stringResource(MR.strings.action_open_in_web_view)
                                        .lowercase(),
                                    onClick = onWebViewClick,
                                ),
                            )
                        }
                        if (source is ConfigurableSource) {
                            add(
                                MetroAppBarMenuItem(
                                    text = stringResource(MR.strings.action_settings).lowercase(),
                                    onClick = {
                                        navigator.push(SourcePreferencesScreen(sourceId))
                                    },
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
                    onFilter = {
                        searchVisible = false
                        viewModel.search(filters = state.filters)
                    },
                    onUpdate = viewModel::setFilters,
                )
            }
            is BrowseSourceViewModel.Dialog.AddDuplicateManga -> {
                DuplicateMangaDialog(
                    duplicates = dialog.duplicates,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { viewModel.addFavorite(dialog.manga) },
                    onOpenManga = { navigator.push(MangaScreen(it.id)) },
                    onMigrate = {
                        viewModel.setDialog(BrowseSourceViewModel.Dialog.Migrate(dialog.manga, it))
                    },
                )
            }

            is BrowseSourceViewModel.Dialog.Migrate -> {
                MigrateMangaDialog(
                    current = dialog.current,
                    target = dialog.target,
                    onClickTitle = { navigator.push(MangaScreen(dialog.current.id)) },
                    onDismissRequest = onDismissRequest,
                )
            }
            is BrowseSourceViewModel.Dialog.RemoveManga -> {
                RemoveMangaDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        viewModel.changeMangaFavorite(dialog.manga)
                    },
                    mangaToRemove = dialog.manga,
                )
            }
            is BrowseSourceViewModel.Dialog.ChangeMangaCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoryScreen()) },
                    onConfirm = { include, _ ->
                        viewModel.changeMangaFavorite(dialog.manga)
                        viewModel.moveMangaToCategories(dialog.manga, include)
                    },
                )
            }
            else -> {}
        }

        LaunchedEffect(Unit) {
            queryEvent.receiveAsFlow()
                .collectLatest {
                    when (it) {
                        is SearchType.Genre -> viewModel.searchGenre(it.txt)
                        is SearchType.Text -> {
                            searchVisible = true
                            viewModel.search(it.txt)
                        }
                    }
                }
        }
    }

    suspend fun search(query: String) = queryEvent.send(SearchType.Text(query))
    suspend fun searchGenre(name: String) = queryEvent.send(SearchType.Genre(name))

    companion object {
        private val queryEvent = Channel<SearchType>()
    }

    sealed class SearchType(val txt: String) {
        class Text(txt: String) : SearchType(txt)
        class Genre(txt: String) : SearchType(txt)
    }
}
