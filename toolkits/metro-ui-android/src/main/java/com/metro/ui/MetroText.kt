package com.metro.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * WP8.1 chrome text.
 *
 * [MetroTextStyle.PageTitle], [MetroTextStyle.HubTitle], and [MetroTextStyle.PivotTab]
 * never wrap. Long titles stay on one line and overflow the screen edge (no ellipsis).
 * `maxLines` / `softWrap` / `overflow` are ignored for those styles.
 */
@Composable
fun MetroText(
    text: String,
    modifier: Modifier = Modifier,
    style: MetroTextStyle = MetroTextStyle.Body,
    color: Color = MetroTheme.colors.primaryText,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
) {
    val textStyle = style.toTextStyle().copy(
        color = color,
        textAlign = textAlign ?: style.toTextStyle().textAlign,
    )
    val layout = resolveMetroTitleOverflow(style, modifier, maxLines, overflow, softWrap)
    BasicText(
        text = text,
        modifier = layout.modifier,
        style = textStyle,
        maxLines = layout.maxLines,
        overflow = layout.overflow,
        softWrap = layout.softWrap,
    )
}

@Composable
fun MetroText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: MetroTextStyle = MetroTextStyle.Body,
    color: Color = MetroTheme.colors.primaryText,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
) {
    val textStyle = style.toTextStyle().copy(
        color = color,
        textAlign = textAlign ?: style.toTextStyle().textAlign,
    )
    val layout = resolveMetroTitleOverflow(style, modifier, maxLines, overflow, softWrap)
    BasicText(
        text = text,
        modifier = layout.modifier,
        style = textStyle,
        maxLines = layout.maxLines,
        overflow = layout.overflow,
        softWrap = layout.softWrap,
    )
}

/**
 * WP8.1 app-title overline — the small app name shown above a hub/pivot/panorama title.
 *
 * Always rendered ALL CAPS so every app presents its name consistently. Use this instead of a
 * raw [MetroText] whenever you need the app-title line.
 *
 * Owns the **12dp start inset** — place it in an unpadded parent and apply the same margin to
 * sibling content per-child. Do not wrap it in a Column with `padding(horizontal = …)` (that
 * double-indents the overline; harness lint-metro enforces this).
 */
@Composable
fun MetroAppTitle(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MetroTheme.colors.primaryText,
) {
    MetroText(
        text = title.uppercase(),
        style = MetroTextStyle.AppTitle,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .padding(start = MetroDimens.ScreenHorizontalMargin, top = 8.dp),
    )
}

/** WP8.1 page header — 64sp title, flush left, 98dp region. */
@Composable
fun MetroPageHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    // Start inset only so long titles overflow the screen edge — never wrap.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(98.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        MetroText(
            text = title,
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(start = MetroDimens.ScreenHorizontalMargin),
        )
    }
}

private data class MetroTitleOverflow(
    val maxLines: Int,
    val softWrap: Boolean,
    val overflow: TextOverflow,
    val modifier: Modifier,
)

private fun resolveMetroTitleOverflow(
    style: MetroTextStyle,
    modifier: Modifier,
    maxLines: Int,
    overflow: TextOverflow,
    softWrap: Boolean,
): MetroTitleOverflow {
    if (!style.overflowsAtScreenEdge()) {
        return MetroTitleOverflow(maxLines, softWrap, overflow, modifier)
    }
    return MetroTitleOverflow(
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,
        modifier = modifier.wrapContentWidth(unbounded = true, align = Alignment.Start),
    )
}
