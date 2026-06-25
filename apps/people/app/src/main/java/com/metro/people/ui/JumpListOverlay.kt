package com.metro.people.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metro.ui.MetroLetterTile

@Composable
fun JumpListOverlay(
    letters: List<Char>,
    onLetterSelected: (Char) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(onClick = onDismiss),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            items(letters) { letter ->
                MetroLetterTile(
                    letter = letter,
                    size = 64.dp,
                    onClick = {
                        onLetterSelected(letter)
                        onDismiss()
                    },
                    modifier = Modifier.padding(6.dp),
                )
            }
        }
    }
}
