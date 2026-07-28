package com.metro.launcher.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.launcher.data.MusicNowPlayingInfo
import com.metro.launcher.data.MusicNowPlayingStore
import com.metro.launcher.data.PinnedTileSize
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        MusicTransportIcon(
            type = if (info.isPlaying) MusicTransportGlyph.Pause else MusicTransportGlyph.Play,
            onClick = { MusicNowPlayingStore.togglePlayPause(info.packageName) },
            enabled = info.canPlayPause,
            contentDescription = if (info.isPlaying) "Pause" else "Play",
            size = 36.dp,
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
                iconSize = 22.dp,
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
                iconSize = 24.dp,
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
    Column(modifier = modifier) {
        if (!title.isNullOrBlank()) {
            MetroText(
                text = title,
                style = MetroTextStyle.Body,
                color = Color.White,
                maxLines = maxTitleLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!artist.isNullOrBlank()) {
            MetroText(
                text = artist,
                style = MetroTextStyle.ListItemSubtitle,
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
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusicTransportIcon(
            type = MusicTransportGlyph.Previous,
            onClick = { MusicNowPlayingStore.skipToPrevious(info.packageName) },
            enabled = info.canSkipPrevious,
            contentDescription = "Previous",
            size = iconSize,
        )
        MusicTransportIcon(
            type = if (info.isPlaying) MusicTransportGlyph.Pause else MusicTransportGlyph.Play,
            onClick = { MusicNowPlayingStore.togglePlayPause(info.packageName) },
            enabled = info.canPlayPause,
            contentDescription = if (info.isPlaying) "Pause" else "Play",
            size = iconSize + 4.dp,
        )
        MusicTransportIcon(
            type = MusicTransportGlyph.Next,
            onClick = { MusicNowPlayingStore.skipToNext(info.packageName) },
            enabled = info.canSkipNext,
            contentDescription = "Next",
            size = iconSize,
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

private enum class MusicTransportGlyph {
    Play,
    Pause,
    Previous,
    Next,
}

@Composable
private fun MusicTransportIcon(
    type: MusicTransportGlyph,
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    size: Dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(size + 12.dp)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val color = if (enabled) Color.White else Color.White.copy(alpha = 0.35f)
            when (type) {
                MusicTransportGlyph.Play -> drawPlayGlyph(color)
                MusicTransportGlyph.Pause -> drawPauseGlyph(color)
                MusicTransportGlyph.Previous -> drawSkipGlyph(color, forward = false)
                MusicTransportGlyph.Next -> drawSkipGlyph(color, forward = true)
            }
        }
    }
}

private fun DrawScope.drawPlayGlyph(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.28f, h * 0.18f)
        lineTo(w * 0.28f, h * 0.82f)
        lineTo(w * 0.82f, h * 0.50f)
        close()
    }
    drawPath(path, color = color)
}

private fun DrawScope.drawPauseGlyph(color: Color) {
    val w = size.width
    val h = size.height
    val barW = w * 0.18f
    val gap = w * 0.14f
    val left = w * 0.28f
    val top = h * 0.18f
    val bottom = h * 0.82f
    drawRect(color = color, topLeft = Offset(left, top), size = GeometrySize(barW, bottom - top))
    drawRect(
        color = color,
        topLeft = Offset(left + barW + gap, top),
        size = GeometrySize(barW, bottom - top),
    )
}

private fun DrawScope.drawSkipGlyph(color: Color, forward: Boolean) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = size.minDimension * 0.12f)
    if (forward) {
        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.20f)
            lineTo(w * 0.18f, h * 0.80f)
            lineTo(w * 0.62f, h * 0.50f)
            close()
        }
        drawPath(path, color = color)
        drawLine(
            color = color,
            start = Offset(w * 0.72f, h * 0.20f),
            end = Offset(w * 0.72f, h * 0.80f),
            strokeWidth = stroke.width,
        )
    } else {
        val path = Path().apply {
            moveTo(w * 0.82f, h * 0.20f)
            lineTo(w * 0.82f, h * 0.80f)
            lineTo(w * 0.38f, h * 0.50f)
            close()
        }
        drawPath(path, color = color)
        drawLine(
            color = color,
            start = Offset(w * 0.28f, h * 0.20f),
            end = Offset(w * 0.28f, h * 0.80f),
            strokeWidth = stroke.width,
        )
    }
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
