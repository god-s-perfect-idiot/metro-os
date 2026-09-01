package com.metro.lockscreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.ui.MetroFontFamily

private val QuickStatusIconSize = 22.dp
private val QuickStatusCountStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Light,
    fontSize = 22.sp,
    lineHeight = 22.sp,
)

data class LockscreenQuickStatusItem(
    val slotIndex: Int,
    val packageName: String,
    val icon: QuickStatusIcon,
    val count: Int,
)

/**
 * Bottom quick-status row — up to five app glyphs with naked counts (WP8.1, capped at 99+).
 */
@Composable
fun LockscreenQuickStatusBar(
    items: List<LockscreenQuickStatusItem>,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val bySlot = items.associateBy { it.slotIndex }
    QuickStatusColumnRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) { columnIndex ->
        bySlot[columnIndex]?.let { item ->
            QuickStatusCell(item = item, contentColor = contentColor)
        }
    }
}

@Composable
private fun QuickStatusCell(
    item: LockscreenQuickStatusItem,
    contentColor: Color,
) {
    val countLabel = LockscreenQuickStatusLogic.formatCount(item.count)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        QuickStatusGlyph(
            icon = item.icon,
            contentColor = contentColor,
            modifier = Modifier.size(QuickStatusIconSize),
        )
        if (countLabel.isNotEmpty()) {
            BasicText(
                text = countLabel,
                style = QuickStatusCountStyle.copy(color = contentColor),
            )
        }
    }
}

@Composable
private fun QuickStatusGlyph(
    icon: QuickStatusIcon,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    when {
        icon.glyphResId != null -> {
            Image(
                painter = painterResource(icon.glyphResId),
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = modifier,
            )
        }
        icon.launcherBitmap != null -> {
            Image(
                bitmap = icon.launcherBitmap,
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = modifier,
            )
        }
    }
}
