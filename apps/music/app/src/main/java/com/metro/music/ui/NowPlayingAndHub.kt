package com.metro.music.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.metro.music.data.artworkModel
import com.metro.ui.MetroColors
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroListItem
import com.metro.ui.MetroPanorama
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val HubBrandText = "metro music"
private val HubBrandInset = 12.dp
private val GetMusicHubTileInset = 8.dp
/** Slightly under half-width so the pair reads lighter than full-bleed Start squares. */
private const val GetMusicHubTileWidthScale = 0.88f

private val MetroMusicBrandStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 96.sp,
    lineHeight = 96.sp,
    letterSpacing = (-1).sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/**
 * Music hub: panoramic brand title (no MetroAppTitle), then
 * collection | get music | now playing.
 * Reference: `references/images/hub_fullpage.png`, `hub_nowplaying_dark_green.jpg`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicHub(
    state: MusicState,
    pagerState: PagerState,
    onOpenCollection: (pivotPage: Int) -> Unit,
    onOpenExplore: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Giant panoramic brand — not the small MUSIC app overline
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .padding(bottom = 4.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            val measurer = rememberTextMeasurer()
            val availableWidthPx = with(density) { (maxWidth - HubBrandInset).toPx() }
            val brandWidthPx = remember(measurer, density) {
                measurer.measure(
                    text = HubBrandText,
                    style = MetroMusicBrandStyle,
                    softWrap = false,
                    maxLines = 1,
                    density = density,
                ).size.width.toFloat()
            }
            // Spread the off-screen remainder across the panorama so the last pane
            // ends with the tail of the brand flush at the right edge.
            val hiddenPx = (brandWidthPx - availableWidthPx).coerceAtLeast(0f)
            val lastPage = (pagerState.pageCount - 1).coerceAtLeast(1)
            val progress = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, lastPage.toFloat()) / lastPage
            val brandOffsetPx = (progress * hiddenPx).roundToInt()

            BasicText(
                text = HubBrandText,
                style = MetroMusicBrandStyle.copy(color = MetroTheme.colors.primaryText),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .offset { IntOffset(-brandOffsetPx, 0) }
                    .padding(start = HubBrandInset)
                    // Measure at full text width; the parent Box does the clipping, so the
                    // part that starts off-screen still exists and slides into view.
                    .wrapContentWidth(align = Alignment.Start, unbounded = true),
            )
        }

        MetroPanorama(
            titles = listOf("collection", "get music", "now playing"),
            pagerState = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 72.dp),
            pageContent = { page ->
                when (page) {
                    MusicState.HUB_COLLECTION -> CollectionHubPane(
                        state = state,
                        onOpenPivot = onOpenCollection,
                    )
                    MusicState.HUB_GET_MUSIC -> GetMusicPane(
                        state = state,
                        onOpenExplore = onOpenExplore,
                        onOpenSettings = onOpenSettings,
                    )
                    else -> NowPlayingPane(state = state)
                }
            },
        )
    }
}

@Composable
fun CollectionHubPane(
    state: MusicState,
    onOpenPivot: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        val links = listOf(
            "artists" to 0,
            "albums" to 1,
            "songs" to 2,
            "genres" to 4,
            "playlists" to 3,
            "radio" to -1,
        )
        links.forEach { (label, pivot) ->
            MetroListItem(
                title = label,
                titleStyle = MetroTextStyle.HubLink,
                verticalPadding = 8.dp,
                oneLineMinHeight = 56.dp,
                onClick = {
                    if (pivot >= 0) onOpenPivot(pivot)
                    else onOpenPivot(0) // radio → collection for v1
                },
            )
        }
        if (state.ytSyncMessage != null && !state.ytSyncing) {
            Spacer(modifier = Modifier.height(12.dp))
            MetroText(
                text = state.ytSyncMessage.orEmpty(),
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
fun GetMusicPane(
    state: MusicState,
    onOpenExplore: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    // WP8.1 Xbox Music leaves a clear gap under the panorama header before the
    // accent squares (`hub_fullpage.png` centre pane). Tiles are flush blocks, so
    // they need more than the collection link list's 12dp.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(top = 24.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tileSize = ((maxWidth - 8.dp) / 2) * GetMusicHubTileWidthScale
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GetMusicHubTile(
                    title = "search",
                    glyph = GetMusicTileGlyph.Search,
                    onClick = onOpenExplore,
                    modifier = Modifier.size(tileSize),
                )
                GetMusicHubTile(
                    title = if (state.ytConnected) "account" else "connect",
                    glyph = GetMusicTileGlyph.Account,
                    onClick = onOpenSettings,
                    modifier = Modifier.size(tileSize),
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        MetroText(
            text = if (state.ytConnected) {
                state.ytSyncMessage?.takeUnless { state.ytSyncing }
                    ?: "YouTube Music connected"
            } else {
                "Connect YouTube Music to stream and sync"
            },
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (!state.ytConnected) {
            MetroBorderButton(text = "connect youtube music", onClick = onOpenSettings)
        } else {
            MetroBorderButton(text = "sync now", onClick = { state.refreshYtLibrary() })
            Spacer(modifier = Modifier.height(8.dp))
            MetroBorderButton(text = "search", onClick = onOpenExplore)
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (state.ytSyncing && state.ytSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                MetroLoadingDots()
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.ytSongs.take(12), key = { it.id }) { song ->
                    MusicListRow(
                        title = song.title,
                        subtitle = song.artist,
                        onClick = {
                            state.playSongs(state.ytSongs, state.ytSongs.indexOf(song).coerceAtLeast(0))
                        },
                    )
                }
            }
        }
    }
}

private enum class GetMusicTileGlyph {
    Search,
    Account,
}

/**
 * Start-style square on the get-music hub: accent fill, centered glyph, label bottom-left.
 * Matches the idle 2×2 Music tile layout (`references/images/start_music_tile_dark_blue.jpg`).
 */
