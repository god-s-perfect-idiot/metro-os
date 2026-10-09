package com.metro.statusbar.ui

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathParser
import androidx.core.graphics.drawable.toBitmap
import com.metro.statusbar.BatteryStatus
import com.metro.statusbar.BluetoothAudioKind
import com.metro.statusbar.SignalBarsStatus
import com.metro.statusbar.TrayIndicator
import com.metro.statusbar.TrayLayout
import com.metro.statusbar.TrayLayoutIcon
import com.metro.statusbar.TrayLayoutSlot
import com.metro.statusbar.TraySnapshot
import com.metro.statusbar.TraySpec
import com.metro.statusbar.TrayVisibilityMode
import com.metro.system.MetroAppBranding
import com.metro.ui.MetroAppGlyphs
import com.metro.ui.MetroColors
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTransitions
import com.metro.ui.MetroWifiBandCount
import com.metro.ui.drawMetroWifiGlyph
import kotlin.math.abs
import kotlinx.coroutines.delay

/** Empty cellular / Wi-Fi segments — darker than secondary text so they sit back on the tray. */
private val SignalInactiveDark = Color(0xFF4A4A4A)
private val SignalInactiveLight = Color(0xFFB0B0B0)
private val GlyphHeight = 14.dp
private val GlyphWidth = 16.dp
// Notification app icons need a touch more presence than stroke glyphs at tray size.
private val NotificationAppGlyphSize = 20.dp
// Cellular signal bars: slightly wider and shorter than the shared glyph box.
private val CellularGlyphHeight = 13.dp
private val CellularGlyphWidth = 18.dp
// Shared [drawMetroWifiGlyph] is square; outer arc clips the box edges.
private val WifiGlyphHeight = 13.dp
private val WifiGlyphWidth = 13.dp
// Mute (speaker + X) — a step larger than Wi-Fi so the mark reads at tray size.
private val MuteGlyphHeight = 16.dp
private val MuteGlyphWidth = 18.dp
// Hotspot + Bluetooth audio (headset/speaker) — larger for tray readability.
private val HotspotAudioGlyphHeight = 18.dp
private val HotspotAudioGlyphWidth = 20.dp
private val DataGlyphWidth = 22.dp
/** 512×512 mute path: speaker cone + X (status tray when ringer volume is 0). */
private const val MuteGlyphPathData =
    "M159.8,320l64,-64l-64,-64l85.3,-85.3l64,64V0L159.8,149.3H74.4v213.3h85.3L309.1,512V341.3l-64,64l-85.3,-85.3zm245.3,-128l-32,-32l-64,64l-64,-64l-32,32l64,64l-64,64l32,32l64,-64l64,64l32,-32l-64,-64l64,-64z"
/** Material-style Wi-Fi hotspot (24×24 viewBox). */
private const val HotspotGlyphPathData =
    "M12,11c-1.1,0-2,0.9-2,2s0.9,2,2,2s2,-0.9,2,-2s-0.9,-2,-2,-2m6,2c0,-3.31,-2.69,-6,-6,-6s-6,2.69,-6,6c0,2.22,1.21,4.15,3,5.19l1,-1.74c-1.19,-0.7,-2,-1.97,-2,-3.45c0,-2.21,1.79,-4,4,-4s4,1.79,4,4c0,1.48,-0.81,2.75,-2,3.45l1,1.74c1.79,-1.04,3,-2.97,3,-5.19M12,3C6.48,3,2,7.48,2,13c0,3.7,2.01,6.92,4.99,8.65l1,-1.73C5.61,18.53,4,15.96,4,13c0,-4.42,3.58,-8,8,-8s8,3.58,8,8c0,2.96,-1.61,5.53,-4,6.92l1,1.73c2.99,-1.73,5,-4.95,5,-8.65c0,-5.52,-4.48,-10,-10,-10"
/** Headset (24×24 viewBox). */
private const val HeadsetGlyphPathData =
    "M12,3a9,9,0,0,0,-9,9v9h6v-8H5v-1c0,-3.87,3.13,-7,7,-7s7,3.13,7,7v1h-4v8h6v-9a9,9,0,0,0,-9,-9"
/** Bluetooth speaker (24×24 viewBox). */
private const val SpeakerGlyphPathData =
    "M18,21H6V3h12zM13.143,8.14q0.473,-0.476,0.473,-1.144t-0.476,-1.14t-1.143,-0.472t-1.14,0.476t-0.472,1.143t0.475,1.14t1.144,0.472t1.14,-0.475m1.306,9.31q1.012,-1.012,1.012,-2.45t-1.012,-2.45T12,11.538T9.55,12.55T8.538,15t1.012,2.45T12,18.462t2.45,-1.012m-4.19,-0.712q-0.721,-0.723,-0.721,-1.74q0,-1.015,0.723,-1.737q0.724,-0.723,1.74,-0.723q1.015,0,1.738,0.724q0.722,0.723,0.722,1.74q0,1.015,-0.724,1.737q-0.723,0.722,-1.74,0.722q-1.015,0,-1.737,-0.723"
// WP8.1 battery sits close to clock cap height, with a slightly longer and shallower silhouette.
private val BatteryWidth = 29.dp
private val BatteryHeight = 14.dp
/** Clock / data-label size for the 32dp tray — a step under dialog body (16sp). */
private val TrayClockFontSize = 14.sp
private val TrayClockLineHeight = 16.sp
/** Data connection label (4G, LTE, …) relative to the glyph box height. */
private const val DataLabelTextSizeFactor = 0.98f

