package mihon.feature.support

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppGlyphs
import com.metro.ui.MetroColors
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.delay
import tachiyomi.core.common.Constants
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private val SupportTileGap = 8.dp
private val SupportTileSize = 132.dp
private val SupportTileInset = 8.dp
private val SupportTileSlideStart = 72.dp
private const val SupportTileEnterMs = 320
private const val SupportTileStaggerMs = 70L
private val SupportTileEnterEasing = CubicBezierEasing(0.3f, 1f, 0.2f, 1f)

private val SupportTileTitleStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 22.sp,
)

private data class SupportTileSpec(
    val title: String,
    val url: String,
    val faceColor: Color,
    val backgroundImageUrl: String? = null,
    val useUserIcon: Boolean = false,
    val titleColor: Color? = null,
)

private object MetronSupportLinks {
    const val BMC_ENTROPY = "https://buymeacoffee.com/godsperfectidiot"
    const val AVATAR_ENTROPY = "https://avatars.githubusercontent.com/u/33544311?v=4"
}

class SupportUsScreen : Screen() {

    @Composable
    override fun Content() {
        @Suppress("UNUSED_VARIABLE")
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current

        MetroSystemTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .metroNavBarPadding()
                    .background(MetroTheme.colors.background),
            ) {
                MetroSettingsHeader(
                    pageTitle = stringResource(MR.strings.label_support_us).lowercase(),
                    appTitle = "metron",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                        .padding(bottom = MetroAppBarDefaults.BarHeight + 32.dp),
                ) {
                    MetroText(
                        text = stringResource(MR.strings.supportUsScreen_perks),
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.primaryText,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    MetroText(
                        text = "support",
                        style = MetroTextStyle.SectionHeader,
                        color = MetroTheme.colors.secondaryText,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    SupportTilesGrid(
                        tiles = supportTiles(),
                        onOpen = { uriHandler.openUri(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun supportTiles(): List<SupportTileSpec> = listOf(
    SupportTileSpec(
        title = stringResource(MR.strings.supportUsScreen_donationPlatform_patreon),
        url = Constants.URL_DONATE_PATREON,
        faceColor = MetroColors.AccentOrange,
    ),
    SupportTileSpec(
        title = stringResource(MR.strings.supportUsScreen_donationPlatform_opencollective),
        url = Constants.URL_DONATE_OPENCOLLECTIVE,
        faceColor = MetroColors.AccentTeal,
    ),
    SupportTileSpec(
        title = "Entropy",
        url = MetronSupportLinks.BMC_ENTROPY,
        faceColor = MetroColors.AccentGreen,
        backgroundImageUrl = MetronSupportLinks.AVATAR_ENTROPY,
        titleColor = MetroColors.LightPrimaryText,
    ),
)

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
                        title = tile.title,
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
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