@Composable
private fun GetMusicHubTile(
    title: String,
    glyph: GetMusicTileGlyph,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MetroTheme.colors.accent
    val content = MetroColors.tileContentColor(background)
    BoxWithConstraints(
        modifier = modifier
            .background(background)
            .clickable(onClick = onClick)
            .semantics { contentDescription = title }
            .padding(GetMusicHubTileInset),
    ) {
        val iconSize = minOf(maxWidth, maxHeight) * 0.54f
        Canvas(
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.Center),
        ) {
            drawGetMusicTileGlyph(glyph, content)
        }
        MetroText(
            text = title,
            style = MetroTextStyle.ListItemTitle,
            color = content,
            maxLines = 1,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

private fun DrawScope.drawGetMusicTileGlyph(glyph: GetMusicTileGlyph, color: Color) {
    when (glyph) {
        GetMusicTileGlyph.Search -> drawSearchTileGlyph(color)
        GetMusicTileGlyph.Account -> drawAccountTileGlyph(color)
    }
}

private fun DrawScope.drawSearchTileGlyph(color: Color) {
    val s = size.minDimension
    val ox = (size.width - s) / 2f
    val oy = (size.height - s) / 2f
    val strokeWidth = s * 0.10f
    val cx = ox + s * 0.40f
    val cy = oy + s * 0.40f
    val radius = s * 0.26f
    drawCircle(color, radius, Offset(cx, cy), style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    val angle = (PI / 4f).toFloat()
    val start = Offset(cx + radius * cos(angle), cy + radius * sin(angle))
    val handleLen = s * 0.38f
    val end = Offset(start.x + handleLen * cos(angle), start.y + handleLen * sin(angle))
    drawLine(color, start, end, strokeWidth, StrokeCap.Round)
}

/** WP People-style silhouette: head + cropped shoulders. */
private fun DrawScope.drawAccountTileGlyph(color: Color) {
    val s = size.minDimension
    val ox = (size.width - s) / 2f
    val oy = (size.height - s) / 2f
    drawCircle(color, s * 0.17f, Offset(ox + s * 0.50f, oy + s * 0.28f))
    val body = Path().apply {
        moveTo(ox + s * 0.12f, oy + s)
        cubicTo(
            ox + s * 0.12f, oy + s * 0.54f,
            ox + s * 0.30f, oy + s * 0.50f,
            ox + s * 0.50f, oy + s * 0.50f,
        )
        cubicTo(
            ox + s * 0.70f, oy + s * 0.50f,
            ox + s * 0.88f, oy + s * 0.54f,
            ox + s * 0.88f, oy + s,
        )
        close()
    }
    drawPath(body, color)
}

@Composable
fun NowPlayingPane(state: MusicState) {
    val song = state.currentSong
    var dragAccum by remember { mutableFloatStateOf(0f) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Clip at the pane edge so long titles run past the end margin and cut off
            // mid-glyph at the screen, matching WP8.1 Xbox Music (never wrap).
            .clipToBounds()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp),
    ) {
        if (song == null) {
            MetroText(
                text = "Nothing playing",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
            )
            Spacer(modifier = Modifier.height(12.dp))
            MetroText(
                text = "Pick a song from collection or get music.",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
            )
            return
        }

        NowPlayingOverflowText(text = song.title, style = MetroTextStyle.ListItemTitle)
        NowPlayingOverflowText(
            text = "by ${song.artist}",
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Art fills the pane minus the glyph rail; the rail keeps shuffle/repeat pinned to the top
        // of the art and the queue mark to its bottom, as in the WP8.1 capture. The scrubber runs
        // the width of the art, directly under it.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val railWidth = 48.dp
            val artSize = maxWidth - railWidth - 8.dp
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(artSize)
                            .background(MetroTheme.colors.secondarySurface)
                            .pointerInput(song.id) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        when {
                                            dragAccum < -80f -> state.skipNext()
                                            dragAccum > 80f -> state.skipPrevious()
                                        }
                                        dragAccum = 0f
                                    },
                                    onVerticalDrag = { _, dragAmount ->
                                        dragAccum += dragAmount
                                    },
                                )
                            },
                    ) {
                        val artModel = song.artworkModel()
                        if (artModel != null) {
                            AsyncImage(
                                model = artModel,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.High,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        modifier = Modifier
                            .width(railWidth)
                            .height(artSize),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            MediaGlyphButton(
                                glyph = MediaGlyph.Shuffle,
                                onClick = { state.toggleShuffle() },
                                contentDescription = if (state.shuffle) "shuffle on" else "shuffle",
                                color = glyphColor(state.shuffle),
                            )
                            MediaGlyphButton(
                                glyph = if (state.repeatMode == 2) MediaGlyph.RepeatOne else MediaGlyph.Repeat,
                                onClick = { state.cycleRepeat() },
                                contentDescription = when (state.repeatMode) {
                                    1 -> "repeat all"
                                    2 -> "repeat one"
                                    else -> "repeat"
                                },
                                color = glyphColor(state.repeatMode != 0),
                            )
                        }
                        MediaGlyphButton(
                            glyph = MediaGlyph.Queue,
                            onClick = { },
                            contentDescription = "queue",
                        )
                    }
                }

                MediaCircleSeekBar(
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    onSeek = { state.seekTo(it) },
                    modifier = Modifier.width(artSize),
                )
            }
        }

        NowPlayingOverflowText(
            text = "Up next: —",
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
        )
        Spacer(modifier = Modifier.weight(1f))
        // Transport sits flush left under the art, one diameter between circles, as in the
        // WP8.1 capture — never centred or spread across the pane.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(MediaTransportButtonSize),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediaTransportButton(
                glyph = MediaGlyph.Previous,
                onClick = { state.skipPrevious() },
                contentDescription = "previous",
            )
            MediaTransportButton(
                glyph = if (state.isPlaying) MediaGlyph.Pause else MediaGlyph.Play,
                onClick = { state.togglePlayPause() },
                contentDescription = if (state.isPlaying) "pause" else "play",
            )
            MediaTransportButton(
                glyph = MediaGlyph.Next,
                onClick = { state.skipNext() },
                contentDescription = "next",
            )
        }
    }
}

/**
 * Single-line now-playing copy. Measures at intrinsic width so the line can overrun the
 * pane's end margin; [NowPlayingPane] clips at the screen edge instead of wrapping.
 */
@Composable
private fun NowPlayingOverflowText(
    text: String,
    style: MetroTextStyle,
    color: Color = MetroTheme.colors.primaryText,
) {
    MetroText(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true),
    )
}

/** Toggle glyphs sit dimmed when off and take the accent when on, per the WP8.1 capture. */
@Composable
private fun glyphColor(active: Boolean): Color =
    if (active) MetroTheme.colors.accent else MetroTheme.colors.secondaryText
