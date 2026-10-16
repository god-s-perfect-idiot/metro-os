package eu.kanade.presentation.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.metro.metron.ui.MetronGlobalSearchContent
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchViewModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import tachiyomi.domain.manga.model.Manga

@Composable
fun MigrateSearchScreen(
    state: SearchViewModel.State,
    fromSourceId: Long?,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedNavigateUp = navigateUp
    @Suppress("UNUSED_PARAMETER")
    val unusedLongClick = onLongClickItem

    MetronGlobalSearchContent(
        state = state,
        fromSourceId = fromSourceId,
        hideSourceFilter = true,
        onChangeSearchQuery = onChangeSearchQuery,
        onSearch = onSearch,
        onChangeSearchFilter = onChangeSearchFilter,
        onToggleResults = onToggleResults,
        getManga = getManga,
        onClickSource = onClickSource,
        onClickItem = onClickItem,
    )
}
