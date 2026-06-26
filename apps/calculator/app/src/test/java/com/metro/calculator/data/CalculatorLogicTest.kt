package com.metro.calculator.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorLogicTest {
  private fun state(mode: CalculatorMode = CalculatorMode.STANDARD) =
      CalculatorState(mode = mode)

  private fun press(
      initial: CalculatorState,
      vararg actions: CalculatorAction,
  ): CalculatorState {
    return actions.fold(initial) { s, a -> CalculatorLogic.reduce(s, a) }
  }

  @Test
  fun standard_leftToRight_addThenMultiply() {
    val result = press(
        state(),
        CalculatorAction.Digit('3'),
        CalculatorAction.Operator("+"),
        CalculatorAction.Digit('5'),
        CalculatorAction.Operator("×"),
        CalculatorAction.Digit('8'),
        CalculatorAction.Equals,
    )
    assertEquals("64", result.display)
  }

  @Test
  fun scientific_precedence_addThenMultiply() {
    val result = press(
        state(CalculatorMode.SCIENTIFIC),
        CalculatorAction.Digit('3'),
        CalculatorAction.Operator("+"),
        CalculatorAction.Digit('5'),
        CalculatorAction.Operator("×"),
        CalculatorAction.Digit('8'),
        CalculatorAction.Equals,
    )
    assertEquals("43", result.display)
  }

  @Test
  fun standard_basicAddition() {
    val result = press(
        state(),
        CalculatorAction.Digit('2'),
        CalculatorAction.Operator("+"),
        CalculatorAction.Digit('3'),
        CalculatorAction.Equals,
    )
    assertEquals("5", result.display)
  }

  @Test
  fun standard_clearResetsDisplay() {
    val result = press(
        state(),
        CalculatorAction.Digit('9'),
        CalculatorAction.Clear,
    )
    assertEquals("0", result.display)
    assertFalse(result.isError)
  }

  @Test
  fun standard_backspaceRemovesDigit() {
    val result = press(
        state(),
        CalculatorAction.Digit('1'),
        CalculatorAction.Digit('2'),
        CalculatorAction.Backspace,
    )
    assertEquals("1", result.display)
  }

  @Test
  fun standard_backspaceOnSingleDigitBecomesZero() {
    val result = press(
        state(),
        CalculatorAction.Digit('7'),
        CalculatorAction.Backspace,
    )
    assertEquals("0", result.display)
  }

  @Test
  fun standard_negateTogglesSign() {
    val result = press(
        state(),
        CalculatorAction.Digit('4'),
        CalculatorAction.Negate,
    )
    assertEquals("-4", result.display)
  }

  @Test
  fun standard_percentDividesByHundred() {
    val result = press(
        state(),
        CalculatorAction.Digit('5'),
        CalculatorAction.Digit('0'),
        CalculatorAction.Percent,
    )
    assertEquals("0.5", result.display)
  }

  @Test
  fun standard_divideByZeroShowsError() {
    val result = press(
        state(),
        CalculatorAction.Digit('8'),
        CalculatorAction.Operator("÷"),
        CalculatorAction.Digit('0'),
        CalculatorAction.Equals,
    )
    assertEquals("Error", result.display)
    assertTrue(result.isError)
  }

  @Test
  fun memory_storeRecallAndClear() {
    var s = press(
        state(),
        CalculatorAction.Digit('4'),
        CalculatorAction.MemoryAdd,
    )
    assertTrue(s.memorySet)
    assertEquals(4.0, s.memory, 0.0001)

    s = press(s, CalculatorAction.Clear, CalculatorAction.MemoryRecall)
    assertEquals("4", s.display)

    s = press(s, CalculatorAction.MemoryClear)
    assertFalse(s.memorySet)
  }

  @Test
  fun scientific_sinInDegrees() {
    val result = press(
        state(CalculatorMode.SCIENTIFIC),
        CalculatorAction.Digit('9'),
        CalculatorAction.Digit('0'),
        CalculatorAction.Function("sin"),
    )
    assertEquals("1", result.display)
  }

  @Test
  fun scientific_factorial() {
    val result = press(
        state(CalculatorMode.SCIENTIFIC),
        CalculatorAction.Digit('5'),
        CalculatorAction.Function("n!"),
    )
    assertEquals("120", result.display)
  }

  @Test
  fun scientific_squareRoot() {
    val result = press(
        state(CalculatorMode.SCIENTIFIC),
        CalculatorAction.Digit('9'),
        CalculatorAction.Function("√"),
    )
    assertEquals("3", result.display)
  }

  @Test
  fun scientific_parentheses() {
    val s = press(
        state(CalculatorMode.SCIENTIFIC),
        CalculatorAction.LeftParen,
        CalculatorAction.Digit('2'),
        CalculatorAction.Operator("+"),
        CalculatorAction.Digit('3'),
        CalculatorAction.RightParen,
        CalculatorAction.Operator("×"),
        CalculatorAction.Digit('4'),
        CalculatorAction.Equals,
    )
    assertEquals("20", s.display)
  }

  @Test
  fun formatNumber_stripsTrailingZeros() {
    assertEquals("1.5", CalculatorLogic.formatNumber(1.5))
    assertEquals("42", CalculatorLogic.formatNumber(42.0))
  }

  @Test
  fun modeSwitch_preservesDisplayAndResetsPendingOperation() {
    val s = press(
        state(),
        CalculatorAction.Digit('9'),
        CalculatorAction.Operator("+"),
        CalculatorAction.SetMode(CalculatorMode.SCIENTIFIC),
    )
    // Rotation keeps the shown value but drops the half-entered standard operation.
    assertEquals("9", s.display)
    assertEquals(CalculatorMode.SCIENTIFIC, s.mode)

    // The next digit starts a fresh operand rather than appending to "9".
    val next = press(s, CalculatorAction.Digit('2'))
    assertEquals("2", next.display)
  }

  @Test
  fun operationText_showsPendingStandardOperation() {
    val s = press(
        state(),
        CalculatorAction.Digit('1'),
        CalculatorAction.Digit('2'),
        CalculatorAction.Operator("×"),
    )
    assertEquals("12 ×", CalculatorLogic.operationText(s))
  }

  @Test
  fun operationText_showsScientificExpression() {
    val s = press(
        state(CalculatorMode.SCIENTIFIC),
        CalculatorAction.Digit('3'),
        CalculatorAction.Operator("+"),
        CalculatorAction.Digit('5'),
    )
    assertEquals("3+5", CalculatorLogic.operationText(s))
  }
}
