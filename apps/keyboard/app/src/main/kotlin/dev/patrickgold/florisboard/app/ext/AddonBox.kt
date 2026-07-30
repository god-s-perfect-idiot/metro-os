/*
 * Copyright (C) 2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.app.ext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metro.ui.MetroColors
import com.metro.ui.MetroDimens
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroTile
import com.metro.ui.MetroTileSize
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.lib.ext.Extension
import dev.patrickgold.florisboard.lib.ext.generateUpdateUrl
import dev.patrickgold.florisboard.lib.util.launchUrl
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.kotlin.curlyFormat

@Composable
fun ImportExtensionBox(navController: NavController) {
    val context = LocalContext.current
    MetroAddonPanel(
        body = stringRes(id = R.string.ext__home__info),
    ) {
        MetroTile(
            title = stringRes(id = R.string.ext__home__visit_store),
            size = MetroTileSize.Medium,
            backgroundColor = MetroTheme.colors.accent,
            onClick = {
                context.launchUrl("https://${BuildConfig.FLADDONS_STORE_URL}/")
            },
        )
        MetroTile(
            title = stringRes(R.string.action__import),
            size = MetroTileSize.Medium,
            backgroundColor = MetroColors.AccentCobalt,
            onClick = {
                navController.navigate(Routes.Ext.Import(ExtensionImportScreenType.EXT_ANY, null))
            },
        )
    }
}

@Composable
fun UpdateBox(extensionIndex: List<Extension>) {
    val context = LocalContext.current
    MetroAddonPanel(
        body = stringRes(id = R.string.ext__update_box__internet_permission_hint),
    ) {
        MetroTile(
            title = stringRes(id = R.string.ext__update_box__search_for_updates),
            size = MetroTileSize.Medium,
            backgroundColor = MetroTheme.colors.accent,
            onClick = {
                context.launchUrl(extensionIndex.generateUpdateUrl())
            },
        )
    }
}

@Composable
fun AddonManagementReferenceBox(
    type: ExtensionListScreenType,
) {
    val navController = LocalNavController.current
    val title = stringRes(id = R.string.ext__addon_management_box__managing_placeholder).curlyFormat(
        "extensions" to type.let { stringRes(id = it.titleResId).lowercase() },
    )
    MetroAddonPanel(
        title = title.lowercase(),
        body = stringRes(id = R.string.ext__addon_management_box__addon_manager_info),
    ) {
        MetroTile(
            title = stringRes(id = R.string.ext__addon_management_box__go_to_page).curlyFormat(
                "ext_home_title" to stringRes(type.titleResId),
            ),
            size = MetroTileSize.Medium,
            backgroundColor = MetroTheme.colors.accent,
            onClick = {
                navController.navigate(Routes.Ext.List(type, showUpdate = true))
            },
        )
    }
}

@Composable
private fun MetroAddonPanel(
    body: String,
    title: String? = null,
    tiles: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MetroDimens.ScreenHorizontalMargin,
                vertical = 12.dp,
            ),
    ) {
        if (title != null) {
            MetroText(
                text = title,
                style = MetroTextStyle.SectionHeader,
                color = MetroTheme.colors.accent,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        MetroText(
            text = body,
            style = MetroTextStyle.ListItemSubtitle,
            color = MetroTheme.colors.secondaryText,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tiles()
        }
    }
}
