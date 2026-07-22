package com.metro.settings.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metro.settings.R
import com.metro.system.MetroFontScale
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroStepSlider
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

@Composable
fun EaseOfAccessScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        MetroSettingsHeader(pageTitle = stringResource(R.string.settings_ease_of_access))

        MetroText(
            text = stringResource(R.string.settings_text_size),
            style = MetroTextStyle.SectionHeader,
            modifier = Modifier.padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 8.dp,
            ),
        )

        Box(
            modifier = Modifier
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                .fillMaxWidth()
                .height(148.dp)
                .border(1.dp, MetroTheme.colors.secondaryText.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            MetroText(
                text = stringResource(R.string.settings_sample),
                style = MetroTextStyle.ListItemTitle,
            )
        }

        MetroStepSlider(
            index = state.fontScaleIndex,
            onIndexChange = state::applyFontScaleIndex,
            stepCount = MetroFontScale.STEP_COUNT,
            modifier = Modifier.padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 20.dp,
            ),
        )

        MetroText(
            text = stringResource(R.string.settings_text_size_help),
            style = MetroTextStyle.Body,
            color = MetroTheme.colors.primaryText,
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )
    }
}
