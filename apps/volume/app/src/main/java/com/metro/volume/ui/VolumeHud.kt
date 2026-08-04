package com.metro.volume.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroBarStepSlider
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.volume.VolumeHudLogic
import com.metro.volume.VolumeHudSnapshot
import com.metro.volume.VolumeHudSpec
import com.metro.volume.VolumeStreamKind

/**
 * WP8.1 volume control HUD — collapsed strip or expanded dual-slider panel.
 *
 * Show / hide and expand / collapse both use a top-anchored height wipe inside a
 * fixed-size overlay window (see [onWindowHeightDp]). The collapsed header row is
 * always the real top-bar chrome; expand only reveals the body beneath it.
 * Animating `WRAP_CONTENT` overlay height every frame is jittery on WindowManager.
 * Call [onExitFinished] after the hide wipe so the host can drop the WindowManager view.
 */
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
    onWindowHeightDp: (Int) -> Unit = {},
    onExitFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    MetroTheme(darkTheme = true, accent = snapshot.accentColor) {
        val targetHeightDp = if (snapshot.visible) {
            VolumeHudSpec.panelHeightDp(
                expanded = snapshot.expanded,
                inCall = snapshot.inCall,
            )
        } else {
            0
        }
        val expandedRestingDp = VolumeHudSpec.panelHeightDp(
            expanded = true,
            inCall = snapshot.inCall,
        )
        val bodyRestingDp = expandedRestingDp - VolumeHudSpec.COLLAPSED_HEIGHT_DP
        val heightAnim = remember { Animatable(0f) }

        LaunchedEffect(snapshot.visible, targetHeightDp, snapshot.expanded) {
            if (snapshot.visible) {
                val panelDp = VolumeHudSpec.panelHeightDp(
                    expanded = snapshot.expanded,
                    inCall = snapshot.inCall,
                )
                val entering = heightAnim.value < 0.5f
                // Grow the overlay before expand / enter wipes. Keep the tall window
                // during collapse so the wipe is not clipped by WindowManager.
                if (snapshot.expanded || entering) {
                    onWindowHeightDp(panelDp)
                }
                heightAnim.animateTo(
                    targetValue = panelDp.toFloat(),
                    animationSpec = tween(
                        durationMillis = if (entering) {
                            VolumeHudSpec.SHOW_HIDE_MS
                        } else {
                            VolumeHudSpec.EXPAND_COLLAPSE_MS
                        },
                        easing = EaseOutCubic,
                    ),
                )
                if (!snapshot.expanded) {
                    onWindowHeightDp(VolumeHudSpec.COLLAPSED_HEIGHT_DP)
                }
            } else if (heightAnim.value > 0.5f) {
                heightAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = VolumeHudSpec.SHOW_HIDE_MS,
                        easing = EaseOutCubic,
                    ),
                )
                onExitFinished()
            } else {
                // Already at zero height while hidden — drop any leftover window.
                onExitFinished()
            }
        }

        if (heightAnim.value <= 0.5f && !snapshot.visible) return@MetroTheme

        val bodyRevealDp = (heightAnim.value - VolumeHudSpec.COLLAPSED_HEIGHT_DP)
            .coerceAtLeast(0f)

        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightAnim.value.dp)
                    .align(Alignment.TopStart)
                    .clipToBounds()
                    .background(VolumeHudSpec.PanelBackground)
                    .padding(horizontal = VolumeHudSpec.HORIZONTAL_PADDING_DP.dp),
            ) {
                // Always the real collapsed top bar — never a clipped slice of the body.
                VolumeHeader(
                    snapshot = snapshot,
                    onToggleExpanded = onToggleExpanded,
                    onCollapse = onCollapse,
                )

                if (bodyRevealDp > 0.5f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(bodyRevealDp.dp)
                            .clipToBounds(),
                    ) {
                        VolumeBody(
                            snapshot = snapshot,
                            modifier = Modifier
                                .fillMaxWidth()
                                .requiredHeight(bodyRestingDp.dp),
                            onRingerLevel = onRingerLevel,
                            onMediaLevel = onMediaLevel,
                            onCallLevel = onCallLevel,
                            onToggleRingerMute = onToggleRingerMute,
                            onToggleMediaMute = onToggleMediaMute,
                            onToggleVibrate = onToggleVibrate,
                        )
                    }
                }
            }
        }
    }
}

/** Collapsed top-bar chrome: level label + chevron. Stable across expand / collapse. */
@Composable
private fun VolumeHeader(
    snapshot: VolumeHudSnapshot,
    onToggleExpanded: () -> Unit,
    onCollapse: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(VolumeHudSpec.COLLAPSED_HEIGHT_DP.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (snapshot.expanded) onCollapse() else onToggleExpanded()
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VolumeLevelLabel(
            level = snapshot.collapsedLevel,
            max = snapshot.collapsedMax,
            streamLabel = snapshot.collapsedLabel,
            modifier = Modifier.weight(1f),
        )
        ChevronIcon(
            pointingDown = !snapshot.expanded,
            color = VolumeHudSpec.PrimaryText,
        )
    }
}

/**
 * Content revealed under the header when expanded. Primary stream matches the
 * header label so the slider appears directly below the text the user already sees.
 */
