package com.metro.lockscreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Five equal columns — slot [columnIndex] always maps to column [columnIndex].
 * Empty columns stay blank (no [SpaceBetween] gap redistribution).
 */
@Composable
fun QuickStatusColumnRow(
    modifier: Modifier = Modifier,
    columnContent: @Composable (columnIndex: Int) -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        repeat(LockscreenQuickStatusLogic.SLOT_COUNT) { columnIndex ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                columnContent(columnIndex)
            }
        }
    }
}
