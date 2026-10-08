package com.metro.metron.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroListItem
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroPageHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.presentation.browse.SourceOptionsDialog
import eu.kanade.presentation.browse.SourceUiModel
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionFilterScreen
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsScreen
import eu.kanade.tachiyomi.ui.browse.migration.manga.MigrateMangaScreen
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrateSourceViewModel
import eu.kanade.tachiyomi.ui.browse.source.SourcesFilterScreen
import eu.kanade.tachiyomi.ui.browse.source.SourcesViewModel
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceViewModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private val HubAccentTileInset = 8.dp
private const val HubAccentTileWidthScale = 0.88f

@Composable
fun MetronBrowsePane(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(top = 24.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tileSize = ((maxWidth - 8.dp) / 2) * HubAccentTileWidthScale
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetronBrowseAccentTile(
                        title = "sources",
                        iconRes = R.drawable.metron_sources,
                        onClick = { navigator.push(MetronSourcesScreen()) },
                        modifier = Modifier.size(tileSize),
                    )
                    MetronBrowseAccentTile(
                        title = "extensions",
                        iconRes = R.drawable.metron_extension,
                        onClick = { navigator.push(MetronExtensionsScreen()) },
                        modifier = Modifier.size(tileSize),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetronBrowseAccentTile(
                        title = "migrate",
                        iconRes = R.drawable.metron_migrate,
                        onClick = { navigator.push(MetronMigrateScreen()) },
                        modifier = Modifier.size(tileSize),
                    )
                    MetronBrowseAccentTile(
                        title = "settings",
                        iconRes = R.drawable.metron_settings,
                        onClick = { navigator.push(SettingsScreen()) },
                        iconSize = 52.dp,
                        modifier = Modifier.size(tileSize),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetronBrowseAccentTile(
    title: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 60.dp,
) {
    val background = MetroTheme.colors.accent
    val content = MetroColors.tileContentColor(background)
    Box(
        modifier = modifier
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = title }
            .padding(HubAccentTileInset),
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.Center),
            colorFilter = ColorFilter.tint(content),
        )
        MetroText(
            text = title,
            style = MetroTextStyle.ListItemTitle,
            color = content,
            maxLines = 1,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

class MetronSourcesScreen : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val viewModel = metroViewModel<SourcesViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

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
                    MetroPageHeader(title = "sources")
                    when {
                        state.isLoading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                MetroLoadingDots()
                            }
                        }
                        state.isEmpty -> {
                            MetroEmptyState(message = "no sources yet")
                        }
                        else -> {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(
                                    items = state.items,
                                    key = {
                                        when (it) {
                                            is SourceUiModel.Header -> "h-${it.language}"
                                            is SourceUiModel.Item -> it.source.key()
                                        }
                                    },
                                ) { model ->
                                    when (model) {
                                        is SourceUiModel.Header -> {
                                            MetroText(
                                                text = LocaleHelper
                                                    .getSourceDisplayName(model.language, context)
                                                    .lowercase(),
                                                style = MetroTextStyle.SectionHeader,
                                                color = MetroTheme.colors.accent,
                                                modifier = Modifier.padding(
                                                    horizontal = MetroDimens.ScreenHorizontalMargin,
                                                    vertical = 12.dp,
                                                ),
                                            )
                                        }
                                        is SourceUiModel.Item -> {
                                            val source = model.source
                                            MetroListItem(
                                                title = source.name,
                                                subtitle = source.lang.uppercase()
                                                    .takeIf { it.isNotEmpty() },
                                                singleLine = true,
                                                onClick = {
                                                    navigator.push(
                                                        BrowseSourceScreen(
                                                            source.id,
                                                            BrowseSourceViewModel.Listing.Popular.query,
                                                        ),
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
                MetroAppBar(
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Search,
                            label = "search",
                            onClick = { navigator.push(GlobalSearchScreen()) },
                        ),
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Filter,
                            label = "filter",
                            onClick = { navigator.push(SourcesFilterScreen()) },
                        ),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        state.dialog?.let { dialog ->
            val source = dialog.source
            SourceOptionsDialog(
                source = source,
                onClickPin = {
                    viewModel.togglePin(source)
                    viewModel.closeDialog()
                },
                onClickDisable = {
                    viewModel.toggleSource(source)
                    viewModel.closeDialog()
                },
                onDismiss = viewModel::closeDialog,
            )
        }
    }
}

class MetronExtensionsScreen : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<ExtensionsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

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
                    MetroPageHeader(title = "extensions")
                    if (state.searchQuery != null) {
                        MetroTextBox(
                            value = state.searchQuery.orEmpty(),
                            onValueChange = viewModel::search,
                            placeholder = stringResource(MR.strings.action_search).lowercase(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = MetroDimens.ScreenHorizontalMargin,
                                    vertical = 8.dp,
                                ),
                        )
                    }
                    when {
                        state.isLoading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                MetroLoadingDots()
                            }
                        }
                        state.isEmpty -> {
                            MetroEmptyState(message = "no extensions")
                        }
                        else -> {
                            val rows = remember(state.items) {
                                state.items.flatMap { (header, items) ->
                                    listOf<Any>(header) + items
                                }
                            }
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(
                                    items = rows,
                                    key = { row ->
                                        when (row) {
                                            is ExtensionUiModel.Header.Resource -> "hr-${row.textRes}"
                                            is ExtensionUiModel.Header.Text -> "ht-${row.text}"
                                            is ExtensionUiModel.Item -> row.extension.pkgName
                                            else -> row.hashCode()
                                        }
                                    },
                                ) { row ->
                                    when (row) {
                                        is ExtensionUiModel.Header.Resource -> {
                                            MetroText(
                                                text = stringResource(row.textRes).lowercase(),
                                                style = MetroTextStyle.SectionHeader,
                                                color = MetroTheme.colors.accent,
                                                modifier = Modifier.padding(
                                                    horizontal = MetroDimens.ScreenHorizontalMargin,
                                                    vertical = 12.dp,
                                                ),
                                            )
                                        }
                                        is ExtensionUiModel.Header.Text -> {
                                            MetroText(
                                                text = row.text.lowercase(),
                                                style = MetroTextStyle.SectionHeader,
                                                color = MetroTheme.colors.accent,
                                                modifier = Modifier.padding(
                                                    horizontal = MetroDimens.ScreenHorizontalMargin,
                                                    vertical = 12.dp,
                                                ),
                                            )
                                        }
                                        is ExtensionUiModel.Item -> {
                                            val ext = row.extension
                                            MetroListItem(
                                                title = ext.name,
                                                subtitle = buildExtensionSubtitle(ext),
                                                singleLine = true,
                                                onClick = {
                                                    when (ext) {
                                                        is Extension.Installed -> {
                                                            navigator.push(
                                                                ExtensionDetailsScreen(ext.pkgName),
                                                            )
                                                        }
                                                        is Extension.Available -> {
                                                            viewModel.installExtension(ext)
                                                        }
                                                        else -> Unit
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                MetroAppBar(
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Search,
                            label = "search",
                            onClick = { viewModel.search("") },
                        ),
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Refresh,
                            label = "refresh",
                            onClick = viewModel::findAvailableExtensions,
                        ),
                    ),
                    menuItems = listOf(
                        MetroAppBarMenuItem(
                            text = stringResource(MR.strings.action_filter).lowercase(),
                            onClick = { navigator.push(ExtensionFilterScreen()) },
                        ),
                        MetroAppBarMenuItem(
                            text = stringResource(MR.strings.extensionStores).lowercase(),
                            onClick = { navigator.push(ExtensionStoresScreen()) },
                        ),
                        MetroAppBarMenuItem(
                            text = stringResource(MR.strings.ext_update_all).lowercase(),
                            onClick = viewModel::updateAllExtensions,
                        ),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

private fun buildExtensionSubtitle(ext: Extension): String {
    val lang = ext.lang?.uppercase().orEmpty()
    return listOfNotNull(
        lang.takeIf { it.isNotEmpty() },
        ext.versionName,
    ).joinToString(" · ")
}

class MetronMigrateScreen : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current
        val viewModel = metroViewModel<MigrateSourceViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

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
                    MetroPageHeader(title = "migrate")
                    when {
                        state.isLoading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                MetroLoadingDots()
                            }
                        }
                        state.isEmpty -> {
                            MetroEmptyState(
                                message = stringResource(MR.strings.information_empty_library),
                            )
                        }
                        else -> {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(
                                    items = state.items,
                                    key = { it.first.id },
                                ) { (source, count) ->
                                    MetroListItem(
                                        title = source.name,
                                        subtitle = "$count",
                                        singleLine = true,
                                        onClick = {
                                            navigator.push(MigrateMangaScreen(source.id))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                MetroAppBar(
                    menuItems = listOf(
                        MetroAppBarMenuItem(
                            text = stringResource(MR.strings.migration_help_guide).lowercase(),
                            onClick = {
                                uriHandler.openUri(
                                    "https://mihon.app/docs/guides/source-migration",
                                )
                            },
                        ),
                        MetroAppBarMenuItem(
                            text = when (state.sortingMode) {
                                SetMigrateSorting.Mode.ALPHABETICAL -> "sort by count"
                                SetMigrateSorting.Mode.TOTAL -> "sort by name"
                            },
                            onClick = viewModel::toggleSortingMode,
                        ),
                        MetroAppBarMenuItem(
                            text = when (state.sortingDirection) {
                                SetMigrateSorting.Direction.ASCENDING -> "sort descending"
                                SetMigrateSorting.Direction.DESCENDING -> "sort ascending"
                            },
                            onClick = viewModel::toggleSortingDirection,
                        ),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
