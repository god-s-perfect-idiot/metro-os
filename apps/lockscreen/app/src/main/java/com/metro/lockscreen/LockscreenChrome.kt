package com.metro.lockscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.ui.MetroDimens
import com.metro.ui.MetroFontFamily

/** Tight metrics so lock chrome stacks like WP8.1 (no extra font padding gap). */
private val LockChromeLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

/**
 * Clock glyphs sit high in the font box; bottom-align + aggressive line height still leaves
 * empty space under digits — day/date are pulled up with [LockClockToDayOverlap].
 */
private val LockTimeLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Bottom,
    trim = LineHeightStyle.Trim.Both,
)

private val LockChromePlatformStyle = PlatformTextStyle(includeFontPadding = false)

/** Pull day/date up into the clock's unused descender space. */
private val LockClockToDayOverlap = (-22).dp

/** WP8.1 lock clock — thin Segoe-like face, oversized vs page titles. */
private val LockTimeStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.ExtraLight,
    fontSize = 120.sp,
    lineHeight = 88.sp,
    letterSpacing = (-2).sp,
    platformStyle = LockChromePlatformStyle,
    lineHeightStyle = LockTimeLineHeightStyle,
)

private val LockDayDateStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Light,
    fontSize = 34.sp,
    lineHeight = 34.sp,
    platformStyle = LockChromePlatformStyle,
    lineHeightStyle = LockChromeLineHeightStyle,
)

private val LockEventStyle = TextStyle(
    fontFamily = MetroFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 26.sp,
    lineHeight = 30.sp,
    platformStyle = LockChromePlatformStyle,
    lineHeightStyle = LockChromeLineHeightStyle,
)

/**
 * Left-aligned lock chrome: large time, weekday, date, optional next calendar event.
 * Matches `references/images/lock_bing_homepage_dark.jpg` + user WP8.1 captures.
 *
 * Event lines stay on one line and clip at the trailing edge (WP overflow-off-screen),
 * so they use the full row width instead of wrapping early.
 */
@Composable
fun LockscreenChrome(
    labels: LockscreenChromeLogic.ChromeLabels,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Start inset only — trailing edge is the screen edge (WP overflow).
            .padding(start = MetroDimens.ScreenHorizontalMargin + 12.dp),
    ) {
        BasicText(
            text = labels.time,
            style = LockTimeStyle.copy(color = contentColor),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
        BasicText(
            text = labels.day,
            style = LockDayDateStyle.copy(color = contentColor),
            modifier = Modifier.offset(y = LockClockToDayOverlap),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
        BasicText(
            text = labels.date,
            style = LockDayDateStyle.copy(color = contentColor),
            modifier = Modifier.offset(y = LockClockToDayOverlap),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )

        val event = labels.event
        if (event != null) {
            Spacer(modifier = Modifier.height(28.dp + LockClockToDayOverlap))
            BasicText(
                text = event.title,
                style = LockEventStyle.copy(color = contentColor),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
            val location = event.location
            if (!location.isNullOrBlank()) {
                BasicText(
                    text = location,
                    style = LockEventStyle.copy(color = contentColor),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }
            BasicText(
                text = event.timeLabel,
                style = LockEventStyle.copy(color = contentColor),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
