package com.metro.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.metro.system.MetroAppBranding
import com.metro.ui.MetroColors
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle

/**
 * Renders the installed package's launcher icon (adaptive foreground unwrapped by
 * [MetroAppBranding]). Suite apps always use their own icon — never launcher `ic_system_*`
 * placeholder overrides.
 */
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

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (installedIcon != null) {
            Image(
                bitmap = installedIcon,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val glyph = fallbackLabel?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            MetroText(
                text = glyph,
                style = MetroTextStyle.ListItemTitle,
                color = fallbackColor,
            )
        }
    }
}
