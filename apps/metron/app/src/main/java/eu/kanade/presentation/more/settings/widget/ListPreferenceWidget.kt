package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption

@Composable
fun <T> ListPreferenceWidget(
    value: T,
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    entries: Map<out T, String>,
    onValueChange: (T) -> Unit,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedSubtitle = subtitle
    @Suppress("UNUSED_PARAMETER")
    val unusedIcon = icon

    val options = remember(entries) {
        entries.map { (key, label) ->
            MetroListPickerOption(value = key as T, label = label.lowercase())
        }
    }

    MetroListPicker(
        selected = value,
        options = options,
        onSelectedChange = onValueChange,
        label = title.lowercase(),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
