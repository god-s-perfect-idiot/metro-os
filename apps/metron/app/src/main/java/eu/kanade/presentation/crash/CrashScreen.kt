package eu.kanade.presentation.crash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.ui.MetroSystemTheme
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Windows Phone 8.1 / Windows 8–style blue sad-face crash surface. */
private val BsodBlue = Color(0xFF0078D7)
private val BsodWhite = Color(0xFFFFFFFF)

@Composable
fun CrashScreen(
    exception: Throwable?,
    onRestartClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val crashLogUtil = remember { context.appGraph.crashLogUtil }
    val stopCode = remember(exception) {
        exception?.javaClass?.simpleName?.uppercase()?.takeIf { it.isNotBlank() }
            ?: "UNEXPECTED_ERROR"
    }
    val detail = remember(exception) {
        exception?.message?.takeIf { it.isNotBlank() } ?: exception?.toString().orEmpty()
    }

    MetroSystemTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BsodBlue)
                .statusBarsPadding()
                .metroNavBarPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BasicText(
                    text = ":(",
                    style = TextStyle(
                        fontFamily = MetroTheme.fontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 96.sp,
                        lineHeight = 100.sp,
                        color = BsodWhite,
                    ),
                )
                BasicText(
                    text = stringResource(MR.strings.crash_screen_title),
                    style = TextStyle(
                        fontFamily = MetroTheme.fontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        color = BsodWhite,
                    ),
                )
                BasicText(
                    text = stringResource(
                        MR.strings.crash_screen_description,
                        stringResource(MR.strings.app_name),
                    ),
                    style = TextStyle(
                        fontFamily = MetroTheme.fontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        color = BsodWhite,
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                BasicText(
                    text = "Stop code: $stopCode",
                    style = TextStyle(
                        fontFamily = MetroTheme.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = BsodWhite,
                    ),
                )
                if (detail.isNotBlank()) {
                    BasicText(
                        text = detail,
                        style = TextStyle(
                            fontFamily = MetroTheme.fontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            color = BsodWhite.copy(alpha = 0.85f),
                        ),
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            BsodActionButton(
                text = stringResource(MR.strings.crash_screen_restart_application).lowercase(),
                onClick = onRestartClick,
            )
            Spacer(modifier = Modifier.height(12.dp))
            BsodActionButton(
                text = stringResource(MR.strings.pref_dump_crash_logs).lowercase(),
                onClick = {
                    scope.launch {
                        crashLogUtil.dumpLogs(exception)
                    }
                },
            )
        }
    }
}

@Composable
private fun BsodActionButton(
    text: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth(0.55f)
            .height(44.dp)
            .background(
                if (pressed) BsodWhite.copy(alpha = 0.2f) else Color.Transparent,
                RectangleShape,
            )
            .border(width = 2.dp, color = BsodWhite, shape = RectangleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                fontFamily = MetroTheme.fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = BsodWhite,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
private fun CrashScreenPreview() {
    CrashScreen(exception = RuntimeException("Dummy stop")) {}
}