/**
 * WP8.1 system tray.
 *
 * Expanded = icons and mid-row spacers freely justified (`SpaceBetween`); spacers after the
 * rightmost icon sit outside that row so the clock stays flush against them. Collapsed = only
 * the rightmost icon remains in the justified row (plus those trailing spacers) — same clock
 * inset, no jump. Tap or going home drops the other icons in one-by-one from above
 * (left → right); they exit upward the same way until only the rightmost icon and its
 * trailing spacers remain.
 * Per-app [TrayVisibilityMode.Hidden] and immersive system-bar hide creep the whole strip into /
 * out of the top edge (200ms). Swipe down opens the Android notification shade and hides this
 * overlay while the shade is expanded. [barHeightDp] lets the overlay fill the whole system
 * status-bar region (including notch/cutout); defaults to the WP 32dp strip for in-app previews.
 *
 * When Android privacy dots (camera / mic / location) appear near the clock,
 * [privacyDotsNearClock] adds a small animated end nudge so content slides a touch left.
 * The tray stays fully opaque — system dots paint on top of the overlay.
 *
 * On display rotation the strip slides out, [onRotateRelayout] refreshes insets, then the strip
 * slides in from the new top of the screen.
 */
@Composable
fun StatusTray(
    snapshot: TraySnapshot,
    onTrayTap: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeOpenNotifications: (() -> Unit)? = null,
    barHeightDp: Int = TraySpec.TRAY_HEIGHT_DP,
    /** Physical left inset (cutout / rounded corner); not RTL start. */
    leftPaddingDp: Int = TraySpec.START_PADDING_DP,
    /** Physical right inset (cutout / rounded corner); not RTL end. */
    rightPaddingDp: Int = TraySpec.END_PADDING_DP,
    /** True while privacy dots sit on the right; animates a small clock nudge left. */
    privacyDotsNearClock: Boolean = false,
    /** [android.view.Surface] rotation constant for the current display. */
    displayRotation: Int = android.view.Surface.ROTATION_0,
    /** Invoked after slide-out and before slide-in when the display rotates. */
    onRotateRelayout: (() -> Unit)? = null,
    /**
     * Optional layout probe for the configure editor: reports each live slot's horizontal
     * center in this tray's root coordinates (same order as [layoutSlots]).
     */
    onSlotCentersChanged: ((List<Float>) -> Unit)? = null,
) {
    // Shade still hides instantly (overlay must drop so SystemUI is not covered).
    if (snapshot.notificationShadeOpen) return

    val trayVisible = snapshot.theme.visibilityMode != TrayVisibilityMode.Hidden &&
        !snapshot.systemStatusBarsHidden
    var onScreen by remember { mutableStateOf(trayVisible) }
    val barOffset = remember { Animatable(if (trayVisible) 0f else 1f) }
    var previousRotation by remember { mutableStateOf(displayRotation) }

    // Keep last opaque/translucent chrome while creeping out — Hidden resolves to Transparent.
    var paintTheme by remember { mutableStateOf(snapshot.theme) }
    SideEffect {
        if (snapshot.theme.visibilityMode != TrayVisibilityMode.Hidden) {
            paintTheme = snapshot.theme
        }
    }

    LaunchedEffect(trayVisible) {
        if (trayVisible) {
            barOffset.snapTo(1f)
            onScreen = true
            barOffset.animateTo(0f, MetroTransitions.statusTrayCreepTween())
        } else if (onScreen) {
            barOffset.animateTo(1f, MetroTransitions.statusTrayCreepTween())
            onScreen = false
        }
    }

    LaunchedEffect(displayRotation) {
        val fromRotation = previousRotation
        if (fromRotation == displayRotation) return@LaunchedEffect
        previousRotation = displayRotation
        if (!trayVisible || !onScreen) {
            onRotateRelayout?.invoke()
            return@LaunchedEffect
        }
        barOffset.animateTo(1f, MetroTransitions.statusTrayCreepTween())
        onRotateRelayout?.invoke()
        barOffset.animateTo(0f, MetroTransitions.statusTrayCreepTween())
    }

    if (!onScreen) return

    val targetForeground = paintTheme.foregroundColor
    // Volume underlay keeps the tray transparent so the HUD can paint one continuous band.
    // Toast / theme fills stay opaque — never fall back to page chrome while transparent.
    val targetBackground = paintTheme.backgroundColor
    val toUnderlay = targetBackground == Color.Transparent
    // Snap to transparent when the volume underlay takes over so charcoal is not veiled by a
    // fading tray fill; morph colors for toast accent and restore-from-underlay.
    val shellFillSpec = if (toUnderlay) {
        tween<Color>(0)
    } else {
        MetroTransitions.statusTrayShellFillTween(snapshot.shellFillAnimationMs)
    }
    val foreground by animateColorAsState(
        targetValue = targetForeground,
        animationSpec = shellFillSpec,
        label = "trayShellFillForeground",
    )
    val background by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = shellFillSpec,
        label = "trayShellFillBackground",
    )
    val backdrop by animateColorAsState(
        targetValue = paintTheme.backdropColor,
        animationSpec = shellFillSpec,
        label = "trayShellFillBackdrop",
    )
    val density = LocalDensity.current
    val slidePx = with(density) { barHeightDp.dp.toPx() }
    // 1 = fully tucked above the top edge; 0 = resting.
    val creepTranslationY = -barOffset.value * slidePx
    val shadeOpenDragPx = with(density) { TraySpec.SHADE_OPEN_DRAG_DP.dp.toPx() }
    val batteryPresent = snapshot.iconFlags.battery && snapshot.battery.present
    val layoutSlots = remember(
        snapshot.layout,
        snapshot.iconFlags,
    ) {
        TrayLayout.liveOccupying(
            slots = snapshot.layout,
            flags = snapshot.iconFlags,
        )
    }
    val rightmostIconIndex = remember(layoutSlots) {
        TrayLayout.rightmostIconIndex(layoutSlots)
    }
    val animatingIcons = remember(layoutSlots) {
        TrayLayout.animatingIcons(layoutSlots)
    }
    val acceptInput = trayVisible && barOffset.value < 0.01f
    val wifiConnected = snapshot.signalBars.wifiBands != null
    val glyphVisibility = remember(
        wifiConnected,
        snapshot.ringerMuted,
        snapshot.hotspotActive,
        snapshot.bluetoothAudio,
        snapshot.notificationPackage,
        batteryPresent,
    ) {
        TrayLayoutIcon.entries.associateWith { kind ->
            TrayLayout.isGlyphVisible(
                kind = kind,
                wifiConnected = wifiConnected,
                ringerMuted = snapshot.ringerMuted,
                hotspotActive = snapshot.hotspotActive,
                bluetoothAudio = snapshot.bluetoothAudio,
                notificationPackage = snapshot.notificationPackage,
                batteryPresent = batteryPresent,
            )
        }
    }

    val privacyNudge by animateDpAsState(
        targetValue = if (privacyDotsNearClock) TraySpec.PRIVACY_CLOCK_NUDGE_DP.dp else 0.dp,
        animationSpec = MetroTransitions.statusTrayPrivacyNudgeTween(),
        label = "privacyClockNudge",
    )

    // Keep the full SpaceBetween layout until stagger exit finishes so icons fade/slide
    // out in place instead of reshuffling as each child leaves the row.
    var holdJustifiedLayout by remember { mutableStateOf(snapshot.expanded) }
    LaunchedEffect(snapshot.expanded, animatingIcons.size) {
        if (snapshot.expanded) {
            holdJustifiedLayout = true
        } else if (holdJustifiedLayout) {
            delay(
                TraySpec.staggerSequenceMs(
                    iconCount = animatingIcons.size,
                    perIconMs = TraySpec.COLLAPSE_ANIMATION_MS,
                ),
            )
            holdJustifiedLayout = false
        }
    }

    // Trailing spacers sit outside SpaceBetween so the rightmost icon stays flush against
    // them when expanded and when collapsed — otherwise the free-justify gap between clock
    // and those spacers vanishes on auto-hide and the clock jumps.
    val justifiedSlotIndices = remember(layoutSlots) {
        TrayLayout.justifiedIndices(layoutSlots)
    }
    val trailingSpacerIndices = remember(layoutSlots) {
        TrayLayout.trailingSpacerIndices(layoutSlots)
    }
    val justifiedVisibleIndices = remember(
        justifiedSlotIndices,
        holdJustifiedLayout,
        rightmostIconIndex,
    ) {
        if (holdJustifiedLayout) {
            justifiedSlotIndices
        } else {
            justifiedSlotIndices.filter { it == rightmostIconIndex }
        }
    }
    val justifiedArrangement = if (holdJustifiedLayout && justifiedVisibleIndices.size > 1) {
        Arrangement.SpaceBetween
    } else {
        Arrangement.End
    }

    val slotCenterScratch = remember(layoutSlots.size, onSlotCentersChanged != null) {
        FloatArray(layoutSlots.size) { Float.NaN }
    }
    var trayRootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    fun publishSlotCentersIfReady() {
        val callback = onSlotCentersChanged ?: return
        // Trailing spacers may leave holes in the scratch array when none exist — only
        // require centers for composed slots.
        val required = justifiedVisibleIndices + trailingSpacerIndices
        if (required.any { it !in slotCenterScratch.indices || slotCenterScratch[it].isNaN() }) {
            return
        }
        callback(slotCenterScratch.toList())
    }
    fun measureModFor(index: Int): Modifier {
        if (onSlotCentersChanged == null) return Modifier
        return Modifier.onGloballyPositioned { coords ->
            val root = trayRootCoords ?: return@onGloballyPositioned
            if (!root.isAttached || !coords.isAttached) return@onGloballyPositioned
            val center = root.localPositionOf(
                sourceCoordinates = coords,
                relativeToSource = Offset(
                    coords.size.width / 2f,
                    coords.size.height / 2f,
                ),
            ).x
            if (index in slotCenterScratch.indices && slotCenterScratch[index] != center) {
                slotCenterScratch[index] = center
                publishSlotCentersIfReady()
            }
        }
    }

    @Composable
    fun TrayJustifiedSlot(index: Int, slot: TrayLayoutSlot) {
        val measureMod = measureModFor(index)
        when (slot) {
            is TrayLayoutSlot.Spacer -> {
                Spacer(modifier = measureMod.width(slot.widthDp.dp))
            }
            is TrayLayoutSlot.Icon -> {
                val isRightmost = index == rightmostIconIndex
                val forwardIndex = animatingIcons.indexOf(slot.kind).coerceAtLeast(0)
                val glyphLive = glyphVisibility[slot.kind] == true
                Box(modifier = measureMod) {
                    TrayLayoutGroup(
                        targetVisible = snapshot.expanded || isRightmost,
                        animate = !isRightmost,
                        forwardIndex = forwardIndex,
                    ) {
                        TraySlotGlyph(
                            kind = slot.kind,
                            foreground = foreground,
                            backdrop = backdrop,
                            inactiveColor = if (paintTheme.darkTheme) {
                                SignalInactiveDark
                            } else {
                                SignalInactiveLight
                            },
                            accentColor = paintTheme.accentColor,
                            clockText = snapshot.clockText,
                            showProgress = snapshot.showProgress,
                            battery = snapshot.battery,
                            dataConnectionLabel = snapshot.dataConnectionLabel,
                            signalBars = snapshot.signalBars,
                            bluetoothAudio = snapshot.bluetoothAudio,
                            notificationPackage = snapshot.notificationPackage,
                            modifier = Modifier.graphicsLayer {
                                alpha = if (glyphLive) 1f else 0f
                            },
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeightDp.dp)
            .clipToBounds()
            .onGloballyPositioned { trayRootCoords = it },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeightDp.dp)
                .graphicsLayer { translationY = creepTranslationY }
                .background(background)
                .pointerInput(shadeOpenDragPx, acceptInput) {
                    if (!acceptInput) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalDragY = 0f
                        var dragged = false
                        val pointerId = down.id
                        drag(pointerId) { change ->
                            val dy = change.positionChange().y
                            totalDragY += dy
                            if (abs(totalDragY) > viewConfiguration.touchSlop) {
                                dragged = true
                            }
                            if (dragged && onSwipeOpenNotifications != null && totalDragY > 0f) {
                                change.consume()
                            }
                        }
                        when {
                            dragged &&
                                onSwipeOpenNotifications != null &&
                                totalDragY >= shadeOpenDragPx -> onSwipeOpenNotifications.invoke()
                            !dragged -> onTrayTap()
                        }
                    }
                }
                .absolutePadding(
                    left = leftPaddingDp.dp,
                    right = rightPaddingDp.dp + privacyNudge,
                )
                .testTag("metro_status_tray"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = justifiedArrangement,
            ) {
                justifiedVisibleIndices.forEach { index ->
                    TrayJustifiedSlot(index = index, slot = layoutSlots[index])
                }
            }
            trailingSpacerIndices.forEach { index ->
                val spacer = layoutSlots[index] as TrayLayoutSlot.Spacer
                Spacer(
                    modifier = measureModFor(index).width(spacer.widthDp.dp),
                )
            }
        }
    }
}

