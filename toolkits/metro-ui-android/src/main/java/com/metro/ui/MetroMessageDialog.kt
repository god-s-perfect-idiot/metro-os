package com.metro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * WP8.1 message dialog — centered rectangle panel, 0dp corners (METRO-UX-LANGUAGE §6.15).
 *
 * Affirmative action is leftmost; cancel/dismiss is rightmost.
 */
@Composable
fun MetroMessageDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
    neutralLabel: String? = null,
    onNeutral: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(0.9f)
                    .background(MetroTheme.colors.secondarySurface, RectangleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(24.dp),
            ) {
                MetroText(
                    text = title,
                    style = MetroTextStyle.DialogTitle,
                    color = MetroTheme.colors.primaryText,
                )
                if (!body.isNullOrBlank()) {
                    MetroText(
                        text = body,
                        style = MetroTextStyle.DialogBody,
                        color = MetroTheme.colors.primaryText,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                if (content != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        content()
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!confirmLabel.isNullOrBlank() && onConfirm != null) {
                        MetroBorderButton(
                            text = confirmLabel.lowercase(),
                            onClick = onConfirm,
                        )
                    }
                    if (!neutralLabel.isNullOrBlank() && onNeutral != null) {
                        MetroBorderButton(
                            text = neutralLabel.lowercase(),
                            onClick = onNeutral,
                        )
                    }
                    if (!dismissLabel.isNullOrBlank()) {
                        MetroBorderButton(
                            text = dismissLabel.lowercase(),
                            onClick = onDismiss ?: onDismissRequest,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MetroMessageDialogPreview() {
    MetroTheme(darkTheme = true) {
        MetroMessageDialog(
            title = "delete item?",
            body = "This can't be undone.",
            confirmLabel = "ok",
            onConfirm = {},
            dismissLabel = "cancel",
            onDismissRequest = {},
        )
    }
}
