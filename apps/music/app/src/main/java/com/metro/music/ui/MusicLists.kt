package com.metro.music.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.music.data.Album
import com.metro.music.data.LibraryLogic
import com.metro.ui.MetroDimens
import com.metro.ui.MetroJumpListLogic
import com.metro.ui.MetroLetterTile
import com.metro.ui.MetroListItem
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import com.metro.ui.metroStickyLetterHeader

/** Xbox Music packs song/album/artist rows tighter than the 76/90dp `MetroListItem` defaults. */
private val RowVerticalPadding = 4.dp
private val RowOneLineMinHeight = 48.dp
private val RowTwoLineMinHeight = 60.dp
private val LetterMarkerVerticalPadding = 4.dp
private val StreamingRadioGlyphSize = 14.dp

/** Dense collection row — songs, albums, artists, and streaming search results. */
@Composable
fun MusicListRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    leading: @Composable (() -> Unit)? = null,
) {
    MetroListItem(
        title = title,
        subtitle = subtitle,
        leading = leading,
        verticalPadding = RowVerticalPadding,
        oneLineMinHeight = if (leading != null) AlbumArtThumbnailSize + 8.dp else RowOneLineMinHeight,
        twoLineMinHeight = if (leading != null) AlbumArtThumbnailSize + 8.dp else RowTwoLineMinHeight,
        singleLine = true,
        onClick = onClick,
    )
}

/**
 * Chart / streaming song row matching WP8.1 Xbox Music: title, then radio glyph + artist.
 * No rank index — flush left like the genre top-songs capture.
 */
@Composable
fun MusicStreamingSongRow(
    title: String,
    artist: String,
    onClick: () -> Unit,
) {
    val subtitleColor = MetroTheme.colors.secondaryText
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowTwoLineMinHeight)
            .metroClickable(onClick = onClick)
            .clipToBounds()
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            MetroText(
                text = title,
                style = MetroTextStyle.ListItemTitle,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.wrapContentWidth(unbounded = true, align = Alignment.Start),
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Canvas(modifier = Modifier.size(StreamingRadioGlyphSize)) {
                    drawStreamingRadioGlyph(subtitleColor)
                }
                MetroText(
                    text = artist,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = subtitleColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.wrapContentWidth(unbounded = true, align = Alignment.Start),
                )
            }
        }
    }
}

/** WP Xbox Music streaming mark — centre hub with left/right radio arcs. */
private fun DrawScope.drawStreamingRadioGlyph(color: Color) {
    val s = size.minDimension
    val cx = size.width / 2f
    val cy = size.height / 2f
    val hub = s * 0.11f
    drawCircle(color, hub, Offset(cx, cy))
    // Inner arcs sit closer to the hub and are shorter/thinner; outer arcs are taller with
    // a clear gap between the two rings (WP `((•))` streaming mark).
    drawRadioArcPair(
        color = color,
        cx = cx,
        cy = cy,
        radius = s * 0.28f,
        sweep = 88f,
        strokeWidth = s * 0.085f,
    )
    drawRadioArcPair(
        color = color,
        cx = cx,
        cy = cy,
        radius = s * 0.46f,
        sweep = 110f,
        strokeWidth = s * 0.11f,
    )
}

private fun DrawScope.drawRadioArcPair(
    color: Color,
    cx: Float,
    cy: Float,
    radius: Float,
    sweep: Float,
    strokeWidth: Float,
) {
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    val half = sweep / 2f
    // Right arc centered on 0°; left arc centered on 180°.
    drawArc(
        color = color,
        startAngle = -half,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2f, radius * 2f),
        style = stroke,
    )
    drawArc(
        color = color,
        startAngle = 180f - half,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2f, radius * 2f),
        style = stroke,
    )
}

/** Album row with square cover art (placeholder + music glyph while loading). */
@Composable
fun MusicAlbumRow(
    album: Album,
    onClick: () -> Unit,
    subtitle: String? = album.artist,
) {
    MusicListRow(
        title = album.title,
        subtitle = subtitle,
        onClick = onClick,
        leading = {
            AlbumArtThumbnail(
                model = album.artworkUri,
                contentDescription = album.title,
            )
        },
    )
}

/**
 * Collection list grouped under sticky letter markers (METRO-UX-LANGUAGE §6.18).
 *
 * Tapping a marker asks the host to open `MetroJumpList`; the picked letter comes back as
 * [jumpTarget] and scrolls that section to the top.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> MusicLetterList(
    items: List<T>,
    labelOf: (T) -> String,
    keyOf: (T) -> Any,
    jumpTarget: Char?,
    onJumpTargetConsumed: () -> Unit,
    onLetterMarkerClick: () -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (T) -> Unit,
) {
    val grouped = remember(items) { LibraryLogic.groupByJumpKey(items, labelOf) }
    val listState = rememberLazyListState()
    val headerIndices = remember(grouped) {
        var index = 0
        buildMap {
            grouped.forEach { (letter, sectionRows) ->
                put(letter, index)
                index += 1 + sectionRows.size
            }
        }
    }

    LaunchedEffect(jumpTarget, headerIndices) {
        val letter = jumpTarget ?: return@LaunchedEffect
        headerIndices[MetroJumpListLogic.normalize(letter)]?.let { listState.scrollToItem(it) }
        onJumpTargetConsumed()
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        grouped.forEach { (letter, sectionRows) ->
            metroStickyLetterHeader(letter) {
                // Opaque background so rows do not show through while the marker is pinned.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MetroTheme.colors.background)
                        .padding(
                            horizontal = MetroDimens.ScreenHorizontalMargin,
                            vertical = LetterMarkerVerticalPadding,
                        ),
                ) {
                    MetroLetterTile(letter = letter, onClick = onLetterMarkerClick)
                }
            }
            items(sectionRows, key = keyOf) { item -> row(item) }
        }
    }
}
