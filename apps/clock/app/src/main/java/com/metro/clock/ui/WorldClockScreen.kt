package com.metro.clock.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.runtime.mutableLongStateOf
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
import com.metro.clock.data.WorldClockEntry
import com.metro.clock.data.WorldClockLogic
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroCheckBox
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
import com.metro.ui.MetroTransitions
import com.metro.ui.metroClickable
import kotlinx.coroutines.delay

data class WorldClockChrome(
    val selecting: Boolean,
    val canDelete: Boolean,
    val showSelectMenu: Boolean,
    val onAdd: () -> Unit,
    val onEnterSelect: () -> Unit,
    val onDeleteSelected: () -> Unit,
    val onCancelSelect: () -> Unit,
)

private class WorldRectRef {
    var value: Rect = Rect.Zero
}

@Composable
fun WorldClockScreen(
    entries: List<WorldClockEntry>,
    onAdd: () -> Unit,
    onDelete: (Set<String>) -> Unit,
    onChromeChange: (WorldClockChrome) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var menuEntry by remember { mutableStateOf<WorldClockEntry?>(null) }
    var menuAnchor by remember { mutableStateOf(Rect.Zero) }
    val popupRootBounds = remember { WorldRectRef() }
    val contextMenuVisible = remember { MutableTransitionState(false) }
    val contextMenuFocusFraction by updateTransition(
        contextMenuVisible,
        label = "worldContextMenuFocus",
    ).animateFloat(
        transitionSpec = { tween(MetroTransitions.AppBarSlideMs) },
        label = "focus",
    ) { visible -> if (visible) 1f else 0f }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    LaunchedEffect(selecting) {
        if (!selecting) selected = emptySet()
    }

    LaunchedEffect(selecting, selected, entries.size) {
        onChromeChange(
            WorldClockChrome(
                selecting = selecting,
                canDelete = selected.isNotEmpty(),
                showSelectMenu = !selecting && entries.isNotEmpty(),
                onAdd = onAdd,
                onEnterSelect = { selecting = true },
                onDeleteSelected = {
                    onDelete(selected)
                    selecting = false
                },
                onCancelSelect = { selecting = false },
            ),
        )
    }

    MetroContextMenuClearOnDismiss(contextMenuVisible) {
        menuEntry = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                popupRootBounds.value = coordinates.boundsInWindow()
            },
    ) {
        if (entries.isEmpty()) {
            MetroEmptyState(
                message = "no cities",
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
                items(entries, key = { it.city.id }) { entry ->
                    WorldClockRow(
                        entry = entry,
                        nowMillis = nowMillis,
                        selecting = selecting,
                        checked = entry.city.id in selected,
                        contextMenuTarget = menuEntry?.city?.id == entry.city.id,
                        contextMenuFocusFraction = contextMenuFocusFraction,
                        onToggleSelect = {
                            selected = if (entry.city.id in selected) {
                                selected - entry.city.id
                            } else {
                                selected + entry.city.id
                            }
                        },
                        onLongPress = { bounds ->
                            if (!selecting) {
                                menuEntry = entry
                                menuAnchor = bounds
                                contextMenuVisible.targetState = true
                            }
                        },
                    )
                }
            }
        }

        menuEntry?.let { entry ->
            MetroContextMenuPopup(
                visibleState = contextMenuVisible,
                anchorBounds = menuAnchor,
                rootBounds = popupRootBounds.value,
                items = listOf(
                    MetroContextMenuItem(
                        label = "delete",
                        onClick = {
                            contextMenuVisible.targetState = false
                            onDelete(setOf(entry.city.id))
                        },
                    ),
                ),
                onDismissRequest = { contextMenuVisible.targetState = false },
            )
        }
    }
}

@Composable
private fun WorldClockRow(
    entry: WorldClockEntry,
    nowMillis: Long,
    selecting: Boolean,
    checked: Boolean,
    contextMenuTarget: Boolean,
    contextMenuFocusFraction: Float,
    onToggleSelect: () -> Unit,
    onLongPress: (Rect) -> Unit,
) {
    val density = LocalDensity.current
    val activeShiftPx = with(density) { MetroContextMenuActiveShift.toPx() }
    val rowBounds = remember { WorldRectRef() }

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
                onClick = if (selecting) onToggleSelect else ({}),
                onLongClick = if (selecting) null else ({ onLongPress(rowBounds.value) }),
            )
            .padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                top = 10.dp,
                bottom = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            BasicText(
                text = WorldClockLogic.formatLocalTime(entry.city.zoneId, nowMillis),
                style = MetroTextStyle.ListItemTitle
                    .toTextStyle(MetroTheme.fontFamily)
                    .copy(
                        color = MetroTheme.colors.primaryText,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                    ),
            )
            MetroText(
                text = entry.city.name,
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.primaryText,
                modifier = Modifier.padding(top = 2.dp),
            )
            MetroText(
                text = WorldClockLogic.formatOffsetLabel(entry.city.zoneId, nowMillis),
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (selecting) {
            MetroCheckBox(
                checked = checked,
                onCheckedChange = { onToggleSelect() },
            )
        }
    }
}
