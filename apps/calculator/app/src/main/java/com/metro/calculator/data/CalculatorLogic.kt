package com.metro.calculator.data

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class CalculatorMode {
    STANDARD,
    SCIENTIFIC,
}

enum class AngleMode {
    DEG,
    RAD,
    GRAD,
}

data class CalculatorState(
    val display: String = "0",
    val isError: Boolean = false,
    val memory: Double = 0.0,
    val memorySet: Boolean = false,
    val angleMode: AngleMode = AngleMode.DEG,
    val mode: CalculatorMode = CalculatorMode.STANDARD,
    val standardAccumulator: Double? = null,
    val standardPendingOp: String? = null,
    val standardFreshEntry: Boolean = true,
    val scientificExpression: String = "",
    val scientificWaitingOperand: Boolean = false,
)

sealed class CalculatorAction {
    data class Digit(val char: Char) : CalculatorAction()
    data class Operator(val symbol: String) : CalculatorAction()
    data class Function(val name: String) : CalculatorAction()
    data class SetAngleMode(val mode: AngleMode) : CalculatorAction()
    data object Clear : CalculatorAction()
    data object Backspace : CalculatorAction()
    data object Negate : CalculatorAction()
    data object Percent : CalculatorAction()
    data object Equals : CalculatorAction()
    data object MemoryClear : CalculatorAction()
    data object MemoryRecall : CalculatorAction()
    data object MemoryAdd : CalculatorAction()
    data object Pi : CalculatorAction()
    data object LeftParen : CalculatorAction()
    data object RightParen : CalculatorAction()
    data class SetMode(val mode: CalculatorMode) : CalculatorAction()
}

object CalculatorLogic {
    fun reduce(state: CalculatorState, action: CalculatorAction): CalculatorState {
        if (state.isError) {
            return when (action) {
                is CalculatorAction.Clear -> clearAll(state)
                is CalculatorAction.Digit -> clearAll(state).let { reduce(it, action) }
                else -> state
            }
        }

        return when (action) {
            is CalculatorAction.Digit -> onDigit(state, action.char)
            is CalculatorAction.Operator -> onOperator(state, action.symbol)
            is CalculatorAction.Function -> onFunction(state, action.name)
            is CalculatorAction.SetAngleMode -> state.copy(angleMode = action.mode)
            is CalculatorAction.Clear -> clearAll(state)
            is CalculatorAction.Backspace -> onBackspace(state)
            is CalculatorAction.Negate -> onNegate(state)
            is CalculatorAction.Percent -> onPercent(state)
            is CalculatorAction.Equals -> onEquals(state)
            is CalculatorAction.MemoryClear -> state.copy(memory = 0.0, memorySet = false)
            is CalculatorAction.MemoryRecall -> {
                if (!state.memorySet) state
                else state.copy(
                    display = formatNumber(state.memory),
                    standardFreshEntry = true,
                    scientificWaitingOperand = true,
                    scientificExpression = "",
                )
            }
            is CalculatorAction.MemoryAdd -> {
                val value = parseDisplay(state.display) ?: return errorState(state)
                state.copy(memory = state.memory + value, memorySet = true)
            }
            is CalculatorAction.Pi -> onPi(state)
            is CalculatorAction.LeftParen -> onLeftParen(state)
            is CalculatorAction.RightParen -> onRightParen(state)
            is CalculatorAction.SetMode -> onSetMode(state, action.mode)
        }
    }

    private fun onSetMode(state: CalculatorState, mode: CalculatorMode): CalculatorState {
        if (state.mode == mode) return state
        // Preserve the displayed value across a rotation (WP8.1 keeps the number);
        // only the pending operation chain is reset because the two modes evaluate differently.
        return state.copy(
            mode = mode,
            standardAccumulator = null,
            standardPendingOp = null,
            standardFreshEntry = true,
            scientificExpression = "",
            scientificWaitingOperand = true,
        )
    }

