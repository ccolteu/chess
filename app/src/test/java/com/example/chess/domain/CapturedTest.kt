package com.example.chess.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CapturedTest {
  @Test
  fun startingPosition_hasNoCaptures() {
    val start = startingGame().squares
    assertEquals(emptyList<Piece>(), capturedOf(Side.WHITE, start))
    assertEquals(emptyList<Piece>(), capturedOf(Side.BLACK, start))
  }

  @Test
  fun missingBlackPawn_isACaptureFromBlack() {
    val squares = startingGame().squares.toMutableList()
    squares[Square.parse("a7").index] = null
    assertEquals(listOf(Piece(PieceType.PAWN, Side.BLACK)), capturedOf(Side.BLACK, squares))
    assertEquals(emptyList<Piece>(), capturedOf(Side.WHITE, squares))
  }
}
