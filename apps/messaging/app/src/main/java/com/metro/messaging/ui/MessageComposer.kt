package com.metro.messaging.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.metro.messaging.R
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/**
 * WP8.1 conversation composer — light-gray outgoing-shaped bubble with
 * "type a text message" placeholder. Send via SIP ImeAction or the border button.
 */
@Composable
fun MessageComposer(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    sendEnabled: Boolean = text.isNotBlank(),
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            MessageBubbleChrome(
                kind = MessageBubbleKind.Outgoing,
                color = ComposerBubbleColor,
                maxWidthFraction = 1f,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    textStyle = MetroTextStyle.Body.toTextStyle().copy(
                        color = ComposerTextColor,
                    ),
                    cursorBrush = SolidColor(MetroTheme.colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { if (sendEnabled) onSend() },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (text.isEmpty()) {
                                MetroText(
                                    text = stringResource(R.string.composer_hint),
                                    style = MetroTextStyle.Body,
                                    color = ComposerHintColor,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetroBorderButton(
                text = stringResource(R.string.send),
                onClick = onSend,
                enabled = sendEnabled,
            )
        }
    }
}
