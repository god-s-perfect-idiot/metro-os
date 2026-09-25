package com.metro.notifications.ui

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
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
 * WP8.1 toast: accent-filled bar, square app logo + message copy with ellipsis.
 * Default copy is single-line `sender: message`; setup may enable two-row
 * `title` / `description`. Group chats always put the conversation name on top.
 * Clock is owned by the status tray — not drawn here.
 *
 * The window sits at y=0. The full accent band ([topInsetDp] under the Metro tray + banner)
 * flips as one tile behind the opaque accent tray so the pivot is not offset to the safe
 * inset. Icon/text live only in the banner below the tray.
 */
@Composable
fun ToastBanner(
    toast: ToastSnapshot,
    accent: Color,
    exiting: Boolean,
    onTap: () -> Unit,
    onSwipeDismiss: () -> Unit,
    onExitFinished: () -> Unit,
    /** Status-bar / cutout height — accent extension under the Metro tray (flips with banner). */
    topInsetDp: Int = 0,
    /** Setup toggle: stack title over description instead of `title: description`. */
    twoRowView: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    // Accent is a remember key so Metro suite icon fills re-resolve when theme changes.
    val iconAsset = remember(toast.packageName, accent) {
        MetroAppBranding.loadAppIconAsset(context, toast.packageName)
    }
    var dragPx by remember(toast.key) { mutableFloatStateOf(0f) }
    val dismissPx = with(density) { ToastSpec.SWIPE_DISMISS_DP.dp.toPx() }
    val groupRow = remember(toast.key, toast.groupTitle) { toast.displayGroupRow() }
    val line = remember(toast.key, toast.title, toast.body) { toast.displayLine() }
    val titleRow = remember(toast.key, toast.title, toast.body) { toast.displayTitleRow() }
    val bodyRow = remember(toast.key, toast.title, toast.body) { toast.displayBodyRow() }
    val showTwoRows = twoRowView && bodyRow != null
    val contentLines = if (showTwoRows) 2 else 1
    val lineCount = (if (groupRow != null) 1 else 0) + contentLines
    val bannerHeightDp = ToastSpec.heightForLines(lineCount)
    val stackCopy = groupRow != null || showTwoRows
    val onTapState = rememberUpdatedState(onTap)
    val onSwipeDismissState = rememberUpdatedState(onSwipeDismiss)

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent),
            ) {
                if (topInsetDp > 0) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(topInsetDp.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bannerHeightDp.dp)
                        .offset { IntOffset(dragPx.roundToInt().coerceAtLeast(0), 0) }
                        .pointerInput(toast.key, exiting, dismissPx) {
                            if (exiting) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var overSlop = Offset.Zero
                                val slopChange = awaitTouchSlopOrCancellation(down.id) { change, over ->
                                    overSlop = over
                                    change.consume()
                                }
                                if (slopChange == null) {
                                    onTapState.value()
                                    return@awaitEachGesture
                                }
                                if (overSlop.x <= 0f) {
                                    horizontalDrag(slopChange.id) { it.consume() }
                                    return@awaitEachGesture
                                }
                                var totalDx = overSlop.x
                                dragPx = totalDx
                                horizontalDrag(slopChange.id) { change ->
                                    totalDx += change.positionChange().x
                                    dragPx = totalDx.coerceAtLeast(0f)
                                    change.consume()
                                }
                                if (dragPx >= dismissPx) {
                                    onSwipeDismissState.value()
                                } else {
                                    dragPx = 0f
                                }
                            }
                        }
                        .padding(horizontal = ToastSpec.HORIZONTAL_PADDING_DP.dp)
                        .testTag("metro_toast_banner"),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToastAppGlyph(
                            drawable = iconAsset.drawable,
                            background = iconAsset.backgroundColor,
                            modifier = Modifier.size(ToastSpec.ICON_DP.dp),
                        )
                        Spacer(modifier = Modifier.width(ToastSpec.ICON_TEXT_GAP_DP.dp))
                        if (stackCopy) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (groupRow != null) {
                                    MetroText(
                                        text = groupRow,
                                        style = MetroTextStyle.DialogBody,
                                        color = MetroColors.TileContentOnAccent,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (showTwoRows) {
                                    MetroText(
                                        text = titleRow,
                                        style = MetroTextStyle.DialogBody,
                                        color = MetroColors.TileContentOnAccent,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    MetroText(
                                        text = bodyRow.orEmpty(),
                                        style = MetroTextStyle.DialogBody,
                                        color = MetroColors.TileContentOnAccent,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                } else {
                                    MetroText(
                                        text = line,
                                        style = MetroTextStyle.DialogBody,
                                        color = MetroColors.TileContentOnAccent,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        } else {
                            MetroText(
                                text = line,
                                style = MetroTextStyle.DialogBody,
                                color = MetroColors.TileContentOnAccent,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
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
