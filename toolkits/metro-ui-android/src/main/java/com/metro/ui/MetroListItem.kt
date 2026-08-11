package com.metro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * WP8.1 list row — full-width rectangle, no separators (METRO-UX-LANGUAGE §6.6).
 *
 * Pass smaller [verticalPadding] / min heights for dense lists (e.g. file browser).
 * Defaults match §6.6 (76dp / 90dp, 12dp vertical padding).
 *
 * [leading] is optional content before the title column (e.g. Files folder/file tiles).
 *
 * With [singleLine], title and subtitle never wrap: long text runs past the row's end margin and
 * clips mid-glyph at the screen edge, matching WP8.1 dense lists (e.g. Xbox Music collection).
 */
@Composable
fun MetroListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    titleStyle: MetroTextStyle = MetroTextStyle.ListItemTitle,
    verticalPadding: Dp = 12.dp,
    oneLineMinHeight: Dp = 76.dp,
    twoLineMinHeight: Dp = 90.dp,
    singleLine: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val titleColor = if (enabled) {
        MetroTheme.colors.primaryText
    } else {
        MetroTheme.colors.secondaryText
    }
    val subtitleColor = MetroTheme.colors.secondaryText
    // Only overrun the end margin when nothing sits to the right of the text.
    val textWidthModifier = if (singleLine && trailing == null) {
        Modifier.wrapContentWidth(unbounded = true, align = Alignment.Start)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle == null) oneLineMinHeight else twoLineMinHeight)
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            // Overrunning text must stop at the row edge, not bleed into the neighbouring
            // pivot/panorama pane.
            .then(if (singleLine) Modifier.clipToBounds() else Modifier)
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (leading != null) {
            leading()
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (leading != null) Modifier.padding(start = 12.dp) else Modifier,
                ),
            verticalArrangement = Arrangement.Center,
        ) {
            MetroText(
                text = title,
                style = titleStyle,
                color = titleColor,
                maxLines = if (singleLine) 1 else 2,
                softWrap = !singleLine,
                overflow = if (singleLine) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = textWidthModifier,
            )
            if (subtitle != null) {
                MetroText(
                    text = subtitle,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = subtitleColor,
                    maxLines = if (singleLine) 1 else 2,
                    softWrap = !singleLine,
                    overflow = if (singleLine) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = textWidthModifier.padding(top = 2.dp),
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun MetroSettingsHeader(
    pageTitle: String,
    modifier: Modifier = Modifier,
    appTitle: String = "settings",
    appTitleColor: Color = MetroTheme.colors.primaryText,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MetroAppTitle(title = appTitle, color = appTitleColor)
        // Start inset only so long titles overflow the screen edge — never wrap.
        MetroText(
            text = pageTitle,
            style = MetroTextStyle.PageTitle,
            color = MetroTheme.colors.primaryText,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                top = 4.dp,
                bottom = 16.dp,
            ),
        )
    }
}
