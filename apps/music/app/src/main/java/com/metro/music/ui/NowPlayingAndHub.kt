package com.metro.music.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.metro.music.data.artworkModel
import com.metro.music.data.loadAlbumBrandGradientArgb
import com.metro.ui.MetroColors
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroListItem
import com.metro.ui.MetroPanorama
import com.metro.ui.MetroPanoramaBodyEnter
import com.metro.ui.MetroPanoramaBrandEnter
import com.metro.ui.MetroPanoramaBrandGradients
import com.metro.ui.MetroPanoramaBrandTitle
import com.metro.ui.MetroPanoramaBrandTrailingSlack
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroSystemIconType
import com.metro.ui.drawMetroSystemIconGlyph
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val HubBrandText = "metro music"
private val HubAccentTileInset = 8.dp
/** Slightly under half-width so the pair reads lighter than full-bleed Start squares. */
private const val HubAccentTileWidthScale = 0.88f
/** Fallback when no colourful album art — warm sunset brand. */
private val MusicBrandBrush = MetroPanoramaBrandGradients.Music

/**
 * Music hub: panoramic brand title (no MetroAppTitle), then
 * collection | get music | now playing | local.
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
    onOpenRecent: () -> Unit,
    onSyncNow: () -> Unit,
    onOpenMusicDirectories: () -> Unit,
    skipIntro: Boolean = false,
    onIntroPlayed: () -> Unit = {},
) {
    val context = LocalContext.current
    // Album-derived brand when colourful art is loaded; otherwise Music sunset preset.
    var brandBrush by remember { mutableStateOf(MusicBrandBrush) }
    LaunchedEffect(state.nowPlayingBackdropArt) {
        val art = state.nowPlayingBackdropArt
        if (art == null) {
            brandBrush = MusicBrandBrush
            return@LaunchedEffect
        }
        val stops = loadAlbumBrandGradientArgb(context, art)
        brandBrush = stops?.let { argbs ->
            Brush.horizontalGradient(argbs.map { Color(it) })
        } ?: MusicBrandBrush
    }

    // Remember after this hub visit so in-app return skips the intro (not mid-animation).
    DisposableEffect(Unit) {
        onDispose { onIntroPlayed() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Giant panoramic brand — not the small MUSIC app overline
        MetroPanoramaBrandEnter(skipEnter = skipIntro) {
            MetroPanoramaBrandTitle(
                text = HubBrandText,
                pageProgress = pagerState.currentPage + pagerState.currentPageOffsetFraction,
                pageCount = pagerState.pageCount,
                brush = brandBrush,
                // Extra bleed past the last glyph so the brand overflows and scrolls more.
                trailingSlack = MetroPanoramaBrandTrailingSlack,
            )
        }

        MetroPanoramaBodyEnter(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            skipEnter = skipIntro,
        ) {
            MetroPanorama(
                titles = listOf("collection", "get music", "now playing", "local"),
                pagerState = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp),
                pageContent = { page ->
                    when (page) {
                        MusicState.HUB_COLLECTION -> CollectionHubPane(
                            onOpenPivot = onOpenCollection,
                        )
                        MusicState.HUB_GET_MUSIC -> GetMusicPane(
                            state = state,
                            onOpenExplore = onOpenExplore,
                            onOpenSettings = onOpenSettings,
                            onOpenRecent = onOpenRecent,
                        )
                        MusicState.HUB_NOW_PLAYING -> NowPlayingPane(state = state)
                        else -> LocalPane(
                            state = state,
                            onOpenSettings = onOpenSettings,
                            onSyncNow = onSyncNow,
                            onOpenMusicDirectories = onOpenMusicDirectories,
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun CollectionHubPane(
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
    }
}

@Composable
fun GetMusicPane(
    state: MusicState,
    onOpenExplore: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRecent: () -> Unit,
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
            val tileSize = ((maxWidth - 8.dp) / 2) * HubAccentTileWidthScale
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MusicHubAccentTile(
                        title = "search",
                        glyph = HubAccentTileGlyph.Search,
                        onClick = onOpenExplore,
                        modifier = Modifier.size(tileSize),
                    )
                    MusicHubAccentTile(
                        title = if (state.ytConnected) "account" else "connect",
                        glyph = HubAccentTileGlyph.Account,
                        onClick = onOpenSettings,
                        modifier = Modifier.size(tileSize),
                    )
                }
                MusicHubAccentTile(
                    title = "recent",
                    glyph = HubAccentTileGlyph.Recent,
                    onClick = onOpenRecent,
                    modifier = Modifier.size(tileSize),
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        MetroText(
            text = if (state.ytConnected) {
                "YouTube Music connected"
            } else {
                "Connect YouTube Music to stream and sync"
            },
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
        )
        if (!state.ytConnected) {
            Spacer(modifier = Modifier.height(8.dp))
            MetroBorderButton(text = "connect youtube music", onClick = onOpenSettings)
        }
    }
}

/**
 * On-device library pane: same Start-style accent tile size and glyph weight as get music,
 * with settings, sync now, and music directories.
 */
@Composable
fun LocalPane(
    state: MusicState,
    onOpenSettings: () -> Unit,
    onSyncNow: () -> Unit,
    onOpenMusicDirectories: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(top = 24.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tileSize = ((maxWidth - 8.dp) / 2) * HubAccentTileWidthScale
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MusicHubAccentTile(
                        title = "settings",
                        glyph = HubAccentTileGlyph.Settings,
                        onClick = onOpenSettings,
                        modifier = Modifier.size(tileSize),
                    )
                    MusicHubAccentTile(
                        title = "sync now",
                        glyph = HubAccentTileGlyph.Sync,
                        onClick = onSyncNow,
                        modifier = Modifier.size(tileSize),
                    )
                }
                MusicHubAccentTile(
                    title = "music directories",
                    glyph = HubAccentTileGlyph.Directories,
                    onClick = onOpenMusicDirectories,
                    modifier = Modifier.size(tileSize),
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        MetroText(
            text = localPaneStatus(state),
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
        )
    }
}

