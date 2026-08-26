package com.example.chess.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ChessColorScheme =
  lightColorScheme(
    primary = Brass,
    onPrimary = Ink,
    secondary = WalnutRail,
    onSecondary = Cream,
    background = WalnutBackground,
    onBackground = Cream,
    surface = Parchment,
    onSurface = Ink,
    surfaceVariant = Color(0xFFD9C4A0),
    onSurfaceVariant = Color(0xFF5C4030),
    outline = Brass,
  )

@Composable
fun ChessTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = ChessColorScheme, typography = Typography, content = content)
}
