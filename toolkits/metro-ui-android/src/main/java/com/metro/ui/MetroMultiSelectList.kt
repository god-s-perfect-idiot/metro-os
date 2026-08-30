package com.metro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One row identity in [MetroMultiSelectList].
 *
 * [id] is the selection key; [title] is the primary label. Optional leading chrome is
 * supplied via [MetroMultiSelectList]'s [itemLeading] slot.
 */
data class MetroMultiSelectItem(
    val id: String,
    val title: String,
)

/**
 * WP8.1 multi-select list — Apps Corner / account-picker style.
 *
 * Dense rows: hollow square checkbox → optional leading → title. Overline [title] (ALL CAPS
 * via [MetroAppTitle]). Bottom [MetroAppBar] with check (confirm) and close (cancel).
 *
 * Selection is controlled via [selectedIds] / [onSelectionChange]; the caller persists on
 * [onConfirm] and discards on [onCancel] / Back.
 */
@Composable
fun MetroMultiSelectList(
    title: String,
    items: List<MetroMultiSelectItem>,
    selectedIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    itemLeading: (@Composable (MetroMultiSelectItem) -> Unit)? = null,
    confirmLabel: String = "done",
    cancelLabel: String = "cancel",
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroAppTitle(title = title)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = MetroMultiSelectDefaults.ListBottomPadding,
                ),
            ) {
                items(items, key = { it.id }) { item ->
                    val checked = item.id in selectedIds
                    MetroMultiSelectRow(
                        title = item.title,
                        checked = checked,
                        leading = itemLeading?.let { leading ->
                            { leading(item) }
                        },
                        onClick = {
                            onSelectionChange(
                                if (checked) selectedIds - item.id else selectedIds + item.id,
                            )
                        },
                    )
                }
            }
        }
        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Check,
                    label = confirmLabel,
                    onClick = onConfirm,
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Close,
                    label = cancelLabel,
                    onClick = onCancel,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun MetroMultiSelectRow(
    title: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MetroMultiSelectDefaults.RowMinHeight)
            .clipToBounds()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                top = MetroMultiSelectDefaults.RowVerticalPadding,
                bottom = MetroMultiSelectDefaults.RowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        MetroCheckBox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            size = MetroMultiSelectDefaults.CheckboxSize,
        )
        if (leading != null) {
            Box(
                modifier = Modifier
                    .padding(start = MetroMultiSelectDefaults.CheckboxToLeadingGap)
                    .size(MetroMultiSelectDefaults.LeadingSize),
                contentAlignment = Alignment.Center,
            ) {
                leading()
            }
        }
        MetroText(
            text = title,
            style = MetroTextStyle.ListItemTitle,
            color = if (enabled) {
                MetroTheme.colors.primaryText
            } else {
                MetroTheme.colors.secondaryText
            },
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .padding(start = MetroMultiSelectDefaults.LeadingToTitleGap)
                .weight(1f, fill = false),
        )
    }
}

object MetroMultiSelectDefaults {
    val RowMinHeight: Dp = 48.dp
    val RowVerticalPadding: Dp = 6.dp
    val LeadingSize: Dp = 40.dp
    val CheckboxSize: Dp = 24.dp
    val CheckboxToLeadingGap: Dp = 12.dp
    val LeadingToTitleGap: Dp = 12.dp
    val ListBottomPadding: Dp = 88.dp
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 640)
@Composable
private fun MetroMultiSelectListDarkPreview() {
    MetroTheme(darkTheme = true) {
        MetroMultiSelectList(
            title = "apps",
            items = listOf(
                MetroMultiSelectItem("a", "Adobe Reader"),
                MetroMultiSelectItem("b", "Alarms"),
                MetroMultiSelectItem("c", "Calculator"),
            ),
            selectedIds = setOf("b"),
            onSelectionChange = {},
            onConfirm = {},
            onCancel = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 640)
@Composable
private fun MetroMultiSelectListLightPreview() {
    MetroTheme(darkTheme = false) {
        MetroMultiSelectList(
            title = "apps",
            items = listOf(
                MetroMultiSelectItem("a", "Adobe Reader"),
                MetroMultiSelectItem("b", "Alarms"),
            ),
            selectedIds = setOf("a"),
            onSelectionChange = {},
            onConfirm = {},
            onCancel = {},
        )
    }
}
