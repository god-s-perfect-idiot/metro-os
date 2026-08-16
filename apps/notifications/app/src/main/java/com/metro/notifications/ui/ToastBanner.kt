package com.metro.notifications.ui

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.metro.notifications.ToastSnapshot
import com.metro.notifications.ToastSpec
import com.metro.system.MetroAppBranding
import com.metro.ui.MetroColors
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTransitions
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * WP8.1 toast: accent-filled bar, square app logo + wrapping `sender: message` line.
 * Clock is owned by the status tray — not drawn here.
 *
 * Enters with a perspective 3D tile flip (`rotationX` 90° → 0°) and leaves as the reverse.
 */
@Composable
fun ToastBanner(
    toast: ToastSnapshot,
    accent: Color,
    exiting: Boolean,
    onTap: () -> Unit,
    onSwipeDismiss: () -> Unit,
    onExitFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val iconAsset = remember(toast.packageName) {
        MetroAppBranding.loadAppIconAsset(context, toast.packageName)
    }
    var dragPx by remember(toast.key) { mutableFloatStateOf(0f) }
    val dismissPx = with(density) { ToastSpec.SWIPE_DISMISS_DP.dp.toPx() }
    val line = remember(toast.key, toast.title, toast.body) { toast.displayLine() }

    key(toast.key) {
        LaunchedEffect(exiting) {
            if (!exiting) return@LaunchedEffect
            delay(MetroTransitions.JumpListFlipMs.toLong())
            onExitFinished()
        }
        ToastFlip(
            exiting = exiting,
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = ToastSpec.FLIP_PROJECTION_PAD_DP.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = ToastSpec.HEIGHT_DP.dp)
                    .offset { IntOffset(dragPx.roundToInt().coerceAtLeast(0), 0) }
                    .background(accent)
                    .pointerInput(toast.key, exiting) {
                        if (exiting) return@pointerInput
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, amount ->
                                dragPx = (dragPx + amount).coerceAtLeast(0f)
                            },
                            onDragEnd = {
                                if (dragPx >= dismissPx) {
                                    onSwipeDismiss()
                                } else {
                                    dragPx = 0f
                                }
                            },
                            onDragCancel = { dragPx = 0f },
                        )
                    }
                    .clickable(
                        enabled = !exiting,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTap,
                    )
                    .padding(
                        horizontal = ToastSpec.HORIZONTAL_PADDING_DP.dp,
                        vertical = ToastSpec.VERTICAL_PADDING_DP.dp,
                    )
                    .testTag("metro_toast_banner"),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    ToastAppGlyph(
                        drawable = iconAsset.drawable,
                        background = iconAsset.backgroundColor,
                        modifier = Modifier.size(ToastSpec.ICON_DP.dp),
                    )
                    Spacer(modifier = Modifier.width(ToastSpec.ICON_TEXT_GAP_DP.dp))
                    MetroText(
                        text = line,
                        style = MetroTextStyle.DialogBody,
                        color = MetroColors.TileContentOnAccent,
                        softWrap = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Start edge-on (WP PlaneProjection RotationX = 90) before flipping flat. */
private const val ToastFlipStartDegrees = 90f

@Composable
private fun ToastFlip(
    exiting: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val rotationX = remember { Animatable(ToastFlipStartDegrees) }
    LaunchedEffect(exiting) {
        if (exiting) {
            rotationX.animateTo(
                targetValue = ToastFlipStartDegrees,
                animationSpec = MetroTransitions.jumpListFlipTween(),
            )
        } else {
            rotationX.snapTo(ToastFlipStartDegrees)
            rotationX.animateTo(
                targetValue = 0f,
                animationSpec = MetroTransitions.jumpListFlipTween(),
            )
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            this.rotationX = rotationX.value
            transformOrigin = TransformOrigin(0.5f, 0.5f)
            clip = false
            cameraDistance = ToastSpec.flipCameraInches(size.width)
        },
    ) {
        content()
    }
}

@Composable
private fun ToastAppGlyph(
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
                    width = ToastSpec.ICON_DP * 3,
                    height = ToastSpec.ICON_DP * 3,
                )
            }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
