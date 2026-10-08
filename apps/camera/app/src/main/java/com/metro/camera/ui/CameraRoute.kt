package com.metro.camera.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroStatusBarFullscreenEffect
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CameraRoute(
    state: CameraUiState,
    hasCameraPermission: Boolean,
    glyphRotationDegrees: Float,
    onRequestPermission: () -> Unit,
    onUpdate: (CameraUiState) -> Unit,
    onShutter: () -> Unit,
    onCycleFlash: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    previewContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    MetroStatusBarFullscreenEffect(active = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (!hasCameraPermission) {
            PermissionPane(onRequestPermission = onRequestPermission)
            return
        }

        previewContent()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutPadding(),
        ) {
            when (state.screen) {
                CameraUiState.Screen.Viewfinder,
                CameraUiState.Screen.More,
                -> {
                    ViewfinderChrome(
                        state = state,
                        glyphRotationDegrees = glyphRotationDegrees,
                        onUpdate = onUpdate,
                        onShutter = onShutter,
                        onCycleFlash = onCycleFlash,
                        onSwitchCamera = onSwitchCamera,
                        onOpenGallery = onOpenGallery,
                    )
                    if (state.screen == CameraUiState.Screen.More) {
                        MoreMenuOverlay(
                            state = state,
                            glyphRotationDegrees = glyphRotationDegrees,
                            onUpdate = onUpdate,
                        )
                    }
                }
                CameraUiState.Screen.PhotoSettings -> {
                    SettingsList(
                        title = "photo settings",
                        rows = photoSettingsRows(state),
                        onBack = { onUpdate(state.copy(screen = CameraUiState.Screen.More)) },
                    )
                }
                CameraUiState.Screen.VideoSettings -> {
                    SettingsList(
                        title = "video settings",
                        rows = videoSettingsRows(state),
                        onBack = { onUpdate(state.copy(screen = CameraUiState.Screen.More)) },
                    )
                }
                CameraUiState.Screen.BurstReview -> {
                    BurstReviewPane(state = state, onUpdate = onUpdate)
                }
            }
        }
    }
}

@Composable
private fun PermissionPane(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .displayCutoutPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        MetroText(
            text = "camera",
            style = MetroTextStyle.PageTitle,
            color = Color.White,
        )
        Spacer(Modifier.height(12.dp))
        MetroText(
            text = "Allow camera access to take photos and videos.",
            style = MetroTextStyle.Body,
            color = Color.White.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(24.dp))
        MetroText(
            text = "allow",
            style = MetroTextStyle.ListItemTitle,
            color = MetroTheme.colors.accent,
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onRequestPermission,
            ),
        )
    }
}

/** Animate glyph angle along the shortest path (avoids 270°→0° long-way spins). */
@Composable
private fun rememberAnimatedGlyphRotation(targetDegrees: Float): Float {
    val anim = remember { Animatable(targetDegrees) }
    LaunchedEffect(targetDegrees) {
        val current = anim.value
        var target = targetDegrees
        var delta = target - current
        while (delta > 180f) {
            target -= 360f
            delta = target - current
        }
        while (delta < -180f) {
            target += 360f
            delta = target - current
        }
        anim.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        )
    }
    return anim.value
}

/**
 * Fixed portrait chrome. Layout never changes with device tilt — only glyph artwork spins.
 */
