package com.metro.dialer.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.dialer.R
import com.metro.dialer.data.ActiveCall
import com.metro.dialer.data.DialerCallLogic
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// Match the dialer keypad: light-gray tiles on dark-gray spacing.
private val InCallSectionBackground = Color(0xFF141414)
private val InCallTileBackground = Color(0xFF252525)
private val InCallTileDisabled = Color(0xFF5A5A5A)
private val InCallTileGap = 6.dp
private val InCallTileHeight = 78.dp

@Composable
fun InCallScreen(
    call: ActiveCall,
    onEndCall: () -> Unit,
    onConnected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onEndCall)

    val context = LocalContext.current
    var elapsedSeconds by remember(call.startedAtMillis) { mutableLongStateOf(0L) }
    var connected by remember(call.phoneNumber) { mutableStateOf(call.connected) }
    var photo by remember(call.phoneNumber) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(call.phoneNumber) {
        photo = loadContactPhoto(context, call.phoneNumber)
    }

    LaunchedEffect(call.phoneNumber) {
        if (!call.connected) {
            delay(1500)
            connected = true
            onConnected()
        }
    }

    LaunchedEffect(connected) {
        if (!connected) return@LaunchedEffect
        while (true) {
            delay(1000)
            elapsedSeconds = ((System.currentTimeMillis() - call.startedAtMillis) / 1000L)
                .coerceAtLeast(0L)
        }
    }

    val statusText = if (connected) {
        DialerCallLogic.formatDuration(elapsedSeconds.toInt())
    } else {
        stringResource(R.string.dialling)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val currentPhoto = photo
            if (currentPhoto != null) {
                Image(
                    bitmap = currentPhoto,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.55f),
                                0.3f to Color.Transparent,
                                0.75f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.75f),
                            ),
                        ),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MetroTheme.colors.accent.copy(alpha = 0.35f),
                                    Color(0xFF1A2A1A),
                                    Color.Black,
                                ),
                            ),
                        ),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetroText(
                        text = statusText,
                        style = MetroTextStyle.ListItemSubtitle,
                        color = Color.White,
                    )
                    MetroText(
                        text = stringResource(R.string.carrier_unknown),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                MetroText(
                    text = call.displayName,
                    style = MetroTextStyle.PageTitle,
                    color = Color.White,
                )

                MetroText(
                    text = stringResource(
                        R.string.mobile_label,
                        DialerCallLogic.formatDisplayNumber(call.phoneNumber),
                    ),
                    style = MetroTextStyle.ListItemTitle,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(InCallSectionBackground)
                .navigationBarsPadding()
                .padding(InCallTileGap),
            verticalArrangement = Arrangement.spacedBy(InCallTileGap),
        ) {
            InCallControlRow(
                controls = listOf(
                    InCallControl(stringResource(R.string.speaker), InCallIcon.Speaker, enabled = true),
                    InCallControl(stringResource(R.string.mute), InCallIcon.Mute, enabled = true),
                    InCallControl(stringResource(R.string.bluetooth), InCallIcon.Bluetooth, enabled = true),
                ),
            )
            InCallControlRow(
                controls = listOf(
                    InCallControl(stringResource(R.string.hold), InCallIcon.Hold, enabled = false),
                    InCallControl(stringResource(R.string.video), InCallIcon.Video, enabled = true),
                    InCallControl(stringResource(R.string.add_call), InCallIcon.AddCall, enabled = false),
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(InCallTileGap),
            ) {
                EndCallTile(
                    onEndCall = onEndCall,
                    modifier = Modifier.weight(2f),
                )
                InCallControlTile(
                    label = stringResource(R.string.keypad),
                    icon = InCallIcon.Keypad,
                    enabled = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private enum class InCallIcon {
    Speaker,
    Mute,
    Bluetooth,
    Hold,
    Video,
    AddCall,
    Keypad,
}

private data class InCallControl(
    val label: String,
    val icon: InCallIcon,
    val enabled: Boolean,
)

@Composable
private fun InCallControlRow(
    controls: List<InCallControl>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(InCallTileGap),
    ) {
        controls.forEach { control ->
            InCallControlTile(
                label = control.label,
                icon = control.icon,
                enabled = control.enabled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InCallControlTile(
    label: String,
    icon: InCallIcon,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (enabled) Color.White else InCallTileDisabled
    Box(
        modifier = modifier
            .height(InCallTileHeight)
            .background(InCallTileBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            InCallControlIcon(icon = icon, color = contentColor)
            MetroText(
                text = label,
                style = MetroTextStyle.ListItemSubtitle,
                color = contentColor,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun EndCallTile(
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val background = if (isPressed) {
        MetroTheme.colors.accent.copy(alpha = 0.75f)
    } else {
        MetroTheme.colors.accent
    }
    Box(
        modifier = modifier
            .height(InCallTileHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onEndCall,
            )
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        MetroText(
            text = stringResource(R.string.end_call),
            style = MetroTextStyle.ListItemTitle,
            color = Color.White,
        )
    }
}

@Composable
private fun InCallControlIcon(
    icon: InCallIcon,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = 30.dp, height = 26.dp)) {
        val w = size.width
        val h = size.height
        val sw = h * 0.085f
        val stroke = Stroke(width = sw)
        when (icon) {
            InCallIcon.Speaker -> {
                val cone = Path().apply {
                    moveTo(0.06f * w, 0.38f * h)
                    lineTo(0.20f * w, 0.38f * h)
                    lineTo(0.40f * w, 0.16f * h)
                    lineTo(0.40f * w, 0.84f * h)
                    lineTo(0.20f * w, 0.62f * h)
                    lineTo(0.06f * w, 0.62f * h)
                    close()
                }
                drawPath(cone, color)
                drawArc(
                    color = color,
                    startAngle = -55f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(0.40f * w, 0.22f * h),
                    size = Size(0.28f * w, 0.56f * h),
                    style = stroke,
                )
                drawArc(
                    color = color,
                    startAngle = -55f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(0.46f * w, 0.10f * h),
                    size = Size(0.46f * w, 0.80f * h),
                    style = stroke,
                )
            }
            InCallIcon.Mute -> {
                val micLeft = 0.39f * w
                val micRight = 0.61f * w
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(micLeft, 0.12f * h),
                    size = Size(micRight - micLeft, micRight - micLeft),
                    style = stroke,
                )
                drawLine(color, Offset(micLeft, 0.23f * h), Offset(micLeft, 0.46f * h), sw)
                drawLine(color, Offset(micRight, 0.23f * h), Offset(micRight, 0.46f * h), sw)
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(0.30f * w, 0.34f * h),
                    size = Size(0.40f * w, 0.40f * h),
                    style = stroke,
                )
                drawLine(color, Offset(0.5f * w, 0.74f * h), Offset(0.5f * w, 0.88f * h), sw)
                drawLine(color, Offset(0.36f * w, 0.88f * h), Offset(0.64f * w, 0.88f * h), sw)
                drawLine(color, Offset(0.12f * w, 0.08f * h), Offset(0.88f * w, 0.92f * h), sw)
            }
            InCallIcon.Bluetooth -> {
                val path = Path().apply {
                    moveTo(0.26f * w, 0.30f * h)
                    lineTo(0.74f * w, 0.70f * h)
                    lineTo(0.50f * w, 0.92f * h)
                    lineTo(0.50f * w, 0.08f * h)
                    lineTo(0.74f * w, 0.30f * h)
                    lineTo(0.26f * w, 0.70f * h)
                }
                drawPath(path, color, style = stroke)
            }
            InCallIcon.Hold -> {
                drawRect(
                    color = color,
                    topLeft = Offset(0.34f * w, 0.20f * h),
                    size = Size(0.12f * w, 0.60f * h),
                )
                drawRect(
                    color = color,
                    topLeft = Offset(0.54f * w, 0.20f * h),
                    size = Size(0.12f * w, 0.60f * h),
                )
            }
            InCallIcon.Video -> {
                drawRect(
                    color = color,
                    topLeft = Offset(0.12f * w, 0.32f * h),
                    size = Size(0.46f * w, 0.36f * h),
                )
                val lens = Path().apply {
                    moveTo(0.60f * w, 0.44f * h)
                    lineTo(0.86f * w, 0.30f * h)
                    lineTo(0.86f * w, 0.70f * h)
                    lineTo(0.60f * w, 0.56f * h)
                    close()
                }
                drawPath(lens, color)
            }
            InCallIcon.AddCall -> {
                val plus = sw * 1.4f
                drawLine(color, Offset(0.5f * w, 0.22f * h), Offset(0.5f * w, 0.78f * h), plus)
                drawLine(color, Offset(0.22f * w, 0.5f * h), Offset(0.78f * w, 0.5f * h), plus)
            }
            InCallIcon.Keypad -> {
                val xs = listOf(0.28f, 0.5f, 0.72f)
                val ys = listOf(0.22f, 0.5f, 0.78f)
                val radius = h * 0.07f
                ys.forEach { fy ->
                    xs.forEach { fx ->
                        drawCircle(color, radius, Offset(fx * w, fy * h))
                    }
                }
            }
        }
    }
}

private suspend fun loadContactPhoto(context: Context, number: String): ImageBitmap? {
    if (number.isBlank()) return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number),
            )
            val photoUriString = context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup.PHOTO_URI),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: return@runCatching null
            context.contentResolver.openInputStream(Uri.parse(photoUriString))?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
}