/**
 * In-place stagger: keeps layout space reserved (no Row reflow) while alpha / translation
 * animate. [forwardIndex] 0 = leftmost animating icon.
 *
 * Exit: L→R, slides up. Enter: reverse of exit — same L→R stagger, slides down from above.
 * Always starts at progress 0 so remounts on expand actually play the enter (icons are not
 * composed while collapsed).
 */
@Composable
private fun TrayLayoutGroup(
    targetVisible: Boolean,
    animate: Boolean,
    forwardIndex: Int,
    content: @Composable () -> Unit,
) {
    if (!animate) {
        if (targetVisible) content()
        return
    }
    StaggeredTrayIconFixed(
        targetVisible = targetVisible,
        forwardIndex = forwardIndex,
        content = content,
    )
}

@Composable
private fun StaggeredTrayIconFixed(
    targetVisible: Boolean,
    forwardIndex: Int,
    content: @Composable () -> Unit,
) {
    // Remounted on each expand (collapsed row omits non-rightmost slots) — must start hidden
    // so enter can play. Exit keeps the same instance and animates 1 → 0.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(targetVisible, forwardIndex) {
        val delayMs = forwardIndex * MetroTransitions.StatusTrayIconStaggerMs.toLong()
        delay(delayMs)
        val duration = if (targetVisible) {
            MetroTransitions.StatusTrayExpandMs
        } else {
            MetroTransitions.StatusTrayCollapseMs
        }
        progress.animateTo(
            targetValue = if (targetVisible) 1f else 0f,
            animationSpec = tween(
                durationMillis = duration,
                easing = MetroTransitions.PageEasing,
            ),
        )
    }
    Box(
        modifier = Modifier.graphicsLayer {
            val p = progress.value
            alpha = p
            // p=0 → parked above the tray; p=1 → resting. Exit reverses enter.
            translationY = -size.height * (1f - p)
        },
    ) {
        content()
    }
}