@Composable
private fun VolumeBody(
    snapshot: VolumeHudSnapshot,
    onRingerLevel: (Int) -> Unit,
    onMediaLevel: (Int) -> Unit,
    onCallLevel: (Int) -> Unit,
    onToggleRingerMute: () -> Unit,
    onToggleMediaMute: () -> Unit,
    onToggleVibrate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val headerKind = snapshot.activeStream
    val headerLevel = snapshot.collapsedLevel
    val otherKind = when {
        snapshot.inCall -> null
        headerKind == VolumeStreamKind.Ringer -> VolumeStreamKind.Media
        else -> VolumeStreamKind.Ringer
    }

    Column(modifier = modifier) {
        StreamSliderRow(
            kind = headerKind,
            level = headerLevel,
            muted = headerLevel <= 0,
            onLevel = levelHandler(headerKind, onRingerLevel, onMediaLevel, onCallLevel),
            onToggleMute = muteHandler(
                kind = headerKind,
                level = headerLevel,
                onRingerMute = onToggleRingerMute,
                onMediaMute = onToggleMediaMute,
                onCallLevel = onCallLevel,
            ),
        )

        if (otherKind != null) {
            Spacer(modifier = Modifier.height(8.dp))
            VolumeStreamRow(
                kind = otherKind,
                level = levelOf(otherKind, snapshot),
                muted = levelOf(otherKind, snapshot) <= 0,
                onLevel = levelHandler(otherKind, onRingerLevel, onMediaLevel, onCallLevel),
                onToggleMute = muteHandler(
                    kind = otherKind,
                    level = levelOf(otherKind, snapshot),
                    onRingerMute = onToggleRingerMute,
                    onMediaMute = onToggleMediaMute,
                    onCallLevel = onCallLevel,
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(VolumeHudSpec.BOTTOM_ROW_HEIGHT_DP.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VibrateToggle(
                    vibrateOn = snapshot.vibrateOn,
                    accent = snapshot.accentColor,
                    onToggle = onToggleVibrate,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun levelOf(kind: VolumeStreamKind, snapshot: VolumeHudSnapshot): Int = when (kind) {
    VolumeStreamKind.Ringer -> snapshot.ringerLevel
    VolumeStreamKind.Media -> snapshot.mediaLevel
    VolumeStreamKind.Call -> snapshot.callLevel
}

private fun levelHandler(
    kind: VolumeStreamKind,
    onRingerLevel: (Int) -> Unit,
    onMediaLevel: (Int) -> Unit,
    onCallLevel: (Int) -> Unit,
): (Int) -> Unit = when (kind) {
    VolumeStreamKind.Ringer -> onRingerLevel
    VolumeStreamKind.Media -> onMediaLevel
    VolumeStreamKind.Call -> onCallLevel
}

private fun muteHandler(
    kind: VolumeStreamKind,
    level: Int,
    onRingerMute: () -> Unit,
    onMediaMute: () -> Unit,
    onCallLevel: (Int) -> Unit,
): () -> Unit = when (kind) {
    VolumeStreamKind.Ringer -> onRingerMute
    VolumeStreamKind.Media -> onMediaMute
    VolumeStreamKind.Call -> ({ onCallLevel(if (level <= 0) 5 else 0) })
}

/** `NN` large white + `/max` muted + stream label muted gray — matches WP8.1 volume chrome. */
@Composable
private fun VolumeLevelLabel(
    level: Int,
    max: Int,
    streamLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
    ) {
        MetroText(
            text = VolumeHudLogic.formatLevelDigits(level),
            style = MetroTextStyle.ListItemTitle,
            color = VolumeHudSpec.PrimaryText,
        )
        MetroText(
            text = VolumeHudLogic.formatMaxSuffix(max),
            style = MetroTextStyle.DialogBody,
            color = VolumeHudSpec.SecondaryText,
            modifier = Modifier.padding(start = 1.dp, end = 10.dp, bottom = 2.dp),
        )
        MetroText(
            text = streamLabel,
            style = MetroTextStyle.DialogBody,
            color = VolumeHudSpec.SecondaryText,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 2.dp),
        )
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
        VolumeLevelLabel(
            level = level,
            max = kind.wpMax,
            streamLabel = kind.label,
            modifier = Modifier.fillMaxWidth(),
        )
        StreamSliderRow(
            kind = kind,
            level = level,
            muted = muted,
            onLevel = onLevel,
            onToggleMute = onToggleMute,
        )
    }
}

@Composable
private fun StreamSliderRow(
    kind: VolumeStreamKind,
    level: Int,
    muted: Boolean,
    onLevel: (Int) -> Unit,
    onToggleMute: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        MetroBarStepSlider(
            index = level,
            onIndexChange = onLevel,
            stepCount = kind.wpMax + 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
    }
}

@Composable
private fun VibrateToggle(
    vibrateOn: Boolean,
    accent: Color,
    onToggle: () -> Unit,
) {
    val color = if (vibrateOn) accent else VolumeHudSpec.SecondaryText
    Row(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onToggle,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VibrateIcon(
            color = color,
            modifier = Modifier.size(22.dp),
        )
        MetroText(
            text = if (vibrateOn) "VIBRATE ON" else "vibrate off",
            style = MetroTextStyle.DialogBody,
            color = color,
            modifier = Modifier.padding(start = 8.dp),
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
private fun VibrateIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.12f
        val midY = size.height / 2f
        val amp = size.height * 0.28f
        val xs = listOf(size.width * 0.22f, size.width * 0.5f, size.width * 0.78f)
        xs.forEach { x ->
            val path = Path().apply {
                moveTo(x, midY - amp)
                cubicTo(
                    x + stroke * 1.6f, midY - amp * 0.45f,
                    x - stroke * 1.6f, midY + amp * 0.45f,
                    x, midY + amp,
                )
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = stroke,
                    cap = StrokeCap.Round,
                ),
            )
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
