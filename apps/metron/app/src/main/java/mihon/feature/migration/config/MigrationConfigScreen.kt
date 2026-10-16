package mihon.feature.migration.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroMultiSelectRow
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateSearchScreen
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mihon.feature.migration.list.MigrationListScreen
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

class MigrationConfigScreen(private val mangaIds: Collection<Long>) : Screen() {

    constructor(mangaId: Long) : this(listOf(mangaId))

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val viewModel = metroViewModel<Model>()
        val state by viewModel.state.collectAsState()

        var migrationSheetOpen by rememberSaveable { mutableStateOf(false) }

        fun continueMigration(openSheet: Boolean, extraSearchQuery: String?) {
            val mangaId = mangaIds.singleOrNull()
            if (mangaId == null && openSheet) {
                migrationSheetOpen = true
                return
            }
            val screen = if (mangaId == null) {
                MigrationListScreen(mangaIds, extraSearchQuery)
            } else {
                MigrateSearchScreen(mangaId)
            }
            navigator.replace(screen)
        }

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

        val (selectedSources, availableSources) = state.sources.partition { it.isSelected }
        val showLanguage by remember(state) {
            derivedStateOf {
                state.sources.distinctBy { it.source.lang }.size > 1
            }
        }

        val lazyListState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            val fromIndex = selectedSources.indexOfFirst { it.id == from.key }
            val toIndex = selectedSources.indexOfFirst { it.id == to.key }
            if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
            viewModel.orderSource(fromIndex, toIndex)
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
                    MetroSettingsHeader(
                        pageTitle = stringResource(MR.strings.label_migration).lowercase(),
                        appTitle = "metron",
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = lazyListState,
                        contentPadding = PaddingValues(bottom = 32.dp),
                    ) {
                        listOf(selectedSources, availableSources).fastForEachIndexed { listIndex, sources ->
                            val selectedSourceList = listIndex == 0
                            if (sources.isNotEmpty()) {
                                val headerPrefix = if (selectedSourceList) "selected" else "available"
                                item("$headerPrefix-header") {
                                    MetroText(
                                        text = stringResource(
                                            resource = if (selectedSourceList) {
                                                MR.strings.migrationConfigScreen_selectedHeader
                                            } else {
                                                MR.strings.migrationConfigScreen_availableHeader
                                            },
                                        ).lowercase(),
                                        style = MetroTextStyle.SectionHeader,
                                        color = MetroTheme.colors.accent,
                                        modifier = Modifier
                                            .padding(
                                                start = 12.dp,
                                                top = 12.dp,
                                                bottom = 4.dp,
                                            )
                                            .animateItem(),
                                    )
                                }
                            }
                            itemsIndexed(
                                items = sources,
                                key = { _, item -> item.id },
                            ) { _, item ->
                                SourceItemContainer(
                                    source = item,
                                    showLanguage = showLanguage,
                                    dragEnabled = selectedSourceList && sources.size > 1,
                                    state = reorderableState,
                                    key = { if (selectedSourceList) it.id else "available-${it.id}" },
                                    onClick = { viewModel.toggleSelection(item.id) },
                                )
                            }
                        }
                    }
                }

                MetroAppBar(
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Forward,
                            label = stringResource(
                                MR.strings.migrationConfigScreen_continueButtonText,
                            ).lowercase(),
                            onClick = {
                                viewModel.saveSources()
                                continueMigration(openSheet = true, extraSearchQuery = null)
                            },
                        ),
                        MetroAppBarIcon(
                            type = MetroSystemIconType.SelectAll,
                            label = stringResource(
                                MR.strings.migrationConfigScreen_selectAllLabel,
                            ).lowercase(),
                            onClick = { viewModel.toggleSelection(Model.SelectionConfig.All) },
                        ),
                    ),
                    menuItems = listOf(
                        MetroAppBarMenuItem(
                            text = stringResource(
                                MR.strings.migrationConfigScreen_selectNoneLabel,
                            ).lowercase(),
                            onClick = { viewModel.toggleSelection(Model.SelectionConfig.None) },
                        ),
                        MetroAppBarMenuItem(
                            text = stringResource(
                                MR.strings.migrationConfigScreen_selectEnabledLabel,
                            ).lowercase(),
                            onClick = { viewModel.toggleSelection(Model.SelectionConfig.Enabled) },
                        ),
                        MetroAppBarMenuItem(
                            text = stringResource(
                                MR.strings.migrationConfigScreen_selectPinnedLabel,
                            ).lowercase(),
                            onClick = { viewModel.toggleSelection(Model.SelectionConfig.Pinned) },
                        ),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        if (migrationSheetOpen) {
            MigrationConfigScreenSheet(
                preferences = viewModel.sourcePreferences,
                onDismissRequest = { migrationSheetOpen = false },
                onStartMigration = { extraSearchQuery ->
                    migrationSheetOpen = false
                    continueMigration(openSheet = false, extraSearchQuery = extraSearchQuery)
                },
            )
        }
    }

    @Composable
    private fun LazyItemScope.SourceItemContainer(
        source: MigrationSource,
        showLanguage: Boolean,
        dragEnabled: Boolean,
        state: ReorderableLazyListState,
        key: (MigrationSource) -> Any,
        onClick: () -> Unit,
    ) {
        val title = if (showLanguage) {
            "${source.name} (${LocaleHelper.getShortDisplayName(source.shortLanguage, uppercase = true)})"
        } else {
            source.name
        }
        ReorderableItem(
            state = state,
            key = key(source),
            enabled = dragEnabled,
        ) { _ ->
            Box(modifier = Modifier.animateItem()) {
                MetroMultiSelectRow(
                    title = title,
                    checked = source.isSelected,
                    onClick = onClick,
                    leading = {
                        SourceIcon(source = source.source, modifier = Modifier.size(40.dp))
                    },
                )
                if (dragEnabled) {
                    MetroCircleIconButton(
                        type = MetroSystemIconType.More,
                        contentDescription = "reorder",
                        onClick = {},
                        glyphSize = 28.dp,
                        modifier = with(this@ReorderableItem) {
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                                .draggableHandle()
                        },
                    )
                }
            }
        }
    }

    @Inject
    @ViewModelKey
    @ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
    class Model(
        val sourcePreferences: SourcePreferences,
        private val sourceManager: SourceManager,
    ) : ViewModel() {

        val state: StateFlow<Model.State>
            field = MutableStateFlow<Model.State>(State())

        private val sourcesComparator = { includedSources: List<Long> ->
            compareBy<MigrationSource>(
                { !it.isSelected },
                { includedSources.indexOf(it.id) },
                { with(it) { "$name ($shortLanguage)" } },
            )
        }

        init {
            viewModelScope.launchIO {
                initSources()
                state.update { it.copy(isLoading = false) }
            }
        }

        private fun updateSources(action: (List<MigrationSource>) -> List<MigrationSource>) {
            state.update { state ->
                val updatedSources = action(state.sources)
                val includedSources = updatedSources.mapNotNull { if (!it.isSelected) null else it.id }
                state.copy(sources = updatedSources.sortedWith(sourcesComparator(includedSources)))
            }
            saveSources()
        }

        private suspend fun initSources() {
            val languages = sourcePreferences.enabledLanguages.get()
            val pinnedSources = sourcePreferences.pinnedSources.get().mapNotNull { it.toLongOrNull() }
            val includedSources = sourcePreferences.migrationSources.get()
            val disabledSources = sourcePreferences.disabledSources.get()
                .mapNotNull { it.toLongOrNull() }
            val sources = sourceManager.getAll()
                .asSequence()
                .filterIsInstance<HttpSource>()
                .filter { it.lang in languages }
                .map {
                    val source = Source(
                        id = it.id,
                        lang = it.lang,
                        name = it.name,
                        supportsLatest = false,
                        isStub = false,
                    )
                    MigrationSource(
                        source = source,
                        isSelected = when {
                            includedSources.isNotEmpty() -> source.id in includedSources
                            pinnedSources.isNotEmpty() -> source.id in pinnedSources
                            else -> source.id !in disabledSources
                        },
                    )
                }
                .toList()

            state.update { state ->
                state.copy(sources = sources.sortedWith(sourcesComparator(includedSources)))
            }
        }

        fun toggleSelection(id: Long) {
            updateSources { sources ->
                sources.map { source ->
                    source.copy(isSelected = if (source.source.id == id) !source.isSelected else source.isSelected)
                }
            }
        }

        fun toggleSelection(config: SelectionConfig) {
            val pinnedSources = sourcePreferences.pinnedSources.get().mapNotNull { it.toLongOrNull() }
            val disabledSources = sourcePreferences.disabledSources.get().mapNotNull { it.toLongOrNull() }
            val isSelected: (Long) -> Boolean = {
                when (config) {
                    SelectionConfig.All -> true
                    SelectionConfig.None -> false
                    SelectionConfig.Pinned -> it in pinnedSources
                    SelectionConfig.Enabled -> it !in disabledSources
                }
            }
            updateSources { sources ->
                sources.map { source ->
                    source.copy(isSelected = isSelected(source.source.id))
                }
            }
        }

        fun orderSource(from: Int, to: Int) {
            updateSources {
                it.toMutableList()
                    .apply {
                        add(to, removeAt(from))
                    }
                    .toList()
            }
        }

        fun saveSources() {
            state.value.sources
                .filter { source -> source.isSelected }
                .map { source -> source.source.id }
                .let { sources -> sourcePreferences.migrationSources.set(sources) }
        }

        data class State(
            val isLoading: Boolean = true,
            val sources: List<MigrationSource> = emptyList(),
        )

        enum class SelectionConfig {
            All,
            None,
            Pinned,
            Enabled,
        }
    }

    data class MigrationSource(
        val source: Source,
        val isSelected: Boolean,
    ) {
        val id: Long
            inline get() = source.id

        val name: String
            inline get() = source.name

        val shortLanguage: String = LocaleHelper.getShortDisplayName(source.lang)
    }
}
