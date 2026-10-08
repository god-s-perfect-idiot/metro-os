package com.metro.metron.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroPivot
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.library.DeleteLibraryMangaDialog
import eu.kanade.presentation.more.onboarding.GETTING_STARTED_URL
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.library.LibraryUpdateWorker
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.ui.library.LibrarySettingsViewModel
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.library.LibraryViewModel
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.isLocal

private val LibraryTileGutter = 8.dp
/** WP8.1 media tiles are square (Music/Photos), not manga portrait cards. */
private val CoverAspect = 1f

@Composable
fun MetronLibraryPane(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val defaultCategoryTitle = stringResource(MR.strings.label_default)

    LibraryTab.CollectSearchEvents { query ->
        viewModel.search(query)
        if (navigator.lastItem !is MetronLibrarySearchScreen) {
            navigator.push(MetronLibrarySearchScreen())
        }
    }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            (context as? MainActivity)?.ready = true
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MetroLoadingDots()
                }
            }
            !state.hasActiveFilters && state.isLibraryEmpty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 24.dp),
                ) {
                    MetroText(
                        text = stringResource(MR.strings.information_empty_library),
                        style = MetroTextStyle.ListItemTitle,
                        color = MetroTheme.colors.secondaryText,
                    )
                    Spacer(Modifier.height(20.dp))
                    MetroBorderButton(
                        text = stringResource(MR.strings.getting_started_guide).lowercase(),
                        onClick = { uriHandler.openUri(GETTING_STARTED_URL) },
                    )
                }
            }
            else -> {
                val categories = state.displayedCategories
                if (categories.isEmpty()) {
                    MetroEmptyState(
                        message = stringResource(MR.strings.information_empty_library),
                    )
                } else {
                    MetronCategoryTileGrid(
                        categories = categories,
                        defaultCategoryTitle = defaultCategoryTitle,
                        itemCount = { state.getItemsForCategory(it).size },
                        coverFor = { category ->
                            state.getItemsForCategory(category)
                                .firstOrNull()
                                ?.libraryManga
                                ?.manga
                                ?.asMangaCover()
                        },
                        onCategoryClick = { category ->
                            val index = categories.indexOfFirst { it.id == category.id }
                                .coerceAtLeast(0)
                            viewModel.updateActiveCategoryIndex(index)
                            navigator.push(MetronLibraryCategoryScreen(category.id))
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    LibraryDialogs(
        viewModel = viewModel,
        state = state,
    )
}

/**
 * Dedicated library search page — opened from panorama / category app-bar search.
 */
class MetronLibrarySearchScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val handleBack = LocalBackPress.current
        val viewModel = metroViewModel<LibraryViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val searchFocus = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) {
            if (viewModel.state.value.searchQuery == null) {
                viewModel.search("")
            }
            searchFocus.requestFocus()
            keyboard?.show()
        }
        BackHandler {
            viewModel.search(null)
            handleBack?.invoke() ?: navigator.pop()
        }

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
                    MetroAppTitle(title = "metron")
                    MetroText(
                        text = "search",
                        style = MetroTextStyle.HubTitle,
                        modifier = Modifier.padding(start = MetroDimens.ScreenHorizontalMargin),
                    )
                    Spacer(Modifier.height(12.dp))
                    MetroTextBox(
                        value = state.searchQuery.orEmpty(),
                        onValueChange = viewModel::search,
                        placeholder = stringResource(MR.strings.action_search).lowercase(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                            .focusRequester(searchFocus),
                    )
                    Spacer(Modifier.height(8.dp))
                    when {
                        state.isLoading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                MetroLoadingDots()
                            }
                        }
                        state.searchQuery.isNullOrEmpty() -> {
                            MetroText(
                                text = stringResource(MR.strings.action_search_hint).lowercase(),
                                style = MetroTextStyle.Body,
                                color = MetroTheme.colors.secondaryText,
                                modifier = Modifier.padding(
                                    horizontal = MetroDimens.ScreenHorizontalMargin,
                                ),
                            )
                        }
                        else -> {
                            val matches = state.libraryData.favorites
                            if (matches.isEmpty()) {
                                MetroEmptyState(
                                    message = stringResource(MR.strings.no_results_found),
                                )
                            } else {
                                MetronMangaCoverGrid(
                                    items = matches,
                                    onClick = {
                                        navigator.push(MangaScreen(it.libraryManga.manga.id))
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
                MetroAppBar(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

/**
 * Pivot of every library category; comics as an n×2 cover tile grid.
 */
data class MetronLibraryCategoryScreen(
    private val initialCategoryId: Long,
) : Screen() {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<LibraryViewModel>()
        val settingsViewModel = metroViewModel<LibrarySettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val defaultCategoryTitle = stringResource(MR.strings.label_default)
        val scope = rememberCoroutineScope()

        val categories = state.displayedCategories
        val initialPage = categories.indexOfFirst { it.id == initialCategoryId }.coerceAtLeast(0)
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { categories.size.coerceAtLeast(1) },
        )

        LaunchedEffect(pagerState.currentPage, categories) {
            if (categories.isNotEmpty()) {
                viewModel.updateActiveCategoryIndex(pagerState.currentPage)
            }
        }
        LaunchedEffect(initialCategoryId, categories) {
            val target = categories.indexOfFirst { it.id == initialCategoryId }
            if (target >= 0 && pagerState.currentPage != target) {
                pagerState.scrollToPage(target)
            }
        }

        var appBarExpanded by remember { mutableStateOf(false) }
        var libraryOptions by remember { mutableStateOf<MetronLibraryOptionsPanel?>(null) }
        var retainedLibraryOptions by remember { mutableStateOf<MetronLibraryOptionsPanel?>(null) }
        fun dismissOptions() {
            libraryOptions = null
        }
        fun collapseAppBar() {
            appBarExpanded = false
        }
        fun openLibraryOptions(panel: MetronLibraryOptionsPanel) {
            collapseAppBar()
            if (libraryOptions == panel) {
                libraryOptions = null
            } else {
                retainedLibraryOptions = panel
                libraryOptions = panel
            }
        }
        val context = LocalContext.current

        MetroSystemTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .metroNavBarPadding()
                    .background(MetroTheme.colors.background),
            ) {
                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            MetroLoadingDots()
                        }
                    }
                    categories.isEmpty() -> {
                        MetroEmptyState(
                            message = stringResource(MR.strings.information_empty_library),
                            modifier = Modifier.padding(bottom = MetroAppBarDefaults.BarHeight),
                        )
                    }
                    else -> {
                        val titles = categories.map { it.displayName(defaultCategoryTitle) }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = MetroAppBarDefaults.BarHeight),
                        ) {
                            MetroPivot(
                                titles = titles,
                                pagerState = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                header = {
                                    MetroAppTitle(title = "library")
                                },
                                onTitleClick = { index ->
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                pageContent = { page ->
                                    val category = categories[page]
                                    val items = state.getItemsForCategory(category)
                                    if (items.isEmpty()) {
                                        MetroEmptyState(
                                            message = stringResource(
                                                MR.strings.information_no_manga_category,
                                            ),
                                        )
                                    } else {
                                        MetronMangaCoverGrid(
                                            items = items,
                                            onClick = {
                                                navigator.push(
                                                    MangaScreen(it.libraryManga.manga.id),
                                                )
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }

                BackHandler(enabled = libraryOptions != null || appBarExpanded) {
                    when {
                        libraryOptions != null -> dismissOptions()
                        else -> collapseAppBar()
                    }
                }

                MetroAppBar(
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Search,
                            label = "search",
                            onClick = {
                                dismissOptions()
                                collapseAppBar()
                                if (navigator.lastItem !is MetronLibrarySearchScreen) {
                                    navigator.push(MetronLibrarySearchScreen())
                                }
                            },
                        ),
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Filter,
                            label = "filter",
                            onClick = {
                                openLibraryOptions(MetronLibraryOptionsPanel.Filter)
                            },
                        ),
                    ),
                    expanded = appBarExpanded,
                    onExpandedChange = { appBarExpanded = it },
                    menuItems = listOf(
                        MetroAppBarMenuItem(
                            text = stringResource(MR.strings.action_sort).lowercase(),
                            onClick = {
                                openLibraryOptions(MetronLibraryOptionsPanel.Sort)
                            },
                        ),
                        MetroAppBarMenuItem(
                            text = stringResource(MR.strings.action_display).lowercase(),
                            onClick = {
                                openLibraryOptions(MetronLibraryOptionsPanel.Display)
                            },
                        ),
                        MetroAppBarMenuItem(
                            text = stringResource(MR.strings.action_update_category).lowercase(),
                            onClick = {
                                LibraryUpdateWorker.startNow(
                                    context.workManager,
                                    state.activeCategory,
                                )
                            },
                        ),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                MetronOptionsCard(
                    visible = libraryOptions != null,
                    onDismiss = ::dismissOptions,
                ) {
                    retainedLibraryOptions?.let { panel ->
                        MetronLibraryOptionsBody(
                            panel = panel,
                            viewModel = settingsViewModel,
                            category = state.activeCategory,
                        )
                    }
                }
            }
        }

        LibraryDialogs(
            viewModel = viewModel,
            state = state,
        )
    }
}

@Composable
private fun LibraryDialogs(
    viewModel: LibraryViewModel,
    state: LibraryViewModel.State,
) {
    val navigator = LocalNavigator.currentOrThrow
    when (val dialog = state.dialog) {
        is LibraryViewModel.Dialog.SettingsSheet -> {
            // Metro hosts filter/sort/display in the bottom app bar — ignore legacy sheet.
            LaunchedEffect(dialog) { viewModel.closeDialog() }
        }
        is LibraryViewModel.Dialog.ChangeCategory -> {
            ChangeCategoryDialog(
                initialSelection = dialog.initialSelection,
                onDismissRequest = viewModel::closeDialog,
                onEditCategories = {
                    viewModel.clearSelection()
                    navigator.push(CategoryScreen())
                },
                onConfirm = { include, exclude ->
                    viewModel.clearSelection()
                    viewModel.setMangaCategories(dialog.manga, include, exclude)
                },
            )
        }
        is LibraryViewModel.Dialog.DeleteManga -> {
            DeleteLibraryMangaDialog(
                containsLocalManga = dialog.manga.any(Manga::isLocal),
                onDismissRequest = viewModel::closeDialog,
                onConfirm = { deleteManga, deleteChapter ->
                    viewModel.removeMangas(dialog.manga, deleteManga, deleteChapter)
                    viewModel.clearSelection()
                },
            )
        }
        null -> Unit
    }
}

@Composable
private fun MetronCategoryTileGrid(
    categories: List<Category>,
    defaultCategoryTitle: String,
    itemCount: (Category) -> Int,
    coverFor: (Category) -> MangaCover?,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .padding(horizontal = 12.dp)
            .padding(top = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(LibraryTileGutter),
        verticalArrangement = Arrangement.spacedBy(LibraryTileGutter),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        items(categories, key = { it.id }) { category ->
            val title = category.displayName(defaultCategoryTitle)
            val count = itemCount(category)
            MetronCategoryTile(
                title = title,
                subtitle = if (count > 0) "$count" else null,
                cover = coverFor(category),
                onClick = { onCategoryClick(category) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
        }
    }
}

@Composable
private fun MetronCategoryTile(
    title: String,
    subtitle: String?,
    cover: MangaCover?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MetroTheme.colors.accent
    val content = MetroColors.tileContentColor(accent)
    Box(
        modifier = modifier
            .background(accent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = title },
    ) {
        if (cover != null) {
            MetronCoverImage(
                cover = cover,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                        ),
                    ),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
        ) {
            MetroText(
                text = title,
                style = MetroTextStyle.ListItemTitle,
                color = content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                MetroText(
                    text = subtitle,
                    style = MetroTextStyle.Body,
                    color = content.copy(alpha = 0.85f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MetronMangaCoverGrid(
    items: List<LibraryItem>,
    onClick: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(LibraryTileGutter),
        verticalArrangement = Arrangement.spacedBy(LibraryTileGutter),
    ) {
        items(items, key = { it.id }) { item ->
            val manga = item.libraryManga.manga
            MetronMangaCoverTile(
                title = manga.title,
                cover = manga.asMangaCover(),
                unreadCount = item.unreadCount,
                onClick = { onClick(item) },
            )
        }
    }
}

@Composable
private fun MetronMangaCoverTile(
    title: String,
    cover: MangaCover,
    unreadCount: Long,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(CoverAspect)
            .background(MetroTheme.colors.secondarySurface, RectangleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = title },
    ) {
        MetronCoverImage(cover = cover, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.78f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
        ) {
            MetroText(
                text = title,
                style = MetroTextStyle.Body,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (unreadCount > 0) {
                MetroText(
                    text = "$unreadCount",
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.accent,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MetronCoverImage(
    cover: MangaCover,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = cover,
        contentDescription = null,
        placeholder = ColorPainter(Color(0x1F888888)),
        error = ColorPainter(Color(0x1F888888)),
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

private fun Category.displayName(defaultTitle: String): String {
    return when {
        isSystemCategory -> defaultTitle.lowercase()
        name.isBlank() -> defaultTitle.lowercase()
        else -> name.lowercase()
    }
}
