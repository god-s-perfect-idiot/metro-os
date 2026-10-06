package com.metro.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.metro.news.data.NewsStory
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarTextButton
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun ArticleScreen(
    story: NewsStory?,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        if (story == null) {
            MetroText(
                text = "Story unavailable",
                style = MetroTextStyle.ListItemSubtitle,
                modifier = Modifier.padding(12.dp),
            )
            return
        }

        val context = LocalContext.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = MetroAppBarDefaults.BarHeight + 16.dp),
        ) {
            MetroAppTitle(title = "news")

            if (!story.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(story.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(MetroTheme.colors.secondarySurface),
                )
            }

            Column(modifier = Modifier.padding(top = 16.dp)) {
                MetroText(
                    text = story.title,
                    style = MetroTextStyle.SectionHeader,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                val byline = buildString {
                    if (!story.author.isNullOrBlank()) {
                        append(story.author)
                        append(" · ")
                    }
                    append(story.source)
                    append(" · ")
                    append(RelativeTime.format(story.publishedAtMs).lowercase())
                }
                MetroText(
                    text = byline,
                    style = MetroTextStyle.ListItemSubtitle,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                MetroText(
                    text = story.summary.ifBlank { "No summary available. Open the full article to read more." },
                    style = MetroTextStyle.Body,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }

        MetroAppBar(
            textButtons = listOf(
                MetroAppBarTextButton(text = "open", onClick = onOpen),
                MetroAppBarTextButton(text = "share", onClick = onShare),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
