package com.metro.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.metro.launcher.data.SystemAppPlaceholders
import com.metro.system.MetroAppBranding
import com.metro.ui.MetroColors
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle

private val PlaceholderIconInsetRatio = 0.12f

@Composable
fun MetroAppIcon(
    packageName: String,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    fallbackLabel: String? = null,
    fallbackColor: Color = MetroColors.DarkPrimaryText,
) {
    val context = LocalContext.current
    val pixelSize = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
    val installedIcon = remember(packageName, pixelSize) {
        MetroAppBranding.loadAppIcon(context, packageName)?.toBitmap(pixelSize, pixelSize)?.asImageBitmap()
    }
    val placeholderResId = remember(packageName) { SystemAppPlaceholders.iconResId(packageName) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            installedIcon != null -> {
                Image(
                    bitmap = installedIcon,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            placeholderResId != null -> {
                Image(
                    painter = painterResource(placeholderResId),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(size * PlaceholderIconInsetRatio),
                )
            }
            else -> {
                val glyph = fallbackLabel?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                MetroText(
                    text = glyph,
                    style = MetroTextStyle.ListItemTitle,
                    color = fallbackColor,
                )
            }
        }
    }
}
