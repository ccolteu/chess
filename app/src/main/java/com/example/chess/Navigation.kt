package com.example.chess

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.chess.ui.game.GameScreen

@Composable
fun MainNavigation() {
  GameScreen(modifier = Modifier.safeDrawingPadding())
}
