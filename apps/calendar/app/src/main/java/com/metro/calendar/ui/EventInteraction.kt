package com.metro.calendar.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.metro.calendar.R
import com.metro.calendar.data.CalendarEvent
import com.metro.ui.MetroContextMenuActiveShift
import com.metro.ui.MetroContextMenuClearOnDismiss
import com.metro.ui.MetroContextMenuDimmedAlpha
import com.metro.ui.MetroContextMenuItem
import com.metro.ui.MetroContextMenuPopup
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroTransitions

/** Mutable holder so layout callbacks can update without triggering recomposition. */
internal class EventBoundsRef {
    var value: Rect = Rect.Zero
}

internal fun CalendarEvent.matchesContextTarget(target: CalendarEvent?): Boolean =
    target != null && id == target.id && startMillis == target.startMillis

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.calendarEventInteraction(
    isContextTarget: Boolean,
    contextMenuFocusFraction: Float,
    boundsRef: EventBoundsRef,
    onClick: () -> Unit,
    onLongPress: (Rect) -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val activeShiftPx = with(density) { MetroContextMenuActiveShift.toPx() }
    val dimmed = contextMenuFocusFraction > 0f && !isContextTarget
    val nudge = if (isContextTarget) contextMenuFocusFraction * activeShiftPx else 0f

    return this
        .graphicsLayer {
            alpha = if (dimmed) {
                1f - contextMenuFocusFraction * (1f - MetroContextMenuDimmedAlpha)
            } else {
                1f
            }
            translationX = -nudge
        }
        .onGloballyPositioned { coordinates ->
            boundsRef.value = coordinates.boundsInWindow()
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = { onLongPress(boundsRef.value) },
        )
}

internal class CalendarEventContextMenuState {
    var event by mutableStateOf<CalendarEvent?>(null)
        private set
    var pendingDelete by mutableStateOf<CalendarEvent?>(null)
        private set
    var anchorBounds by mutableStateOf(Rect.Zero)
        private set
    var rootBounds by mutableStateOf(Rect.Zero)
        private set
    val visibleState = MutableTransitionState(false)

    fun open(event: CalendarEvent, anchor: Rect, root: Rect) {
        pendingDelete = null
        anchorBounds = anchor
        rootBounds = root
        this.event = event
        visibleState.targetState = true
    }

    fun dismiss() {
        visibleState.targetState = false
    }

    fun requestDelete(event: CalendarEvent) {
        visibleState.targetState = false
        pendingDelete = event
    }

    fun clearEvent() {
        event = null
    }

    fun clearPendingDelete() {
        pendingDelete = null
        event = null
    }
}

@Composable
internal fun rememberCalendarEventContextMenuState(): CalendarEventContextMenuState =
    remember { CalendarEventContextMenuState() }

@Composable
internal fun rememberContextMenuFocusFraction(
    visibleState: MutableTransitionState<Boolean>,
): Float {
    val transition = updateTransition(visibleState, label = "calendarEventContextMenuFocus")
    val fraction by transition.animateFloat(
        transitionSpec = { tween(MetroTransitions.AppBarSlideMs) },
        label = "focusFraction",
    ) { visible -> if (visible) 1f else 0f }
    return fraction
}

/**
 * Hold-to-expand event menu + delete confirmation. Callers open it via
 * [CalendarEventContextMenuState.open].
 */
@Composable
internal fun CalendarEventContextMenu(
    menuState: CalendarEventContextMenuState,
    onDeleteConfirmed: (CalendarEvent) -> Unit,
) {
    MetroContextMenuClearOnDismiss(menuState.visibleState) {
        if (menuState.pendingDelete == null) {
            menuState.clearEvent()
        }
    }

    menuState.event?.let { event ->
        val deleteLabel = stringResource(R.string.event_delete)
        MetroContextMenuPopup(
            visibleState = menuState.visibleState,
            anchorBounds = menuState.anchorBounds,
            rootBounds = menuState.rootBounds,
            items = listOf(
                MetroContextMenuItem(
                    label = deleteLabel,
                    onClick = { menuState.requestDelete(event) },
                ),
            ),
            onDismissRequest = menuState::dismiss,
        )
    }

    menuState.pendingDelete?.let { event ->
        MetroMessageDialog(
            title = stringResource(R.string.event_delete_title),
            body = stringResource(R.string.event_delete_body),
            confirmLabel = stringResource(R.string.event_delete_confirm),
            onConfirm = {
                onDeleteConfirmed(event)
                menuState.clearPendingDelete()
            },
            dismissLabel = stringResource(R.string.event_delete_cancel),
            onDismiss = { menuState.clearPendingDelete() },
            onDismissRequest = { menuState.clearPendingDelete() },
        )
    }
}
