package com.metro.lockscreen

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Matches Settings Start background preview square. */
private val LockBackgroundThumbSize = 108.dp

private val LockBackgroundPlaceholderGray = Color(0xFF6E6E6E)

/**
 * WP8.1-style custom lock background block: square thumb | label + choose photo (+ remove).
 * Same layout language as Settings start+theme Start background.
 */
@Composable
fun LockscreenCustomBackgroundRow(
    enabled: Boolean,
    reloadEpoch: Int,
    onChoosePhoto: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var thumbBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(enabled, reloadEpoch) {
        thumbBitmap = if (enabled) {
            withContext(Dispatchers.IO) { LockscreenCustomBackground.decode(context) }
        } else {
            null
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(LockBackgroundThumbSize)
                .background(LockBackgroundPlaceholderGray),
        ) {
            thumbBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = stringResource(R.string.lock_background_label),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f, fill = false)) {
            MetroText(
                text = stringResource(R.string.lock_background_label),
                style = MetroTextStyle.Body,
            )
            Spacer(modifier = Modifier.size(10.dp))
            MetroBorderButton(
                text = stringResource(R.string.lock_background_choose),
                onClick = onChoosePhoto,
            )
            if (enabled) {
                Spacer(modifier = Modifier.size(12.dp))
                val removeLabel = stringResource(R.string.lock_background_remove)
                MetroText(
                    text = remember(removeLabel) {
                        buildAnnotatedString {
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append(removeLabel)
                            }
                        }
                    },
                    style = MetroTextStyle.Body,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onRemove,
                        )
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
fun LockscreenCustomBackgroundPicker(
    customEnabled: Boolean,
    reloadEpoch: Int,
    onBeginCrop: (Uri) -> Unit,
    onCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onBeginCrop(uri)
    }

    LockscreenCustomBackgroundRow(
        enabled = customEnabled,
        reloadEpoch = reloadEpoch,
        onChoosePhoto = {
            pickPhoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onRemove = {
            LockscreenCustomBackground.clear(context)
            onCleared()
        },
        modifier = modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
    )
}
