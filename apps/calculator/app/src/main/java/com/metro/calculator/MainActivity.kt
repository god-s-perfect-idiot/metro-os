package com.metro.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.metro.calculator.data.CalculatorAction
import com.metro.calculator.data.CalculatorLogic
import com.metro.calculator.data.CalculatorState
import com.metro.calculator.ui.CalculatorShell
import com.metro.ui.MetroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var state by remember { mutableStateOf(CalculatorState()) }

            MetroTheme {
                CalculatorShell(
                    state = state,
                    onAction = { action ->
                        state = CalculatorLogic.reduce(state, action)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
