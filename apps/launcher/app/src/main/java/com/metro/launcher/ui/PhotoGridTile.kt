package com.metro.launcher.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.system.MetroPreferences
import com.metro.system.MetroTileGridCell
import com.metro.ui.MetroTransitions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** Slow Ken-Burns pan while a photo is on-screen (WP8.1 Photos live tile). */
private const val CYCLE_PAN_MS = 3_000
/** Vertical wipe that carries the current photo out and the next in from below. */
private const val CYCLE_SLIDE_MS = 600
/** Extra scale so the cropped photo can drift upward without empty edges. */
private const val CYCLE_PAN_OVERFLOW = 0.18f

/** Pick a different cell at random so the Photos live tile never walks library order. */
internal fun nextRandomCycleIndex(
    current: Int,
    size: Int,
    random: Random = Random.Default,
): Int {
    if (size <= 1) return 0
    var next: Int
    do {
        next = random.nextInt(size)
    } while (next == current)
    return next
}

/** How long a People mosaic cell stays before the next flip attempt. */
private const val MOSAIC_FLIP_HOLD_MS = 3_200L
private const val MOSAIC_FLIP_HOLD_JITTER_MS = 1_400L
private const val MOSAIC_FLIP_STAGGER_MAX_MS = 2_500L
private const val MOSAIC_FLIP_CAMERA_DISTANCE = 16f

private val MosaicFlipHalfAnimation = MetroTransitions.tileFlipHalfTween<Float>()
private val MosaicFlipSettleAnimation = MetroTransitions.tileFlipSettleSpring<Float>()

/** Survives enter-wave recomposition so People/Photos faces do not flash empty. */
private val tilePhotoBitmapCache = object : LruCache<String, ImageBitmap>(64) {}

private fun decodeTilePhoto(context: Context, uriString: String): ImageBitmap? {
    tilePhotoBitmapCache.get(uriString)?.let { return it }
    val decoded = runCatching {
        context.contentResolver.openInputStream(Uri.parse(uriString))?.use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull()
    if (decoded != null) tilePhotoBitmapCache.put(uriString, decoded)
    return decoded
}

@Composable
private fun rememberTilePhotoBitmap(imageUri: String?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(imageUri) {
        mutableStateOf(imageUri?.let { tilePhotoBitmapCache.get(it) })
    }
    LaunchedEffect(imageUri) {
        val uri = imageUri
        if (uri.isNullOrBlank()) {
            bitmap = null
            return@LaunchedEffect
        }
        val cached = tilePhotoBitmapCache.get(uri)
        if (cached != null) {
            bitmap = cached
            return@LaunchedEffect
        }
        val decoded = withContext(Dispatchers.IO) { decodeTilePhoto(context, uri) }
        if (decoded != null) bitmap = decoded
    }
    return bitmap
}

private fun Modifier.tilePhotoLayer(): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}

/**
 * WP8.1 People hub mosaic (3×3 on medium, 6×3 on wide): live flip refresh on each sub-tile.
 * At most [PhotoGridLiveLogic.MAX_VISIBLE_CONTACTS] cells show contacts; the rest are
 * accent color. When [animate] is false (edit mode), the layout stays static.
 */
@Composable
fun PhotoGridTileContent(
    cells: List<MetroTileGridCell>,
    columns: Int,
    rows: Int,
    title: String,
    animate: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val cellCount = columns * rows
    val pool = remember(cells) { PhotoGridLiveLogic.contactPool(cells) }
    val accents = remember(cells) { PhotoGridLiveLogic.accentTemplates(cells) }
    val poolKeys = remember(cells) { PhotoGridLiveLogic.poolIdentity(cells) }
    val poolState = rememberUpdatedState(pool)
    val accentsState = rememberUpdatedState(accents)
    val seed = remember(poolKeys, cellCount) {
        poolKeys.hashCode() * 31 + cellCount
    }
    var displayCells by remember(poolKeys, cellCount) {
        mutableStateOf(
            PhotoGridLiveLogic.initialLayout(pool, accents, cellCount, Random(seed.toLong())),
        )
    }

    LaunchedEffect(poolKeys, cellCount, animate) {
        if (!animate || poolState.value.isEmpty()) return@LaunchedEffect
        val rng = Random(seed.toLong() xor System.nanoTime())
        delay(rng.nextLong(0L, MOSAIC_FLIP_STAGGER_MAX_MS + 1))
        var ticks = 0
        while (true) {
            val jitter = rng.nextLong(-MOSAIC_FLIP_HOLD_JITTER_MS, MOSAIC_FLIP_HOLD_JITTER_MS + 1)
            delay((MOSAIC_FLIP_HOLD_MS + jitter).coerceAtLeast(1_800L))
            val currentPool = poolState.value
            val currentAccents = accentsState.value
            if (currentPool.isEmpty()) continue

            // Every few ticks, rebuild a fresh random 4-from-pool layout so the mosaic
            // cannot stick on the same faces even if single-cell swaps stall.
            ticks += 1
            if (ticks % 5 == 0 && currentPool.size > PhotoGridLiveLogic.MAX_VISIBLE_CONTACTS) {
                val fresh = PhotoGridLiveLogic.initialLayout(
                    currentPool,
                    currentAccents,
                    cellCount,
                    rng,
                )
                displayCells = fresh
                continue
            }

            val flip = PhotoGridLiveLogic.nextFlip(
                displayCells,
                currentPool,
                currentAccents,
                rng,
            ) ?: continue
            val (index, newCell) = flip
            if (PhotoGridLiveLogic.contactKey(displayCells.getOrNull(index) ?: MetroTileGridCell()) ==
                PhotoGridLiveLogic.contactKey(newCell)
            ) {
                continue
            }
            displayCells = displayCells.toMutableList().also { it[index] = newCell }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .tilePhotoLayer(),
    ) {
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val index = row * columns + col
                val left = maxWidth * col / columns
                val top = maxHeight * row / rows
                val right = maxWidth * (col + 1) / columns
                val bottom = maxHeight * (row + 1) / rows
                key(index) {
                    FlippingPhotoGridCell(
                        cell = displayCells.getOrElse(index) { MetroTileGridCell() },
                        animate = animate,
                        flipSeed = seed + index * 17,
                        modifier = Modifier
                            .offset(x = left, y = top)
                            .size(width = right - left, height = bottom - top),
                    )
                }
            }
        }
        TileText(
            text = title,
            style = LocalTileChrome.current.titleStyle,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    horizontal = LocalTileChrome.current.titlePaddingH,
                    vertical = LocalTileChrome.current.titlePaddingV,
                ),
        )
    }
}

