package com.metro.messaging.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroComposerBubbleColor
import com.metro.ui.MetroComposerHintColor
import com.metro.ui.MetroComposerTextColor
import com.metro.ui.MetroMessageBubble
import com.metro.ui.MetroMessageBubbleKind
import com.metro.ui.metroIncomingBubbleColor
import com.metro.ui.metroOutgoingBubbleColor

/** @see MetroMessageBubbleKind */
typealias MessageBubbleKind = MetroMessageBubbleKind

/**
 * WP8.1 messaging bubble chrome — thin wrapper over toolkit [MetroMessageBubble].
 */
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
    MetroMessageBubble(
        kind = kind,
        color = color,
        modifier = modifier,
        maxWidthFraction = maxWidthFraction,
        tailWidth = tailWidth,
        tailHeight = tailHeight,
        content = content,
    )
}

fun outgoingBubbleColor(accent: Color): Color = metroOutgoingBubbleColor(accent)

fun incomingBubbleColor(accent: Color): Color = metroIncomingBubbleColor(accent)

val ComposerBubbleColor = MetroComposerBubbleColor

val ComposerHintColor = MetroComposerHintColor

val ComposerTextColor = MetroComposerTextColor
