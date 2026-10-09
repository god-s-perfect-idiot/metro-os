package com.metro.ui

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.metro.system.MetroAppBranding
import kotlinx.coroutines.delay

/** Accent strip height: square icon + single-line message (matches notifications toast). */
private const val ToastHeightDp = 52
private const val ToastIconDp = 32
private const val ToastHorizontalPaddingDp = 12
private const val ToastIconTextGapDp = 10
private const val ToastFlipProjectionPadDp = 24
private const val ToastFlipStartDegrees = 90f
private const val ToastFlipCameraWidthFactor = 0.9f
private const val SkiaPointsPerInch = 72f

/** Default in-app toast peek (~3s short). */
const val MetroToastShortMs = 3_000L

/** Longer in-app toast peek (~5s). */
const val MetroToastLongMs = 5_000L

/**
 * In-app WP8.1 toast controller — accent bar + square app logo + one line.
 * Host with [MetroToastHost] at the activity root.
 */
class MetroToastController {
    var message by mutableStateOf<String?>(null)
        private set
    var durationMs by mutableLongStateOf(MetroToastShortMs)
        private set
    var generation by mutableLongStateOf(0L)
        private set

    fun show(text: String, durationMs: Long = MetroToastShortMs) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        this.durationMs = durationMs.coerceAtLeast(500L)
        generation += 1L
        message = trimmed
    }

    fun dismiss() {
        message = null
    }

    internal fun clearIf(generation: Long) {
        if (this.generation == generation) {
            message = null
        }
    }
}

@Composable
fun rememberMetroToastController(): MetroToastController = remember { MetroToastController() }

/**
 * Renders the active [MetroToastController] toast as a top overlay (flip enter/exit).
 * Place last in the activity root [Box] so it sits above page content.
 */
@Composable
fun MetroToastHost(
    controller: MetroToastController,
    modifier: Modifier = Modifier,
    /** Package whose glyph appears on the accent bar (defaults to the host app). */
    iconPackageName: String? = null,
) {
    val message = controller.message ?: return
    val generation = controller.generation
    val durationMs = controller.durationMs
    val context = LocalContext.current
    val packageName = iconPackageName ?: context.packageName
    var exiting by remember(generation) { mutableStateOf(false) }

    LaunchedEffect(generation, durationMs) {
        delay(durationMs)
        exiting = true
    }

    key(generation) {
        MetroToastBanner(
            message = message,
            accent = MetroTheme.colors.accent,
            packageName = packageName,
            exiting = exiting,
            onExitFinished = { controller.clearIf(generation) },
            modifier = modifier.fillMaxWidth(),
        )
    }
}

/**
 * WP8.1 toast banner chrome — accent fill, square logo, single ellipsized line.
 * Prefer [MetroToastHost] for timed shows; use this when embedding a one-shot banner.
 */
@Composable
fun MetroToastBanner(
    message: String,
    accent: Color,
    packageName: String,
    exiting: Boolean,
    onExitFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val iconAsset = remember(packageName, accent) {
        MetroAppBranding.loadAppIconAsset(context, packageName)
    }

    LaunchedEffect(exiting) {
        if (!exiting) return@LaunchedEffect
        delay(MetroTransitions.JumpListFlipMs.toLong())
        onExitFinished()
    }

    ToastFlip(
        exiting = exiting,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = ToastFlipProjectionPadDp.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(accent)
                .statusBarsPadding()
                .height(ToastHeightDp.dp)
                .padding(horizontal = ToastHorizontalPaddingDp.dp)
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
                    modifier = Modifier.size(ToastIconDp.dp),
                )
                Spacer(modifier = Modifier.width(ToastIconTextGapDp.dp))
                MetroText(
                    text = message,
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
            cameraDistance = flipCameraInches(size.width)
        },
    ) {
        content()
    }
}

private fun flipCameraInches(widthPx: Float): Float {
    if (widthPx <= 0f) return 8f
    return (widthPx / SkiaPointsPerInch) * ToastFlipCameraWidthFactor
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
                    width = ToastIconDp * 3,
                    height = ToastIconDp * 3,
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

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroToastBannerPreview() {
    MetroTheme(darkTheme = true) {
        MetroToastBanner(
            message = "Notes was uninstalled.",
            accent = MetroColors.AccentBlue,
            packageName = "com.metro.notes",
            exiting = false,
            onExitFinished = {},
        )
    }
}
