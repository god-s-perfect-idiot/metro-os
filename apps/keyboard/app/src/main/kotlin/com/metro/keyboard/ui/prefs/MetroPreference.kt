package com.metro.keyboard.ui.prefs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroListItem
import dev.patrickgold.jetpref.datastore.model.PreferenceDataEvaluator
import dev.patrickgold.jetpref.datastore.model.PreferenceDataEvaluatorScope
import dev.patrickgold.jetpref.datastore.ui.LocalIconSpaceReserved
import dev.patrickgold.jetpref.datastore.ui.LocalIsPrefEnabled
import dev.patrickgold.jetpref.datastore.ui.LocalIsPrefVisible

/**
 * WP8.1 preference row — [MetroListItem] stand-in for JetPref [dev.patrickgold.jetpref.datastore.ui.Preference].
 * Icons are ignored (Metro settings lists are text-first).
 */
@Composable
fun Preference(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconSpaceReserved: Boolean = LocalIconSpaceReserved.current,
    title: String,
    summary: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    enabledIf: PreferenceDataEvaluator = { true },
    visibleIf: PreferenceDataEvaluator = { true },
    onClick: (() -> Unit)? = null,
    eventModifier: (@Composable () -> Modifier)? = null,
) {
    require(!(onClick != null && eventModifier != null)) {
        "You cannot provide both an onClick lambda and an eventModifier."
    }
    if (LocalIsPrefVisible.current && visibleIf(PreferenceDataEvaluatorScope)) {
        val isEnabled = LocalIsPrefEnabled.current && enabledIf(PreferenceDataEvaluatorScope)
        CompositionLocalProvider(
            LocalIconSpaceReserved provides iconSpaceReserved,
            LocalIsPrefEnabled provides isEnabled,
            LocalIsPrefVisible provides true,
        ) {
            val rowModifier = if (onClick != null) {
                modifier
            } else {
                modifier.then(eventModifier?.invoke() ?: Modifier)
            }
            MetroListItem(
                title = title.lowercase(),
                subtitle = summary,
                enabled = isEnabled,
                trailing = trailing,
                modifier = rowModifier,
                verticalPadding = 8.dp,
                oneLineMinHeight = 56.dp,
                twoLineMinHeight = 68.dp,
                onClick = if (onClick != null && isEnabled) onClick else null,
            )
        }
    }
    // Suppress unused icon warning — Metro chrome is text-first.
    @Suppress("UNUSED_VARIABLE")
    val ignoredIcon = icon
}
