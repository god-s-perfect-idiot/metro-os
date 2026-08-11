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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import com.metro.photos.data.FullImageCache
import com.metro.photos.data.PhotoBitmapDecoder
import com.metro.photos.data.ThumbnailCache
import com.metro.ui.MetroLoadingDots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun PhotoThumbnail(
    uri: Uri,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    targetSizePx: Int = 0,
) {
    val context = LocalContext.current
    var measuredPx by remember { mutableIntStateOf(0) }
    val requestSize = remember(targetSizePx, measuredPx) {
        val side = if (targetSizePx > 0) targetSizePx else measuredPx
        if (side <= 0) 0 else PhotoBitmapDecoder.thumbnailBucket(side)
    }
    var bitmap by remember(uri, requestSize) {
        mutableStateOf(
            if (requestSize > 0) ThumbnailCache.get(uri, requestSize) else null,
        )
    }
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    LaunchedEffect(uri, requestSize) {
        if (requestSize <= 0) return@LaunchedEffect
        if (bitmap != null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(
                        uri,
                        Size(requestSize, requestSize),
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
            ThumbnailCache.put(uri, requestSize, loaded)
            bitmap = loaded
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { size ->
                measuredPx = max(size.width, size.height)
            },
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFF333333)),
                contentAlignment = Alignment.Center,
            ) {
                MetroLoadingDots()
            }
        }
    }
}

@Composable
fun PhotoFullImage(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val maxEdge = remember {
        val metrics = context.resources.displayMetrics
        PhotoBitmapDecoder.maxDecodeEdge(
            maxOf(metrics.widthPixels, metrics.heightPixels),
        )
    }
    var bitmap by remember(uri) {
        mutableStateOf<Bitmap?>(FullImageCache.get(uri))
    }
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    LaunchedEffect(uri, maxEdge) {
        FullImageCache.get(uri)?.let { cached ->
            bitmap = cached
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            PhotoBitmapDecoder.decodeFull(context.contentResolver, uri, maxEdge)
        }
        if (loaded != null) {
            FullImageCache.put(uri, loaded)
            bitmap = loaded
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
        Box(
            modifier = modifier.background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            MetroLoadingDots()
        }
    }
}
