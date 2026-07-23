package com.metro.dialer.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.dialer.data.SpeedDialEntry
import com.metro.ui.MetroContextMenuActiveShift
import com.metro.ui.MetroContextMenuClearOnDismiss
import com.metro.ui.MetroContextMenuDimmedAlpha
import com.metro.ui.MetroContextMenuItem
import com.metro.ui.MetroContextMenuPopup
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions

/** Mutable holder so layout callbacks can update without triggering recomposition. */
private class SpeedDialRectRef {
    var value: Rect = Rect.Zero
}

@Composable
fun SpeedDialScreen(
    entries: List<SpeedDialEntry>,
    onCall: (SpeedDialEntry) -> Unit,
    onPinToStart: (SpeedDialEntry) -> Unit,
    canPinToStart: (SpeedDialEntry) -> Boolean,
    onRemove: (SpeedDialEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        EmptyPane(
            message = stringResource(R.string.speed_dial_empty),
            modifier = modifier,
        )
        return
    }

    var contextMenuEntry by remember { mutableStateOf<SpeedDialEntry?>(null) }
    var contextMenuAnchor by remember { mutableStateOf(Rect.Zero) }
    var contextMenuRoot by remember { mutableStateOf(Rect.Zero) }
    val contextMenuVisible = remember { MutableTransitionState(false) }
    val popupRootBounds = remember { SpeedDialRectRef() }
    val contextMenuFocusTransition =
        updateTransition(contextMenuVisible, label = "speedDialContextMenuFocus")
    val contextMenuFocusFraction by contextMenuFocusTransition.animateFloat(
        transitionSpec = { tween(MetroTransitions.AppBarSlideMs) },
        label = "focusFraction",
    ) { visible -> if (visible) 1f else 0f }

    val openContextMenu: (SpeedDialEntry, Rect) -> Unit = { entry, bounds ->
        contextMenuAnchor = bounds
        contextMenuRoot = popupRootBounds.value
        contextMenuEntry = entry
        contextMenuVisible.targetState = true
    }
    val dismissContextMenu: () -> Unit = {
        contextMenuVisible.targetState = false
    }
    MetroContextMenuClearOnDismiss(contextMenuVisible) {
        contextMenuEntry = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                popupRootBounds.value = coordinates.boundsInWindow()
            },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 12.dp),
        ) {
            items(entries, key = { it.id }) { entry ->
                SpeedDialRow(
                    entry = entry,
                    contextMenuTarget = contextMenuEntry?.id == entry.id,
                    contextMenuFocusFraction = contextMenuFocusFraction,
                    onCall = { onCall(entry) },
                    onLongPress = { bounds -> openContextMenu(entry, bounds) },
                )
            }
        }

        contextMenuEntry?.let { entry ->
            val pinLabel = stringResource(R.string.pin_to_start)
            val removeLabel = stringResource(R.string.remove_from_speed_dial)
            MetroContextMenuPopup(
                visibleState = contextMenuVisible,
                anchorBounds = contextMenuAnchor,
                rootBounds = contextMenuRoot,
                items = listOf(
                    MetroContextMenuItem(
                        label = pinLabel,
                        enabled = canPinToStart(entry),
                        onClick = {
                            onPinToStart(entry)
                            dismissContextMenu()
                        },
                    ),
                    MetroContextMenuItem(
                        label = removeLabel,
                        onClick = {
                            onRemove(entry)
                            dismissContextMenu()
                        },
                    ),
                ),
                onDismissRequest = dismissContextMenu,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeedDialRow(
    entry: SpeedDialEntry,
    contextMenuTarget: Boolean,
    contextMenuFocusFraction: Float,
    onCall: () -> Unit,
    onLongPress: (Rect) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val rowBounds = remember(entry.id) { SpeedDialRectRef() }
    val density = LocalDensity.current
    val activeShiftPx = with(density) { MetroContextMenuActiveShift.toPx() }
    val dimmed = contextMenuFocusFraction > 0f && !contextMenuTarget
    val nudge = if (contextMenuTarget) contextMenuFocusFraction * activeShiftPx else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .graphicsLayer {
                alpha = if (dimmed) {
                    1f - contextMenuFocusFraction * (1f - MetroContextMenuDimmedAlpha)
                } else {
                    1f
                }
                translationX = -nudge
            }
            .onGloballyPositioned { coordinates ->
                rowBounds.value = coordinates.boundsInWindow()
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onCall,
                onLongClick = { onLongPress(rowBounds.value) },
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpeedDialAvatar(name = entry.displayName)
        Column(modifier = Modifier.padding(start = 16.dp)) {
            MetroText(
                text = entry.displayName,
                style = MetroTextStyle.ListItemTitle,
            )
            MetroText(
                text = entry.phoneNumber,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
            )
        }
    }
}

@Composable
private fun SpeedDialAvatar(name: String, modifier: Modifier = Modifier) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
    Box(
        modifier = modifier
            .size(48.dp)
            .background(MetroTheme.colors.accent),
        contentAlignment = Alignment.Center,
    ) {
        MetroText(
            text = initial,
            style = MetroTextStyle.ListItemTitle,
            color = Color.White,
        )
    }
}
