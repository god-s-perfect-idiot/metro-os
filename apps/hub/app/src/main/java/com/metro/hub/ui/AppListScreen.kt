package com.metro.hub.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.metro.hub.R
import com.metro.hub.data.HubLogoDecoder
import com.metro.hub.data.ReleaseApkAsset
import com.metro.system.MetroPreferences
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppTitle
import com.metro.ui.MetroColors
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroLoadingScreen
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import java.io.File

internal val StoreIconSize = 72.dp
/** Larger catalog tiles for the panorama featured pane. */
internal val FeaturedStoreIconSize = 108.dp
internal val StoreIconGlyphScale = 0.72f
private val StoreRowSpacing = 16.dp
/** Extra space under the last row so it clears the overlay app bar with room to breathe. */
internal val ListBottomExtraPadding = 64.dp

internal val StoreMetaStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 18.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Composable
fun AppListScreen(
    state: HubState,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observe = generation

    Column(modifier = modifier.fillMaxSize()) {
        MetroAppTitle(title = stringResource(R.string.app_name))
        MetroText(
            text = state.listTitle,
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp),
        )

        val tag = state.release?.tagName
        if (tag != null) {
            MetroText(
                text = tag,
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        when {
            state.catalogLoadMode == CatalogLoadMode.Loading && state.visibleAssets.isEmpty() -> {
                MetroLoadingScreen(
                    message = stringResource(R.string.apps_loading),
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = MetroAppBarDefaults.BarHeight),
                )
            }
            state.releaseError != null && state.visibleAssets.isEmpty() -> {
                MetroText(
                    text = state.releaseError ?: stringResource(R.string.apps_error),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(12.dp),
                )
            }
            state.visibleAssets.isEmpty() -> {
                MetroText(
                    text = stringResource(R.string.apps_empty),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(12.dp),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(StoreRowSpacing),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        // Clear the overlay app bar so the last row’s text isn’t cut off.
                        bottom = MetroAppBarDefaults.BarHeight + ListBottomExtraPadding,
                    ),
                ) {
                    items(state.visibleAssets, key = { it.name }) { asset ->
                        StoreAppRow(
                            asset = asset,
                            iconPath = state.iconPathFor(asset),
                            onVisible = { state.ensureIcon(asset) },
                            onClick = { state.openAppDetail(asset) },
                        )
                    }
                }
            }
        }

        state.downloadError?.let { error ->
            MetroText(
                text = error,
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
internal fun StoreAppRow(
    asset: ReleaseApkAsset,
    iconPath: String?,
    onVisible: () -> Unit,
    onClick: () -> Unit,
    iconSize: Dp = StoreIconSize,
) {
    LaunchedEffect(asset.name, asset.sizeBytes, asset.iconUrl) {
        onVisible()
    }

    val secondary = MetroTheme.colors.secondaryText
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StoreAppIcon(
            title = asset.displayName,
            packageName = asset.packageName,
            iconUrl = asset.iconUrl,
            iconPath = iconPath,
            logoXml = asset.logoXml,
            logoPngBase64 = asset.logoPngBase64,
            backgroundColorHex = asset.backgroundColor,
            glyphResId = asset.glyphResId,
            iconSize = iconSize,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            MetroText(
                text = asset.displayName,
                style = MetroTextStyle.ListItemTitle,
                maxLines = 1,
            )
            BasicText(
                text = asset.description,
                style = StoreMetaStyle.copy(fontFamily = MetroTheme.fontFamily, color = secondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = "By: ${asset.publisher}",
                style = StoreMetaStyle.copy(fontFamily = MetroTheme.fontFamily, color = secondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun StoreAppIcon(
    title: String,
    packageName: String,
    iconUrl: String?,
    iconPath: String?,
    logoXml: String?,
    logoPngBase64: String?,
    backgroundColorHex: String?,
    glyphResId: Int?,
    iconSize: Dp = StoreIconSize,
) {
    val context = LocalContext.current
    val hasMetroTileBg = !backgroundColorHex.isNullOrBlank()
    val background = remember(backgroundColorHex) {
        backgroundColorHex
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { MetroPreferences.parseAccentHex(it) }.getOrNull() }
    } ?: MetroTheme.colors.accent
    val content = MetroColors.tileContentColor(background)
    val firestorePng = remember(logoPngBase64) {
        logoPngBase64?.let { HubLogoDecoder.bitmapFromPngBase64(it) }
    }
    val firestoreVector = remember(logoXml) {
        logoXml?.let { xml -> HubLogoDecoder.bitmapFromLogoXml(context, xml) }
    }
    val letterSp = (42f * (iconSize / StoreIconSize)).sp
    Box(
        modifier = Modifier
            .size(iconSize)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        when {
            firestorePng != null -> {
                Image(
                    painter = BitmapPainter(firestorePng.asImageBitmap()),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(iconSize * 0.12f),
                )
            }
            firestoreVector != null -> {
                Image(
                    painter = BitmapPainter(firestoreVector.asImageBitmap()),
                    contentDescription = title,
                    colorFilter = ColorFilter.tint(content),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(iconSize * StoreIconGlyphScale),
                )
            }
            // Prefer flat Metro glyph/letter on the catalog tile color — never nest a
            // rounded Android adaptive launcher icon inside the square.
            hasMetroTileBg || glyphResId != null -> {
                if (glyphResId != null) {
                    val entryName = runCatching {
                        context.resources.getResourceEntryName(glyphResId)
                    }.getOrNull().orEmpty()
                    val tintVectors = !entryName.contains("people", ignoreCase = true)
                    Image(
                        painter = painterResource(glyphResId),
                        contentDescription = title,
                        colorFilter = if (tintVectors) ColorFilter.tint(content) else null,
                        modifier = Modifier.size(iconSize * StoreIconGlyphScale),
                    )
                } else {
                    StoreAppLetter(title = title, color = content, fontSize = letterSp)
                }
            }
            !iconUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(iconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            !iconPath.isNullOrBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(iconPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                val installed = runCatching {
                    context.packageManager.getApplicationIcon(packageName)
                }.getOrNull()
                if (installed != null) {
                    val bitmap = installed.toBitmap(
                        width = 192,
                        height = 192,
                    )
                    Image(
                        painter = BitmapPainter(bitmap.asImageBitmap()),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    StoreAppLetter(title = title, color = content, fontSize = letterSp)
                }
            }
        }
    }
}

@Composable
private fun StoreAppLetter(
    title: String,
    color: Color,
    fontSize: TextUnit = 42.sp,
) {
    BasicText(
        text = title.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
        style = TextStyle(
            fontFamily = MetroTheme.fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = fontSize,
            color = color,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
    )
}
