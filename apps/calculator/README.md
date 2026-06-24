# Calculator

**Package:** `com.metro.calculator`  
**Tier:** 2

## Status

Harness docs only — Android project not scaffolded yet. This README is the implementation brief for the WP8.1 calculator.

## App role

This app recreates the WP8.1 **Calculator** in portrait mode with standard and scientific functionality presented in Metro styling.

It should feel like a clean, flat, high-contrast utility. Avoid turning it into a Material keypad app or a landscape-tablet scientific layout.

## Build gate

- Toolkits verified
- Tier 0 shell passes verify
- Core calculation/state engine defined before UI branching

## Screen inventory

### 1. Standard calculator surface

- Primary numeric input and result display
- Expected reference: `references/images/standard_dark_blue.png`

### 2. Scientific surface

- Extended operations surface
- May be implemented as a flip/pivot/panel transition depending on reference fidelity
- Expected reference: `references/images/scientific_dark_blue.png`

## System functions and contracts

- Calculation engine must be deterministic and test-heavy
- Explicitly define operator precedence, decimal behavior, sign toggle, clear rules, and error states
- If memory/history is not in v1, keep that omission explicit

## UI and interaction guardrails

- Large Noto Sans display
- Flat Metro buttons with accent-on-press states
- No raised Material buttons, cards, or dynamic shadows
- Portrait-first only in v1

## Data and state model

- `CalculatorState`: display text, operand stack/current values, pending operator, mode, error state
- Keep expression evaluation independent from composables

## Primary implementation order

1. Build pure calculation engine with tests
2. Build standard keypad and display
3. Add scientific functions
4. Add mode switching and layout polish

## Test-critical user flows

1. Basic arithmetic
2. Decimal and sign operations
3. Clear/backspace behavior
4. Scientific function evaluation
5. Error-state rendering and recovery

## Reference and golden expectations

- `references/images/standard_dark_blue.png`
- `references/images/scientific_dark_blue.png`

## Commands

```bash
cd apps/calculator

./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:test
./gradlew :app:connectedDebugAndroidTest

# From repo root
../../scripts/verify-app.sh calculator
```

## Agent entrypoint

[`AGENTS.md`](AGENTS.md)

## Platform exceptions

| WP8.1 behavior | Android limitation | Compromise |
|----------------|-------------------|------------|
| Exact OS calculator feature parity | Feature scope may exceed v1 utility goals | Prioritize standard and core scientific functions with exact Metro presentation before expanding feature set |

## Agent postmortem

_None._
