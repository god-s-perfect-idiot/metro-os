package com.metro.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
    BasicText(
        text = text,
        modifier = modifier,
        style = textStyle,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
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
    BasicText(
        text = text,
        modifier = modifier,
        style = textStyle,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
    )
}

/**
 * WP8.1 app-title overline — the small app name shown above a hub/pivot/panorama title.
 *
 * Always rendered ALL CAPS so every app presents its name consistently. Use this instead of a
 * raw [MetroText] whenever you need the app-title line.
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
    // Single line; start inset only so long titles clip at the screen edge — never wrap.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(98.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        MetroText(
            text = title,
            style = MetroTextStyle.PageTitle,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MetroDimens.ScreenHorizontalMargin),
        )
    }
}
