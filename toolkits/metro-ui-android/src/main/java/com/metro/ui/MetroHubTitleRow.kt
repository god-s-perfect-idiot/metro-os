package com.metro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class MetroHubTitleMode {
    /** All titles visible; active tab gets accent underline. */
    Pivot,
    /** Active title flush-left; next pane title peeks from the right edge. */
    Panorama,
}

/**
 * Baseline-aligned hub / pivot / panorama page titles.
 * Shared by [MetroPivot] and [MetroPanorama].
 */
@Composable
fun MetroHubTitleRow(
    titles: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    mode: MetroHubTitleMode = MetroHubTitleMode.Pivot,
    onTitleClick: ((Int) -> Unit)? = null,
) {
    if (mode == MetroHubTitleMode.Panorama) {
        PanoramaTitleRow(
            titles = titles,
            selectedIndex = selectedIndex,
            modifier = modifier,
            onTitleClick = onTitleClick,
        )
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        titles.forEachIndexed { index, title ->
            PivotTitle(
                title = title,
                active = index == selectedIndex,
                onClick = onTitleClick?.let { { it(index) } },
                modifier = Modifier
                    .alignByBaseline()
                    .padding(end = 12.dp),
            )
        }
    }
}

@Composable
private fun PanoramaTitleRow(
    titles: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onTitleClick: ((Int) -> Unit)? = null,
) {
    val activeTitle = titles.getOrElse(selectedIndex) { "" }
    val peekIndex = selectedIndex + 1
    val peekTitle = titles.getOrNull(peekIndex)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        PanoramaTitle(
            title = activeTitle,
            active = true,
            onClick = onTitleClick?.let { { it(selectedIndex) } },
            modifier = Modifier.align(Alignment.BottomStart),
        )
        if (peekTitle != null) {
            PanoramaTitle(
                title = peekTitle,
                active = false,
                onClick = onTitleClick?.let { { it(peekIndex) } },
                textAlign = TextAlign.End,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(0.5f),
            )
        }
    }
}

@Composable
private fun PivotTitle(
    title: String,
    active: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val accent = MetroTheme.colors.accent
    val underlineModifier = if (active) {
        Modifier.drawBehind {
            val strokeHeight = 3.dp.toPx()
            drawRect(
                color = accent,
                topLeft = Offset(0f, size.height - strokeHeight),
                size = Size(size.width, strokeHeight),
            )
        }
    } else {
        Modifier
    }
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    MetroText(
        text = title,
        style = MetroTextStyle.HubTitle,
        color = if (active) MetroTheme.colors.primaryText else MetroTheme.colors.secondaryText,
        maxLines = 1,
        modifier = modifier.then(clickModifier).then(underlineModifier),
    )
}

@Composable
private fun PanoramaTitle(
    title: String,
    active: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    MetroText(
        text = title,
        style = MetroTextStyle.HubTitle,
        color = if (active) MetroTheme.colors.primaryText else MetroTheme.colors.secondaryText,
        textAlign = textAlign,
        maxLines = 1,
        overflow = overflow,
        modifier = modifier.then(clickModifier),
    )
}
