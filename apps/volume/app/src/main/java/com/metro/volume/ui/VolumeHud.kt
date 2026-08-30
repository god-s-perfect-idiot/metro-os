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
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.ui.MetroBarStepSlider
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.volume.VolumeHudLogic
import com.metro.volume.VolumeHudSnapshot
import com.metro.volume.VolumeHudSpec
import com.metro.volume.VolumeStreamKind
import kotlin.math.atan2
import kotlin.math.sin

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
    onToggleSilentMode: () -> Unit,
    onOpenSoundSettings: () -> Unit,
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
                            onCollapse = onCollapse,
                            onRingerLevel = onRingerLevel,
                            onMediaLevel = onMediaLevel,
                            onCallLevel = onCallLevel,
                            onToggleRingerMute = onToggleRingerMute,
                            onToggleMediaMute = onToggleMediaMute,
                            onToggleSilentMode = onToggleSilentMode,
                            onOpenSoundSettings = onOpenSoundSettings,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Collapsed top-bar chrome: level label + down chevron.
 * When expanded (non-call), the label is always Ringer (fixed dual-row order) and
 * the up chevron lives below the actions row instead.
 */
@Composable
private fun VolumeHeader(
    snapshot: VolumeHudSnapshot,
    onToggleExpanded: () -> Unit,
    onCollapse: () -> Unit,
) {
    val chevronInHeader = !snapshot.expanded || snapshot.inCall
    // Expanded dual-slider keeps Ringer on top — header must match that row, not
    // activeStream, or adjusting Media would make the label disagree with the slider.
    val headerKind = when {
        snapshot.expanded && !snapshot.inCall -> VolumeStreamKind.Ringer
        else -> snapshot.activeStream
    }
    val headerLevel = levelOf(headerKind, snapshot)
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
            level = headerLevel,
            max = headerKind.wpMax,
            streamLabel = headerKind.label,
            modifier = Modifier.weight(1f),
        )
        if (chevronInHeader) {
            ChevronIcon(
                pointingDown = !snapshot.expanded,
                color = VolumeHudSpec.PrimaryText,
                sizeDp = VolumeHudSpec.CHEVRON_HEADER_SIZE_DP,
            )
        }
    }
}

/**
 * Content revealed under the header when expanded.
 * Non-call order is fixed: Ringer slider under the header, then Media — never swap
 * when [VolumeHudSnapshot.activeStream] changes (that made the lower slider jump).
 * Actions row: silent mode + sound settings; up chevron sits below that row.
 */
@Composable
private fun VolumeBody(
    snapshot: VolumeHudSnapshot,
    onCollapse: () -> Unit,
    onRingerLevel: (Int) -> Unit,
    onMediaLevel: (Int) -> Unit,
    onCallLevel: (Int) -> Unit,
    onToggleRingerMute: () -> Unit,
    onToggleMediaMute: () -> Unit,
    onToggleSilentMode: () -> Unit,
    onOpenSoundSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (snapshot.inCall) {
            val callLevel = snapshot.callLevel
            StreamSliderRow(
                kind = VolumeStreamKind.Call,
                level = callLevel,
                muted = callLevel <= 0,
                onLevel = onCallLevel,
                onToggleMute = {
                    onCallLevel(if (callLevel <= 0) 5 else 0)
                },
            )
            return@Column
        }

        val ringerLevel = snapshot.ringerLevel
        StreamSliderRow(
            kind = VolumeStreamKind.Ringer,
            level = ringerLevel,
            muted = ringerLevel <= 0,
            onLevel = onRingerLevel,
            onToggleMute = onToggleRingerMute,
        )
        Spacer(modifier = Modifier.height(8.dp))
        VolumeStreamRow(
            kind = VolumeStreamKind.Media,
            level = snapshot.mediaLevel,
            muted = snapshot.mediaLevel <= 0,
            onLevel = onMediaLevel,
            onToggleMute = onToggleMediaMute,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(VolumeHudSpec.BOTTOM_ROW_HEIGHT_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SilentModeToggle(
                silentOn = snapshot.silentModeOn,
                accent = snapshot.accentColor,
                onToggle = onToggleSilentMode,
            )
            Spacer(modifier = Modifier.weight(1f))
            SoundSettingsAction(onClick = onOpenSoundSettings)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(VolumeHudSpec.CHEVRON_BELOW_ROW_HEIGHT_DP.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            ChevronIcon(
                pointingDown = false,
                color = VolumeHudSpec.PrimaryText,
                sizeDp = VolumeHudSpec.CHEVRON_EXPANDED_SIZE_DP,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCollapse,
                ),
            )
        }
    }
}

