package com.metro.news.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.metro.news.data.NewsStory
import com.metro.ui.MetroListItem
import com.metro.ui.MetroTheme

@Composable
fun StoryRow(
    story: NewsStory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = buildString {
        append(story.source.uppercase())
        append(" · ")
        append(RelativeTime.format(story.publishedAtMs))
    }
    MetroListItem(
        title = story.title,
        subtitle = subtitle,
        modifier = modifier,
        leading = {
            StoryThumbnail(imageUrl = story.imageUrl)
        },
        onClick = onClick,
    )
}

@Composable
fun StoryThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .padding(end = 12.dp)
            .size(72.dp)
            .background(MetroTheme.colors.secondarySurface),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
