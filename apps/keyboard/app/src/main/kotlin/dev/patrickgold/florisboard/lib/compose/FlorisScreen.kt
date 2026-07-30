/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.lib.compose

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import dev.patrickgold.florisboard.app.FlorisPreferenceModel
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.jetpref.datastore.ui.PreferenceLayout
import dev.patrickgold.jetpref.datastore.ui.PreferenceUiContent
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.florisVerticalScroll

@Composable
fun FlorisScreen(builder: @Composable FlorisScreenScope.() -> Unit) {
    val scope = remember { FlorisScreenScopeImpl() }
    builder(scope)
    scope.Render()
}

typealias FlorisScreenActions = @Composable RowScope.() -> Unit
typealias FlorisScreenBottomBar = @Composable () -> Unit
typealias FlorisScreenContent = PreferenceUiContent<FlorisPreferenceModel>
typealias FlorisScreenFab = @Composable () -> Unit
typealias FlorisScreenNavigationIcon = @Composable () -> Unit

interface FlorisScreenScope {
    var title: String

    var navigationIconVisible: Boolean

    var previewFieldVisible: Boolean

    var scrollable: Boolean

    var iconSpaceReserved: Boolean

    fun actions(actions: FlorisScreenActions)

    fun bottomBar(bottomBar: FlorisScreenBottomBar)

    fun content(content: FlorisScreenContent)

    fun floatingActionButton(fab: FlorisScreenFab)

    fun navigationIcon(navigationIcon: FlorisScreenNavigationIcon)
}

private class FlorisScreenScopeImpl : FlorisScreenScope {
    override var title: String by mutableStateOf("")
    override var navigationIconVisible: Boolean by mutableStateOf(true)
    override var previewFieldVisible: Boolean by mutableStateOf(false)
    override var scrollable: Boolean by mutableStateOf(true)
    override var iconSpaceReserved: Boolean by mutableStateOf(false)

    private var actions: FlorisScreenActions = @Composable { }
    private var bottomBar: FlorisScreenBottomBar = @Composable { }
    private var content: FlorisScreenContent = @Composable { }
    private var fab: FlorisScreenFab = @Composable { }
    private var navigationIcon: FlorisScreenNavigationIcon = @Composable { }

    override fun actions(actions: FlorisScreenActions) {
        this.actions = actions
    }

    override fun bottomBar(bottomBar: FlorisScreenBottomBar) {
        this.bottomBar = bottomBar
    }

    override fun content(content: FlorisScreenContent) {
        this.content = content
    }

    override fun floatingActionButton(fab: FlorisScreenFab) {
        this.fab = fab
    }

    override fun navigationIcon(navigationIcon: FlorisScreenNavigationIcon) {
        this.navigationIcon = navigationIcon
    }

    @Composable
    fun Render() {
        val context = LocalContext.current
        val previewFieldController = LocalPreviewFieldController.current
        val background = MetroTheme.colors.background

        SideEffect {
            val window = (context as Activity).window
            previewFieldController?.isVisible = previewFieldVisible
            window.statusBarColor = Color.Transparent.toArgb()
            if (AndroidVersion.ATLEAST_API29_Q) {
                window.navigationBarColor = Color.Transparent.toArgb()
                window.isNavigationBarContrastEnforced = true
            } else {
                window.navigationBarColor = background.toArgb()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .metroNavBarPadding()
                .background(background),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MetroSettingsHeader(
                    pageTitle = title.lowercase(),
                    appTitle = "keyboard",
                )
                val scrollModifier = if (scrollable) {
                    Modifier.florisVerticalScroll()
                } else {
                    Modifier
                }
                PreferenceLayout(
                    FlorisPreferenceStore,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(scrollModifier),
                    iconSpaceReserved = iconSpaceReserved,
                    content = content,
                )
                bottomBar()
            }
            // Legacy top actions strip (edit/delete) when a screen registers actions.
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 4.dp),
                content = actions,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                fab()
            }
        }
    }
}
