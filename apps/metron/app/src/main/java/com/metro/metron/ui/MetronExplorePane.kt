package com.metro.metron.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.ui.history.HistoryViewModel
import eu.kanade.tachiyomi.ui.library.LibraryViewModel
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.toast
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR

/**
 * MixRadio home mosaic proportions (content width unit = 409).
 * Bottom row sits one gutter under continue; lower satellite is right-aligned
 * under the hero/continue so the gutter to explore stays even.
 */
private const val MosaicRefWidth = 409f
private const val MosaicGutter = 13f
private const val MosaicLarge = 228f
private const val MosaicAccent = 168f
/** Larger than MixRadio’s ~97; sized so the right-column stack still clears gutters. */
private const val MosaicSmall = 108f
private const val MosaicRightX = MosaicLarge + MosaicGutter // 241
private const val MosaicContinueX = MosaicLarge - MosaicAccent // 60
private const val MosaicContinueY = MosaicLarge + MosaicGutter // 241
private const val MosaicAccentMangaY = MosaicSmall + MosaicGutter // 121
private const val MosaicSmallMidY = MosaicAccentMangaY + MosaicAccent + MosaicGutter // 302
/** Even with continue’s bottom + gutter (and with explore’s top). */
private const val MosaicBottomRowY = MosaicContinueY + MosaicAccent + MosaicGutter // 422
private const val MosaicAccentCategoryY = MosaicBottomRowY
/** Right-aligned under continue / hero — one gutter from explore. */
private const val MosaicSmallBottomX = MosaicLarge - MosaicSmall // 120
private const val MosaicHeight = MosaicBottomRowY + MosaicAccent // 590

/** Eye glyph path from the continue-tile SVG (viewBox 0 0 512 512). */
private const val ContinueEyePathData =
    "M0 226v32c128 192 384 192 512 0v-32C384 34 128 34 0 226m256 144c-70.7 0-128-57.3-128-128s57.3-128 128-128s128 57.3 128 128s-57.3 128-128 128m0-200c0-8.3 1.7-16.1 4.3-23.6c-1.5-.1-2.8-.4-4.3-.4c-53 0-96 43-96 96s43 96 96 96s96-43 96-96c0-1.5-.4-2.8-.4-4.3c-7.4 2.6-15.3 4.3-23.6 4.3c-39.8 0-72-32.2-72-72"

private val SatelliteFlipBackTitleStyle = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 15.sp,
)
private val SatelliteFlipBackChapterStyle = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 13.sp,
)

/** Hero cover title — a touch under [MetroTextStyle.ListItemTitle] (24sp). */
private val HeroTitleStyle = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 20.sp,
    lineHeight = 24.sp,
)

/** Accent action / prompt labels (continue, random manga, random category). */
private val AccentTileLabelStyle = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 22.sp,
    lineHeight = 26.sp,
)

/** Match Start live-tile flip cadence (launcher TileGrid). */
private const val SatelliteFlipHoldMs = 5_000L
private const val SatelliteFlipStaggerMaxMs = 4_000L
private const val SatelliteFlipHoldJitterMs = 1_200L
private const val SatelliteFlipCameraDistance = 16f

private val SatelliteFlipHalfAnimation = MetroTransitions.tileFlipHalfTween<Float>()
private val SatelliteFlipSettleAnimation = MetroTransitions.tileFlipSettleSpring<Float>()

private data class ExploreBook(
    val mangaId: Long,
    val title: String,
    val chapterNumber: Double?,
    val cover: MangaCover,
)

/**
 * Explore panorama pane — MixRadio-style mosaic:
 * last-read hero, continue accent, three satellite covers,
 * random manga, and random category.
 */
