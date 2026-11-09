package com.metro.messaging.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.metro.messaging.data.MmsImageDecoder
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MmsBubbleImage(
    uri: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberDecodedMmsBitmap(uri, maxEdgePx = 720)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 280.dp)
            .background(Color.Black.copy(alpha = 0.25f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpen,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "mms image",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            MetroText(
                text = "image",
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
fun MmsImageViewerScreen(
    uri: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val bitmap = rememberDecodedMmsBitmap(uri, maxEdgePx = 2048)
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onBack,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "mms image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            )
        } else {
            MetroText(
                text = "Unable to open image",
                style = MetroTextStyle.Body,
                color = MetroTheme.colors.secondaryText,
            )
        }
    }
}

@Composable
private fun rememberDecodedMmsBitmap(uri: String, maxEdgePx: Int): Bitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri, maxEdgePx) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri, maxEdgePx) {
        bitmap = withContext(Dispatchers.IO) {
            MmsImageDecoder.decode(context, uri, maxEdgePx)
        }
    }
    return bitmap
}