    /** Secondary line shown above the main display: the pending operation / expression so far. */
    fun operationText(state: CalculatorState): String {
        if (state.isError) return ""
        return when (state.mode) {
            CalculatorMode.STANDARD -> {
                val acc = state.standardAccumulator
                val op = state.standardPendingOp
                if (acc != null && op != null) "${formatNumber(acc)} $op" else ""
            }
            CalculatorMode.SCIENTIFIC -> currentScientificExpression(state)
        }
    }

    private fun clearAll(state: CalculatorState): CalculatorState {
        return state.copy(
            display = "0",
            isError = false,
            standardAccumulator = null,
            standardPendingOp = null,
            standardFreshEntry = true,
            scientificExpression = "",
            scientificWaitingOperand = false,
        )
    }

    private fun errorState(state: CalculatorState): CalculatorState {
        return state.copy(display = "Error", isError = true)
    }

    private fun onDigit(state: CalculatorState, char: Char): CalculatorState {
        val display = when {
            state.mode == CalculatorMode.STANDARD && state.standardFreshEntry -> char.toString()
            state.mode == CalculatorMode.SCIENTIFIC && state.scientificWaitingOperand -> char.toString()
            state.display == "0" && char != '.' -> char.toString()
            else -> state.display + char
        }
        return state.copy(
            display = display,
            standardFreshEntry = false,
            scientificWaitingOperand = false,
        )
    }

    private fun onBackspace(state: CalculatorState): CalculatorState {
        val display = when {
            state.display.length <= 1 -> "0"
            else -> state.display.dropLast(1)
        }
        return state.copy(display = display)
    }

    private fun onNegate(state: CalculatorState): CalculatorState {
        val value = parseDisplay(state.display) ?: return errorState(state)
        return state.copy(display = formatNumber(-value))
    }

    private fun onPercent(state: CalculatorState): CalculatorState {
        val value = parseDisplay(state.display) ?: return errorState(state)
        return state.copy(display = formatNumber(value / 100.0))
    }

    private fun onPi(state: CalculatorState): CalculatorState {
        return state.copy(
            display = formatNumber(Math.PI),
            standardFreshEntry = true,
            scientificWaitingOperand = false,
        )
    }

    private fun onLeftParen(state: CalculatorState): CalculatorState {
        val expr = state.scientificExpression + "("
        return state.copy(
            scientificExpression = expr,
            display = "0",
            scientificWaitingOperand = false,
        )
    }

    private fun onRightParen(state: CalculatorState): CalculatorState {
        val expr = currentScientificExpression(state) + ")"
        return state.copy(
            scientificExpression = expr,
            display = "0",
            scientificWaitingOperand = true,
        )
    }

    private fun onOperator(state: CalculatorState, symbol: String): CalculatorState {
        return when (state.mode) {
            CalculatorMode.STANDARD -> onStandardOperator(state, symbol)
            CalculatorMode.SCIENTIFIC -> onScientificOperator(state, symbol)
        }
    }

    private fun onStandardOperator(state: CalculatorState, symbol: String): CalculatorState {
        val value = parseDisplay(state.display) ?: return errorState(state)
        val acc = state.standardAccumulator
        val pending = state.standardPendingOp

        val newAcc = if (acc != null && pending != null && !state.standardFreshEntry) {
            try {
                applyStandard(acc, pending, value)
            } catch (_: ArithmeticException) {
                return errorState(state)
            }
        } else {
            value
        }

        return state.copy(
            display = formatNumber(newAcc),
            standardAccumulator = newAcc,
            standardPendingOp = symbol,
            standardFreshEntry = true,
        )
    }

    private fun onScientificOperator(state: CalculatorState, symbol: String): CalculatorState {
        val expr = currentScientificExpression(state) + toExprOperator(symbol)
        return state.copy(
            scientificExpression = expr,
            scientificWaitingOperand = true,
        )
    }

