package com.metro.metron.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import eu.kanade.presentation.history.HistoryUiModel
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.ui.history.HistoryViewModel
import eu.kanade.tachiyomi.ui.library.LibraryViewModel
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.model.asMangaCover

private val ExploreTileGutter = 12.dp

private data class ExploreBook(
    val mangaId: Long,
    val title: String,
    val chapterNumber: Double?,
    val cover: MangaCover,
)

/**
 * Explore panorama pane — latest unique reads as oversized cover tiles.
 * Falls back to one random library book, then an empty placeholder.
 */
@Composable
fun MetronExplorePane(
    historyViewModel: HistoryViewModel,
    libraryViewModel: LibraryViewModel,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.currentOrThrow
    val historyState by historyViewModel.state.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()

    val historyList = historyState.list
    val libraryLoading = libraryState.isLoading
    val libraryFavorites = libraryState.libraryData.favorites
    val libraryIds = libraryFavorites.map { it.id }

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
            ExploreTileGrid(
                books = books,
                onClick = { navigator.push(MangaScreen(it.mangaId)) },
                modifier = modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ExploreTileGrid(
    books: List<ExploreBook>,
    onClick: (ExploreBook) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
            .padding(top = 24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // Full-bleed squares — larger than normal library/browse 2-up half-width tiles.
        val tileSize = maxWidth
        Column(verticalArrangement = Arrangement.spacedBy(ExploreTileGutter)) {
            books.forEach { book ->
                ExploreCoverTile(
                    title = book.title,
                    chapterNumber = book.chapterNumber,
                    cover = book.cover,
                    onClick = { onClick(book) },
                    modifier = Modifier.size(tileSize),
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
    val chapterLabel = chapterNumber?.let { "ch. ${formatChapterNumber(it)}" }
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
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.78f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .fillMaxWidth(),
        ) {
            MetroText(
                text = title,
                style = MetroTextStyle.ListItemTitle,
                color = Color.White,
                maxLines = if (chapterLabel != null) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (chapterLabel != null) {
                MetroText(
                    text = chapterLabel,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
