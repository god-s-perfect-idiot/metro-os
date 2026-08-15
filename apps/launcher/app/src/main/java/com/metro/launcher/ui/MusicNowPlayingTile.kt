package com.metro.launcher.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.launcher.data.MusicNowPlayingInfo
import com.metro.launcher.data.MusicNowPlayingStore
import com.metro.launcher.data.PinnedTileSize
import com.metro.ui.MetroMediaGlyph
import com.metro.ui.MetroMediaTransportButton
import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Circular transport controls on live tiles — same ring style as Music now playing, scaled down. */
private val TileTransportSmall = 40.dp
private val TileTransportMedium = 34.dp
private val TileTransportWide = 36.dp

/**
 * Xbox Music–style now-playing Start face.
 *
 * - 1×1: album art + centered play/pause only
 * - 2×2: full-bleed art, song + artist, prev / play-pause / next
 * - 4×2: art panel + metadata + the same transport row (wide Xbox Music layout)
 */
@Composable
fun MusicNowPlayingTileContent(
    info: MusicNowPlayingInfo,
    size: PinnedTileSize,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
) {
    when (size) {
        PinnedTileSize.OneByOne -> MusicSmallNowPlayingFace(
            info = info,
            fallbackColor = fallbackColor,
            modifier = modifier,
        )
        PinnedTileSize.TwoByTwo -> MusicMediumNowPlayingFace(
            info = info,
            fallbackColor = fallbackColor,
            modifier = modifier,
        )
        PinnedTileSize.FourByTwo -> MusicWideNowPlayingFace(
            info = info,
            fallbackColor = fallbackColor,
            modifier = modifier,
        )
    }
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
            glyph = if (info.isPlaying) MetroMediaGlyph.Pause else MetroMediaGlyph.Play,
            onClick = { MusicNowPlayingStore.togglePlayPause(info.packageName) },
            contentDescription = if (info.isPlaying) "Pause" else "Play",
            buttonSize = TileTransportSmall,
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
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            MusicTrackMeta(
                title = info.title,
                artist = info.artist,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(modifier = Modifier.height(6.dp))
            MusicTransportRow(
                info = info,
                buttonSize = TileTransportMedium,
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
                .padding(horizontal = 10.dp, vertical = 8.dp),
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
                buttonSize = TileTransportWide,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

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
            glyph = MetroMediaGlyph.Previous,
            onClick = { MusicNowPlayingStore.skipToPrevious(info.packageName) },
            contentDescription = "Previous",
            buttonSize = buttonSize,
            color = Color.White,
            enabled = info.canSkipPrevious,
        )
        MetroMediaTransportButton(
            glyph = if (info.isPlaying) MetroMediaGlyph.Pause else MetroMediaGlyph.Play,
            onClick = { MusicNowPlayingStore.togglePlayPause(info.packageName) },
            contentDescription = if (info.isPlaying) "Pause" else "Play",
            buttonSize = buttonSize,
            color = Color.White,
            enabled = info.canPlayPause,
        )
        MetroMediaTransportButton(
            glyph = MetroMediaGlyph.Next,
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
