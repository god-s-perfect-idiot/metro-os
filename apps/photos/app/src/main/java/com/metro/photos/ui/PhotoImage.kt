package com.metro.photos.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PhotoThumbnail(
    uri: Uri,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    targetSizePx: Int = 256,
) {
    val context = LocalContext.current
    var bitmap by remember(uri, targetSizePx) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri, targetSizePx) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(
                        uri,
                        Size(targetSizePx, targetSizePx),
                        null,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Thumbnails.getThumbnail(
                        context.contentResolver,
                        android.content.ContentUris.parseId(uri),
                        android.provider.MediaStore.Images.Thumbnails.MINI_KIND,
                        null,
                    )
                }
            }.getOrNull()
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier,
        )
    } else {
        androidx.compose.foundation.layout.Box(
            modifier = modifier.background(Color(0xFF333333)),
        )
    }
}

@Composable
fun PhotoFullImage(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(
                        uri,
                        Size(2048, 2048),
                        null,
                    )
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        android.graphics.BitmapFactory.decodeStream(stream)
                    }
                }
            }.getOrNull()
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    } else {
        androidx.compose.foundation.layout.Box(
            modifier = modifier.background(Color.Black),
        )
    }
}
