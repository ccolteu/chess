package com.cc.chess.ui.game

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class TabletLayoutTest {
  @Test
  fun landscapeBoardMatchesOuterMarginOnAllSides() {
    val side = tabletBoardSide(1280.dp, 800.dp, landscape = true, hudGutter = 300.dp, margin = 32.dp)
    assertEquals(736.dp, side)
  }

  @Test
  fun portraitBoardUsesTheSameOuterMarginAsLandscape() {
    val side = tabletBoardSide(800.dp, 1280.dp, landscape = false, hudGutter = 300.dp, margin = 32.dp)
    assertEquals(736.dp, side)
  }
}
