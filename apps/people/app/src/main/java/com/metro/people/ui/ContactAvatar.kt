package com.metro.people.ui

import android.content.ContentResolver
import android.content.ContentUris
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object ContactPhotoCache {
    private val cache = object : LruCache<String, ImageBitmap>(64) {}

    fun get(key: String): ImageBitmap? = synchronized(cache) { cache.get(key) }

    fun put(key: String, bitmap: ImageBitmap) {
        synchronized(cache) { cache.put(key, bitmap) }
    }

    fun key(contactId: Long, photoUri: String?): String =
        photoUri?.takeIf { it.isNotBlank() } ?: "id:$contactId"
}

@Composable
fun ContactAvatar(
    contactId: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    /** Prefer this ContactsContract photo URI when non-null (faster than id-only lookup). */
    photoUri: String? = null,
    /** When set and no photo loads, fill with accent and draw this name (recent tiles). */
    fallbackLabel: String? = null,
) {
    val context = LocalContext.current
    val cacheKey = ContactPhotoCache.key(contactId, photoUri)
    var photo by remember(cacheKey) {
        mutableStateOf(ContactPhotoCache.get(cacheKey))
    }
    var loaded by remember(cacheKey) { mutableStateOf(photo != null) }

    LaunchedEffect(cacheKey) {
        ContactPhotoCache.get(cacheKey)?.let {
            photo = it
            loaded = true
            return@LaunchedEffect
        }
        loaded = false
        val bitmap = withContext(Dispatchers.IO) {
            loadContactPhotoBitmap(context.contentResolver, contactId, photoUri)
        }
        if (bitmap != null) {
            ContactPhotoCache.put(cacheKey, bitmap)
        }
        photo = bitmap
        loaded = true
    }

    val clickModifier = if (onClick != null) Modifier.metroClickable(onClick = onClick) else Modifier
    val showFallback = loaded && photo == null && !fallbackLabel.isNullOrBlank()
    Box(
        modifier = modifier
            .background(
                if (showFallback) MetroTheme.colors.accent else MetroTheme.colors.secondarySurface,
            )
            .then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        photo?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        if (showFallback) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                MetroText(
                    text = fallbackLabel.orEmpty(),
                    style = MetroTextStyle.ListItemSubtitle,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal fun loadContactPhotoBitmap(
    resolver: ContentResolver,
    contactId: Long,
    photoUri: String?,
): ImageBitmap? {
    // 1) Explicit PHOTO_URI / thumbnail URI from the contact row.
    decodeUri(resolver, photoUri)?.let { return it }

    if (contactId <= 0L) return null

    // 2) Thumbnail URI for list density.
    val thumbUri = ContentUris.withAppendedId(
        ContactsContract.Contacts.CONTENT_URI,
        contactId,
    ).buildUpon()
        .appendPath(ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
        .build()
    decodeUri(resolver, thumbUri.toString())?.let { return it }

    // 3) Display photo stream (hi-res then thumbnail).
    val contactUri = ContentUris.withAppendedId(
        ContactsContract.Contacts.CONTENT_URI,
        contactId,
    )
    openPhotoStream(resolver, contactUri, preferHighres = false)?.let { return it }
    return openPhotoStream(resolver, contactUri, preferHighres = true)
}

private fun decodeUri(resolver: ContentResolver, uriString: String?): ImageBitmap? {
    if (uriString.isNullOrBlank()) return null
    return runCatching {
        resolver.openInputStream(Uri.parse(uriString))?.use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull()
}

private fun openPhotoStream(
    resolver: ContentResolver,
    contactUri: Uri,
    preferHighres: Boolean,
): ImageBitmap? {
    return runCatching {
        ContactsContract.Contacts.openContactPhotoInputStream(
            resolver,
            contactUri,
            preferHighres,
        )?.use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull()
}
