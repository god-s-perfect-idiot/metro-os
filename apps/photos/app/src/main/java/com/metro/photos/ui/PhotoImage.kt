package com.metro.photos.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.metro.photos.data.ThumbnailCache
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
    var bitmap by remember(uri, targetSizePx) {
        mutableStateOf(ThumbnailCache.get(uri, targetSizePx))
    }
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    LaunchedEffect(uri, targetSizePx) {
        if (bitmap != null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
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
        if (loaded != null) {
            ThumbnailCache.put(uri, targetSizePx, loaded)
            bitmap = loaded
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier.background(Color(0xFF333333)))
    }
}

@Composable
fun PhotoFullImage(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

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

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier.background(Color.Black))
    }
}
