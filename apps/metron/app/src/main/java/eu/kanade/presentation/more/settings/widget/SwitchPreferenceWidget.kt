package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch

@Composable
fun SwitchPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean = false,
    onCheckedChanged: (Boolean) -> Unit,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedIcon = icon

    Column(modifier = modifier.padding(vertical = 2.dp)) {
        MetroToggleSwitch(
            checked = checked,
            onCheckedChange = onCheckedChanged,
            label = title.lowercase(),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        if (!subtitle.isNullOrBlank()) {
            MetroText(
                text = subtitle.lowercase(),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
    }
}
