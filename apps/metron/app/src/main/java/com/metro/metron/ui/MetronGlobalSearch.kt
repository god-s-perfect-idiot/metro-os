package com.metro.metron.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchViewModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Metro chrome for Mihon global / browse search — text box, tag buttons, source result rows.
 */
@Composable
fun MetronGlobalSearchContent(
    state: SearchViewModel.State,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    hideSourceFilter: Boolean = false,
    fromSourceId: Long? = null,
) {
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        searchFocus.requestFocus()
        keyboard?.show()
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
                    onValueChange = { onChangeSearchQuery(it) },
                    placeholder = stringResource(MR.strings.action_search).lowercase(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            val q = state.searchQuery.orEmpty()
                            if (q.isNotBlank()) onSearch(q)
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                        .focusRequester(searchFocus),
                )
                Spacer(Modifier.height(12.dp))
                MetronSearchTagRow(
                    hideSourceFilter = hideSourceFilter,
                    sourceFilter = state.sourceFilter,
                    onlyShowHasResults = state.onlyShowHasResults,
                    onChangeSearchFilter = onChangeSearchFilter,
                    onToggleResults = onToggleResults,
                )
                if (state.progress in 1..<state.total) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 8.dp)
                            .height(3.dp)
                            .background(MetroTheme.colors.secondarySurface),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(state.progress / state.total.toFloat())
                                .height(3.dp)
                                .background(MetroTheme.colors.accent),
                        )
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                }

                when {
                    state.searchQuery.isNullOrBlank() -> {
                        MetroText(
                            text = stringResource(MR.strings.action_search_hint).lowercase(),
                            style = MetroTextStyle.Body,
                            color = MetroTheme.colors.secondaryText,
                            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
                        )
                    }
                    state.filteredItems.isEmpty() && state.total == 0 -> {
                        MetroEmptyState(message = stringResource(MR.strings.no_results_found))
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            state.filteredItems.forEach { (source, result) ->
                                item(key = source.id) {
                                    MetronGlobalSearchSourceBlock(
                                        title = fromSourceId?.let {
                                            "▶ ${source.name}".takeIf { source.id == fromSourceId }
                                        } ?: source.name,
                                        subtitle = LocaleHelper.getLocalizedDisplayName(source.lang),
                                        result = result,
                                        getManga = getManga,
                                        onClickSource = { onClickSource(source) },
                                        onClickItem = onClickItem,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            MetroAppBar(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun MetronSearchTagRow(
    hideSourceFilter: Boolean,
    sourceFilter: SourceFilter,
    onlyShowHasResults: Boolean,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!hideSourceFilter) {
            MetronTagButton(
                text = stringResource(MR.strings.pinned_sources).lowercase(),
                selected = sourceFilter == SourceFilter.PinnedOnly,
                onClick = { onChangeSearchFilter(SourceFilter.PinnedOnly) },
            )
            MetronTagButton(
                text = stringResource(MR.strings.all).lowercase(),
                selected = sourceFilter == SourceFilter.All,
                onClick = { onChangeSearchFilter(SourceFilter.All) },
            )
        }
        MetronTagButton(
            text = stringResource(MR.strings.has_results).lowercase(),
            selected = onlyShowHasResults,
            onClick = onToggleResults,
        )
    }
}

/** Square Metro tag — border at rest, accent fill when selected. */
@Composable
private fun MetronTagButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = MetroTheme.colors.accent
    val border = if (selected) accent else MetroTheme.colors.primaryText
    val fill = if (selected) accent else Color.Transparent
    val label = if (selected) {
        MetroColors.tileContentColor(accent)
    } else {
        MetroTheme.colors.primaryText
    }
    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .background(fill, RectangleShape)
            .border(2.dp, border, RectangleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        MetroText(
            text = text,
            style = MetroTextStyle.Body,
            color = label,
            maxLines = 1,
        )
    }
}

@Composable
private fun MetronGlobalSearchSourceBlock(
    title: String,
    subtitle: String,
    result: SearchItemResult,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: () -> Unit,
    onClickItem: (Manga) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .metroClickable(onClick = onClickSource)
                .padding(
                    horizontal = MetroDimens.ScreenHorizontalMargin,
                    vertical = 10.dp,
                ),
        ) {
            MetroText(
                text = title,
                style = MetroTextStyle.ListItemTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MetroText(
                text = subtitle,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                maxLines = 1,
            )
        }
        when (result) {
            SearchItemResult.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MetroLoadingDots()
                }
            }
            is SearchItemResult.Error -> {
                MetroText(
                    text = result.throwable.message
                        ?: stringResource(MR.strings.unknown_error),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(
                        horizontal = MetroDimens.ScreenHorizontalMargin,
                        vertical = 8.dp,
                    ),
                )
            }
            is SearchItemResult.Success -> {
                if (result.result.isEmpty()) {
                    MetroText(
                        text = stringResource(MR.strings.no_results_found),
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.padding(
                            horizontal = MetroDimens.ScreenHorizontalMargin,
                            vertical = 8.dp,
                        ),
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(
                            horizontal = MetroDimens.ScreenHorizontalMargin,
                            vertical = 4.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(result.result, key = { it.id }) { manga ->
                            val resolved by getManga(manga)
                            MetronSearchCoverTile(
                                title = resolved.title,
                                coverModel = resolved.asMangaCover(),
                                onClick = { onClickItem(resolved) },
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MetronSearchCoverTile(
    title: String,
    coverModel: Any,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .metroClickable(onClick = onClick),
    ) {
        AsyncImage(
            model = coverModel,
            contentDescription = null,
            placeholder = ColorPainter(Color(0x1F888888)),
            error = ColorPainter(Color(0x1F888888)),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .background(MetroTheme.colors.secondarySurface, RectangleShape),
        )
        Spacer(Modifier.height(4.dp))
        MetroText(
            text = title,
            style = MetroTextStyle.ListItemSubtitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