@Composable
private fun rememberNotificationIconBitmap(packageName: String?): ImageBitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = LocalDensity.current
    val px = with(density) { NotificationAppGlyphSize.roundToPx().coerceAtLeast(1) }
    return remember(packageName, px) {
        if (packageName.isNullOrBlank()) return@remember null
        val suiteRes = MetroAppGlyphs.forPackage(packageName)
        val drawable = if (suiteRes != null) {
            ContextCompat.getDrawable(context, suiteRes)
        } else {
            MetroAppBranding.loadAppIcon(context, packageName)
        }
        drawable
            ?.let { MetroAppBranding.metroGlyphDrawable(it) ?: it }
            ?.toBitmap(px, px)
            ?.asImageBitmap()
    }
}


/**
 * One tray group glyph (network cluster, Wi-Fi, battery, clock, …). Shared by the live
 * overlay and setup previews / configure editor so every surface draws the same marks.
 */
@Composable
fun TraySlotGlyph(
    kind: TrayLayoutIcon,
    foreground: Color,
    backdrop: Color,
    inactiveColor: Color,
    clockText: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.Transparent,
    showProgress: Boolean = false,
    battery: BatteryStatus = TrayPreviewSamples.battery,
    dataConnectionLabel: String? = TrayPreviewSamples.DATA_LABEL,
    signalBars: SignalBarsStatus = TrayPreviewSamples.signalBars,
    bluetoothAudio: BluetoothAudioKind? = TrayPreviewSamples.bluetoothAudio,
    notificationPackage: String? = null,
) {
    when (kind) {
        TrayLayoutIcon.Clock -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = modifier,
            ) {
                if (showProgress) {
                    TrayProgressSpinner(color = accentColor)
                }
                BasicText(
                    text = clockText,
                    style = MetroTextStyle.DialogBody.toTextStyle().copy(
                        color = foreground,
                        fontSize = TrayClockFontSize,
                        lineHeight = TrayClockLineHeight,
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "Clock"
                    },
                )
            }
        }
        TrayLayoutIcon.Battery -> {
            TrayBatteryGlyph(
                battery = battery,
                color = foreground,
                backgroundColor = backdrop,
                modifier = modifier,
            )
        }
        TrayLayoutIcon.Network -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    TraySpec.CELLULAR_DATA_LABEL_GAP_DP.dp,
                ),
                modifier = modifier,
            ) {
                TrayIndicatorItem(
                    indicator = TrayIndicator.Cellular,
                    color = foreground,
                    inactiveColor = inactiveColor,
                    backgroundColor = backdrop,
                    signalBars = signalBars,
                )
                if (dataConnectionLabel != null) {
                    TrayIndicatorItem(
                        indicator = TrayIndicator.DataConnection,
                        color = foreground,
                        inactiveColor = inactiveColor,
                        backgroundColor = backdrop,
                        dataConnectionLabel = dataConnectionLabel,
                        signalBars = signalBars,
                    )
                }
            }
        }
        TrayLayoutIcon.Notifications -> {
            FlippingNotificationAppGlyph(
                packageName = notificationPackage,
                color = foreground,
                modifier = modifier,
            )
        }
        TrayLayoutIcon.Wifi,
        TrayLayoutIcon.Mute,
        TrayLayoutIcon.Hotspot,
        TrayLayoutIcon.BluetoothAudio,
        -> {
            val indicator = when (kind) {
                TrayLayoutIcon.Wifi -> TrayIndicator.Wifi
                TrayLayoutIcon.Mute -> TrayIndicator.Ringer
                TrayLayoutIcon.Hotspot -> TrayIndicator.WifiHotspot
                TrayLayoutIcon.BluetoothAudio -> TrayIndicator.BluetoothAudio
                else -> return
            }
            TrayIndicatorItem(
                indicator = indicator,
                color = foreground,
                inactiveColor = inactiveColor,
                backgroundColor = backdrop,
                dataConnectionLabel = dataConnectionLabel,
                signalBars = signalBars,
                bluetoothAudio = bluetoothAudio,
                modifier = modifier,
            )
        }
    }
}

