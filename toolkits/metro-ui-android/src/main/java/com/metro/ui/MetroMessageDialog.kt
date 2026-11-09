package com.metro.ui

import android.content.Context
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.metro.system.MetroPreferences.Companion.toHexString
import com.metro.system.MetroStatusBar
import kotlin.math.roundToInt

/** Match app-bar text-button outline height: bar row minus vertical insets. */
private val MessageDialogButtonHeight =
    MetroAppBarDefaults.BarHeight - MetroAppBarDefaults.TextButtonVerticalInset * 2

/** Start edge-on (WP PlaneProjection RotationX = 90) before flipping flat. */
private const val MessageDialogFlipStartDegrees = 90f

private const val SkiaPointsPerInch = 72f

/** Camera Z ≈ 0.9× panel width so full-width flip reads as 3D, not a 2D squash. */
private const val MessageDialogFlipCameraWidthFactor = 0.9f

private const val MessageDialogFlipCameraDefaultInches = 8f

/** Same slot count as a Store-style app-bar text-button row with `textButtonSlots = 2`. */
private const val MessageDialogButtonSlots = 2

/**
 * WP8.1 message dialog — top-anchored full-width panel, 0dp corners (METRO-UX-LANGUAGE §6.15).
 *
 * Same band model as the notifications toast: chrome fills from y=0 under the Metro tray,
 * copy/actions sit in the safe inset below a spacer, and the tray is shell-filled opaque
 * to the same chrome so the strip reads as one continuous band.
 *
 * Panel fill matches [MetroAppBarDefaults.ChromeBackground] (bottom-bar chrome). Action
 * buttons use a **fixed 2-slot** outlined row like app-bar text buttons (same outline height):
 * at most two verbs; a single button occupies the left half and leaves the right slot empty.
 *
 * Enters with a perspective `rotationX` flip (90° → 0°); every dismiss path flips out before
 * invoking the matching callback. Affirmative action is leftmost; cancel/dismiss is rightmost.
 */
@Composable
fun MetroMessageDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    titleStyle: MetroTextStyle = MetroTextStyle.DialogTitle,
    bodyStyle: MetroTextStyle = MetroTextStyle.DialogBody,
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

    // Chrome panel is always the dark app-bar surface — force light glyphs for contrast.
    val chrome = MetroAppBarDefaults.ChromeBackground
    val onChrome = MetroColors.DarkPrimaryText
    val context = LocalContext.current
    val topInsetDp = rememberStatusTrayInsetDp()

    // Same as notifications toast: opaque tray shell-fill matching the panel chrome.
    DisposableEffect(chrome) {
        val appContext = context.applicationContext
        MetroStatusBar.requestShellFill(
            context = appContext,
            owner = MetroStatusBar.OWNER_DIALOG,
            colorHex = chrome.toHexString(),
            durationMs = MetroTransitions.JumpListFlipMs,
            underlay = false,
        )
        onDispose {
            MetroStatusBar.requestShellFill(
                context = appContext,
                owner = MetroStatusBar.OWNER_DIALOG,
                colorHex = null,
                durationMs = MetroTransitions.JumpListFlipMs,
                underlay = false,
            )
        }
    }

    Dialog(
        onDismissRequest = { requestExit(onDismissRequest) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            // Edge-to-edge so chrome can sit under the Metro tray (toast band model).
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            WindowCompat.getInsetsController(window, dialogView).isAppearanceLightStatusBars = false
        }

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
            ) {
                // ToastBanner layout: chrome Column from y=0, spacer for tray, content below.
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .background(chrome, RectangleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                ) {
                    if (topInsetDp > 0) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(topInsetDp.dp),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    ) {
                        MetroText(
                            text = title,
                            style = titleStyle,
                            color = onChrome,
                        )
                        if (!body.isNullOrBlank()) {
                            MetroText(
                                text = body,
                                style = bodyStyle,
                                color = onChrome,
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
                            if (!neutralLabel.isNullOrBlank() && onNeutral != null) {
                                add(
                                    DialogAction(
                                        label = neutralLabel.lowercase(),
                                        onClick = { requestExit(onNeutral) },
                                        enabled = neutralEnabled,
                                    ),
                                )
                            }
                        }.take(MessageDialogButtonSlots)

                        if (buttons.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp)
                                    .height(MessageDialogButtonHeight),
                                horizontalArrangement = Arrangement.spacedBy(
                                    MetroAppBarDefaults.TextButtonSpacing,
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                buttons.forEach { action ->
                                    MessageDialogBorderButton(
                                        text = action.label,
                                        onClick = action.onClick,
                                        enabled = action.enabled,
                                        foreground = onChrome,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    )
                                }
                                // Keep a fixed 2-slot row so a lone button is half-width, left-anchored.
                                repeat(MessageDialogButtonSlots - buttons.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top of the Metro tray / system status-bar region, including notch or hole-punch.
 * Same calculation as notifications toast so the chrome spacer matches the tray height.
 */
@Composable
private fun rememberStatusTrayInsetDp(): Int {
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(density.density) {
        statusTrayInsetDp(context, density.density)
    }
}

private fun statusTrayInsetDp(context: Context, density: Float): Int {
    val topPx: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val insets = wm.currentWindowMetrics.windowInsets
            val statusBars = insets.getInsets(WindowInsets.Type.statusBars())
            val cutoutTop = insets.displayCutout?.safeInsetTop ?: 0
            maxOf(statusBars.top, cutoutTop)
        } else {
            val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resId > 0) {
                context.resources.getDimensionPixelSize(resId)
            } else {
                (24 * density).toInt()
            }
        }
    val minTrayPx = (MetroStatusBar.HEIGHT_DP * density).roundToInt()
    return (maxOf(topPx, minTrayPx) / density + 0.5f).toInt()
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
 * Outlined dialog action matching Store / app-bar text-button chrome, sized by the
 * fixed 2-slot row (half width when alone).
 */
@Composable
private fun MessageDialogBorderButton(
    text: String,
    onClick: () -> Unit,
    foreground: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderColor = if (enabled) foreground else foreground.copy(alpha = 0.4f)
    val textColor = if (enabled) foreground else foreground.copy(alpha = 0.4f)
    val background = when {
        !enabled -> Color.Transparent
        pressed -> foreground.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .background(background, RectangleShape)
            .border(width = 2.dp, color = borderColor, shape = RectangleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroMessageDialogSingleButtonPreview() {
    MetroTheme(darkTheme = true) {
        MetroMessageDialog(
            title = "timer",
            body = "time's up",
            confirmLabel = "close",
            onConfirm = {},
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
