package com.metro.settings.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.metro.settings.R
import com.metro.system.MetroIconPacks
import com.metro.system.MetroStartBackground
import com.metro.system.MetroTypeface
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroDimens
import com.metro.ui.MetroListPicker
import com.metro.ui.MetroListPickerOption
import com.metro.ui.MetroSettingsHeader
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme
import com.metro.ui.MetroToggleSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** WP8.1 Start background preview square (references/images/start_theme_background_*.png). */
private val StartBackgroundThumbSize = 108.dp

/** Empty thumbnail fill when no Start background is set. */
private val StartBackgroundPlaceholderGray = Color(0xFF6E6E6E)

@Composable
fun StartThemeScreen(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    val accentPhrase = stringResource(R.string.settings_start_theme_intro_accent)
    val introPrefix = stringResource(R.string.settings_start_theme_intro_prefix)
    val introSuffix = stringResource(R.string.settings_start_theme_intro_suffix)
    val accent = MetroTheme.colors.accent
    val intro = remember(introPrefix, accentPhrase, introSuffix, accent) {
        buildAnnotatedString {
            append(introPrefix)
            withStyle(SpanStyle(color = accent)) {
                append(accentPhrase)
            }
            append(introSuffix)
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            state.beginStartBackgroundCrop(uri)
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        MetroSettingsHeader(pageTitle = stringResource(R.string.settings_start_theme))

        MetroText(
            text = intro,
            style = MetroTextStyle.Body,
            modifier = Modifier.padding(
                start = MetroDimens.ScreenHorizontalMargin,
                end = MetroDimens.ScreenHorizontalMargin,
                bottom = 28.dp,
            ),
        )

        SettingsFieldLabel(text = stringResource(R.string.settings_accent_label))

        Row(
            modifier = Modifier
                .padding(horizontal = MetroDimens.ScreenHorizontalMargin)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .border(2.dp, MetroTheme.colors.primaryText)
                .clickable { state.open(SettingsRoute.AccentPicker) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(state.accentColor),
            )
            Spacer(modifier = Modifier.width(12.dp))
            MetroText(
                text = state.accentDisplayName,
                style = MetroTextStyle.Body,
            )
        }

        SettingsSpacer(height = 28)

        MetroListPicker(
            selected = state.typeface,
            options = listOf(
                MetroListPickerOption(
                    value = MetroTypeface.MetroNoto,
                    label = stringResource(R.string.settings_font_metro_noto),
                ),
                MetroListPickerOption(
                    value = MetroTypeface.SourceSans3,
                    label = stringResource(R.string.settings_font_source_sans_3),
                ),
                MetroListPickerOption(
                    value = MetroTypeface.AlegreyaSans,
                    label = stringResource(R.string.settings_font_alegreya_sans),
                ),
            ),
            onSelectedChange = state::applyTypeface,
            label = stringResource(R.string.settings_font_label),
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )

        SettingsSpacer(height = 28)

        val context = LocalContext.current
        val noneLabel = stringResource(R.string.settings_icon_pack_none)
        val iconPackLabel = remember(state.iconPackPackage, noneLabel) {
            val pkg = state.iconPackPackage
            if (pkg == null) {
                noneLabel
            } else {
                MetroIconPacks.labelFor(context, pkg) ?: pkg
            }
        }
        MetroListPicker(
            selected = state.iconPackPackage,
            options = emptyList<MetroListPickerOption<String?>>(),
            onSelectedChange = {},
            label = stringResource(R.string.settings_icon_pack_label),
            placeholder = iconPackLabel,
            onOpen = { state.open(SettingsRoute.IconPackPicker) },
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )
        SettingsHelpText(text = stringResource(R.string.settings_icon_pack_help))

        SettingsSpacer(height = 28)

        StartBackgroundRow(
            enabled = state.startBackgroundEnabled,
            reloadEpoch = state.startBackgroundEpoch,
            onChoosePhoto = {
                pickPhoto.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onRemove = state::clearStartBackground,
        )

        SettingsSpacer(height = 28)

        MetroToggleSwitch(
            checked = state.showMoreColumns,
            onCheckedChange = state::applyShowMoreColumns,
            label = stringResource(R.string.settings_show_more_columns),
            modifier = Modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        )
        SettingsHelpText(text = stringResource(R.string.settings_show_more_columns_help))
    }
}

/**
 * WP8.1 Start background block: square thumb | label + choose photo (+ remove when set).
 * Reference: `start_theme_background_unset_dark_cobalt.png`, `start_theme_background_set_dark_yellow.png`.
 */
@Composable
private fun StartBackgroundRow(
    enabled: Boolean,
    reloadEpoch: Int,
    onChoosePhoto: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var thumbBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(enabled, reloadEpoch) {
        thumbBitmap = if (enabled) {
            withContext(Dispatchers.IO) { MetroStartBackground.decode(context) }
        } else {
            null
        }
    }

    Row(
        modifier = modifier.padding(horizontal = MetroDimens.ScreenHorizontalMargin),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(StartBackgroundThumbSize)
                .background(StartBackgroundPlaceholderGray),
        ) {
            thumbBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = stringResource(R.string.settings_start_background_label),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f, fill = false)) {
            MetroText(
                text = stringResource(R.string.settings_start_background_label),
                style = MetroTextStyle.Body,
            )
            Spacer(modifier = Modifier.size(10.dp))
            MetroBorderButton(
                text = stringResource(R.string.settings_start_background_choose),
                onClick = onChoosePhoto,
            )
            if (enabled) {
                Spacer(modifier = Modifier.size(12.dp))
                val removeLabel = stringResource(R.string.settings_start_background_remove)
                MetroText(
                    text = remember(removeLabel) {
                        buildAnnotatedString {
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append(removeLabel)
                            }
                        }
                    },
                    style = MetroTextStyle.Body,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onRemove,
                        )
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}
