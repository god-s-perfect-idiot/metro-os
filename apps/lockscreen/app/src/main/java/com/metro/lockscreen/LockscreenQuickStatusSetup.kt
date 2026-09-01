package com.metro.lockscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroAppSlotButton
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/**
 * Setup section — five bordered slots for WP8.1 quick-status app picks.
 */
@Composable
fun LockscreenQuickStatusSetup(
    slots: List<String?>,
    onSlotClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MetroText(
            text = stringResource(R.string.notifications_section),
            style = MetroTextStyle.SectionHeader,
            color = MetroTheme.colors.accent,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
        )
        MetroText(
            text = stringResource(R.string.quick_status_label),
            style = MetroTextStyle.Body,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        )
        QuickStatusColumnRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) { index ->
            QuickStatusSlotButton(
                packageName = slots[index],
                onClick = { onSlotClick(index) },
            )
        }
    }
}

@Composable
private fun QuickStatusSlotButton(
    packageName: String?,
    onClick: () -> Unit,
) {
    val icon = rememberQuickStatusIcon(packageName)
    val launcherPainter = rememberQuickStatusIconPainter(icon)
    MetroAppSlotButton(
        onClick = onClick,
        iconResId = icon?.glyphResId,
        iconPainter = launcherPainter,
        contentDescription = packageName,
    )
}
