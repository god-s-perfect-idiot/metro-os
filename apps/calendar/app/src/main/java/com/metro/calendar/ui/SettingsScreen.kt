package com.metro.calendar.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.calendar.R
import com.metro.calendar.data.CalendarViewType
import com.metro.ui.MetroAppBar
import com.metro.ui.MetroAppBarDefaults
import com.metro.ui.MetroAppBarIcon
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroToggleSwitch
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment

@Composable
fun SettingsScreen(
    useFahrenheit: Boolean,
    showWeather: Boolean,
    use24HourTime: Boolean,
    defaultViewType: CalendarViewType,
    onUseFahrenheitChange: (Boolean) -> Unit,
    onShowWeatherChange: (Boolean) -> Unit,
    onUse24HourTimeChange: (Boolean) -> Unit,
    onDefaultViewTypeChange: (CalendarViewType) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var openPicker by remember { mutableStateOf<String?>(null) }
    val tempOptions = listOf(
        stringResource(R.string.settings_temp_f),
        stringResource(R.string.settings_temp_c),
    )
    val viewOptions = listOf(
        stringResource(R.string.view_type_day),
        stringResource(R.string.view_type_week),
        stringResource(R.string.view_type_month),
        stringResource(R.string.view_type_year),
    )
    val tempIndex = if (useFahrenheit) 0 else 1
    val viewIndex = defaultViewType.ordinal.coerceIn(0, viewOptions.lastIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MetroText(
                text = stringResource(R.string.settings_title),
                style = MetroTextStyle.SectionHeader,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(top = 24.dp, bottom = 8.dp),
            )
            MetroText(
                text = stringResource(R.string.settings_hub_title),
                style = MetroTextStyle.HubTitle,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 20.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(bottom = MetroAppBarDefaults.BarHeight + 24.dp),
            ) {
                MetroToggleSwitch(
                    checked = showWeather,
                    onCheckedChange = onShowWeatherChange,
                    label = stringResource(R.string.settings_show_weather),
                )

                Spacer(modifier = Modifier.height(20.dp))
                MetroListPicker(
                    options = tempOptions,
                    selectedOptionIndex = tempIndex,
                    onSelectOption = { index ->
                        onUseFahrenheitChange(index == 0)
                        openPicker = null
                    },
                    label = stringResource(R.string.settings_temperature_unit),
                    enabled = showWeather,
                    expanded = openPicker == "temp",
                    onExpandedChange = { expanded ->
                        openPicker = if (expanded) "temp" else null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(20.dp))
                MetroToggleSwitch(
                    checked = use24HourTime,
                    onCheckedChange = onUse24HourTimeChange,
                    label = stringResource(R.string.settings_24_hour),
                )

                Spacer(modifier = Modifier.height(20.dp))
                MetroListPicker(
                    options = viewOptions,
                    selectedOptionIndex = viewIndex,
                    onSelectOption = { index ->
                        onDefaultViewTypeChange(CalendarViewType.fromIndex(index))
                        openPicker = null
                    },
                    label = stringResource(R.string.settings_default_view),
                    expanded = openPicker == "view",
                    onExpandedChange = { expanded ->
                        openPicker = if (expanded) "view" else null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        MetroAppBar(
            icons = listOf(
                MetroAppBarIcon(
                    type = MetroSystemIconType.Back,
                    label = stringResource(R.string.event_cancel),
                    onClick = onBack,
                    contentDescription = stringResource(R.string.event_cancel),
                ),
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