private fun localPaneStatus(state: MusicState): String = when {
    !state.hasAudioPermission -> "Allow music access to sync this device"
    state.libraryLoading -> "Scanning…"
    state.ytSyncing -> state.ytSyncMessage ?: "Syncing…"
    state.ytSyncMessage != null -> state.ytSyncMessage.orEmpty()
    else -> {
        val n = state.localSongs.size
        if (n == 1) "1 song on this device" else "$n songs on this device"
    }
}

private enum class HubAccentTileGlyph {
    Search,
    Account,
    Recent,
    Settings,
    Sync,
    Directories,
}

/**
 * Start-style square on hub discovery panes: accent fill, centered glyph, label bottom-left.
 * Matches the idle 2×2 Music tile layout (`references/images/start_music_tile_dark_blue.jpg`).
 */
@Composable
private fun MusicHubAccentTile(
    title: String,
    glyph: HubAccentTileGlyph,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MetroTheme.colors.accent
    val content = MetroColors.tileContentColor(background)
    BoxWithConstraints(
        modifier = modifier
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = title }
            .padding(HubAccentTileInset),
    ) {
        val iconSize = minOf(maxWidth, maxHeight) * 0.54f
        Canvas(
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.Center),
        ) {
            drawHubAccentTileGlyph(glyph, content)
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

private fun DrawScope.drawHubAccentTileGlyph(glyph: HubAccentTileGlyph, color: Color) {
    when (glyph) {
        HubAccentTileGlyph.Search -> drawSearchTileGlyph(color)
        HubAccentTileGlyph.Account -> drawAccountTileGlyph(color)
        HubAccentTileGlyph.Recent -> drawRecentTileGlyph(color)
        // Chrome system glyphs are sized for app-bar rings (~0.38–0.42). Boost to the
        // hub-tile weight used by recent (~0.82) so local matches get music.
        HubAccentTileGlyph.Settings -> drawHubSystemTileGlyph(
            MetroSystemIconType.Settings,
            color,
            chromeGlyphScale = 0.42f,
        )
        HubAccentTileGlyph.Sync -> drawHubSystemTileGlyph(
            MetroSystemIconType.Refresh,
            color,
            chromeGlyphScale = 0.38f,
        )
        HubAccentTileGlyph.Directories -> drawHubSystemTileGlyph(
            MetroSystemIconType.Folder,
            color,
            chromeGlyphScale = 0.42f,
        )
    }
}

/** Hub accent tiles target the same ink weight as [drawRecentTileGlyph] (0.82 of the canvas). */
private const val HubTileGlyphScale = 0.82f

private fun DrawScope.drawHubSystemTileGlyph(
    type: MetroSystemIconType,
    color: Color,
    chromeGlyphScale: Float,
) {
    val boost = HubTileGlyphScale / chromeGlyphScale
    withTransform({
        scale(scaleX = boost, scaleY = boost, pivot = center)
    }) {
        drawMetroSystemIconGlyph(type, color)
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

/** History / recent plays — 512 viewBox path from the provided SVG. */
private const val RecentGlyphPathData =
    "M256 0C179.9 0 111.7 33.4 64.9 86.2L0 21.3V192h170.7l-60.2-60.2C145.6 90.5 197.5 64 256 64c106 0 192 85.9 192 192s-86 192-192 192c-53 0-101-21.5-135.8-56.2L75 437c46.4 46.3 110.4 75 181 75c141.4 0 256-114.6 256-256S397.4 0 256 0m-21.3 106.7v170.7h128v-42.7h-85.3v-128z"

private val recentGlyphPath: Path by lazy {
    PathParser().parsePathString(RecentGlyphPathData).toPath()
}

private fun DrawScope.drawRecentTileGlyph(color: Color) {
    // Slightly under the canvas so the history mark reads lighter than search/account fills.
    val scale = size.minDimension / 512f * 0.82f
    val cx = size.width / 2f
    val cy = size.height / 2f
    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        translate(left = -256f, top = -256f)
    }) {
        drawPath(recentGlyphPath, color)
    }
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
            // Top inset and the weight spacer under Up next trade the same gap — nudge
            // title down from the panorama header without pushing transport off-screen.
            .padding(top = 16.dp, bottom = 8.dp),
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
                            onClick = { state.openQueue() },
                            contentDescription = "queue",
                        )
                    }
                }

                MediaCircleSeekBar(
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    onSeek = { state.seekTo(it) },
                    loading = state.loadingPlayback || state.durationMs <= 0L,
                    modifier = Modifier.width(artSize),
                )
            }
        }

        NowPlayingOverflowText(
            text = state.upNextLabel,
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
                type = MetroSystemIconType.Previous,
                onClick = { state.skipPrevious() },
                contentDescription = "previous",
            )
            MediaTransportButton(
                type = if (state.isPlaying) MetroSystemIconType.Pause else MetroSystemIconType.Play,
                onClick = { state.togglePlayPause() },
                contentDescription = if (state.isPlaying) "pause" else "play",
            )
            MediaTransportButton(
                type = MetroSystemIconType.Next,
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
