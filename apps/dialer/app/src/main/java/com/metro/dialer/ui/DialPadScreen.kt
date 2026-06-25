package com.metro.dialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.dialer.R
import com.metro.dialer.data.CallDirection
import com.metro.dialer.data.CallGroup
import com.metro.dialer.data.DialerCallLogic
import com.metro.ui.MetroColors
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import kotlinx.coroutines.delay

/** WP8.1 dial-pad tile accent flash — hold at least 150ms so quick taps stay visible. */
private const val DialKeyPressFlashMs = 150L

private val DialKeyDigitStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 36.sp,
    lineHeight = 36.sp,
)

private val DialKeyGap = 6.dp

private val DialKeyHintStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 14.sp,
)

private val DialNumberStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Light,
    fontSize = 42.sp,
    lineHeight = 46.sp,
)

private val DialKeyBackground = Color(0xFF252525)
private val DialKeypadSectionBackground = Color(0xFF141414)
private val DialKeyHeight = 64.dp

@Composable
fun CallDetailScreen(
    group: CallGroup,
    onBack: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 16.dp),
        ) {
            MetroText(
                text = group.displayName.uppercase(),
                style = MetroTextStyle.SectionHeader,
            )
            if (group.displayName != DialerCallLogic.formatDisplayNumber(group.phoneNumber)) {
                MetroText(
                    text = DialerCallLogic.formatDisplayNumber(group.phoneNumber),
                    style = MetroTextStyle.ListItemTitle,
                    color = MetroTheme.colors.accent,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            group.calls.forEach { call ->
                val directionLabel = when (call.type) {
                    CallDirection.Incoming -> stringResource(R.string.incoming)
                    CallDirection.Outgoing -> stringResource(R.string.outgoing)
                    CallDirection.Missed -> stringResource(R.string.missed)
                }
                val durationLabel = when {
                    call.type == CallDirection.Missed -> stringResource(R.string.missed)
                    call.durationSeconds > 0 -> DialerCallLogic.formatDuration(call.durationSeconds)
                    else -> stringResource(R.string.declined)
                }
                val titleColor = when (call.type) {
                    CallDirection.Missed -> MetroColors.AccentRed
                    else -> MetroTheme.colors.primaryText
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    MetroText(
                        text = directionLabel,
                        style = MetroTextStyle.ListItemTitle,
                        color = titleColor,
                    )
                    MetroText(
                        text = DialerCallLogic.formatTimestamp(call.timestamp),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.secondaryText,
                    )
                    MetroText(
                        text = durationLabel,
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
                ListDivider()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCall,
                )
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhoneCallIcon()
            MetroText(
                text = stringResource(R.string.call),
                style = MetroTextStyle.ListItemTitle,
                color = MetroTheme.colors.accent,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
fun DialPadPane(
    suggestions: List<com.metro.dialer.data.ContactSuggestion>,
    onAppend: (Char) -> Unit,
    onLongPressZero: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onCall: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (suggestions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    suggestions.forEach { suggestion ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSuggestionClick(suggestion.phoneNumber) }
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                        ) {
                            MetroText(text = suggestion.displayName, style = MetroTextStyle.ListItemTitle)
                            MetroText(
                                text = suggestion.phoneNumber,
                                style = MetroTextStyle.ListItemSubtitle,
                                color = MetroTheme.colors.secondaryText,
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DialKeypadSectionBackground)
                .padding(horizontal = 6.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(DialKeyGap),
        ) {
            KeypadRow(listOf("1" to "", "2" to "ABC", "3" to "DEF"), onAppend)
            KeypadRow(listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"), onAppend)
            KeypadRow(listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"), onAppend)
            KeypadRow(
                keys = listOf("*" to "", "0" to "+", "#" to ""),
                onAppend = onAppend,
                onLongPress = mapOf("0" to onLongPressZero),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DialKeyGap),
            ) {
                CallActionKey(
                    label = stringResource(R.string.call),
                    modifier = Modifier.weight(2f),
                    onClick = onCall,
                )
                SaveActionKey(
                    label = stringResource(R.string.save),
                    modifier = Modifier.weight(1f),
                    onClick = onSave,
                )
            }
        }
    }
}

@Composable
internal fun DialNumberField(
    dialString: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = dialString,
            modifier = Modifier.weight(1f),
            style = DialNumberStyle.copy(color = MetroTheme.colors.primaryText),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (dialString.isNotEmpty()) {
            BackspaceIcon(
                onClick = onDelete,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun BackspaceIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MetroTheme.colors.primaryText,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .semantics {
                role = Role.Button
                contentDescription = "backspace"
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(width = 32.dp, height = 22.dp)) {
            val strokeWidth = size.minDimension * 0.08f
            val stroke = Stroke(width = strokeWidth)
            val tipX = size.width * 0.08f
            val bodyLeft = size.width * 0.32f
            val outline = Path().apply {
                moveTo(bodyLeft, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(bodyLeft, size.height)
                lineTo(tipX, size.height * 0.5f)
                close()
            }
            drawPath(outline, color, style = stroke)

            val inset = size.width * 0.12f
            val xLeft = bodyLeft + inset
            val xRight = size.width - inset
            val xTop = size.height * 0.28f
            val xBottom = size.height * 0.72f
            drawLine(color, Offset(xLeft, xTop), Offset(xRight, xBottom), strokeWidth)
            drawLine(color, Offset(xRight, xTop), Offset(xLeft, xBottom), strokeWidth)
        }
    }
}

@Composable
private fun KeypadRow(
    keys: List<Pair<String, String>>,
    onAppend: (Char) -> Unit,
    onLongPress: Map<String, () -> Unit> = emptyMap(),
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DialKeyGap),
    ) {
        keys.forEach { (digit, hint) ->
            DialKey(
                digit = digit,
                hint = hint,
                modifier = Modifier.weight(1f),
                onClick = { onAppend(digit.first()) },
                onLongClick = onLongPress[digit],
            )
        }
    }
}

@Composable
private fun rememberDialKeyPressed(
    interactionSource: MutableInteractionSource,
    minimumDurationMs: Long = DialKeyPressFlashMs,
): Boolean {
    val isPhysicallyPressed by interactionSource.collectIsPressedAsState()
    var holdPressed by remember { mutableStateOf(false) }
    var pressStartedAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPhysicallyPressed) {
        if (isPhysicallyPressed) {
            pressStartedAt = System.currentTimeMillis()
            holdPressed = true
        } else if (holdPressed) {
            val remaining = minimumDurationMs - (System.currentTimeMillis() - pressStartedAt)
            if (remaining > 0) delay(remaining)
            holdPressed = false
        }
    }

    return isPhysicallyPressed || holdPressed
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKey(
    digit: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = rememberDialKeyPressed(interactionSource)
    val background = if (isPressed) MetroTheme.colors.accent else DialKeyBackground
    val digitColor = if (isPressed) Color.White else MetroTheme.colors.primaryText
    val hintColor = if (isPressed) Color.White.copy(alpha = 0.85f) else MetroTheme.colors.secondaryText
    val centered = digit == "*" || digit == "#"
    val touchModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DialKeyHeight)
            .semantics { role = Role.Button }
            .then(touchModifier)
            .background(background),
        contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart,
    ) {
        if (centered) {
            BasicText(
                text = digit,
                style = DialKeyDigitStyle.copy(color = digitColor),
            )
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = digit,
                    style = DialKeyDigitStyle.copy(color = digitColor),
                )
                if (hint.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    BasicText(
                        text = hint,
                        style = DialKeyHintStyle.copy(color = hintColor),
                    )
                }
            }
        }
    }
}

@Composable
private fun CallActionKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = rememberDialKeyPressed(interactionSource)
    val background = if (isPressed) {
        MetroTheme.colors.accent.copy(alpha = 0.75f)
    } else {
        MetroTheme.colors.accent
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DialKeyHeight)
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        MetroText(
            text = label,
            style = MetroTextStyle.ListItemSubtitle,
            color = Color.White,
        )
    }
}

@Composable
private fun SaveActionKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = rememberDialKeyPressed(interactionSource)
    val background = if (isPressed) MetroTheme.colors.accent else DialKeyBackground
    val contentColor = if (isPressed) Color.White else MetroTheme.colors.primaryText
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DialKeyHeight)
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SaveFloppyIcon(color = contentColor)
            MetroText(
                text = label,
                style = MetroTextStyle.ListItemSubtitle,
                color = contentColor,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SaveFloppyIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    Canvas(modifier = modifier.size(width = 22.dp, height = 22.dp)) {
        val strokeWidth = size.minDimension * 0.08f
        val stroke = Stroke(width = strokeWidth)
        val bodyLeft = size.width * 0.12f
        val bodyRight = size.width * 0.88f
        val bodyTop = size.height * 0.28f
        val bodyBottom = size.height * 0.9f
        drawRect(color, topLeft = Offset(bodyLeft, bodyTop), size = androidx.compose.ui.geometry.Size(bodyRight - bodyLeft, bodyBottom - bodyTop), style = stroke)

        val slotLeft = size.width * 0.22f
        val slotRight = size.width * 0.78f
        val slotTop = size.height * 0.12f
        val slotBottom = size.height * 0.34f
        drawRect(color, topLeft = Offset(slotLeft, slotTop), size = androidx.compose.ui.geometry.Size(slotRight - slotLeft, slotBottom - slotTop), style = stroke)

        val lineY = size.height * 0.58f
        drawLine(color, Offset(bodyLeft + strokeWidth, lineY), Offset(bodyRight - strokeWidth, lineY), strokeWidth)
    }
}