@Composable
private fun BoxScope.ViewfinderChrome(
    state: CameraUiState,
    glyphRotationDegrees: Float,
    onUpdate: (CameraUiState) -> Unit,
    onShutter: () -> Unit,
    onCycleFlash: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenGallery: () -> Unit,
) {
    val flashOff = state.flashValue == "flash_off"
    val glyphRotation = rememberAnimatedGlyphRotation(glyphRotationDegrees)

    // Top row reversed: iso → lens → flip → flash → gallery
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleGlyphButton(
            glyph = CameraChromeGlyph.Iso,
            glyphRotationDegrees = glyphRotation,
            onClick = { onUpdate(cycleIso(state)) },
        )
        CircleGlyphButton(
            glyph = CameraChromeGlyph.Lens,
            glyphRotationDegrees = glyphRotation,
            onClick = { onUpdate(state.copy(statusMessage = "no lenses installed")) },
        )
        CircleGlyphButton(
            glyph = CameraChromeGlyph.FlipCamera,
            glyphRotationDegrees = glyphRotation,
            onClick = onSwitchCamera,
        )
        CircleGlyphButton(
            glyph = if (flashOff) CameraChromeGlyph.FlashOff else CameraChromeGlyph.Flash,
            glyphRotationDegrees = glyphRotation,
            onClick = onCycleFlash,
        )
        GalleryButton(
            lastCaptureUri = state.lastCaptureUri,
            glyphRotationDegrees = glyphRotation,
            onClick = onOpenGallery,
        )
    }

    // Bottom row reversed: video → burst → photo
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 24.dp, end = 56.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModeButton(
            glyph = CameraChromeGlyph.Video,
            selected = state.mode == CameraUiState.Mode.Video || state.isRecording,
            glyphRotationDegrees = glyphRotation,
            onClick = {
                if (state.mode == CameraUiState.Mode.Video) onShutter()
                else onUpdate(state.copy(mode = CameraUiState.Mode.Video, statusMessage = "video"))
            },
        )
        ModeButton(
            glyph = CameraChromeGlyph.Burst,
            selected = state.mode == CameraUiState.Mode.Burst,
            glyphRotationDegrees = glyphRotation,
            onClick = {
                if (state.mode == CameraUiState.Mode.Burst) onShutter()
                else onUpdate(state.copy(mode = CameraUiState.Mode.Burst, statusMessage = "burst"))
            },
        )
        ModeButton(
            glyph = CameraChromeGlyph.Photo,
            selected = state.mode == CameraUiState.Mode.Photo,
            glyphRotationDegrees = glyphRotation,
            onClick = {
                if (state.mode == CameraUiState.Mode.Photo) onShutter()
                else onUpdate(state.copy(mode = CameraUiState.Mode.Photo, statusMessage = "photo"))
            },
        )
    }

    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(10.dp)
            .size(44.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                onUpdate(
                    state.copy(
                        screen = if (state.screen == CameraUiState.Screen.More) {
                            CameraUiState.Screen.Viewfinder
                        } else {
                            CameraUiState.Screen.More
                        },
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // Horizontal ellipsis — never follows device-tilt glyph rotation.
        CameraChromeIcon(glyph = CameraChromeGlyph.More, size = 28.dp)
    }

    StatusToast(
        message = state.statusMessage,
        glyphRotationDegrees = glyphRotation,
        onDismiss = { onUpdate(state.copy(statusMessage = null)) },
    )
}

@Composable
private fun GalleryButton(
    lastCaptureUri: String?,
    glyphRotationDegrees: Float,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val thumb by produceState<ImageBitmap?>(null, lastCaptureUri) {
        value = null
        val uri = lastCaptureUri ?: return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(2.5.dp, Color.White, CircleShape)
            .background(Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.graphicsLayer { rotationZ = glyphRotationDegrees }) {
            if (thumb != null) {
                Image(
                    bitmap = thumb!!,
                    contentDescription = "gallery",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                )
            } else {
                CameraChromeIcon(glyph = CameraChromeGlyph.Gallery, size = 20.dp)
            }
        }
    }
}

@Composable
private fun BoxScope.StatusToast(
    message: String?,
    glyphRotationDegrees: Float,
    onDismiss: () -> Unit,
) {
    if (message.isNullOrBlank()) return

    // Always just after the ISO/lens row. Portrait: sit under the row.
    // Landscape 270°/CW: rotating around top-start swings glyphs *up* into the
    // icon strip — pre-offset by the measured text width so after rotation the
    // label lands in the preview, past the controls (not between the icons).
    val isoRowClearance = 12.dp + 44.dp + 12.dp
    val afterRowGap = 10.dp
    val edgeInset = 48.dp
    val density = LocalDensity.current
    val deviceDegrees =
        (((( -glyphRotationDegrees / 90f).roundToInt() * 90) % 360) + 360) % 360
    val textMeasurer = rememberTextMeasurer()
    val fontFamily = MetroTheme.fontFamily
    val textLayout = textMeasurer.measure(
        text = message.lowercase(),
        style = MetroTextStyle.PageTitle.toTextStyle(fontFamily),
    )
    val textWidthDp = with(density) { textLayout.size.width.toDp() }
    val topPad = when (deviceDegrees) {
        270, 180 -> isoRowClearance + afterRowGap + textWidthDp
        else -> isoRowClearance + afterRowGap
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .absolutePadding(left = edgeInset, top = topPad)
                .graphicsLayer {
                    rotationZ = glyphRotationDegrees
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) {
            MetroText(
                text = message.lowercase(),
                style = MetroTextStyle.PageTitle,
                color = Color.White,
            )
        }
    }
    LaunchedEffect(message) {
        delay(1400)
        onDismiss()
    }
}

/**
 * WP8.1 More panel: black pane pushes up from the bottom (~58% height), live preview
 * remains in the top sliver — matches `references/images/photo_settings_dark.jpg`.
 */
@Composable
private fun BoxScope.MoreMenuOverlay(
    state: CameraUiState,
    glyphRotationDegrees: Float,
    onUpdate: (CameraUiState) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val slide = remember { Animatable(1f) }
    var dismissing by remember { mutableStateOf(false) }
    val contentRotation = rememberAnimatedGlyphRotation(glyphRotationDegrees)
    val flashOff = state.flashValue == "flash_off"

    LaunchedEffect(Unit) {
        slide.snapTo(1f)
        slide.animateTo(0f, MetroTransitions.appBarCreepTween())
    }

    fun dismiss() {
        if (dismissing) return
        dismissing = true
        scope.launch {
            slide.animateTo(1f, MetroTransitions.appBarCreepTween())
            onUpdate(state.copy(screen = CameraUiState.Screen.Viewfinder))
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panelHeight = maxHeight * 0.58f
        val panelHeightPx = with(LocalDensity.current) { panelHeight.toPx() }

        // Preview sliver — tap dismisses.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.42f)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { dismiss() },
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(panelHeight)
                .offset { IntOffset(0, (slide.value * panelHeightPx).roundToInt()) }
                .background(Color.Black)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Horizontal ellipsis stays put (right-aligned, never tilt-rotated).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { dismiss() },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    CameraChromeIcon(glyph = CameraChromeGlyph.More, size = 28.dp)
                }
            }

            // Rotate the whole content block as one square — avoids per-item overlap.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val side = minOf(maxWidth, maxHeight)
                    Column(
                        modifier = Modifier
                            .size(side)
                            .graphicsLayer { rotationZ = contentRotation }
                            .padding(12.dp),
                    ) {
                        MetroText(
                            text = "photo settings…",
                            style = MetroTextStyle.HubLink,
                            color = Color.White,
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { onUpdate(state.copy(screen = CameraUiState.Screen.PhotoSettings)) },
                        )
                        Spacer(Modifier.height(18.dp))
                        MetroText(
                            text = "video settings…",
                            style = MetroTextStyle.HubLink,
                            color = Color.White,
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { onUpdate(state.copy(screen = CameraUiState.Screen.VideoSettings)) },
                        )
                        Spacer(Modifier.weight(1f))
                        MetroText(
                            text = "Show these settings in the viewfinder",
                            style = MetroTextStyle.ListItemSubtitle,
                            color = Color.White.copy(alpha = 0.55f),
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CameraUiState.QuickSetting.entries.take(5).forEach { option ->
                                val selected = option in state.viewfinderSlots
                                val glyph = option.toViewfinderGlyph(flashOff = flashOff)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(
                                            width = if (selected) 3.dp else 2.5.dp,
                                            color = Color.White,
                                        )
                                        .background(
                                            if (selected) {
                                                Color.White.copy(alpha = 0.12f)
                                            } else {
                                                Color.Transparent
                                            },
                                        )
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember {
                                                MutableInteractionSource()
                                            },
                                        ) {
                                            val next = state.viewfinderSlots.toMutableList()
                                            if (selected) {
                                                if (next.size > 1) next.remove(option)
                                            } else if (next.size < 4) {
                                                next.add(option)
                                            }
                                            onUpdate(state.copy(viewfinderSlots = next))
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CameraChromeIcon(glyph = glyph, size = 22.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun CameraUiState.QuickSetting.toViewfinderGlyph(flashOff: Boolean): CameraChromeGlyph =
    when (this) {
        CameraUiState.QuickSetting.Flash ->
            if (flashOff) CameraChromeGlyph.FlashOff else CameraChromeGlyph.Flash
        CameraUiState.QuickSetting.SwitchCamera -> CameraChromeGlyph.FlipCamera
        CameraUiState.QuickSetting.Lenses -> CameraChromeGlyph.Lens
        CameraUiState.QuickSetting.Iso -> CameraChromeGlyph.Iso
        CameraUiState.QuickSetting.WhiteBalance -> CameraChromeGlyph.Iso
        CameraUiState.QuickSetting.Exposure -> CameraChromeGlyph.Photo
        CameraUiState.QuickSetting.Scene -> CameraChromeGlyph.Lens
    }

@Composable
private fun SettingsList(
    title: String,
    rows: List<Pair<String, String>>,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        MetroText(
            text = title,
            style = MetroTextStyle.PageTitle,
            color = Color.White,
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onBack,
            ),
        )
        Spacer(Modifier.height(24.dp))
        rows.forEach { (label, value) ->
            Column(Modifier.padding(vertical = 12.dp)) {
                MetroText(text = label, style = MetroTextStyle.ListItemTitle, color = Color.White)
                MetroText(
                    text = value,
                    style = MetroTextStyle.ListItemSubtitle,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun BurstReviewPane(
    state: CameraUiState,
    onUpdate: (CameraUiState) -> Unit,
) {
    val accent = MetroTheme.colors.accent
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
    ) {
        MetroText(text = "burst", style = MetroTextStyle.HubTitle, color = Color.White)
        Spacer(Modifier.height(12.dp))
        MetroText(
            text = "${state.burstFrames.size} frames",
            style = MetroTextStyle.Body,
            color = Color.White.copy(alpha = 0.7f),
        )
        Spacer(Modifier.weight(1f))
        MetroText(
            text = "Deleting unsaved photos in ${state.burstRetentionDays} days",
            style = MetroTextStyle.Body,
            color = Color.White,
        )
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(1.dp))
            CircleGlyphButton(
                glyph = CameraChromeGlyph.Photo,
                selected = true,
                accent = accent,
                size = 64.dp,
                onClick = {
                    onUpdate(
                        state.copy(
                            screen = CameraUiState.Screen.Viewfinder,
                            burstFrames = emptyList(),
                            statusMessage = "saved",
                        ),
                    )
                },
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        onUpdate(
                            state.copy(
                                screen = CameraUiState.Screen.Viewfinder,
                                burstFrames = emptyList(),
                            ),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                CameraChromeIcon(glyph = CameraChromeGlyph.More, size = 28.dp)
            }
        }
    }
}

@Composable
private fun ModeButton(
    glyph: CameraChromeGlyph,
    selected: Boolean,
    glyphRotationDegrees: Float,
    onClick: () -> Unit,
) {
    // Selection is size-only — always a white ring (no accent/red highlight).
    val size = if (selected) 76.dp else 48.dp
    val border = if (selected) 3.5.dp else 2.5.dp
    Box(
        modifier = Modifier
            .size(size)
            .border(border, Color.White, CircleShape)
            .background(Color.White.copy(alpha = if (selected) 0.18f else 0.10f), CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CameraChromeIcon(
            glyph = glyph,
            size = if (selected) 30.dp else 20.dp,
            modifier = Modifier.graphicsLayer { rotationZ = glyphRotationDegrees },
        )
    }
}

@Composable
private fun CircleGlyphButton(
    glyph: CameraChromeGlyph,
    onClick: () -> Unit,
    glyphRotationDegrees: Float = 0f,
    selected: Boolean = false,
    accent: Color = Color.White,
    size: Dp = 44.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .border(2.5.dp, if (selected) accent else Color.White, CircleShape)
            .background(Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CameraChromeIcon(
            glyph = glyph,
            size = size * 0.45f,
            modifier = Modifier.graphicsLayer { rotationZ = glyphRotationDegrees },
        )
    }
}

private fun cycleIso(state: CameraUiState): CameraUiState {
    val values = listOf("auto", "100", "200", "400", "800", "1600")
    val next = values[(values.indexOf(state.iso).coerceAtLeast(0) + 1) % values.size]
    return state.copy(iso = next, statusMessage = "iso $next")
}

private fun photoSettingsRows(state: CameraUiState): List<Pair<String, String>> = listOf(
    "ISO" to state.iso,
    "White balance" to state.whiteBalance,
    "Aspect ratio" to state.photoAspect,
    "Focus assist light" to if (state.focusAssistLight) "On" else "Off",
    "Delete unsaved bursts after" to "${state.burstRetentionDays} days",
)

private fun videoSettingsRows(state: CameraUiState): List<Pair<String, String>> = listOf(
    "Video quality" to state.videoQuality,
    "Continuous focus" to if (state.continuousFocusVideo) "On" else "Off",
    "White balance" to state.whiteBalance,
)
