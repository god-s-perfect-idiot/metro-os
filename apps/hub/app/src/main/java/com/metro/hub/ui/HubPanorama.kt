package com.metro.hub.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.hub.R
import com.metro.hub.data.HubAppCategory
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroColors
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroPanorama
import com.metro.ui.MetroPanoramaBodyEnter
import com.metro.ui.MetroPanoramaBrandEnter
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import kotlin.math.roundToInt

private const val HubBrandText = "hub"
private val HubBrandInset = 12.dp
private val QuickLinkTileInset = 8.dp
private const val QuickLinkTileWidthScale = 0.88f

private val HubBrandStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 96.sp,
    lineHeight = 96.sp,
    letterSpacing = (-1).sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/** Slightly under toolkit HubLink (38sp) for the about-app menu density. */
private val HubMenuLinkStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Light,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Composable
fun HubPanorama(
    state: HubState,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    skipIntro: Boolean = false,
    onIntroPlayed: () -> Unit = {},
) {
    val density = LocalDensity.current

    // Remember after this hub visit so in-app return skips the intro (not mid-animation).
    DisposableEffect(Unit) {
        onDispose { onIntroPlayed() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        MetroPanoramaBrandEnter(skipEnter = skipIntro) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(bottom = 4.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                val measurer = rememberTextMeasurer()
                val availableWidthPx = with(density) { (maxWidth - HubBrandInset).toPx() }
                val fontFamily = MetroTheme.fontFamily
                val brandWidthPx = remember(measurer, density, fontFamily) {
                    measurer.measure(
                        text = HubBrandText,
                        style = HubBrandStyle.copy(fontFamily = fontFamily),
                        softWrap = false,
                        maxLines = 1,
                        density = density,
                    ).size.width.toFloat()
                }
                val hiddenPx = (brandWidthPx - availableWidthPx).coerceAtLeast(0f)
                val lastPage = (pagerState.pageCount - 1).coerceAtLeast(1)
                val progress = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    .coerceIn(0f, lastPage.toFloat()) / lastPage
                val brandOffsetPx = (progress * hiddenPx).roundToInt()

                BasicText(
                    text = HubBrandText,
                    style = HubBrandStyle.copy(fontFamily = MetroTheme.fontFamily, color = MetroTheme.colors.primaryText),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .offset { IntOffset(-brandOffsetPx, 0) }
                        .padding(start = HubBrandInset)
                        .wrapContentWidth(align = Alignment.Start, unbounded = true),
                )
            }
        }

        MetroPanoramaBodyEnter(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            skipEnter = skipIntro,
        ) {
            MetroPanorama(
                // Brand-only hub — no pane HubTitles (`home` / `apps` / `featured`).
                titles = listOf("", "", ""),
                pagerState = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = MetroAppBarDefaults.BarHeight),
                pageContent = { page ->
                    when (page) {
                        HubState.HUB_HOME -> HomePane(state = state)
                        HubState.HUB_APPS -> QuickLinksPane(state = state)
                        else -> FeaturedAppsPane(state = state)
                    }
                },
            )
        }
    }
}

@Composable
private fun HomePane(state: HubState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp),
    ) {
        val metroOsAppsTitle = stringResource(R.string.link_metro_os_apps)
        HubLinkRow(
            title = metroOsAppsTitle,
            onClick = { state.openAllApps(metroOsAppsTitle) },
        )
        val relatedAppsTitle = stringResource(R.string.link_related_apps)
        HubLinkRow(
            title = relatedAppsTitle,
            onClick = {
                state.openCategory(HubAppCategory.SecondParty, title = relatedAppsTitle)
            },
        )
        val unofficialTitle = stringResource(R.string.link_unofficial_metro_apps)
        HubLinkRow(
            title = unofficialTitle,
            onClick = {
                state.openCategory(HubAppCategory.ThirdParty, title = unofficialTitle)
            },
        )
        HubLinkRow(
            title = stringResource(R.string.link_get_started_with_os),
            enabled = false,
            onClick = null,
        )
        HubLinkRow(
            title = stringResource(R.string.link_metro_os_github),
            onClick = { state.openExternalUrl(HubState.GITHUB_URL) },
        )
        HubLinkRow(
            title = stringResource(R.string.link_extras_info),
            onClick = state::openExtrasInfo,
        )
    }
}

@Composable
private fun HubLinkRow(
    title: String,
    enabled: Boolean = true,
    onClick: (() -> Unit)?,
) {
    val color = if (enabled) {
        MetroTheme.colors.primaryText
    } else {
        MetroTheme.colors.secondaryText
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(
            text = title,
            style = HubMenuLinkStyle.copy(fontFamily = MetroTheme.fontFamily, color = color),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun QuickLinksPane(state: HubState) {
    val tiles = listOf(
        HubAppCategory.Core,
        HubAppCategory.Shell,
        HubAppCategory.SecondParty,
        HubAppCategory.ThirdParty,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(top = 24.dp),
    ) {
        MetroText(
            text = stringResource(R.string.quick_links).uppercase(),
            style = MetroTextStyle.SectionHeader,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tileSize = ((maxWidth - 8.dp) / 2) * QuickLinkTileWidthScale
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tiles.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { category ->
                            QuickLinkTile(
                                title = category.label,
                                onClick = { state.openCategory(category) },
                                modifier = Modifier.size(tileSize),
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.size(tileSize))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedAppsPane(state: HubState) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observe = generation

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
    ) {
        MetroText(
            text = stringResource(R.string.featured_apps).uppercase(),
            style = MetroTextStyle.SectionHeader,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
        )
        when {
            state.catalogLoadMode == CatalogLoadMode.Loading && state.featuredAssets.isEmpty() -> {
                MetroLoadingScreen(
                    message = stringResource(R.string.apps_loading),
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = MetroAppBarDefaults.BarHeight),
                )
            }
            state.featuredAssets.isEmpty() -> {
                MetroText(
                    text = stringResource(R.string.apps_empty),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    state.featuredAssets.forEach { asset ->
                        StoreAppRow(
                            asset = asset,
                            iconPath = state.iconPathFor(asset),
                            onVisible = { state.ensureIcon(asset) },
                            onClick = { state.openAppDetail(asset) },
                            iconSize = FeaturedStoreIconSize,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickLinkTile(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MetroTheme.colors.accent
    val content = MetroColors.tileContentColor(background)
    Box(
        modifier = modifier
            .background(background)
            .clickable(onClick = onClick)
            .semantics { contentDescription = title }
            .padding(QuickLinkTileInset),
    ) {
        MetroText(
            text = title,
            style = MetroTextStyle.ListItemTitle,
            color = content,
            maxLines = 2,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}
