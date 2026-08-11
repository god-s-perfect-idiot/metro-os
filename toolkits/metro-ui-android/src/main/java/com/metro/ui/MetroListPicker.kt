package com.metro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * One option in a [MetroListPicker].
 */
data class MetroListPickerOption<T>(
    val value: T,
    val label: String,
)

private val BorderWidth = 2.dp
private val CollapsedMinHeight = 44.dp
private val OptionVerticalPadding = 10.dp
private val OptionHorizontalPadding = 12.dp
private val MaxExpandedVisibleOptions = 6
private val ErrorBorder = Color(0xFFE51400)
private val ExpandCollapseMs = MetroTransitions.PivotSwitchMs

/**
 * WP8.1 ListPicker — labelled bordered field that expands inline to an inverted options panel
 * (METRO-UX-LANGUAGE §6.19).
 *
 * Collapsed: secondary [label] above a square-bordered box showing the selected value (no chevron).
 * Expanded: solid inverted panel; selected option uses accent, others use inverted primary text.
 * Expand/collapse animates height (250ms ease-in-out).
 *
 * Pass [onOpen] with an empty [options] list for drill-in pickers (e.g. full locale list) that keep
 * the collapsed chrome but navigate elsewhere instead of expanding.
 */
@Composable
fun <T> MetroListPicker(
    selected: T?,
    options: List<MetroListPickerOption<T>>,
    onSelectedChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    placeholder: String = "",
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
    labelStyle: MetroTextStyle = MetroTextStyle.ListItemSubtitle,
    optionStyle: MetroTextStyle = MetroTextStyle.Body,
) {
    val selectedLabel = options.find { it.value == selected }?.label
        ?: placeholder.takeIf { it.isNotEmpty() }
        ?: selected?.toString().orEmpty()

    MetroListPickerChrome(
        label = label,
        valueLabel = selectedLabel,
        options = options.map { it.label to (it.value == selected) },
        onSelectIndex = { index ->
            onSelectedChange(options[index].value)
        },
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        onOpen = onOpen,
        canExpand = options.isNotEmpty() && onOpen == null,
        labelStyle = labelStyle,
        optionStyle = optionStyle,
    )
}

/**
 * Index-based ListPicker for callers that already track a selected index (e.g. JetPref ports).
 */
@Composable
fun MetroListPicker(
    options: List<String>,
    selectedOptionIndex: Int,
    onSelectOption: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    placeholder: String = "",
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
    labelStyle: MetroTextStyle = MetroTextStyle.ListItemSubtitle,
    optionStyle: MetroTextStyle = MetroTextStyle.Body,
) {
    val safeIndex = selectedOptionIndex.coerceIn(
        minimumValue = if (options.isEmpty()) 0 else 0,
        maximumValue = (options.size - 1).coerceAtLeast(0),
    )
    val valueLabel = when {
        options.isEmpty() -> placeholder
        selectedOptionIndex !in options.indices -> placeholder.ifEmpty { options.firstOrNull().orEmpty() }
        else -> options[safeIndex]
    }

    MetroListPickerChrome(
        label = label,
        valueLabel = valueLabel,
        options = options.mapIndexed { index, text -> text to (index == selectedOptionIndex) },
        onSelectIndex = onSelectOption,
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        onOpen = onOpen,
        canExpand = options.isNotEmpty() && onOpen == null,
        labelStyle = labelStyle,
        optionStyle = optionStyle,
    )
}

