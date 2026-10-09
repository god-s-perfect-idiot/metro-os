package com.metro.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Start edge-on (WP PlaneProjection RotationX = 90) before flipping flat. */
private const val MessageDialogFlipStartDegrees = 90f

private const val SkiaPointsPerInch = 72f

/** Camera Z ≈ 0.9× panel width so full-width flip reads as 3D, not a 2D squash. */
private const val MessageDialogFlipCameraWidthFactor = 0.9f

private const val MessageDialogFlipCameraDefaultInches = 8f

/**
 * WP8.1 message dialog — top-anchored full-width panel below the status bar, 0dp corners
 * (METRO-UX-LANGUAGE §6.15). Matches WP MessageBox / CustomMessageBox confirmations
 * (title + body + equal-width outlined yes/no).
 *
 * Enters with a perspective `rotationX` flip (90° → 0°); every dismiss path (scrim, back,
 * action buttons) flips out before invoking the matching callback. Affirmative action is
 * leftmost; cancel/dismiss is rightmost. Action buttons share the panel width equally.
 */
@Composable
fun MetroMessageDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
    neutralLabel: String? = null,
    onNeutral: (() -> Unit)? = null,
    neutralEnabled: Boolean = true,
    content: (@Composable () -> Unit)? = null,
) {
    var exiting by remember { mutableStateOf(false) }
    val exitActionRef = remember { arrayOf(onDismissRequest) }
    val requestExit: (exit: () -> Unit) -> Unit = { exit ->
        if (!exiting) {
            exitActionRef[0] = exit
            exiting = true
        }
    }

    Dialog(
        onDismissRequest = { requestExit(onDismissRequest) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { requestExit(onDismissRequest) },
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            MessageDialogFlip(
                exiting = exiting,
                onExitComplete = { exitActionRef[0]() },
                modifier = Modifier.statusBarsPadding(),
            ) {
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .background(MetroTheme.colors.background, RectangleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(24.dp),
                ) {
                    MetroText(
                        text = title,
                        style = MetroTextStyle.DialogTitle,
                        color = MetroTheme.colors.primaryText,
                    )
                    if (!body.isNullOrBlank()) {
                        MetroText(
                            text = body,
                            style = MetroTextStyle.DialogBody,
                            color = MetroTheme.colors.primaryText,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    if (content != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            content()
                        }
                    }
                    data class DialogAction(
                        val label: String,
                        val onClick: () -> Unit,
                        val enabled: Boolean = true,
                    )
                    val buttons = buildList {
                        if (!confirmLabel.isNullOrBlank() && onConfirm != null) {
                            add(
                                DialogAction(
                                    label = confirmLabel.lowercase(),
                                    onClick = { requestExit(onConfirm) },
                                    enabled = confirmEnabled,
                                ),
                            )
                        }
                        if (!neutralLabel.isNullOrBlank() && onNeutral != null) {
                            add(
                                DialogAction(
                                    label = neutralLabel.lowercase(),
                                    onClick = { requestExit(onNeutral) },
                                    enabled = neutralEnabled,
                                ),
                            )
                        }
                        if (!dismissLabel.isNullOrBlank()) {
                            add(
                                DialogAction(
                                    label = dismissLabel.lowercase(),
                                    onClick = {
                                        requestExit(onDismiss ?: onDismissRequest)
                                    },
                                ),
                            )
                        }
                    }
                    if (buttons.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val shareWidth = buttons.size > 1
                            buttons.forEach { action ->
                                MessageDialogBorderButton(
                                    text = action.label,
                                    onClick = action.onClick,
                                    enabled = action.enabled,
                                    modifier = if (shareWidth) Modifier.weight(1f) else Modifier,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageDialogFlip(
    exiting: Boolean,
    onExitComplete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val rotationX = remember { Animatable(MessageDialogFlipStartDegrees) }
    LaunchedEffect(exiting) {
        if (exiting) {
            rotationX.animateTo(
                targetValue = MessageDialogFlipStartDegrees,
                animationSpec = MetroTransitions.jumpListFlipTween(),
            )
            onExitComplete()
        } else {
            rotationX.snapTo(MessageDialogFlipStartDegrees)
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
            cameraDistance = messageDialogFlipCameraInches(size.width)
        },
    ) {
        content()
    }
}

/**
 * Full-width outlined dialog action — equal shares in the message-dialog button row.
 * Distinct from [MetroBorderButton], which hugs its label for in-page use.
 */
@Composable
private fun MessageDialogBorderButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val foreground = MetroTheme.colors.primaryText
    val borderColor = if (enabled) foreground else foreground.copy(alpha = 0.4f)
    val textColor = if (enabled) foreground else foreground.copy(alpha = 0.4f)
    val background = when {
        !enabled -> Color.Transparent
        pressed -> foreground.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .background(background, RectangleShape)
            .border(width = 2.dp, color = borderColor, shape = RectangleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                fontFamily = MetroTheme.fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = textColor,
                textAlign = TextAlign.Center,
            ),
            maxLines = 2,
        )
    }
}

private fun messageDialogFlipCameraInches(widthPx: Float): Float {
    if (widthPx <= 0f) return MessageDialogFlipCameraDefaultInches
    return (widthPx / SkiaPointsPerInch) * MessageDialogFlipCameraWidthFactor
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroMessageDialogPreview() {
    MetroTheme(darkTheme = true) {
        MetroMessageDialog(
            title = "Uninstall selected apps?",
            body = "The selected apps, as well as data and supporting files for the apps, " +
                "will be deleted. Are you sure you want to uninstall them?",
            confirmLabel = "yes",
            onConfirm = {},
            dismissLabel = "no",
            onDismissRequest = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MetroMessageDialogLightPreview() {
    MetroTheme(darkTheme = false) {
        MetroMessageDialog(
            title = "delete item?",
            body = "This can't be undone.",
            confirmLabel = "ok",
            onConfirm = {},
            dismissLabel = "cancel",
            onDismissRequest = {},
        )
    }
}
