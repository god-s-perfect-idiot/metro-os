package com.metro.launcher.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.system.MetroPreferences
import com.metro.system.MetroTileGridCell
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTransitions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Slow Ken-Burns pan while a photo is on-screen (WP8.1 Photos live tile). */
private const val CYCLE_PAN_MS = 3_000
/** Vertical wipe that carries the current photo out and the next in from below. */
private const val CYCLE_SLIDE_MS = 600
/** Extra height fraction so the image can drift upward without empty edges. */
private const val CYCLE_PAN_OVERFLOW = 0.18f

@Composable
fun PhotoGridTileContent(
    cells: List<MetroTileGridCell>,
    columns: Int,
    rows: Int,
    title: String,
    modifier: Modifier = Modifier,
) {
    val displayCells = if (cells.size >= columns * rows) {
        cells.take(columns * rows)
    } else {
        cells + List(columns * rows - cells.size) { MetroTileGridCell() }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val left = maxWidth * col / columns
                val top = maxHeight * row / rows
                val right = maxWidth * (col + 1) / columns
                val bottom = maxHeight * (row + 1) / rows
                PhotoGridCell(
                    cell = displayCells[row * columns + col],
                    modifier = Modifier
                        .offset(x = left, y = top)
                        .size(width = right - left, height = bottom - top),
                )
            }
        }
        MetroText(
            text = title,
            style = MetroTextStyle.Body,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}

/**
 * WP8.1 Photos-tile style: each photo slowly pans up for [CYCLE_PAN_MS], then slides up as
 * the next photo enters from below. Color-only fallback cells are ignored.
 */
@Composable
fun CyclingPhotoTileContent(
    cells: List<MetroTileGridCell>,
    title: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val accent = MetroPreferences(context).accentColor
    val photoCells = remember(cells) { cells.filter { !it.imageUri.isNullOrBlank() } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(accent),
    ) {
        if (photoCells.isNotEmpty()) {
            var index by remember(photoCells) { mutableIntStateOf(0) }
            LaunchedEffect(photoCells) {
                if (photoCells.size <= 1) return@LaunchedEffect
                while (true) {
                    delay(CYCLE_PAN_MS.toLong())
                    index = (index + 1) % photoCells.size
                    delay(CYCLE_SLIDE_MS.toLong())
                }
            }
            AnimatedContent(
                targetState = index.coerceIn(0, photoCells.lastIndex),
                transitionSpec = {
                    slideInVertically(
                        animationSpec = tween(
                            durationMillis = CYCLE_SLIDE_MS,
                            easing = MetroTransitions.PageEasing,
                        ),
                        initialOffsetY = { height -> height },
                    ) togetherWith slideOutVertically(
                        animationSpec = tween(
                            durationMillis = CYCLE_SLIDE_MS,
                            easing = MetroTransitions.PageEasing,
                        ),
                        targetOffsetY = { height -> -height },
                    )
                },
                label = "photoTileCycle",
            ) { currentIndex ->
                PanningPhotoCell(
                    cell = photoCells[currentIndex],
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        MetroText(
            text = title,
            style = MetroTextStyle.Body,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}

/** Draws [cell] oversized and drifts it upward over [CYCLE_PAN_MS]. */
@Composable
private fun PanningPhotoCell(
    cell: MetroTileGridCell,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val background = cell.colorHex?.let { MetroPreferences.parseAccentHex(it) }
        ?: MetroPreferences(context).accentColor
    var bitmap by remember(cell.imageUri) { mutableStateOf<ImageBitmap?>(null) }
    val panProgress = remember(cell.imageUri) { Animatable(0f) }

    LaunchedEffect(cell.imageUri) {
        val uri = cell.imageUri
        bitmap = if (uri.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    LaunchedEffect(cell.imageUri, bitmap) {
        if (bitmap == null) return@LaunchedEffect
        panProgress.snapTo(0f)
        panProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = CYCLE_PAN_MS,
                easing = LinearEasing,
            ),
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .background(background),
        contentAlignment = Alignment.TopCenter,
    ) {
        val image = bitmap
        if (image != null) {
            val overflow = maxHeight * CYCLE_PAN_OVERFLOW
            val imageHeight = maxHeight + overflow
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(maxWidth)
                    .height(imageHeight)
                    .offset(y = -overflow * panProgress.value),
            )
        }
    }
}

@Composable
private fun PhotoGridCell(
    cell: MetroTileGridCell,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val background = cell.colorHex?.let { MetroPreferences.parseAccentHex(it) }
        ?: MetroPreferences(context).accentColor
    var bitmap by remember(cell.imageUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(cell.imageUri) {
        val uri = cell.imageUri
        bitmap = if (uri.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    Box(
        modifier = modifier.background(background),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Full-bleed static photo face (People contact tiles). No Ken Burns / cycle motion.
 * Optional [title] overlays bottom-left like other live photo faces.
 */
@Composable
fun StaticPhotoTileContent(
    imageUri: String,
    fallbackColor: Color,
    title: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember(imageUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(imageUri) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(imageUri))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(fallbackColor),
    ) {
        bitmap?.let { image ->
            Image(
                bitmap = image,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (!title.isNullOrBlank()) {
            MetroText(
                text = title,
                style = MetroTextStyle.Body,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}
