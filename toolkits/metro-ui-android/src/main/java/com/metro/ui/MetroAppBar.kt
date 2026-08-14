package com.metro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * WP8.1 application bar (§6.2 of METRO-UX-LANGUAGE.md).
 *
 * Anchored to the **bottom** of the screen. In its collapsed state it shows up to four
 * monochrome icon buttons plus the `…` ellipsis. Tapping the ellipsis (or any of the icons'
 * white dots) expands the bar to reveal:
 *   1. a short text label beneath every icon, and
 *   2. a vertical list of text-only overflow [menuItems] below the icon row.
 *
 * Place it last inside a bottom-aligned [Box] so the expanded panel can overlay page content:
 *
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     PageContent()
 *     MetroAppBar(
 *         icons = listOf(
 *             MetroAppBarIcon(MetroSystemIconType.Add, label = "new", onClick = { /* … */ }),
 *         ),
 *         menuItems = listOf(MetroAppBarMenuItem("settings") { /* … */ }),
 *         modifier = Modifier.align(Alignment.BottomCenter),
 *     )
 * }
 * ```
 */

/** A single primary icon button in the app bar icon row. */
class MetroAppBarIcon(
    val label: String,
    val onClick: () -> Unit,
    val contentDescription: String = label,
    val enabled: Boolean = true,
    /**
     * When true (default), the app bar draws the rest-state circular outline. Set false only when
     * [icon] already paints its own circle (e.g. a custom glyph that matches [MetroSystemIcon]).
     * The filled press circle is always drawn by the app bar.
     */
    val showRestOutline: Boolean = true,
    /**
     * Draws the monochrome glyph using the supplied tint. Do not bake a circle in unless
     * [showRestOutline] is false — the app bar draws the standard rest outline and filled press
     * circle around every icon by default.
     */
    val icon: @Composable (color: Color) -> Unit,
)

/**
 * Convenience builder backed by a [MetroSystemIconType] glyph. The standard circular outline
 * (rest) and filled press circle are both drawn by the app bar, so the glyph itself is rendered
 * without its own circle.
 */
@Suppress("FunctionName")
fun MetroAppBarIcon(
    type: MetroSystemIconType,
    label: String,
    onClick: () -> Unit,
    contentDescription: String = label,
    enabled: Boolean = true,
): MetroAppBarIcon = MetroAppBarIcon(
    label = label,
    onClick = onClick,
    contentDescription = contentDescription,
    enabled = enabled,
    icon = { color ->
        MetroSystemIcon(
            type = type,
            iconSize = MetroAppBarDefaults.GlyphSize,
            color = color,
            showCircle = false,
        )
    },
)

/** A text-only overflow row revealed when the bar is expanded. */
class MetroAppBarMenuItem(
    val text: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

object MetroAppBarDefaults {
    val BarHeight: Dp = 52.dp
    val GlyphSize: Dp = 42.dp
    val TouchTarget: Dp = 48.dp
    /** Diameter of the standard circular outline drawn around every app-bar icon glyph. */
    val IconCircleSize: Dp = 40.dp
    /** Stroke width of the rest-state circular outline. */
    val IconCircleBorder: Dp = 1.5.dp
    /** Secondary-surface chrome behind the bar (`#1F1F1F`). */
    val ChromeBackground: Color = MetroColors.DarkSecondarySurface
    /** Top inset for the `…` ellipsis — near the bar’s top edge, with a small breath. */
    val EllipsisTopPadding: Dp = 6.dp
    /** Width of the three-dot ellipsis glyph. */
    val EllipsisWidth: Dp = 28.dp
    /** Height of the three-dot ellipsis glyph (keeps dots near the top edge). */
    val EllipsisHeight: Dp = 10.dp
    /** Radius of each ellipsis dot. */
    val EllipsisDotRadius: Dp = 2.dp
    /** Center-to-center spacing between ellipsis dots. */
    val EllipsisDotSpacing: Dp = 9.dp
    const val MaxIcons = 4
    const val MaxMenuItems = 5
}

