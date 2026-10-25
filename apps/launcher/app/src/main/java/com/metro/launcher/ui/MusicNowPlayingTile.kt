package com.metro.launcher.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.launcher.data.MusicNowPlayingInfo
import com.metro.launcher.data.MusicNowPlayingStore
import com.metro.launcher.data.PinnedTileSize
import com.metro.system.MetroMusicLiveTileStyle
import com.metro.ui.MetroMediaTransportButton
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTheme
import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Suite default from Settings → connected apps → Music; pins may override. */
val LocalMusicLiveTileStyle = compositionLocalOf { MetroMusicLiveTileStyle.DEFAULT }

/** Circular transport controls on live tiles — same ring style as Music now playing, scaled down. */
private val TileTransportSmall = 40.dp
private val TileTransportMedium = 34.dp
private val TileTransportWide = 36.dp

/**
 * Dense Start (6-col / 3 medium across) uses a smaller ring so prev/play/next fit without
 * Compose shrinking the trailing control on narrower cells.
 */
private val TileTransportSmallDense = 34.dp
private val TileTransportMediumDense = 28.dp
private val TileTransportWideDense = 30.dp

private const val PresentationPanMs = 14_000

/**
 * Xbox Music–style now-playing Start face.
 *
 * - 1×1: album art + centered play/pause only (all styles)
 * - Compact: 2×2 bottom meta+transport; 4×2 art half + controls half
 * - Spacious: full-bleed art with title + transport across the whole tile
 * - Presentation: big uppercase title/artist panning in opposite directions on the accent
 */
@Composable
fun MusicNowPlayingTileContent(
    info: MusicNowPlayingInfo,
    size: PinnedTileSize,
    fallbackColor: Color,
    style: MetroMusicLiveTileStyle = MetroMusicLiveTileStyle.DEFAULT,
    modifier: Modifier = Modifier,
) {
    when (size) {
        PinnedTileSize.OneByOne -> MusicSmallNowPlayingFace(
            info = info,
            fallbackColor = fallbackColor,
            modifier = modifier,
        )
        PinnedTileSize.TwoByTwo, PinnedTileSize.FourByTwo -> when (style) {
            MetroMusicLiveTileStyle.Presentation -> MusicPresentationNowPlayingFace(
                info = info,
                fallbackColor = fallbackColor,
                modifier = modifier,
            )
            MetroMusicLiveTileStyle.SpaciousControls -> MusicSpaciousNowPlayingFace(
                info = info,
                size = size,
                fallbackColor = fallbackColor,
                modifier = modifier,
            )
            MetroMusicLiveTileStyle.CompactControls -> if (size == PinnedTileSize.TwoByTwo) {
                MusicMediumNowPlayingFace(
                    info = info,
                    fallbackColor = fallbackColor,
                    modifier = modifier,
                )
            } else {
                MusicWideNowPlayingFace(
                    info = info,
                    fallbackColor = fallbackColor,
                    modifier = modifier,
                )
            }
        }
    }
}

/** Transport ring diameter for [size]; [dense] is Settings → show more columns (3 medium across). */
internal fun musicTileTransportSize(size: PinnedTileSize, dense: Boolean): Dp = when (size) {
    PinnedTileSize.OneByOne -> if (dense) TileTransportSmallDense else TileTransportSmall
    PinnedTileSize.TwoByTwo -> if (dense) TileTransportMediumDense else TileTransportMedium
    PinnedTileSize.FourByTwo -> if (dense) TileTransportWideDense else TileTransportWide
}

@Composable
private fun MusicSmallNowPlayingFace(
    info: MusicNowPlayingInfo,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(fallbackColor),
        contentAlignment = Alignment.Center,
    ) {
        AlbumArtBackground(albumArtUri = info.albumArtUri, contentDescription = info.title)
        ScrimOverlay(alpha = 0.35f)
        MetroMediaTransportButton(
            type = if (info.isPlaying) MetroSystemIconType.Pause else MetroSystemIconType.Play,
            onClick = { MusicNowPlayingStore.togglePlayPause(info.packageName) },
            contentDescription = if (info.isPlaying) "Pause" else "Play",
            buttonSize = musicTileTransportSize(
                PinnedTileSize.OneByOne,
                dense = LocalTileChrome.current == TileChrome.Dense,
            ),
            color = Color.White,
            enabled = info.canPlayPause,
        )
    }
}

