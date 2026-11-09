package com.metro.clock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.metro.clock.data.AlarmLogic
import com.metro.ui.MetroMultiSelectItem
import com.metro.ui.MetroMultiSelectList

@Composable
fun AlarmRepeatsScreen(
    selectedDays: Set<Int>,
    onConfirm: (Set<Int>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember {
        AlarmLogic.Weekdays.map { (id, title) ->
            MetroMultiSelectItem(id = id.toString(), title = title)
        }
    }
    var selectedIds by remember(selectedDays) {
        mutableStateOf(selectedDays.map { it.toString() }.toSet())
    }

    MetroMultiSelectList(
        title = "ALARM REPEATS ON",
        items = items,
        selectedIds = selectedIds,
        onSelectionChange = { selectedIds = it },
        onConfirm = {
            onConfirm(selectedIds.mapNotNull { it.toIntOrNull() }.toSet())
        },
        onCancel = onCancel,
        modifier = modifier,
        confirmLabel = "done",
        cancelLabel = "cancel",
    )
}
