package com.metro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val JumpListEdgePadding = 16.dp
private val JumpListTileGap = 8.dp
private val JumpListScrimAlpha = 0.72f

/**
 * WP8.1 find-by-letter overlay: 4-column grid of `#`, `a`–`z`, and a locale (globe) tile.
 *
 * Active letters (present in [activeLetters]) use the system accent; inactive letters use
 * [MetroColors.JumpListInactive]. Tapping an active letter invokes [onLetterSelected] with a
 * normalized key (`#` or lowercase `a`–`z`) and dismisses. Inactive tiles consume the tap and
 * do nothing. Back or tapping the scrim calls [onDismiss].
 *
 * Open this when the user taps a letter section marker in a LongListSelector-style list.
 */
@Composable
fun MetroJumpList(
    activeLetters: Set<Char>,
    onLetterSelected: (Char) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    localeActive: Boolean = false,
    onLocaleClick: (() -> Unit)? = null,
) {
    val normalizedActive = remember(activeLetters) {
        MetroJumpListLogic.activeLetters(activeLetters)
    }
    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = JumpListScrimAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .semantics { contentDescription = "find by letter" },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(JumpListEdgePadding),
        ) {
            val columns = MetroJumpListLogic.GridColumns
            val tileSize = (maxWidth - JumpListTileGap * (columns - 1)) / columns

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(JumpListTileGap),
                verticalArrangement = Arrangement.spacedBy(JumpListTileGap),
                userScrollEnabled = false,
            ) {
                items(MetroJumpListLogic.LetterKeys, key = { it }) { letter ->
                    val active = letter in normalizedActive
                    MetroLetterTile(
                        letter = letter,
                        size = tileSize,
                        enabled = active,
                        onClick = {
                            if (active) {
                                onLetterSelected(letter)
                                onDismiss()
                            }
                        },
                    )
                }
                item(key = "locale") {
                    val localeEnabled = localeActive && onLocaleClick != null
                    JumpListLocaleTile(
                        size = tileSize,
                        enabled = localeEnabled,
                        onClick = {
                            if (localeEnabled) {
                                onLocaleClick!!()
                                onDismiss()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun JumpListLocaleTile(
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val background = if (enabled) MetroTheme.colors.accent else MetroColors.JumpListInactive
    Box(
        modifier = Modifier
            .size(size)
            .background(background)
            .semantics {
                role = Role.Button
                contentDescription = "jump to other languages"
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size * 0.42f)) {
            val strokeWidth = this.size.minDimension * 0.07f
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val r = this.size.minDimension / 2f
            val c = Offset(center.x, center.y)
            drawCircle(Color.White, r, c, style = stroke)
            drawOval(
                color = Color.White,
                topLeft = Offset(c.x - r * 0.42f, c.y - r),
                size = Size(r * 0.84f, r * 2f),
                style = stroke,
            )
            drawLine(
                Color.White,
                Offset(c.x - r, c.y),
                Offset(c.x + r, c.y),
                strokeWidth,
                StrokeCap.Round,
            )
            drawLine(
                Color.White,
                Offset(c.x - r * 0.82f, c.y - r * 0.38f),
                Offset(c.x + r * 0.82f, c.y - r * 0.38f),
                strokeWidth,
                StrokeCap.Round,
            )
            drawLine(
                Color.White,
                Offset(c.x - r * 0.82f, c.y + r * 0.38f),
                Offset(c.x + r * 0.82f, c.y + r * 0.38f),
                strokeWidth,
                StrokeCap.Round,
            )
        }
    }
}