/** Camera distance for tray notification tile flips (same scale as Start live tiles). */
private const val NotificationFlipCameraDistance = 16f

private val NotificationFlipHalf = MetroTransitions.tileFlipHalfTween<Float>()
private val NotificationFlipSettle = MetroTransitions.tileFlipSettleSpring<Float>()

/**
 * Live notification app glyph with a WP live-tile flip when [packageName] cycles to another
 * app: rotate to edge-on, swap face, settle flat.
 */
@Composable
private fun FlippingNotificationAppGlyph(
    packageName: String?,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val rotation = remember { Animatable(0f) }
    var displayedPackage by remember { mutableStateOf(packageName) }
    val displayedIcon = rememberNotificationIconBitmap(displayedPackage)

    LaunchedEffect(packageName) {
        if (packageName == displayedPackage) {
            if (abs(rotation.value) > 0.01f) {
                rotation.snapTo(0f)
            }
            return@LaunchedEffect
        }
        // Prior flip may have been cancelled mid-turn — never sit edge-on.
        if (abs(rotation.value) > 0.01f) {
            rotation.snapTo(0f)
        }
        val hadFace = !displayedPackage.isNullOrBlank()
        val hasFace = !packageName.isNullOrBlank()
        if (hadFace && hasFace) {
            // Front → edge: 0° → +90°, swap, −90° → 0° (same turnstile as Start live tiles).
            rotation.animateTo(90f, animationSpec = NotificationFlipHalf)
            displayedPackage = packageName
            rotation.snapTo(-90f)
            rotation.animateTo(0f, animationSpec = NotificationFlipSettle)
        } else {
            displayedPackage = packageName
            rotation.snapTo(0f)
        }
    }

    Canvas(
        modifier = modifier
            .size(NotificationAppGlyphSize)
            .graphicsLayer {
                rotationX = rotation.value
                transformOrigin = TransformOrigin(0.5f, 0.5f)
                cameraDistance = NotificationFlipCameraDistance * density
            },
    ) {
        drawIndicator(
            indicator = TrayIndicator.NotificationApp,
            color = color,
            inactiveColor = color,
            backgroundColor = Color.Transparent,
            notificationIcon = displayedIcon,
        )
    }
}

@Composable
private fun TrayIndicatorItem(
    indicator: TrayIndicator,
    color: Color,
    inactiveColor: Color,
    backgroundColor: Color,
    dataConnectionLabel: String? = null,
    signalBars: SignalBarsStatus = SignalBarsStatus.Unknown,
    bluetoothAudio: BluetoothAudioKind? = null,
    notificationIcon: ImageBitmap? = null,
    modifier: Modifier = Modifier,
) {
    val (width, height) = when (indicator) {
        TrayIndicator.Cellular -> CellularGlyphWidth to CellularGlyphHeight
        TrayIndicator.DataConnection -> DataGlyphWidth to GlyphHeight
        TrayIndicator.Wifi -> WifiGlyphWidth to WifiGlyphHeight
        TrayIndicator.Ringer -> MuteGlyphWidth to MuteGlyphHeight
        TrayIndicator.WifiHotspot,
        TrayIndicator.BluetoothAudio,
        -> HotspotAudioGlyphWidth to HotspotAudioGlyphHeight
        TrayIndicator.NotificationApp -> NotificationAppGlyphSize to NotificationAppGlyphSize
        else -> GlyphWidth to GlyphHeight
    }
    Canvas(modifier = modifier.size(width = width, height = height)) {
        drawIndicator(
            indicator = indicator,
            color = color,
            inactiveColor = inactiveColor,
            backgroundColor = backgroundColor,
            dataConnectionLabel = dataConnectionLabel,
            signalBars = signalBars,
            bluetoothAudio = bluetoothAudio,
            notificationIcon = notificationIcon,
        )
    }
}