@Composable
private fun MusicMediumNowPlayingFace(
    info: MusicNowPlayingInfo,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
) {
    val chrome = LocalTileChrome.current
    val dense = chrome == TileChrome.Dense
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(fallbackColor),
    ) {
        AlbumArtBackground(albumArtUri = info.albumArtUri, contentDescription = info.title)
        // Darker bottom scrim so song/artist + transport stay readable over bright album art.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.70f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (dense) chrome.titlePaddingH else 8.dp,
                    vertical = if (dense) chrome.titlePaddingV else 6.dp,
                ),
            verticalArrangement = Arrangement.Bottom,
        ) {
            MusicTrackMeta(
                title = info.title,
                artist = info.artist,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(modifier = Modifier.height(if (dense) 4.dp else 6.dp))
            MusicTransportRow(
                info = info,
                buttonSize = musicTileTransportSize(PinnedTileSize.TwoByTwo, dense = dense),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Wide (4×2) face: left album art square, right metadata + transport — Xbox Music wide structure.
 */
@Composable
private fun MusicWideNowPlayingFace(
    info: MusicNowPlayingInfo,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
) {
    val chrome = LocalTileChrome.current
    val dense = chrome == TileChrome.Dense
    Row(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(fallbackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .clipToBounds()
                .background(fallbackColor),
        ) {
            AlbumArtBackground(albumArtUri = info.albumArtUri, contentDescription = info.title)
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(
                    horizontal = if (dense) 6.dp else 10.dp,
                    vertical = if (dense) chrome.titlePaddingV else 8.dp,
                ),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            MusicTrackMeta(
                title = info.title,
                artist = info.artist,
                modifier = Modifier.fillMaxWidth(),
                maxTitleLines = 2,
            )
            MusicTransportRow(
                info = info,
                buttonSize = musicTileTransportSize(PinnedTileSize.FourByTwo, dense = dense),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Spacious: album art as the full tile background; title + transport use the whole face
 * (not a half-panel on wide tiles).
 */
@Composable
private fun MusicSpaciousNowPlayingFace(
    info: MusicNowPlayingInfo,
    size: PinnedTileSize,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
) {
    val chrome = LocalTileChrome.current
    val dense = chrome == TileChrome.Dense
    val padH = when {
        dense && size == PinnedTileSize.FourByTwo -> 8.dp
        dense -> chrome.titlePaddingH
        size == PinnedTileSize.FourByTwo -> 12.dp
        else -> 8.dp
    }
    val padV = when {
        dense -> chrome.titlePaddingV
        size == PinnedTileSize.FourByTwo -> 10.dp
        else -> 8.dp
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(fallbackColor),
    ) {
        AlbumArtBackground(albumArtUri = info.albumArtUri, contentDescription = info.title)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.72f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padH, vertical = padV),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            MusicTrackMeta(
                title = info.title,
                artist = info.artist,
                modifier = Modifier.fillMaxWidth(),
                maxTitleLines = if (size == PinnedTileSize.FourByTwo) 2 else 1,
            )
            MusicTransportRow(
                info = info,
                buttonSize = musicTileTransportSize(size, dense = dense),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Presentation: album art (accent fallback) with oversized uppercase title (L→R) and
 * artist (R→L) panning.
 */
@Composable
private fun MusicPresentationNowPlayingFace(
    info: MusicNowPlayingInfo,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
) {
    val title = info.title?.trim().orEmpty()
    val artist = info.artist?.trim().orEmpty()
    val hasArt = !info.albumArtUri.isNullOrBlank()
    // Soft accent glyphs on solid fill; white over album art so pans stay readable.
    val textColor = if (hasArt) {
        Color.White.copy(alpha = 0.92f)
    } else {
        presentationTextColor(fallbackColor)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(fallbackColor),
    ) {
        AlbumArtBackground(albumArtUri = info.albumArtUri, contentDescription = info.title)
        if (hasArt) {
            // Darken art so the large uppercase pans stay legible over bright covers.
            ScrimOverlay(alpha = 0.42f)
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val lineHeight = maxHeight * 0.46f
            val fontSize = with(LocalDensity.current) {
                (lineHeight.toPx() * 0.82f).toSp()
            }
            val style = TextStyle(
                fontFamily = MetroTheme.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = (-0.5f).sp,
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                PresentationPanLine(
                    text = title.uppercase(),
                    style = style,
                    color = textColor,
                    // Song pans left → right.
                    startAtLeading = true,
                    lineHeight = lineHeight,
                    modifier = Modifier.fillMaxWidth(),
                )
                PresentationPanLine(
                    text = artist.uppercase(),
                    style = style,
                    color = textColor,
                    // Artist pans right → left.
                    startAtLeading = false,
                    lineHeight = lineHeight,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PresentationPanLine(
    text: String,
    style: TextStyle,
    color: Color,
    startAtLeading: Boolean,
    lineHeight: Dp,
    modifier: Modifier = Modifier,
) {
    if (text.isBlank()) {
        Box(modifier = modifier.height(lineHeight))
        return
    }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val measured = remember(text, style) {
        measurer.measure(text = text, style = style)
    }
    val textWidthDp = with(density) { measured.size.width.toDp() }
    BoxWithConstraints(
        modifier = modifier
            .height(lineHeight)
            .clipToBounds(),
        contentAlignment = Alignment.CenterStart,
    ) {
        val overflow = (textWidthDp - maxWidth).coerceAtLeast(maxWidth * 0.35f)
        val transition = rememberInfiniteTransition(label = "musicPresentationPan")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = PresentationPanMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "panProgress",
        )
        // startAtLeading: 0 → offset −overflow (text starts left-cropped), 1 → +overflow
        // !startAtLeading: opposite direction so the two lines cross.
        val offsetX = if (startAtLeading) {
            -overflow + overflow * 2f * progress
        } else {
            overflow - overflow * 2f * progress
        }
        TileText(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            modifier = Modifier
                .width(textWidthDp.coerceAtLeast(maxWidth + overflow * 2f))
                .offset(x = offsetX),
        )
    }
}

/** Softened accent glyph color for Presentation faces (light sage on dark green accent). */
internal fun presentationTextColor(background: Color): Color =
    Color(
        red = background.red * 0.42f + 0.58f,
        green = background.green * 0.42f + 0.58f,
        blue = background.blue * 0.42f + 0.58f,
        alpha = 1f,
    )

@Composable
private fun MusicTrackMeta(
    title: String?,
    artist: String?,
    modifier: Modifier = Modifier,
    maxTitleLines: Int = 1,
) {
    val chrome = LocalTileChrome.current
    Column(modifier = modifier) {
        if (!title.isNullOrBlank()) {
            TileText(
                text = title,
                style = chrome.liveBodyStyle,
                color = Color.White,
                maxLines = maxTitleLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!artist.isNullOrBlank()) {
            TileText(
                text = artist,
                style = chrome.liveBodyStyle,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MusicTransportRow(
    info: MusicNowPlayingInfo,
    buttonSize: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroMediaTransportButton(
            type = MetroSystemIconType.Previous,
            onClick = { MusicNowPlayingStore.skipToPrevious(info.packageName) },
            contentDescription = "Previous",
            buttonSize = buttonSize,
            color = Color.White,
            enabled = info.canSkipPrevious,
        )
        MetroMediaTransportButton(
            type = if (info.isPlaying) MetroSystemIconType.Pause else MetroSystemIconType.Play,
            onClick = { MusicNowPlayingStore.togglePlayPause(info.packageName) },
            contentDescription = if (info.isPlaying) "Pause" else "Play",
            buttonSize = buttonSize,
            color = Color.White,
            enabled = info.canPlayPause,
        )
        MetroMediaTransportButton(
            type = MetroSystemIconType.Next,
            onClick = { MusicNowPlayingStore.skipToNext(info.packageName) },
            contentDescription = "Next",
            buttonSize = buttonSize,
            color = Color.White,
            enabled = info.canSkipNext,
        )
    }
}

@Composable
private fun AlbumArtBackground(
    albumArtUri: String?,
    contentDescription: String?,
) {
    if (albumArtUri.isNullOrBlank()) return
    val context = LocalContext.current
    var bitmap by remember(albumArtUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(albumArtUri) {
        bitmap = withContext(Dispatchers.IO) {
            decodeAlbumArt(context, albumArtUri)
        }
    }
    bitmap?.let { image ->
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ScrimOverlay(alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha)),
    )
}

private fun decodeAlbumArt(context: android.content.Context, uriString: String): ImageBitmap? {
    return runCatching {
        val uri = Uri.parse(uriString)
        val stream = when (uri.scheme?.lowercase()) {
            "http", "https" -> URL(uriString).openStream()
            "file" -> uri.path?.let { File(it).inputStream() }
            else -> context.contentResolver.openInputStream(uri)
        } ?: return null
        stream.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    }.getOrNull()
}
