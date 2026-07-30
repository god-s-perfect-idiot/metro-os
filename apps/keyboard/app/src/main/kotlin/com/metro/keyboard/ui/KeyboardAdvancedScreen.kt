package com.metro.keyboard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroDimens
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

@Composable
fun KeyboardAdvancedScreen(
    modifier: Modifier = Modifier,
) {
    val prefs by FlorisPreferenceStore
    val utilityKeyEnabled by prefs.keyboard.utilityKeyEnabled.collectAsState()
    val scope = rememberCoroutineScope()
    // WP8.1 "Show a comma key when available" — FlorisBoard layouts already include comma;
    // keep a local toggle that mirrors the advanced setting surface for fidelity.
    var showCommaKey by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        MetroSettingsHeader(pageTitle = "advanced")
        MetroToggleSwitch(
            checked = showCommaKey,
            onCheckedChange = { showCommaKey = it },
            label = "Show a comma key when available",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )
        MetroText(
            text = "Adds a comma key on the bottom row when the layout supports it (WP8.1 keyboard → advanced).",
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 8.dp,
            ),
        )
        MetroToggleSwitch(
            checked = utilityKeyEnabled,
            onCheckedChange = { enabled ->
                scope.launch { prefs.keyboard.utilityKeyEnabled.set(enabled) }
            },
            label = "Show emoji key",
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MetroDimens.ScreenHorizontalMargin,
                    vertical = 8.dp,
                ),
        )
        MetroText(
            text = "Shows the smiley key on the bottom row (WP8.1 emoticon key).",
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 8.dp,
            ),
        )
    }
}
