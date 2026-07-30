package com.metro.statusbar.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.metro.statusbar.ActionCenterSpec
import com.metro.statusbar.ActionNotificationGroup
import com.metro.statusbar.ActionNotificationItem
import com.metro.statusbar.QuickActionSlot
import com.metro.statusbar.QuickActionType
import com.metro.system.MetroAppBranding
import com.metro.ui.MetroColors
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle

/**
 * WP8.1 Action Center body (below the system tray strip): quick actions, Clear All / All Settings,
 * and notification groups. Height is controlled by the parent via [Modifier].
 */
@Composable
fun ActionCenterShade(
    slots: List<QuickActionSlot>,
    notificationGroups: List<ActionNotificationGroup>,
    accent: Color,
    darkTheme: Boolean,
    foreground: Color,
    background: Color,
    onToggleQuickAction: (QuickActionType) -> Unit,
    onClearAll: () -> Unit,
    onAllSettings: () -> Unit,
    onNotificationClick: (ActionNotificationItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inactiveTile = if (darkTheme) {
        Color(ActionCenterSpec.INACTIVE_TILE_DARK)
    } else {
        Color(ActionCenterSpec.INACTIVE_TILE_LIGHT)
    }
    val secondary = MetroColors.secondaryText(darkTheme)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .testTag("metro_action_center"),
    ) {
        QuickActionRow(
            slots = slots,
            accent = accent,
            inactive = inactiveTile,
            contentColor = MetroColors.TileContentOnAccent,
            onToggle = onToggleQuickAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ActionCenterSpec.TILE_HORIZONTAL_PADDING_DP.dp)
                .padding(top = ActionCenterSpec.TILE_TOP_PADDING_DP.dp),
        )

        ActionsLinkRow(
            foreground = foreground,
            showClearAll = notificationGroups.isNotEmpty(),
            onClearAll = onClearAll,
            onAllSettings = onAllSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ActionCenterSpec.CONTENT_HORIZONTAL_DP.dp)
                .padding(
                    top = ActionCenterSpec.ACTIONS_ROW_TOP_DP.dp,
                    bottom = ActionCenterSpec.ACTIONS_ROW_BOTTOM_DP.dp,
                ),
        )

        Box(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth(),
        ) {
            if (notificationGroups.isEmpty()) {
                MetroText(
                    text = "No notifications",
                    style = MetroTextStyle.HubTitle,
                    color = secondary,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp)
                        .testTag("metro_action_center_empty"),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = ActionCenterSpec.CONTENT_HORIZONTAL_DP.dp),
                    verticalArrangement = Arrangement.spacedBy(ActionCenterSpec.GROUP_SPACING_DP.dp),
                ) {
                    notificationGroups.forEach { group ->
                        NotificationGroup(
                            group = group,
                            foreground = foreground,
                            secondary = secondary,
                            onItemClick = onNotificationClick,
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        ActionCenterHandle(
            accent = accent,
            modifier = Modifier
                .fillMaxWidth()
                .height(ActionCenterSpec.HANDLE_BAR_HEIGHT_DP.dp),
        )
    }
}

@Composable
private fun QuickActionRow(
    slots: List<QuickActionSlot>,
    accent: Color,
    inactive: Color,
    contentColor: Color,
    onToggle: (QuickActionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(ActionCenterSpec.TILE_HEIGHT_DP.dp),
        horizontalArrangement = Arrangement.spacedBy(ActionCenterSpec.TILE_GAP_DP.dp),
    ) {
        slots.forEach { slot ->
            QuickActionTile(
                slot = slot,
                background = if (slot.enabled) accent else inactive,
                contentColor = contentColor,
                onClick = { onToggle(slot.type) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    slot: QuickActionSlot,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 10.dp)
            .testTag("metro_quick_action_${slot.type.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(modifier = Modifier.size(ActionCenterSpec.TILE_ICON_SIZE_DP.dp)) {
            drawQuickActionIcon(slot.type, contentColor)
        }
        Spacer(modifier = Modifier.height(8.dp))
        MetroText(
            text = slot.label.uppercase(java.util.Locale.getDefault()),
            style = MetroTextStyle.AppTitle,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ActionsLinkRow(
    foreground: Color,
    showClearAll: Boolean,
    onClearAll: () -> Unit,
    onAllSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showClearAll) {
            Row(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClearAll,
                    )
                    .testTag("metro_action_clear_all"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ActionCenterSpec.ACTIONS_ROW_ICON_GAP_DP.dp),
            ) {
                Canvas(modifier = Modifier.size(18.dp)) {
                    val stroke = Stroke(width = size.minDimension * 0.12f, cap = StrokeCap.Round)
                    drawLine(
                        color = foreground,
                        start = Offset(size.width * 0.2f, size.height * 0.2f),
                        end = Offset(size.width * 0.8f, size.height * 0.8f),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = foreground,
                        start = Offset(size.width * 0.8f, size.height * 0.2f),
                        end = Offset(size.width * 0.2f, size.height * 0.8f),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                }
                MetroText(
                    text = "clear all",
                    style = MetroTextStyle.DialogBody,
                    color = foreground,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAllSettings,
                )
                .testTag("metro_action_all_settings"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ActionCenterSpec.ACTIONS_ROW_ICON_GAP_DP.dp),
        ) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val r = size.minDimension * 0.18f
                val stroke = Stroke(width = size.minDimension * 0.1f)
                drawCircle(foreground, radius = size.minDimension * 0.22f, style = stroke)
                drawCircle(foreground, radius = r)
                // Simple gear teeth as short radial ticks.
                repeat(8) { i ->
                    val angle = Math.toRadians(i * 45.0)
                    val inner = size.minDimension * 0.32f
                    val outer = size.minDimension * 0.46f
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawLine(
                        color = foreground,
                        start = Offset(
                            cx + (inner * kotlin.math.cos(angle)).toFloat(),
                            cy + (inner * kotlin.math.sin(angle)).toFloat(),
                        ),
                        end = Offset(
                            cx + (outer * kotlin.math.cos(angle)).toFloat(),
                            cy + (outer * kotlin.math.sin(angle)).toFloat(),
                        ),
                        strokeWidth = size.minDimension * 0.1f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            MetroText(
                text = "all settings",
                style = MetroTextStyle.DialogBody,
                color = foreground,
            )
        }
    }
}

@Composable
private fun NotificationGroup(
    group: ActionNotificationGroup,
    foreground: Color,
    secondary: Color,
    onItemClick: (ActionNotificationItem) -> Unit,
) {
    val context = LocalContext.current
    val iconAsset = remember(group.packageName) {
        MetroAppBranding.loadAppIconAsset(context, group.packageName)
    }
    Column(verticalArrangement = Arrangement.spacedBy(ActionCenterSpec.ITEM_VERTICAL_GAP_DP.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppGlyph(
                drawable = iconAsset.drawable,
                background = iconAsset.backgroundColor,
                modifier = Modifier.size(ActionCenterSpec.GROUP_ICON_DP.dp),
            )
            Spacer(modifier = Modifier.width(ActionCenterSpec.GROUP_TITLE_GAP_DP.dp))
            MetroText(
                text = group.appLabel,
                style = MetroTextStyle.SectionHeader,
                color = foreground,
            )
        }
        group.items.forEach { item ->
            NotificationRow(
                item = item,
                foreground = foreground,
                secondary = secondary,
                onClick = { onItemClick(item) },
            )
        }
    }
}

@Composable
private fun NotificationRow(
    item: ActionNotificationItem,
    foreground: Color,
    secondary: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .testTag("metro_action_notification"),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MetroText(
                text = item.title,
                style = MetroTextStyle.ListItemTitle,
                color = foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!item.body.isNullOrBlank()) {
                MetroText(
                    text = item.body,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        MetroText(
            text = item.timeText,
            style = MetroTextStyle.DialogBody,
            color = secondary,
        )
    }
}

@Composable
private fun AppGlyph(
    drawable: Drawable?,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (drawable != null) {
            val bitmap = remember(drawable) {
                drawable.toBitmap(
                    width = (ActionCenterSpec.GROUP_ICON_DP * 3),
                    height = (ActionCenterSpec.GROUP_ICON_DP * 3),
                )
            }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(3.dp),
            )
        }
    }
}

@Composable
private fun ActionCenterHandle(
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(accent),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(ActionCenterSpec.HANDLE_GRIP_WIDTH_DP.dp)
                .height(ActionCenterSpec.HANDLE_GRIP_HEIGHT_DP.dp)
                .background(Color.White.copy(alpha = 0.85f)),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawQuickActionIcon(
    type: QuickActionType,
    color: Color,
) {
    val w = size.width
    val h = size.height
    when (type) {
        QuickActionType.Wifi -> {
            val anchor = Offset(w * 0.5f, h * 0.78f)
            val stroke = w * 0.1f
            for (i in 1..3) {
                val radius = w * (0.12f + i * 0.16f)
                drawArc(
                    color = color,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(anchor.x - radius, anchor.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            drawCircle(color, w * 0.06f, anchor)
        }
        QuickActionType.Bluetooth -> {
            val stroke = w * 0.1f
            val cx = w / 2f
            drawLine(color, Offset(cx, h * 0.12f), Offset(cx, h * 0.88f), stroke, StrokeCap.Round)
            drawLine(color, Offset(cx, h * 0.12f), Offset(cx + w * 0.28f, h * 0.32f), stroke, StrokeCap.Round)
            drawLine(color, Offset(cx + w * 0.28f, h * 0.32f), Offset(cx - w * 0.08f, h * 0.5f), stroke, StrokeCap.Round)
            drawLine(color, Offset(cx, h * 0.88f), Offset(cx + w * 0.28f, h * 0.68f), stroke, StrokeCap.Round)
            drawLine(color, Offset(cx + w * 0.28f, h * 0.68f), Offset(cx - w * 0.08f, h * 0.5f), stroke, StrokeCap.Round)
        }
        QuickActionType.Airplane -> {
            val path = Path().apply {
                moveTo(w * 0.12f, h * 0.55f)
                lineTo(w * 0.45f, h * 0.48f)
                lineTo(w * 0.38f, h * 0.22f)
                lineTo(w * 0.48f, h * 0.22f)
                lineTo(w * 0.62f, h * 0.45f)
                lineTo(w * 0.88f, h * 0.38f)
                lineTo(w * 0.9f, h * 0.48f)
                lineTo(w * 0.62f, h * 0.55f)
                lineTo(w * 0.55f, h * 0.78f)
                lineTo(w * 0.45f, h * 0.78f)
                lineTo(w * 0.5f, h * 0.58f)
                close()
            }
            drawPath(path, color)
        }
        QuickActionType.InternetSharing -> {
            // Broadcast / hotspot: center disc + radiating arcs.
            drawCircle(color, w * 0.1f, Offset(w / 2f, h / 2f))
            val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
            for (i in 1..2) {
                val radius = w * (0.22f + i * 0.16f)
                drawArc(
                    color = color,
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(w / 2f - radius, h / 2f - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = stroke,
                )
            }
        }
    }
}