@Composable
private fun MetroListPickerChrome(
    label: String?,
    valueLabel: String,
    options: List<Pair<String, Boolean>>,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    isError: Boolean,
    expanded: Boolean?,
    onExpandedChange: ((Boolean) -> Unit)?,
    onOpen: (() -> Unit)?,
    canExpand: Boolean,
    labelStyle: MetroTextStyle,
    optionStyle: MetroTextStyle,
) {
    var internalExpanded by remember { mutableStateOf(false) }
    val isExpanded = expanded ?: internalExpanded
    fun setExpanded(value: Boolean) {
        if (onExpandedChange != null) {
            onExpandedChange(value)
        } else {
            internalExpanded = value
        }
    }

    val foreground = MetroTheme.colors.primaryText
    val secondary = MetroTheme.colors.secondaryText
    val accent = MetroTheme.colors.accent
    val alpha = if (enabled) 1f else 0.4f
    val borderColor = when {
        isError -> ErrorBorder
        else -> foreground.copy(alpha = foreground.alpha * alpha)
    }
    // Inverted panel: white on dark, black on light (WP ListPicker expanded chrome).
    val panelBackground = foreground
    val panelUnselectedText = MetroTheme.colors.background

    if (isExpanded && canExpand) {
        BackHandler { setExpanded(false) }
    }

    val heightSpec = tween<IntSize>(
        durationMillis = ExpandCollapseMs,
        easing = MetroTransitions.PivotEasing,
    )

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            MetroText(
                text = label,
                style = labelStyle,
                color = secondary.copy(alpha = secondary.alpha * alpha),
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        AnimatedContent(
            targetState = isExpanded && canExpand,
            transitionSpec = {
                val vertical = tween<IntSize>(
                    durationMillis = ExpandCollapseMs,
                    easing = MetroTransitions.PivotEasing,
                )
                (fadeIn(animationSpec = tween(ExpandCollapseMs / 2)) +
                    expandVertically(
                        animationSpec = vertical,
                        expandFrom = Alignment.Top,
                    ))
                    .togetherWith(
                        fadeOut(animationSpec = tween(ExpandCollapseMs / 2)) +
                            shrinkVertically(
                                animationSpec = vertical,
                                shrinkTowards = Alignment.Top,
                            ),
                    )
                    .using(SizeTransform(clip = true) { _, _ -> heightSpec })
            },
            label = "metroListPickerExpand",
            modifier = Modifier.fillMaxWidth(),
        ) { showExpanded ->
            if (showExpanded) {
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panelBackground, RectangleShape)
                        .heightIn(max = CollapsedMinHeight * MaxExpandedVisibleOptions)
                        .verticalScroll(scroll),
                ) {
                    options.forEachIndexed { index, (text, selected) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = enabled) {
                                    onSelectIndex(index)
                                    setExpanded(false)
                                }
                                .padding(
                                    horizontal = OptionHorizontalPadding,
                                    vertical = OptionVerticalPadding,
                                ),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            MetroText(
                                text = text,
                                style = optionStyle,
                                color = if (selected) accent else panelUnselectedText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = CollapsedMinHeight)
                        .border(BorderWidth, borderColor, RectangleShape)
                        .clickable(enabled = enabled) {
                            when {
                                onOpen != null -> onOpen()
                                canExpand -> setExpanded(true)
                            }
                        }
                        .padding(
                            horizontal = OptionHorizontalPadding,
                            vertical = OptionVerticalPadding,
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    MetroText(
                        text = valueLabel,
                        style = optionStyle,
                        color = foreground.copy(alpha = foreground.alpha * alpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroListPickerCollapsedDarkPreview() {
    MetroTheme(darkTheme = true) {
        MetroListPicker(
            label = "Background",
            options = listOf("dark", "light"),
            selectedOptionIndex = 0,
            onSelectOption = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroListPickerExpandedDarkPreview() {
    MetroTheme(darkTheme = true) {
        MetroListPicker(
            label = "Background",
            options = listOf("dark", "light"),
            selectedOptionIndex = 0,
            onSelectOption = {},
            expanded = true,
            onExpandedChange = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MetroListPickerCollapsedLightPreview() {
    MetroTheme(darkTheme = false) {
        MetroListPicker(
            label = "Background",
            options = listOf("dark", "light"),
            selectedOptionIndex = 0,
            onSelectOption = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}
