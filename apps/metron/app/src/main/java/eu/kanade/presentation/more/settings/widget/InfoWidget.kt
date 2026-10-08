package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
internal fun InfoWidget(text: String) {
    MetroText(
        text = text,
        style = MetroTextStyle.Body,
        color = MetroTheme.colors.secondaryText,
        modifier = Modifier.padding(
            horizontal = MetroDimens.ScreenHorizontalMargin,
            vertical = 12.dp,
        ),
    )
}