/**
 * WP8.1 Photos-tile style: each photo slowly pans up for [CYCLE_PAN_MS], then slides up as
 * the next photo enters from below. Color-only fallback cells are ignored.
 * Photos advance in random order (never sequential library / date order).
 */
@Composable
fun CyclingPhotoTileContent(
    cells: List<MetroTileGridCell>,
    title: String,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val context = LocalContext.current
    val accent = MetroPreferences(context).accentColor
    val photoCells = remember(cells) {
        cells.filter { !it.imageUri.isNullOrBlank() }.shuffled()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .tilePhotoLayer()
            .background(accent),
    ) {
        if (photoCells.isNotEmpty()) {
            var index by remember(photoCells) {
                mutableIntStateOf(Random.nextInt(photoCells.size))
            }
            LaunchedEffect(photoCells, animate) {
                if (!animate || photoCells.size <= 1) return@LaunchedEffect
                while (true) {
                    delay(CYCLE_PAN_MS.toLong())
                    index = nextRandomCycleIndex(index, photoCells.size)
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
                    animate = animate,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        TileText(
            text = title,
            style = LocalTileChrome.current.titleStyle,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    horizontal = LocalTileChrome.current.titlePaddingH,
                    vertical = LocalTileChrome.current.titlePaddingV,
                ),
        )
    }
}

/**
 * Draws [cell] full-bleed (Crop) then zooms slightly and drifts upward over [CYCLE_PAN_MS].
 * Scale+translate (not a taller Fit/height layout) so the tile stays filled with photo
 * pixels — no accent strip below while panning.
 */
@Composable
private fun PanningPhotoCell(
    cell: MetroTileGridCell,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val context = LocalContext.current
    val background = cell.colorHex?.let { MetroPreferences.parseAccentHex(it) }
        ?: MetroPreferences(context).accentColor
    val bitmap = rememberTilePhotoBitmap(cell.imageUri)
    val panProgress = remember(cell.imageUri) { Animatable(0f) }

    LaunchedEffect(cell.imageUri, bitmap, animate) {
        if (!animate || bitmap == null) return@LaunchedEffect
        if (panProgress.value >= 1f) panProgress.snapTo(0f)
        val remaining = ((1f - panProgress.value) * CYCLE_PAN_MS).toInt().coerceAtLeast(1)
        panProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = remaining,
                easing = LinearEasing,
            ),
        )
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .background(background),
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = 1f + CYCLE_PAN_OVERFLOW
                        scaleX = scale
                        scaleY = scale
                        // Extra pixels hang below the top edge; pan consumes that overflow.
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        translationY = -size.height * CYCLE_PAN_OVERFLOW * panProgress.value
                    },
            )
        }
    }
}

/**
 * 600ms vertical flip when [cell] content changes. Mid-flip the Start black behind the cell
 * shows through (accent/photo rides on the rotating face).
 */
@Composable
private fun FlippingPhotoGridCell(
    cell: MetroTileGridCell,
    animate: Boolean,
    flipSeed: Int,
    modifier: Modifier = Modifier,
) {
    var displayed by remember(flipSeed) { mutableStateOf(cell) }
    val rotation = remember(flipSeed) { Animatable(0f) }
    val density = LocalDensity.current.density

    LaunchedEffect(cell, animate) {
        if (cell == displayed) return@LaunchedEffect
        if (!animate) {
            displayed = cell
            rotation.snapTo(0f)
            return@LaunchedEffect
        }
        rotation.animateTo(90f, animationSpec = MosaicFlipHalfAnimation)
        displayed = cell
        rotation.snapTo(-90f)
        rotation.animateTo(0f, animationSpec = MosaicFlipSettleAnimation)
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .graphicsLayer {
                rotationX = rotation.value
                transformOrigin = TransformOrigin(0.5f, 0.5f)
                cameraDistance = MOSAIC_FLIP_CAMERA_DISTANCE * density
            },
    ) {
        PhotoGridCellFace(
            cell = displayed,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PhotoGridCellFace(
    cell: MetroTileGridCell,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val background = cell.colorHex?.let { MetroPreferences.parseAccentHex(it) }
        ?: MetroPreferences(context).accentColor
    val bitmap = rememberTilePhotoBitmap(cell.imageUri)

    Box(
        modifier = modifier.background(background),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        val label = cell.label
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (!label.isNullOrBlank()) {
            TileText(
                text = label,
                style = TileTextStyles.Title,
                color = Color.White,
                maxLines = 1,
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
    val bitmap = rememberTilePhotoBitmap(imageUri)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .tilePhotoLayer()
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
            val chrome = LocalTileChrome.current
            TileText(
                text = title,
                style = chrome.titleStyle,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        horizontal = chrome.titlePaddingH,
                        vertical = chrome.titlePaddingV,
                    ),
            )
        }
    }
}
