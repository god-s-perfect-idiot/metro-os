package com.metro.ui

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

private val PivotTabSpacing = 20.dp
/** Content margin for the active title; inactive titles bleed past screen edges. */
private val HubTitleStartInset = MetroDimens.ScreenHorizontalMargin

enum class MetroHubTitleMode {
    /** Active tab flush-left; earlier tabs scroll off to the left. */
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

    PivotTitleRow(
        titles = titles,
        selectedIndex = selectedIndex,
        modifier = modifier,
        onTitleClick = onTitleClick,
    )
}

@Composable
private fun PivotTitleRow(
    titles: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onTitleClick: ((Int) -> Unit)? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = MetroTextStyle.PivotTab.toTextStyle()
    val spacingPx = remember(density) { with(density) { PivotTabSpacing.roundToPx() } }
    val tabContentWidthsPx = remember(titles, textStyle, textMeasurer) {
        titles.map { title ->
            textMeasurer.measure(title, style = textStyle).size.width
        }
    }
    val targetOffsetPx = remember(tabContentWidthsPx, selectedIndex, spacingPx) {
        if (selectedIndex <= 0) {
            0
        } else {
            var offset = 0
            for (index in 0 until selectedIndex) {
                offset += tabContentWidthsPx[index] + spacingPx
            }
            offset
        }
    }
    val animatedOffsetPx by animateIntAsState(
        targetValue = targetOffsetPx,
        animationSpec = MetroTransitions.pivotTween(),
        label = "pivotTitleOffset",
    )

    // Full-bleed strip: clip at screen edges only. Active title starts at the
    // content margin; adjacent titles may overflow past the left/right edges.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        Row(
            modifier = Modifier
                .padding(start = HubTitleStartInset)
                .wrapContentWidth(unbounded = true, align = Alignment.Start)
                .offset { IntOffset(-animatedOffsetPx, 0) },
            verticalAlignment = Alignment.Bottom,
        ) {
            titles.forEachIndexed { index, title ->
                PivotTitle(
                    title = title,
                    active = index == selectedIndex,
                    onClick = onTitleClick?.let { { it(index) } },
                    modifier = Modifier.padding(
                        end = if (index < titles.lastIndex) PivotTabSpacing else 0.dp,
                    ),
                )
            }
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
            .clipToBounds(),
    ) {
        PanoramaTitle(
            title = activeTitle,
            active = true,
            onClick = onTitleClick?.let { { it(selectedIndex) } },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = HubTitleStartInset),
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
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    MetroText(
        text = title,
        style = MetroTextStyle.PivotTab,
        color = if (active) MetroTheme.colors.primaryText else MetroTheme.colors.secondaryText,
        maxLines = 1,
        modifier = modifier.then(clickModifier),
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
