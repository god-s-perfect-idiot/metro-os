package com.metro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 */
@Composable
fun MetroListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    verticalPadding: Dp = 12.dp,
    oneLineMinHeight: Dp = 76.dp,
    twoLineMinHeight: Dp = 90.dp,
    onClick: (() -> Unit)? = null,
) {
    val titleColor = if (enabled) {
        MetroTheme.colors.primaryText
    } else {
        MetroTheme.colors.secondaryText
    }
    val subtitleColor = MetroTheme.colors.secondaryText

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
                style = MetroTextStyle.ListItemTitle,
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                MetroText(
                    text = subtitle,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = subtitleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
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
        // Single line; clips at the screen edge (WP8.1) — never wraps.
        MetroText(
            text = pageTitle,
            style = MetroTextStyle.PageTitle,
            color = MetroTheme.colors.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MetroDimens.ScreenHorizontalMargin,
                    top = 4.dp,
                    bottom = 16.dp,
                ),
        )
    }
}
