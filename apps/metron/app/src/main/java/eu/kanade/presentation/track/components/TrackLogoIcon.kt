package eu.kanade.presentation.track.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroColors
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.data.track.Tracker
import tachiyomi.presentation.core.util.clickableNoIndication
import kotlin.math.absoluteValue

private val TrackTileSize = 48.dp
private val TrackLogoInset = 8.dp

@Composable
fun TrackLogoIcon(
    tracker: Tracker,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    size: Dp = TrackTileSize,
) {
    val tileColor = remember(tracker.id) { trackTileColor(tracker.id) }
    val clickModifier = if (onClick != null) {
        Modifier.clickableNoIndication(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier
    }

    Box(
        modifier = clickModifier
            .size(size)
            .background(tileColor, RectangleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(tracker.getLogo()),
            contentDescription = tracker.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(size)
                .padding(TrackLogoInset),
        )
    }
}

/** Stable Metro accent tile fill — varied across trackers, never recomputed randomly. */
internal fun trackTileColor(trackerId: Long): Color {
    preferredTrackTileColor(trackerId)?.let { return it }
    val palette = MetroColors.AccentPalette
    return palette[(trackerId.absoluteValue % palette.size).toInt()]
}

/** Ids match [eu.kanade.tachiyomi.data.track.TrackerManager] construction. */
private fun preferredTrackTileColor(trackerId: Long): Color? =
    when (trackerId) {
        1L -> MetroColors.AccentCobalt // MyAnimeList
        2L -> MetroColors.AccentCyan // AniList
        3L -> MetroColors.AccentOrange // Kitsu
        4L -> MetroColors.AccentMauve // Shikimori
        5L -> MetroColors.AccentPink // Bangumi
        6L -> MetroColors.AccentCobalt // Komga
        7L -> MetroColors.AccentSteel // MangaUpdates
        8L -> MetroColors.AccentTeal // Kavita
        9L -> MetroColors.AccentCyan // Suwayomi
        10L -> MetroColors.AccentCrimson // Hikka
        11L -> MetroColors.AccentAmber // MangaBaka
        else -> null
    }

@PreviewLightDark
@Composable
private fun TrackLogoIconPreviews(
    @PreviewParameter(TrackLogoIconPreviewProvider::class)
    tracker: Tracker,
) {
    TachiyomiPreviewTheme {
        TrackLogoIcon(
            tracker = tracker,
            onClick = null,
            onLongClick = null,
        )
    }
}
