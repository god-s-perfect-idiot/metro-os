package com.metro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
     * Draws the monochrome glyph using the supplied tint. Do not bake a circle in — the app bar
     * draws the standard rest outline and filled press circle around every icon.
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
    /** Neutral gray chrome behind the bar. */
    val ChromeBackground: Color = Color(0xFF4C4C4C)
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
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    MetroAppBar(
        icons = icons,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
        menuItems = menuItems,
        minimized = minimized,
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
) {
    val visibleIcons = icons.take(MetroAppBarDefaults.MaxIcons)
    val visibleMenu = menuItems.take(MetroAppBarDefaults.MaxMenuItems)
    val chrome = MetroAppBarDefaults.ChromeBackground

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
                .background(chrome)
                .navigationBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MetroAppBarDefaults.BarHeight)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                val showIconRow = expanded || !minimized
                if (showIconRow) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.Top,
                    ) {
                        visibleIcons.forEach { item ->
                            AppBarIconButton(item = item, showLabel = expanded)
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(84.dp),
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
                        if (active) {
                            Modifier.background(baseColor, CircleShape)
                        } else {
                            Modifier.border(MetroAppBarDefaults.IconCircleBorder, baseColor, CircleShape)
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
    Box(
        modifier = modifier
            .size(MetroAppBarDefaults.TouchTarget)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        MetroSystemIcon(
            type = MetroSystemIconType.More,
            iconSize = MetroAppBarDefaults.GlyphSize,
            color = MetroTheme.colors.primaryText,
            showCircle = false,
        )
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
            .padding(horizontal = 24.dp, vertical = 12.dp),
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
