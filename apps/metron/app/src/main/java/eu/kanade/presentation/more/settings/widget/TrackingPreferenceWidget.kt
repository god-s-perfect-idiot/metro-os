package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroCircleIconButton
import com.metro.ui.MetroListItem
import com.metro.ui.MetroSystemIcon
import com.metro.ui.MetroSystemIconType
import com.metro.ui.MetroTheme
import eu.kanade.presentation.more.settings.LocalPreferenceHighlighted
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TrackingPreferenceWidget(
    modifier: Modifier = Modifier,
    tracker: Tracker,
    isLoggedIn: Boolean,
    isRefreshing: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val highlighted = LocalPreferenceHighlighted.current
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val displayName = tracker.getDisplayUsername()
    val subtitle = if (isLoggedIn && displayName.isNotBlank()) {
        displayName.lowercase()
    } else {
        null
    }

    Box(modifier = Modifier.highlightBackground(highlighted)) {
        MetroListItem(
            title = tracker.name.lowercase(),
            subtitle = subtitle,
            modifier = modifier,
            leading = { TrackLogoIcon(tracker) },
            trailing = if (isLoggedIn) {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (tracker !is EnhancedTracker) {
                            MetroCircleIconButton(
                                type = MetroSystemIconType.Refresh,
                                enabled = !isRefreshing,
                                onClick = {
                                    scope.launch { tracker.refreshUser() }
                                },
                                contentDescription = stringResource(MR.strings.refresh_tracker_profile),
                                glyphSize = 28.dp,
                                color = MetroTheme.colors.primaryText,
                            )
                        }
                        MetroSystemIcon(
                            type = MetroSystemIconType.Check,
                            iconSize = 28.dp,
                            showCircle = false,
                            color = MetroTheme.colors.accent,
                        )
                    }
                }
            } else {
                null
            },
            oneLineMinHeight = 64.dp,
            twoLineMinHeight = 72.dp,
            verticalPadding = 8.dp,
            onClick = onClick,
        )
    }
}
