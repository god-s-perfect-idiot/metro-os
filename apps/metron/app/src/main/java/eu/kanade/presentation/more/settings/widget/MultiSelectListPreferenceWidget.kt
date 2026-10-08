package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.MultiSelectPreferenceScreen

@Composable
fun <T> MultiSelectListPreferenceWidget(
    values: Set<T>,
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    entries: Map<out T, String>,
    onValuesChange: (Set<T>) -> Unit,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedIcon = icon
    val navigator = LocalNavigator.currentOrThrow
    val entryPairs = entries.map { (key, label) -> key.toString() to label }
    val keyById = entries.keys.associateBy { it.toString() }
    val selectedIds = values.map { it.toString() }.toSet()

    TextPreferenceWidget(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onPreferenceClick = {
            navigator.push(
                MultiSelectPreferenceScreen(
                    title = title,
                    entries = entryPairs,
                    initialSelected = selectedIds,
                    onConfirm = { ids ->
                        val mapped = ids.mapNotNull { keyById[it] }.toSet()
                        onValuesChange(mapped)
                    },
                ),
            )
        },
    )
}
