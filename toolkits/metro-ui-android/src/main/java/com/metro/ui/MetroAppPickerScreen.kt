package com.metro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One row in [MetroAppPickerScreen]. [packageName] null = the WP8.1 **none** option.
 */
data class MetroAppPickerEntry(
    val packageName: String?,
    val label: String,
)

/**
 * WP8.1 **choose an app** page — full-screen vertical list for picking a single app
 * (METRO-UX-LANGUAGE §6.20).
 *
 * Shows a small caps header, then **none** (accent when selected) followed by app labels.
 * Tap a row → [onSelected] with the package name (null for none). Parent should pop back.
 */
@Composable
fun MetroAppPickerScreen(
    apps: List<MetroAppPickerEntry>,
    selectedPackageName: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    headerTitle: String = "choose an app",
    onBack: (() -> Unit)? = null,
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.secondarySurface),
    ) {
        MetroAppTitle(title = headerTitle)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "none") {
                MetroAppPickerRow(
                    label = "none",
                    selected = selectedPackageName.isNullOrBlank(),
                    onClick = { onSelected(null) },
                )
            }
            items(
                items = apps,
                key = { it.packageName ?: it.label },
            ) { entry ->
                MetroAppPickerRow(
                    label = entry.label,
                    selected = entry.packageName == selectedPackageName,
                    onClick = { onSelected(entry.packageName) },
                )
            }
        }
    }
}

@Composable
private fun MetroAppPickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val titleColor = if (selected) {
        MetroTheme.colors.accent
    } else {
        MetroTheme.colors.primaryText
    }
    MetroListItem(
        title = label,
        titleStyle = MetroTextStyle.ListItemTitle,
        singleLine = true,
        oneLineMinHeight = MetroAppPickerDefaults.RowMinHeight,
        verticalPadding = MetroAppPickerDefaults.RowVerticalPadding,
        onClick = onClick,
        titleColor = titleColor,
    )
}

object MetroAppPickerDefaults {
    val RowMinHeight: Dp = 48.dp
    val RowVerticalPadding: Dp = 6.dp
}

@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F, widthDp = 360, heightDp = 640)
@Composable
private fun MetroAppPickerScreenPreview() {
    MetroTheme(darkTheme = true) {
        MetroAppPickerScreen(
            apps = listOf(
                MetroAppPickerEntry("com.metro.calendar", "Calendar"),
                MetroAppPickerEntry("com.metro.messaging", "Messaging"),
                MetroAppPickerEntry("com.metro.dialer", "Phone"),
            ),
            selectedPackageName = null,
            onSelected = {},
        )
    }
}
