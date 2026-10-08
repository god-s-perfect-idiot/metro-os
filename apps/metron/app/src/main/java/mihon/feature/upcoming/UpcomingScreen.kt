package mihon.feature.upcoming

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.metro.metron.ui.MetronOptionsCard
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroListItem
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

class UpcomingScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val viewModel = metroViewModel<UpcomingViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val filterVisible = state.dialog is UpcomingViewModel.Dialog.FilterSheet

        Box(modifier = Modifier.fillMaxSize()) {
            UpcomingScreenContent(
                state = state,
                setSelectedYearMonth = viewModel::setSelectedYearMonth,
                onClickUpcoming = { navigator.push(MangaScreen(it.id)) },
                hasActiveFilters = state.hasActiveFilters,
                onClickFilter = {
                    if (filterVisible) {
                        viewModel.resetDialog()
                    } else {
                        viewModel.showFilterDialog()
                    }
                },
            )

            MetronOptionsCard(
                visible = filterVisible,
                onDismiss = viewModel::resetDialog,
            ) {
                UpcomingCategoryFilterOptions(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun UpcomingCategoryFilterOptions(
    viewModel: UpcomingViewModel,
) {
    MetroText(
        text = stringResource(MR.strings.pref_filter_upcoming_categories_details).lowercase(),
        style = MetroTextStyle.Body,
        color = MetroTheme.colors.secondaryText,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )

    val allCategories by viewModel.getCategories.subscribe().collectAsState(initial = emptyList())

    if (allCategories.isEmpty()) {
        MetroLoadingDots(modifier = Modifier.padding(16.dp))
        return
    }

    val excluded by viewModel.upcomingPreferences.filterExcludedCategories.collectAsState()
    val included by viewModel.upcomingPreferences.filterIncludedCategories.collectAsState()

    val selected = remember(allCategories, included, excluded) {
        allCategories.map { category ->
            when (category.id) {
                in included -> TriState.ENABLED_IS
                in excluded -> TriState.ENABLED_NOT
                else -> TriState.DISABLED
            }
        }.toMutableStateList()
    }

    allCategories.fastForEachIndexed { idx, category ->
        val filterState = selected.getOrElse(idx) { TriState.DISABLED }
        TriStateOptionRow(
            label = category.visualName,
            state = filterState,
            onClick = {
                selected[idx] = filterState.next()
                viewModel.cycleCategory(category)
            },
        )
    }
}

@Composable
private fun TriStateOptionRow(
    label: String,
    state: TriState,
    onClick: () -> Unit,
) {
    val status = when (state) {
        TriState.DISABLED -> "any"
        TriState.ENABLED_IS -> "include"
        TriState.ENABLED_NOT -> "exclude"
    }
    MetroListItem(
        title = label.lowercase(),
        subtitle = status,
        singleLine = true,
        leading = {
            when (state) {
                TriState.DISABLED -> MetroCheckBox(checked = false, onCheckedChange = null)
                TriState.ENABLED_IS -> MetroCheckBox(checked = true, onCheckedChange = null)
                TriState.ENABLED_NOT -> MetroText(
                    text = "✕",
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.accent,
                )
            }
        },
        oneLineMinHeight = 48.dp,
        twoLineMinHeight = 56.dp,
        verticalPadding = 6.dp,
        onClick = onClick,
    )
}
