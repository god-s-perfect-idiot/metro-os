package com.metro.statusbar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.statusbar.ActiveCallBanner
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle

/** WP8.1 green return-to-call strip under the system tray. */
private val ActiveCallBannerGreen = Color(0xFF107C10)

@Composable
fun ActiveCallBannerBar(
    banner: ActiveCallBanner,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(ActiveCallBannerGreen)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MetroText(
            text = banner.title,
            style = MetroTextStyle.ListItemSubtitle,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        MetroText(
            text = banner.statusText,
            style = MetroTextStyle.ListItemSubtitle,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