    private fun onFunction(state: CalculatorState, name: String): CalculatorState {
        val value = parseDisplay(state.display) ?: return errorState(state)
        val result = try {
            applyUnary(name, value, state.angleMode)
        } catch (_: ArithmeticException) {
            return errorState(state)
        }
        return state.copy(
            display = formatNumber(result),
            standardFreshEntry = true,
            scientificExpression = formatNumber(result),
            scientificWaitingOperand = true,
        )
    }

    private fun onEquals(state: CalculatorState): CalculatorState {
        return when (state.mode) {
            CalculatorMode.STANDARD -> onStandardEquals(state)
            CalculatorMode.SCIENTIFIC -> onScientificEquals(state)
        }
    }

    private fun onStandardEquals(state: CalculatorState): CalculatorState {
        val value = parseDisplay(state.display) ?: return errorState(state)
        val acc = state.standardAccumulator
        val pending = state.standardPendingOp ?: return state

        val result = try {
            applyStandard(acc ?: value, pending, value)
        } catch (_: ArithmeticException) {
            return errorState(state)
        }

        return state.copy(
            display = formatNumber(result),
            standardAccumulator = null,
            standardPendingOp = null,
            standardFreshEntry = true,
        )
    }

    private fun onScientificEquals(state: CalculatorState): CalculatorState {
        val expr = currentScientificExpression(state)
        if (expr.isBlank()) return state

        val result = try {
            CalculatorExpression.evaluate(expr, state.angleMode)
        } catch (_: ArithmeticException) {
            return errorState(state)
        } catch (_: IllegalArgumentException) {
            return errorState(state)
        }

        return state.copy(
            display = formatNumber(result),
            scientificExpression = "",
            scientificWaitingOperand = true,
        )
    }

    fun currentScientificExpression(state: CalculatorState): String {
        return if (state.scientificWaitingOperand) {
            state.scientificExpression
        } else {
            state.scientificExpression + state.display
        }
    }

    fun applyStandard(left: Double, op: String, right: Double): Double {
        return when (op) {
            "+" -> left + right
            "-" -> left - right
            "×" -> left * right
            "÷" -> {
                if (right == 0.0) throw ArithmeticException()
                left / right
            }
            "^" -> left.pow(right)
            else -> throw IllegalArgumentException("Unknown operator: $op")
        }
    }

    fun applyUnary(name: String, value: Double, angleMode: AngleMode): Double {
        return when (name) {
            "sin" -> sin(toRadians(value, angleMode))
            "cos" -> cos(toRadians(value, angleMode))
            "tan" -> tan(toRadians(value, angleMode))
            "ln" -> {
                if (value <= 0.0) throw ArithmeticException()
                ln(value)
            }
            "log" -> {
                if (value <= 0.0) throw ArithmeticException()
                log10(value)
            }
            "10ˣ" -> 10.0.pow(value)
            "x²" -> value * value
            "xʸ" -> value // handled as binary operator
            "n!" -> factorial(value)
            "√" -> {
                if (value < 0.0) throw ArithmeticException()
                sqrt(value)
            }
            else -> throw IllegalArgumentException("Unknown function: $name")
        }
    }

    fun factorial(value: Double): Double {
        if (value < 0.0 || value != value.toLong().toDouble() || value > 170) {
            throw ArithmeticException()
        }
        var result = 1.0
        for (i in 2..value.toLong()) {
            result *= i
        }
        return result
    }

    fun toRadians(value: Double, angleMode: AngleMode): Double {
        return when (angleMode) {
            AngleMode.DEG -> Math.toRadians(value)
            AngleMode.RAD -> value
            AngleMode.GRAD -> value * (Math.PI / 200.0)
        }
    }

    fun parseDisplay(display: String): Double? {
        if (display == "Error") return null
        return display.toDoubleOrNull()
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        if (abs(value) >= 1e15) return "Error"

        val rounded = if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            val text = "%.10f".format(value).trimEnd('0').trimEnd('.')
            if (text.length > 16) {
                "%.10g".format(value)
            } else {
                text
            }
        }
        return rounded
    }

    private fun toExprOperator(symbol: String): String = when (symbol) {
        // Keep × and ÷ in the stored expression so the operation line reads cleanly;
        // CalculatorExpression.normalize() converts them at evaluation time.
        "xʸ" -> "^"
        else -> symbol
    }
}

