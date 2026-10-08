package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroCheckBoxDefaults
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroMultiSelectDefaults
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroNavBarPadding
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SourceFilterDialog(
    onDismissRequest: () -> Unit,
    filters: FilterList,
    onReset: () -> Unit,
    onFilter: () -> Unit,
    onUpdate: (FilterList) -> Unit,
) {
    val updateFilters = { onUpdate(filters) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        MetroSystemTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .metroNavBarPadding()
                    .background(MetroTheme.colors.background),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MetroSettingsHeader(
                        pageTitle = stringResource(MR.strings.action_filter).lowercase(),
                        appTitle = "metron",
                    )
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            bottom = MetroAppBarDefaults.BarHeight + 24.dp,
                        ),
                    ) {
                        itemsIndexed(
                            items = filters,
                            key = { index, filter -> "${index}-${filter.name}" },
                        ) { _, filter ->
                            FilterItem(filter = filter, onUpdate = updateFilters)
                        }
                    }
                }

                MetroAppBar(
                    icons = listOf(
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Check,
                            label = stringResource(MR.strings.action_filter).lowercase(),
                            onClick = {
                                onFilter()
                                onDismissRequest()
                            },
                        ),
                        MetroAppBarIcon(
                            type = MetroSystemIconType.Close,
                            label = stringResource(MR.strings.action_cancel).lowercase(),
                            onClick = onDismissRequest,
                        ),
                    ),
                    menuItems = listOf(
                        MetroAppBarMenuItem(
                            text = stringResource(MR.strings.action_reset).lowercase(),
                            onClick = onReset,
                        ),
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun FilterItem(filter: Filter<*>, onUpdate: () -> Unit) {
    when (filter) {
        is Filter.Header -> {
            MetroText(
                text = filter.name.lowercase(),
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent,
                modifier = Modifier.padding(
                    horizontal = MetroDimens.ScreenHorizontalMargin,
                    vertical = 12.dp,
                ),
            )
        }
        is Filter.Separator -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 8.dp)
                    .height(1.dp)
                    .background(MetroTheme.colors.secondarySurface),
            )
        }
        is Filter.CheckBox -> {
            FilterCheckRow(
                label = filter.name.lowercase(),
                checked = filter.state,
                onClick = {
                    filter.state = !filter.state
                    onUpdate()
                },
            )
        }
        is Filter.TriState -> {
            val tri = filter.state.toTriStateFilter()
            FilterTriStateRow(
                label = filter.name.lowercase(),
                state = tri,
                onClick = {
                    filter.state = tri.next().toTriStateInt()
                    onUpdate()
                },
            )
        }
        is Filter.Text -> {
            MetroTextBox(
                value = filter.state,
                onValueChange = {
                    filter.state = it
                    onUpdate()
                },
                placeholder = filter.name.lowercase(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MetroDimens.ScreenHorizontalMargin,
                        vertical = 8.dp,
                    ),
            )
        }
        is Filter.Select<*> -> {
            MetroListPicker(
                options = filter.values.map { it.toString().lowercase() },
                selectedOptionIndex = filter.state,
                onSelectOption = {
                    filter.state = it
                    onUpdate()
                },
                label = filter.name.lowercase(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MetroDimens.ScreenHorizontalMargin,
                        vertical = 8.dp,
                    ),
            )
        }
        is Filter.Sort -> {
            var expanded by remember(filter.name) { mutableStateOf(true) }
            FilterCollapsible(
                heading = filter.name.lowercase(),
                expanded = expanded,
                onToggle = { expanded = !expanded },
            ) {
                filter.values.forEachIndexed { index, item ->
                    val selected = filter.state?.index == index
                    val ascending = filter.state?.ascending?.takeIf { selected }
                    FilterSortRow(
                        label = item.lowercase(),
                        selected = selected,
                        ascending = ascending,
                        onClick = {
                            val nextAscending = if (selected) {
                                !(filter.state?.ascending ?: true)
                            } else {
                                filter.state?.ascending ?: true
                            }
                            filter.state = Filter.Sort.Selection(
                                index = index,
                                ascending = nextAscending,
                            )
                            onUpdate()
                        },
                    )
                }
            }
        }
        is Filter.Group<*> -> {
            var expanded by remember(filter.name) { mutableStateOf(true) }
            FilterCollapsible(
                heading = filter.name.lowercase(),
                expanded = expanded,
                onToggle = { expanded = !expanded },
            ) {
                filter.state
                    .filterIsInstance<Filter<*>>()
                    .forEach { FilterItem(filter = it, onUpdate = onUpdate) }
            }
        }
    }
}

