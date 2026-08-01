package com.metro.statusbar.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.metro.statusbar.ActionCenterSpec
import com.metro.statusbar.ActionCenterState
import com.metro.statusbar.ActionNotificationItem
import com.metro.statusbar.QuickActionType
import com.metro.statusbar.TraySnapshot
import com.metro.statusbar.TraySpec
import com.metro.ui.MetroTransitions
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Combined WP8.1 system tray + Action Center shade hosted in the overlay window.
 *
 * - Tap tray → reveal indicators (existing behavior)
 * - Swipe down on tray → open Action Center
 * - Swipe up on shade / handle → close Action Center
 */
@Composable
fun StatusShell(
    traySnapshot: TraySnapshot,
    actionCenter: ActionCenterState,
    onTrayTap: () -> Unit,
    onOpenFractionChanged: (Float) -> Unit,
    barHeightDp: Int = TraySpec.TRAY_HEIGHT_DP,
    startPaddingDp: Int = TraySpec.START_PADDING_DP,
    endPaddingDp: Int = TraySpec.END_PADDING_DP,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val openAnim = remember { Animatable(actionCenter.openFraction) }
    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(actionCenter.openFraction) {
        // External close (e.g. All Settings) snaps the shade and restores wrap-content height.
        if (abs(actionCenter.openFraction - openAnim.value) > 0.001f) {
            openAnim.stop()
            openAnim.snapTo(actionCenter.openFraction)
            onOpenFractionChanged(actionCenter.openFraction)
        }
    }

    fun animateTo(target: Float) {
        scope.launch {
            openAnim.animateTo(
                targetValue = target.coerceIn(0f, 1f),
                animationSpec = tween(
                    durationMillis = if (target > 0.5f) {
                        ActionCenterSpec.OPEN_MS
                    } else {
                        ActionCenterSpec.CLOSE_MS
                    },
                    easing = MetroTransitions.PageEasing,
                ),
            )
            onOpenFractionChanged(openAnim.value)
        }
    }

    fun applyFraction(next: Float) {
        val clamped = next.coerceIn(0f, 1f)
        scope.launch {
            openAnim.snapTo(clamped)
            onOpenFractionChanged(clamped)
        }
    }

    val fraction = openAnim.value
    val shadeVisible = fraction > 0.001f
    val trayHeight = if (traySnapshot.actionCenterOpen || shadeVisible) {
        maxOf(barHeightDp, 44)
    } else {
        barHeightDp
    }

    Column(modifier = modifier.fillMaxWidth().then(if (shadeVisible) Modifier.fillMaxHeight() else Modifier)) {
        StatusTray(
            snapshot = traySnapshot,
            onTrayTap = {
                if (fraction > 0.9f) {
                    animateTo(0f)
                } else {
                    onTrayTap()
                }
            },
            barHeightDp = trayHeight,
            startPaddingDp = startPaddingDp,
            endPaddingDp = endPaddingDp,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    val travelPx = with(density) { 420.dp.toPx() }
                    detectVerticalDragGestures(
                        onDragStart = {
                            dragAccumulated = openAnim.value
                            scope.launch { openAnim.stop() }
                        },
                        onVerticalDrag = { _, dragAmount ->
                            dragAccumulated = (dragAccumulated + dragAmount / travelPx).coerceIn(0f, 1f)
                            applyFraction(dragAccumulated)
                        },
                        onDragEnd = {
                            animateTo(if (openAnim.value >= 0.28f) 1f else 0f)
                        },
                        onDragCancel = {
                            animateTo(if (openAnim.value >= 0.28f) 1f else 0f)
                        },
                    )
                },
        )

        val callBanner = actionCenter.activeCallBanner
        if (callBanner != null && !shadeVisible) {
            ActiveCallBannerBar(
                banner = callBanner,
                onClick = { actionCenter.openActiveCallBanner() },
            )
        }

        if (shadeVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .pointerInput(Unit) {
                        val travelPx = with(density) { 420.dp.toPx() }
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragAccumulated = openAnim.value
                                scope.launch { openAnim.stop() }
                            },
                            onVerticalDrag = { _, dragAmount ->
                                dragAccumulated =
                                    (dragAccumulated + dragAmount / travelPx).coerceIn(0f, 1f)
                                applyFraction(dragAccumulated)
                            },
                            onDragEnd = {
                                animateTo(if (openAnim.value >= 0.55f) 1f else 0f)
                            },
                            onDragCancel = {
                                animateTo(if (openAnim.value >= 0.55f) 1f else 0f)
                            },
                        )
                    },
            ) {
                ActionCenterShade(
                    slots = actionCenter.slots,
                    notificationGroups = actionCenter.notificationGroups,
                    accent = traySnapshot.theme.accentColor,
                    darkTheme = traySnapshot.theme.darkTheme,
                    foreground = traySnapshot.theme.foregroundColor,
                    background = traySnapshot.theme.backgroundColor,
                    onToggleQuickAction = { type: QuickActionType ->
                        actionCenter.toggleQuickAction(type)
                    },
                    onClearAll = { actionCenter.clearAllNotifications() },
                    onAllSettings = { actionCenter.openAllSettings() },
                    onNotificationClick = { item: ActionNotificationItem ->
                        actionCenter.openNotification(item)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction.coerceIn(0.02f, 1f)),
                )
            }
        }
    }
}
