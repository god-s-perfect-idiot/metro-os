package com.metro.keyboard.ui.prefs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroMessageDialog
import com.metro.ui.MetroSlider
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.PreferenceDataEvaluator
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.DialogPrefStrings
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.LocalDefaultDialogPrefStrings
import dev.patrickgold.jetpref.datastore.ui.LocalIconSpaceReserved
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * WP8.1 dialog slider preference — Metro list row + [MetroMessageDialog] with [MetroSlider].
 */
@ExperimentalJetPrefDatastoreUi
@Composable
fun DialogSliderPreference(
    pref: PreferenceData<Int>,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconSpaceReserved: Boolean = LocalIconSpaceReserved.current,
    title: String,
    valueLabel: @Composable (Int) -> String = { it.toString() },
    summary: @Composable (Int) -> String = valueLabel,
    min: Int,
    max: Int,
    stepIncrement: Int,
    onPreviewSelectedValue: (Int) -> Unit = { },
    dialogStrings: DialogPrefStrings = LocalDefaultDialogPrefStrings.current,
    enabledIf: PreferenceDataEvaluator = { true },
    visibleIf: PreferenceDataEvaluator = { true },
) {
    require(stepIncrement > 0) { "Step increment must be greater than 0!" }
    require(max > min) { "Maximum value ($max) must be greater than minimum value ($min)!" }

    val scope = rememberCoroutineScope()
    val prefValue by pref.collectAsState()
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isDialogOpen by remember { mutableStateOf(false) }
    val steps = ((max - min) / stepIncrement) - 1

    Preference(
        modifier = modifier,
        icon = icon,
        iconSpaceReserved = iconSpaceReserved,
        title = title,
        summary = summary(prefValue),
        enabledIf = enabledIf,
        visibleIf = visibleIf,
        onClick = {
            sliderValue = prefValue.toFloat()
            isDialogOpen = true
        },
    )

    if (isDialogOpen) {
        MetroMessageDialog(
            title = title.lowercase(),
            confirmLabel = dialogStrings.confirmLabel,
            onConfirm = {
                scope.launch { pref.set(sliderValue.roundToInt()) }
                isDialogOpen = false
            },
            dismissLabel = dialogStrings.dismissLabel,
            onDismiss = { isDialogOpen = false },
            neutralLabel = dialogStrings.neutralLabel,
            onNeutral = {
                scope.launch { pref.reset() }
                isDialogOpen = false
            },
            onDismissRequest = { isDialogOpen = false },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                MetroText(
                    text = valueLabel(sliderValue.roundToInt()),
                    style = MetroTextStyle.Body,
                    color = MetroTheme.colors.primaryText,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                MetroSlider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        onPreviewSelectedValue(it.roundToInt())
                    },
                    valueRange = min.toFloat()..max.toFloat(),
                    steps = steps.coerceAtLeast(0),
                )
            }
        }
    }
}

@ExperimentalJetPrefDatastoreUi
@Composable
fun DialogSliderPreference(
    primaryPref: PreferenceData<Int>,
    secondaryPref: PreferenceData<Int>,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconSpaceReserved: Boolean = LocalIconSpaceReserved.current,
    title: String,
    primaryLabel: String,
    secondaryLabel: String,
    valueLabel: @Composable (Int) -> String = { it.toString() },
    summary: @Composable (Int, Int) -> String = { p, s -> "${valueLabel(p)} / ${valueLabel(s)}" },
    min: Int,
    max: Int,
    stepIncrement: Int,
    onPreviewSelectedPrimaryValue: (Int) -> Unit = { },
    onPreviewSelectedSecondaryValue: (Int) -> Unit = { },
    dialogStrings: DialogPrefStrings = LocalDefaultDialogPrefStrings.current,
    enabledIf: PreferenceDataEvaluator = { true },
    visibleIf: PreferenceDataEvaluator = { true },
) {
    require(stepIncrement > 0) { "Step increment must be greater than 0!" }
    require(max > min) { "Maximum value ($max) must be greater than minimum value ($min)!" }

    val scope = rememberCoroutineScope()
    val primaryPrefValue by primaryPref.collectAsState()
    val secondaryPrefValue by secondaryPref.collectAsState()
    var primarySliderValue by remember { mutableFloatStateOf(0f) }
    var secondarySliderValue by remember { mutableFloatStateOf(0f) }
    var isDialogOpen by remember { mutableStateOf(false) }
    val steps = ((max - min) / stepIncrement) - 1

    Preference(
        modifier = modifier,
        icon = icon,
        iconSpaceReserved = iconSpaceReserved,
        title = title,
        summary = summary(primaryPrefValue, secondaryPrefValue),
        enabledIf = enabledIf,
        visibleIf = visibleIf,
        onClick = {
            primarySliderValue = primaryPrefValue.toFloat()
            secondarySliderValue = secondaryPrefValue.toFloat()
            isDialogOpen = true
        },
    )

    if (isDialogOpen) {
        MetroMessageDialog(
            title = title.lowercase(),
            confirmLabel = dialogStrings.confirmLabel,
            onConfirm = {
                scope.launch {
                    primaryPref.set(primarySliderValue.roundToInt())
                    secondaryPref.set(secondarySliderValue.roundToInt())
                }
                isDialogOpen = false
            },
            dismissLabel = dialogStrings.dismissLabel,
            onDismiss = { isDialogOpen = false },
            neutralLabel = dialogStrings.neutralLabel,
            onNeutral = {
                scope.launch {
                    primaryPref.reset()
                    secondaryPref.reset()
                }
                isDialogOpen = false
            },
            onDismissRequest = { isDialogOpen = false },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MetroText(
                        text = primaryLabel.lowercase(),
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.primaryText,
                    )
                    MetroText(
                        text = valueLabel(primarySliderValue.roundToInt()),
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
                MetroSlider(
                    value = primarySliderValue,
                    onValueChange = {
                        primarySliderValue = it
                        onPreviewSelectedPrimaryValue(it.roundToInt())
                    },
                    valueRange = min.toFloat()..max.toFloat(),
                    steps = steps.coerceAtLeast(0),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MetroText(
                        text = secondaryLabel.lowercase(),
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.primaryText,
                    )
                    MetroText(
                        text = valueLabel(secondarySliderValue.roundToInt()),
                        style = MetroTextStyle.Body,
                        color = MetroTheme.colors.secondaryText,
                    )
                }
                MetroSlider(
                    value = secondarySliderValue,
                    onValueChange = {
                        secondarySliderValue = it
                        onPreviewSelectedSecondaryValue(it.roundToInt())
                    },
                    valueRange = min.toFloat()..max.toFloat(),
                    steps = steps.coerceAtLeast(0),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