@Composable
fun MetronExplorePane(
    historyViewModel: HistoryViewModel,
    libraryViewModel: LibraryViewModel,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val historyState by historyViewModel.state.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()

    val historyList = historyState.list
    val libraryLoading = libraryState.isLoading
    val libraryFavorites = libraryState.libraryData.favorites
    val libraryIds = libraryFavorites.map { it.id }
    val categories = libraryState.displayedCategories

    val fromHistory = remember(historyList) {
        historyList
            ?.asSequence()
            ?.filterIsInstance<HistoryUiModel.Item>()
            ?.map { it.item }
            ?.distinctBy { it.mangaId }
            ?.take(4)
            ?.map {
                ExploreBook(
                    mangaId = it.mangaId,
                    title = it.title,
                    chapterNumber = it.chapterNumber.takeIf { n -> n >= 0 },
                    cover = it.coverData,
                )
            }
            ?.toList()
            .orEmpty()
    }
    val randomFallback = remember(libraryIds) {
        libraryFavorites.randomOrNull()?.let {
            ExploreBook(
                mangaId = it.libraryManga.manga.id,
                title = it.libraryManga.manga.title,
                chapterNumber = null,
                cover = it.libraryManga.manga.asMangaCover(),
            )
        }
    }
    val books = when {
        fromHistory.isNotEmpty() -> fromHistory
        randomFallback != null -> listOf(randomFallback)
        else -> emptyList()
    }
    val loading = historyList == null || libraryLoading

    when {
        loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MetroLoadingDots()
            }
        }
        books.isEmpty() -> {
            MetroEmptyState(
                message = "maybe you should read something :)",
                modifier = modifier,
            )
        }
        else -> {
            val lastRead = books.first()
            val satellites = books.drop(1).take(3)
            ExploreMixRadioMosaic(
                lastRead = lastRead,
                satellites = satellites,
                onOpenBook = { navigator.push(MangaScreen(it.mangaId)) },
                onContinueReading = {
                    scope.launch {
                        val chapter = historyViewModel.getNextChapter()
                        if (chapter != null) {
                            context.startActivity(
                                ReaderActivity.newIntent(context, chapter.mangaId, chapter.id),
                            )
                        } else {
                            context.toast(MR.strings.no_next_chapter)
                        }
                    }
                },
                onRandomManga = {
                    libraryFavorites.randomOrNull()?.let {
                        navigator.push(MangaScreen(it.libraryManga.manga.id))
                    }
                },
                onRandomCategory = {
                    val nonDefault = categories.filter { !it.isSystemCategory }
                    val target = nonDefault.randomOrNull()
                        ?: categories.firstOrNull()
                    if (target != null) {
                        navigator.push(MetronLibraryCategoryScreen(target.id))
                    } else {
                        navigator.push(MetronLibraryCategoryScreen(0L))
                    }
                },
                modifier = modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ExploreMixRadioMosaic(
    lastRead: ExploreBook,
    satellites: List<ExploreBook>,
    onOpenBook: (ExploreBook) -> Unit,
    onContinueReading: () -> Unit,
    onRandomManga: () -> Unit,
    onRandomCategory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
            .padding(top = 24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        fun ref(v: Float): Dp = maxWidth * (v / MosaicRefWidth)
        val large = ref(MosaicLarge)
        val accent = ref(MosaicAccent)
        val small = ref(MosaicSmall)
        val mosaicHeight = ref(MosaicHeight)

        // Absolute positions from MixRadio reference (origin = top-left of mosaic).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(mosaicHeight),
        ) {
            // 1 — last-read hero (top-left large cover)
            ExploreCoverTile(
                title = lastRead.title,
                chapterNumber = lastRead.chapterNumber,
                cover = lastRead.cover,
                onClick = { onOpenBook(lastRead) },
                modifier = Modifier
                    .size(large)
                    .offset(x = 0.dp, y = 0.dp),
            )

            // 2 — satellite covers (live-tile flip: cover ↔ title/chapter)
            satellites.getOrNull(0)?.let { book ->
                ExploreFlippingSatelliteTile(
                    book = book,
                    flipSeed = book.mangaId.hashCode() xor 1,
                    onClick = { onOpenBook(book) },
                    modifier = Modifier
                        .size(small)
                        .offset(x = ref(MosaicRightX), y = ref(0f)),
                )
            }
            satellites.getOrNull(1)?.let { book ->
                ExploreFlippingSatelliteTile(
                    book = book,
                    flipSeed = book.mangaId.hashCode() xor 2,
                    onClick = { onOpenBook(book) },
                    modifier = Modifier
                        .size(small)
                        .offset(x = ref(MosaicRightX), y = ref(MosaicSmallMidY)),
                )
            }
            satellites.getOrNull(2)?.let { book ->
                ExploreFlippingSatelliteTile(
                    book = book,
                    flipSeed = book.mangaId.hashCode() xor 3,
                    onClick = { onOpenBook(book) },
                    modifier = Modifier
                        .size(small)
                        .offset(x = ref(MosaicSmallBottomX), y = ref(MosaicBottomRowY)),
                )
            }

            // 3 — continue (MixRadio "play me")
            ExploreAccentActionTile(
                label = "continue",
                onClick = onContinueReading,
                modifier = Modifier
                    .size(accent)
                    .offset(x = ref(MosaicContinueX), y = ref(MosaicContinueY)),
                icon = { color, size -> ExploreContinueEyeIcon(color = color, size = size) },
            )

            // 4 — random manga (MixRadio "play music crafted…")
            ExploreAccentTextTile(
                label = "read a random manga",
                onClick = onRandomManga,
                modifier = Modifier
                    .size(accent)
                    .offset(x = ref(MosaicRightX), y = ref(MosaicAccentMangaY)),
            )

            // 5 — random category (MixRadio "take mixes offline…")
            ExploreAccentTextTile(
                label = "explore a random category",
                onClick = onRandomCategory,
                modifier = Modifier
                    .size(accent)
                    .offset(x = ref(MosaicRightX), y = ref(MosaicAccentCategoryY)),
            )
        }
    }
}

/**
 * Live-tile satellite: cover on the front, title + chapter on the accent back face.
 * Flip cadence matches Start ([MetroTransitions] 600ms turnstile).
 */
@Composable
private fun ExploreFlippingSatelliteTile(
    book: ExploreBook,
    flipSeed: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val rotation = remember(flipSeed) { Animatable(0f) }
    var showingBack by remember(flipSeed) { mutableStateOf(false) }
    val chapterLabel = book.chapterNumber?.let { formatChapterNumber(it) }
    val a11y = if (chapterLabel != null) "${book.title}, $chapterLabel" else book.title
    val accent = MetroTheme.colors.accent
    val backContent = MetroColors.tileContentColor(accent)

    LaunchedEffect(flipSeed) {
        if (abs(rotation.value) > 0.01f) {
            rotation.snapTo(0f)
            showingBack = false
        }
        val rng = Random(flipSeed)
        delay(rng.nextLong(0L, SatelliteFlipStaggerMaxMs + 1))
        while (true) {
            val jitter = rng.nextLong(-SatelliteFlipHoldJitterMs, SatelliteFlipHoldJitterMs + 1)
            delay((SatelliteFlipHoldMs + jitter).coerceAtLeast(2_500L))
            if (showingBack) {
                rotation.animateTo(-90f, animationSpec = SatelliteFlipHalfAnimation)
                showingBack = false
                rotation.snapTo(90f)
                rotation.animateTo(0f, animationSpec = SatelliteFlipSettleAnimation)
            } else {
                rotation.animateTo(90f, animationSpec = SatelliteFlipHalfAnimation)
                showingBack = true
                rotation.snapTo(-90f)
                rotation.animateTo(0f, animationSpec = SatelliteFlipSettleAnimation)
            }
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .graphicsLayer {
                rotationX = rotation.value
                transformOrigin = TransformOrigin(0.5f, 0.5f)
                cameraDistance = SatelliteFlipCameraDistance * density
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = a11y },
    ) {
        if (showingBack) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accent, RectangleShape),
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .fillMaxWidth(),
                ) {
                    BasicText(
                        text = book.title,
                        style = SatelliteFlipBackTitleStyle.copy(
                            color = backContent,
                            fontFamily = MetroTheme.fontFamily,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (chapterLabel != null) {
                        BasicText(
                            text = chapterLabel,
                            style = SatelliteFlipBackChapterStyle.copy(
                                color = backContent.copy(alpha = 0.85f),
                                fontFamily = MetroTheme.fontFamily,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MetroTheme.colors.secondarySurface, RectangleShape),
            ) {
                AsyncImage(
                    model = book.cover,
                    contentDescription = null,
                    placeholder = ColorPainter(Color(0x1F888888)),
                    error = ColorPainter(Color(0x1F888888)),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ExploreCoverTile(
    title: String,
    chapterNumber: Double?,
    cover: MangaCover,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chapterLabel = chapterNumber?.let { formatChapterNumber(it) }
    val a11y = if (chapterLabel != null) "$title, $chapterLabel" else title
    Box(
        modifier = modifier
            .background(MetroTheme.colors.secondarySurface, RectangleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = a11y },
    ) {
        AsyncImage(
            model = cover,
            contentDescription = null,
            placeholder = ColorPainter(Color(0x1F888888)),
            error = ColorPainter(Color(0x1F888888)),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.28f to Color.Transparent,
                        0.58f to Color.Black.copy(alpha = 0.55f),
                        1f to Color.Black.copy(alpha = 0.88f),
                    ),
                ),
        )
        ExploreTitleChapterRow(
            title = title,
            chapterLabel = chapterLabel,
            titleStyle = HeroTitleStyle.copy(
                color = Color.White,
                fontFamily = MetroTheme.fontFamily,
            ),
            chapterStyle = HeroTitleStyle.copy(
                color = Color.White.copy(alpha = 0.85f),
                fontFamily = MetroTheme.fontFamily,
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .fillMaxWidth(),
        )
    }
}

/** Bottom bar: title start, chapter number end (no "ch." prefix). */
@Composable
private fun ExploreTitleChapterRow(
    title: String,
    chapterLabel: String?,
    titleStyle: TextStyle,
    chapterStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicText(
            text = title,
            style = titleStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (chapterLabel != null) {
            Spacer(Modifier.width(8.dp))
            BasicText(
                text = chapterLabel,
                style = chapterStyle,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun ExploreAccentTextTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MetroTheme.colors.accent
    val content = MetroColors.tileContentColor(accent)
    Box(
        modifier = modifier
            .background(accent, RectangleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
    ) {
        BasicText(
            text = label,
            style = AccentTileLabelStyle.copy(
                color = content,
                fontFamily = MetroTheme.fontFamily,
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .fillMaxWidth(0.92f),
        )
    }
}

@Composable
private fun ExploreAccentActionTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (Color, Dp) -> Unit,
) {
    val accent = MetroTheme.colors.accent
    val content = MetroColors.tileContentColor(accent)
    Box(
        modifier = modifier
            .background(accent, RectangleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
    ) {
        // True H/V center of the tile (label overlays bottom, does not shift the glyph).
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            icon(content, 72.dp)
        }
        BasicText(
            text = label,
            style = AccentTileLabelStyle.copy(
                color = content,
                fontFamily = MetroTheme.fontFamily,
                textAlign = TextAlign.Start,
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        )
    }
}

/**
 * Eye glyph for the continue tile. Drawn in the SVG’s 512×512 viewBox, then
 * uniformly scaled and centered in [size] so it sits dead-center H and V.
 */
@Composable
private fun ExploreContinueEyeIcon(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val path = remember {
        PathParser().parsePathString(ContinueEyePathData).toPath()
    }
    Canvas(modifier = modifier.size(size)) {
        val view = 512f
        val scaleFactor = minOf(this.size.width, this.size.height) / view
        val dx = (this.size.width - view * scaleFactor) / 2f
        val dy = (this.size.height - view * scaleFactor) / 2f
        translate(left = dx, top = dy) {
            scale(scale = scaleFactor, pivot = Offset.Zero) {
                drawPath(path = path, color = color)
            }
        }
    }
}
