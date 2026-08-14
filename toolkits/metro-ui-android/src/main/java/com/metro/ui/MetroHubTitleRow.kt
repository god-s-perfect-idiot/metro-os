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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

private val PivotTabSpacing = 20.dp
/** Content margin for the active title; inactive titles bleed past screen edges. */
private val HubTitleStartInset = MetroDimens.ScreenHorizontalMargin

enum class MetroHubTitleMode {
    /** Active tab flush-left; earlier tabs scroll off to the left. */
    Pivot,
}

/**
 * Baseline-aligned pivot page titles. Panorama titles use [MetroPanoramaTitleRow].
 */
@Composable
fun MetroHubTitleRow(
    titles: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    mode: MetroHubTitleMode = MetroHubTitleMode.Pivot,
    onTitleClick: ((Int) -> Unit)? = null,
) {
    ScrollingTitleRow(
        titles = titles,
        selectedIndex = selectedIndex,
        style = MetroTextStyle.PivotTab,
        spacing = PivotTabSpacing,
        modifier = modifier,
        onTitleClick = onTitleClick,
    )
}

/**
 * Titles laid out sequentially on one line and translated so the selected title sits at the
 * content margin. Neighbouring titles bleed past the screen edges instead of stacking on top of
 * each other, so a long title never overlaps the next one.
 */
@Composable
private fun ScrollingTitleRow(
    titles: List<String>,
    selectedIndex: Int,
    style: MetroTextStyle,
    spacing: Dp,
    modifier: Modifier = Modifier,
    onTitleClick: ((Int) -> Unit)? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = style.toTextStyle()
    val spacingPx = remember(density, spacing) { with(density) { spacing.roundToPx() } }
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
                HubTitle(
                    title = title,
                    style = style,
                    active = index == selectedIndex,
                    onClick = onTitleClick?.let { { it(index) } },
                    modifier = Modifier.padding(
                        end = if (index < titles.lastIndex) spacing else 0.dp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun HubTitle(
    title: String,
    style: MetroTextStyle,
    active: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    MetroText(
        text = title,
        style = style,
        color = if (active) MetroTheme.colors.primaryText else MetroTheme.colors.secondaryText,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier.then(clickModifier),
    )
}
