package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroListItem
import com.metro.ui.MetroTheme

@Composable
fun TextPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MetroTheme.colors.accent,
    widget: @Composable (() -> Unit)? = null,
    onPreferenceClick: (() -> Unit)? = null,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedIcon = icon
    @Suppress("UNUSED_PARAMETER")
    val unusedTint = iconTint

    MetroListItem(
        title = title.orEmpty().lowercase(),
        subtitle = subtitle?.lowercase(),
        modifier = modifier,
        trailing = widget,
        oneLineMinHeight = 64.dp,
        twoLineMinHeight = 72.dp,
        verticalPadding = 8.dp,
        onClick = onPreferenceClick,
    )
}