private fun DrawScope.drawIndicator(
    indicator: TrayIndicator,
    color: Color,
    inactiveColor: Color,
    backgroundColor: Color,
    dataConnectionLabel: String? = null,
    signalBars: SignalBarsStatus = SignalBarsStatus.Unknown,
    bluetoothAudio: BluetoothAudioKind? = null,
    notificationIcon: ImageBitmap? = null,
) {
    val w = size.width
    val h = size.height
    when (indicator) {
        TrayIndicator.Cellular -> {
            val barWidth = w * 0.20f
            val gap = w * 0.065f
            val filled = signalBars.cellularBars.coerceIn(0, SignalBarsStatus.CELLULAR_BAR_COUNT)
            repeat(SignalBarsStatus.CELLULAR_BAR_COUNT) { index ->
                val barHeight = h * (0.4f + index * 0.2f)
                drawRect(
                    color = if (index < filled) color else inactiveColor,
                    topLeft = Offset(index * (barWidth + gap), h - barHeight),
                    size = Size(barWidth, barHeight),
                )
            }
        }
        TrayIndicator.DataConnection -> {
            val label = dataConnectionLabel ?: return
            drawContext.canvas.nativeCanvas.drawText(
                label,
                w / 2f,
                h * 0.86f,
                Paint().apply {
                    this.color = color.toArgb()
                    textSize = h * DataLabelTextSizeFactor
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                },
            )
        }
        TrayIndicator.CallForwarding -> {
            val stroke = h * 0.12f
            drawLine(color, Offset(w * 0.12f, h * 0.72f), Offset(w * 0.7f, h * 0.72f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.7f, h * 0.72f), Offset(w * 0.7f, h * 0.28f), stroke, StrokeCap.Round)
            val head = Path().apply {
                moveTo(w * 0.52f, h * 0.42f)
                lineTo(w * 0.7f, h * 0.22f)
                lineTo(w * 0.88f, h * 0.42f)
            }
            drawPath(head, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
        TrayIndicator.Roaming -> {
            val tri = Path().apply {
                moveTo(w * 0.5f, h * 0.18f)
                lineTo(w * 0.86f, h * 0.82f)
                lineTo(w * 0.14f, h * 0.82f)
                close()
            }
            drawPath(tri, color)
        }
        TrayIndicator.Wifi -> {
            drawMetroWifiGlyph(
                color = color,
                inactiveColor = inactiveColor,
                filledBands = (signalBars.wifiBands ?: 0).coerceIn(0, MetroWifiBandCount),
                bandCount = MetroWifiBandCount,
            )
        }
        TrayIndicator.Bluetooth -> {
            val stroke = h * 0.1f
            val cx = w / 2f
            drawLine(color, Offset(cx, h * 0.12f), Offset(cx, h * 0.88f), stroke, StrokeCap.Round)
            drawLine(color, Offset(cx, h * 0.12f), Offset(cx + w * 0.18f, h * 0.32f), stroke, StrokeCap.Round)
            drawLine(color, Offset(cx + w * 0.18f, h * 0.32f), Offset(cx - w * 0.04f, h * 0.5f), stroke, StrokeCap.Round)
            drawLine(color, Offset(cx, h * 0.88f), Offset(cx + w * 0.18f, h * 0.68f), stroke, StrokeCap.Round)
            drawLine(color, Offset(cx + w * 0.18f, h * 0.68f), Offset(cx - w * 0.04f, h * 0.5f), stroke, StrokeCap.Round)
        }
        TrayIndicator.QuietHours -> {
            // Crescent moon: a disc with an offset disc carved out in the tray background color.
            val r = h * 0.46f
            val center = Offset(w * 0.5f, h * 0.5f)
            drawCircle(color, r, center)
            drawCircle(backgroundColor, r * 0.92f, Offset(center.x + r * 0.6f, center.y - r * 0.18f))
        }
        TrayIndicator.DrivingMode -> {
            val stroke = h * 0.08f
            // Cabin + body of a small car.
            drawRoundRectPath(
                left = w * 0.22f, top = h * 0.28f, right = w * 0.78f, bottom = h * 0.55f, color = color,
            )
            drawRoundRectPath(
                left = w * 0.08f, top = h * 0.48f, right = w * 0.92f, bottom = h * 0.72f, color = color,
            )
            drawCircle(backgroundColor, w * 0.1f, Offset(w * 0.3f, h * 0.72f))
            drawCircle(backgroundColor, w * 0.1f, Offset(w * 0.7f, h * 0.72f))
            drawCircle(color, w * 0.06f, Offset(w * 0.3f, h * 0.72f))
            drawCircle(color, w * 0.06f, Offset(w * 0.7f, h * 0.72f))
            drawLine(color, Offset(w * 0.08f, h * 0.72f), Offset(w * 0.92f, h * 0.72f), stroke)
        }
        TrayIndicator.Ringer -> {
            // Mute: speaker + X when ringer volume is 0 (expanded row only).
            drawMuteGlyph(color)
        }
        TrayIndicator.WifiHotspot -> {
            drawScaledPathGlyph(HotspotGlyphPathData, color, viewBox = 24f)
        }
        TrayIndicator.BluetoothAudio -> {
            val path = when (bluetoothAudio) {
                BluetoothAudioKind.Speaker -> SpeakerGlyphPathData
                BluetoothAudioKind.Headset, null -> HeadsetGlyphPathData
            }
            drawScaledPathGlyph(path, color, viewBox = 24f)
        }
        TrayIndicator.NotificationApp -> {
            if (notificationIcon != null) {
                drawImage(
                    image = notificationIcon,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                )
            } else {
                // Default / preview mark: Windows Start flag (not this app's launcher icon).
                drawWindowsStartGlyph(color)
            }
        }
        TrayIndicator.Location -> {
            val stroke = Stroke(width = h * 0.09f)
            drawCircle(color, w * 0.3f, Offset(w / 2f, h / 2f), style = stroke)
            drawCircle(color, w * 0.1f, Offset(w / 2f, h / 2f))
            drawLine(color, Offset(w / 2f, h * 0.04f), Offset(w / 2f, h * 0.22f), stroke.width)
            drawLine(color, Offset(w / 2f, h * 0.78f), Offset(w / 2f, h * 0.96f), stroke.width)
            drawLine(color, Offset(w * 0.04f, h / 2f), Offset(w * 0.22f, h / 2f), stroke.width)
            drawLine(color, Offset(w * 0.78f, h / 2f), Offset(w * 0.96f, h / 2f), stroke.width)
        }
        TrayIndicator.Battery -> Unit // Rendered separately on the right by TrayBatteryGlyph.
    }
}

/** Speaker + X mute mark scaled from the 512×512 tray reference path. */
private fun DrawScope.drawMuteGlyph(color: Color) {
    drawScaledPathGlyph(MuteGlyphPathData, color, viewBox = 512f)
}

/**
 * Four-pane Windows Start flag, tight to the glyph box (same proportions as
 * [com.metro.ui.R.drawable.metro_app_launcher] without adaptive safe-zone padding).
 */
private fun DrawScope.drawWindowsStartGlyph(color: Color) {
    val gap = size.minDimension * 0.06f
    val left = size.width * 0.06f
    val top = size.height * 0.10f
    val right = size.width * 0.94f
    val bottom = size.height * 0.90f
    val midX = left + (right - left) * 0.42f
    val midY = top + (bottom - top) * 0.48f
    // Top-left pane (slightly lower outer edge — WP flag perspective).
    drawPath(
        Path().apply {
            moveTo(left, top + size.height * 0.04f)
            lineTo(midX - gap / 2f, top)
            lineTo(midX - gap / 2f, midY - gap / 2f)
            lineTo(left, midY - gap / 2f)
            close()
        },
        color = color,
    )
    // Top-right
    drawPath(
        Path().apply {
            moveTo(midX + gap / 2f, top - size.height * 0.02f)
            lineTo(right, top - size.height * 0.06f)
            lineTo(right, midY - gap / 2f - size.height * 0.01f)
            lineTo(midX + gap / 2f, midY - gap / 2f)
            close()
        },
        color = color,
    )
    // Bottom-left
    drawPath(
        Path().apply {
            moveTo(left, midY + gap / 2f)
            lineTo(midX - gap / 2f, midY + gap / 2f + size.height * 0.01f)
            lineTo(midX - gap / 2f, bottom + size.height * 0.02f)
            lineTo(left, bottom - size.height * 0.02f)
            close()
        },
        color = color,
    )
    // Bottom-right
    drawPath(
        Path().apply {
            moveTo(midX + gap / 2f, midY + gap / 2f)
            lineTo(right, midY + gap / 2f)
            lineTo(right, bottom + size.height * 0.06f)
            lineTo(midX + gap / 2f, bottom)
            close()
        },
        color = color,
    )
}

private fun DrawScope.drawScaledPathGlyph(pathData: String, color: Color, viewBox: Float) {
    val androidPath = PathParser.createPathFromPathData(pathData)
    val matrix = Matrix().apply {
        setScale(size.width / viewBox, size.height / viewBox)
    }
    androidPath.transform(matrix)
    drawPath(androidPath.asComposePath(), color)
}

/** Small filled-rect helper to keep the car/handset glyphs readable at tray sizes. */
private fun DrawScope.drawRoundRectPath(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    color: Color,
) {
    drawRect(color = color, topLeft = Offset(left, top), size = Size(right - left, bottom - top))
}

/**
 * WP8.1 charging overlay — solid two-prong plug with a dark edge stroke.
 * Rounded head interrupts the top casing; cord runs to the bottom casing line
 * (does not extend past the battery frame).
 */
private data class ChargingPlugLayout(
    val topGapLeft: Float,
    val topGapRight: Float,
    val bottomGapLeft: Float,
    val bottomGapRight: Float,
    val outlineWidth: Float,
    val path: Path,
)

private fun chargingPlugLayout(
    centerX: Float,
    bodyTop: Float,
    bodyBottom: Float,
    bodyWidth: Float,
    strokeWidth: Float,
): ChargingPlugLayout {
    val bodyHeight = bodyBottom - bodyTop
    val plugW = bodyWidth * 0.32f
    // Prong/cord weight matches the battery stroke.
    val prongW = strokeWidth * 1.15f
    val prongGap = plugW * 0.30f
    val cordW = strokeWidth * 1.25f
    val corner = CornerRadius(plugW * 0.18f, plugW * 0.18f)
    val outlineWidth = strokeWidth * 1.15f

    // Rounded-square head; top edge covers the top casing stroke.
    val headH = plugW * 0.95f
    val headTop = bodyTop - strokeWidth * 0.5f
    val headBottom = headTop + headH
    val headLeft = centerX - plugW / 2f
    val headRight = headLeft + plugW

    // Prongs sit entirely above the battery top border.
    val prongH = (headTop - 0f).coerceIn(bodyHeight * 0.22f, bodyHeight * 0.38f)
    val prongTop = headTop - prongH
    val leftProngX = centerX - prongGap / 2f - prongW
    val rightProngX = centerX + prongGap / 2f

    // Cord ends so its dark outline sits flush with the outer bottom casing line.
    val cordTop = headBottom - strokeWidth * 0.25f
    val cordBottom = bodyBottom + strokeWidth * 0.5f - outlineWidth * 0.5f
    val cordH = (cordBottom - cordTop).coerceAtLeast(strokeWidth)

    // Gap pad absorbs half the plug outline so casing meets the dark edge cleanly.
    val gapPad = outlineWidth * 0.5f + strokeWidth * 0.08f
    val path = Path().apply {
        addRect(Rect(Offset(leftProngX, prongTop), Size(prongW, prongH)))
        addRect(Rect(Offset(rightProngX, prongTop), Size(prongW, prongH)))
        addRoundRect(
            RoundRect(
                left = headLeft,
                top = headTop,
                right = headRight,
                bottom = headBottom,
                cornerRadius = corner,
            ),
        )
        addRect(Rect(Offset(centerX - cordW / 2f, cordTop), Size(cordW, cordH)))
    }
    return ChargingPlugLayout(
        topGapLeft = headLeft - gapPad,
        topGapRight = headRight + gapPad,
        bottomGapLeft = centerX - cordW / 2f - gapPad,
        bottomGapRight = centerX + cordW / 2f + gapPad,
        outlineWidth = outlineWidth,
        path = path,
    )
}

/** Open U-shells: top gap matches the plug head; bottom gap matches the cord. */
private fun DrawScope.drawBatteryOutlineWithPlugGaps(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    topGapLeft: Float,
    topGapRight: Float,
    bottomGapLeft: Float,
    bottomGapRight: Float,
    strokeWidth: Float,
    color: Color,
) {
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt, join = StrokeJoin.Miter)
    val leftShell = Path().apply {
        moveTo(topGapLeft, top)
        lineTo(left, top)
        lineTo(left, bottom)
        lineTo(bottomGapLeft, bottom)
    }
    val rightShell = Path().apply {
        moveTo(topGapRight, top)
        lineTo(right, top)
        lineTo(right, bottom)
        lineTo(bottomGapRight, bottom)
    }
    drawPath(leftShell, color = color, style = stroke)
    drawPath(rightShell, color = color, style = stroke)
}

@Composable
private fun TrayBatteryGlyph(
    battery: BatteryStatus,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = BatteryWidth, height = BatteryHeight)) {
        val bodyWidth = size.width * 0.898f
        val bodyHeight = size.height * 0.70f
        val left = 0f
        val top = (size.height - bodyHeight) / 2f
        val right = left + bodyWidth
        val bottom = top + bodyHeight
        val strokeWidth = bodyHeight * 0.13f
        val fillColor = if (battery.isLow) MetroColors.AccentRed else color

        val plug = if (battery.charging) {
            chargingPlugLayout(
                centerX = left + bodyWidth * 0.5f,
                bodyTop = top,
                bodyBottom = bottom,
                bodyWidth = bodyWidth,
                strokeWidth = strokeWidth,
            )
        } else {
            null
        }

        val inset = strokeWidth * 1.55f
        val fillTrackWidth = bodyWidth - inset * 2f
        val fillWidth = fillTrackWidth * battery.fraction.coerceIn(0f, 1f)
        if (fillWidth > 0f) {
            drawRect(
                color = fillColor,
                topLeft = Offset(left + inset, top + inset),
                size = Size(fillWidth, bodyHeight - inset * 2f),
            )
        }

        // Clear charge fill under the plug so red/white fill does not show through.
        if (plug != null) {
            drawPath(plug.path, color = backgroundColor)
        }

        if (plug != null) {
            drawBatteryOutlineWithPlugGaps(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                topGapLeft = plug.topGapLeft,
                topGapRight = plug.topGapRight,
                bottomGapLeft = plug.bottomGapLeft,
                bottomGapRight = plug.bottomGapRight,
                strokeWidth = strokeWidth,
                color = color,
            )
        } else {
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(bodyWidth, bodyHeight),
                style = Stroke(width = strokeWidth),
            )
        }

        val nubWidth = size.width - bodyWidth
        drawRect(
            color = color,
            topLeft = Offset(right, top + bodyHeight * 0.30f),
            size = Size(nubWidth, bodyHeight * 0.40f),
        )

        if (plug != null) {
            drawPath(
                plug.path,
                color = Color.Black,
                style = Stroke(
                    width = plug.outlineWidth,
                    join = StrokeJoin.Round,
                    cap = StrokeCap.Round,
                ),
            )
            drawPath(plug.path, color = color)
        }
    }
}

@Composable
private fun TrayProgressSpinner(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(16.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(13.dp)) {
            rotate(degrees = -90f) {
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = size.minDimension * 0.12f, cap = StrokeCap.Round),
                )
            }
        }
    }
}
