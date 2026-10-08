package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroStepSlider
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/**
 * WP8.1-style discrete slider preference — same control language as Settings
 * ease of access ([MetroStepSlider] with tick notches).
 */
@Composable
fun SliderPreferenceWidget(
    value: Int,
    valueRange: IntProgression,
    title: String,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    valueString: String = value.toString(),
) {
    val values = remember(valueRange.first, valueRange.last, valueRange.step) {
        valueRange.toList()
    }
    val stepCount = values.size.coerceAtLeast(2)
    val index = values.indexOf(value).coerceIn(0, stepCount - 1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetroText(
                text = title.lowercase(),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.weight(1f),
            )
            MetroText(
                text = valueString.lowercase(),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.primaryText,
            )
        }
        if (!subtitle.isNullOrBlank()) {
            MetroText(
                text = subtitle.lowercase(),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
        MetroStepSlider(
            index = index,
            onIndexChange = { next ->
                values.getOrNull(next)?.let(onChange)
            },
            stepCount = stepCount,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 12.dp,
            ),
        )
    }
}
