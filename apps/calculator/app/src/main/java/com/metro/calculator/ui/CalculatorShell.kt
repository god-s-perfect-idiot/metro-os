package com.metro.calculator.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metro.calculator.data.AngleMode
import com.metro.calculator.data.CalculatorAction
import com.metro.calculator.data.CalculatorLogic
import com.metro.calculator.data.CalculatorMode
import com.metro.calculator.data.CalculatorState
import com.metro.ui.MetroColors
import com.metro.ui.MetroFontFamily
import com.metro.ui.MetroTheme
import com.metro.ui.metroNavBarPadding

private val ButtonBackground = Color(0xFF1C1C1C)
private val AngleSelectedBackground = Color(0xFF3A3A3A)

@Composable
fun CalculatorShell(
    state: CalculatorState,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val targetMode = if (isLandscape) CalculatorMode.SCIENTIFIC else CalculatorMode.STANDARD

    // WP8.1 calculator: portrait = standard, landscape = scientific. Sync the engine mode
    // to the device orientation rather than exposing tabs.
    LaunchedEffect(targetMode) {
        if (targetMode != state.mode) {
            onAction(CalculatorAction.SetMode(targetMode))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .metroNavBarPadding(),
    ) {
        CalculatorDisplay(
            operation = CalculatorLogic.operationText(state),
            value = state.display,
            isError = state.isError,
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (isLandscape) 0.18f else 0.34f),
        )

        if (isLandscape) {
            ScientificKeypad(
                angleMode = state.angleMode,
                onAction = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.82f),
            )
        } else {
            StandardKeypad(
                onAction = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.66f),
            )
        }
    }
}

