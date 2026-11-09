package com.metro.clock.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.metro.clock.data.AlarmItem
import com.metro.clock.data.AlarmLogic
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroAppBarMenuItem
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextBox
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTransitions

private enum class EditorSheet {
    None,
    Time,
    Repeats,
}

@Composable
fun AlarmEditorScreen(
    initial: AlarmItem,
    isNew: Boolean,
    onSave: (AlarmItem) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf(initial.minute) }
    var name by remember { mutableStateOf(initial.name) }
    var repeatDays by remember { mutableStateOf(initial.repeatDays) }
    var sheet by remember { mutableStateOf(EditorSheet.None) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // While a sheet is up, ignore presses on the editor so TextBox selection
                // cannot activate underneath the time/repeats overlay.
                .then(
                    if (sheet != EditorSheet.None) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            // Tight app-title → page-title stack (same rules as MetroSettingsHeader).
            MetroSettingsHeader(
                appTitle = "alarms",
                pageTitle = if (isNew) "new" else name.ifBlank { "alarm" }.lowercase(),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = MetroDimens.ScreenHorizontalMargin,
                        end = MetroDimens.ScreenHorizontalMargin,
                        top = 16.dp,
                        bottom = 80.dp,
                    ),
            ) {
                MetroListPicker(
                    selected = Unit,
                    options = emptyList(),
                    onSelectedChange = {},
                    label = "Time",
                    placeholder = AlarmLogic.formatTime(hour, minute),
                    onOpen = {
                        focusManager.clearFocus(force = true)
                        sheet = EditorSheet.Time
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
                MetroListPicker(
                    selected = Unit,
                    options = emptyList(),
                    onSelectedChange = {},
                    label = "Repeats",
                    placeholder = AlarmLogic.formatRepeatSummary(repeatDays),
                    onOpen = {
                        focusManager.clearFocus(force = true)
                        sheet = EditorSheet.Repeats
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
                MetroText(
                    text = "Name",
                    style = MetroTextStyle.ListItemSubtitle,
                    color = MetroTheme.colors.secondaryText,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                MetroTextBox(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Save,
                    label = "save",
                    onClick = {
                        onSave(
                            initial.copy(
                                hour = hour,
                                minute = minute,
                                name = name.ifBlank { "Alarm" },
                                repeatDays = repeatDays,
                                enabled = if (isNew) true else initial.enabled,
                            ),
                        )
                    },
                ),
            ),
            menuItems = buildList {
                if (onDelete != null) {
                    add(MetroAppBarMenuItem("delete") { onDelete() })
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        AnimatedVisibility(
            visible = sheet == EditorSheet.Time,
            enter = fadeIn(tween(MetroTransitions.PagePivotLoadMs)),
            exit = fadeOut(tween(MetroTransitions.PagePivotExitMs)),
            modifier = Modifier.fillMaxSize(),
        ) {
            BackHandler { sheet = EditorSheet.None }
            TimePickerScreen(
                hour24 = hour,
                minute = minute,
                onConfirm = { h, m ->
                    hour = h
                    minute = m
                    sheet = EditorSheet.None
                },
                onCancel = { sheet = EditorSheet.None },
                modifier = Modifier.fillMaxSize(),
            )
        }

        AnimatedVisibility(
            visible = sheet == EditorSheet.Repeats,
            enter = fadeIn(tween(MetroTransitions.PagePivotLoadMs)),
            exit = fadeOut(tween(MetroTransitions.PagePivotExitMs)),
            modifier = Modifier.fillMaxSize(),
        ) {
            BackHandler { sheet = EditorSheet.None }
            AlarmRepeatsScreen(
                selectedDays = repeatDays,
                onConfirm = {
                    repeatDays = it
                    sheet = EditorSheet.None
                },
                onCancel = { sheet = EditorSheet.None },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
