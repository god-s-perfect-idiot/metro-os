package com.metro.keyboard.ui.prefs

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
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption
import com.metro.ui.MetroToggleSwitch
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.PreferenceDataEvaluator
import dev.patrickgold.jetpref.datastore.model.PreferenceDataEvaluatorScope
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.DialogPrefStrings
import dev.patrickgold.jetpref.datastore.ui.ListPreferenceEntry
import dev.patrickgold.jetpref.datastore.ui.LocalDefaultDialogPrefStrings
import dev.patrickgold.jetpref.datastore.ui.LocalIconSpaceReserved
import dev.patrickgold.jetpref.datastore.ui.LocalIsPrefEnabled
import dev.patrickgold.jetpref.datastore.ui.LocalIsPrefVisible
import kotlinx.coroutines.launch

/**
 * WP8.1 list preference — [MetroListPicker] stand-in for JetPref dialog ListPreference.
 *
 * Optional [switchPref] shows a [MetroToggleSwitch] above the picker; the picker is enabled
 * only while the switch is on.
 */
@Composable
fun <V : Any> ListPreference(
    listPref: PreferenceData<V>,
    switchPref: PreferenceData<Boolean>? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconSpaceReserved: Boolean = LocalIconSpaceReserved.current,
    title: String,
    summarySwitchDisabled: String? = null,
    dialogStrings: DialogPrefStrings = LocalDefaultDialogPrefStrings.current,
    enabledIf: PreferenceDataEvaluator = { true },
    visibleIf: PreferenceDataEvaluator = { true },
    entries: List<ListPreferenceEntry<V>>,
) {
    if (!(LocalIsPrefVisible.current && visibleIf(PreferenceDataEvaluatorScope))) return

    val scope = rememberCoroutineScope()
    val listPrefValue by listPref.collectAsState()
    val switchPrefValue = switchPref?.collectAsState()
    val isPrefEnabled = LocalIsPrefEnabled.current && enabledIf(PreferenceDataEvaluatorScope)
    val switchOn = switchPrefValue?.value != false
    val pickerEnabled = isPrefEnabled && switchOn

    val options = entries.map { MetroListPickerOption(value = it.key, label = it.label) }
    val displaySelected = if (switchOn) {
        listPrefValue
    } else {
        null
    }
    val placeholder = when {
        !switchOn && !summarySwitchDisabled.isNullOrBlank() -> summarySwitchDisabled
        else -> entries.find { it.key == listPrefValue }?.label.orEmpty()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 8.dp,
            ),
    ) {
        if (switchPref != null && switchPrefValue != null) {
            MetroToggleSwitch(
                checked = switchPrefValue.value,
                onCheckedChange = { checked ->
                    if (isPrefEnabled) {
                        scope.launch { switchPref.set(checked) }
                    }
                },
                enabled = isPrefEnabled,
                label = title.lowercase(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
            MetroListPicker(
                selected = displaySelected,
                options = options,
                onSelectedChange = { value ->
                    scope.launch { listPref.set(value) }
                },
                enabled = pickerEnabled,
                placeholder = placeholder,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            MetroListPicker(
                selected = listPrefValue,
                options = options,
                onSelectedChange = { value ->
                    scope.launch { listPref.set(value) }
                },
                label = title.lowercase(),
                enabled = pickerEnabled,
                placeholder = placeholder,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Kept for API parity with JetPref ListPreference call sites.
    @Suppress("UNUSED_VARIABLE")
    val ignored = icon to iconSpaceReserved to dialogStrings
}
