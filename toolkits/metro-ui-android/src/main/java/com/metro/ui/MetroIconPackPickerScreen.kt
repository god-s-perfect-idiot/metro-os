package com.metro.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * WP8.1-style **choose an icon pack** page — same chrome as [MetroAppPickerScreen]
 * (METRO-UX-LANGUAGE §6.20 / §6.21): secondary surface, small-caps header, **none** first,
 * then installed pack labels.
 *
 * Use from Settings → start+theme via [MetroListPicker] `onOpen` (drill-in, not inline expand).
 */
@Composable
fun MetroIconPackPickerScreen(
    packs: List<MetroAppPickerEntry>,
    selectedPackageName: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    headerTitle: String = "choose an icon pack",
    onBack: (() -> Unit)? = null,
) {
    MetroAppPickerScreen(
        apps = packs,
        selectedPackageName = selectedPackageName,
        onSelected = onSelected,
        modifier = modifier,
        headerTitle = headerTitle,
        onBack = onBack,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1F1F1F, widthDp = 360, heightDp = 640)
@Composable
private fun MetroIconPackPickerScreenPreview() {
    MetroTheme(darkTheme = true) {
        MetroIconPackPickerScreen(
            packs = listOf(
                MetroAppPickerEntry("com.example.icons.arctic", "Arctic Icons"),
                MetroAppPickerEntry("com.example.icons.flat", "Flat Metro"),
            ),
            selectedPackageName = null,
            onSelected = {},
        )
    }
}
