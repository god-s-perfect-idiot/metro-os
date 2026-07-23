package com.metro.dialer.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import com.metro.dialer.data.CallDirection
import com.metro.dialer.data.CallGroup
import com.metro.dialer.data.DialerCallLogic
import com.metro.ui.MetroColors
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
private class RectRef {
    var value: Rect = Rect.Zero
}

@Composable
fun HistoryScreen(
    groups: List<CallGroup>,
    hasPermission: Boolean,
    onOpenDetail: (CallGroup) -> Unit,
    onCallBack: (CallGroup) -> Unit,
    onAddSpeedDial: (CallGroup) -> Unit,
    onPinToStart: (CallGroup) -> Unit,
    canPinToStart: (CallGroup) -> Boolean,
    modifier: Modifier = Modifier,
) {
    if (!hasPermission) {
        EmptyPane(
            message = stringResource(R.string.history_permission_denied),
            modifier = modifier,
        )
        return
    }
    if (groups.isEmpty()) {
        EmptyPane(
            message = stringResource(R.string.history_empty),
            modifier = modifier,
        )
        return
    }

    var contextMenuGroup by remember { mutableStateOf<CallGroup?>(null) }
    var contextMenuAnchor by remember { mutableStateOf(Rect.Zero) }
    var contextMenuRoot by remember { mutableStateOf(Rect.Zero) }
    val contextMenuVisible = remember { MutableTransitionState(false) }
    val popupRootBounds = remember { RectRef() }
    val contextMenuFocusTransition =
        updateTransition(contextMenuVisible, label = "historyContextMenuFocus")
    val contextMenuFocusFraction by contextMenuFocusTransition.animateFloat(
        transitionSpec = { tween(MetroTransitions.AppBarSlideMs) },
        label = "focusFraction",
    ) { visible -> if (visible) 1f else 0f }

    val openContextMenu: (CallGroup, Rect) -> Unit = { group, bounds ->
        contextMenuAnchor = bounds
        contextMenuRoot = popupRootBounds.value
        contextMenuGroup = group
        contextMenuVisible.targetState = true
    }
    val dismissContextMenu: () -> Unit = {
        contextMenuVisible.targetState = false
    }
    MetroContextMenuClearOnDismiss(contextMenuVisible) {
        contextMenuGroup = null
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
            items(groups, key = { it.normalizedNumber }) { group ->
                HistoryRow(
                    group = group,
                    contextMenuTarget = contextMenuGroup?.normalizedNumber == group.normalizedNumber,
                    contextMenuFocusFraction = contextMenuFocusFraction,
                    onOpenDetail = { onOpenDetail(group) },
                    onCallBack = { onCallBack(group) },
                    onLongPress = { bounds -> openContextMenu(group, bounds) },
                )
            }
        }

        contextMenuGroup?.let { group ->
            val speedDialLabel = stringResource(R.string.add_to_speed_dial)
            val pinLabel = stringResource(R.string.pin_to_start)
            MetroContextMenuPopup(
                visibleState = contextMenuVisible,
                anchorBounds = contextMenuAnchor,
                rootBounds = contextMenuRoot,
                items = listOf(
                    MetroContextMenuItem(
                        label = speedDialLabel,
                        onClick = {
                            onAddSpeedDial(group)
                            dismissContextMenu()
                        },
                    ),
                    MetroContextMenuItem(
                        label = pinLabel,
                        enabled = canPinToStart(group),
                        onClick = {
                            onPinToStart(group)
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
private fun HistoryRow(
    group: CallGroup,
    contextMenuTarget: Boolean,
    contextMenuFocusFraction: Float,
    onOpenDetail: () -> Unit,
    onCallBack: () -> Unit,
    onLongPress: (Rect) -> Unit,
) {
    val primaryColor = when (group.latestType) {
        CallDirection.Missed -> MetroColors.AccentRed
        else -> MetroTheme.colors.primaryText
    }
    val directionLabel = when (group.latestType) {
        CallDirection.Incoming -> stringResource(R.string.incoming)
        CallDirection.Outgoing -> stringResource(R.string.outgoing)
        CallDirection.Missed -> stringResource(R.string.missed)
    }
    val subtitle = "${directionLabel} · ${DialerCallLogic.relativeTime(group.latestTimestamp)}"
    val labelBounds = remember(group.normalizedNumber) { RectRef() }
    val density = LocalDensity.current
    val activeShiftPx = with(density) { MetroContextMenuActiveShift.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .graphicsLayer {
                if (contextMenuTarget) {
                    translationX = -contextMenuFocusFraction * activeShiftPx
                    alpha = 1f
                } else {
                    translationX = 0f
                    alpha = 1f - contextMenuFocusFraction * (1f - MetroContextMenuDimmedAlpha)
                }
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { coordinates ->
                    labelBounds.value = coordinates.boundsInWindow()
                }
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenDetail,
                    onLongClick = { onLongPress(labelBounds.value) },
                ),
        ) {
            MetroText(
                text = DialerCallLogic.primaryLabel(group),
                style = MetroTextStyle.ListItemTitle,
                color = primaryColor,
            )
            MetroText(
                text = subtitle,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
            )
        }
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCallBack,
                )
                .padding(start = 8.dp),
        ) {
            PhoneCallIcon(color = Color.White)
        }
    }
}
