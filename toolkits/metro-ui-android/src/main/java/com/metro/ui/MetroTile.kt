package com.metro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class MetroTileSize(val size: DpSize) {
    Small(DpSize(99.dp, 99.dp)),
    Medium(DpSize(198.dp, 99.dp)),
    Wide(DpSize(198.dp, 198.dp)),
    ;
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetroTile(
    title: String,
    modifier: Modifier = Modifier,
    size: MetroTileSize = MetroTileSize.Medium,
    backgroundColor: Color,
    contentColor: Color = MetroColors.tileContentColor(backgroundColor),
    counter: Int? = null,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    elevated: Boolean = false,
) {
    val interactionModifier = if (onLongClick != null) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier.clickable(onClick = onClick)
    }
    Box(
        modifier = modifier
            .then(if (elevated) Modifier.shadow(12.dp) else Modifier)
            .size(size.size)
            .background(backgroundColor)
            .then(interactionModifier)
            .padding(8.dp),
    ) {
        MetroText(
            text = title,
            style = MetroTextStyle.Body,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart),
        )
        if (counter != null && counter > 0) {
            MetroText(
                text = counter.toString(),
                style = MetroTextStyle.Body,
                color = contentColor,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}
