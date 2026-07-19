package com.metro.people.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.people.R
import com.metro.people.data.PeopleFilter
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun FilterScreen(
    initial: PeopleFilter,
    accounts: Set<String>,
    onSave: (PeopleFilter) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hideNoPhone by remember(initial) { mutableStateOf(initial.hideWithoutPhone) }
    var visible by remember(initial) { mutableStateOf(initial.visibleAccounts.ifEmpty { accounts }) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        MetroText(
            text = stringResource(R.string.filter_contacts).uppercase(),
            style = MetroTextStyle.SectionHeader,
            modifier = Modifier.padding(top = 24.dp, bottom = 24.dp),
        )
        MetroText(
            text = stringResource(R.string.hide_no_phone),
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
        )
        MetroText(
            text = if (hideNoPhone) "On" else "Off",
            style = MetroTextStyle.ListItemTitle,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .clickable { hideNoPhone = !hideNoPhone },
        )
        MetroText(
            text = stringResource(R.string.hide_no_phone_hint),
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        MetroText(
            text = stringResource(R.string.show_contacts_from),
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        accounts.sorted().forEach { account ->
            val checked = account in visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        visible = if (checked) visible - account else visible + account
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetroCheckBox(checked = checked)
                MetroText(
                    text = account,
                    style = MetroTextStyle.ListItemTitle,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetroCircleIconButton(
                type = MetroSystemIconType.Add,
                onClick = { onSave(PeopleFilter(hideNoPhone, visible)) },
                contentDescription = "save",
            )
            MetroCircleIconButton(
                type = MetroSystemIconType.Close,
                onClick = onCancel,
                modifier = Modifier.padding(start = 16.dp),
                contentDescription = "cancel",
            )
        }
    }
}

@Composable
private fun MetroCheckBox(checked: Boolean) {
    val accent = MetroTheme.colors.accent
    Canvas(modifier = Modifier.size(20.dp)) {
        if (checked) {
            drawRect(color = accent)
        } else {
            drawRect(color = Color.White, style = Stroke(width = 2.dp.toPx()))
        }
    }
}
