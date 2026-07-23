package com.metro.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

data class MetroContextMenuItem(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

private val ContextMenuGapBelowAnchor = 4.dp
private val ContextMenuHorizontalPadding = 16.dp
private val ContextMenuVerticalPadding = 12.dp
private val ContextMenuMinWidth = 160.dp
private val ContextMenuMaxVisibleItems = 6
private val ContextMenuItemRowHeight = 48.dp
private val ContextMenuMaxScrollHeight =
    ContextMenuItemRowHeight * ContextMenuMaxVisibleItems + ContextMenuVerticalPadding * 2
private val ContextMenuExpandMs = MetroTransitions.AppBarSlideMs

/**
 * WP8.1-style context menu popup — light panel, black labels, height wipe reveal.
 * Anchored just below [anchorBounds] within [rootBounds] (both window coordinates).
 */
@Composable
fun MetroContextMenuPopup(
    visibleState: MutableTransitionState<Boolean>,
    anchorBounds: Rect,
    rootBounds: Rect,
    items: List<MetroContextMenuItem>,
    onDismissRequest: () -> Unit,
) {
    if (!visibleState.currentState && !visibleState.targetState && visibleState.isIdle) return
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val menuGapPx = with(density) { ContextMenuGapBelowAnchor.roundToPx() }
    val transition = updateTransition(visibleState, label = "metroContextMenu")
    val revealFraction by transition.animateFloat(
        transitionSpec = { tween(ContextMenuExpandMs) },
        label = "reveal",
    ) { visible -> if (visible) 1f else 0f }

    val menuOffset = IntOffset(
        x = (anchorBounds.left - rootBounds.left).toInt(),
        y = (anchorBounds.bottom - rootBounds.top + menuGapPx).toInt(),
    )

    Popup(
        alignment = Alignment.TopStart,
        offset = menuOffset,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Layout(
            modifier = Modifier.clipToBounds(),
            content = {
                MetroContextMenuPanel(items = items)
            },
        ) { measurables, constraints ->
            val placeable = measurables.first().measure(
                constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity),
            )
            val height = (placeable.height * revealFraction)
                .roundToInt()
                .coerceIn(0, placeable.height)
            layout(placeable.width, height) {
                placeable.placeRelative(0, 0)
            }
        }
    }
}

@Composable
fun MetroContextMenuPanel(
    items: List<MetroContextMenuItem>,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val needsScroll = items.size > ContextMenuMaxVisibleItems
    Column(
        modifier = modifier
            .widthIn(min = ContextMenuMinWidth)
            .background(MetroColors.LightBackground)
            .padding(
                horizontal = ContextMenuHorizontalPadding,
                vertical = ContextMenuVerticalPadding,
            )
            .then(
                if (needsScroll) {
                    Modifier
                        .heightIn(max = ContextMenuMaxScrollHeight)
                        .verticalScroll(scrollState)
                } else {
                    Modifier
                },
            ),
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = item.enabled,
                        onClick = item.onClick,
                    )
                    .padding(vertical = ContextMenuVerticalPadding),
                contentAlignment = Alignment.CenterStart,
            ) {
                MetroText(
                    text = item.label,
                    style = MetroTextStyle.ListItemTitle,
                    color = if (item.enabled) {
                        MetroColors.LightPrimaryText
                    } else {
                        MetroColors.LightSecondaryText
                    },
                )
            }
        }
    }
}

/** Dim non-target rows and nudge the active row left while a context menu is open. */
const val MetroContextMenuDimmedAlpha = 0.45f
val MetroContextMenuActiveShift = 8.dp

/**
 * Clears [host] once [visibleState] finishes collapsing. Call from the screen that owns the menu.
 */
@Composable
fun MetroContextMenuClearOnDismiss(
    visibleState: MutableTransitionState<Boolean>,
    onCleared: () -> Unit,
) {
    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (visibleState.isIdle && !visibleState.currentState) {
            onCleared()
        }
    }
}