private fun levelOf(kind: VolumeStreamKind, snapshot: VolumeHudSnapshot): Int = when (kind) {
    VolumeStreamKind.Ringer -> snapshot.ringerLevel
    VolumeStreamKind.Media -> snapshot.mediaLevel
    VolumeStreamKind.Call -> snapshot.callLevel
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
private fun SilentModeToggle(
    silentOn: Boolean,
    accent: Color,
    onToggle: () -> Unit,
) {
    val color = if (silentOn) accent else VolumeHudSpec.PrimaryText
    Row(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onToggle,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SilentModeIcon(
            color = color,
            modifier = Modifier.size(VolumeHudSpec.ACTION_ICON_SIZE_DP.dp),
        )
        ActionLabelText(
            text = if (silentOn) "SILENT MODE ON" else "SILENT MODE OFF",
            color = color,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun SoundSettingsAction(onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetroSystemIcon(
            type = MetroSystemIconType.Settings,
            iconSize = VolumeHudSpec.ACTION_ICON_SIZE_DP.dp,
            color = VolumeHudSpec.PrimaryText,
            showCircle = false,
        )
        ActionLabelText(
            text = "SOUND SETTINGS",
            color = VolumeHudSpec.PrimaryText,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** Compact Medium-weight action captions under the sliders. */
@Composable
private fun ActionLabelText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontFamily = MetroFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = VolumeHudSpec.ACTION_LABEL_FONT_SP.sp,
            lineHeight = 18.sp,
            color = color,
        ),
    )
}

/**
 * WP8.1 volume HUD caret measured from the system reference:
 * aspect ≈ 1.49 (wider than tall), tip opening ≈ 76°, fine stroke, flat
 * horizontal end caps (not stroke-cap ends).
 */
@Composable
private fun ChevronIcon(
    pointingDown: Boolean,
    color: Color,
    sizeDp: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val s = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f
        // Centerline box ≈ 1.49 aspect; arms ~38° from vertical.
        val halfW = s * 0.34f
        val halfH = s * 0.228f
        // Medium-bold stroke — a touch thicker than the hairline WP chrome weight.
        val t = s * 0.12f
        val armDx = halfW
        val armDy = halfH * 2f
        val theta = atan2(armDy, armDx)
        val horizInset = t / sin(theta).toFloat()
        val halfTip = atan2(armDx, armDy)
        val tipLift = t / sin(halfTip).toFloat()
        val top = cy - halfH
        val bot = cy + halfH
        val left = cx - halfW
        val right = cx + halfW
        // Filled down-chevron with axis-aligned end caps, then flip for up.
        val down = Path().apply {
            moveTo(left, top)
            lineTo(cx, bot)
            lineTo(right, top)
            lineTo(right - horizInset, top)
            lineTo(cx, bot - tipLift)
            lineTo(left + horizInset, top)
            close()
        }
        if (pointingDown) {
            drawPath(down, color)
        } else {
            rotate(degrees = 180f, pivot = Offset(cx, cy)) {
                drawPath(down, color)
            }
        }
    }
}

/** Speaker cone with a diagonal slash — WP silent-mode glyph. */
@Composable
private fun SilentModeIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val body = Path().apply {
            moveTo(w * 0.12f, h * 0.38f)
            lineTo(w * 0.32f, h * 0.38f)
            lineTo(w * 0.52f, h * 0.18f)
            lineTo(w * 0.52f, h * 0.82f)
            lineTo(w * 0.32f, h * 0.62f)
            lineTo(w * 0.12f, h * 0.62f)
            close()
        }
        drawPath(body, color)
        drawLine(
            color,
            Offset(w * 0.62f, h * 0.32f),
            Offset(w * 0.72f, h * 0.22f),
            strokeWidth = w * 0.07f,
            cap = StrokeCap.Square,
        )
        drawLine(
            color,
            Offset(w * 0.62f, h * 0.68f),
            Offset(w * 0.72f, h * 0.78f),
            strokeWidth = w * 0.07f,
            cap = StrokeCap.Square,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.08f, h * 0.82f),
            end = Offset(w * 0.88f, h * 0.18f),
            strokeWidth = w * 0.1f,
            cap = StrokeCap.Square,
        )
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
