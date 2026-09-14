package com.metro.hub.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.hub.R
import com.metro.ui.MetroBorderButton
import com.metro.ui.MetroColors
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroText
import com.metro.ui.MetroTextStyle
import com.metro.ui.MetroTheme

/** Brand red for Metro Ruby alphas — WP crimson, matching Lumia Cyan accent treatment. */
internal val MetroRubyColor: Color = MetroColors.AccentCrimson

private val ReleaseNameStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Light,
    fontSize = 36.sp,
    lineHeight = 42.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private val ComponentLineStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 26.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private val UnderlineLinkStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    textDecoration = TextDecoration.Underline,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Composable
fun ExtrasInfoScreen(
    state: HubState,
    modifier: Modifier = Modifier,
) {
    val generation = state.generation
    @Suppress("UNUSED_VARIABLE")
    val observe = generation

    val release = state.release
    val tag = release?.tagName?.takeIf { it.isNotBlank() }
    val releaseLabel = tag ?: stringResource(R.string.extras_release_unknown)
    val assetCount = state.allAssets.size
    val primary = MetroTheme.colors.primaryText
    val secondary = MetroTheme.colors.secondaryText

    Column(modifier = modifier.fillMaxSize()) {
        MetroText(
            text = stringResource(R.string.extras_info_title),
            style = MetroTextStyle.PageTitle,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 20.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
        ) {
            MetroText(
                text = stringResource(R.string.extras_software_release),
                style = MetroTextStyle.SectionHeader,
                color = secondary,
                modifier = Modifier.padding(bottom = 6.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 20.dp),
            ) {
                BasicText(
                    text = stringResource(R.string.extras_metro_ruby),
                    style = ReleaseNameStyle.copy(color = MetroRubyColor),
                    maxLines = 1,
                )
                RubyInfoGlyph(color = MetroRubyColor)
            }

            BasicText(
                text = stringResource(R.string.extras_source_on_github),
                style = UnderlineLinkStyle.copy(color = primary),
                modifier = Modifier
                    .clickable { state.openExternalUrl(HubState.GITHUB_URL) }
                    .padding(vertical = 4.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BasicText(
                text = stringResource(R.string.extras_intro),
                style = ComponentLineStyle.copy(color = primary),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // First list action — Buy Me a Coffee (border button, like WP extras chrome).
            MetroBorderButton(
                text = stringResource(R.string.extras_buy_me_a_coffee),
                onClick = { state.openExternalUrl(HubState.BUY_ME_A_COFFEE_URL) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            ComponentLine(
                text = stringResource(R.string.extras_line_latest_release, releaseLabel),
                color = primary,
            )
            ComponentLine(
                text = stringResource(R.string.extras_line_channel),
                color = primary,
            )
            ComponentLine(
                text = stringResource(R.string.extras_line_suite_apps, assetCount),
                color = primary,
            )
            ComponentLine(
                text = stringResource(R.string.extras_line_publisher),
                color = primary,
            )
            ComponentLine(
                text = stringResource(R.string.extras_line_platform),
                color = primary,
            )

            Spacer(modifier = Modifier.height(28.dp))

            MetroBorderButton(
                text = stringResource(R.string.extras_more_info),
                onClick = {
                    val url = if (tag != null) {
                        HubState.releaseUrlForTag(tag)
                    } else {
                        HubState.GITHUB_URL
                    }
                    state.openExternalUrl(url)
                },
            )
        }
    }
}

@Composable
private fun ComponentLine(
    text: String,
    color: Color,
) {
    BasicText(
        text = text,
        style = ComponentLineStyle.copy(color = color),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

@Composable
private fun RubyInfoGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .border(width = 1.5.dp, color = color, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = "i",
            style = TextStyle(
                fontFamily = MetroFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                color = color,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}
