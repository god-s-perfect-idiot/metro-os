package com.metro.clock.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.clock.data.AlarmItem
import com.metro.clock.data.AlarmLogic
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroContextMenuActiveShift
import com.metro.ui.MetroContextMenuClearOnDismiss
import com.metro.ui.MetroContextMenuDimmedAlpha
import com.metro.ui.MetroContextMenuItem
import com.metro.ui.MetroContextMenuPopup
import com.metro.ui.MetroDimens
import com.metro.ui.MetroEmptyState
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.MetroTransitions
import com.metro.ui.metroClickable

data class AlarmsChrome(
    val onAdd: () -> Unit,
)

private class RectRef {
    var value: Rect = Rect.Zero
}

@Composable
fun AlarmsListScreen(
    alarms: List<AlarmItem>,
    onAdd: () -> Unit,
    onOpen: (AlarmItem) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onChromeChange: (AlarmsChrome) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    var menuAnchor by remember { mutableStateOf(Rect.Zero) }
    val popupRootBounds = remember { RectRef() }
    val contextMenuVisible = remember { MutableTransitionState(false) }
    val contextMenuFocusFraction by updateTransition(
        contextMenuVisible,
        label = "alarmContextMenuFocus",
    ).animateFloat(
        transitionSpec = { tween(MetroTransitions.AppBarSlideMs) },
        label = "focus",
    ) { visible -> if (visible) 1f else 0f }

    LaunchedEffect(Unit) {
        onChromeChange(AlarmsChrome(onAdd = onAdd))
    }

    MetroContextMenuClearOnDismiss(contextMenuVisible) {
        menuAlarm = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                popupRootBounds.value = coordinates.boundsInWindow()
            },
    ) {
        if (alarms.isEmpty()) {
            MetroEmptyState(
                message = "no alarms",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = MetroAppBarDefaults.BarHeight + 16.dp,
                ),
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmRow(
                        alarm = alarm,
                        contextMenuTarget = menuAlarm?.id == alarm.id,
                        contextMenuFocusFraction = contextMenuFocusFraction,
                        onOpen = { onOpen(alarm) },
                        onToggle = { onToggle(alarm.id, it) },
                        onLongPress = { bounds ->
                            menuAlarm = alarm
                            menuAnchor = bounds
                            contextMenuVisible.targetState = true
                        },
                    )
                }
            }
        }

        menuAlarm?.let { alarm ->
            MetroContextMenuPopup(
                visibleState = contextMenuVisible,
                anchorBounds = menuAnchor,
                rootBounds = popupRootBounds.value,
                items = listOf(
                    MetroContextMenuItem(
                        label = "edit",
                        onClick = {
                            contextMenuVisible.targetState = false
                            onOpen(alarm)
                        },
                    ),
                    MetroContextMenuItem(
                        label = "delete",
                        onClick = {
                            contextMenuVisible.targetState = false
                            onDelete(alarm.id)
                        },
                    ),
                ),
                onDismissRequest = { contextMenuVisible.targetState = false },
            )
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: AlarmItem,
    contextMenuTarget: Boolean,
    contextMenuFocusFraction: Float,
    onOpen: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onLongPress: (Rect) -> Unit,
) {
    val density = LocalDensity.current
    val activeShiftPx = with(density) { MetroContextMenuActiveShift.toPx() }
    val rowBounds = remember { RectRef() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp)
            .clipToBounds()
            .graphicsLayer {
                if (contextMenuTarget) {
                    translationX = -contextMenuFocusFraction * activeShiftPx
                    alpha = 1f
                } else {
                    translationX = 0f
                    alpha = 1f - contextMenuFocusFraction * (1f - MetroContextMenuDimmedAlpha)
                }
            }
            .onGloballyPositioned { coordinates ->
                rowBounds.value = coordinates.boundsInWindow()
            }
            .metroClickable(
                onClick = onOpen,
                onLongClick = { onLongPress(rowBounds.value) },
            )
            .padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                top = 10.dp,
                bottom = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            BasicText(
                text = AlarmLogic.formatTime(alarm.hour, alarm.minute),
                style = MetroTextStyle.ListItemTitle
                    .toTextStyle(MetroTheme.fontFamily)
                    .copy(
                        color = MetroTheme.colors.primaryText,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                    ),
            )
            MetroText(
                text = AlarmLogic.statusLabel(alarm.enabled, alarm.name),
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText,
                modifier = Modifier.padding(top = 2.dp),
            )
            MetroText(
                text = AlarmLogic.formatRepeatSummary(alarm.repeatDays),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        MetroToggleSwitch(
            checked = alarm.enabled,
            onCheckedChange = onToggle,
            showStatus = false,
        )
    }
}
