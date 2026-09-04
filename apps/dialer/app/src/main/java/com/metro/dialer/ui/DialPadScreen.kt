package com.metro.dialer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.dialer.R
import com.metro.dialer.data.CallDirection
import com.metro.dialer.data.CallGroup
import com.metro.dialer.data.DialerCallLogic
import com.metro.dialer.telecom.DialPadTonePlayer
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroColors
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** WP8.1 dial-pad tile accent flash — hold at least 150ms so quick taps stay visible. */
internal const val DialKeyPressFlashMs = 150L

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
    fontSize = 13.sp,
    lineHeight = 16.sp,
)

private val DialNumberStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Light,
    fontSize = 42.sp,
    lineHeight = 46.sp,
)

/** Smallest dialed-number size before ellipsis — blueprint minimum is 32sp. */
private const val DialNumberMinFontSizeSp = 24f

private val DialKeyBackground = Color(0xFF252525)
private val DialKeypadSectionBackground = Color(0xFF141414)
private val DialKeyHeight = 64.dp

@Composable
fun CallDetailScreen(
    group: CallGroup,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .metroNavBarPadding()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = MetroAppBarDefaults.BarHeight),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
            ) {
                val hasContactName =
                    group.displayName != DialerCallLogic.formatDisplayNumber(group.phoneNumber)
                MetroText(
                    text = group.displayName.uppercase(),
                    style = MetroTextStyle.SectionHeader,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hasContactName) {
                    MetroText(
                        text = DialerCallLogic.formatDisplayNumber(group.phoneNumber),
                        style = MetroTextStyle.ListItemSubtitle,
                        color = MetroTheme.colors.accent,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
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
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Phone,
                    label = stringResource(R.string.call),
                    onClick = onCall,
                ),
                MetroAppBarIcon(
                    type = MetroSystemIconType.Message,
                    label = stringResource(R.string.message),
                    onClick = onMessage,
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun DialPadPane(
    suggestions: List<com.metro.dialer.data.ContactSuggestion>,
    onAppend: (Char) -> Unit,
    onLongPressZero: () -> Unit,
    onSuggestionClick: (com.metro.dialer.data.ContactSuggestion) -> Unit,
    onCall: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        onDispose { DialPadTonePlayer.release() }
    }
    val appendWithTone: (Char) -> Unit = remember(onAppend) {
        { digit ->
            DialPadTonePlayer.play(context, digit)
            onAppend(digit)
        }
    }
    val longPressZeroWithTone: () -> Unit = remember(onLongPressZero) {
        {
            DialPadTonePlayer.play(context, '+')
            onLongPressZero()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                suggestions.forEach { suggestion ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSuggestionClick(suggestion) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
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
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DialKeypadSectionBackground)
                .padding(horizontal = 6.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(DialKeyGap),
        ) {
            KeypadRow(listOf("1" to "", "2" to "ABC", "3" to "DEF"), appendWithTone)
            KeypadRow(listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"), appendWithTone)
            KeypadRow(listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"), appendWithTone)
            KeypadRow(
                keys = listOf("*" to "", "0" to "+", "#" to ""),
                onAppend = appendWithTone,
                onLongPress = mapOf("0" to longPressZeroWithTone),
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
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AutoScaleDialNumberText(
            text = dialString,
            baseStyle = DialNumberStyle,
            color = MetroTheme.colors.primaryText,
            modifier = Modifier.weight(1f),
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
private fun AutoScaleDialNumberText(
    text: String,
    baseStyle: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = constraints.maxWidth
        val lineHeightRatio = baseStyle.lineHeight.value / baseStyle.fontSize.value
        val style = remember(text, maxWidthPx, baseStyle, color) {
            val tinted = baseStyle.copy(color = color)
            if (text.isEmpty() || maxWidthPx <= 0) {
                tinted
            } else {
                val maxSp = baseStyle.fontSize.value
                val minSp = DialNumberMinFontSizeSp.coerceAtMost(maxSp)
                var low = minSp
                var high = maxSp
                var bestSp = minSp
                while (low <= high) {
                    val midSp = (low + high) / 2f
                    val candidate = tinted.copy(
                        fontSize = midSp.sp,
                        lineHeight = (midSp * lineHeightRatio).sp,
                    )
                    val width = textMeasurer.measure(
                        text = text,
                        style = candidate,
                        maxLines = 1,
                    ).size.width
                    if (width <= maxWidthPx) {
                        bestSp = midSp
                        low = midSp + 0.5f
                    } else {
                        high = midSp - 0.5f
                    }
                }
                tinted.copy(
                    fontSize = bestSp.sp,
                    lineHeight = (bestSp * lineHeightRatio).sp,
                )
            }
        }
        BasicText(
            text = text,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
        MetroSystemIcon(
            type = MetroSystemIconType.Backspace,
            iconSize = 40.dp,
            color = color,
            showCircle = false,
        )
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

/**
 * Accent press flash for dial-pad tiles.
 *
 * Collects [PressInteraction] from the source (not pressed-as-state) so same-frame
 * tap down/up still shows the WP8.1 minimum accent flash.
 */
@Composable
internal fun rememberDialKeyPressed(
    interactionSource: MutableInteractionSource,
    minimumDurationMs: Long = DialKeyPressFlashMs,
): Boolean {
    var showPressed by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource, minimumDurationMs) {
        var pressStartedAt = 0L
        var releaseJob: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    releaseJob?.cancel()
                    pressStartedAt = System.currentTimeMillis()
                    showPressed = true
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    releaseJob?.cancel()
                    releaseJob = launch {
                        val remaining =
                            minimumDurationMs - (System.currentTimeMillis() - pressStartedAt)
                        if (remaining > 0) delay(remaining)
                        showPressed = false
                    }
                }
            }
        }
    }

    return showPressed
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
                        // "+" sits beside "0" — match digit size; letter hints stay secondary.
                        style = if (hint == "+") {
                            DialKeyDigitStyle.copy(color = hintColor)
                        } else {
                            DialKeyHintStyle.copy(color = hintColor)
                        },
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
            MetroSystemIcon(
                type = MetroSystemIconType.Save,
                iconSize = 36.dp,
                color = contentColor,
                showCircle = false,
            )
            MetroText(
                text = label,
                style = MetroTextStyle.ListItemSubtitle,
                color = contentColor,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
