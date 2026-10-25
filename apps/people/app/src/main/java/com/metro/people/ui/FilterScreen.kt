package com.metro.people.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.people.R
import com.metro.people.data.PeopleFilter
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarTextButton
import com.metro.ui.MetroCheckBox
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import com.metro.ui.metroClickable

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

    BackHandler(onBack = onCancel)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
                .padding(bottom = MetroAppBarDefaults.BarHeight),
        ) {
            MetroText(
                text = stringResource(R.string.filter_contacts).uppercase(),
                style = MetroTextStyle.SectionHeader,
                modifier = Modifier.padding(top = 24.dp, bottom = 24.dp),
            )
            MetroToggleSwitch(
                checked = hideNoPhone,
                onCheckedChange = { hideNoPhone = it },
                label = stringResource(R.string.hide_no_phone),
                modifier = Modifier.fillMaxWidth(),
            )
            MetroText(
                text = stringResource(R.string.hide_no_phone_hint),
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
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
                        .metroClickable {
                            visible = if (checked) visible - account else visible + account
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetroCheckBox(checked = checked, onCheckedChange = null)
                    MetroText(
                        text = account,
                        style = MetroTextStyle.ListItemTitle,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        MetroAppBar(
            textButtons = listOf(
                MetroAppBarTextButton(
                    text = stringResource(R.string.done),
                    onClick = { onSave(PeopleFilter(hideNoPhone, visible)) },
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
