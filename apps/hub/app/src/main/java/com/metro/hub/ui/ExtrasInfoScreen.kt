package com.metro.hub.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.metro.hub.R
import com.metro.ui.MetroAppGlyphs
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroColors
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import kotlinx.coroutines.delay

/** Brand red for Metro Ruby alphas — WP crimson, matching Lumia Cyan accent treatment. */
internal val MetroRubyColor: Color = MetroColors.AccentCrimson

private val ReleaseNameStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Light,
    fontSize = 36.sp,
    lineHeight = 42.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private val ComponentLineStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 26.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private val UnderlineLinkStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    textDecoration = TextDecoration.Underline,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private val SupportTileGap = 8.dp
/** Smaller than half-width 2-up; Start 2×2 is 198dp — keep these compact. */
private val SupportTileSize = 132.dp
private val SupportTileInset = 8.dp
private val SupportTileSlideStart = 72.dp
private const val SupportTileEnterMs = 320
private const val SupportTileStaggerMs = 70L
private val SupportTileEnterEasing = CubicBezierEasing(0.3f, 1f, 0.2f, 1f)

/** Compact Start-tile label — a touch under ListItemTitle (24sp). */
private val SupportTileTitleStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 22.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private data class SupportTileSpec(
    val titleRes: Int,
    val url: String,
    val faceColor: Color,
    val backgroundImageUrl: String? = null,
    val useUserIcon: Boolean = false,
    val titleColor: Color? = null,
)

