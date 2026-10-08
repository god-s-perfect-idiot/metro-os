package com.metro.metron.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroPanorama
import com.metro.ui.MetroPanoramaBodyEnter
import com.metro.ui.MetroPanoramaBrandEnter
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.tachiyomi.data.library.LibraryUpdateWorker
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.history.HistoryViewModel
import eu.kanade.tachiyomi.ui.library.LibrarySettingsViewModel
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.library.LibraryViewModel
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesSettingsViewModel
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import eu.kanade.tachiyomi.ui.updates.UpdatesViewModel
import eu.kanade.tachiyomi.util.system.workManager
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import mihon.feature.upcoming.UpcomingScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private const val BrandText = "metron"
private val BrandInset = 12.dp

private val BrandStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 96.sp,
    lineHeight = 100.sp,
    letterSpacing = (-1).sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private val HubTabs: List<Tab> = listOf(
    LibraryTab,
    UpdatesTab,
    HistoryTab,
    BrowseTab,
)

/**
 * WP8.1 panorama home chrome with Metro list panes over real Mihon ViewModels
 * (library / updates / history / browse). Replaces Material tab Content() embedding.
 *
 * [MoreTab] is opened full-page (not a panorama pane) via the app-bar overflow menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetronHomeScaffold(
    onOpenMore: () -> Unit,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabNavigator = LocalTabNavigator.current
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var introPlayed by remember { mutableStateOf(false) }
    val showingMore = tabNavigator.current::class == MoreTab::class

    val libraryViewModel = metroViewModel<LibraryViewModel>()
    val librarySettingsViewModel = metroViewModel<LibrarySettingsViewModel>()
    val updatesViewModel = metroViewModel<UpdatesViewModel>()
    val updatesSettingsViewModel = metroViewModel<UpdatesSettingsViewModel>()
    val historyViewModel = metroViewModel<HistoryViewModel>()

    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()

    BrowseTab.CollectOpenExtensions {
        navigator.push(MetronExtensionsScreen())
    }

    MetroSystemTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .metroNavBarPadding()
                .background(MetroTheme.colors.background),
        ) {
            if (showingMore) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = MetroAppBarDefaults.BarHeight),
                ) {
                    MoreTab.Content()
                }
                MetroAppBar(
                    menuItems = listOf(
                        MetroAppBarMenuItem("downloads", onClick = onOpenDownloads),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            } else {
                val initialPage = HubTabs.indexOfFirst { it::class == tabNavigator.current::class }
                    .coerceAtLeast(0)
                val pagerState = rememberPagerState(
                    initialPage = initialPage,
                    pageCount = { HubTabs.size },
                )

                LaunchedEffect(pagerState.currentPage) {
                    val target = HubTabs[pagerState.currentPage]
                    if (tabNavigator.current::class != target::class) {
                        tabNavigator.current = target
                    }
                }
                LaunchedEffect(tabNavigator.current) {
                    val index = HubTabs.indexOfFirst { it::class == tabNavigator.current::class }
                    if (index >= 0 && pagerState.currentPage != index) {
                        pagerState.scrollToPage(index)
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    MetroPanoramaBrandEnter(skipEnter = introPlayed) {
                        BrandTitle(
                            pageProgress = pagerState.currentPage + pagerState.currentPageOffsetFraction,
                        )
                    }
                    MetroPanoramaBodyEnter(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        skipEnter = introPlayed,
                    ) {
                        MetroPanorama(
                            titles = listOf("library", "updates", "history", "browse"),
                            pagerState = pagerState,
                            titleStyle = MetroTextStyle.HubLink,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = MetroAppBarDefaults.BarHeight),
                            pageContent = { page ->
                                when (page) {
                                    0 -> MetronLibraryPane(
                                        viewModel = libraryViewModel,
                                        settingsViewModel = librarySettingsViewModel,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    1 -> MetronUpdatesPane(
                                        viewModel = updatesViewModel,
                                        settingsViewModel = updatesSettingsViewModel,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    2 -> MetronHistoryPane(
                                        viewModel = historyViewModel,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    else -> MetronBrowsePane(modifier = Modifier.fillMaxSize())
                                }
                            },
                        )
                    }
                }

                val page = pagerState.currentPage
                val (icons, menuItems) = when (page) {
                    0 -> {
                        val icons = buildList {
                            add(
                                MetroAppBarIcon(
                                    type = MetroSystemIconType.Search,
                                    label = "search",
                                    onClick = { libraryViewModel.search("") },
                                ),
                            )
                            add(
                                MetroAppBarIcon(
                                    type = MetroSystemIconType.Filter,
                                    label = "filter",
                                    onClick = libraryViewModel::showSettingsDialog,
                                ),
                            )
                        }
                        val menus = listOf(
                            MetroAppBarMenuItem(
                                text = stringResource(MR.strings.action_update_library).lowercase(),
                                onClick = {
                                    LibraryUpdateWorker.startNow(context.workManager, null)
                                },
                            ),
                            MetroAppBarMenuItem(
                                text = stringResource(MR.strings.action_update_category).lowercase(),
                                onClick = {
                                    LibraryUpdateWorker.startNow(
                                        context.workManager,
                                        libraryState.activeCategory,
                                    )
                                },
                            ),
                            MetroAppBarMenuItem(
                                text = stringResource(MR.strings.action_open_random_manga).lowercase(),
                                onClick = {
                                    scope.launch {
                                        val random =
                                            libraryViewModel.getRandomLibraryItemForCurrentCategory()
                                        if (random != null) {
                                            navigator.push(
                                                MangaScreen(random.libraryManga.manga.id),
                                            )
                                        }
                                    }
                                },
                            ),
                            MetroAppBarMenuItem("more", onClick = onOpenMore),
                            MetroAppBarMenuItem("downloads", onClick = onOpenDownloads),
                        )
                        icons to menus
                    }
                    1 -> {
                        listOf(
                            MetroAppBarIcon(
                                type = MetroSystemIconType.Filter,
                                label = "filter",
                                onClick = updatesViewModel::showFilterDialog,
                            ),
                            MetroAppBarIcon(
                                type = MetroSystemIconType.Refresh,
                                label = "update",
                                onClick = { updatesViewModel.updateLibrary() },
                            ),
                        ) to listOf(
                            MetroAppBarMenuItem(
                                text = stringResource(MR.strings.action_view_upcoming).lowercase(),
                                onClick = { navigator.push(UpcomingScreen()) },
                            ),
                            MetroAppBarMenuItem("more", onClick = onOpenMore),
                            MetroAppBarMenuItem("downloads", onClick = onOpenDownloads),
                        )
                    }
                    2 -> {
                        val icons = listOf(
                            MetroAppBarIcon(
                                type = MetroSystemIconType.Search,
                                label = "search",
                                onClick = { historyViewModel.updateSearchQuery("") },
                            ),
                            MetroAppBarIcon(
                                type = MetroSystemIconType.Clear,
                                label = "clear",
                                onClick = {
                                    historyViewModel.setDialog(HistoryViewModel.Dialog.DeleteAll)
                                },
                            ),
                        )
                        icons to listOf(
                            MetroAppBarMenuItem("more", onClick = onOpenMore),
                            MetroAppBarMenuItem("downloads", onClick = onOpenDownloads),
                        )
                    }
                    else -> {
                        listOf(
                            MetroAppBarIcon(
                                type = MetroSystemIconType.Search,
                                label = "search",
                                onClick = { navigator.push(GlobalSearchScreen()) },
                            ),
                        ) to listOf(
                            MetroAppBarMenuItem("sources", onClick = {
                                navigator.push(MetronSourcesScreen())
                            }),
                            MetroAppBarMenuItem("extensions", onClick = {
                                navigator.push(MetronExtensionsScreen())
                            }),
                            MetroAppBarMenuItem("migrate", onClick = {
                                navigator.push(MetronMigrateScreen())
                            }),
                            MetroAppBarMenuItem("more", onClick = onOpenMore),
                            MetroAppBarMenuItem("downloads", onClick = onOpenDownloads),
                        )
                    }
                }

                MetroAppBar(
                    icons = icons,
                    menuItems = menuItems,
                    enterKey = page,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { introPlayed = true }
    }
}

@Composable
private fun BrandTitle(pageProgress: Float) {
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .padding(bottom = 4.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        val measurer = rememberTextMeasurer()
        val availableWidthPx = with(density) { (maxWidth - BrandInset).toPx() }
        val fontFamily = MetroTheme.fontFamily
        val brandWidthPx = remember(measurer, density, fontFamily) {
            measurer.measure(
                text = BrandText,
                style = BrandStyle.copy(fontFamily = fontFamily),
                softWrap = false,
                maxLines = 1,
                density = density,
            ).size.width.toFloat()
        }
        val hiddenPx = (brandWidthPx - availableWidthPx).coerceAtLeast(0f)
        val lastPage = (HubTabs.size - 1).coerceAtLeast(1)
        val progress = pageProgress.coerceIn(0f, lastPage.toFloat()) / lastPage
        val brandOffsetPx = (progress * hiddenPx).roundToInt()

        BasicText(
            text = BrandText,
            style = BrandStyle.copy(
                fontFamily = MetroTheme.fontFamily,
                color = MetroTheme.colors.primaryText,
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .offset { IntOffset(-brandOffsetPx, 0) }
                .padding(start = BrandInset)
                .wrapContentWidth(align = Alignment.Start, unbounded = true),
        )
    }
}
