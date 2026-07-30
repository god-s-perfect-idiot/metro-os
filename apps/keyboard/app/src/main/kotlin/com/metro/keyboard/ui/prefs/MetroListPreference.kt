package com.metro.keyboard.ui.prefs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.PreferenceDataEvaluator
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.DialogPrefStrings
import dev.patrickgold.jetpref.datastore.ui.ListPreferenceEntry
import dev.patrickgold.jetpref.datastore.ui.LocalDefaultDialogPrefStrings
import dev.patrickgold.jetpref.datastore.ui.LocalIconSpaceReserved
import kotlinx.coroutines.launch

/**
 * WP8.1 list preference — Metro list row + [MetroMessageDialog] radio list.
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
    val scope = rememberCoroutineScope()
    val listPrefValue by listPref.collectAsState()
    val switchPrefValue = switchPref?.collectAsState()
    var tmpListPrefValue by remember { mutableStateOf(listPref.get()) }
    var tmpSwitchPrefValue by remember { mutableStateOf(false) }
    var isDialogOpen by remember { mutableStateOf(false) }

    Preference(
        modifier = modifier,
        icon = icon,
        iconSpaceReserved = iconSpaceReserved,
        title = title,
        summary = if (switchPrefValue?.value == true || switchPrefValue == null) {
            entries.find { it.key == listPrefValue }?.label ?: "—"
        } else {
            summarySwitchDisabled
        },
        trailing = if (switchPrefValue != null) {
            {
                MetroToggleSwitch(
                    checked = switchPrefValue.value,
                    onCheckedChange = { checked ->
                        scope.launch { switchPref.set(checked) }
                    },
                    showStatus = false,
                )
            }
        } else {
            null
        },
        enabledIf = enabledIf,
        visibleIf = visibleIf,
        onClick = {
            tmpListPrefValue = listPrefValue
            if (switchPrefValue != null) {
                tmpSwitchPrefValue = switchPrefValue.value
            }
            isDialogOpen = true
        },
    )

    if (isDialogOpen) {
        MetroMessageDialog(
            title = title.lowercase(),
            confirmLabel = dialogStrings.confirmLabel,
            onConfirm = {
                scope.launch {
                    listPref.set(tmpListPrefValue)
                    switchPref?.set(tmpSwitchPrefValue)
                }
                isDialogOpen = false
            },
            dismissLabel = dialogStrings.dismissLabel,
            onDismiss = { isDialogOpen = false },
            neutralLabel = dialogStrings.neutralLabel,
            onNeutral = {
                scope.launch {
                    listPref.reset()
                    switchPref?.reset()
                }
                isDialogOpen = false
            },
            onDismissRequest = { isDialogOpen = false },
        ) {
            Column {
                if (switchPrefValue != null) {
                    MetroToggleSwitch(
                        checked = tmpSwitchPrefValue,
                        onCheckedChange = { tmpSwitchPrefValue = it },
                        label = title.lowercase(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    )
                }
                val alpha = when {
                    switchPrefValue == null -> 1f
                    tmpSwitchPrefValue -> 1f
                    else -> 0.38f
                }
                for (entry in entries) {
                    val selected = entry.key == tmpListPrefValue
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                enabled = switchPrefValue == null || tmpSwitchPrefValue,
                                onClick = { tmpListPrefValue = entry.key },
                            )
                            .padding(vertical = 10.dp)
                            .alpha(alpha),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MetroText(
                            text = if (selected) "●  ${entry.label}" else "○  ${entry.label}",
                            style = MetroTextStyle.Body,
                            color = if (selected) {
                                MetroTheme.colors.accent
                            } else {
                                MetroTheme.colors.primaryText
                            },
                        )
                    }
                }
            }
        }
    }
}
