package com.metro.calculator

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.metro.calculator.data.CalculatorAction
import com.metro.calculator.data.CalculatorLogic
import com.metro.calculator.data.CalculatorState
import com.metro.calculator.ui.CalculatorShell
import com.metro.ui.MetroTheme

class MainActivity : ComponentActivity() {
    // Held on the Activity so configChanges rotation keeps the running calculation.
    private var state by mutableStateOf(CalculatorState())
    private var configurationEpoch by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Recompose when orientation/size changes under configChanges.
            configurationEpoch
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationEpoch++
    }
}