@Composable
fun ExtrasInfoScreen(
    state: HubState,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observe = generation

    val release = state.release
    val tag = release?.tagName?.takeIf { it.isNotBlank() }
    val releaseLabel = tag ?: stringResource(R.string.extras_release_unknown)
    val assetCount = state.allAssets.size
    val primary = MetroTheme.colors.primaryText
    val secondary = MetroTheme.colors.secondaryText

    Column(modifier = modifier.fillMaxSize()) {
        MetroText(
            text = stringResource(R.string.extras_info_title),
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 20.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
        ) {
            MetroText(
                text = stringResource(R.string.extras_software_release),
                style = MetroTextStyle.SectionHeader,
                color = secondary,
                modifier = Modifier.padding(bottom = 6.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 20.dp),
            ) {
                BasicText(
                    text = stringResource(R.string.extras_metro_ruby),
                    style = ReleaseNameStyle.copy(fontFamily = MetroTheme.fontFamily, color = MetroRubyColor),
                    maxLines = 1,
                )
                RubyInfoGlyph(color = MetroRubyColor)
            }

            BasicText(
                text = stringResource(R.string.extras_source_on_github),
                style = UnderlineLinkStyle.copy(fontFamily = MetroTheme.fontFamily, color = primary),
                modifier = Modifier
                    .clickable { state.openExternalUrl(HubState.GITHUB_URL) }
                    .padding(vertical = 4.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BasicText(
                text = stringResource(R.string.extras_intro),
                style = ComponentLineStyle.copy(fontFamily = MetroTheme.fontFamily, color = primary),
            )

            Spacer(modifier = Modifier.height(20.dp))

            MetroText(
                text = stringResource(R.string.extras_support_section).uppercase(),
                style = MetroTextStyle.SectionHeader,
                color = secondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            SupportTilesGrid(
                tiles = supportTiles(),
                onOpen = state::openExternalUrl,
            )

            Spacer(modifier = Modifier.height(20.dp))

            ComponentLine(
                text = stringResource(R.string.extras_line_latest_release, releaseLabel),
                color = primary,
            )
            ComponentLine(
                text = stringResource(R.string.extras_line_channel),
                color = primary,
            )
            ComponentLine(
                text = stringResource(R.string.extras_line_suite_apps, assetCount),
                color = primary,
            )
            ComponentLine(
                text = stringResource(R.string.extras_line_publisher),
                color = primary,
            )
            ComponentLine(
                text = stringResource(R.string.extras_line_platform),
                color = primary,
            )

            Spacer(modifier = Modifier.height(28.dp))

            MetroBorderButton(
                text = stringResource(R.string.extras_more_info),
                onClick = {
                    val url = if (tag != null) {
                        HubState.releaseUrlForTag(tag)
                    } else {
                        HubState.GITHUB_URL
                    }
                    state.openExternalUrl(url)
                },
            )
        }
    }
}

@Composable
private fun supportTiles(): List<SupportTileSpec> = listOf(
    SupportTileSpec(
        titleRes = R.string.extras_support_mine,
        url = HubState.BUY_ME_A_COFFEE_URL,
        faceColor = MetroColors.AccentGreen,
        backgroundImageUrl = HubState.SUPPORT_AVATAR_MINE,
        titleColor = MetroColors.LightPrimaryText,
    ),
    SupportTileSpec(
        titleRes = R.string.extras_support_alexthew,
        url = HubState.SUPPORT_URL_ALEXTHEW,
        faceColor = MetroColors.AccentCobalt,
        useUserIcon = true,
    ),
    SupportTileSpec(
        titleRes = R.string.extras_support_cherryhoax,
        url = HubState.SUPPORT_URL_CHERRYHOAX,
        faceColor = MetroColors.AccentPurple,
        backgroundImageUrl = HubState.SUPPORT_AVATAR_CHERRYHOAX,
    ),
    SupportTileSpec(
        titleRes = R.string.extras_support_cyanexani,
        url = HubState.SUPPORT_URL_CYANEXANI,
        faceColor = MetroColors.AccentRed,
        useUserIcon = true,
    ),
)

/**
 * Start-menu–style 2×2 tiles. Enter order: (1,1) → (1,2) → (2,1) → (2,2),
 * each sliding in from the right with a short stagger.
 */
@Composable
private fun SupportTilesGrid(
    tiles: List<SupportTileSpec>,
    onOpen: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SupportTileGap)) {
        tiles.chunked(2).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(SupportTileGap)) {
                row.forEachIndexed { colIndex, tile ->
                    val enterIndex = rowIndex * 2 + colIndex
                    SupportTile(
                        title = stringResource(tile.titleRes),
                        faceColor = tile.faceColor,
                        backgroundImageUrl = tile.backgroundImageUrl,
                        useUserIcon = tile.useUserIcon,
                        titleColor = tile.titleColor,
                        enterIndex = enterIndex,
                        onClick = { onOpen(tile.url) },
                        modifier = Modifier.size(SupportTileSize),
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportTile(
    title: String,
    faceColor: Color,
    backgroundImageUrl: String?,
    useUserIcon: Boolean,
    titleColor: Color?,
    enterIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val slideStartPx = with(density) { SupportTileSlideStart.toPx() }
    val translationX = remember { Animatable(slideStartPx) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(enterIndex) {
        translationX.snapTo(slideStartPx)
        alpha.snapTo(0f)
        delay(enterIndex * SupportTileStaggerMs)
        alpha.snapTo(1f)
        translationX.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = SupportTileEnterMs,
                easing = SupportTileEnterEasing,
            ),
        )
    }

    val contentColor = titleColor ?: if (backgroundImageUrl != null) {
        MetroColors.TileContentOnAccent
    } else {
        MetroColors.tileContentColor(faceColor)
    }

    BoxWithConstraints(
        modifier = modifier
            .graphicsLayer {
                this.translationX = translationX.value
                this.alpha = alpha.value
            }
            .background(faceColor)
            .clickable(onClick = onClick)
            .semantics { contentDescription = title },
    ) {
        when {
            !backgroundImageUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(backgroundImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            useUserIcon -> {
                val iconSize = minOf(maxWidth, maxHeight) * 0.48f
                Image(
                    painter = painterResource(id = MetroAppGlyphs.People),
                    contentDescription = null,
                    modifier = Modifier
                        .size(iconSize)
                        .align(Alignment.Center),
                )
            }
        }

        BasicText(
            text = title,
            style = SupportTileTitleStyle.copy(
                fontFamily = MetroTheme.fontFamily,
                color = contentColor,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(SupportTileInset),
        )
    }
}

@Composable
private fun ComponentLine(
    text: String,
    color: Color,
) {
    BasicText(
        text = text,
        style = ComponentLineStyle.copy(fontFamily = MetroTheme.fontFamily, color = color),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

@Composable
private fun RubyInfoGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .border(width = 1.5.dp, color = color, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = "i",
            style = TextStyle(
                fontFamily = MetroTheme.fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                color = color,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}