@Composable
private fun CalculatorDisplay(
    operation: String,
    value: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val valueSize = when {
        value.length <= 8 -> 64.sp
        value.length <= 12 -> 48.sp
        else -> 34.sp
    }

    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        BasicText(
            text = operation,
            style = TextStyle(
                fontFamily = MetroFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 24.sp,
                color = MetroTheme.colors.secondaryText,
                textAlign = TextAlign.End,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        )
        BasicText(
            text = value,
            style = TextStyle(
                fontFamily = MetroFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = valueSize,
                color = if (isError) MetroColors.AccentRed else MetroTheme.colors.primaryText,
                textAlign = TextAlign.End,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StandardKeypad(
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember {
        listOf(
            listOf(
                key("C", CalculatorAction.Clear),
                key("MC", CalculatorAction.MemoryClear),
                key("MR", CalculatorAction.MemoryRecall),
                key("M+", CalculatorAction.MemoryAdd),
            ),
            listOf(
                key("⌫", CalculatorAction.Backspace),
                key("±", CalculatorAction.Negate),
                key("%", CalculatorAction.Percent),
                key("÷", CalculatorAction.Operator("÷")),
            ),
            digitRow('7', '8', '9', "×", CalculatorAction.Operator("×")),
            digitRow('4', '5', '6', "−", CalculatorAction.Operator("-")),
            digitRow('1', '2', '3', "+", CalculatorAction.Operator("+")),
            listOf(
                key("0", CalculatorAction.Digit('0'), span = 2),
                key(".", CalculatorAction.Digit('.')),
                key("=", CalculatorAction.Equals, isEquals = true),
            ),
        )
    }

    KeypadGrid(rows = rows, modifier = modifier, onAction = onAction)
}

@Composable
private fun ScientificKeypad(
    angleMode: AngleMode,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(angleMode) {
        listOf(
            listOf(
                key("(", CalculatorAction.LeftParen),
                key(")", CalculatorAction.RightParen),
                key("π", CalculatorAction.Pi),
                key("C", CalculatorAction.Clear),
                key("⌫", CalculatorAction.Backspace),
                key("±", CalculatorAction.Negate),
                key("÷", CalculatorAction.Operator("÷")),
                key("%", CalculatorAction.Percent),
            ),
            listOf(
                angleKey("Deg", AngleMode.DEG, angleMode),
                angleKey("Rad", AngleMode.RAD, angleMode),
                angleKey("Grad", AngleMode.GRAD, angleMode),
                key("7", CalculatorAction.Digit('7')),
                key("8", CalculatorAction.Digit('8')),
                key("9", CalculatorAction.Digit('9')),
                key("×", CalculatorAction.Operator("×")),
                funcKey("√", "√"),
            ),
            listOf(
                funcKey("sin", "sin"),
                funcKey("cos", "cos"),
                funcKey("tan", "tan"),
                key("4", CalculatorAction.Digit('4')),
                key("5", CalculatorAction.Digit('5')),
                key("6", CalculatorAction.Digit('6')),
                key("−", CalculatorAction.Operator("-")),
                key("MC", CalculatorAction.MemoryClear),
            ),
            listOf(
                funcKey("ln", "ln"),
                funcKey("log", "log"),
                funcKey("10ˣ", "10ˣ"),
                key("1", CalculatorAction.Digit('1')),
                key("2", CalculatorAction.Digit('2')),
                key("3", CalculatorAction.Digit('3')),
                key("+", CalculatorAction.Operator("+")),
                key("MR", CalculatorAction.MemoryRecall),
            ),
            listOf(
                funcKey("n!", "n!"),
                funcKey("x²", "x²"),
                key("xʸ", CalculatorAction.Operator("xʸ")),
                key("0", CalculatorAction.Digit('0'), span = 2),
                key(".", CalculatorAction.Digit('.')),
                key("=", CalculatorAction.Equals, isEquals = true),
                key("M+", CalculatorAction.MemoryAdd),
            ),
        )
    }

    KeypadGrid(rows = rows, modifier = modifier, onAction = onAction, compact = true)
}

private data class KeySpec(
    val label: String,
    val action: CalculatorAction,
    val span: Int = 1,
    val isEquals: Boolean = false,
    val isSelected: Boolean = false,
)

private fun key(
    label: String,
    action: CalculatorAction,
    span: Int = 1,
    isEquals: Boolean = false,
): KeySpec = KeySpec(label, action, span, isEquals)

private fun funcKey(label: String, name: String): KeySpec =
    KeySpec(label, CalculatorAction.Function(name))

private fun angleKey(label: String, mode: AngleMode, selected: AngleMode): KeySpec =
    KeySpec(label, CalculatorAction.SetAngleMode(mode), isSelected = mode == selected)

private fun digitRow(
    d1: Char,
    d2: Char,
    d3: Char,
    opLabel: String,
    opAction: CalculatorAction,
): List<KeySpec> = listOf(
    key(d1.toString(), CalculatorAction.Digit(d1)),
    key(d2.toString(), CalculatorAction.Digit(d2)),
    key(d3.toString(), CalculatorAction.Digit(d3)),
    key(opLabel, opAction),
)

@Composable
private fun KeypadGrid(
    rows: List<List<KeySpec>>,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MetroTheme.colors.background)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { spec ->
                    CalcButton(
                        spec = spec,
                        compact = compact,
                        onClick = { onAction(spec.action) },
                        modifier = Modifier.weight(spec.span.toFloat()),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalcButton(
    spec: KeySpec,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val accent = MetroTheme.colors.accent

    val background = when {
        spec.isEquals && pressed -> accent
        spec.isEquals -> MetroColors.AccentRed
        pressed -> accent
        spec.isSelected -> AngleSelectedBackground
        else -> ButtonBackground
    }

    val textColor = when {
        spec.isEquals -> Color.White
        pressed -> Color.White
        else -> MetroTheme.colors.primaryText
    }

    val fontSize = when {
        compact && spec.label.length >= 3 -> 17.sp
        compact -> 20.sp
        else -> 26.sp
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = spec.label,
            style = TextStyle(
                fontFamily = MetroFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = fontSize,
                color = textColor,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}
