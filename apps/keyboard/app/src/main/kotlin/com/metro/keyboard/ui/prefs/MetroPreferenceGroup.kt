package com.metro.keyboard.ui.prefs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import dev.patrickgold.florisboard.app.FlorisPreferenceModel
import dev.patrickgold.jetpref.datastore.model.PreferenceDataEvaluator
import dev.patrickgold.jetpref.datastore.model.PreferenceDataEvaluatorScope
import dev.patrickgold.jetpref.datastore.ui.LocalIconSpaceReserved
import dev.patrickgold.jetpref.datastore.ui.LocalIsPrefEnabled
import dev.patrickgold.jetpref.datastore.ui.LocalIsPrefVisible
import dev.patrickgold.jetpref.datastore.ui.PreferenceUiScope

/**
 * WP8.1 preference section — accent section header instead of Material ListItem group title.
 */
@Composable
fun PreferenceUiScope<FlorisPreferenceModel>.PreferenceGroup(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconSpaceReserved: Boolean = LocalIconSpaceReserved.current,
    title: String,
    enabledIf: PreferenceDataEvaluator = { true },
    visibleIf: PreferenceDataEvaluator = { true },
    content: @Composable PreferenceUiScope<FlorisPreferenceModel>.() -> Unit,
) {
    if (LocalIsPrefVisible.current && visibleIf(PreferenceDataEvaluatorScope)) {
        Column(modifier = modifier.fillMaxWidth()) {
            val preferenceScope = PreferenceUiScope(
                prefs = this@PreferenceGroup.prefs,
                columnScope = this@Column,
            )
            CompositionLocalProvider(
                LocalIconSpaceReserved provides iconSpaceReserved,
                LocalIsPrefEnabled provides enabledIf(PreferenceDataEvaluatorScope),
                LocalIsPrefVisible provides visibleIf(PreferenceDataEvaluatorScope),
            ) {
                MetroText(
                    text = title.lowercase(),
                    style = MetroTextStyle.SectionHeader,
                    color = MetroTheme.colors.accent,
                    modifier = Modifier.padding(
                        start = MetroDimens.ScreenHorizontalMargin,
                        end = MetroDimens.ScreenHorizontalMargin,
                        top = 12.dp,
                        bottom = 2.dp,
                    ),
                )
                content(preferenceScope)
            }
        }
    }
    @Suppress("UNUSED_VARIABLE")
    val ignoredIcon = icon
}
