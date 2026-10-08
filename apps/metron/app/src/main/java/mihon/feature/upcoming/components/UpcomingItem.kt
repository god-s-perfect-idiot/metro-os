package mihon.feature.upcoming.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metro.ui.MetroListItem
import com.metro.ui.MetroTheme
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover

@Composable
fun UpcomingItem(
    upcoming: Manga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MetroListItem(
        title = upcoming.title,
        singleLine = false,
        leading = {
            AsyncImage(
                model = upcoming.asMangaCover(),
                contentDescription = null,
                placeholder = ColorPainter(Color(0x1F888888)),
                error = ColorPainter(Color(0x1F888888)),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 48.dp, height = 72.dp)
                    .background(MetroTheme.colors.secondarySurface, RectangleShape),
            )
        },
        oneLineMinHeight = 80.dp,
        twoLineMinHeight = 88.dp,
        verticalPadding = 8.dp,
        onClick = onClick,
        modifier = modifier,
    )
}
