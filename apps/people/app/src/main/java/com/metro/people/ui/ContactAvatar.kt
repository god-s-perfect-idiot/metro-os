package com.metro.people.ui

import android.graphics.BitmapFactory
import android.provider.ContactsContract
import android.content.ContentUris
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.metro.ui.MetroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ContactAvatar(
    contactId: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var photo by remember(contactId) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(contactId) {
        photo = withContext(Dispatchers.IO) {
            val contactUri = ContentUris.withAppendedId(
                ContactsContract.Contacts.CONTENT_URI,
                contactId,
            )
            ContactsContract.Contacts.openContactPhotoInputStream(
                context.contentResolver,
                contactUri,
                true,
            )?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }
    }

    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Box(
        modifier = modifier
            .background(MetroTheme.colors.secondarySurface)
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
    }
}