object CalculatorExpression {
    fun evaluate(expression: String, angleMode: AngleMode): Double {
        val tokens = tokenize(normalize(expression))
        val rpn = toRpn(tokens)
        return evalRpn(rpn, angleMode)
    }

    private fun normalize(expression: String): String {
        return expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "pi")
    }

    private sealed class Token {
        data class Number(val value: Double) : Token()
        data class Op(val symbol: String) : Token()
        data class Func(val name: String) : Token()
        data object LeftParen : Token()
        data object RightParen : Token()
    }

    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < input.length) {
            when (val ch = input[i]) {
                in "0123456789." -> {
                    val start = i
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) i++
                    tokens.add(Token.Number(input.substring(start, i).toDouble()))
                    continue
                }
                '(' -> tokens.add(Token.LeftParen)
                ')' -> tokens.add(Token.RightParen)
                '+', '-', '*', '/', '^', '%' -> tokens.add(Token.Op(ch.toString()))
                else -> {
                    val start = i
                    while (i < input.length && input[i].isLetter()) i++
                    val name = input.substring(start, i)
                    if (name == "pi") {
                        tokens.add(Token.Number(Math.PI))
                    } else {
                        tokens.add(Token.Func(name))
                    }
                    continue
                }
            }
            i++
        }
        return tokens
    }

    private fun precedence(op: String): Int = when (op) {
        "+", "-" -> 1
        "*", "/", "%" -> 2
        "^" -> 3
        else -> 0
    }

    private fun toRpn(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = ArrayDeque<Token>()

        for (token in tokens) {
            when (token) {
                is Token.Number -> output.add(token)
                is Token.Func -> stack.addLast(token)
                is Token.Op -> {
                    while (stack.isNotEmpty()) {
                        val top = stack.last()
                        if (top is Token.Op && precedence(top.symbol) >= precedence(token.symbol)) {
                            output.add(stack.removeLast())
                        } else {
                            break
                        }
                    }
                    stack.addLast(token)
                }
                Token.LeftParen -> stack.addLast(token)
                Token.RightParen -> {
                    while (stack.isNotEmpty() && stack.last() !is Token.LeftParen) {
                        output.add(stack.removeLast())
                    }
                    if (stack.isEmpty()) throw IllegalArgumentException("Mismatched parentheses")
                    stack.removeLast()
                    if (stack.isNotEmpty() && stack.last() is Token.Func) {
                        output.add(stack.removeLast())
                    }
                }
            }
        }

        while (stack.isNotEmpty()) {
            val top = stack.removeLast()
            if (top is Token.LeftParen) throw IllegalArgumentException("Mismatched parentheses")
            output.add(top)
        }
        return output
    }

    private fun evalRpn(tokens: List<Token>, angleMode: AngleMode): Double {
        val stack = ArrayDeque<Double>()

        for (token in tokens) {
            when (token) {
                is Token.Number -> stack.addLast(token.value)
                is Token.Func -> {
                    if (stack.isEmpty()) throw ArithmeticException()
                    val value = stack.removeLast()
                    stack.addLast(CalculatorLogic.applyUnary(token.name, value, angleMode))
                }
                is Token.Op -> {
                    if (stack.size < 2) throw ArithmeticException()
                    val right = stack.removeLast()
                    val left = stack.removeLast()
                    stack.addLast(CalculatorLogic.applyStandard(left, fromExprOp(token.symbol), right))
                }
                else -> throw IllegalArgumentException("Unexpected token in RPN")
            }
        }

        if (stack.size != 1) throw ArithmeticException()
        return stack.last()
    }

    private fun fromExprOp(symbol: String): String = when (symbol) {
        "*" -> "×"
        "/" -> "÷"
        "%" -> "×" // percent as multiply by 0.01 handled at input; treat % in expr as mod fallback
        else -> symbol
    }
}
