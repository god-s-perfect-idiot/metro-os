package com.metro.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.metro.news.data.NewsCategory
import com.metro.news.data.NewsStory
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroLoadingDots
import com.metro.ui.MetroPanorama
import com.metro.ui.MetroPanoramaBodyEnter
import com.metro.ui.MetroPanoramaBrandEnter
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import kotlin.math.roundToInt

private const val NewsBrandText = "news"
private val NewsBrandInset = 12.dp

private val NewsBrandStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 96.sp,
    lineHeight = 96.sp,
    letterSpacing = (-1).sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Composable
fun NewsPanorama(
    state: NewsState,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    skipIntro: Boolean = false,
    onIntroPlayed: () -> Unit = {},
) {
    val density = LocalDensity.current

    DisposableEffect(Unit) {
        onDispose { onIntroPlayed() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        MetroPanoramaBrandEnter(skipEnter = skipIntro) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(bottom = 4.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                val measurer = rememberTextMeasurer()
                val availableWidthPx = with(density) { (maxWidth - NewsBrandInset).toPx() }
                val fontFamily = MetroTheme.fontFamily
                val brandWidthPx = remember(measurer, density, fontFamily) {
                    measurer.measure(
                        text = NewsBrandText,
                        style = NewsBrandStyle.copy(fontFamily = fontFamily),
                        softWrap = false,
                        maxLines = 1,
                        density = density,
                    ).size.width.toFloat()
                }
                val hiddenPx = (brandWidthPx - availableWidthPx).coerceAtLeast(0f)
                val lastPage = (pagerState.pageCount - 1).coerceAtLeast(1)
                val progress = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    .coerceIn(0f, lastPage.toFloat()) / lastPage
                val brandOffsetPx = (progress * hiddenPx).roundToInt()

                BasicText(
                    text = NewsBrandText,
                    style = NewsBrandStyle.copy(
                        fontFamily = MetroTheme.fontFamily,
                        color = MetroTheme.colors.primaryText,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .offset { IntOffset(-brandOffsetPx, 0) }
                        .padding(start = NewsBrandInset)
                        .wrapContentWidth(align = Alignment.Start, unbounded = true),
                )
            }
        }

        MetroPanoramaBodyEnter(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            skipEnter = skipIntro,
        ) {
            MetroPanorama(
                titles = listOf("", "headlines", "world", "technology", "business"),
                pagerState = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = MetroAppBarDefaults.BarHeight),
                pageContent = { page ->
                    when (page) {
                        NewsState.HUB_HERO -> HeroPane(
                            story = state.featuredStory(),
                            loading = state.loading,
                            onOpen = { story -> state.openArticle(story) },
                        )
                        NewsState.HUB_HEADLINES -> CategoryListPane(
                            stories = state.storiesFor(NewsCategory.Top),
                            loading = state.loading,
                            onOpen = state::openArticle,
                        )
                        NewsState.HUB_WORLD -> CategoryListPane(
                            stories = state.storiesFor(NewsCategory.World),
                            loading = state.loading,
                            onOpen = state::openArticle,
                        )
                        NewsState.HUB_TECH -> CategoryListPane(
                            stories = state.storiesFor(NewsCategory.Technology),
                            loading = state.loading,
                            onOpen = state::openArticle,
                        )
                        else -> CategoryListPane(
                            stories = state.storiesFor(NewsCategory.Business),
                            loading = state.loading,
                            onOpen = state::openArticle,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun HeroPane(
    story: NewsStory?,
    loading: Boolean,
    onOpen: (NewsStory) -> Unit,
) {
    if (story == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (loading) {
                MetroLoadingDots()
            } else {
                MetroText(
                    text = "No stories yet",
                    style = MetroTextStyle.ListItemSubtitle,
                )
            }
        }
        return
    }

    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .metroClickable { onOpen(story) },
    ) {
        if (!story.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(story.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MetroTheme.colors.secondarySurface),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    ),
                )
                .padding(start = 12.dp, end = 24.dp, top = 48.dp, bottom = 20.dp),
        ) {
            Column {
                MetroText(
                    text = story.title,
                    style = MetroTextStyle.SectionHeader,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(8.dp))
                MetroText(
                    text = "${story.source.uppercase()} · ${RelativeTime.format(story.publishedAtMs)}",
                    style = MetroTextStyle.ListItemSubtitle,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun CategoryListPane(
    stories: List<NewsStory>,
    loading: Boolean,
    onOpen: (NewsStory) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
    ) {
        if (loading && stories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                ) {
                    MetroLoadingDots()
                }
            }
        } else if (stories.isEmpty()) {
            item {
                MetroText(
                    text = "No stories for this topic",
                    style = MetroTextStyle.ListItemSubtitle,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp),
                )
            }
        } else {
            items(stories, key = { it.id }) { story ->
                StoryRow(story = story, onClick = { onOpen(story) })
            }
        }
    }
}
