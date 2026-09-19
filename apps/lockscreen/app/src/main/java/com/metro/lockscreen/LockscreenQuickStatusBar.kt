package com.metro.lockscreen

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroTransitions

private val QuickStatusIconSize = 22.dp
private val QuickStatusCountStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Light,
    fontSize = 22.sp,
    lineHeight = 22.sp,
)

/** Extra camera distance so rotationX reads as a 3D flip on the small glyph. */
private const val GlanceFlipCameraDistance = 12f

data class LockscreenQuickStatusItem(
    val slotIndex: Int,
    val packageName: String,
    val icon: QuickStatusIcon,
    val count: Int,
    /** Bumps when a new notification is posted for this package — drives Glance flip. */
    val flipGeneration: Long = 0L,
)

/**
 * Bottom quick-status row — up to five app glyphs with naked counts (WP8.1, capped at 99+).
 *
 * In [flipOnNewNotification] mode (Glance), a new post for a configured app flips the tile
 * from the previous icon + count to the updated icon + count (no message peek).
 */
@Composable
fun LockscreenQuickStatusBar(
    items: List<LockscreenQuickStatusItem>,
    contentColor: Color,
    modifier: Modifier = Modifier,
    flipOnNewNotification: Boolean = false,
) {
    if (items.isEmpty()) return

    val bySlot = items.associateBy { it.slotIndex }
    QuickStatusColumnRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) { columnIndex ->
        bySlot[columnIndex]?.let { item ->
            QuickStatusCell(
                item = item,
                contentColor = contentColor,
                flipOnNewNotification = flipOnNewNotification,
            )
        }
    }
}

@Composable
private fun QuickStatusCell(
    item: LockscreenQuickStatusItem,
    contentColor: Color,
    flipOnNewNotification: Boolean,
) {
    val density = LocalDensity.current.density
    val rotation = remember { Animatable(0f) }
    var rendered by remember { mutableStateOf(item) }
    var lastFlipGeneration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(item, flipOnNewNotification) {
        if (!flipOnNewNotification) {
            rendered = item
            lastFlipGeneration = item.flipGeneration
            rotation.snapTo(0f)
            return@LaunchedEffect
        }
        val shouldFlip =
            item.flipGeneration > 0L && item.flipGeneration != lastFlipGeneration
        if (!shouldFlip) {
            rendered = item
            lastFlipGeneration = item.flipGeneration
            return@LaunchedEffect
        }
        lastFlipGeneration = item.flipGeneration
        // Front (previous icon + count) → 90°, swap to updated icon + count, −90° → 0°.
        rotation.animateTo(90f, animationSpec = MetroTransitions.tileFlipHalfTween())
        rendered = item
        rotation.snapTo(-90f)
        rotation.animateTo(0f, animationSpec = MetroTransitions.tileFlipSettleSpring())
    }

    Box(
        modifier = Modifier
            .heightIn(min = QuickStatusIconSize)
            .graphicsLayer {
                rotationX = rotation.value
                transformOrigin = TransformOrigin(0.5f, 0.5f)
                cameraDistance = GlanceFlipCameraDistance * density
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        QuickStatusFront(item = rendered, contentColor = contentColor)
    }
}

@Composable
private fun QuickStatusFront(
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
