package com.metro.keyboard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.PreferenceDataEvaluator
import dev.patrickgold.jetpref.datastore.model.PreferenceDataEvaluatorScope
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.LocalIconSpaceReserved
import dev.patrickgold.jetpref.datastore.ui.LocalIsPrefEnabled
import dev.patrickgold.jetpref.datastore.ui.LocalIsPrefVisible
import kotlinx.coroutines.launch

/**
 * WP8.1 switch preference — [MetroToggleSwitch] stand-in for JetPref SwitchPreference.
 */
@Composable
fun MetroSwitchPreference(
    switchPref: PreferenceData<Boolean>,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconSpaceReserved: Boolean = LocalIconSpaceReserved.current,
    title: String,
    summary: String? = null,
    summaryOn: String? = null,
    summaryOff: String? = null,
    enabledIf: PreferenceDataEvaluator = { true },
    visibleIf: PreferenceDataEvaluator = { true },
) {
    if (!(LocalIsPrefVisible.current && visibleIf(PreferenceDataEvaluatorScope))) return

    val checked by switchPref.collectAsState()
    val isEnabled = LocalIsPrefEnabled.current && enabledIf(PreferenceDataEvaluatorScope)
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 4.dp,
            ),
    ) {
        MetroToggleSwitch(
            checked = checked,
            onCheckedChange = { value ->
                if (isEnabled) {
                    scope.launch { switchPref.set(value) }
                }
            },
            enabled = isEnabled,
            label = title.lowercase(),
            modifier = Modifier.fillMaxWidth(),
        )

        val resolvedSummary = when {
            checked && !summaryOn.isNullOrBlank() -> summaryOn
            !checked && !summaryOff.isNullOrBlank() -> summaryOff
            else -> summary
        }

        if (!resolvedSummary.isNullOrBlank()) {
            MetroText(
                text = resolvedSummary,
                style = MetroTextStyle.ListItemSubtitle,
                color = MetroTheme.colors.secondaryText,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
    @Suppress("UNUSED_VARIABLE")
    val ignored = icon to iconSpaceReserved
}

/** Alias matching JetPref SwitchPreference call sites. */
@Composable
fun SwitchPreference(
    switchPref: PreferenceData<Boolean>,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconSpaceReserved: Boolean = LocalIconSpaceReserved.current,
    title: String,
    summary: String? = null,
    summaryOn: String? = null,
    summaryOff: String? = null,
    enabledIf: PreferenceDataEvaluator = { true },
    visibleIf: PreferenceDataEvaluator = { true },
) {
    MetroSwitchPreference(
        switchPref = switchPref,
        modifier = modifier,
        icon = icon,
        iconSpaceReserved = iconSpaceReserved,
        title = title,
        summary = summary,
        summaryOn = summaryOn,
        summaryOff = summaryOff,
        enabledIf = enabledIf,
        visibleIf = visibleIf,
    )
}