@Composable
private fun FilterCollapsible(
    heading: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .metroClickable(onClick = onToggle)
                .padding(
                    horizontal = MetroDimens.ScreenHorizontalMargin,
                    vertical = 10.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MetroText(
                text = heading,
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent,
                modifier = Modifier.weight(1f),
            )
            MetroText(
                text = if (expanded) "−" else "+",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
            )
        }
        if (expanded) {
            content()
        }
    }
}

@Composable
private fun FilterCheckRow(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MetroMultiSelectDefaults.RowMinHeight)
            .metroClickable(onClick = onClick)
            .padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = MetroMultiSelectDefaults.RowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroCheckBox(
            checked = checked,
            onCheckedChange = null,
            size = MetroMultiSelectDefaults.CheckboxSize,
        )
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemTitle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(start = MetroMultiSelectDefaults.LeadingToTitleGap),
        )
    }
}

@Composable
private fun FilterTriStateRow(
    label: String,
    state: TriState,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MetroMultiSelectDefaults.RowMinHeight)
            .metroClickable(onClick = onClick)
            .padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = MetroMultiSelectDefaults.RowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (state) {
            TriState.DISABLED -> MetroCheckBox(
                checked = false,
                onCheckedChange = null,
                size = MetroMultiSelectDefaults.CheckboxSize,
            )
            TriState.ENABLED_IS -> MetroCheckBox(
                checked = true,
                onCheckedChange = null,
                size = MetroMultiSelectDefaults.CheckboxSize,
            )
            TriState.ENABLED_NOT -> ExcludeCheckBox(size = MetroMultiSelectDefaults.CheckboxSize)
        }
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemTitle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(start = MetroMultiSelectDefaults.LeadingToTitleGap),
        )
    }
}

@Composable
private fun FilterSortRow(
    label: String,
    selected: Boolean,
    ascending: Boolean?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MetroMultiSelectDefaults.RowMinHeight)
            .metroClickable(onClick = onClick)
            .padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = MetroMultiSelectDefaults.RowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemTitle,
            color = if (selected) MetroTheme.colors.accent else MetroTheme.colors.primaryText,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.weight(1f),
        )
        if (ascending != null) {
            MetroText(
                text = if (ascending) "↑" else "↓",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.accent,
            )
        }
    }
}

@Composable
private fun ExcludeCheckBox(size: Dp) {
    val accent = MetroTheme.colors.accent
    val glyph = MetroColors.DarkPrimaryText
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size),
    ) {
        Canvas(modifier = Modifier.size(size)) {
            drawRect(color = accent)
            val inset = this.size.minDimension * 0.28f
            val stroke = MetroCheckBoxDefaults.BorderWidth.toPx()
            drawLine(
                color = glyph,
                start = Offset(inset, inset),
                end = Offset(this.size.width - inset, this.size.height - inset),
                strokeWidth = stroke,
            )
            drawLine(
                color = glyph,
                start = Offset(this.size.width - inset, inset),
                end = Offset(inset, this.size.height - inset),
                strokeWidth = stroke,
            )
        }
    }
}

private fun Int.toTriStateFilter(): TriState {
    return when (this) {
        Filter.TriState.STATE_IGNORE -> TriState.DISABLED
        Filter.TriState.STATE_INCLUDE -> TriState.ENABLED_IS
        Filter.TriState.STATE_EXCLUDE -> TriState.ENABLED_NOT
        else -> throw IllegalStateException("Unknown TriState state: $this")
    }
}

private fun TriState.toTriStateInt(): Int {
    return when (this) {
        TriState.DISABLED -> Filter.TriState.STATE_IGNORE
        TriState.ENABLED_IS -> Filter.TriState.STATE_INCLUDE
        TriState.ENABLED_NOT -> Filter.TriState.STATE_EXCLUDE
    }
}
