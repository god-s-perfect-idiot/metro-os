package com.metro.volume.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroStepSlider
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.volume.VolumeHudLogic
import com.metro.volume.VolumeHudSnapshot
import com.metro.volume.VolumeHudSpec
import com.metro.volume.VolumeStreamKind

/** WP8.1 volume control HUD — collapsed strip or expanded dual-slider panel. */
@Composable
fun VolumeHud(
    snapshot: VolumeHudSnapshot,
    onToggleExpanded: () -> Unit,
    onCollapse: () -> Unit,
    onRingerLevel: (Int) -> Unit,
    onMediaLevel: (Int) -> Unit,
    onCallLevel: (Int) -> Unit,
    onToggleRingerMute: () -> Unit,
    onToggleMediaMute: () -> Unit,
    onToggleVibrate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!snapshot.visible) return

    MetroTheme(darkTheme = true, accent = snapshot.accentColor) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(VolumeHudSpec.PanelBackground),
        ) {
            if (snapshot.expanded) {
                ExpandedVolumePanel(
                    snapshot = snapshot,
                    accent = snapshot.accentColor,
                    onCollapse = onCollapse,
                    onRingerLevel = onRingerLevel,
                    onMediaLevel = onMediaLevel,
                    onCallLevel = onCallLevel,
                    onToggleRingerMute = onToggleRingerMute,
                    onToggleMediaMute = onToggleMediaMute,
                    onToggleVibrate = onToggleVibrate,
                )
            } else {
                CollapsedVolumeStrip(
                    snapshot = snapshot,
                    onExpand = onToggleExpanded,
                )
            }
        }
    }
}

