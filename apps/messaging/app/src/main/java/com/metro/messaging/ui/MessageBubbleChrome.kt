package com.metro.messaging.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * WP8.1 messaging bubble chrome: square body + right-triangle tail.
 *
 * Incoming — left-aligned, tail on the **top-left** pointing up/left.
 * Outgoing / composer — right-aligned, tail on the **bottom-right** pointing down/right.
 */
enum class MessageBubbleKind {
    Incoming,
    Outgoing,
}

@Composable
fun MessageBubbleChrome(
    kind: MessageBubbleKind,
    color: Color,
    modifier: Modifier = Modifier,
    maxWidthFraction: Float = 0.82f,
    tailWidth: Dp = 16.dp,
    tailHeight: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    val alignment = when (kind) {
        MessageBubbleKind.Incoming -> Alignment.Start
        MessageBubbleKind.Outgoing -> Alignment.End
    }
    Column(
        modifier = modifier.fillMaxWidth(maxWidthFraction),
        horizontalAlignment = alignment,
    ) {
        if (kind == MessageBubbleKind.Incoming) {
            BubbleTail(
                kind = kind,
                color = color,
                tailWidth = tailWidth,
                tailHeight = tailHeight,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color),
        ) {
            content()
        }
        if (kind == MessageBubbleKind.Outgoing) {
            BubbleTail(
                kind = kind,
                color = color,
                tailWidth = tailWidth,
                tailHeight = tailHeight,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun BubbleTail(
    kind: MessageBubbleKind,
    color: Color,
    tailWidth: Dp,
    tailHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .width(tailWidth)
            .height(tailHeight),
    ) {
        val path = Path()
        when (kind) {
            MessageBubbleKind.Incoming -> {
                // Right triangle: (0,h) → (0,0) → (w,h) — points up/left along the left edge.
                path.moveTo(0f, size.height)
                path.lineTo(0f, 0f)
                path.lineTo(size.width, size.height)
                path.close()
            }
            MessageBubbleKind.Outgoing -> {
                // Right triangle: (w,0) → (w,h) → (0,0) — points down/right along the right edge.
                path.moveTo(size.width, 0f)
                path.lineTo(size.width, size.height)
                path.lineTo(0f, 0f)
                path.close()
            }
        }
        drawPath(path, color)
    }
}

/** Sent-bubble fill: accent darkened ~30% (WP uses ~0.7 opacity over black). */
fun outgoingBubbleColor(accent: Color): Color = Color(
    red = accent.red * 0.7f,
    green = accent.green * 0.7f,
    blue = accent.blue * 0.7f,
    alpha = 1f,
)

/** Incoming-bubble fill: full system accent. */
fun incomingBubbleColor(accent: Color): Color = accent

/** Composer prompt fill — light gray outgoing-shaped bubble. */
val ComposerBubbleColor = Color(0xFFD0D0D0)

val ComposerHintColor = Color(0xFF666666)

val ComposerTextColor = Color(0xFF1A1A1A)