/** Uncontrolled variant — manages its own expand/collapse state. */
@Composable
fun MetroAppBar(
    icons: List<MetroAppBarIcon>,
    modifier: Modifier = Modifier,
    menuItems: List<MetroAppBarMenuItem> = emptyList(),
    minimized: Boolean = false,
    /** When false, plays a creep-out animation before removing the bar from composition. */
    visible: Boolean = true,
    /** When non-null, replays the enter animation whenever this value changes while [visible]. */
    enterKey: Any? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (!visible) {
            expanded = false
        }
    }
    MetroAppBar(
        icons = icons,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
        menuItems = menuItems,
        minimized = minimized,
        visible = visible,
        enterKey = enterKey,
    )
}

/**
 * Controlled variant. Hosts that want the system Back key to collapse the bar should pass
 * [expanded] / [onExpandedChange] and wire their own `BackHandler(expanded) { … false }`.
 *
 * @param minimized when true the collapsed bar shows only the `…` ellipsis (mandatory on
 *   panorama pages); the icon row appears once expanded.
 */
@Composable
fun MetroAppBar(
    icons: List<MetroAppBarIcon>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    menuItems: List<MetroAppBarMenuItem> = emptyList(),
    minimized: Boolean = false,
    /** When false, plays a creep-out animation before removing the bar from composition. */
    visible: Boolean = true,
    /** When non-null, replays the enter animation whenever this value changes while [visible]. */
    enterKey: Any? = null,
) {
    val visibleIcons = icons.take(MetroAppBarDefaults.MaxIcons)
    val visibleMenu = menuItems.take(MetroAppBarDefaults.MaxMenuItems)
    val chrome = MetroAppBarDefaults.ChromeBackground
    var enterEpoch by remember { mutableIntStateOf(0) }
    var onScreen by remember { mutableStateOf(visible) }

    val barOffset = remember { Animatable(1f) }
    val density = LocalDensity.current
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val defaultSlidePx = with(density) { (MetroAppBarDefaults.BarHeight + navBottomInset).toPx() }
    var collapsedSlidePx by remember { mutableFloatStateOf(defaultSlidePx) }

    LaunchedEffect(visible, enterKey) {
        if (visible) {
            barOffset.snapTo(1f)
            onScreen = true
            enterEpoch++
            barOffset.animateTo(0f, MetroTransitions.appBarCreepTween())
        } else if (onScreen) {
            if (expanded) {
                onExpandedChange(false)
            }
            barOffset.animateTo(1f, MetroTransitions.appBarCreepTween())
            onScreen = false
        }
    }

    if (!onScreen) return

    val hiddenFraction = barOffset.value
    val slidePx = collapsedSlidePx.takeIf { it > 0f } ?: defaultSlidePx
    val creepTranslationY = hiddenFraction * slidePx
    val animateIconKeys = visible && enterEpoch > 0

    // Fill the parent only while expanded so the dismiss scrim can intercept outside taps;
    // collapsed it simply wraps the bar so page content underneath stays interactive.
    val rootModifier = if (expanded) Modifier.fillMaxSize() else Modifier.fillMaxWidth()

    Box(
        modifier = modifier.then(rootModifier),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onExpandedChange(false) },
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    // Latch resting collapsed height only — never while creeping (avoids squish loop).
                    if (!expanded && hiddenFraction <= 0f) {
                        collapsedSlidePx = size.height.toFloat()
                    }
                }
                .graphicsLayer {
                    translationY = creepTranslationY
                }
                .background(chrome)
                .navigationBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MetroAppBarDefaults.BarHeight)
                    .padding(start = 8.dp, end = 8.dp, bottom = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                val showIconRow = expanded || !minimized
                if (showIconRow) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        visibleIcons.forEach { item ->
                            AppBarIconButton(
                                item = item,
                                showLabel = expanded,
                                enterEpoch = enterEpoch,
                                animateKeys = animateIconKeys,
                            )
                        }
                    }
                }
                EllipsisButton(
                    expanded = expanded,
                    onClick = { onExpandedChange(!expanded) },
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }

            AnimatedVisibility(
                visible = expanded && visibleMenu.isNotEmpty(),
                enter = expandVertically(animationSpec = tween(MetroTransitions.AppBarSlideMs)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(MetroTransitions.AppBarSlideMs)) + fadeOut(),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    visibleMenu.forEach { menuItem ->
                        AppBarMenuRow(
                            item = menuItem,
                            onSelected = { onExpandedChange(false) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBarIconButton(
    item: MetroAppBarIcon,
    showLabel: Boolean,
    enterEpoch: Int,
    animateKeys: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val baseColor = MetroTheme.colors.primaryText.let {
        if (item.enabled) it else it.copy(alpha = 0.4f)
    }
    val active = pressed && item.enabled
    // Standard affordance: a circular outline at rest that fills on press; the glyph then inverts
    // to the chrome color so it reads on the filled circle.
    val glyphColor = if (active) MetroTheme.colors.background else baseColor
    val buttonHeightPx = with(LocalDensity.current) { MetroAppBarDefaults.TouchTarget.toPx() }
    val offsetAnim = remember { Animatable(0f) }
    val opacityAnim = remember { Animatable(1f) }

    LaunchedEffect(enterEpoch, animateKeys) {
        if (!animateKeys || enterEpoch == 0) {
            offsetAnim.snapTo(0f)
            opacityAnim.snapTo(1f)
            return@LaunchedEffect
        }
        offsetAnim.snapTo(MetroTransitions.AppBarButtonStartOffsetFraction)
        opacityAnim.snapTo(0f)
        launch {
            opacityAnim.animateTo(1f, MetroTransitions.appBarCreepTween())
        }
        offsetAnim.animateTo(0f, MetroTransitions.appBarButtonOvershootKeyframes())
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(84.dp)
            .graphicsLayer {
                translationY = offsetAnim.value * buttonHeightPx
                alpha = opacityAnim.value
            },
    ) {
        Box(
            modifier = Modifier
                .size(MetroAppBarDefaults.TouchTarget)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = item.enabled,
                    onClick = item.onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(MetroAppBarDefaults.IconCircleSize)
                    .then(
                        when {
                            active -> Modifier.background(baseColor, CircleShape)
                            item.showRestOutline -> Modifier.border(
                                MetroAppBarDefaults.IconCircleBorder,
                                baseColor,
                                CircleShape,
                            )
                            else -> Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                item.icon(glyphColor)
            }
        }
        AnimatedVisibility(visible = showLabel) {
            BasicText(
                text = item.label,
                style = TextStyle(
                    fontFamily = MetroFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    color = baseColor,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun EllipsisButton(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = MetroTheme.colors.primaryText
    Box(
        modifier = modifier
            .size(MetroAppBarDefaults.TouchTarget)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(top = MetroAppBarDefaults.EllipsisTopPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(
            modifier = Modifier.size(
                width = MetroAppBarDefaults.EllipsisWidth,
                height = MetroAppBarDefaults.EllipsisHeight,
            ),
        ) {
            val radius = MetroAppBarDefaults.EllipsisDotRadius.toPx()
            val spacing = MetroAppBarDefaults.EllipsisDotSpacing.toPx()
            val cy = radius
            val cx = size.width / 2f
            drawCircle(color, radius, Offset(cx - spacing, cy))
            drawCircle(color, radius, Offset(cx, cy))
            drawCircle(color, radius, Offset(cx + spacing, cy))
        }
    }
}

@Composable
private fun AppBarMenuRow(
    item: MetroAppBarMenuItem,
    onSelected: () -> Unit,
) {
    val color = MetroTheme.colors.primaryText.let {
        if (item.enabled) it else it.copy(alpha = 0.4f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MetroAppBarDefaults.TouchTarget)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = item.enabled,
                onClick = {
                    item.onClick()
                    onSelected()
                },
            )
            .padding(horizontal = MetroDimens.ScreenHorizontalMargin, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(
            text = item.text,
            style = TextStyle(
                fontFamily = MetroFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                color = color,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