@Composable
private fun CollapsedVolumeStrip(
    snapshot: VolumeHudSnapshot,
    onExpand: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(VolumeHudSpec.COLLAPSED_HEIGHT_DP.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExpand,
            )
            .padding(horizontal = VolumeHudSpec.HORIZONTAL_PADDING_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroText(
            text = VolumeHudLogic.formatLevelDigits(snapshot.collapsedLevel),
            style = MetroTextStyle.ListItemTitle,
            color = VolumeHudSpec.PrimaryText,
        )
        MetroText(
            text = VolumeHudLogic.formatMaxSuffix(snapshot.collapsedMax),
            style = MetroTextStyle.ListItemSubtitle,
            color = VolumeHudSpec.SecondaryText,
            modifier = Modifier.padding(start = 2.dp, end = 10.dp),
        )
        MetroText(
            text = snapshot.collapsedLabel,
            style = MetroTextStyle.ListItemSubtitle,
            color = VolumeHudSpec.PrimaryText,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        ChevronIcon(pointingDown = true, color = VolumeHudSpec.PrimaryText)
    }
}

@Composable
private fun ExpandedVolumePanel(
    snapshot: VolumeHudSnapshot,
    accent: Color,
    onCollapse: () -> Unit,
    onRingerLevel: (Int) -> Unit,
    onMediaLevel: (Int) -> Unit,
    onCallLevel: (Int) -> Unit,
    onToggleRingerMute: () -> Unit,
    onToggleMediaMute: () -> Unit,
    onToggleVibrate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VolumeHudSpec.HORIZONTAL_PADDING_DP.dp)
            .padding(top = 8.dp, bottom = 4.dp),
    ) {
        if (snapshot.inCall) {
            VolumeStreamRow(
                kind = VolumeStreamKind.Call,
                level = snapshot.callLevel,
                muted = snapshot.callLevel <= 0,
                onLevel = onCallLevel,
                onToggleMute = { onCallLevel(if (snapshot.callLevel <= 0) 5 else 0) },
            )
        } else {
            VolumeStreamRow(
                kind = VolumeStreamKind.Ringer,
                level = snapshot.ringerLevel,
                muted = snapshot.ringerLevel <= 0,
                onLevel = onRingerLevel,
                onToggleMute = onToggleRingerMute,
            )
            Spacer(modifier = Modifier.height(4.dp))
            VolumeStreamRow(
                kind = VolumeStreamKind.Media,
                level = snapshot.mediaLevel,
                muted = snapshot.mediaLevel <= 0,
                onLevel = onMediaLevel,
                onToggleMute = onToggleMediaMute,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(VolumeHudSpec.BOTTOM_ROW_HEIGHT_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (!snapshot.inCall) {
                MetroText(
                    text = if (snapshot.vibrateOn) "VIBRATE" else "vibrate off",
                    style = MetroTextStyle.ListItemSubtitle,
                    color = if (snapshot.vibrateOn) accent else VolumeHudSpec.SecondaryText,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleVibrate,
                    ),
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
            ChevronIcon(
                pointingDown = false,
                color = VolumeHudSpec.PrimaryText,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCollapse,
                ),
            )
        }
    }
}

@Composable
private fun VolumeStreamRow(
    kind: VolumeStreamKind,
    level: Int,
    muted: Boolean,
    onLevel: (Int) -> Unit,
    onToggleMute: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetroText(
                text = VolumeHudLogic.formatLevelDigits(level),
                style = MetroTextStyle.ListItemTitle,
                color = VolumeHudSpec.PrimaryText,
            )
            MetroText(
                text = VolumeHudLogic.formatMaxSuffix(kind.wpMax),
                style = MetroTextStyle.ListItemSubtitle,
                color = VolumeHudSpec.SecondaryText,
                modifier = Modifier.padding(start = 2.dp, end = 8.dp),
            )
            MetroText(
                text = kind.label,
                style = MetroTextStyle.ListItemSubtitle,
                color = VolumeHudSpec.PrimaryText,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            StreamMuteIcon(
                kind = kind,
                muted = muted,
                color = if (muted) VolumeHudSpec.SecondaryText else VolumeHudSpec.PrimaryText,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleMute,
                    ),
            )
        }
        MetroStepSlider(
            index = level,
            onIndexChange = onLevel,
            stepCount = kind.wpMax + 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChevronIcon(
    pointingDown: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val strokeWidth = size.minDimension * 0.1f
        val midX = size.width / 2f
        val top = size.height * 0.32f
        val bot = size.height * 0.68f
        val wing = size.width * 0.28f
        if (pointingDown) {
            drawLine(color, Offset(midX - wing, top), Offset(midX, bot), strokeWidth, StrokeCap.Square)
            drawLine(color, Offset(midX + wing, top), Offset(midX, bot), strokeWidth, StrokeCap.Square)
        } else {
            drawLine(color, Offset(midX - wing, bot), Offset(midX, top), strokeWidth, StrokeCap.Square)
            drawLine(color, Offset(midX + wing, bot), Offset(midX, top), strokeWidth, StrokeCap.Square)
        }
    }
}

@Composable
private fun StreamMuteIcon(
    kind: VolumeStreamKind,
    muted: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.padding(6.dp)) {
        val w = size.width
        val h = size.height
        when (kind) {
            VolumeStreamKind.Ringer, VolumeStreamKind.Call -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    quadraticBezierTo(w * 0.82f, h * 0.18f, w * 0.78f, h * 0.55f)
                    lineTo(w * 0.88f, h * 0.68f)
                    lineTo(w * 0.12f, h * 0.68f)
                    lineTo(w * 0.22f, h * 0.55f)
                    quadraticBezierTo(w * 0.18f, h * 0.18f, w * 0.5f, h * 0.12f)
                    close()
                }
                drawPath(path, color)
                drawRect(
                    color = color,
                    topLeft = Offset(w * 0.38f, h * 0.72f),
                    size = Size(w * 0.24f, h * 0.14f),
                )
            }
            VolumeStreamKind.Media -> {
                drawCircle(color, w * 0.16f, Offset(w * 0.32f, h * 0.72f))
                drawCircle(color, w * 0.16f, Offset(w * 0.68f, h * 0.62f))
                drawLine(
                    color,
                    Offset(w * 0.46f, h * 0.72f),
                    Offset(w * 0.46f, h * 0.22f),
                    strokeWidth = w * 0.08f,
                    cap = StrokeCap.Square,
                )
                drawLine(
                    color,
                    Offset(w * 0.82f, h * 0.62f),
                    Offset(w * 0.82f, h * 0.18f),
                    strokeWidth = w * 0.08f,
                    cap = StrokeCap.Square,
                )
                drawLine(
                    color,
                    Offset(w * 0.46f, h * 0.22f),
                    Offset(w * 0.82f, h * 0.18f),
                    strokeWidth = w * 0.08f,
                    cap = StrokeCap.Square,
                )
            }
        }
        if (muted) {
            drawLine(
                color = Color(0xFFFF5252),
                start = Offset(w * 0.1f, h * 0.85f),
                end = Offset(w * 0.9f, h * 0.15f),
                strokeWidth = w * 0.1f,
                cap = StrokeCap.Square,
            )
        }
    }
}
